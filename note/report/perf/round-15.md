# 性能优化第 15 轮：写路径单次元数据往返归一（第三轮循环开篇）

日期：2026-08-16
基准数据：[benchmark/results/round-15-server.tsv](../../../benchmark/results/round-15-server.tsv)（服务器实机，Paper 1.21.11 b132 + Slimefun 5.0.0）
红线核查：安全性 ✅ 稳定性 ✅（会话 0 插件异常，启动 656ms 正常）兼容性 ✅（无数据格式变更，PDC/BlockStorage 键零改动）

## 优化域：写路径单次元数据往返归一

前两轮循环（1-9、10-14）覆盖了读路径与判定路径；本轮针对**提交/写路径**——
每次成功落盘（故事提交/提取/统计计数/法杖 lore 写回）中的多次 ItemMeta
克隆与应用往返。ItemMeta 克隆含完整 NBT 深拷贝（故事物品的 PDC 列表随
克隆复制），是写路径的主导成本。

### 旧链克隆计数（每操作，tick 热路径实测前推算，基准已证实）

| 路径 | 触发频率 | 旧 getItemMeta 克隆 | 旧 setItemMeta 应用 | 列表反序列化 |
|------|---------|--------------------:|--------------------:|-------------:|
| 故事提交（常规） | 面板 testChance 通过时（T1 7%/tick） | 8 | 3 | 2 |
| 故事提交（终格+独特） | 最后一格提交 | 10 | 4 | 3 |
| 祭坛提取 | 祭坛 1/6 每工作 tick | 7 | 2 | 2 |
| 统计计数 | 每次施法/发掘/提取 | —（路径字符串双构建） | — | — |
| 法杖 lore 重建 | 每次成功施法写回 | —（动态字符串拼接） | — | — |

## 量化结果（ns/op，中位数；旧/新同场同会话）

| 基准 | 旧 | 新 | 提升 |
|------|---:|---:|-----:|
| writePath.storyCommit | 58,575 | 23,750 | **2.47x** |
| writePath.storyCommitUnique | 102,675 | 66,675 | **1.54x** |
| writePath.storyExtract | 24,575 | 18,925 | **1.30x** |
| writePath.statsIncrement | 463.9 | 396.5 | 1.17x |
| writePath.staveLoreRebuild | 15,135 | 13,978 | 1.08x |

跨会话佐证（run2）：storyCommit 2.25x / storyCommitUnique 2.06x——单次会话
存在 ±25% 的 JIT/负载方差，方向与量级一致。残留成本由 `setItemMeta` 应用与
lore 重建中的 Paper 组件转换（`CraftChatMessage.fromComponent` 正则）构成，
为 API 边界。

## 变更内容

1. **StoryUtils.commitStory（核心）**：选取后的常规故事（+可选独特故事）落盘
   归并为单次克隆 + 单次应用——故事列表反序列化/序列化、计数、满槽附魔、
   名称与 lore 重建共用同一 meta 快照。异常时保持原状态不落盘（原子性优于
   旧序列的中间态部分落盘）。
2. **StoryUtils.removeStoryAndRebuild**：祭坛提取路径同构归并；列表清空时不
   重建（调用方随即销毁物品，与旧行为一致）。
3. **pickStory/pickUniqueStory** 拆出纯选取；requestNewStory/requestUniqueStory
   委托实现保持原行为（公开 API 保留，旧链基准直接调用旧公开方法而非代码副本）。
4. **makeStoried** 归并为单次往返（2 克隆 → 1）。
5. **PlayerStatistics.addUsage/addChronicle/addRealisation**：读改写共用单次
   路径构建（原实现 get 与 set 各构建一次完整路径字符串）。
6. **InstanceStave.buildLore 静态片段常量化**：头部/尾部/标签前缀/槽位标签
   预构建为类常量，每次施法写回仅拼晶能数字。
7. **接线**：ChroniclerPanelCache.processStack 与 RealisationAltarCache.processItem
   切换到归一路径；附带 meta 快照重载（getAllStories/isGilded/getItemStackName/
   rebuildStoriedStack）。
8. **附带稳定性修复**：祭坛提取时故事列表缺失（损坏数据）按空处理失败关闭
   （原实现 `storyList.isEmpty()` 对 null 直接 NPE 穿透 tick）。

## 等价性验证（运行时断言，四组全部通过）

| 组 | 场景 | 比对 |
|----|------|------|
| A | 常规提交（2 预置 → 3） | ✅ true |
| B | 终格常规+独特提交（4 预置 → 满 5 + 附魔分支） | ✅ true |
| C | 提取（3 预置 → 2） | ✅ true |
| D | 法杖 lore（旧动态拼接 vs 新静态片段） | ✅ true |

比对内容：PDC 键集 + 逐键多类型探测值、lore 列表、显示名、附魔映射、物品标志。
另经 commitStory 的代码审查确认：计数仅常规故事 +1（独特不计数）、附魔阈值
读自同一快照（上限不受提交影响）、名称读取自快照与已应用状态一致。

## 回会话过程中的发现（记录，未在本轮修复）

1. **"有故事的"前缀叠加（上游既有外观缺陷）**：`rebuildStoriedStack` 的
   `setName` 每次在既有显示名前再叠加一次"有故事的"前缀（旧/新行为一致，
   等价性 C 已证）。游戏内单物品最多重建约 11 次（5 常规+独特+提取），名字
   叠加 ≤11 层前缀；基准中若不复位则无界增长且 Paper 组件转换呈二次方变慢
   （run2 卡顿根因）。属上游遗留，修复涉及可见行为变更，留待审计轮处理。
2. **Bukkit `setLore` 256 行上限**：基准状态增长型操作须小批 + 批间复位
   （每故事约 9 行 lore）。游戏内上限 5 故事约 50 行，不受影响。
3. **看门狗转储 4 次**（run4）：2 次为 round-8 blocks.yml 双解析基准既有现象
   （r14 会话同样存在）；2 次为 round-15 lore 重建计时批次（10 万次 × ~15µs
   ≈ 连续占主线程 8s，跨变体越过 10s 窗口）——纯基准现象，非插件路径，
   `DO NOT REPORT` 级线程转储，数据有效。后续轮可将该组 batchOps 减半。

## 不做项论证

- **ChroniclerPanelCache.summonParticles 每 tick 2 次 Location 分配**：
  ~几十 ns/tick，相对当 tick 的 spawnParticle（微秒级 API 边界）收益可忽略，
  不值得为此引入可变共享 Location。
- **animateLight 的 BlockData 读写往返**：灯光呼吸为视觉必需，等级每 tick
  必变，无法缓存跳过。
- **rebuildStoriedStack 内 `BaseComponent.toLegacyText` 的逐行组件转换**：
  单次 ~15µs 中占主导，但输出格式（彩色 lore）为既定视觉契约，重写为手拼
  legacy 码属行为风险大于收益。

## 变更文件

- src：StoryUtils / StoriesManager / NameUtils / GildingUtils / PlayerStatistics /
  InstanceStave / Spell（getName 缓存试行后回退）/ ChroniclerPanelCache /
  RealisationAltarCache
- benchmark：CHPerfBench.benchRound15（timeResettable 复位计时器 + 四组等价性断言）
- commit：e4fcd57（perf core）/ 22ab077（bench）/ ccfb9c1（getName 回退）/
  e05c95c、及 roundTrip→extract 修正与预热前复位修正（bench）

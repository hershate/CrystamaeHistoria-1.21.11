# 性能优化第 12 轮：启动路径（冷路径）

日期：2026-08-16
基准数据：[benchmark/results/round-12-server.tsv](../../../benchmark/results/round-12-server.tsv)（组件基准）+ 启动分段计时（会话日志）
红线核查：安全性 ✅ 稳定性 ✅（跑分会话 0 插件异常，优雅关停）兼容性 ✅（落盘终态逐字节等价，见下）

## 测量设施（本轮新增）

1. **onEnable 分段计时**（`CrystamaeHistoria.onEnable`）：配置加载/故事域构建/
   管理器/法术配置/物品注册/命令六段，单条 INFO 汇总——冷路径画像的永久设施。
2. **基准组链式调度**（CHPerfBench）：各基准组之间让出 2 tick，修复基准套件
   连续阻塞主线程触发 watchdog 线程转储乃至强制停机的问题（第三轮迭代：链式
   调度 → Runnable 数组类型修正 → 失控变体 batchOps 修正）。
3. **run-server-bench.sh**：固化「清理 25565 残留服务端 → 启动 → 到时停机 →
   状态汇报」会话流程（仅清理确认监听 25565 的进程，不影响 Gradle/Maven 等）。

## 基线画像（优化前，单次启动）

| 阶段 | 耗时 |
|------|------|
| 配置加载（ConfigManager，5 YAML） | 211.3ms |
| 故事域构建（StoriesManager） | 30.9ms |
| 管理器（监听/任务/SpellMemory/可选插件） | 157.3ms |
| 法术配置（loadConfig + enabledSpells） | 167.8ms |
| 物品注册（setupSlimefun，274 物品） | 262.8ms |
| 命令 | 1.69ms |
| **合计** | **831.8ms** |

> 插件 onEnable 合计约 0.8s，占整机启动（~22s）不到 4%——大头在 Paper/Slimefun
> 引擎侧（边界）。本轮把插件侧可动部分做完。

## 本轮优化（commit 57720c1）

### 1. updateConfig 稳态免落盘（每次启动生效）

原实现对 blocks.yml / generic-stories.yml **每次启动无条件 `config.save(file)`**
（995 键全量 YAML 序列化 + 写盘）。copyDefaults 语义只补缺失键、不覆写既有值，
故"无缺失键"时写盘产物与现存文件逐字节等价——改为先做默认键存在性检查，仅
存在缺失时落盘。in-memory 表示（addDefaults + copyDefaults）不变，读取路径
行为完全一致。

### 2. loadConfig 首次启动批量补键

原实现每发现一个缺失法术键就整文件 `spells.save(file)` 一次（首次启动至多 69
次全量序列化+写盘）。改为循环内仅 set，循环后检测到新键时一次落盘。

### 3. 故事解析单读

`Story` 构造器 `getIntegerList("shards")` 读两次（1220+ 个故事构造）；
`fillBlockDefinitions` 的 `getStringList("elements")` 读两次（995 键）。各改单读。

## 量化结果

组件基准（Paper 1.21.11 b132 实机）：

| 场景 | 旧 | 新 | 加速比 |
|------|-----|-----|--------|
| blocks.yml 启动落盘决策（save vs 存在性检查） | 37.9ms | 2.4ms | **16.0x** |
| spells.yml 首次启动补键（69 次逐键 save vs 一次） | 54.8ms | 0.86ms | **63.8x** |
| 故事 shards 列表读取（双读 vs 单读） | 132.6ns | 61.8ns | **2.1x**（×1220 构造） |

启动分段 A/B（单次启动有 JIT/类加载抖动，after 为三次启动 543.6/576.1/585.2ms）：

| 指标 | 基线 | 优化后 |
|------|------|--------|
| onEnable 合计 | 831.8ms | ~568ms（均值，±20ms） |
| 配置加载段 | 211.3ms | ~168ms（−43ms，与 38ms save 消除吻合） |

> 归因说明：配置段 −43ms 与组件基准的 save 消除量吻合，为可归因收益；其余
> 分段（管理器 157→~80、法术配置 168→~63）的降幅超出本轮改动可解释范围，
> 主要为单次启动抖动（类加载/JIT/磁盘缓存顺序效应），不纳入宣称。

## 兼容性分析（红线）

1. **落盘终态等价**：稳态跳过 save 时文件内容本就与将写内容一致；首次启动
   批量补键的最终 spells.yml 与逐键保存逐字节一致（中间态不可观察）。
2. addDefaults/copyDefaults 的 in-memory 语义不变——用户删掉的键仍由默认值
   兜底读取；新增默认键（升级）时仍会写盘一次。
3. 计时日志为纯增量输出，无行为影响。

## 验证

- `JAVA_HOME=F:/Java/21 mvn package` 构建通过；
- 跑分会话：COMPLETE 正常，0 插件异常，优雅关停（EXIT=0）；全套件 60 变体
  数值与历轮同量级（无回归）。

## 变更文件

- [CrystamaeHistoria.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/CrystamaeHistoria.java)（分段计时）
- [ConfigManager.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/managers/ConfigManager.java)（两项落盘策略）
- [Story.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/stories/Story.java) /
  [StoriesManager.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/managers/StoriesManager.java)（单读）
- benchmark/server-addon（benchRound12 + 链式调度框架）；benchmark/run-server-bench.sh（新增）

## 下一轮候选

- `RECIPES_ITEMS` 物品配方匹配（条件复杂量大时可索引化）
- 剩余启动分段中「物品注册 232ms」为 Slimefun API 边界（274 物品构造），插件侧无可动空间
- 收敛评估：热路径/事件路径/机械路径/冷路径四域已做，剩余均为引擎边界

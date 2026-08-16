# 性能优化第 16 轮：展示行构建缓存（Story display/lore 记忆化）

日期：2026-08-16
基准数据：[benchmark/results/round-16-server.tsv](../../../benchmark/results/round-16-server.tsv)（服务器实机，Paper 1.21.11 b132 + Slimefun 5.0.0）
红线核查：安全性 ✅ 稳定性 ✅（会话真实插件异常 0；日志中 1 条含插件名的 ERROR 为看门狗转储堆栈帧，时间戳与转储一致，见 §基准设施观察）兼容性 ✅（无数据格式变更）

## 优化域：展示行的重复构建消除

第 15 轮实测确认故事提交/提取后 `rebuildStoriedStack` 的残留主导成本是
lore 重建（每故事一次 `getDisplayName` + 一次 `getStoryLore`）。本轮观察：
**Story 池实例是全局共享单例**（`StoriesManager` 启动期构建，995 条），而
`getDisplayName()/getStoryLore()` 的输出仅依赖构造后不可变的字段
（id/rarity/storyStrings/author/sponsor 均为 final 无 setter）——同一故事
在任何物品上重复出现时展示行完全恒定，却每次重建 bungee 组件并做
`BaseComponent.toLegacyText` 转换。

调用方核实：`StoriesManager.rebuildStoriedStack`（addAll，只读）与
`StoryCollectionFlexGroup`（CustomItemStack.create 可变参数，只读）——
缓存列表可以不可变副本安全返回。

## 量化结果（ns/op，中位数）

| 基准 | 旧（每次新鲜构建） | 新（缓存命中） | 提升 |
|------|------------------:|--------------:|-----:|
| storyDisplay.displayName | 144.57 | 2.92 | **49.5x** |
| storyDisplay.loreLines | 121.41 | 2.99 | **40.6x** |
| storyDisplay.rebuild4Stories（4 条组装段） | 1,210.90 | 122.51 | **9.9x** |

生效路径：每条故事提交/提取后的物品 lore 重建（第 15 轮后仍有 ~19-24µs 的
路径再降约 1µs/条 + 组件分配清零）、故事图鉴 FlexGroup 构建、独特故事展示。
`rebuild4Stories` 组装段从 1.21µs 降至 0.12µs。

## 变更内容

1. **Story.getDisplayName 懒缓存**（cachedDisplayName 字段）：输入不可变，
   输出恒定；主线程单线程访问，偶发重复计算无害（幂等）。
2. **Story.getStoryLore 懒缓存**（cachedStoryLore 字段，`List.copyOf`
   不可变副本）：防调用方意外变异共享缓存（两处调用方均为只读，防御性约束）。
3. `copy()` 拷贝构造不受影响（拷贝实例首次访问时独立计算并缓存自身副本）。

## 等价性验证

跨稀有度采样（COMMON..MYTHICAL 各取 1 条，共 6 条）：新鲜构建（0.3.0 同构
副本）与缓存输出的显示名（String.equals）与正文行（List.equals）**逐条一致
（true）**。缓存命中路径返回首次计算的同一字符串/不可变列表。

## 基准设施观察

- 会话看门狗转储 3 次：1 次 round-8 YAML 旧疾 + 2 次 round-15 lore 计时
  批次（10 万次 × ~15µs 连续占主线程）；后者本轮已将 batchOps 减半至 4 万
  （后续会话消除）。日志扫描脚本会把看门狗转储中的插件堆栈帧误计为
  "CH_ERROR"——人工归因确认真实插件异常为 0（帧时间戳与转储一致）。

## 变更文件

- src：stories/Story.java
- benchmark：CHPerfBench.benchRound16（displayName/loreLines/rebuild4Stories
  六变体 + 采样等价性断言 + 同构新鲜构建副本）
- commit：27832cf（perf core）/ f77fbcf（bench）/ 本轮数据落盘 chore

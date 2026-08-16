# 性能优化第 23 轮：统计计数纪元缓存

日期：2026-08-16
域：**PlayerStatistics 计数路径的重复查询**——图鉴统计堆（每页 1 次）、
图鉴命令、液化池 Exalted/Uniques rank 谓词（每次催化剂投入调用
`getStoryRank` → `getStoriesUnlocked`，274 键全扫描）。
红线：安全/稳定/兼容，无数据格式变更，失效完备性经真实写方法断言。

## 热点定位（源自 round-22 服务器实测）

`stats.countStories274 new_relative_read = 255,725 ns`——相对读取后**每次
计数仍为 O(n) 全键扫描**，且 `getStoryRank/getSpellRank/getGildingRank`
经谓词被高频重复调用（同一玩家连续投催化剂/反复翻图鉴时，结果在两次
统计写入之间恒定）。

## 前提核验（推翻 round-22 不做项）

round-22 曾论证"跨调用缓存有失效协议风险"而未做。本轮核验前提：

1. `player_stats.yml` 运行期**全部 6 个写点**（unlockSpell/unlockUniqueStory/
   unlockStoryGilded/addUsage/addChronicle/addRealisation）均在
   `PlayerStatistics` 类内（grep 全库无外部 `getPlayerStats().set`）；
2. 该文件运行期**不重载**（ConfigManager 仅加载一次 + 周期落盘）；
3. 主线程单线程模型（与项目既有缓存一致）。

→ 全量写点递增纪元（epoch）的失效协议**完备**，与 round-5 机械判定
备忘录同族（显式失效点模式）。

## 实现（91cfe69）

- `statsEpoch` 静态纪元：6 个写方法写后各递增一次；
- `COUNT_CACHE: Map<UUID, CountCache>`（字段级纪元：spells/stories/gilded
  各带独立纪元与值）；
- 三个计数方法：纪元命中直接返回；未命中走相对读取全扫描后写缓存；
- `hasUnlocked*` 判定**不缓存**（页级 36 次相对读取已够快，且解锁判定
  语义要求即时）；
- 缓存条目有界：键为查询过计数的玩家，条目为 6 个 int。

## 量化

### standalone（benchmark/results/round-23.tsv，真实 YamlConfiguration，3 fork 均值，每次完整计数）

| 变体（274 键计数） | ns/次 | 相对提升 |
|------|----|------|
| 旧：全路径逐键重建（round-22 前） | 61,485.68 | — |
| 中：子节相对读取（round-22，无缓存） | 36,414.17 | 1.69x |
| 新：纪元缓存命中 | **5.21** | **~6,990x（vs 相对）/ ~11,800x（vs 全路径）** |
| 新：失效后重算（写后第一次） | 37,794.01 | 与相对读取同（缓存开销 +3.8%，噪声级） |

等价性断言（每 fork）：steady / invalidation（写入→纪元递增→计数必须变化）/
restore 全 true。

### 服务器内（Paper 1.21.11 build 132 + Slimefun 5.0.0，真实 PlayerStatistics）

实测规模注：`blockDefinitionMap` 实为 ~998 个方块定义（本轮以
`getBlockDefinitionsSortedByMaterial()` 实注入解锁，i%2==0 → 计数 499，
等价性日志行可证），此前报告引用的 274 为 blocks.yml 顶层节数而非
定义总数——计数成本与真实规模成正比，倍率结论不受影响。

| 基准（每次调用） | 旧 (ns) | 新 (ns) | 提升 |
|------|----|----|------|
| stats.countStories274.r23 全路径逐键重建 | 121,205.40 | — | — |
| stats.countStories274.r23 相对读取（round-22 实现） | 68,922.30 | — | 1.76x |
| stats.countStories274.r23 纪元缓存命中 | — | **8.77** | **~7,859x（vs 相对）/ ~13,820x（vs 全路径）** |
| stats.rankPredicate 液化池 rank 谓词稳态（getStoryRank 全链） | — | 11.17 | 旧为一次全量计数（≥68.9µs）→ **>6,000x** |

同会话 round-22 组的 `new_relative_read` 变体显示 8.27ns——该组注入
数据后首次调用已计算并进入缓存，后续循环为命中（本轮行为），符合预期。

## 等价性与回归

- 服务器内断言（真实写方法）：缓存值 == 现场重算；`unlockUniqueStory`
  后计数 +1（纪元失效生效）；`addChronicle`（计数类写入）后缓存与现场
  一致；rank 谓词路径正常；
- 单槽判定路径（hasUnlocked*）未缓存，行为与 round-22 完全一致；
- 基准数据注入均经真实写方法（自动递增纪元）或先注入后首查（无陈旧
  缓存条目），清理 `set(uuid, null)` 前无落盘（SaveConfigRunnable 周期内）。

## 基准缺陷更正（连带 round-22）

本轮发现 round-22 standalone 计数两行的变体 lambda 未按 `size` 循环，
被 Harness 按 size 误除——countStories274 的跨变体比率（原 6.3x）失真，
已在 [round-22.md](round-22.md) 更正为 1.89x（countSpells69 同除数
比率 1.95x 巧合成立）。服务器端数字不受影响。本轮基准已修正为逐次
循环并复测。

## 兼容性

- 无数据格式变更；读 API 语义不变（写入后立即可见新值——纪元失效保证）；
- 缓存为进程内状态，重启即重建。

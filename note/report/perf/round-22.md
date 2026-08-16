# 性能优化第 22 轮：玩家统计读取路径

日期：2026-08-16
域：**PlayerStatistics 读路径**——图鉴翻页每槽解锁判定（36/页 ×3 图鉴）、
页级统计堆的解锁计数（O(n)）、以及同源的液化池配方 rank 谓词
（`Exalted/Uniques::isMaxRank` → `get*Unlocked` 计数）。
红线：安全/稳定/兼容，无数据格式变更，读语义逐键等价（断言验证）。

## 热点定位

1. **每槽全路径解析**：`hasUnlockedSpell/hasUnlockedUniqueStory/hasUnlockedStoryGilded`
   每次构建 `uuid.TYPE.<id>.UNLOCKED` 字符串并从配置根逐层行走（3 层
   MemorySection 解析）——图鉴每页 ×36。
2. **解锁计数 O(n) 全路径重建**：`getSpellsUnlocked/getStoriesUnlocked/getBlocksGilded`
   已持有子节 `getKeys(false)`，但内层逐键重建全路径再**从根**解析——
   O(n) 次全路径行走 + 长字符串拼接（n=69/274）。
3. 计数方法被统计堆（每页 1 次）与液化池 rank 谓词（每次催化剂匹配）
   调用。

## 实现（9b44cef）

1. `PlayerStatistics` 新增 `getSpellStatSection/getStoryStatSection`
   （单次解析玩家统计子节）与三个 `hasUnlocked*` 相对读取重载
   （子节内单层行走；子节为 null 按未解锁——与原全路径缺失返回 false 语义一致）。
2. 三个 FlexGroup 翻页：页级单次解析子节，36 槽相对读取。
3. 三个计数方法内层改子节相对读取。
4. 全路径方法保留：机械路径（记录者面板独特故事解锁检查、液化池充能板
   解锁检查——事件级）与既有签名不变，兼容性零影响。

## 不做项论证

- **不做跨调用缓存**（如已解锁集快照）：解锁写入点分散
  （面板/液化池/镀金器），失效协议引入正确性风险，收益/风险比低；
  本轮以"同数据同语义的更短路径"为界。
- `getChronicle/getRealisation`（详情堆 ×2 全路径）：详情点击级 ×1，
  非页级热点，保持。

## 量化

### standalone（benchmark/results/round-22.tsv，真实 YamlConfiguration，3 fork 均值）

规模与真实一致：1 玩家 × 69 法术（2/3 解锁）× 274 故事（1/2 解锁、1/4 镀金）。

| 基准 | 旧 | 新 | 提升 |
|------|----|----|------|
| stats.singleCheck（单次判定） | 158.43 ns | 78.46 ns | 2.0x |
| stats.pageCheck36（页 36 槽） | 5,410.95 ns | 2,799.99 ns | 1.93x |
| stats.countSpells69（每键） | 1.29 ns | 0.66 ns | 2.0x |
| stats.countStories274（每键） | 24.57 ns | 3.89 ns | 6.3x |

等价性断言（每 fork）：single / count / missing（无统计玩家两路径
同 false 且子节 null）全 true。

### 服务器内（Paper 1.21.11 build 132 + Slimefun 5.0.0，真实 PlayerStatistics）

| 基准（每次调用） | 旧 (ns) | 新 (ns) | 提升 |
|------|----|----|------|
| stats.pageCheck36（图鉴页 36 槽判定） | 11,845.40 | 5,763.22 | **2.06x** |
| stats.countSpells69（法术解锁计数全量） | 25,082.44 | 11,234.27 | **2.23x** |
| stats.countStories274（故事解锁计数全量） | 342,625.80 | 255,725.07 | 1.34x |

说明：活配置绝对值大于 standalone（`FileConfiguration` 读取含 defaults
链检查与更大的映射）；countStories274 的剩余成本由 `getKeys(false)` 的
LinkedHashMap 键集复制主导（两变体同侧），路径行走优化份额为 1.34x。
该计数经 `getStoryRank` 被液化池 Exalted/Uniques 配方谓词在每次催化剂
匹配时调用，绝对削减 ~87µs/次。

## 等价性与回归

- standalone + 服务器内逐键等价断言（全路径 vs 相对、计数一致、缺失语义）：
  服务器日志 `round22 等价性: spell=true story=true count=true missing=true`；
- 全路径方法未删除未改语义（机械/配方路径不变）；
- 服务器会话：CHPERFBENCH COMPLETE、0 SEVERE（会话日志
  `benchmark/results/round-22-session.log`；5 次 watchdog 转储仍为旧轮
  configParse 重型组基线，见 round-21 报告说明）。

## 兼容性

- 无数据格式变更；合成基准数据注入后即行清理（`set(uuid, null)`），
  不落盘。

# 性能优化第 31 轮：解锁集合纪元缓存（第六轮循环第 1 轮）

日期：2026-08-17
域：**图鉴页级解锁判定**——三图鉴翻页 36 槽 ×3（round-22 相对读取后
每页仍 ~3µs）。红线：用户体验一致 + 对外 API 不变 + 写后即时可见。

## 实现（774d6e7 + c095603 + f181593）

- `PlayerStatistics` 新增三个不可变集合快照：`getUnlockedSpellIdSet` /
  `getUnlockedUniqueStorySet` / `getGildedSet`（字段级纪元 + 不可变视图）；
- **成员资格纪元与计数纪元分离**（c095603）：`membershipEpoch` 仅由
  3 个解锁写递增；计数写（addUsage/addChronicle/addRealisation）只改
  TIMES_* 数值不改变成员资格——避免"施法后翻页"交错场景反复重建
  （基准首轮揭示的退化，服务器断言"计数写不失效集合"实证）；
- 三个 FlexGroup 页级取快照，36 槽 `Set.contains`；
- 既有 per-call `hasUnlocked*` 保留（机械/事件级与对外 API 不变）。

## 量化（服务器内真实 PlayerStatistics，round-31-server.tsv）

| 基准 | 相对读取（round-22） | 集合命中 | 提升 |
|------|----|----|------|
| stats.pageCheck36.sets（法术集页 36 槽） | 3,020.00 ns | 337.12 ns | **8.96x** |
| stats.storyPageCheck36（故事集页 36 槽，~998 定义键空间） | 2,946.70 ns | 116.24 ns | **25.3x** |
| stats.snapshotRebuild（解锁写后首次查询，三集合） | — | 178,057 ns | 摊销：仅解锁触发 |

等价性断言：spells / stories / gilded / invalidation（解锁后集合更新）/
empty（无统计空集）/ 计数写不失效集合——**全 true**；
计数缓存未受影响（r23 命中 10.13ns 复验）；会话 0 SEVERE、0 组失败。

## 过程事件记录（如实）

1. **纪元补丁半应用**（已修 f181593）：批量字符串替换同时命中计数与
   集合两族六处 setter 且未覆盖集合方法判断行——导致两族缓存永久 miss
   （storyPage 集合"命中"89µs ≈ 重建成本，页判定反转慢 37x）。
   服务器内"计数写不失效集合"断言直接暴露该缺陷。教训：**批量文本
   替换改代码后必须 grep 全部同名字段核对 check/set 配对**；
2. **基准会话六连失败排查**（最终 COMPLETE）：宿主阶段性 ~2x 变慢
   （用户 Gradle 守护进程等活动）使整套基准超出会话窗口，且重型组
   整组单 tick 超 watchdog 阈值触发强制停机——三项基础设施修复：
   configParse 组批量 20→4（慢宿主 watchdog 安全）、测试服
   `spigot.yml timeout-time` 60→600、会话窗口 660→1200s；
   最终会话回到基线（3 次转储）。

## 兼容性

- 旧存档/数据格式零变更；集合为进程内读缓存（重启重建）；
- 解锁判定即时性：解锁写后下一次查询即见新值（成员纪元失效保证）。

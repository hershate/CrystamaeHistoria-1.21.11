# 性能优化第 17 轮：召唤物 AI 每 tick 路径

日期：2026-08-16
基准数据：[benchmark/results/round-17-server.tsv](../../../benchmark/results/round-17-server.tsv)（服务器实机，Paper 1.21.11 b132 + Slimefun 5.0.0）
红线核查：安全性 ✅ 稳定性 ✅（会话 0 插件异常，看门狗 2 次均为 round-8 YAML 基准旧疾）兼容性 ✅（无数据格式变更）

## 优化域：召唤物 AI 目标路径（utils/mobgoals/，前两轮循环未覆盖）

召唤物存活期间其 Goal 每 tick 运行（AbstractGoal.tick 或覆写版）。域内逐类
精读后落点：

1. **AbstractGoal.removeOffline**：每 tick 走 `getOfflinePlayer(uuid)` →
   `isOnline()` → `getPlayer()` 三步（OfflinePlayer 档案对象 + 三次查找）。
2. **AbstractGoal/HolyCowGoal.tick**：`self.getTarget()` 每 tick 最多读 3 次。
3. **AbstractGoal.getTypes**：每次调用 `EnumSet.of(GoalType.TARGET)` 新分配。
4. **BatteringRamGoal.tick**（冲撞车每 tick）：5×3×5=75 个 `BlockPosition`
   中间对象 + `List.contains` 去重（O(n²) ≈ 2800 次比较/tick）——三重循环的
   (x,y,z) 组合构造上互异，去重是纯无效工作。
5. 跟随距离 `distance`（开方）与 `getVelocity()` 双读。

## 量化结果（ns/op，中位数）

| 基准 | 旧 | 新 | 提升 |
|------|---:|---:|-----:|
| ramGoal.blockScan | 23,722.85 | 1,755.64 | **13.5x** |
| mobGoal.ownerLookup | 17.75 | 3.26 | **5.4x** |
| mobGoal.typesAlloc | 8.56 | 2.74 | 3.1x |
| mobGoal.followDistance | 7.32 | 6.69 | 1.09x（边际） |
| mobGoal.targetReads | 2.71 | 3.73 | **无显著差异（噪声级）** |

如实记录：Paper 的 `CraftMob#getTarget` 是廉价字段/记忆读（三连读仅 2.7ns），
目标读取缓存无 measurable 收益（保留改动仅为结构性减少 API 调用）；
`sqrt` 为硬件级廉价，distanceSquared 仅边际。**本轮实质收益集中在
blockScan（冲撞车每 tick 22µs → 1.8µs）与 ownerLookup（每召唤物每 tick
省 ~14.5ns + 免 OfflinePlayer 分配）**；typesAlloc 消除注册/重评估期的
EnumSet 分配。

## 等价性验证（四组断言全部通过）

| 组 | 内容 | 结果 |
|----|------|------|
| types | `EnumSet.of(TARGET)` 与共享常量内容相等 | ✅ |
| ownerOffline | 离线 UUID 下旧三步与新单次同判 null | ✅ |
| scanOrder | 旧去重列表与新直接遍历的坐标序列逐项一致（75 项同序） | ✅ |
| distance | 64 组位置采样：`distance > t ⇔ distanceSquared > t²` | ✅ |

代码级论证：`getPlayer(uuid)` 为 null ⇔ `getOfflinePlayer(uuid).isOnline()`
为 false（同一 UUID 的在线语义等价）；冲撞车块访问顺序与原集合构建序一致
（去重本为 no-op）。

## 域内不做项论证

- **HolyCowGoal 每 tick 的 1³ 实体扫描**：爆炸触发机制本身（接近即爆），
  节流属行为变更，不做。
- **AbstractRidableGoal/RidableGroundGoal**：类注释标记 "Unused currently"，
  运行期不可达，不做。
- **ThemeType.getByRarity/getByType**：核查为 O(1) switch 表，无需处理。
- **目标选取的 getNearbyEntitiesByType（10³ 盒 + 逐候选 PDC 读）**：实体
  PDC 读 ~15ns（round-10 实测），扫描为重定向机制的必要 API 边界。

## 变更文件

- src：mobgoals/AbstractGoal、HolyCowGoal、BatteringRamGoal
- benchmark：CHPerfBench.benchRound17（十变体 + 四组等价性断言）
- commit：5b05eaa（perf core + bench 合并提交）

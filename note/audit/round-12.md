# 审计第 12 轮：mobgoals 召唤物 AI 目标类

日期：2026-08-15
范围：`utils/mobgoals/` 全部 10 个类 + 使用方（7 个召唤系法术）核验

## 已修复（1 个 commit）

| commit | 问题 |
|--------|------|
| `1105890` | **跨世界 distance IAE ×2**：AbstractGoal/HolyCowGoal 的跟随逻辑在主人穿过传送门后 AI 每 tick 抛 IllegalArgumentException（日志风暴+AI 卡死）——不同世界跳过跟随（SpellMemory 过期清理兜底）。**AbstractRidableGoal 离线空引用**：乘客未必是主人（他人可骑上召唤物），`Bukkit.getPlayer(owner)` null 时链式 `getEyeLocation()` NPE——离线退回常规 AI tick |

## 核验安全（记录依据）

- AbstractGoal 主链：owner 离线自毁（removeOffline）、目标敌对实体的召唤物豁免（PDC owner 过滤）、目标即主人时清除 ✓
- FlyingBatGoal/RidableGroundGoal **非死代码**：PhantomsFlight/Ravage 法术实际使用（类注释"Unused currently"过时，更正记录）
- HolyCowGoal 自爆：createExplosion(player 源, 10, 不点火, 不破块)，玩家在线已校验 ✓
- BatteringRamGoal：破块走 `blockCanBeBroken`（领地+机械/TileState/硬度豁免）；velocity 静止自毁 ✓；不调 super.tick 由 SpellMemory 120s 过期兜底 ✓
- BoringGoal/DeityGoal/FiendGoal/GolemGoal/LeechGoal：纯配置覆写，主链安全 ✓

## 记录（不改）

1. BatteringRamGoal 每 AI tick 扫 75 方块且 ArrayList.contains 线性比较——攻城兽存活期短（速度阈值自毁），可接受；若未来调长寿命需改 Set。
2. AbstractRidableGoal 类注释"Unused currently"与实际使用不符（已在使用），仅注释过时。

## 验证

`JAVA_HOME=F:/Java/21 mvn -q package` 构建通过。

# 审计第 10 轮：gadgets 深审（刷怪塔全家桶等 18 类）

日期：2026-08-15
范围：`slimefun/items/gadgets/` 全部 18 个类逐个精读

## 已修复（2 个 commit）

| commit | 问题 |
|--------|------|
| `b41eaf6` | **CursedEarth 多方块共享计数器（状态污染）**：刷怪计时为单例实例字段，N 块诅咒之土共享——计数以 N 倍速推进，刷怪频率随方块数失控。改 per-location 映射 + 消失/破坏清理。**FragmentedVoid 物品不落盘（数据丢失）**：吸收掉落物经 `toInventory().addItem()` 绕过 BlockMenu 脏标记，内容不持久化——重启后玩家物品丢失；补 markDirty + 背包缺失判空 |
| `96e8671` | **BlockPlacer 放置缺键 NPE**：MobFan（tick+onNewInstance 的 valueOf/fromString(null) 每 tick 报错；无所有者失败关闭以防绕过领地校验）、MobLamp（循环内重复解析+缺键 NPE）、MobMat/GreenHouseGlass（onFirstTick NPE 阻断标记写入→每 tick 重抛）。**映射泄漏**：MobMat/MobTrap/GreenHouseGlass/MysteriousTicker 破坏不清条目。TrophyDisplay 死状态 locationConsumer（只写不读且跨实例共享）与 defaultConsumer 删除 |

## 核验安全（记录依据）

- EnderInhibitor：抑制表 2s 过期由 TemporaryEffectsRunnable 清理，有界 ✓
- AngelBlock：视线放置有权限+高度界检查，BlockStorage.store 还原掉落 ✓
- Waystone/MysteriousTickerNoInteraction：继承已修复的 MysteriousTicker ✓
- GlassOfMilk/MobCandle/PhilosophersSpray：简单无状态或已在触发器链路核验 ✓
- ExaltationStand：afterTick 从物品反查效果，无共享状态 ✓
- TrophyDisplay 空手右键：Slimefun `getItem()` 空手返回 AIR（REF 源码核验），无 NPE ✓
- MobFan 非生存玩家中断：上游设计（防滥用），保留

## 验证

`JAVA_HOME=F:/Java/21 mvn -q package` 构建通过。

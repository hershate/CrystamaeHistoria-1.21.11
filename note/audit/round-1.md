# 审计第 1 轮：常驻定时器 + SpellMemory 生命周期 + 施法者离线路径

日期：2026-08-15
范围：`runnables/`、`SpellMemory.java`、`listeners/SpellEffectListener.java`、`listeners/MiscListener.java`、`magic/spells/core/Spell.java`、`magic/spells/tier1/*`（UUID 比较类）、`slimefun/items/tools/SleepingBag.java`、`slimefun/items/tools/LuminescenceScoop.java`

## 已修复（4 个 commit）

| commit | 问题 | 影响 |
|--------|------|------|
| `7133f1a` | `ParticleDisplayRunnable` 循环内误用 `return`（应 `continue`） | 遇到第一个未持荧光勺的玩家即终止整个循环，多用户下 LIGHT 粒子高亮基本失效 |
| `8d41424` | 全库 13 处 `==`/`!=` 比较 UUID 引用 | 玩家重登/实体重建后引用不同 → 自我豁免失效，自己的 AOE/弹射物可命中自己（SpellEffectListener×2、Spell.getTargets、Tracer、BloodMagics、ChillWind、Gyroscopic、Push、Quake、Shroud、StarFall、TimeDilation、Vacuum） |
| `8d41424` | ChillWind 注册 negative 效果却调用 `applyPositiveEffects`（读空的 positive 表） | 冻结满值后的 SLOWNESS/MINING_FATIGUE 从未生效，与描述不符 |
| `c0d3b75` | `strikeMap` 无过期清理、`clearAll()` 遗漏 | LightningStrikeEvent 被其他插件取消时条目永久泄漏（连带 CastInformation 引用） |
| `c0d3b75` | flight/frozenTime/frozenWeather 离线玩家条目永不移除 | 多用户长跑无界泄漏 |
| `c0d3b75` | `removeBlocks` 对卸载世界抛 `IllegalStateException`（dough BlockPosition 弱引用） | 异常穿透中断本轮**全部**后续清理（飞行/时间/天气/末影人/禁刷区/展示物品停摆）+ 每秒刷日志。现捕获并保留条目待世界重载 |
| `c0d3b75` | tick 法术在施法者离线后 NPE（大量回调链式 `getCasterAsPlayer().getLocation()`） | `SpellTickRunnable` 现离线即终止 + tick/收尾回调断路器防日志风暴 |
| `9d06e03` | 睡袋刷床复制：下线残留床 / 他人挖掘爆炸采集 / sleep 失败不回滚 | 睡袋不消耗且生成真实床方块，可无限刷床物品。现三路径全堵 |

## 遗留观察（后续轮次处理）

1. **Round 5**：`SpellEffectListener` 命中回调（pre/affect/post）在施法者离线后仍可能 NPE（弹射物命中路径，无中央守卫）；`StarFall.onTick` 的 `getProjectile()` 为 @Nullable 未判空。
2. **Round 6**：`MiscListener.checkCooldown` 只查主手——副手持冷却物品可绕过；`onUseScoop` 未过滤副手事件与左键，可调光勺副手在场时每次右键触发两次 `adjustLight`。
3. **待确认（歧义）**：`Shroud` 描述"造成轻微伤害并致盲"但 `makeDamagingSpell(0,…)` 且无伤害调用，实际只有 WITHER/BLINDNESS 效果——"轻微伤害"是否指 WITHER 的持续伤害？暂不改。
4. **已知限制（接受）**：睡袋地图仅内存态，服务器**崩溃**（未走 onDisable）会残留一张床；正常关停由 `clearAll` 清理。床仅放半块（视觉缺陷，功能不受影响）。
5. 死代码：`TunnelBore`/`TunnelBoreRunnable` 未注册（见 spell-system-analysis.md）。

## 验证

`JAVA_HOME=F:/Java/21 mvn -q package` 构建通过，产物 `target/CrystamaeHistoria-1.21.11-1.jar`。

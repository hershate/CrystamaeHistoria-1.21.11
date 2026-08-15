# 审计第 25 轮：消费/扣减时序与事件否决回滚完整性

日期：2026-08-15
范围：玩家交互路径的物品消耗时机 vs 事件链否决（防"否决交互上的白扣"与绕过）

## 依据（REF/Slimefun4.1 源码核验）

`SlimefunItemInteractListener.onRightClick` 为默认 `@EventHandler`（无 `ignoreCancelled`）——**被其他插件取消的 `PlayerInteractEvent` 仍会派发 `PlayerRightClickEvent`**，附属的 ItemUseHandler/BlockUseHandler 照常执行。这是 Slimefun 生态级行为（附属普遍依赖自带领地校验兜底——本附属的盐/勺/灵绸/画笔/Displacer/ImbuedStand/PowderedEssence 校验即此用途，round 6/11 补齐后已闭环）。

## 已修复（1 个 commit）

| commit | 问题 |
|--------|------|
| `9cc3759` | **直接监听 PlayerInteract 系事件的 5 处缺 `ignoreCancelled`**：被保护/防作弊插件否决的交互仍进入附属逻辑——SpellCastListener（**否决交互仍施法并扣充能/冷却**）、ThaumaturgicSaltsListener（仍清池扣盐）、PoseChangerListener ×2、MiscListener.onUseScoop。统一补 `ignoreCancelled=true`；checkCooldown 刻意保持 LOWEST 不加（其职责正是在其他插件前否决冷却物品）；RefractingLensListener 原本已有 ✓ |

## 核验安全

- 施法内部先结算后执行（round-2）+ 本轮否决短路——被否决交互零消耗零效果
- 经 PlayerRightClickEvent 的全部消耗物品均有自带领地校验（前轮闭环）
- 其余非交互类监听（方块/实体/世界事件）的取消语义均为只读或防御性取消，无消耗路径

## 验证

`JAVA_HOME=F:/Java/21 mvn -q package` 构建通过（真实退出码验证）。

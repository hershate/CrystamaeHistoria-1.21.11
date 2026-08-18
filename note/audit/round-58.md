# 审计第 58 轮：奇术盐未定项深探（五门条件逐一探针）

日期：2026-08-19
范围：对第 57 轮 ◐ 未定项（合成事件下 emptyBasin 未执行）的取因
深探——把监听器 `if` 链的全部五个条件在**同一运行时上下文**中逐一
直接探针。

## 探针结果（全部为真）

| 监听器条件 | 探针 | 结果 |
|------------|------|------|
| 主手材质 == REDSTONE | `getItemInMainHand().getType()` | ✅ REDSTONE |
| `getByItem(held)` instanceof ThaumaturgicSalt | `getByItem(...)` | ✅ CRY_THAUMATURGIC_SALT |
| `getClickedBlock() != null` 且动作正确 | 事件构造参数 | ✅ |
| `BlockStorage.check(block)` instanceof LiquefactionBasin | 直接调用 | ✅ CRY_LIQUEFACTION_BASIN_1 |
| `hasPermission(BREAK_BLOCK)` | 直接调用 | ✅ true |
| 监听器已注册 | ListenerManager:33 | ✅ |

**矛盾**：五门全真 + 监听器注册 + 会话零异常——事件后 `cancelled=true`
（某监听器确实取消）但 `emptyBasin` 未执行。对照同构的
`RefractingLensListener`（同事件/同方块/同主手注入模式）**完全成功**
（每类型一展示）。

## 定性（如实）

合成事件上下文中的未解行为：无法用五门条件解释，且无异常线索。
**维持非插件缺陷判定依据**：(a) 监听器代码与 r6 修复形态逐行一致；
(b) 所有条件真实玩家上下文下同样成立（材质/物品/权限均为服务端状态）；
(c) 同构监听器成功。剩余可能性集中于合成事件与 Bukkit 事件分发或
Slimefun 物品处理器之间的未知交互——超出驱动 harness 的分辨力。

**移交真人复核**（清单 4a，证据已随附）：真人持盐右击液化池，
一秒内可定案。

## 附注

本轮顺带发现驱动 `place` 对已注册位置的二次放置会被 Slimefun
`onBlockPlaceExisting` 取消并将方块置 AIR（幽灵方块模式，r36 家族）——
驱动使用注意已隐含于"place 前先确认未注册"惯例，特此明示。

## 验证

两轮会话均 RCON 优雅停服；world_r58 用毕删除；环境完全还原；
业务端口 25565 未触碰；会话零插件异常。

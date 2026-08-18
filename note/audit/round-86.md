# 审计第 86 轮：可取消事件状态变更矩阵（判定轮 2/2——收敛休眠第九次）

日期：2026-08-19
范围：全新探针角度——**互补于 r74**（r74 查"取消后的副作用"；本轮查
"无 `ignoreCancelled` 的可取消事件处理器是否变更状态"——即被他人
取消后仍会执行的变更面）。

## 矩阵结果与误报解剖

脚本标记 10 处，逐一人工复核后**全部为误报**，两类根因：

1. **脚本空格缺陷**（`'ignoreCancelled = true' in ann.replace(' ','')`
   自相矛盾——去空格后带空格模式永假）：CrystalBreakListener×2、
   SpellEffectListener 的 onFallingBlockLands/onRideRavager 实际
   **均带 `ignoreCancelled = true`**（源码直读确认）；
2. **不可取消事件误入清单**：`PlayerQuitEvent`/`PlayerBedLeaveEvent`/
   `EntityPortalEnterEvent` 在 Bukkit API 非 Cancellable——
   leaveSleepingBag/onQuitWithSleepingBag/portalDraining 的"无
   ignoreCancelled"无语义（属性只对可取消事件有效）；checkCooldown
   为红线 4 文档化例外（LOWEST 前置否决自身即取消者）。

**实质零发现**：全部真实可取消事件上的状态变更处理器都带
`ignoreCancelled` 或为豁免项——与 r71（交互族矩阵）+ r74（取消
时机）三面拼合成**完整的事件处理一致性立方体**。

## 判定：零发现（2/2）——循环收敛休眠（第九次）

r85（基类边界形态）+ r86（事件可取消性×状态变更形态）连续互异
角度零发现。**休眠待触发**：Paper/Slimefun 演进、玩法变更、负载
画像、用户指示（含真人复核清单执行）。

**方法论第四次印证**：脚本矩阵的两类缺陷（模式自相矛盾/事件清单
过宽）再次证明——零发现结论必须经人工复核后才可采信。

## 验证

纯代码判定轮（无服务器启动/无进程占用）；业务端口 25565 未触碰。

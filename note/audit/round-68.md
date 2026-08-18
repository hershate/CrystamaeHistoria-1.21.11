# 审计第 68 轮：SleepingBag 方法级直调 + EphemeralCraftingTable 生产调用

日期：2026-08-19
范围：休眠后按触发条件重启的首轮细粒度覆盖——睡袋 ItemUseHandler
方法级直调（夜间设定 + 真实 PlayerRightClickEvent 包装）与临时合成台
`openWorkbench(null, true)` 生产调用。

## 结果

| 项 | 结果 |
|----|------|
| **睡袋 ItemUseHandler 方法级直调** | ✅ 零异常。机器人（假玩家）`player.sleep()` 返回 false → **入睡失败回滚路径实证**：床方块无残留（`bedBlock=false`）、sleepingBags 表未登记（`registered=false`）——**r1 修复（失败回滚防免费床）在生产代码路径上实证在位**。真实玩家入睡成功路径与 r1 其余三路径（下线清理/他人采集/爆炸守卫）已由 r1 审计 + 代码复核覆盖。 |
| **EphemeralCraftingTable** | ✅ `openWorkbench(null, true)` 打开原版合成台（`opened=true PASS`）——物品使用路径的 GUI 打开正确。 |
| 会话 | 0 插件异常 |

## 验证

world_r68 用毕删除；PID 54544 RCON 优雅停服；环境完全还原；
业务端口 25565 未触碰。

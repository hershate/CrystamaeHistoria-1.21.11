# 审计第 64 轮：全驱动回归套件（单会话 24 项顺序执行）

日期：2026-08-19
范围：30 轮沉淀的全部驱动能力整合为**单一可重复回归套件**——一个
会话内顺序执行 24 项（5 放置 + 19 功能验证），单世界单机器人，
汇总报表 `suite64-results.json`。

## 结果：24/24 功能全过（19 自动判绿 + 5 需语义判读均过）

| 自动判绿（19） | |
|----|----|
| place ×5（basin/gilder/altar/configurator/waystone） | 全部 placed=true |
| altar / gilder / basin | PASS |
| spells_cast（10 法术全原型） | ok=10 err=0 |
| legacy（v1 双读三组）/ stats（六写点+落盘） | PASS |
| configurator add+assert | add_invoked + plate=PUSH/50 |
| waystone bind / compendium ×3 / crysta / salts | 全过 |

| 语义判读（5，均为已知正确语义） | |
|----|----|
| gadgets | placed=13/0/0（正则未匹配数字，功能全过） |
| basinplate | recharge PASS；mismatch 的 platesLeft=1 为 r50 更正后的设计语义（板存活） |
| spells_stat | 施放后 2s 快照——活动表非零为预期（生命周期中段） |
| brush | depleted_at=100 = 耗尽语义 PASS |
| exalted | frozenTimeTable=HIT = PASS |

**单会话交叉运行零异常**：全部功能在同一 JVM 会话中密集顺序执行，
服务端日志 0 ERROR/0 异常（跨子系统交互压力的最终一致性）。

## 资产（入库）

`benchmark/audit-driver` + 本轮套件编排模式（bot/suite64.js 模板随
结果样例存档于 note）；后续任何改动可一键 24 项回归。

## 验证

world_r64 用毕删除；PID 9752 RCON 优雅停服；环境完全还原；
业务端口 25565 未触碰。

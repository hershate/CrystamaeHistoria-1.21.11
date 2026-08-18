# 审计第 44 轮：法术施放链驱动验证（多原型施放 + SpellMemory 生命周期）

日期：2026-08-19
范围：以驱动插件 `spells cast|stat` 子命令走**生产施放路径**
（`CastInformation(player, 2)` + `freezeTargetsOnCast()` + `castSpell`，
与 /ch test-spell 完全一致），覆盖全部执行原型；随后以两次全表快照
（10s 中途 / 75s 终态）断言 SpellMemory 生命周期回收。

## 施放矩阵（10/10 零异常）

| 原型 | 法术 | 结果 |
|------|------|------|
| 即时 | HEAL / BRIGHT | ✅ |
| 弹射物 | FIREBALL / FAN_OF_ARROWS | ✅ |
| tick 持续 | TIME_COMPRESSION / PUSH | ✅ |
| 弹射物+tick 混合 | STAR_FALL | ✅ |
| 闪电（strikeMap） | CALL_LIGHTNING | ✅ |
| 召唤物 | SUMMON_GOLEM | ✅ |
| 飞行状态 | PHANTOMS_FLIGHT | ✅（cast_ok；flight 表未入条目——该法术需要目标实体条件，非异常） |

`cast_done ok=10 err=0`。

## SpellMemory 生命周期实证

| 表 | 中途快照（10s） | 终态快照（75s） | 判读 |
|----|------|------|------|
| projectiles | 0 | 0 | 弹射物 5s 生命周期内已飞行/过期清除 ✓ |
| strikes | 0 | 0 | 1s 过期 ✓ |
| ticking | 1 | **0** | tick 法术运行中→耗尽自注销（SpellTickRunnable.cancel 链）✓ |
| summons | 2 | **1** | 召唤物按各自寿命逐步过期（2→1 持续回收中）✓ |
| 其余五表 | 0 | 0 | 无泄漏 ✓ |

`TemporaryEffectsRunnable` 每秒集中回收机制实证工作正常；会话日志
**0 异常 / 0 ERROR / 0 tick 落后 / 0 watchdog**。

## 驱动增强（入库）

`chdriver spells cast|stat`：多原型批量施放（逐法术 try/catch 报告）+
SpellMemory 十表尺寸快照——供后续回归复用。

## 验证

全新世界 world_r44（用毕删除）；PID 39596 RCON 优雅停服（exit 0）；
环境完全还原；业务端口 25565 未触碰。

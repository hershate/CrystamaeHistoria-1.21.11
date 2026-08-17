# 性能优化第 62 轮：实体生成预配置 consumer 化（第十二轮循环开启）

日期：2026-08-17
域：**实体生成 API 形态**——探针角度（与 r42-61 互异）：spawn 后逐个
setter 配置 vs Paper 的 `spawn(loc, clazz, consumer)` / `dropItem(loc,
stack, consumer)` 预配置——consumer 在实体**加入世界/被跟踪前**应用
配置，生成包即携带终态；原形态生成包以默认态发出后再逐个补发实体
元数据同步包。

## 实现（本轮提交 f8e2deb）

| 站点 | 配置项 | 处理 |
|------|--------|------|
| `SpellUtils.summonMagicProjectile`（弹射物法术热路径） | shooter / bounce / fireball 双标志 | 全部入 consumer |
| `SpellUtils.summonTemporaryMob`（召唤法术） | PDC 属主标记 + 蛋名 | 入 consumer；**AI 目标（goal API）保守留在生成后**（时序语义不变） |
| `GeneralUtils.spawnDisplayItem`（机械展示架工作态切换） | PDC 标记/名称/名称可见/无重力/零速/禁玩家拾取/拾取延迟 **七项** | 全部入 consumer |

无 `EntityType.getEntityClass()` 映射的类型保留原路径回退（失败开放
双路）。`spawnFallingBlock` 无生成后配置，不属本族；`getById` 为
6 元素线性循环（~ns，不属优化域）。

## 量化（服务器内复合形态，含同侧 remove 抵消，round-62-server.tsv）

| 基准 | 旧（spawn 后配置） | 新（consumer 预配置） | 提升 |
|------|----|----|------|
| entitySpawn.projectile | 2584.32 ns | 1340.13 ns | **1.93x**（每次弹射物施法省 ~1.24µs） |
| entitySpawn.displayItem | 2780.46 ns | 1810.48 ns | **1.54x**（每次展示物生成省 ~0.97µs） |

等价性：两形态生成物终态逐项一致（bounce/shooter、名称可见/重力/
拾取延迟/禁拾取 + PDC 标记存在性）true。

**边界说明**：无观察者时包成本为 0，上述为纯服务端收益（元数据
脏标记与广播队列构建）；生产环境每跟踪玩家每生成另有元数据包
N→1 收益（弹射物 3-4 包→0、展示物 7 包→0），基准不可测（同 r57
口径，如实标注）。

## 会话记录

COMPLETE=1、CH 插件错误 0、watchdog 2 次（基准批次固有）。

## 判定

族矩阵增补行：**实体生成预配置（consumer 重载）——实质（服务端
1.5-1.9x + 未计元数据包 N→1 收益）**。弹射物路径为全部弹射物法术
共享（19+ 法术），展示物路径为五类机械展示架共享。第十二轮循环
开启（0.12.0 后用户再触发），下一轮判定轮换新角度。

# 性能优化第 2 轮：施法触发路径（懒 raycast + 冻结 + 事件顺序重排）

日期：2026-08-15
基准数据：[benchmark/results/round-2-server.tsv](../../../benchmark/results/round-2-server.tsv)（服务器内实测，Paper 1.21.11 + Slimefun 5.0.0）
红线核查：安全性 ✅ 稳定性 ✅（服务器回归通过，0 异常）兼容性 ✅（getter 签名不变，`setTargetedBlock*` 无调用方）

## 问题

`SpellCastListener` 在**任何前置校验之前**就付出两项固定成本：

1. `new CastInformation(...)` 构造器急切执行 **2 次 50 格 raycast**
   （`getTargetBlockExact(50)` + `getTargetBlockFace(50)`，各自内部独立 rayTrace）。
   空栏位/法术禁用/晶能不足/冷却中的交互全部白付。
2. `new InstanceStave(stack)`（法杖 PDC 反序列化）先于栏位解析执行——
   PHYSICAL 等未映射动作（踩踏压力板等）也白付整张法术板映射反序列化。

## 优化（3 处）

| # | 变更 | 文件 |
|---|------|------|
| 1 | `CastInformation` 视线目标改为**懒计算 + 单次 rayTraceBlocks(50) + 备忘录**：`getTargetedBlockOnCast/getTargetedBlockFaceOnCast` 首次读取时解析，一次 raycast 同时给出方块与命中面（两次减为一次，Paper 内部实现语义完全一致） | `CastInformation.java` |
| 2 | **冻结点**：`InstancePlate.tryCastSpell` 在前置校验全部通过后、`castSpell` 前 `freezeTargetsOnCast()`；`/ch test-spell` 路径同构冻结 | `InstancePlate.java`、`TestSpell.java` |
| 3 | 监听器顺序重排：栏位解析（无成本）→ null 早退 → PDC 反序列化 | `SpellCastListener.java` |

**语义安全论证（关键）**：`StripMine` 等 tick 型法术在**后续 tick** 读取
`getTargetedBlockOnCast()`（字段名义即"施法瞬间"）——纯懒加载会让跨 tick 读取
拿到玩家**当前**视线（语义漂移）或离线 null。冻结点设计保证：失败路径零 raycast，
成功路径在任何法术回调执行前已解析并恒定，与旧实现逐语义一致。

## 量化（服务器内真实世界实测）

| 场景 | 旧 ns/次施法交互 | 新 ns/次施法交互 | 说明 |
|------|------------------|------------------|------|
| 前置校验失败（空栏位/禁用/缺晶能/冷却） | ≥102.77（miss）/ ≥346.54（hit） | **0**（不 raycast） | 完全省去 |
| 成功施法（miss：50 格无命中） | 102.77 | 61.14 | **1.68x** |
| 成功施法（hit：5 格命中石墙） | 346.54 | 177.60 | **1.95x** |
| PHYSICAL 等未映射动作 | 5369.10（PDC 反序列化）+ 102.77 | **≈0**（栏位早退先于反序列化） | 完全省去 |

> 实测方法：服务器内基准插件（[benchmark/server-addon/](../../../benchmark/server-addon/)）
> 于真实主线程/真实世界中测量真实 LivingEntity API 与真实插件 PDC 反序列化代码，
> 时间驱动预热 300ms + 5 批中位数。

## 顺带发现（后续轮次储备）

`stavePdc.deserialize` 实测 **5369ns/次**（getItemMeta 克隆 + 4 板 PDC 反序列化），
是施法路径上最大的单项成本——候选方向：法杖实例缓存（需处理物品堆叠/移动失效）。

## 稳定性验证

Paper 1.21.11 build 132 + Slimefun 5.0.0 实机：插件启用正常，基准插件运行完毕
（含实体生成/清理与方块改写还原），**全会话 0 异常**。

## 变更文件

- [CastInformation.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/magic/CastInformation.java)
- [InstancePlate.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/magic/spells/core/InstancePlate.java)
- [SpellCastListener.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/listeners/SpellCastListener.java)
- [TestSpell.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/commands/TestSpell.java)
- benchmark/server-addon/（新增：服务器内基准插件 + build.sh）

# 性能优化第 10 轮：世界级高频事件路径 O(1) 化（第二轮循环开启）

日期：2026-08-16
基准数据：[benchmark/results/round-10-server.tsv](../../../benchmark/results/round-10-server.tsv)（含历轮全量 48 变体复测）
红线核查：安全性 ✅ 稳定性 ✅（实机跑分会话 0 插件异常、0 次 "Can't keep up"）兼容性 ✅（行为等价分析见下）

## 背景：第二轮循环的起点

第 9 轮收敛判定后，本轮以「完全重写授权」重新审视残余热路径，发现一个此前
未覆盖的域：**世界级高频事件**（world-scoped events）——`ProjectileHitEvent`
（任意原版箭矢/雪球/末影珍珠命中）、`EntityChangeBlockEvent`（任意沙砾落地/
羊吃草）、`EntityDamageEvent`（任意生物受伤）、`EntityDeathEvent`（刷怪塔高频）
对**全服所有实体**触发，与插件自身活跃度无关。原实现中四处监听器在这些事件上
无条件付出 stream 流水线分配或实体 PDC 读取成本。

## 本轮优化（commit 7551316）

### 1. 弹射物/下落方块反查：stream 线性扫描 → UUID 哈希索引（O(1)）

`SpellEffectListener.onProjectileHit`/`onFallingBlockLands` 原先对
`projectileMap`/`fallingBlockMap` 的 keySet 做 `stream().filter().findFirst()`——
空表时也要分配整条 Stream 流水线，表满时 O(n) 扫描。新增
`SpellMemory.projectileIndex`/`fallingBlockIndex`（UUID → wrapper），由
`registerProjectile`/`registerFallingBlock`/`unregister*` 与主表同步维护
（写入点收拢：`SpellUtils` 的 2 处 put 与 `MagicProjectile`/`MagicFallingBlock`
的 kill 自移除全部走注册 API），监听器改为单次哈希查询。

### 2. 无敌标记：实体 PDC 读写 → 会话内注册表

`PDC_IS_INVULNERABLE` 原先写入实体 NBT，且 `onInvulnerablePlayerDamaged` 每次
伤害事件读取。唯一写入方 Protectorate 的过期仅 **1050ms**（远短于任何重启耗时），
会话内存表语义等价：迁移为 `SpellMemory.invulnerableEntities`（UUID → 到期），
周期清扫挂入 `TemporaryEffectsRunnable`。收益为双边的：每次伤害事件的读路径、
Protectorate 每受保护实体每秒的写路径，且消除实体 NBT 残留键（旧实体上的
`invul` 键成为惰性数据，无人读取，无害）。

### 3. 召唤物门控：类型 EnumSet 前置过滤

`onMagicSummonDeath`/`onRideRavager` 原先每次死亡/方块变化事件无条件读实体
PDC（`PDC_IS_SPAWN_OWNER`）。新增 `SpellUtils.SUMMONABLE_MOB_TYPES`
（EnumSet，预置现存 12 个召唤法术的全部类型；`summonTemporaryMob` 运行期自动
登记新类型）：非候选类型（骷髅/牛/羊/苦力怕等）零成本跳过。

## 量化结果（Paper 1.21.11 b132 + Slimefun 5.0.0 实机，同 JVM 对比）

| 场景（触发频率） | 旧 ns/次 | 新 ns/次 | 加速比 |
|------|----------|----------|--------|
| 弹射物命中反查·空表（常态：无魔法弹射物） | 69.63 | 3.48 | **20.0x** |
| 弹射物命中反查·满表 8 条目未命中 | 189.39 | 4.92 | **38.5x** |
| 下落方块落地反查·空表 | 69.42 | 3.58 | **19.4x** |
| 无敌检查（每次任意实体受伤） | 14.54 | 3.35 | **4.3x** |
| 无敌写入（Protectorate 每实体每秒） | 58.70 | 10.29 | **5.7x** |
| 召唤物门控（非候选类型常态，如骷髅刷怪塔） | 16.86 | 3.34 | **5.0x** |
| 召唤物门控（白名单类型复合路径，如僵尸） | 16.86 | 14.98 | 1.1x（如预期） |

> 注：旧变体数值为热循环下界（JIT 充分编译、分配被摊销）；真实事件路径频率
> 约百次/秒级且多为温态，实际收益不低于表值。

## 兼容性分析（红线）

1. **索引与主表同生同灭**：put/kill/clearAll 全部收拢到注册 API；`clearAll` 追加
   索引清空。公共 API `getProjectileMap()`/`getFallingBlockMap()` 签名不变
   （`MagicProjectile.matches` 保留）。
2. **无敌语义等价**：1050ms 过期 ≪ 任何重启/关停耗时，会话内丢失场景不存在；
   关服时随 `clearAll` 结束，与其它临时效果（飞行/时间/天气）一致。
3. **召唤物残留覆盖**：预置 12 类型保证崩溃重启后仅存 PDC 标记的残留召唤物仍被
   门控捕获（行为与旧版逐字节一致）；白名单类型（含僵尸）门控通过后仍读 PDC
   确认，无误杀。未来新增召唤类型由 `summonTemporaryMob` 自动登记。
4. `EntityChangeBlockEvent` 的 `onFallingBlockLands` 保持 `instanceof FallingBlock`
   前置，下落方块之外的实体方块变化行为不变。

## 验证

- `JAVA_HOME=F:/Java/21 mvn package` 构建通过；
- 实机跑分会话（启动 + 全量 48 变体基准 + 优雅关停）：**0 插件异常、
  0 次 "Can't keep up"**，CHPerfBench 正常完成；历轮 35 个基准变体复测数值
  与 round-9 同量级（无回归）。

## 变更文件

- [SpellMemory.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/SpellMemory.java)（双索引 + 无敌注册表 + 注册 API）
- [SpellEffectListener.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/listeners/SpellEffectListener.java)（4 处处理器重写）
- [SpellUtils.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/utils/SpellUtils.java)（注册 API 接管 + 类型门控白名单）
- [MagicProjectile.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/magic/spells/spellobjects/MagicProjectile.java) /
  [MagicFallingBlock.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/magic/spells/spellobjects/MagicFallingBlock.java)（kill 走注销）
- [Protectorate.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/magic/spells/tier1/Protectorate.java)（写路径迁移）
- [TemporaryEffectsRunnable.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/runnables/TemporaryEffectsRunnable.java)（清扫链 +1）
- [Keys.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/utils/Keys.java)（移除 invul 常量）
- benchmark/server-addon（benchRound10，13 变体）

## 下一轮候选

- 启动路径（16.5s 服务端启动中插件 onEnable 占比分解）
- `ParticleDisplayRunnable`（每 4s 每玩家 ±5 格 1331 方块扫描）
- `LiquefactionBasinCache` 每 tick 展示架混色/液位路径

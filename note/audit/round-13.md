# 审计第 13 轮：剩余 tier1 法术逐文件精读（第二批收官）

日期：2026-08-15
范围：`magic/spells/tier1/` 全部 67 类——精读 20+ 个重点类，其余 30+ 个经危险模式全量扫描（getTargetBlockExact/getCasterAsPlayer/setSize/teleport/inventory/createExplosion/strikeLightning 等）后逐个核验命中

## 已修复（1 个 commit）

| commit | 问题 |
|--------|------|
| `6aaf782` | **召唤物 tick 消费者离线 NPE 中断清理链**（round-1 遗留盲区：只覆盖了 SpellTickRunnable）：SummonGolem/LeechBomb 的 onTick 链式 `getPlayer().getLocation()`——主人下线后 NPE 抛进 TemporaryEffectsRunnable，**每秒中断整个法术清理链**（弹射物/临时方块/召唤物全部停止回收，泄漏级联）。修复两层：`removeEntities` 主人离线即清理（与 mobgoals 离线自毁语义一致）+ 法术侧纵深兜底；`MagicSummon.run()` 加断路器。**GrowUp 尺寸钳制**：slime/phantom setSize 无上限，多次施法触发原版 API 越界异常 |

## 核验安全（记录依据）

- Nova 系×5/FanOfArrows：弹射物伤害全部 UUID 权限路径；Fireball 系经 SpellUtils 防火（isIncendiary=false/yield=0）✓
- BloodMagics：递归传播 ≤5 层有界，伤害走权限；Cascada：方块扬起逐块 BREAK_BLOCK 校验 ✓
- AbstractVoid：随机换位有 INTERACT_ENTITY 校验；Tracer：tick 消费者（离线守卫覆盖）✓
- Teleport/KnowledgeShare：判空完备，经验消耗有界（min 当前总量）✓
- WitherWeather：即时施法在线保证；SpawnFiends 固定 size=2 ✓
- Tempest/CallLightning 的 strikeLightning 原版伤害：上游设计（法术=真实闪电），保护由服务器侧闪电保护插件接管——记录不改

## 记录（不改）

1. Nova 系每次施法生成 ~120 弹射物实体（step=3°）——上游设计；高负载服务器可关注。
2. Tempest 的 beforeProjectileHit setFireTicks 无权限校验（与 CallLightning 不一致）——着火 2s 无伤害，影响极小。
3. KnowledgeShare 上游 TODO 注释保留。

## 验证

`JAVA_HOME=F:/Java/21 mvn -q package` 构建通过。

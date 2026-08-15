# 审计第 5 轮：法术系统安全

日期：2026-08-15
范围：`magic/`（SpellMemory、MagicProjectile、SpellUtils、DisplayItem、CastInformation）+ `magic/spells/tier1/*` 按执行上下文（即时/tick/弹射物命中/闪电命中）分类核查 + `utils/GeneralUtils` 推拉与伤害/破坏权限闭合

## 执行上下文安全基线（本轮确立）

- **即时法术**：施法时玩家必然在线，`getCasterAsPlayer()` 安全。
- **tick 法术**：第 1 轮 `SpellTickRunnable` 已加离线终止 + 断路器。
- **命中回调（弹射物/闪电/落块）**：可晚于施法者下线（弹射物寿命 5s、流星下落数秒）——本轮重点。

## 已修复（3 个 commit）

| commit | 问题 |
|--------|------|
| `2693226` | **功能缺陷（上游遗留）**：`summonMagicProjectile` 的 tick 消费者框架存在，StarFall/Chaos/Hellscape 均注册了拖尾粒子回调，但全库无调用方执行 `MagicProjectile.run()`（对比召唤物路径每秒驱动 `magicSummon.run()`）——弹射物周期效果从未生效。现 `removeProjectiles` 与召唤物同构驱动：未过期驱动 run()、实体消失 kill 清理、消费者异常断路器停用 |
| `3be2e43` | **round-1 遗留项闭合**：4 个命中回调的离线施法者 NPE（异常穿透 ProjectileHit/LightningStrike/EntityChangeBlock 事件链）——Hellscape 击退源降级命中位置；PlutosDecent 离线改无源爆炸重载；CallLightning/AntiPrism 权限改 UUID 重载 |
| `39506dc` | Break/PhilosophersStone 视线无方块时 `getTargetBlockExact` 返回 null 的链式 NPE（此前表现为静默失败+白扣充能+刷日志）；贤者之石 block_colors.yml 缺项 NPE → Number 安全读取+白色回退；GeneralUtils 三处归一化零向量 NaN 速度守卫 |

## 核验安全（记录依据）

- `GeneralUtils.damageEntity/tryBreakBlock/hasPermission`：全部经 Slimefun ProtectionManager；ATTACK_PLAYER 附加世界 PVP 校验；破坏跳过 BlockStorage 机械块/TileState/硬度 -1。领地闭合良好。
- Bright/EndermansVeil/Chaos(cast)/Heal 等即时路径已自带判空或时序上必然在线。
- AntiPrism 的 PRISM/ANTIPRISM PDC 标记、WitherWeather 的掉落保底等随法术逻辑无数据风险。

## 遗留观察

1. **玩法层面（不改）**：PlutosDecent 流星落地爆炸 `breakBlocks=true`，爆炸破坏不受领地保护约束——与上游设计一致（法术本身的破坏性效果），如需领地安全应由服务器侧爆炸保护插件接管。
2. `CastInformation` 构造时的 50 格 raycast（targetedBlockOnCast/Face）无任何消费方——每次施法的无效开销，暂不动（行为无关，性能微优化，留给后续轮次评估）。
3. 死代码 TunnelBore/TunnelBoreRunnable 维持第 1 轮记录。

## 验证

`JAVA_HOME=F:/Java/21 mvn -q package` 构建通过。

# 审计索引（/loop 持续审计）

目标：逻辑闭合、代码安全、可长期高负载多用户运行；不信任任何客户端输入。
每轮覆盖不同方面，轮次记录见 `round-<N>.md`。

## 轮次计划与状态

| 轮次 | 方面 | 状态 |
|------|------|------|
| 1 | 常驻定时器 + SpellMemory 生命周期 + 施法者离线路径（[round-1](round-1.md)） | ✅ 完成（8 项修复，4 commit：7133f1a/8d41424/c0d3b75/9d06e03） |
| 2 | 机械缓存数据操作（液化池/现实祭坛/记录者面板/镀金器/法杖配置器）：复制/吞没/竞态 | 待做 |
| 3 | GUI 交互安全（菜单点击/shift 点击/光标物品/越权槽位） | 待做 |
| 4 | PDC 反序列化与不可信数据（物品/实体/区块 PDC，NaN/负值/缺键） | 待做 |
| 5 | 法术系统安全（施法校验、冷却、充能扣除原子性、AOE 边界） | 待做 |
| 6 | 监听器闭合（故事方块禁放置/禁合成、冷却拦截、掉落物处理） | 待做 |
| 7 | 无界集合与内存增长（高负载长跑）、异常吞没、日志风暴 | 待做 |
| 8 | 功能与表述一致性（README/note 声明 vs 实际实现） | 待做 |

## 已修复问题汇总

| 轮次 | 文件 | 问题 | 修复 | commit |
|------|------|------|------|--------|
| 1 | `runnables/ParticleDisplayRunnable.java` | 循环误用 return 提前终止 | 改 continue | `7133f1a` |
| 1 | 12 文件（listener/core/tier1×10） | 13 处 UUID `==`/`!=` 引用比较，自我豁免失效 | 改 `.equals()` | `8d41424` |
| 1 | `magic/spells/tier1/ChillWind.java` | negative 效果调用 applyPositiveEffects，效果永不生效 | 改 applyNegativeEffects | `8d41424` |
| 1 | `SpellMemory.java` | strikeMap 无过期清理；离线条目不删；removeBlocks 卸载世界抛异常中断全部清理；clearAll 遗漏 strikeMap | removeStrikes + 离线移除 + 异常守卫 + clearAll 补齐 | `c0d3b75` |
| 1 | `runnables/spells/SpellTickRunnable.java` | 施法者离线 tick 法术 NPE；异常每 tick 重刷 | 离线终止 + 断路器 | `c0d3b75` |
| 1 | `slimefun/items/tools/SleepingBag.java`、`listeners/MiscListener.java` | 睡袋刷床复制（下线残留/他人采集/爆炸掉落/sleep 失败不回滚） | 退出兜底清理 + 挖掘/爆炸守卫 + 回滚 | `9d06e03` |

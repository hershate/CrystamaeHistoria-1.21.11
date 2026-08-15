# 审计索引（/loop 持续审计）

目标：逻辑闭合、代码安全、可长期高负载多用户运行；不信任任何客户端输入。
每轮覆盖不同方面，轮次记录见 `round-<N>.md`。

## 轮次计划与状态

| 轮次 | 方面 | 状态 |
|------|------|------|
| 1 | 常驻定时器 + SpellMemory 生命周期 + 施法者离线路径（[round-1](round-1.md)） | ✅ 完成（8 项修复，4 commit：7133f1a/8d41424/c0d3b75/9d06e03） |
| 2 | 机械缓存数据操作（液化池/现实祭坛/记录者面板/镀金器/法杖配置器）（[round-2](round-2.md)） | ✅ 完成（10 项修复，4 commit：7d7d148/d081833/4b99b32/9d9849e） |
| 3 | GUI 交互安全 + 图鉴表述一致性（[round-3](round-3.md)） | ✅ 完成（全部 GUI 面核验安全；9 项修复，2 commit：b238d99/f0be106） |
| 4 | PDC 反序列化与不可信数据（[round-4](round-4.md)） | ✅ 完成（LOCATION 反序列化白名单等 3 commit：9db17c0/52e6e3c/1746e4c） |
| 5 | 法术系统安全（[round-5](round-5.md)） | ✅ 完成（弹射物消费者驱动缺失等 3 commit：2693226/3be2e43/39506dc；round-1 遗留项闭合） |
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
| 2 | `mechanisms/DisplayStandHolder.java` | kill 时序漏洞致展示架实体永久泄漏；损坏 UUID/消失实体 NPE | 先取架再清信息；findDisplayStand 防御 + 缺失重建 | `7d7d148` |
| 2 | `liquefactionbasin/*` | 持久化数据损坏致机械永久失效；**玩家条件绕过尊贵物品进度门槛** | 防御解析 + 失败关闭 | `d081833` |
| 2 | `chroniclerpanel/*`、`realisationaltar/*`、`prismaticgilder/*` | 损坏数据机械失效、T5 面板 pushOut NPE、onBreak NPE 吞输入物品、祭坛状态不一致 NPE 链 | 全面空值/异常守卫 | `4b99b32` |
| 2 | `staveconfigurator/*`、`InstancePlate.java`、`SpellCastListener.java` | 作弊充能板 EnumMap NPE；施法先执行后扣费可零成本重试；副手事件覆盖成功提示 | 退还无效板 + 先结算后施法断路器 + 忽略副手 | `9d9849e` |
| 3 | `itemgroups/SpellCollectionFlexGroup.java` | 法术集图鉴 7 处表述与实现不符（缩放标志/治疗量/范围判断/弹射物击退全部误读字段） | 逐一改为正确字段 | `b238d99` |
| 3 | `tools/satchel/SatchelInstance.java` | PDC 反序列化数组无校验（长度/负值）→ 负库存污染；removeAmount 无下界 | 长度 9 校验 + 负值钳 0 | `f0be106` |
| 4 | `utils/datatypes/LocationDataType.java` | **物品 PDC 无过滤 Java 反序列化（RCE 攻击面）** | resolveClass 类白名单（编码不变） | `9db17c0` |
| 4 | `utils/datatypes/DoubleArrayDataType.java` | 长度字段无校验 → 负长度崩溃/超大长度 OOM | 长度与字节量一致性校验 | `9db17c0` |
| 4 | `utils/datatypes/Persistent{Plate,Stave,Stories,StoryChunk,SatchelInstance,Pose}*` | 缺键拆箱 NPE、非法值 IAE、null 故事入列表连锁 NPE | 失败关闭/坏条目跳过/保守默认 | `52e6e3c` |
| 4 | `DataTypeMethods.java` + 6 个读取方 | 类型错配 IAE 穿透全部调用方；回忆水晶格 world==null NPE | 断路器 + 逐点降级处理 | `1746e4c` |
| 5 | `SpellMemory.java`、`MagicProjectile.java` | **弹射物 tick 消费者从未被驱动**（StarFall/Chaos/Hellscape 拖尾效果缺失，上游遗留） | removeProjectiles 同构驱动 + 消失清理 + 断路器 | `2693226` |
| 5 | `tier1/{Hellscape,PlutosDecent,CallLightning,AntiPrism}.java` | 命中回调离线施法者 NPE 穿透事件链（round-1 遗留） | 位置降级/无源爆炸/UUID 权限 | `3be2e43` |
| 5 | `tier1/{Break,PhilosophersStone}.java`、`utils/GeneralUtils.java` | 视线无方块 NPE；颜色表缺项 NPE；零向量归一化 NaN | 空值守卫 + Number 安全读取 + 零长跳过 | `39506dc` |

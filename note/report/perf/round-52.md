# 性能优化第 52 轮：判定轮——调度分配/异常构造/时钟读探针（零发现，循环收敛）

日期：2026-08-17
性质：判定轮（收敛计数 2/2）。探针角度与 r48-r51 互异：**tick 路径
调度器/lambda 分配、预期流中的异常构造、同方法重复时钟读**。

## 探针结果（全库 grep + 逐点复核，全部零命中）

1. **调度器调用**：`runTaskTimer`/`runTaskLater`/`getScheduler` 全部
   位于施法级（registerTicker 每次施法 1 个）、生命周期级
   （RunnableManager 启动、FloatingHeadAnimation 工作态切换、
   SupportedPluginManager 延迟检测 1 tick）——**无每 tick 分配点**；
   `forEach(` lambda 在 tick 类零命中；
2. **异常构造**：`new IllegalStateException("Unexpected value: ...")`
   全部位于 switch default 分支（真异常路径）；数据损坏防御分支
   （PersistentPlateDataType 等）按设计失败关闭——**预期流中零异常
   构造**（异常仅在异常路径付出代价）；
3. **重复时钟读**：SpellMemory 12 处 `System.currentTimeMillis()`
   分散在 11 个 remove 方法（每秒各 1 次由 TemporaryEffectsRunnable
   驱动）——合计 ~220ns/s，**低于任何可测阈值**；集中传时戳需重构
   11 个内部方法签名，收益为零，判定不做（边界备注）。

## 收敛宣告（r44 准则达成）

- **连续两轮零发现判定轮**（r51 嵌套/查找/复制角度 + r52 调度/异常/
  时钟角度），且角度互异（r49 方法论要求满足）；
- 复查节奏全程（r42-r52）：实质优化 4 轮（44/45/46/49）、阴性边界
  确认 1 轮（50）、卫生 1 轮（47）、判定轮 5 轮（42/43/48/51/52）；
  地板判定修订史（33→34、43→44、48→49）与最终收敛共同表明：
  探针角度轮换 × 持续小步清扫是长尾域的正确终止协议；
- **性能优化循环收敛**，版本收口 0.10.0（全量终验 + soak 见
  release/0.10.0.md）；重启触发条件不变：Paper/Slimefun 演进、
  玩法变更、集成环境实测画像。

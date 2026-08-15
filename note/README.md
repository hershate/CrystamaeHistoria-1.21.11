# note

本目录存放项目要点文档。

## 版本发布记录

- [0.1.0](release/0.1.0.md)：**版本序列自 0.1.0 起算**。28 轮系统审计修订版（117+ 项
  稳定性/安全性/正确性修复，8 次服务器回归验证，详见 [audit/](audit/README.md)）；
  无数据格式变更，旧存档兼容。
- [1.21.11-1](release/1.21.11-1.md)（历史）：迁移至 Paper 1.21.11 + Slimefun 5.0.0（REF/Slimefun4.1），
  移除 bstats / GuizhanLibPlugin / InfinityLib / EffectLib / MorePersistentDataTypes 及全部第三方可选插件集成。

## 专项分析

- [法术系统结构化分析](spell-system-analysis.md)：`magic/`、`listeners/`、`runnables/` 包与 `SpellMemory.java` 的核心抽象、注册/触发/执行机制、69 个法术的数量与层级划分、监听器与定时任务职责、法术执行流程调用链（2026-08-15）。

## 性能优化轮次

持续性能优化（红线：安全/稳定/兼容，量化见 [benchmark/](../benchmark/)，
报告与索引见 [report/perf/](report/perf/README.md)）：
第 1-6 轮完成（施法前置校验 29x；SpellMemory 零复制 8x；施法触发懒 raycast；
交互路径 ItemMeta 削减 8.7x；机械 tick 判定备忘录 1034x；法杖单槽 PDC 读取 1.5x，服务器回归均通过）。

## 维护要点（改代码前必读）

1. **构建**：`mvn package`（需 Java 21）。Slimefun 依赖来自本地仓库的
   `com.github.slimefun:Slimefun:5.0.0`（由 `REF/Slimefun4.1/target/SlimeFun4.1-5.0.0.jar` 安装：
   `mvn install:install-file -Dfile=... -DgroupId=com.github.slimefun -DartifactId=Slimefun -Dversion=5.0.0 -Dpackaging=jar`）。
2. **SlimefunItemStack ≠ ItemStack**：Slimefun 5 中两者已分离，需要 `ItemStack` 时用 `.item()` 转换；
   `asQuantity(int)` 返回 `ItemStack`；物品注册、`RecipeType` 等上下文仍要求 `SlimefunItemStack`。
3. **机械基类**：本地 `slimefun/machines/MenuBlock`、`TickingMenuBlock`（等价移植自 InfinityLib），
   新增机械照抄 `ChroniclerPanel`/`RealisationAltar` 骨架。
4. **命令**：本地 `commands/SubCommand` + `HistoriaCommand`；新增子命令在主类 `setupCommands()` 挂载。
5. **PDC 数据类型**：`utils/datatypes/DataType`（BOOLEAN/DOUBLE_ARRAY/INTEGER_ARRAY/LOCATION），
   编码与原 MorePersistentDataTypes 一致，勿改编码格式（涉及历史数据兼容）。
6. **依赖红线**：`depend` 仅 Slimefun；运行时核心只依赖 paper-api（1.21.11）与 Slimefun 5.0.0。
   `softdepend` 仅允许 Slimefun 附属插件（ExoticGarden/Networks/Netheopoiesis/SlimeTinker/HeadLimiter），
   且所有附属集成必须有运行时守卫（参见 `SupportedPluginManager`），保证未安装时行为不变。
7. **验证环境**：`F:/paper-test-1.21.11` 存有 Paper 1.21.11 build 132 测试服务端
   （plugins 内已放 Slimefun 5.0.0 与本插件），可直接启动回归。
8. **不可信输入红线**（28 轮审计沉淀，改代码前必读，详见 [audit/](audit/README.md)）：
   - 物品/实体/区块 PDC 与 BlockStorage 一律视为不可信（改造客户端可注入任意 NBT）——
     解析必须失败关闭（拒绝/跳过/保守默认），禁止裸 `valueOf`/`fromString`/拆箱；
   - 事件回调（弹射物/闪电/落块命中、tick、召唤物周期）可晚于施法者下线——
     禁止链式 `getCasterAsPlayer().xxx`，用 UUID 重载或降级路径；
   - 周期回调必须带断路器（异常即停用/终止该次效果 + 限流日志），防日志风暴；
   - 直接监听 PlayerInteract 系事件须 `ignoreCancelled = true`
     （checkCooldown 例外——LOWEST 前置否决）；
   - 施法/消耗类操作先结算后执行（扣费在前，效果在后）；
   - Location 键缓存（cacheMap 等）必须在 onBreak 清理；共享状态禁止放
     SlimefunItem 实例字段（单例多方块污染）。

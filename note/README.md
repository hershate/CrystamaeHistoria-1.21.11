# note

本目录存放项目要点文档。

## 版本发布记录

- [0.2.0](release/0.2.0.md)（当前）：性能优化版——9 轮性能优化循环
  （量化对比见 [report/perf/](report/perf/README.md)，基准设施 [benchmark/](../benchmark/)）；
  无数据格式变更，旧存档兼容。
- [0.1.0](release/0.1.0.md)：**版本序列自 0.1.0 起算**。28 轮系统审计修订版（117+ 项
  稳定性/安全性/正确性修复，8 次服务器回归验证，详见 [audit/](audit/README.md)）；
  无数据格式变更，旧存档兼容。
- [1.21.11-1](release/1.21.11-1.md)（历史）：迁移至 Paper 1.21.11 + Slimefun 5.0.0（REF/Slimefun4.1），
  移除 bstats / GuizhanLibPlugin / InfinityLib / EffectLib / MorePersistentDataTypes 及全部第三方可选插件集成。

## 专项分析

- [代码库全量分析](analysis/2026-08-16/index.md)：项目根完整分析（264 个 Java 文件，v0.2.0）——
  5 层事件驱动架构、运行原理（施法/故事管线/SpellMemory 状态管理）、工作流、AI 替代方案评分，
  附 4 份 Skill Blueprint；索引见 [analysis/](analysis/README.md)（2026-08-16）。
- [法术系统结构化分析](spell-system-analysis.md)：`magic/`、`listeners/`、`runnables/` 包与 `SpellMemory.java` 的核心抽象、注册/触发/执行机制、69 个法术的数量与层级划分、监听器与定时任务职责、法术执行流程调用链（2026-08-15）。

## 性能优化轮次

持续性能优化（红线：安全/稳定/兼容，量化见 [benchmark/](../benchmark/)，
报告与索引见 [report/perf/](report/perf/README.md)）：
第一轮第 1-9 轮完成并**闭合**（收敛判定见 [round-9](report/perf/round-9.md)）——
施法前置校验 29x；SpellMemory 零复制 8x；施法触发懒 raycast；交互路径 ItemMeta
削减 8.9x；机械 tick 判定备忘录 1010x；法杖单槽 PDC 读取 1.6x；gadgets 每 tick
清扫 2.0-5.9x；故事选取索引 21x + 配置双解析消除 2.3x；统计路径 12.4x；
v0.2.0 全套基准 + 10 分钟 soak 终验 0 异常 0 tick 落后。
**第二轮循环**（完全重写授权）自 2026-08-16 起进行中：第 10 轮世界级高频事件
路径 O(1) 化（弹射物/下落方块反查 20-38.5x、无敌内存注册表 4.3-5.7x、
召唤物类型门控 5.0x）；第 11 轮液化池路径全套（syncBlock 脏标记 431x、
配方索引 140x、top-3 单遍 7.5x）；第 12 轮启动路径（稳态零配置落盘 16x、
首启批量补键 63.8x、分段计时画像）；第 13 轮剩余全局监听器门控审计收官
（isStoried 元克隆门控 5.8x ×4 处）。见 [round-10](report/perf/round-10.md)、
[round-11](report/perf/round-11.md)、[round-12](report/perf/round-12.md)、
[round-13](report/perf/round-13.md)。

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

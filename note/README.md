# note

本目录存放项目要点文档。

## 版本发布记录

- [1.21.11-2](release/1.21.11-2.md)：8 轮系统审计修订版（70+ 项稳定性/安全性/正确性修复，
  详见 [audit/](audit/README.md)）；无数据格式变更，旧存档兼容。
- [1.21.11-1](release/1.21.11-1.md)：迁移至 Paper 1.21.11 + Slimefun 5.0.0（REF/Slimefun4.1），
  移除 bstats / GuizhanLibPlugin / InfinityLib / EffectLib / MorePersistentDataTypes 及全部第三方可选插件集成。

## 专项分析

- [法术系统结构化分析](spell-system-analysis.md)：`magic/`、`listeners/`、`runnables/` 包与 `SpellMemory.java` 的核心抽象、注册/触发/执行机制、69 个法术的数量与层级划分、监听器与定时任务职责、法术执行流程调用链（2026-08-15）。

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

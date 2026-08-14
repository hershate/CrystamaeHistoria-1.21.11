# CrystamaeHistoria（魔法水晶编年史）代码分析报告

**分析时间**：2026-08-15_011442
**分析范围**：`/f/Github/repo/CrystamaeHistoria-1.21.11`（当前仓库根目录）
**分析模式**：完整分析（中型项目）
**代码规模**：255 个 Java 文件 / ~28,497 行 + ~21,500 行 YAML 数据
**技术栈**：Java 11 · Maven（shade）· Paper API 1.19 · Slimefun4（SlimefunGuguProject 2024.3.1）· InfinityLib · EffectLib · bStats · GuizhanLibPlugin

## 报告目录

| 报告 | 内容概要 | 篇幅 |
|------|---------|------|
| [项目架构](01-architecture.md) | 分层结构、包依赖图、7 种架构模式、3 条函数级调用链、健康度评估 | 约 7 节 |
| [运行原理](02-operation-principles.md) | 10 步启动序列、4 条核心数据流（变量级）、SpellMemory 13 状态表、3 个状态机、错误处理体系 | 约 6 节 |
| [工作流分析](03-workflow.md) | CI 管线（仅构建）、零测试现状、4 个业务/开发流程、2 棵决策树、异常恢复路径 | 约 4 节 |
| [AI 替代方案](04-ai-substitution.md) | 7 个模块 6 维评分、ROI 矩阵、三阶段路线图 | 约 5 节 |
| [Skill Blueprint 索引](blueprints/index.md) | 5 个可 AI 替代组件的完整 Skill 设计规格 | 5 Blueprint |

## 核心发现

1. **清晰的"故事→水晶→法术"游戏流水线架构**：5 个机械方块（记录者面板/现实祭坛/液化池/法杖配置器/镀金器）全部采用 `TickingMenuBlock + 伴随 Cache + static Map<Location, Cache>` 的同构模式（如 `ChroniclerPanel.java:32, 99-104`），扩展新机械有固定模板可循。
2. **法术系统是完全声明式的**：70 个法术全部由 `SpellCoreBuilder` 链式 API 描述（冷却/射程/消耗/三种执行路径/回调），`Spell.castSpell()`（`Spell.java:87-108`）按标志分派——这使得"新增法术"成为高度模板化、适合 AI 生成的任务。
3. **零自动化测试 + 仅构建的 CI**：全仓库无 `src/test/`，CI 只有 `mvn package`（`.github/workflows/maven.yml`）；版本兼容缺陷（issue #121、#122）全部在生产服务器暴露后修复，且历史上有一次失败的兼容提交被 revert（`4dc4783` → `74f05dd`）。
4. **持久化四通道但无数据库**：YAML 配置（含启动期默认值合并）、Slimefun BlockStorage、Bukkit PDC（物品/实体/区块，8 个自定义 PersistentDataType）、Slimefun 研究系统；玩家统计每 10 分钟落盘一次（崩溃可丢最近 10 分钟数据）。
5. **三层 fork 血缘决定维护模式**：Sefiraat 上游 → SlimefunGuguProject 汉化（pull[bot] 自动同步，127 个 merge 提交）→ 本地 1.21.11 适配工作区；本地化翻译与版本适配是本 fork 的两大高频开发活动。

## 关键建议

1. **立即落地 3 个 Quick Win Skill**：方块故事数据生成（28/30）、汉化翻译流水线（26/30）、新法术创作（25/30）——三者合计覆盖本项目最高频的重复性开发活动，Blueprint 已备好（见 [blueprints/](blueprints/index.md)）。
2. **补建最小测试脚手架**：pom.xml 引入 JUnit 5 + surefire，先覆盖 `StoryChances`（累积概率语义易错）、`RecipeSpell.recipeMatches` 等纯逻辑，随后在 CI 中 `mvn package` 前加 `mvn test`，把版本兼容缺陷拦截在构建期。
3. **修复两处已识别的潜在缺陷**：`ChroniclerPanel.onBreak()` 缺少 Cache 判空（`ChroniclerPanel.java:92-93`，对照 `RealisationAltar.java:113-116` 的正确写法）；`SpellMemory.removeEntities()` 对 null 过期值无防护（`SpellMemory.java:130`）。
4. **统一版本管理语义**：`pom.xml` 的 `MODIFIED` 版本与 CONTRIBUTING.md 声称的 SemVer 不符，建议在 note 中记录本 fork 的版本策略（构建号制）避免混淆。
5. **InfinityLib 退役规划**：`pom.xml:216` 已标注"To be removed"，但项目仍依赖其 `AbstractAddon`/`TickingMenuBlock`/`SubCommand` 三大基类；建议作为独立迁移任务跟踪，勿与功能性变更混合提交。

---

*本报告由 codebase-analyzer 静态分析生成，全部论断附有文件路径:行号证据；未执行任何项目运行（仅静态分析）。*

# CrystamaeHistoria 代码分析报告

**分析时间**：2026-08-16 13:19
**分析范围**：`f:\Github\repo\CrystamaeHistoria-1.21.11`（项目根，完整分析）
**分析模式**：完整分析（264 个 Java 文件 / ~33,605 行）
**技术栈**：Java 21 · Maven · Paper 1.21.11 · Slimefun 5.0.0（零第三方运行时依赖）

## 报告目录

| 报告 | 内容概要 | 篇幅 |
|------|---------|------|
| [项目架构](01-architecture.md) | 5 层分层结构、包依赖图、法术系统类图、9 类架构模式判定、性能工程模式清单 | 长（10+ 图表） |
| [运行原理](02-operation-principles.md) | 启动序列、施法数据流（变量级）、故事生产管线、SpellMemory 13 表状态管理、3 个状态机、三层错误防御体系 | 长（7+ 图表） |
| [工作流分析](03-workflow.md) | CI 管线、审计 33 轮/性能 9 轮/发布三条工作流、玩家业务旅程、决策树与量化业务规则 | 中（4+ 图表） |
| [AI 替代方案](04-ai-substitution.md) | 9 模块 6 维评分、函数级契约提取、ROI 矩阵、三阶段改造路线图 | 中 |
| [Skill Blueprint 索引](blueprints/index.md) | 4 份可直接用于 skill-for-skills 的完整设计规格 | 4 Blueprint |

## 核心发现

1. **声明式法术范式是最大架构资产**：69 个法术共享 `SpellCoreBuilder` 流式构建 + `Consumer<CastInformation>` 策略槽的同一模板（`Spell.java:87-108` 分派骨架），新增法术的实现成本极低且被断路器隔离（`InstancePlate.java:99-108`）——这是 AI 替代可行性的结构性基础。
2. **统一时间戳过期模型**：13 张 SpellMemory 表 + 法术板冷却 + gadget 过期全部采用 `currentTimeMillis` 绝对到期 + 每秒集中扫描（`TemporaryEffectsRunnable.java:9-24`），配合"先收集后执行"的零复制扫描与机械判定备忘录（`ChroniclerPanelCache.java:55-59`），支撑了 0.2.0 的 9 轮性能优化（最高 1034x）。
3. **防御式解析体系完整**：所有 PDC/BlockStorage/YAML 不可信输入均有 try-catch 降级路径（02 报告 §5.1 六处实证），错误从不穿透主循环——这是无测试项目维持 8 次服务器回归零异常的关键。
4. **质量流程文档化但无自动化测试**：CI 仅 build-only（`maven.yml`），质量依赖 note/ 体系下的审计轮次（33 轮 117+ 修复）与基准驱动开发（benchmark/ 真实服务器实测）。
5. **两处轻微缺陷**：`BlockTier.java:11-13` 注释（1000 分母）与实现（`ChroniclerPanelCache.java:314` 用 10000）不一致；空故事池时记录者面板仍播放"假闪电"特效（`ChroniclerPanelCache.java:314-328`）。

## 关键建议

1. **立即实施三个 Quick Win Skill**（法术生成 / 故事内容 / 发布收口）——均具备完全 AI 化条件（24+ 分），其中法术生成可直接支撑 README 宣称的"80+ 组合"内容扩展。
2. **将性能优化循环固化为 Skill**——0.2.0 的 9 轮已人工验证该流水线可复制，固化后每轮节省约半天。
3. **补齐 BlockTier 注释与假闪电两处缺陷**（改动各 1-3 行，属低风险文档/UX 修正）。
4. **为 StoryUtils/InstancePlate 补单元测试**——两者是纯逻辑密集且契约清晰的最易测模块，可打破"零测试"现状。

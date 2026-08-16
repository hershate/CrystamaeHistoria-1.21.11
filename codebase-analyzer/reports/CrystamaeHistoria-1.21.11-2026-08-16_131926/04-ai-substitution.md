# CrystamaeHistoria AI 工作流替代方案报告

> 分析时间：2026-08-16 13:19
> 评估基于静态代码分析；每个模块给出 6 维评分（1-5，5 = 最适合 AI）、替代等级、函数级清单与接口契约。Blueprint 见 `blueprints/` 目录。

---

## 1. 模块级 AI 替代可行性评估

评分维度：确定性 / 输入结构化 / 安全风险（越高越安全）/ 领域复杂度（越高越通用）/ 上下文需求（越高越局部）/ 重复性。**总分 24-30 = 🤖 完全 AI 化；15-23 = 🧑‍💻 AI 辅助；6-14 = 👤 人工主导。**

### 1.1 新法术实现（`magic/spells/tier1/`，69 个法术）— 🤖 完全 AI 化（28/30）

| 维度 | 分 | 理由 |
|------|----|------|
| 确定性 | 5 | 模板完全固定：继承 Spell + SpellCoreBuilder 链 + 4 个抽象方法（`Push.java:18-40` 范式） |
| 输入结构化 | 5 | 输入 = 法术概念描述（名称/行为/数值/配方三元组 StoryType） |
| 安全风险 | 4 | 新法术 bug 被断路器隔离（`InstancePlate.java:99-108`），不伤全局 |
| 领域复杂度 | 4 | 需知 Bukkit API + 项目 SpellCore 能力集，但范式收敛 |
| 上下文需求 | 5 | 只需 Spell.java + 2-3 个示例法术 + RecipeSpell 定义 |
| 重复性 | 5 | 69 个既有同构实现即少样本库 |

**当前状态**：人工逐个编写。**AI 替代**：给定自然语言法术需求 → 生成完整 Spell 类 + SpellType 枚举注册 + spells.yml 默认项。**实施难度：低。预期收益：单法术 1-2 小时 → 10 分钟。**

**对应 Blueprint**：[blueprints/01-spell-author.md](blueprints/01-spell-author.md)

### 1.2 故事内容创作（`generic-stories.yml` / `blocks.yml`）— 🤖 完全 AI 化（27/30）

| 维度 | 分 |
|------|----|
| 确定性 5（YAML schema 固定：name/type/lore/shards/author）· 输入结构化 5 · 安全风险 5（纯内容，六段校验链兜底 `StoriesManager.java:200-274`）· 领域复杂度 4（需中文 flavor + shards 9 元数组约束 `Story.java:57-61`）· 上下文 4（需既有故事风格样例）· 重复性 4 |

**AI 替代**：批量生成符合 schema 的故事条目（lore 文案创作正是 LLM 强项）。**收益：内容量产化。**
**对应 Blueprint**：[blueprints/02-story-content-author.md](blueprints/02-story-content-author.md)

### 1.3 版本发布收口（`note/release/<版本>.md` + pom 版本号）— 🤖 完全 AI 化（26/30）

确定性 5（git log → changelog 聚合）· 结构化 5 · 风险 5 · 领域 4 · 上下文 4（需读 note 索引）· 重复性 5。项目已有 3 份 release 文档可作范式（`note/release/0.1.0.md` 等）。
**对应 Blueprint**：[blueprints/04-release-notes-generator.md](blueprints/04-release-notes-generator.md)

### 1.4 性能优化循环（benchmark 变体 + 跑分 + 报告）— 🧑‍💻 AI 辅助（22/30）

确定性 3（优化假设需创造）· 结构化 4（benchmark/ 已有完整范式）· 风险 4（有基准守护）· 领域 3（需 JVM/Bukkit tick 模型直觉）· 上下文 3（需跨机械/法术链路）· 重复性 5（已完成 9 轮，模式高度稳定）。
**关键实证**：git log 显示 0.2.0 的 9 轮优化即"AI 提出假设 + 写变体 + 出报告"模式——本工作流**已被 AI 实际执行过**，本评估是对既成事实的确认。
**对应 Blueprint**：[blueprints/03-perf-optimization-loop.md](blueprints/03-perf-optimization-loop.md)

### 1.5 稳定性审计轮次（`note/audit/` 33 轮模式）— 🧑‍💻 AI 辅助（21/30）

确定性 3 · 结构化 4（审计 checklist 已沉淀 33 轮）· 风险 3（修复建议需人工定夺兼容性）· 领域 3（PDC 信任边界/事件时序等深层知识）· 上下文 2（需全库视野）· 重复性 5。117+ 项修复历史即少样本。

### 1.6 新机械/Gadget 实现（`slimefun/items/`）— 🧑‍💻 AI 辅助（19/30）

确定性 3 · 结构化 4（照抄 ChroniclerPanel 骨架，`note/README.md` 维护要点 3）· 风险 3（tick 路径性能与状态恢复正确性）· 领域 2（Slimefun BlockStorage 生命周期深知识）· 上下文 3 · 重复性 4。

### 1.7 PDC 数据类型（`utils/datatypes/`）— 🧑‍💻 AI 辅助（18/30）

12 个既有类型范式清晰，但新类型需理解 Bukkiet PersistentDataType 序列化边界 + 损坏降级要求。

### 1.8 架构决策 / 版本兼容性策略 — 👤 人工主导（11/30）

如"是否移除某第三方集成"（`SupportedPluginManager.java:20-31` 记录的 mcMMO/WildStacker 移除决策）、Slimefun 5.0 API 破坏性变更应对（`note/README.md` 维护要点 2 的 SlimefunItemStack 分离）。确定性 2 · 风险 2 · 领域 1 · 上下文 1。

### 1.9 施法热路径调优 / SpellMemory 演进 — 👤 人工主导（13/30）

主线程单线程模型下的并发改造、13 张表结构变更等，错判即影响全服 TPS。

---

## 2. 函数级替代粒度分析（下钻 §1.1 完全 AI 化模块）

`magic/spells/tier1/` 69 个法术的公共函数清单（每个法术固定 4-8 个方法）：

| 函数名 | 签名（范式） | 位置示例 | 行数 | AI 替代潜力 |
|--------|-------------|---------|------|-----------|
| 构造器 | `X() { setSpellCore(new SpellCoreBuilder(...).make...().build()) }` | Push.java:20-27 | 5-15 | 完全 AI 化 |
| `getRecipe` | `@Nonnull RecipeSpell getRecipe()` | Push.java:51-58 | 8 | 完全 AI 化（三元组 StoryType） |
| `getName` | `@Nonnull String getName()` | Push.java:60-63 | 4 | 完全 AI 化（中文命名） |
| `getLore` | `@Nonnull String[] getLore()` | Push.java:65-71 | 8 | 完全 AI 化（文案创作） |
| `getId` | `@Nonnull String getId()` | Push.java:73-76 | 4 | 完全 AI 化（大写蛇形） |
| `getMaterial` | `@Nonnull Material getMaterial()` | Push.java:78-81 | 4 | 完全 AI 化（图标选材） |
| 事件回调 | `void onX(CastInformation ci)` | Push.java:29-40 | 5-30 | 完全 AI 化（Fireball 弹射物型 / Push 滴答型两大范式） |

**接口契约提取（法术实现合约）**：

```markdown
### 新法术接口契约
前置条件：
- SpellType 枚举已添加常量（SpellType.java:83-151 尾部追加）
- 使用的每个 Consumer 事件槽在 SpellCoreBuilder 有对应 make*/add* 方法
- RecipeSpell 的 3 个 StoryType 组合不与既有 69 个法术重复（液化池以 top-3 集合匹配）
后置条件：
- spells.yml 启用项由 ConfigManager.loadConfig() 自动补全（ConfigManager.java:89-97）
- 启用法术自动注册进液化池 RECIPES_SPELL（ConfigManager.java:100-102）
不变式：
- getId() 返回值全局唯一且持久于 spells.yml/player_stats.yml path
- crysta 扣减先于 castSpell（InstancePlate.java:93-100），回调内不得再触碰法杖 meta
错误场景：
- 回调抛异常 → 断路器吞掉 + 首次日志（InstancePlate.java:99-108）
```

**依赖上下文清单**（生成一个新法术 Skill 所需 context）：

| 依赖类型 | 内容 | 来源 |
|---------|------|------|
| 范式样例 | Push.java（ticking 型）、Fireball.java（projectile 型）各 1 份 | src/.../tier1/ |
| 能力集 API | SpellCoreBuilder 全部 make*/add* 方法签名 | SpellCoreBuilder.java:76-160+ |
| 配方类型 | RecipeSpell 构造（tier + 3 StoryType） | liquefactionbasin/RecipeSpell.java |
| 领域枚举 | StoryType 9 项 | stories/definition/StoryType.java |
| 工具库 | GeneralUtils/SpellUtils/ParticleUtils 方法签名 | utils/ |
| 注册点 | SpellType 枚举尾部 + （如需）图鉴组 | SpellType.java:151 |

---

## 3. ROI 优先级矩阵

```
                    高收益
                      │
  🥈 2.性能优化循环    │   1.法术生成 🥇
  🥈 5.审计轮次       │   2.故事内容 🥇
  7.机械骨架          │   3.发布收口 🥇
                      │  6.PDC 类型
低难度 ───────────────┼───────────────── 高难度
                      │
  8.文档同步（低收益） │  9.架构决策 👤
                      │  10.热路径演进 👤
                    低收益
```

| 编号 | 建议 | 等级 | 实施难度 | 预期收益 | 优先级 |
|------|------|------|---------|---------|--------|
| 1 | 法术生成 Skill | 🤖 | 低 | 单法术 1-2h→10min；解锁 80+ 组合内容扩展 | 🥇 Quick Win |
| 2 | 故事内容 Skill | 🤖 | 低 | 内容量产（数百条/批） | 🥇 Quick Win |
| 3 | 发布收口 Skill | 🤖 | 低 | 每版本省 30min 且格式恒定 | 🥇 Quick Win |
| 4 | 性能优化循环 Skill | 🧑‍💻 | 中 | 已实证 9 轮可复制（29x/12.4x/1034x 级收益） | 🥈 Strategic |
| 5 | 审计轮次 Skill | 🧑‍💻 | 中 | 已实证 33 轮 117+ 修复 | 🥈 Strategic |
| 6 | PDC 类型生成 | 🧑‍💻 | 低 | 偶发需求 | 🥉 Incremental |
| 7 | 机械骨架生成 | 🧑‍💻 | 中 | 新机械起步 4h→1h | 🥉 Incremental |
| 8 | 文档同步 | 🤖 | 低 | note 索引自动更新 | 🥉 Incremental |
| 9 | 架构决策 | 👤 | — | AI 仅提供调研材料 | 不替代 |
| 10 | 热路径演进 | 👤 | — | AI 仅做基准执行者 | 不替代 |

---

## 4. AI 改造路线图

### Phase 1 — Quick Wins（本月）
1. **法术生成 Skill**（Blueprint 01）——直接消费 SpellCoreBuilder 范式，预计支撑后续 80+ 组合的内容迭代。
2. **故事内容 Skill**（Blueprint 02）——YAML schema + 风格样例驱动批量创作。
3. **发布收口 Skill**（Blueprint 04）——git log 聚合 → note/release + pom 版本联动。

### Phase 2 — Strategic（本季度）
4. **性能优化循环 Skill**（Blueprint 03）——固化"benchmark 变体 → 服务器跑分 → 量化报告 → 三连提交"流水线（0.2.0 已人工跑通 9 轮，流程模板完整）。
5. **审计轮次 Skill**——以 note/audit 33 轮 checklist 为知识库的周期性静态审计。

### Phase 3 — Transformative（本年度）
6. **单元测试补齐**——以 AI 辅助为 StoryUtils/InstancePlate 等纯逻辑类补 JUnit（需先解除静态服务定位器耦合，属架构决策 9 的前置）。

---

## 5. 风险与限制（全局限性）

- **AI 生成法术的质量闸门**：RecipeSpell 三元组冲突检测必须机械执行（液化池 top-3 集合匹配语义，`LiquefactionBasinCache.java:374-383`），不能依赖 AI 自查。
- **上下文窗口**：全库 33.6k 行超出单次上下文——所有 Skill 必须设计为"范式样例 + API 签名"的局部上下文模式（本报告各 Blueprint 已按此设计）。
- **运行时验证缺位**：项目无测试基建，AI 产物只能靠 CI 构建 + 服务器 soak 验证——Skill 工作流中应内置"编译门禁"步骤。
- **安全边界**：PDC 信任边界（02 报告 §5.1 三层防御）是本项目重要资产，AI 生成代码必须保留同等级防御式解析，Blueprint 已将此列入 Constraints。

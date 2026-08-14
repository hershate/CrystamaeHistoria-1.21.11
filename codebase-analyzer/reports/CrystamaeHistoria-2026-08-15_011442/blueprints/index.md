# Skill Blueprint 索引

**项目**：CrystamaeHistoria（魔法水晶编年史，Slimefun4 附属插件）
**生成时间**：2026-08-15_011442

| # | Blueprint | 组件 | AI 等级 | 评分 | 优先级 | 文件 |
|---|-----------|------|---------|------|--------|------|
| 1 | crystamae-story-data-author | 方块故事数据定义（blocks.yml） | 🤖 完全 AI 化 | 28/30 | 🥇 Quick Win | [02-story-data-author.md](02-story-data-author.md) |
| 2 | crystamae-translation | 本地化与翻译 | 🤖 完全 AI 化 | 26/30 | 🥇 Quick Win | [03-translation-l10n.md](03-translation-l10n.md) |
| 3 | crystamae-spell-author | 新法术创作 | 🤖 完全 AI 化 | 25/30 | 🥇 Quick Win | [01-spell-author.md](01-spell-author.md) |
| 4 | crystamae-test-gen | 单元测试生成 | 🤖 完全 AI 化 | 24/30 | 🥈 Strategic | [04-unit-test-gen.md](04-unit-test-gen.md) |
| 5 | crystamae-version-compat | 依赖升级与版本兼容 | 🧑‍💻 AI 辅助 | 23/30 | 🥇 Quick Win | [05-version-compat-deps.md](05-version-compat-deps.md) |

## 实施路线图

### 立即实施（Quick Win）
1. **方块故事数据定义**（[02-story-data-author.md](02-story-data-author.md)）— 最高分 28/30；纯 YAML 数据、解析器自带启动期校验兜底，风险最低
2. **本地化与翻译**（[03-translation-l10n.md](03-translation-l10n.md)）— 本汉化 fork 的刚性高频需求（127 个上游合并提交持续引入新文本）
3. **新法术创作**（[01-spell-author.md](01-spell-author.md)）— 70 个同构模板证明模式稳定；唯一需人工判断的是效果回调与数值平衡
4. **依赖升级与版本兼容**（[05-version-compat-deps.md](05-version-compat-deps.md)）— 含人工运行时验证审查点

### 规划实施（Strategic）
5. **单元测试生成**（[04-unit-test-gen.md](04-unit-test-gen.md)）— 前置条件：pom.xml 引入 JUnit 5 + surefire；从 StoryChances/RecipeSpell 等纯逻辑类起步

### 明确不替代（人工主导）
- 机械逻辑与数值平衡（12/30）：玩法设计决策，AI 仅可做蒙特卡洛数值模拟辅助

---

> 每个 Blueprint 文件包含了创建对应 Skill 所需的完整设计规格。
> 使用 `skill-for-skills` 加载对应 Blueprint 文件即可生成标准 SKILL.md。

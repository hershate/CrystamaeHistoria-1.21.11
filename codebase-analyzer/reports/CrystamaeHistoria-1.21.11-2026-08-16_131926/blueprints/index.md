# Skill Blueprint 索引

> 分析时间：2026-08-16 13:19 · 项目：CrystamaeHistoria 0.2.0

| # | Blueprint | 组件 | AI 等级 | 优先级 | 文件 |
|---|-----------|------|---------|--------|------|
| 1 | crystamae-spell-author | 法术实现生成（69 个同构法术范式） | 🤖 完全 AI 化 | 🥇 Quick Win | [01-spell-author.md](01-spell-author.md) |
| 2 | crystamae-story-author | 故事 YAML 内容批量创作 | 🤖 完全 AI 化 | 🥇 Quick Win | [02-story-content-author.md](02-story-content-author.md) |
| 3 | crystamae-perf-loop | 性能优化轮次执行（基准驱动） | 🧑‍💻 AI 辅助 | 🥈 Strategic | [03-perf-optimization-loop.md](03-perf-optimization-loop.md) |
| 4 | crystamae-release-closer | 版本发布说明 + 收口 | 🤖 完全 AI 化 | 🥇 Quick Win | [04-release-notes-generator.md](04-release-notes-generator.md) |

## 实施路线图

### 立即实施（Quick Win）
1. **法术生成**（01）——最大内容杠杆：SpellCoreBuilder 声明式范式使 69 → 80+ 法术的扩展成本趋近于零；内置编译门禁与配方冲突机械检测。
2. **故事内容**（02）——纯内容生产，schema 硬约束明确，六段校验链兜底。
3. **发布收口**（04）——git 历史已规范化（perf/docs/chore scope 前缀），聚合即文档。

### 规划实施（Strategic）
4. **性能优化循环**（03）——0.2.0 已人工跑通 9 轮的完整流水线模板化；人工审核点设在"优化假设选择"。

### 逐步推进（Incremental）
5. PDC 数据类型生成、机械骨架生成（见 [../04-ai-substitution.md](../04-ai-substitution.md) §3 矩阵 6/7 项）。

---

> 每个 Blueprint 文件包含创建对应 Skill 所需的完整设计规格（frontmatter 触发词、接口契约、依赖清单、Workflow、Constraints、Do/Not 示例）。
> 使用 `skill-for-skills` 加载对应 Blueprint 文件即可生成标准 SKILL.md。

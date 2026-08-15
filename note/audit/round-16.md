# 审计第 16 轮：核心引擎终审——Spell 分发 / StoriesManager 加载器

日期：2026-08-15
范围：`magic/spells/core/Spell.java`（施法分发/数值换算/目标选取，全量精读）、`managers/StoriesManager`（995 方块加载器，全量精读）

## 已修复（1 个 commit）

| commit | 问题 |
|--------|------|
| `d65dbec` | **StoriesManager 加载器三处损坏数据缺陷**（blocks.yml 可被编辑）：① 非法 tier（缺失/越界）将 null BlockTier 装进定义——`canBeStoried` 等运行期路径延迟 NPE；② 非法元素名只打日志不剔除——null 混入元素池，发掘时 NPE；③ `rebuildStoriedStack` 对带故事标记但无故事列表的伪造物品 for 循环 NPE（记录者 tick 路径）。另 `Spell.applyEffects` 的 amplification-1 钳制（当前数据 ≥1，防御未来新法术） |

## 核验安全（记录依据）

- Spell.castSpell 三路分发与事件槽注入、registerTicker（cancel 即移除，round-1 闭环）✓
- 数值换算（冷却/范围/消耗/伤害/击退/AOE）与图鉴展示字段一致（round-3 修复后）✓
- getTargets 的 caster 自豁免用 UUID equals（round-1 已修）✓；mainTarget 引用比较依赖 Paper 实体实例缓存，行为一致——记录
- 五级 BlockTier 硬编码与 StoryChances 合计 100 校验 ✓
- fillStories 对缺失稀有度段 fail-fast（Preconditions，插件启用期可见错误）✓

## 验证

`JAVA_HOME=F:/Java/21 mvn -q package` 构建通过。

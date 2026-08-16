# Skill Blueprint: 故事内容创作器（story-content-author）

> 自动生成自 codebase-analyzer
> 分析时间：2026-08-16 13:19
> 源模块路径：`src/main/resources/generic-stories.yml`、`src/main/resources/blocks.yml`、`stories/Story.java`

---

## 1. 基本信息

| 字段 | 值 |
|------|-----|
| **推荐 Skill 名称** | `crystamae-story-author` |
| **用途** | 批量生成符合 schema 与项目文风的原创故事条目（通用故事池 / 方块独特故事） |
| **AI 替代等级** | 🤖 完全 AI 化（27/30） |
| **实施优先级** | 🥇 Quick Win |
| **源文件数** | 2 个 YAML + Story/BlockDefinition 领域类 |
| **源代码行数** | YAML 数千行（内容资产） |

## 2. 触发场景与关键词

- "给故事池加 20 条虚空系故事"
- "为紫水晶方块写一条独特故事"
- "批量扩充 COMMON 档故事"

**推荐 description 触发词：**
```yaml
description: >-
  Batch-generate CrystamaeHistoria story entries (generic-stories.yml / blocks.yml)
  matching schema and established writing style. Triggered by: "写故事", "扩充故事池",
  "添加方块故事", "story content", "故事创作", "批量生成故事".
```

## 3. 输入输出契约

### 数据模型（`Story(ConfigurationSection, StoryRarity)`，`Story.java:49-75`）

```yaml
# generic-stories.yml 结构（五档顶层键：COMMON/UNCOMMON/RARE/EPIC/MYTHICAL）
COMMON:
  <STORY_NAME>:            # 同时是 id（Story.java:53 读取 "name"）与 map key
    type: ELEMENTAL        # 必须是 9 个 StoryType 之一（ELEMENTAL/MECHANICAL/ALCHEMICAL/
                          #  HISTORICAL/HUMAN/ANIMAL/CELESTIAL/VOID/PHILOSOPHICAL）
    shards: [1,1,1,1,1,1,1,1,1]   # 必须 9 元素（Story.java:57-61 校验告警）
    lore:                  # 展示文案（Story.java:72）
      - "第一行"
      - "第二行"
    author: 可选           # 原作者署名（Story.java:73）
    sponsor: 可选          # 赞助者（Story.java:74）

# blocks.yml 结构（StoriesManager.fillBlockDefinitions :200-274 消费）
<MATERIAL_NAME>:           # 必须 Material.valueOf 可解析（:222-236 守卫）
  tier: 1-5                # 非法 tier 整块跳过（:252-260）
  elements: [ELEMENTAL, VOID]   # 元素池，非法名剔除（:240-250）
  story:
    name: 独特故事名
    type: ...              # 同上
    shards: [...]
    lore: [...]
```

### 硬约束（schema 违反后果）

| 字段 | 约束 | 违反后果（系统行为） |
|------|------|--------------------|
| `type` | 9 枚举之一 | `StoryType.getByName` 返回 null → warning（`Story.java:63-67`），后续 addStory 空池跳过 |
| `shards` | 恰 9 元素 | warning（`Story.java:57-61`） |
| `MATERIAL_NAME` | Material 枚举名 | 整条跳过（`StoriesManager.java:231-236`） |
| `tier` | 1-5 | 整条跳过（`:252-260`） |
| 顶层档位 | 5 档之一 | `Preconditions.checkNotNull` 抛出（`:180-197`）→ **启动失败**（唯一致命约束） |

### 输出语义

- 通用故事进入 `storyMap<RARITY>` → `storiesByRarityAndType` 索引（`StoriesManager.java:65-71`）→ 记录者发掘时按稀有度×类型随机抽取（`StoryUtils.java:291-303`）。
- 方块独特故事在最后一个故事槽位解锁时必得（`StoryUtils.java:351-360`）。

## 4. 依赖清单

| 依赖类型 | 内容 | 来源 |
|---------|------|------|
| schema 定义 | Story 构造器字段读取 | `stories/Story.java:49-75` |
| 校验链 | 六段容错 | `managers/StoriesManager.java:200-274` |
| 类型枚举 | StoryType 9 项 / StoryRarity 6 项 | `stories/definition/` |
| 风格样例 | 既有条目（读取 YAML 前 50 条作 few-shot） | `generic-stories.yml` |
| 数值平衡 | shards 值域参考既有同档条目 | 同上 |

## 5. Skill 工作流设计

### 建议的 Workflow 步骤
```markdown
### Step 1: 解析需求
确定：目标档位（或 blocks.yml 目标材质）、数量、类型（StoryType）约束。
### Step 2: 加载风格样例
Read generic-stories.yml 中同档位同类型既有条目 ≥10 条，提取句式与长度分布。
### Step 3: 批量生成
按 schema 生成；name 唯一性用 Grep 对 YAML 自检。
### Step 4: schema 机械校验
shards=9 元素、type ∈ 9 枚举、（blocks.yml 时）Material 合法 + tier 1-5。
### Step 5: 追加写入
保持顶层档位结构，追加到对应 section 末尾。
### Step 6: 用户确认输出位置（聊天 / 文件，遵守项目询问惯例）
```

### 建议的 Constraints
```markdown
- Always 生成前 Grep 检查 name 与既有 id 不重复（id 是 map key，重复会静默覆盖）
- Always shards 为 9 元素整数数组
- Never 修改既有条目（只追加）
- Never 在 blocks.yml 使用不存在的 Material 枚举名
- Never 一次性生成超过 100 条（保持可审查批次）
```

## 6. 所需工具权限

| 工具 | 用途 | 必需性 |
|------|------|--------|
| `Read` | 风格样例 + schema | 必需 |
| `Edit` | YAML 追加 | 必需 |
| `Grep` | id 唯一性检查 | 必需 |

**建议 allowed-tools：** `Read Edit Grep`

## 7. 使用示例

### ✅ Do This
```
用户："给 VOID 类型加 10 条 RARE 档故事"
→ Read RARE 段 + VOID 类型样例 → 生成 10 条（lore 2-4 行短句、末尾留悬念风格）
→ Grep 确认 name 无重复 → Edit 追加 → 报告追加行号区间
```
### ❌ Not This
```
生成 type: "VOID_MAGIC"（非枚举名，运行期静默变 null 类型）
```

## 8. 参考材料

- schema 消费端：`stories/Story.java:49-75`
- 校验链：`managers/StoriesManager.java:176-274`
- 抽取逻辑：`utils/StoryUtils.java:264-303`
- 展示渲染：`Story.getStoryLore()`（`Story.java:104-124`）

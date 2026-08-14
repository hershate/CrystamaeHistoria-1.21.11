# Skill Blueprint: 方块故事数据定义（story-data-author）

> 自动生成自 codebase-analyzer
> 分析时间：2026-08-15_011442
> 源模块路径：`src/main/resources/blocks.yml`（354KB / ~21,500 行）+ `generic-stories.yml`

---

## 1. 基本信息

| 字段 | 值 |
|------|-----|
| **推荐 Skill 名称** | `crystamae-story-data-author` |
| **用途** | 为指定 Material 生成/校验符合 blocks.yml schema 的故事定义条目，或批量生成故事文本池条目 |
| **AI 替代等级** | 🤖 完全 AI 化 |
| **实施优先级** | 🥇 Quick Win |
| **源文件数** | 2 个 YAML + 解析器 3 个类 |
| **源代码行数** | blocks.yml ~21,500 行（约 880 个方块条目） |

## 2. 触发场景与关键词

- "给 DEEPSLATE_TILES 加一个故事定义"
- "新版本 MC 加了这些方块，批量补齐 blocks.yml"
- "校验 blocks.yml 有没有格式错误的条目"

**推荐 description 触发词：**
```yaml
description: >-
  Generate or validate blocks.yml / generic-stories.yml entries for CrystamaeHistoria
  with schema checks derived from the parser code. Triggered by: "blocks.yml", "故事定义",
  "方块故事", "批量生成故事", "story definition", "校验故事配置".
```

## 3. 输入输出契约

### blocks.yml 条目 schema（由解析代码反推）

```yaml
<MATERIAL 枚举名>:            # Material.getMaterial() 可解析（StoriesManager.java:185-199）
  tier: 1|2|3|4|5             # 必填 int；决定面板门槛与发掘概率
  elements:                   # 必填 List<String>；每个必须是 9 种 StoryType 之一
    - ELEMENTAL               # StoryType.java:8-16
    - ...
  story:                      # 必填 section（StoriesManager.java:175-182）
    name: <中文标题>           # 必填（StoriesManager.java:184-192）
    type: <StoryType>         # 故事主属性（Story.java:63-67；非法值仅警告不致命）
    lore: [<中文行>...]       # 2-4 行（Story.java:72 storyStrings）
    shards: [n1..n9]          # 必填 9 个 int，顺序固定 = StoryType ID 1-9
                              # （StoryShardProfile.java:21-31）
    author: <可选>            # Story.java:73
    sponsor: <可选>           # Story.java:74
```

### tier 对应的固有属性（硬编码，`StoriesManager.java:56-137`）

| tier | chroniclingChance（万分比/tick） | maxStories | minStories | 稀有度概率 C/U/R/E/M |
|------|------------------------------|-----------|-----------|----------------------|
| 1 | 700 | 3 | 1 | 85/15/0/0/0 |
| 2 | 600 | 3 | 2 | 70/25/5/0/0 |
| 3 | 500 | 4 | 2 | 50/35/10/5/0 |
| 4 | 400 | 5 | 3 | 25/40/20/10/5 |
| 5 | 300 | 5 | 4 | 5/30/30/20/15 |

### generic-stories.yml 条目 schema

```yaml
<COMMON|UNCOMMON|RARE|EPIC|MYTHICAL>:   # 五个池缺一不可（StoriesManager.java:142-161 Preconditions 硬失败）
  <故事 ID（英文）>:
    name: <中文显示名>
    type: <StoryType>
    shards: [n1..n9]
    lore: [<中文行>...]
```

### 错误场景

| 错误 | 触发条件 | 解析行为 |
|------|---------|---------|
| Material 非法 | `Material.getMaterial(key) == null` | 日志跳过（`StoriesManager.java:194-199`） |
| story 节缺失 | `storySection == null` | 日志跳过（`StoriesManager.java:177-182`） |
| name 缺失 | `name == null` | 日志跳过（`StoriesManager.java:187-192`） |
| elements 含非法值 | `StoryType.getByName == null` | 日志警告，条目仍加载（`StoriesManager.java:207-213`） |
| shards 长度 ≠9 | `section.getIntegerList("shards")` 越界 | `StoryShardProfile` 构造抛 IndexOutOfBounds → 启动失败 |
| 稀有度池缺失 | generic-stories.yml 五池之一不存在 | `Preconditions.checkNotNull` → 启动崩溃（`StoriesManager.java:143`） |

## 4. 依赖清单

| 类型 | 名称 | 用途 |
|------|------|------|
| 内部模块 | `managers/StoriesManager` | schema 权威来源（解析器） |
| 内部模块 | `stories/definition/StoryType` | 9 种元素枚举值 |
| 内部模块 | `stories/definition/StoryShardProfile` | shards 数组顺序定义 |
| 数据参考 | 既有 blocks.yml 条目 | 文风与 shards 数值分布参考（如 `ACACIA_BOAT` 条目 blocks.yml:1-21） |
| 数据参考 | `block_colors.yml` | 新方块若缺失颜色条目需一并补充（方块颜色 RGB，block_colors.yml:1-3 格式 `MATERIAL: [r,g,b]`） |

## 5. Skill 工作流设计

```markdown
## Workflow / Steps
### Step 1: 解析输入
确定目标 Material 列表、目标 tier（未指定时按物品稀有度/获取难度建议并询问用户）。
### Step 2: 重复性校验
Grep blocks.yml 确认 Material 键不存在（避免 YAML 键覆盖）。
### Step 3: 生成条目
按 schema 生成；elements 选 2-4 种符合方块语义的 StoryType；shards 总量参考同 tier 既有条目分布（通常 2-6 点，集中于与 type 一致的元素）；lore 为 2-4 行中文短诗，风格对齐既有文本。
### Step 4: 生成后自检
逐条核对 §3 错误场景表；特别验证 shards 数组长度==9。
### Step 5: 写回与验证建议
写入 blocks.yml（保持字母序插入），建议用户 `mvn package` 后启动服务器观察"已加载: N 个独特的方块故事."日志（StoriesManager.java:224-226）确认计数增加。
```

### 建议的 Constraints
```markdown
- Always 保持 shards 数组长度恰好为 9，顺序按 StoryType ID（ELEMENTAL→PHILOSOPHICAL）
- Always 新 Material 键按字母序插入 blocks.yml
- Never 修改既有条目（仅追加），除非用户明确要求
- Never 生成与既有故事完全雷同的文本（先 Grep 查重）
```

## 6. 所需工具权限

| 工具 | 用途 | 必需性 |
|------|------|--------|
| `Read` | 读取既有 blocks.yml 样本与解析器源码 | 必需 |
| `Grep` | 键重复检查、文本查重 | 必需 |
| `Edit` | 追加 YAML 条目 | 必需 |

**建议 allowed-tools：** `Read Grep Edit`

## 7. 使用示例

### ✅ Do This
```
输入："给 TUFF 和 CALCITE 加 tier1 定义"
输出：两个完整 YAML 条目（elements 贴合岩石语义、shards 9 元素数组、2 行中文 lore）+ 插入位置说明 + 自检清单
```

### ❌ Not This
```
输入："给 TUFF 加故事"
错误输出：shards 只写 3 个数字 → 启动崩溃（IndexOutOfBounds）
正确输出：严格 9 元素 shards 数组
```

## 8. 参考材料

- Schema 权威：`managers/StoriesManager.java:163-227`、`stories/Story.java:44-75`、`stories/definition/StoryShardProfile.java:21-31`
- 样本条目：`src/main/resources/blocks.yml:1-21`（ACACIA_BOAT）
- 文本池样本：`src/main/resources/generic-stories.yml:1-20`（COMMON/Quiet Days）
- tier 常量：`managers/StoriesManager.java:56-137`

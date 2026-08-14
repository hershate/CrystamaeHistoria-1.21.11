# Skill Blueprint: 本地化与翻译（translation-l10n）

> 自动生成自 codebase-analyzer
> 分析时间：2026-08-15_011442
> 源模块路径：全仓库用户可见文本（源码内嵌中文 + `resources/*.yml`）

---

## 1. 基本信息

| 字段 | 值 |
|------|-----|
| **推荐 Skill 名称** | `crystamae-translation` |
| **用途** | 对比上游英文提交与汉化分支，提取新增/变更字符串，按既有术语表产出中文译文补丁 |
| **AI 替代等级** | 🤖 完全 AI 化 |
| **实施优先级** | 🥇 Quick Win |
| **源文件数** | ~120 个含用户可见文本的文件 |
| **源代码行数** | 文本点约 800-1200 处（ActionBar/GUI/lore/命令输出） |

## 2. 触发场景与关键词

- "上游合并了新内容，把新增英文翻译成中文"
- "检查代码里还有没有漏翻的英文"
- "同步 Sefiraat 上游的翻译"

**推荐 description 触发词：**
```yaml
description: >-
  Extract and translate new user-facing strings after upstream merges for the
  CrystamaeHistoria Chinese fork, using the established glossary. Triggered by:
  "翻译", "汉化", "漏翻", "translation", "本地化", "上游同步翻译".
```

## 3. 输入输出契约

### 术语表（从既有译文提取的强制约定）

| 英文 | 既有中文 | 证据 |
|------|---------|------|
| Crysta / Crystamae | 魔法水晶 / 充能（语境） | `InstancePlate` lore（`InstanceStave.java:62`）、README.md |
| Stave | 法杖 | README.md:60-65 |
| Plate (Blank/Charged/Magical) | 魔法板/充能法术板 | README.md:48-58 |
| Story / Storied | 故事 / 有故事的 | `StoriesManager.java:256`（"有故事的"+物品名） |
| Chronicler Panel | 记录者（面板） | README.md:23-29 |
| Realisation Altar | 现实祭坛 | README.md:31-37 |
| Liquefaction Basin | 液化池 | README.md:39-46 |
| Stave Configurator | 法杖配置器 | README.md:60 |
| Cast / CastResult | 释放法术 / 施法 | `SpellCastListener.java:49,53` |
| Cooldown | 冷却中 | `CastResult.java:9` |
| Spell Compendium | 法术集 | `commands/OpenSpellCompendium.java`、提交 d52f0e4 |
| Gilded / Gilding | 镀金 | `player/GildingRank.java`、`GildedCollectionFlexGroup.java` |
| Shard | 碎片（水晶碎片） | `StoryShardProfile.dropShards`（`CrystalBreakListener.java:60`） |

### CastResult 消息风格（动作栏短语，无标点结尾）
`CastResult.java:6-10`："成功"/"充能不足"/"该栏位没有法术"/"法术冷却中"/"该法术已被禁用"

### 错误场景

| 错误 | 触发条件 | 处理 |
|------|---------|------|
| 术语不一致 | 同一英文使用多个中文译名 | 以术语表为唯一权威；冲突时询问用户 |
| MC 官方译名冲突 | 物品/效果名与官方中文版不一致 | 优先使用 MC 官方中文译名 |
| Java 字符串转义 | 译文含 `"` 或换行 | 保持源码字符串转义合法 |

## 4. 依赖清单

| 类型 | 名称 | 用途 |
|------|------|------|
| 外部 | 上游仓库 `https://github.com/Sefiraat/CrystamaeHistoria` | 英文原文来源 |
| 外部 | 汉化上游 `https://github.com/SlimefunGuguProject/CrystamaeHistoria` | 既有译文基准 |
| 工具 | `git diff upstream/master...master` | 差异提取 |
| 内部 | `utils/theme/ThemeType` | 颜色码与文本拼接约定（译文须保留 ThemeType 着色结构，如 `ThemeType.WARNING.getColor() + "..."`） |

## 5. Skill 工作流设计

```markdown
## Workflow / Steps
### Step 1: 提取差异
运行 git diff 定位新增/变更的用户可见字符串（排除注释与日志；日志面向腐竹可保留英文或中英双语）。
### Step 2: 分类
按类型分组：ActionBar 短语 / GUI 标题与按钮 / 物品 lore / 命令输出 / 故事文学文本。
### Step 3: 翻译
套用术语表；ActionBar 保持短促无标点；lore 保持诗意换行；文学文本（故事）允许意译。
### Step 4: 一致性自检
Grep 术语表中的英文原词，确认全仓库无第二种译法。
### Step 5: 输出补丁并建议验证
以 Edit 应用译文；建议 `mvn package` + 游戏内浏览对应 GUI/法术图鉴验证。
```

### 建议的 Constraints
```markdown
- Always 术语表优先于直译
- Always 保留 ThemeType/ChatColor 颜色包裹结构，只替换其中文本
- Never 翻译代码标识符、PDC 键名、配置文件键名
- Never 修改 generic-stories.yml 既有故事文本（文学作品，改动需人工决策）
```

## 6. 所需工具权限

| 工具 | 用途 | 必需性 |
|------|------|--------|
| `Bash` | git diff 提取 | 必需 |
| `Grep` | 术语一致性检查 | 必需 |
| `Read` | 上下文阅读 | 必需 |
| `Edit` | 应用译文 | 必需 |

**建议 allowed-tools：** `Bash Grep Read Edit`

## 7. 使用示例

### ✅ Do This
```
输入："上游新增了 'Spell has been disabled by the server' 消息"
输出：按 CastResult 风格译为"该法术已被服务器禁用"，并保持 ThemeType.WARNING 包裹
```

### ❌ Not This
```
输入：翻译 GUI 按钮 "Back"
错误输出："回去"（与既有 GUI 用语不一致）
正确输出：先 Grep 既有 FlexGroup 中的返回按钮用语（ChestMenuUtils 约定），沿用一致译名
```

## 8. 参考材料

- 风格基准：`magic/CastResult.java:5-10`、`listeners/SpellCastListener.java:48-55`
- GUI 文本基准：`slimefun/itemgroups/SpellCollectionFlexGroup.java`、`MainFlexGroup.java`
- 术语基准：`README.md` 全文（官方中文玩法术语）
- 历史译文提交：`86adb3c`（修正部分法术翻译）、`3b117aa`（修复文本）

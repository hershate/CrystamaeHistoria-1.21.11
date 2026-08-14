# Skill Blueprint: 新法术创作（spell-author）

> 自动生成自 codebase-analyzer
> 分析时间：2026-08-15_011442
> 源模块路径：`src/main/java/io/github/sefiraat/crystamaehistoria/magic/spells/tier1/`（70 个样板实例）

---

## 1. 基本信息

| 字段 | 值 |
|------|-----|
| **推荐 Skill 名称** | `crystamae-spell-author` |
| **用途** | 根据自然语言法术概念生成符合本项目模板的完整新法术（Java 类 + 枚举注册 + 液化池配方） |
| **AI 替代等级** | 🤖 完全 AI 化 |
| **实施优先级** | 🥇 Quick Win |
| **源文件数** | 70（模板样本）+ 3（注册点） |
| **源代码行数** | 每法术约 60-150 行 |

## 2. 触发场景与关键词

- "给魔法水晶编年史加一个新法术：效果是……"
- "写一个火焰投射物法术，冷却 30 秒，配方用 FIRE+VOID+CELESTIAL"
- "为 CrystamaeHistoria 新增 tier2 法术"

**推荐 description 触发词：**
```yaml
description: >-
  Generate a complete new spell for CrystamaeHistoria (Slimefun4 addon) following the
  project's SpellCoreBuilder template. Triggered by: "新法术", "添加法术", "写一个法术",
  "add spell", "new spell", "CrystamaeHistoria spell".
```

## 3. 输入输出契约

### 法术模板固定点（每个法术必须实现的 6 个成员）

| 成员 | 签名 | 说明 | 参考 |
|------|------|------|------|
| 构造函数 | `public Xxx()` | 用 `SpellCoreBuilder` 构建 `SpellCore` 并 `setSpellCore(builder.build())` | `magic/spells/tier1/Push.java:22-28` |
| `getRecipe()` | `@Nonnull RecipeSpell getRecipe()` | `new RecipeSpell(tier, StoryType×3)`；3 元素组合在全法术集中必须唯一 | `Push.java:51-59`、`RecipeSpell.java:16-25` |
| `getName()` | `@Nonnull String getName()` | 中文法术名（汉化分支覆写基类默认） | `Push.java:61-64` |
| `getLore()` | `@Nonnull String[] getLore()` | 2-4 行中文诗意描述 | `Push.java:66-72` |
| `getId()` | `@Nonnull String getId()` | 大写英文 ID（spells.yml 开关键，如 `"PUSH"`） | `Push.java:74-77` |
| `getMaterial()` | `@Nonnull Material getMaterial()` | 图鉴图标材质 | `Push.java:79-82` |

### SpellCoreBuilder API 契约（`magic/spells/core/SpellCoreBuilder.java`）

| 方法 | 行号 | 参数语义 |
|------|------|---------|
| 构造函数 `(cooldownSeconds, cooldownDivided, range, rangeMultiplied, crystaCost, crystaMultiplied)` | :67 | 冷却秒数（true=被法杖等级除）、射程（true=乘等级）、充能消耗（true=乘等级） |
| `makeHealingSpell(healAmount, healMultiplied)` | :78 | 治疗系 |
| `makeDamagingSpell(damage, damageMultiplied, knockback, knockbackMultiplied)` | :96 | 伤害系 |
| `makeEffectingSpell(ampMultiplied, durMultiplied)` | :87 | 药水效果系（配合 addPositive/NegativeEffect） |
| `makeInstantSpell(Consumer<CastInformation>)` | :108 | 即时施法 |
| `makeProjectileSpell(...)` / `makeProjectileVsEntitySpell(...)` / `makeProjectileVsBlockSpell(...)` | :117/:136/:147 | 投射物系 |
| `addBeforeProjectileHitEntityEvent / addAfterProjectileHitEntityEvent / addProjectileHitBlockEvent` | :158/:166/:174 | 命中回调 |
| `makeTickingSpell(Consumer, numberOfTicks, ticksMultiplied, tickInterval, tickIntervalMultiplied)` | :182 | tick 系 |
| `addAfterTicksEvent(Consumer)` | :195 | 全部 tick 结束回调 |
| `addPositiveEffect / addNegativeEffect(PotionEffectType, level, durationSeconds)` | :213/:225 | 效果挂载 |
| `build()` | :232 | 产出不可变 SpellCore |

### 注册步骤（输出物 3 处改动）

1. 新文件 `magic/spells/tier1/<Name>.java`
2. `magic/SpellType.java` 枚举区按字母序追加 `<NAME>(new <Name>()),`（现有注册区 `SpellType.java:83-151`）
3. （汉化）确认 `getName()/getLore()` 为中文；若上游先行则需同步英文原名注释

### 错误场景

| 错误 | 触发条件 | 后果/处理 |
|------|---------|----------|
| 配方冲突 | 3 元素组合+plate tier 与既有权术重复 | `getMatchingRecipe` 只返回首个匹配（`LiquefactionBasinCache.java:362-371`），旧法术被顶替 — 生成前必须全量比对既有 70 个 `getRecipe()` |
| ID 重复 | `getId()` 与既有重复 | spells.yml 键冲突，启用状态错乱 |
| 缺 Consumer | `makeProjectileSpell` 未提供发射回调 | NPE 于 `Spell.castSpell()`（`Spell.java:93-94`） |

## 4. 依赖清单

### 内部模块
| 模块 | 用途 | 关键接口 |
|------|------|---------|
| `magic/spells/core/Spell` | 基类 | 6 个可覆写成员（见上） |
| `utils/SpellUtils` | 召唤投射物/掉落块/生物 | `summonMagicProjectile(castInfo, entityType, location[, seconds][, tickConsumer])`、`summonTemporaryMob(...)`、`summonMagicFallingBlock(...)`（`SpellUtils.java:33-205`） |
| `utils/GeneralUtils` | 伤害/治疗/推挤/权限 | `damageEntity`、`healEntity`、`pushEntity`、`hasPermission`（`GeneralUtils.java:129-260`） |
| `utils/ParticleUtils` | 粒子表现 | `displayParticleEffect(...)` |
| `SpellMemory` | 长期状态登记 | 经 `CrystamaeHistoria.getSpellMemory()` |

### 配置项
| 配置键 | 说明 |
|--------|------|
| `spells.yml#<ID>` | 布尔开关，默认 true（`ConfigManager.java:79-98` 自动生成） |

## 5. Skill 工作流设计

```markdown
## Workflow / Steps
### Step 1: 解析输入
从用户描述提取：效果类型（即时/投射物/tick/召唤）、数值（冷却/射程/消耗/伤害）、3 元素配方、tier、中文名、图标材质。缺失项向用户询问，禁止猜测。
### Step 2: 配方唯一性校验
Grep `magic/spells/tier1/*.java` 中全部 `new RecipeSpell(` 调用，确认 3 元素组合+tier 未被占用。
### Step 3: 生成法术类
按 Push.java 模板生成；Consumer 回调中仅使用 SpellUtils/GeneralUtils/ParticleUtils 既有 API；世界修改类操作必须走 GeneralUtils.hasPermission 权限检查。
### Step 4: 注册枚举
在 SpellType.java 字母序位置插入一行。
### Step 5: 验证
建议用户执行 `mvn package` 确认编译；提醒在游戏内用 `/historia testspell <ID>`（commands/TestSpell.java）验证。
```

### 建议的 Constraints
```markdown
- Always 保持 getId() 为大写英文、getName()/getLore() 为中文
- Always 校验 RecipeSpell 三元素组合全局唯一
- Always 让数值随法杖等级的缩放标志与法术定位一致（辅助法术通常不乘等级伤害）
- Never 在 Consumer 中直接修改玩家飞行/时间等状态而不经 SpellMemory 登记（否则插件关闭时无法清理，见 SpellMemory.clearAll()）
- Never 生成未经权限检查的方块破坏/放置（参考 GeneralUtils.tryBreakBlock）
```

## 6. 所需工具权限

| 工具 | 用途 | 必需性 |
|------|------|--------|
| `Read` | 读取模板法术与 SpellCoreBuilder | 必需 |
| `Grep` | 配方唯一性校验 | 必需 |
| `Write` | 写入新法术文件 | 必需 |
| `Edit` | SpellType.java 注册 | 必需 |

**建议 allowed-tools：** `Read Grep Write Edit`

## 7. 使用示例

### ✅ Do This
```
输入："新法术『寒霜新星』：以自身为中心 6 格内敌人减速并受到 4 点伤害，冷却 45s，消耗 8 充能，配方 AIR+CELESTIAL+VOID tier1"
输出：FrostNova2.java（makeDamagingSpell + makeTickingSpell + addNegativeEffect(SLOW)）+ SpellType 注册行 + 配方冲突检查通过说明
```

### ❌ Not This
```
输入："加个法术"（未给效果/配方）
错误输出：直接编造效果与配方
正确输出：向用户追问效果类型、数值、3 元素配方、中文名
```

## 8. 参考材料

- 模板样本：`src/main/java/io/github/sefiraat/crystamaehistoria/magic/spells/tier1/Push.java`（tick 系）、同目录 `Fireball.java`（投射物系）、`Heal.java`（治疗系）、`Teleport.java`（即时系）
- 核心 API：`magic/spells/core/SpellCoreBuilder.java`、`Spell.java:87-108`（三路径分派）
- 注册表：`magic/SpellType.java:83-151`
- 配方匹配：`slimefun/items/mechanisms/liquefactionbasin/RecipeSpell.java:22-24`

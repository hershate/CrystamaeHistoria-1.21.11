# Skill Blueprint: 法术生成器（spell-author）

> 自动生成自 codebase-analyzer
> 分析时间：2026-08-16 13:19
> 源模块路径：`src/main/java/io/github/sefiraat/crystamaehistoria/magic/spells/tier1/`（69 个法术）

---

## 1. 基本信息

| 字段 | 值 |
|------|-----|
| **推荐 Skill 名称** | `crystamae-spell-author` |
| **用途** | 从自然语言法术需求生成完整的 Spell 实现（Java 类 + SpellType 注册 + 必要的测试命令钩子） |
| **AI 替代等级** | 🤖 完全 AI 化（28/30） |
| **实施优先级** | 🥇 Quick Win |
| **源文件数** | 69（tier1/）+ 5（core/）+ 1（SpellType） |
| **源代码行数** | ~8,000 |

## 2. 触发场景与关键词

用户在以下情况触发该 Skill：
- "添加一个新法术：冰墙，在视线落点生成临时冰块"
- "实现一个召唤类法术，召唤 3 只蝙蝠护卫"
- "给法术库加一个治疗系法术，圆形范围回血"

**推荐 description 触发词（用于 SKILL.md frontmatter）：**
```yaml
description: >-
  Generate new CrystamaeHistoria spell implementations (Spell subclass + SpellType
  registry entry + RecipeSpell) from natural language requirements. Triggered by:
  "添加法术", "新法术", "实现一个法术", "add spell", "new spell", "法术生成".
```

## 3. 输入输出契约

### 主要函数接口（生成的法术必须实现的模板）

| 函数 | 输入 | 输出 | 副作用 | 代码范式位置 |
|------|------|------|--------|-------------|
| 构造器 | — | `SpellCore`（经 `setSpellCore`） | 无 | `Push.java:20-27` / `Fireball.java:20-26` |
| `getRecipe()` | — | `RecipeSpell(tier, StoryType×3)` | 无 | `Push.java:51-58` |
| `getName()` | — | 中文法术名 | 无 | `Push.java:60-63` |
| `getLore()` | — | `String[]` 中文风味文案（1-3 行） | 无 | `Push.java:65-71` |
| `getId()` | — | 大写蛇形唯一 ID（如 `"PUSH"`） | 持久化为配置 path | `Push.java:73-76` |
| `getMaterial()` | — | 图标 `Material` | 无 | `Push.java:78-81` |
| 事件回调 0-N 个 | `CastInformation` | void | 世界/实体修改 | `Push.java:29-40` |

### 数据模型（生成的类继承链）

```java
// 两大范式（覆盖全部 69 个既有法术的形态）：
// A. 滴答/瞬时型（Push.java:18-40）
new SpellCoreBuilder(cooldownSeconds, cooldownDivided, range, rangeMultiplied, crystaCost, crystaMultiplied)
    .makeDamagingSpell(damage, dmgX?, knockback, kbX?)
    .makeTickingSpell(this::onTick, tickCount, ticksX?, tickInterval, intervalX?)
    .addAfterTicksEvent(this::afterAllTicks)
    .build();
// B. 弹射物型（Fireball.java:19-47）
new SpellCoreBuilder(...)
    .makeDamagingSpell(...)
    .makeProjectileSpell(this::fireProjectile, aoeRange, aoeX?, projKnockback, projKbX?)
    .makeProjectileVsEntitySpell(this::projectileHit)
    .addBeforeProjectileHitEntityEvent(this::beforeProjectileHit)
    .build();
// 其余能力：makeInstantSpell / makeHealingSpell / makeEffectingSpell /
//          makeProjectileVsBlockSpell / addPositiveEffect / addNegativeEffect
```

### 错误场景

| 错误 | 触发条件 | 系统行为（AI 生成代码无需处理，但必须知晓） |
|------|---------|------|
| 回调异常 | 法术逻辑 bug | 断路器吞掉 + 同法术仅首次日志（`InstancePlate.java:99-108`） |
| 配方冲突 | 三元组与既有法术相同 | 液化池线性扫描取首个匹配（`LiquefactionBasinCache.java:374-383`）→ **必须生成时机械检测** |

## 4. 依赖清单

### 内部模块
| 模块 | 用途 | 关键接口 |
|------|------|---------|
| `SpellCoreBuilder` | 声明法术能力 | 全部 `make*/add*` 方法签名（`SpellCoreBuilder.java:76+`） |
| `CastInformation` | 回调上下文 | `getCaster()/getCastLocation()/getTargetedBlockOnCast()/getDamageLocation()/getMainTarget()/getStaveLevel()` |
| `Spell` 基类 | 继承 | `getDamage(ci)/getRange(ci)/getTargets(ci,range,includeMain)/applyPositiveEffects(...)`（`Spell.java:130-284`） |
| `SpellUtils` | 召唤 | `summonMagicProjectile/summonTemporaryMob/summonMagicFallingBlock`（`SpellUtils.java:33-183+`） |
| `GeneralUtils` | 伤害/权限/随机 | `damageEntity/healEntity/hasPermission/testChance/pushEntity`（`GeneralUtils.java:46-324+`） |
| `ParticleUtils` | 粒子 | `displayParticleEffect` |

### 配置项（生成后自动生效）
| 键 | 类型 | 默认 | 说明 |
|----|------|------|------|
| `spells.yml → <ID>` | bool | true（自动补全） | `ConfigManager.java:89-97` |
| `player_stats.yml → <uuid>.SPELL.<ID>.*` | — | 自动 | 使用统计 |

## 5. Skill 工作流设计

### 建议的 Workflow 步骤
```markdown
### Step 1: 解析需求
从用户描述提取：法术名（中文）、行为类别（瞬时/弹射物/滴答/召唤）、目标语义。
### Step 2: 选择范式
瞬时/滴答 → 范式 A（Push）；弹射物 → 范式 B（Fireball）；召唤类参考 SummonGolem。
### Step 3: 数值映射
冷却/射程/晶能/伤害由 AI 依 69 个既有法术的数值分布给出建议值，列表呈现供用户确认。
### Step 4: 配方生成
从 9 个 StoryType 选 3 元组；Grep 既有 getRecipe() 确认无冲突。
### Step 5: 写入文件
tier1/<Name>.java + SpellType.java 枚举追加 import 与常量。
### Step 6: 编译门禁
mvn package -q 通过才报告成功。
```

### 建议的 Constraints
```markdown
- Always 保持与既有 69 个法术完全一致的类结构（构造器→setSpellCore→4 抽象方法）
- Always 中文 getName/getLore（项目语言规范，plugin.yml/全部法术均为中文）
- Always 事件回调内不得修改施法者手持物品元数据（写回契约，SpellCastListener.java:51-63）
- Never 在回调内自行 try-catch（断路器已存在，双层吞异常会掩盖问题）
- Never 使用 SpellCoreBuilder 未提供的能力（先扩展 core 再写法术）
- Never 生成与既有法术相同的 RecipeSpell 三元组
```

## 6. 所需工具权限

| 工具 | 用途 | 必需性 |
|------|------|--------|
| `Read` | 读取范式样例与 API 签名 | 必需 |
| `Write` | 写新法术文件 | 必需 |
| `Edit` | SpellType.java 注册追加 | 必需 |
| `Grep` | 配方冲突检测 / API 查找 | 必需 |
| `Bash`（mvn package） | 编译门禁 | 必需 |

**建议 allowed-tools：** `Read Write Edit Grep Bash(mvn:*)`

## 7. 使用示例

### ✅ Do This
```
用户："添加法术：寒冰新星，以自身为中心 5 格内敌人冰冻 3 秒，冷却 30 秒"
→ 生成 FrostNova 风格类（已存在，实际应选新名）：makeEffectingSpell +
   addNegativeEffect(SLOW, ...) + makeInstantSpell；RecipeSpell(tier, ELEMENTAL, ELEMENTAL, VOID)
→ 编译通过 → 报告：文件路径 + 注册行号 + 数值表
```
### ❌ Not This
```
用户："添加法术：斩杀" → 直接生成 9999 伤害无冷却法术（未走 Step 3 数值确认）
```

## 8. 参考材料

- 范式 A 全文：`src/main/java/io/github/sefiraat/crystamaehistoria/magic/spells/tier1/Push.java`
- 范式 B 全文：`.../tier1/Fireball.java`
- 能力集：`.../magic/spells/core/SpellCoreBuilder.java`、`SpellCore.java`
- 注册表：`.../magic/SpellType.java:83-151`
- 既有法术清单：`note/spell-system-analysis.md`

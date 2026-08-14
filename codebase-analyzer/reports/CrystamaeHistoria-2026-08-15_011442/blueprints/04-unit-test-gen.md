# Skill Blueprint: 单元测试生成（unit-test-gen）

> 自动生成自 codebase-analyzer
> 分析时间：2026-08-15_011442
> 源模块路径：目标为全仓库纯逻辑类（当前仓库 0 测试）

---

## 1. 基本信息

| 字段 | 值 |
|------|-----|
| **推荐 Skill 名称** | `crystamae-test-gen` |
| **用途** | 为本项目纯逻辑类生成 JUnit 5 单元测试，并给出 pom.xml 测试脚手架改造建议 |
| **AI 替代等级** | 🤖 完全 AI 化 |
| **实施优先级** | 🥈 Strategic（需先搭建脚手架） |
| **源文件数** | 潜在目标 ~15 个纯逻辑类 |
| **源代码行数** | 潜在测试 ~1500-2500 行 |

## 2. 触发场景与关键词

- "给 StoryChances 写单元测试"
- "为配方匹配逻辑生成测试"
- "补齐 CI 测试阶段"

**推荐 description 触发词：**
```yaml
description: >-
  Generate JUnit 5 unit tests for CrystamaeHistoria pure-logic classes and advise on
  Maven test scaffolding. Triggered by: "单元测试", "写测试", "生成测试", "unit test",
  "test coverage", "补测试".
```

## 3. 输入输出契约

### 可测目标清单（按"无需 Bukkit 运行时"筛选）

| 目标类 | 可测点 | 代码位置 | 风险等级 |
|--------|--------|---------|---------|
| `StoryChances` | 构造校验（概率和必须=100，否则 IllegalArgumentException，`StoryChances.java:18`）；getBasic/getUncommon/getRare/getEpic 的**链式累加语义**（getBasic 返回 basic+uncommon+rare+epic+mythical，即累积分布函数，`StoryChances.java:26-43`） | `stories/definition/StoryChances.java` | 高（语义极易误读） |
| `RecipeSpell` | `recipeMatches(set, tier)`：containsAll + tier 相等（`RecipeSpell.java:22-24`）；输入集合大小 <3 时的宽松匹配行为 | `liquefactionbasin/RecipeSpell.java` | 中 |
| `SpellType` | `getById()` 命中/未命中；`setupEnabledSpells()` 过滤逻辑 | `magic/SpellType.java:166-185` | 低 |
| `StoryRarity/StoryType` | `getById()/getByName()` 边界 | `stories/definition/*.java` | 低 |
| `TextUtils.toTitleCase` | ID → 标题转换 | `utils/TextUtils.java` | 低 |
| `TimePeriod` | 时间段判定 | `utils/TimePeriod.java` | 低 |
| `BlockTier` | 字段构造 | `stories/BlockTier.java:18-25` | 低 |

### 测试命名与结构契约

```java
// 每个测试类对应一个目标类：src/test/java/<同包>/<ClassName>Test.java
// 命名：<方法名>_<场景>_<期望>，如 recipeMatches_tierMismatch_returnsFalse
// 断言库：JUnit 5 Assertions；异常断言 assertThrows
```

### 错误场景（测试基础设施）

| 错误 | 触发条件 | 处理 |
|------|---------|------|
| 目标类静态依赖 `CrystamaeHistoria.getInstance()` | 如 `StoryUtils`、`Keys.newKey()` | 标记为"需 MockBukkit 或 mockito-inline mock 静态"，**不**生成会 NPE 的测试 |
| pom 无 test 依赖 | 当前 `pom.xml:116-231` 无 JUnit | 先输出 pom 改造片段（junit-jupiter + surefire），再生成测试 |

## 4. 依赖清单

| 类型 | 名称 | 用途 | 版本建议 |
|------|------|------|---------|
| 外部 | JUnit 5 | 测试框架 | 5.10+（test scope） |
| 外部 | maven-surefire-plugin | `mvn test` 执行 | 3.x |
| 外部（可选） | MockBukkit | Bukkit API mock（仅二期） | 与 Paper 1.19 匹配的版本 |
| 内部 | 上表目标类 | 被测对象 | — |

## 5. Skill 工作流设计

```markdown
## Workflow / Steps
### Step 1: 目标选择
用户指定类或按风险清单从 StoryChances 开始。
### Step 2: 静态依赖审查
Grep 目标类是否调用 CrystamaeHistoria.getInstance()/Bukkit.*；有则降级为脚手架建议，不生成测试。
### Step 3: 读取实现并识别边界
逐方法列出：正常路径、边界值（0/负数/空集合）、异常路径（Preconditions）。
### Step 4: 生成测试
每个公开方法 ≥3 用例（正常/边界/异常）；StoryChances 必须覆盖累积分布语义的 5 个 getter。
### Step 5: 脚手架与 CI 建议
若无 test 依赖：输出 pom.xml <dependency>(junit-jupiter, scope=test) 与 surefire 配置片段；建议 .github/workflows/maven.yml 在 `mvn package` 前增加 `mvn test`（或改 `mvn verify`）。
```

### 建议的 Constraints
```markdown
- Always 先做静态依赖审查，禁止生成依赖 Bukkit 运行时却未 mock 的测试
- Always 异常路径用 assertThrows 精确断言异常类型
- Never 为了让测试通过而弱化断言（如把 assertEquals 改成 assertNotNull）
- Never 修改被测源码（发现缺陷只报告，交人工决策）
```

## 6. 所需工具权限

| 工具 | 用途 | 必需性 |
|------|------|--------|
| `Read` | 读取被测类 | 必需 |
| `Grep` | 静态依赖审查 | 必需 |
| `Write` | 写入 src/test/ | 必需 |

**建议 allowed-tools：** `Read Grep Write`

## 7. 使用示例

### ✅ Do This
```
输入："给 RecipeSpell 写测试"
输出：RecipeSpellTest.java，覆盖：tier 相等+集合相等→true；tier 不等→false；集合为子集时 containsAll 语义；getInput 索引边界
```

### ❌ Not This
```
输入："给 ChroniclerPanelCache 写测试"
错误输出：直接生成测试 → BlockStorage/BlockMenu 无运行时导致 NPE
正确输出：报告该类需 MockBukkit 二期支持，先生成 StoryChances/RecipeSpell 等纯逻辑测试
```

## 8. 参考材料

- 被测类：`stories/definition/StoryChances.java`、`liquefactionbasin/RecipeSpell.java`、`magic/SpellType.java`
- CI 现状：`.github/workflows/maven.yml`（当前仅 `mvn package`，无测试阶段）
- 测试空白证据：全仓库无 `src/test/` 目录

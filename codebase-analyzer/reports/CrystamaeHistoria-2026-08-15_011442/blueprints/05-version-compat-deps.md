# Skill Blueprint: 依赖升级与版本兼容适配（version-compat-deps）

> 自动生成自 codebase-analyzer
> 分析时间：2026-08-15_011442
> 源模块路径：`pom.xml`（16 个依赖）+ 受影响代码点（历史案例：GuizhanLib 包名、法术集 GUI、paper repo URL）

---

## 1. 基本信息

| 字段 | 值 |
|------|-----|
| **推荐 Skill 名称** | `crystamae-version-compat` |
| **用途** | 依赖升级/Minecraft 服务端版本升级时，预判受影响代码点、生成升级 diff 与兼容性检查清单 |
| **AI 替代等级** | 🧑‍💻 AI 辅助（含人工审查点） |
| **实施优先级** | 🥇 Quick Win |
| **源文件数** | pom.xml + 潜在全部源码 |
| **源代码行数** | — |

## 2. 触发场景与关键词

- "把 GuizhanLib 升到 2.4.0，看哪里要改"
- "服务器要升 1.21.x，检查兼容性"
- "paper-api 更新后构建失败了"

**推荐 description 触发词：**
```yaml
description: >-
  Assess dependency/Minecraft version upgrades for CrystamaeHistoria: locate affected
  code, draft upgrade diffs, produce a runtime verification checklist. Triggered by:
  "依赖升级", "版本兼容", "升级1.21", "dependency upgrade", "compatibility", "更新依赖".
```

## 3. 输入输出契约

### 项目依赖地图（升级影响面基准）

| 依赖 | 版本 | scope | 耦合点 | 升级风险 |
|------|------|-------|--------|---------|
| paper-api | 1.19-R0.1-SNAPSHOT | provided | 全仓库 Bukkit API | **高**（MC 版本行为变化） |
| Slimefun4（SlimefunGuguProject） | 2024.3.1 | provided | 注册层/BlockStorage/BlockMenu/FlexItemGroup | **高**（API 迁移频繁） |
| GuizhanLibPlugin | 2.3.0 | provided | `CrystamaeHistoria.java:38` GuizhanUpdater、SpellCollectionFlexGroup 的 PotionEffectTypeHelper | **中**（历史案例：2.x 包名迁移 `f0b0223`） |
| InfinityLib | 1.3.9 | compile（shade） | AbstractAddon/TickingMenuBlock/SubCommand 基类 | **中**（标记待移除 `pom.xml:216`） |
| EffectLib | 10.3 | compile（shade） | `CrystamaeHistoria.java:187`、`LiquefactionBasinCache.java:421` | 低 |
| bstats-bukkit | 3.0.0 | compile（shade） | `CrystamaeHistoria.java:205` | 低 |
| MorePersistentDataTypes | 2.4.0 | compile | utils/datatypes | 低 |
| mcMMO/ExoticGarden/Networks/WildStacker/RoseStacker/Netheopoiesis | 见 pom.xml:166-214 | provided | `SupportedPluginManager.java:47-58` 探测 + 适配方法 | **中**（可选集成，API 变动仅影响 hook 方法） |

### 历史兼容修复案例（模式库）

| 案例 | 类型 | 修复 | 提交 |
|------|------|------|------|
| GuizhanLib 新包名 | import 迁移 | 4 文件 import `net.guizhanss.guizhanlibplugin.*` → `net.guizhanss.minecraft.guizhanlib.*` | `f0b0223` |
| 法术集 1.21 报错 | 运行期行为 | `SpellCollectionFlexGroup.java` 2 行修正 | `d52f0e4` |
| paper repo URL 失效 | 构建环境 | pom.xml repository URL | `3d05b93` |
| 1.20.6 兼容 | 综合 | 先修复后 revert 再重做 | `4dc4783` → `74f05dd` |

### 输出物契约
1. **影响面清单**：`<文件:行号> — 旧 API 符号 → 新 API 符号` 表
2. **升级 diff**：pom.xml 版本号变更 + 代码修正
3. **运行时验证清单**（人工执行）：见 §5 Step 4

### 错误场景

| 错误 | 触发条件 | 处理 |
|------|---------|------|
| 编译通过但运行崩溃 | API 语义变化（非签名） | 必须走运行时验证清单，禁止仅凭编译结论"已兼容" |
| provided 依赖钉 commit hash | ExoticGarden `edae221160`、Networks `3de3c9d608`（`pom.xml:170,188`） | 提示用户该依赖无版本号语义，需人工确认上游对应提交 |

## 4. 依赖清单

| 类型 | 名称 | 用途 |
|------|------|------|
| 外部 | [repo.papermc.io](https://repo.papermc.io/repository/maven-public/) | Paper API 制品库 |
| 外部 | [jitpack.io](https://jitpack.io) | GitHub 仓库制品化 |
| 外部 | 上游仓库 changelog/release notes | API 变更来源 |
| 内部 | `SupportedPluginManager` | 可选插件探测模式（延迟 1 tick 探测，`SupportedPluginManager.java:43,47-58`）——新增可选依赖时复用该模式 |

## 5. Skill 工作流设计

```markdown
## Workflow / Steps
### Step 1: 解析升级意图
确定：目标依赖与新版本（或目标 MC/Paper 版本）。缺失则询问用户。
### Step 2: 变更面侦察
对依赖升级：grep 全仓库该依赖 groupId/包名 import，统计受影响文件。
对 MC 版本升级：比对 Paper API changelog（WebFetch/WebSearch 官方文档），grep 已废弃/移除符号。
### Step 3: 生成升级 diff
pom.xml 版本号 + 逐文件代码修正建议（每处附 文件:行号）。
### Step 4: 运行时验证清单（人工审查点）
输出必须人工在游戏内验证的清单：
  a. 插件正常启用（无 NoClassDefFoundError）
  b. 5 个机械放置/tick/破坏全流程
  c. 法术图鉴/故事图鉴 GUI 打开（历史高发点，提交 d52f0e4）
  d. 施法 3 路径各抽测 1 个（即时/投射物/tick）
  e. 自动更新开关行为（config.yml#auto-update）
### Step 5: 回滚预案
升级前记录当前可工作版本号；建议独立分支提交，验证通过再合入 master。
```

### 建议的 Constraints
```markdown
- Always 输出"编译通过 ≠ 兼容"的运行时验证清单
- Always 升级提交信息写明影响面与验证状态（项目惯例：`fix: X, fix #issue` 或 `chore: 更新依赖`）
- Never 在同一提交中混合"依赖升级"与"功能变更"
- Never 仅凭静态分析宣称运行期行为不变
```

## 6. 所需工具权限

| 工具 | 用途 | 必需性 |
|------|------|--------|
| `Read` | 读取 pom 与受影响源码 | 必需 |
| `Grep` | 影响面侦察 | 必需 |
| `WebFetch`/`WebSearch` | 上游 changelog 查询 | 可选 |
| `Edit` | 应用 diff | 必需 |
| `Bash` | `mvn package` 验证（不运行服务器） | 可选 |

**建议 allowed-tools：** `Read Grep Edit WebFetch WebSearch Bash`

## 7. 使用示例

### ✅ Do This
```
输入："GuizhanLib 要升到 2.4.0"
输出：grep 出全部 net.guizhanss import 点（CrystamaeHistoria.java:38 等）→ 比对新旧包名 → 生成 import 替换 diff → pom 版本号变更 → 5 项运行时验证清单
```

### ❌ Not This
```
输入："升 paper-api 到 1.21"
错误输出：改完 pom 编译通过即宣布完成
正确输出：附 GUI/实体 API 变化侦察 + 强制人工游戏内验证清单（历史证明 1.21 有运行期回归，提交 d52f0e4）
```

## 8. 参考材料

- 依赖声明：`pom.xml:16-231`
- 探测模式：`managers/SupportedPluginManager.java:41-58, 118-138`
- 兜底模式（NoClassDefFoundError）：`CrystamaeHistoria.java:271-277`
- CI 基准：`.github/workflows/maven.yml`（升级后至少须通过 `mvn package`）

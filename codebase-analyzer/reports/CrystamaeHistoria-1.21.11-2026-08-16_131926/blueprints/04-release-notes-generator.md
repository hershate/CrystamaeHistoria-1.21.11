# Skill Blueprint: 版本发布收口器（release-notes-generator）

> 自动生成自 codebase-analyzer
> 分析时间：2026-08-16 13:19
> 源模块路径：`note/release/`（0.1.0.md、0.2.0.md、1.21.11-1.md）、`pom.xml`、`note/README.md`

---

## 1. 基本信息

| 字段 | 值 |
|------|-----|
| **推荐 Skill 名称** | `crystamae-release-closer` |
| **用途** | 从 git 历史聚合生成版本发布说明 + 联动 pom 版本号与 note 索引收口 |
| **AI 替代等级** | 🤖 完全 AI 化（26/30） |
| **实施优先级** | 🥇 Quick Win |
| **源资产** | 3 份既有 release 文档（范式）、git log 规范化提交 |

## 2. 触发场景与关键词

- "发布 0.3.0"
- "收口这个版本"
- "生成发布说明"

**推荐 description 触发词：**
```yaml
description: >-
  Generate CrystamaeHistoria release notes from git history, bump the pom version,
  and close the note index loop. Triggered by: "发布版本", "版本收口", "生成发布说明",
  "release notes", "版本号更新", "发版".
```

## 3. 输入输出契约

### 产物契约（对齐既有 3 份 release 文档结构）

| 产物 | 内容要求 | 范式来源 |
|------|---------|---------|
| `note/release/<版本号>.md` | 版本定位一句话 → 分类改动汇总（按 perf/docs/chore 分组）→ 数据格式兼容性声明 → 引用报告链接 | `note/release/0.2.0.md` |
| `pom.xml` version 字段 | `<version>` 更新（`pom.xml:8`） | `108805c` chore(release) |
| `note/README.md` | 版本列表顶部插入新版本行 + 各索引收口 | `d6d4c01` "索引收口（循环闭合）" |
| 提交序列 | `chore(release): 版本号升至 X.Y.Z` → `docs(note): 发布说明 + 索引收口` | git log |

### 输入

| 输入 | 来源 | 用途 |
|------|------|------|
| `git log <上一版本 tag/commit>..HEAD --oneline` | Bash | 改动清单（提交信息已规范：perf/docs/chore(scope)） |
| 上一版本 release 文档 | Read | 版本基线与遗留项 |
| note/report/、note/audit/ 新增文档 | Glob | 引用链接 |

### 错误场景

| 错误 | 触发 | 处理 |
|------|------|------|
| 版本号格式非法 | 非 `\d+\.\d+\.\d+(-\w+)?` | 拒绝并询问 |
| 无新提交 | range 为空 | 提示用户确认版本必要性 |

## 4. 依赖清单

| 依赖 | 用途 | 关键接口 |
|------|------|---------|
| git | 历史聚合 | `git log --oneline` |
| `pom.xml` | 版本号 | `<version>`（`pom.xml:8`） |
| `note/README.md` | 索引 | 版本列表 + 专项分析 + 轮次三节 |
| 用户全局规则 | 提交规范 | 细粒度提交、禁 co-author |

## 5. Skill 工作流设计

### 建议的 Workflow 步骤
```markdown
### Step 1: 确定版本号与范围
向用户确认版本号；git log 定位自上一 release commit 起的全部提交。
### Step 2: 分类聚合
按 perf(scope)/docs(note)/chore 分组；perf 类提炼量化成果表。
### Step 3: 生成发布说明
note/release/<版本>.md，含兼容性声明（对照实际是否改数据格式）。
### Step 4: 版本号联动
Edit pom.xml <version>。
### Step 5: 索引收口
更新 note/README.md 版本列表与相关索引节。
### Step 6: 细粒度提交
chore(release) 与 docs(note) 分开提交（遵守项目无 co-author 规范）。
```

### 建议的 Constraints
```markdown
- Always 发布说明中的每个论断附 git commit hash 或 note/ 文档相对链接
- Always 声明数据格式兼容性（既有版本均"无数据格式变更，旧存档兼容"）
- Never 混入未在 git 历史出现的改动描述（禁止编造）
- Never 单次提交混合版本号与文档（对齐既有 chore/docs 分离惯例）
```

## 6. 所需工具权限

| 工具 | 用途 | 必需性 |
|------|------|--------|
| `Bash`（git log） | 历史聚合 | 必需 |
| `Read` | 范式文档 | 必需 |
| `Write`/`Edit` | 产物生成 | 必需 |
| `Glob` | note/ 新增文档发现 | 可选 |

**建议 allowed-tools：** `Read Write Edit Bash(git log:*,git status:*), Glob`

## 7. 使用示例

### ✅ Do This
```
用户："发布 0.3.0"
→ git log <0.2.0 commit>..HEAD → 聚合（例：5 个 perf + 3 个 docs）
→ note/release/0.3.0.md（含量化表 + 兼容声明）
→ pom.xml 0.2.0→0.3.0 → note/README.md 顶部插入
→ 2 个规范提交
```
### ❌ Not This
```
在发布说明中写"重构了法术系统"（历史中不存在该提交——编造）
```

## 8. 参考材料

- 范式：`note/release/0.2.0.md`、`note/release/0.1.0.md`
- 版本策略：`note/README.md` 版本发布记录节
- 版本号位置：`pom.xml:8`

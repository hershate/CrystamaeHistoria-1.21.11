# Skill Blueprint: 性能优化循环执行器（perf-optimization-loop）

> 自动生成自 codebase-analyzer
> 分析时间：2026-08-16 13:19
> 源模块路径：`benchmark/`（基准设施）、`note/report/perf/`（9 轮报告范式）、`pom.xml`

---

## 1. 基本信息

| 字段 | 值 |
|------|-----|
| **推荐 Skill 名称** | `crystamae-perf-loop` |
| **用途** | 执行"优化假设 → 基准变体 → 服务器跑分 → 量化对比 → 三连提交"的完整性能优化轮次 |
| **AI 替代等级** | 🧑‍💻 AI 辅助（22/30，人工审核优化方案后执行） |
| **实施优先级** | 🥈 Strategic |
| **源资产** | benchmark/ 4 子目录 + note/report/perf/ 9 轮报告 + git 9 组三连提交范式 |

## 2. 触发场景与关键词

- "开启第 10 轮性能优化"
- "XX 路径感觉有开销，跑一轮优化循环"
- "继续性能优化轮次"

**推荐 description 触发词：**
```yaml
description: >-
  Execute a CrystamaeHistoria performance-optimization round: hypothesis, benchmark
  variant, server measurement, quantified report, and the 3-commit convention.
  Triggered by: "性能优化轮次", "跑基准", "优化循环", "perf round", "性能分析",
  "基准测试", "继续优化".
```

## 3. 输入输出契约

### 轮次产物契约（对齐 0.2.0 第 1-9 轮实证范式）

| 产物 | 格式 | 范式来源 |
|------|------|---------|
| 基准变体代码 | benchmark/src 下新增变体（含真实 raycast/PDC/tick 判定，非微基准） | git `751152e`、`bc0e723`、`99934dc` 等 perf(bench) 提交 |
| 跑分数据 | benchmark/results/ + 服务器内实测（run.sh） | `benchmark/run.sh` |
| 报告文档 | `note/report/perf/round-N.md`（含对比表 + 收敛判定） | round-1..9 |
| 三连提交 | `perf(scope):` 本体 → `perf(bench):` 数据 → `docs(note):` 报告 | git log 任意轮次 |

### 优化红线（`note/README.md`）

安全 / 稳定 / 兼容三红线；无数据格式变更（旧存档兼容）。

### 已完成轮次的量化基线（报告须引用对照）

| 轮次 | 目标路径 | 成果 |
|------|---------|------|
| 1 | 施法前置校验缓存 + SpellMemory 零复制 | 29x / 8x |
| 2 | 施法懒 raycast + 冻结 + 事件重排 | 消除失败路径开销 |
| 3 | 交互路径 ItemMeta 削减 | 8.9x |
| 4 | 机械 tick 判定链 + 拾取点预计算 | 判定链 1010x+ |
| 5 | 机械 tick 备忘录 | 稳态 1034x |
| 6 | 法杖 PDC 单槽局部读取 | 1.6x |
| 7 | gadgets 7 类每 tick 清扫 | 2.0-5.9x |
| 8 | 故事选取稀有度×类型索引 + 配置双解析消除 | 21x / 2.3x |
| 9 | 统计路径 MessageFormat 消除 | 12.4x |

## 4. 依赖清单

| 依赖 | 用途 | 接口 |
|------|------|------|
| `benchmark/run.sh` | 服务器跑分入口 | Bash 执行 |
| `benchmark/server-addon/` | 服务器内基准插件源码 | 既有变体为模板 |
| `mvn package` | 构建（含基准插件） | Bash |
| `note/report/perf/README.md` | 轮次索引（每轮须更新） | Edit |
| 热点候选 | 02 报告 §4/§7 的调用链清单 | 分析输入 |

## 5. Skill 工作流设计

### 建议的 Workflow 步骤
```markdown
### Step 1: 候选热点识别
Read 上一轮报告的"收敛判定"与遗留项；或分析指定路径的每 tick 分配。
### Step 2: 提出假设（人工审核点 🧑‍💻）
列出 2-3 个优化假设及预期量级，等待用户选择。
### Step 3: 编写基准变体
在 benchmark/ 按既有变体模板新增对照（旧实现 vs 新实现同屏对比）。
### Step 4: 构建与跑分
mvn package → run.sh → 采集数据（含 soak 验证 0 异常）。
### Step 5: 量化报告
生成 note/report/perf/round-N.md（对比表 + 红线核查 + 收敛判定）。
### Step 6: 三连提交
perf(scope) → perf(bench) → docs(note)，并更新 perf/README.md 索引。
```

### 建议的 Constraints
```markdown
- Always 优化前先有基准变体（无数据不优化——项目铁律，git log 全部轮次实证）
- Always 保持三红线：不改数据格式、不降安全防御、不破坏行为语义
- Always 性能注释写明量化来源（既有代码范式：ChroniclerPanelCache.java:50-54）
- Never 在无 soak 验证情况下宣布轮次完成
- Never 跳过人工审核直接修改主源码（本 Skill 为 AI 辅助级）
```

## 6. 所需工具权限

| 工具 | 用途 | 必需性 |
|------|------|--------|
| `Read` | 轮次范式 / 热点源码 | 必需 |
| `Write`/`Edit` | 变体代码 / 报告 | 必需 |
| `Bash`（mvn、run.sh） | 构建跑分 | 必需 |
| `Grep` | 热点扫描 | 必需 |

**建议 allowed-tools：** `Read Write Edit Grep Bash(mvn:*,bash benchmark/run.sh:*)`

## 7. 使用示例

### ✅ Do This
```
用户："开启第 10 轮"
→ Read round-9 收敛判定 → 列出候选（如 SatchelListener 拾取路径）
→ 用户选定假设 → 变体 → 跑分（例：1.8x）→ round-10.md → 三连提交
```
### ❌ Not This
```
直接改主源码后补基准（顺序颠倒，违反项目铁律）
```

## 8. 参考材料

- 范式总索引：`note/README.md` 性能优化轮次节
- 报告模板：`note/report/perf/round-9.md`
- 基准设施说明：`benchmark/README.md`
- 既有性能模式清单：`codebase-analyzer/reports/...-2026-08-16_131926/01-architecture.md` §7

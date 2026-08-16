# CrystamaeHistoria 工作流分析报告

> 分析时间：2026-08-16 13:19
> 覆盖：CI/CD 管线、开发/审计/性能工作流（基于 git 历史与 note/ 文档体系）、业务工作流映射、决策树与异常恢复。

---

## 1. CI/CD 管线（`.github/workflows/maven.yml`）

```mermaid
flowchart LR
    A[push / PR<br/>→ master] --> B{commit 以 [CI skip] 开头?}
    B --|是| Z[跳过]
    B --|否| C[actions/checkout@v4]
    C --> D[setup-java@v4<br/>temurin JDK 21<br/>maven 缓存]
    D --> E["mvn install:install-file<br/>lib/Slimefun-5.0.0.jar →<br/>本地仓库 com.github.slimefun:Slimefun:5.0.0"]
    E --> F[mvn package]
    F --> G["upload-artifact@v4<br/>target/CrystamaeHistoria-*.jar<br/>（if-no-files-found: error）"]
```

| 维度 | 值 | 证据 |
|------|-----|------|
| 触发 | push/PR → master；`[CI skip]` 前缀豁免 | `maven.yml` `on:` 块与 `if:` 行 |
| 运行器 | ubuntu-latest | `jobs.build.runs-on` |
| 阶段 | 单 job：vendored 依赖安装 → 构建 → 产物上传 | steps 列表 |
| 特殊依赖处理 | Slimefun 5.0.0 为本地 vendored jar（`lib/Slimefun-5.0.0.jar`，来自 `REF/Slimefun4.1` 构建），CI 手动 install-file | `maven.yml` "Install vendored Slimefun 5.0.0" 步骤；`note/README.md` 维护要点 1 |
| 部署 | **无部署阶段**——产物仅 artifact，发布为手动（GitHub Release 流程在仓库外） | — |
| 回滚 | 无自动化（单体插件 jar 替换即回滚） | — |

**测试矩阵：无 CI 测试阶段**。`pom.xml` 无 surefire 配置、无 test 依赖；`src/test` 不存在。质量保障替代路径见 §2。

---

## 2. 开发与质量工作流（git 历史考古 + note/ 体系）

### 2.1 三条并行工作流（0.1.0 → 0.2.0 实证）

```mermaid
flowchart TD
    subgraph W1[审计工作流 note/audit/ 33 轮]
        A1[round-1.md] --> A2[round-33.md<br/>117+ 项稳定性/安全性/正确性修复]
        A2 --> A3[8 次服务器回归验证]
    end
    subgraph W2[性能工作流 note/report/perf/ 9 轮]
        B1[提出优化假设] --> B2["benchmark/ 编写变体<br/>（真实 raycast/PDC 服务器内基准）"]
        B2 --> B3[mvn package + 服务器跑分]
        B3 --> B4{量化提升?}
        B4 --|是| B5["perf(xxx): 提交优化<br/>+ perf(bench): 数据 + docs(note): 报告"]
        B4 --|否| B6[回滚变体]
        B5 --> B1
        B1 --> B7[round-9 收敛判定 → 闭合]
    end
    subgraph W3[发布工作流 note/release/]
        C1[改动汇总] --> C2[release/&lt;版本&gt;.md]
        C2 --> C3[pom.xml 版本号 bump]
        C3 --> C4[docs(note) 收口提交]
    end
    W1 --> R[0.1.0]
    W2 --> R2[0.2.0]
    W3 --> R & R2
```

**Git 提交规范（实证自 `git log`）**：`perf(core|cast|machines|gadgets|stats|bench)` / `docs(note)` / `chore(release|bench)` 三段式 scope 前缀；每个优化轮次固定 3 连提交：优化本体 → 基准数据 → 报告文档（如 `a3af0c5` → `3aa86c0` → `d6d4c01`）。

**基准设施**（`benchmark/` 目录）：

```
benchmark/
├── build/          构建产物
├── results/        各轮量化数据（note/report/perf 引用）
├── run.sh          服务器启动跑分脚本
├── server-addon/   服务器内基准插件（真实 raycast/PDC/tick 判定实测）
└── src/            基准源码
```

### 2.2 测试策略评估

| 测试类型 | 状态 | 替代手段 |
|---------|------|---------|
| 单元测试 | ❌ 无 | — |
| 集成测试 | ❌ 无 CI 自动化 | benchmark server-addon + 手动服务器回归（8 次，`note/README.md`） |
| E2E | ⚠️ 手动 | 10 分钟 soak 终验（0 异常 0 tick 落后，`note/report/perf/round-9`） |
| 静态保障 | ✅ | spotbugs/jsr305 + jetbrains 注解（`pom.xml:112-134`），`@ParametersAreNonnullByDefault` 全量标注 |

**判断**：测试覆盖度为零，质量完全依赖"审计轮次 + 基准回归 + soak"的人工/AI 混合循环。这是 AI 替代的最大机会点（见 04 报告）。

---

## 3. 业务工作流映射（玩家视角）

### 3.1 核心业务流程清单

| # | 流程 | 一句话描述 | 主要模块 |
|---|------|-----------|---------|
| 1 | 故事发掘 | 将方块物品放入记录者面板，随机获得 lore 故事 | `ChroniclerPanel(Cache)`、`StoryUtils`、`StoriesManager` |
| 2 | 能量提取 | 满故事物品在现实祭坛生成魔法水晶簇 | `RealisationAltarCache` |
| 3 | 法术合成 | 3 种水晶液体 + 催化剂在液化池产出法术板 | `LiquefactionBasinCache`、`RecipeSpell` |
| 4 | 施法 | 法杖装配法术板后左/右键（含 Shift 变体）释放 | `SpellCastListener`、`InstanceStave/Plate`、69 个 Spell |
| 5 | 图鉴收集 | 法术/故事/镀金三图鉴逐步解锁 | `commands/Open*Compendium`、`ItemGroups` 3 个 FlexGroup（`ItemGroups.java:108-122`） |
| 6 | 玩家统计 | 解锁与使用次数持久化 + 排名查询 | `PlayerStatistics`、`commands/GetRanks` |

### 3.2 流程 1+2+3 玩家旅程图

```mermaid
flowchart TD
    Start[玩家获得普通方块物品] --> P1[放入记录者面板]
    P1 --> P2{面板每 tick 判定}
    P2 --|不可记录| E1[物品弹出]
    P2 --|可记录| P3[物品锁定故事潜力<br/>随机 1-5 个槽位]
    P3 --> P4{testChance 通过?}
    P4 --|否| P4
    P4 --|是| P5[随机稀有度+类型故事写入 lore]
    P5 --> P6{槽位耗尽?}
    P6 --|否| P4
    P6 --|是| P7[独特故事解锁+闪电特效<br/>物品发光]
    P7 --> P8[放入现实祭坛]
    P8 --> P9[魔法水晶簇生长]
    P9 --> P10[破坏水晶簇获得 Crystal]
    P10 --> P11[投入液化池]
    P11 --> P12[液体混色+液位上升]
    P12 --> P13{投入催化剂}
    P13 --|空白板+配方匹配| S1[充能法术板诞生]
    P13 --|充能板+同法术| S2[法术板晶能+液量]
    P13 --|不匹配| F1[液体全部销毁]
    P13 --|其他物品+物品配方| S3[魔法造物/背包升级]
    S1 --> P14[法杖配置器装入法杖]
    S2 --> P14
    P14 --> P15[手持法杖施法]
```

### 3.3 决策树：施法完整分支（合并 02 报告 §2.1 细化到条件来源）

```text
条件链（SpellCastListener.java:26 → InstancePlate.java:70-110）：
├── 条件0: e.getHand() == OFF_HAND? (:29-31)            [防御双触发]
├── 条件1: SlimefunItem instanceof Stave? (:34-35)      [物品识别]
├── 条件2: SpellSlot.getByPlayerAndAction != null? (:39-42) [4 栏位映射]
├── 条件3: PDC 反序列化成功? (InstanceStave.java:46-57)  [损坏→空法杖]
├── 条件4: 槽位有法术板? (InstanceStave.java:164-171)   → CAST_FAIL_SLOT_EMPTY
├── 条件5: spells.yml 启用? (InstancePlate.java:75-77)  → SPELL_DISABLED
├── 条件6: crysta >= cost? (:80-82)                     → CAST_FAIL_NO_CRYSTA
├── 条件7: cooldown <= now? (:85-87)                    → ON_COOLDOWN
├── 条件8: 施法回调抛异常? (:99-108)                     → 吞异常仍报成功
└── 条件9: 写回 PDC 成功? (SpellCastListener.java:51-53) → 失败跳过写回
```

**量化业务规则常量**：

| 常量 | 值 | 位置 |
|------|-----|------|
| 冷却/射程/晶能等 | 各法术 SpellCoreBuilder 首参（如 Push: 100s/30/5，`Push.java:21`） | 69 个法术构造器 |
| 故事潜力槽位 | `ThreadLocalRandom.nextInt(minStories, maxStories+1)` | `StoryUtils.java:171-175` |
| 发掘概率 | tier.chroniclingChance / 10000 每 tick | `ChroniclerPanelCache.java:313-314` |
| 稀有度门槛 | StoryChances 累积（和=100 校验） | `StoryChances.java` 构造器 |
| 水晶价值 | C1/U3/R10/E25/M50/X2 | `Crystal.java:19-26` |
| 拾取范围 | 0.3×0.3×0.3 盒 | `ChroniclerPanelCache.java:209-214` |
| 法术清理周期 | 20 tick | `RunnableManager.java:22` |
| 统计落盘周期 | 12000 tick | `RunnableManager.java:25` |

---

## 4. 异常恢复路径（业务层汇总）

| 业务流程 | 风险 | 机制 | 评估 |
|---------|------|------|------|
| 故事发掘 | blocks.yml 被改坏 | 六段校验链跳过非法条目 | ✅ 容错 |
| 液化合成 | 玩家投错催化剂 | 设计性惩罚（清空液体） | ✅ 有意设计 |
| 施法 | 法术实现 bug | 断路器 + 日志限流 | ✅ 不影响他人 |
| 服务器崩溃 | PDC/BlockStorage 半写 | 全部读取路径防御式降级 | ✅ |
| 服务器重启 | 机械状态恢复 | BlockStorage + Cache 重建（onNewInstance） | ✅ |
| 世界卸载 | 临时方块清理失败 | 保留条目惰性重试 | ⚠️ 潜在长期残留（世界永不重载则永不清理） |
| 玩家退服 | 飞行/时间/天气状态 | PlayerQuitEvent + 离线判移除 | ✅ |

---

## 5. 团队协作流程（贡献者视角）

- **上游关系**：本仓库 fork 自 Sefiraat/CrystamaeHistoria（420 commits 的主要作者），`ybw0014`（117，汉化维护者）。当前维护者 Zurker（115 commits，主导 1.21.11 迁移 + 0.1.0/0.2.0）。
- **文档即流程**：`note/` 是流程执行现场——audit/（审计轮次）、report/perf/（性能轮次）、release/（版本收口）三目录 + README.md 索引构成可追溯链。
- **贡献入口**：`CONTRIBUTING.md` + `CODE_OF_CONDUCT.md` 存在（根目录），CI 保障 master 可构建。
- **分支策略**：单 master 直推（近期历史无 feature 分支合并痕迹），风险由"小步提交 + 轮次文档"对冲。

---

## 6. 工作流总结

1. **CI 极简**（build-only），**质量流程文档化人工化**——审计 33 轮 + 性能 9 轮全部留痕于 note/，可复现可回滚。
2. **基准驱动开发**是本项目最成熟的工作流：每轮优化必须先在 benchmark/ 写变体、跑真实服务器、出量化数据，再合入（git log 中 perf(bench) 先行于 perf(core) 或紧随其后）。
3. **缺失环节**：无单元测试、无自动部署、无 PR 强制门禁——为 AI 工作流替代提供了明确空间（04 报告）。

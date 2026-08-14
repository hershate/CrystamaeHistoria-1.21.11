# CrystamaeHistoria 项目分析要点

> 来源：codebase-analyzer 全量静态分析（2026-08-15_011442）
> 完整报告：`codebase-analyzer/reports/CrystamaeHistoria-2026-08-15_011442/`
> 本文件为要点速查版，论断证据（文件:行号）见完整报告。

## 一、项目定位

- Slimefun4 附属插件"魔法水晶编年史"，玩法核心：**方块中发掘故事 → 故事提炼为水晶 → 水晶液化合成法术/魔法物品 → 法杖施法**。
- 三层 fork：原作者 Sefiraat → 汉化维护 SlimefunGuguProject（pull[bot] 自动同步上游）→ 本工作区面向 1.21.11 适配。
- 技术栈：Java 11 + Maven shade 打包 + Paper API 1.19 + Slimefun4 2024.3.1 + InfinityLib（标注待移除）+ EffectLib + bStats + GuizhanLibPlugin（硬依赖，提供汉化与自动更新）。

## 二、架构要点（改代码前必读）

1. **入口**：`CrystamaeHistoria.enable()`（`CrystamaeHistoria.java:154-202`）按固定顺序装配 7 个管理器；全项目经静态门面 `CrystamaeHistoria.getXxx()` 访问管理器。
2. **机械方块统一模式**：`TickingMenuBlock` 子类 + 伴随 `*Cache` + `static Map<Location, Cache> CACHES`；放置时创建（BlockPlaceHandler）、区块加载恢复（onNewInstance）、破坏销毁（onBreak + kill()）。新增机械照抄 `ChroniclerPanel`/`RealisationAltar` 骨架。
3. **法术声明式模板**：新法术 = `tier1/Xxx.java`（6 个固定成员：构造函数用 SpellCoreBuilder、getRecipe、getName(中文)、getLore(中文)、getId(大写)、getMaterial）+ `SpellType` 枚举一行。配方（3 元素 × plate tier）必须全局唯一，否则顶替旧法术（`LiquefactionBasinCache.getMatchingRecipe` 只取首个匹配）。
4. **故事数据**：blocks.yml 条目 schema = tier + elements(9 种 StoryType) + story(name/type/lore/shards)；**shards 数组必须恰好 9 个整数**，顺序固定为 StoryType ID 1-9（ELEMENTAL→PHILOSOPHICAL），写错启动即崩。
5. **持久化四通道**：YAML（blocks/generic-stories 只读带默认合并；player_stats/spells 运行时写）、Slimefun BlockStorage、Bukkit PDC（8 个自定义 PersistentDataType，键名在 `utils/Keys.java`）、Slimefun 研究系统。玩家统计每 10 分钟落盘一次。
6. **运行时状态中枢**：`SpellMemory` 13 张 Map 管理全部法术临时效果，插件关闭时 `clearAll()` 兜底；法术产生的长期状态（飞行/时间冻结/禁刷区等）**必须**登记进 SpellMemory，否则无法清理。
7. **线程模型**：全部逻辑主线程同步（机械 `synchronous()==true`），无跨线程；不要引入异步 tick 而不重写 SpellMemory 并发防护。

## 三、关键数值常量（平衡调整入口）

| 常量 | 值 | 位置 |
|------|-----|------|
| 方块层级发掘概率（万分比/tick） | T1=700 T2=600 T3=500 T4=400 T5=300 | `StoriesManager.java:56-137` |
| 故事稀有度分布（C/U/R/E/M） | T1: 85/15/0/0/0 … T5: 5/30/30/20/15 | 同上 |
| 水晶生长概率 | SMALL→MEDIUM 1/10，MEDIUM→LARGE 1/20 | `RealisationAltarCache.java:122-133` |
| 现实祭坛提取概率 | 1/6 /tick | `RealisationAltarCache.java:157` |
| 镀金碎片倍率 | ×2(75%) ×3(20%) ×4(5%)；护符掉率=rarity.id% | `StoryShardProfile.java:49-66, 46-55` |
| 记录者面板 tier 门槛 | 可处理方块 tier ≤ 面板 tier+1 | `ChroniclerPanelCache.java:123` |
| 定时任务周期 | 特效清理 20t、配置保存 12000t、粒子展示 80t | `RunnableManager.java:18-29` |
| bStats id | 12065 | `CrystamaeHistoria.java:205` |

## 四、工作流要点

- **CI**：GitHub Actions 仅 `mvn package`（无测试、无部署）；产物经 builds.guizhanss.net 分发，插件端 `GuizhanUpdater` 自动更新（仅版本号以 `Build` 开头时生效）。
- **测试**：**零自动化测试**；`/historia test-spell <ID> <power≤5>` 与 `test-wand` 为游戏内人工测试命令。
- **上游同步**：`.github/pull.yml` 自动从 `Sefiraat:master` 合并（assignee ybw0014）；`CODEOWNERS` 要求 code-reviewers 审全部 `.java`。
- **版本兼容历史教训**：1.20.6 兼容曾提交后 revert（`4dc4783`→`74f05dd`）；1.21 修复均为运行期问题（GUI 崩溃 `d52f0e4`、包名迁移 `f0b0223`）——**编译通过 ≠ 兼容**，必须游戏内验证。

## 五、已识别的潜在缺陷（供后续修复参考）

1. `ChroniclerPanel.onBreak()` 未对 `CACHES.remove()` 判空即调 `kill()`（`ChroniclerPanel.java:92-93`）；`RealisationAltar` 有判空，实现不一致。
2. `SpellMemory.removeEntities()` 对 `summonedEntities.get()` 返回 null 无防护（`SpellMemory.java:130`）。
3. 配置 IO 全部静默 `printStackTrace`（`ConfigManager.java:50,59,89,111`），用户无感知。
4. `pom.xml` 版本为 `MODIFIED`，与 CONTRIBUTING.md 声称的 SemVer 不符。
5. 部分 provided 依赖钉在 commit hash（ExoticGarden/Networks），构建可复现性依赖 jitpack 可用性。

## 六、AI 化结论摘要

- 🤖 完全 AI 化（可直接创建 Skill）：**方块故事数据生成（28/30）**、**汉化翻译（26/30）**、**新法术创作（25/30）**、**单元测试生成（24/30）**
- 🧑‍💻 AI 辅助（含人工审查点）：**依赖升级/版本兼容（23/30）**、新物品注册（22/30）
- 👤 人工主导：机械逻辑与数值平衡（12/30）
- 5 个组件的完整 Skill 设计规格见 `codebase-analyzer/reports/CrystamaeHistoria-2026-08-15_011442/blueprints/`

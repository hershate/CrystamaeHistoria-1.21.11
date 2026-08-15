# 性能优化第 8 轮：故事选取索引 + 配置加载双解析消除

日期：2026-08-15
基准数据：[benchmark/results/round-8-server.tsv](../../../benchmark/results/round-8-server.tsv)（服务器内实测，Paper 1.21.11 + Slimefun 5.0.0）
红线核查：安全性 ✅ 稳定性 ✅（服务器回归通过，0 异常）兼容性 ✅（磁盘格式零变更；故事池内容与选取分布不变）

## 问题

1. **每次记录故事全表过滤**：`StoryUtils.addStory` 每次对整稀有度故事表做
   `values().stream().filter(type).collect(...)`（中间列表分配 + 全表扫描）——
   记录者面板每次成功记录（1/chroniclingChance 概率 tick）都执行。
2. **配置文件双重解析**：`ConfigManager.getConfig` 对每个文件先
   `YamlConfiguration.loadConfiguration(file)` 再 `configuration.load(file)`——
   同一 tick 内文件不可能变化，第二次纯属重复解析整个文件（5 个配置文件全部如此）。

## 优化

| # | 变更 |
|---|------|
| 1 | `StoriesManager` 新增 `storiesByRarityAndType` 索引（启动期一次构建，5 稀有度 × 9 类型）+ `getStories(rarity, type)` 查表；`StoryUtils.addStory` 改传 `StoryRarity` 走索引（空池跳过语义保持，含原"空池跳过防每 tick 报错"守卫） |
| 2 | `ConfigManager.getConfig` 删除冗余 `configuration.load(file)`（catch 子句相应收敛为 IOException） |

**语义安全论证**：索引在启动期从既有的 5 张稀有度故事表构建（同一 Story 单例引用），
运行期故事表不可变（仅构造期填充）→ 索引无失效问题；过滤结果与原 stream 路径
同源同序（同为 HashMap values 顺序），随机选取分布不变。

## 量化（服务器内实测，真实 StoriesManager 数据 / 真实 995 键 blocks.yml）

| 场景 | 旧 | 新 | 加速比 |
|------|-----|-----|--------|
| 记录故事按类型选取（COMMON×ELEMENTAL） | 110.00ns/次 | 4.80ns/次 | **22.9x** |
| blocks.yml 加载（995 键） | 79.98ms | 34.60ms | **2.31x** |

启动路径合计省下 blocks.yml + generic-stories.yml + 其余 3 文件的一次重复解析
（约 45ms+）；故事选取是记录者面板推进路径的每次成本（多面板并发记录时叠加）。

## 稳定性验证

Paper 1.21.11 build 132 + Slimefun 5.0.0 实机：插件启用正常（19.45s 完成启动，
含世界生成），全部基准完成，**全会话 0 异常**。

## 变更文件

- [StoriesManager.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/managers/StoriesManager.java)
- [StoryUtils.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/utils/StoryUtils.java)（addStory 签名 internal 变更：仅 requestNewStory 一个调用方）
- [ConfigManager.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/managers/ConfigManager.java)
- benchmark/server-addon（新增 storyPick/configParse 变体）

# 性能优化第 5 轮：机械 tick 判定备忘录（消除 JSON 解析地板）

日期：2026-08-15
基准数据：[benchmark/results/round-5-server.tsv](../../../benchmark/results/round-5-server.tsv)（服务器内实测，Paper 1.21.11 + Slimefun 5.0.0）
红线核查：安全性 ✅ 稳定性 ✅（服务器回归通过，0 异常）兼容性 ✅（纯机械内部字段，无 API 变更）

## 问题（第 4 轮遗留地板）

记录者面板**工作状态**每 tick 的判定成本：`process()` 判定链（第 4 轮已降为单次
元数据克隆）+ `processStack()` 内**再次** `hasRemainingStorySlots(i)` +
`getRemainingStoryAmount(i)`（2 次克隆 + 2 次故事上限 JSON 解析）。现实祭坛每
tick 判定 1 次克隆 + JSON。这些判定在"物品实例未变且未被修改"时**结果恒定**，
但旧实现每 tick 重算。

## 优化：实例判定备忘录（显式失效）

机械内部新增 `verdictItem` 引用 + 判定结果字段。tick 时先做**引用比较**：
- 命中（物品实例相同）→ 直接复用判定，零元数据读取、零 JSON 解析；
- 未命中（新物品实例）→ 单次 `getItemMeta` 全量判定并记录。

**失效点集合（完整性论证——所有会改变判定结果的路径都显式置空备忘录）**：

| 机械 | 修改点 | 处理 |
|------|--------|------|
| 面板 | `makeStoried`（写入新上限） | 立即重判（仅新物品首 tick） |
| 面板 | `processStack` 成功记录故事（`requestNewStory`/`requestUniqueStory`/`rebuildStoriedStack` 修改元数据） | 置空，下 tick 重判（池空跳过时为保守失效，安全） |
| 面板 | `tryInsertItem`/`pushOutItem`/`reject`（换实例/移除/置 AIR） | 换实例→引用比较自动未命中；置 AIR→空槽早退路径 |
| 祭坛 | `processItem`（`removeStory`/`reject`/`setAmount(0)`） | 分支结束后置空 |

`processStack` 直接复用 `process()` 同 tick 已判定的 `verdictHasRemaining/verdictRemaining`
（同 tick 内无中间修改，与旧时序逐值等价）。

**物品实例不可被外部篡改论证**：判定对象是机械 GUI 槽内物品；玩家仅能整体取出
（实例消失）或放入新物品（新实例）；槽内合并不改变判定字段；槽内物品无漏斗通路。

## 量化（服务器内真实插件代码，真实已记录 STONE，完整工作 tick 判定序列）

| 形态 | ns/tick/台 | 相对原始 |
|------|-----------|---------|
| 第 3 轮前（独立 ItemStack 读取 ×3 + JSON ×2） | 2719.35 | 1x |
| 第 4 轮（单次 meta + processStack 独立读取） | 2296.60 | 1.18x |
| **第 5 轮（备忘录命中，稳态）** | **2.63** | **1034x** |

稳态（面板持续工作中绝大多数 tick——记录进度按 `chroniclingChance/10000` 概率
推进，推进 tick 才重判）判定成本趋近于零；每次推进后的一次重判约 1.2μs 摊销。

## 稳定性验证

Paper 1.21.11 build 132 + Slimefun 5.0.0 实机：插件启用正常，全部基准完成，
**全会话 0 异常**。
（基准自我纠错：初版备忘录命中变体的两引用不同实例，永假分支会被 JIT 消除——
已改为同实例后重测。）

## 变更文件

- [ChroniclerPanelCache.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/slimefun/items/mechanisms/chroniclerpanel/ChroniclerPanelCache.java)
- [RealisationAltarCache.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/slimefun/items/mechanisms/realisationaltar/RealisationAltarCache.java)
- benchmark/server-addon（新增 fullWorking 三形态对比变体）

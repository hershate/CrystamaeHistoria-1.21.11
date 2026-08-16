# 性能优化第 44 轮：T5 吸取路径 stream().findFirst() 改直接迭代（地板下的裂缝）

日期：2026-08-17
域：**T5 机械吸取路径取首元素**——round-43 地板判定的首轮复核即
发现残留：面板/祭坛 `tryInsertItem`（每 tick 每 T5 机械）对已按
`Item.class::isInstance` 预过滤的集合用 `stream().findFirst()` 取
首元素——流分配 + spliterator + Optional 包装为纯开销（round-1
零复制扫描族的遗留形态，当时的清扫聚焦 filter 扫描而非取首元素）。

## 实现（本轮提交）

- `ChroniclerPanelCache`/`RealisationAltarCache` 的 tryInsertItem：
  `entities.isEmpty() ? null : (Item) entities.iterator().next()`——
  遭遇序首元素语义与 findFirst 一致（集合已过滤，转型安全）；
- `FragmentedVoid:77` 不改：事件级且形态不同（`Optional<ItemStack>`
  消费）。

## 量化（服务器内真实实体集合，round-44-server.tsv）

| 基准 | 旧（stream findFirst） | 新（iterator next） | 提升 |
|------|----|----|------|
| insertItem.firstElement | 24.67 ns | **2.75 ns** | **8.97x** |

绝对量小（~22ns/tick/T5 机械）但**实测为正**——与 r42 的持平不同，
流分配确实存在且可消除。同实体断言 true；会话 0 SEVERE、0 组失败。

## 判定修订

round-43 的"可测地板"判定**再次被修订**（第三次：33→34、43→44）——
地板之下仍有裂缝，但裂缝形态已从"结构域"退化为"单行惯用法"。
族探针矩阵应增补一行：**流式惯用法**（预过滤集合上的 stream 包装）。
复查节奏的最终形态确认为：**低成本单行惯用法清扫 + 周期性判定轮**
交替，直至连续判定轮零发现。

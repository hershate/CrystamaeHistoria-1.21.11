# 性能优化第 46 轮：枚举 values() 克隆与集合双复制惯用法（cast 染色/tick 消费）

日期：2026-08-17
域：**"枚举 `values()` 克隆 + 集合双复制"惯用法族**——r45 流式惯用法
族闭合后对相邻形态的全库扫描：`values()` 每次调用克隆新数组（数组
逃逸进 stream 时不可被 JIT 消除）、`Collection.stream().toList()` 对
已有集合的二次复制。三处调用点，两处实质一处卫生。

## 实现（本轮提交 e47fa07）

- `Bobulate.processEntities`（cast 路径，每只可染色实体）：`DyeColor`
  数组静态缓存——原实现 `Arrays.stream(DyeColor.values()).count()` +
  `DyeColor.values()[rnd]` 每实体两次数组克隆 + 流计数（克隆逃逸进
  stream，逃逸分析不可消除）；
- `ExaltedFertilityPharo.onExalt`（**tick 级**——由展示架 afterTick
  周期驱动，即 r39 弱缓存覆盖的同一路径）：
  `getNearbyEntitiesByType` 返回 `Collection`，原 `.stream().toList()`
  为每 tick 二次复制 + List 转型；改直接迭代取第 rnd 个元素，零复制，
  均匀随机语义不变（rnd 均匀分布于 [0,size)，迭代序确定）；
  —— 编译器实证：该重载返回 `Collection<T>` 而非 `List<T>`，原
  `.toList()` 兼任类型转换，故采用迭代而非 `get(rnd)`；
- `BalmySponge` 放置处理：`BlockFace` 数组静态缓存（50+ 元素）。

## 量化（服务器内真实枚举/集合，round-46-server.tsv）

| 基准 | 旧 | 新 | 提升 |
|------|----|----|------|
| enumValues.dyePick（两次克隆+stream 计数 vs 静态数组） | 33.60 ns | 4.28 ns | **7.85x** |
| enumValues.faceIter（values() 克隆纯迭代 vs 静态数组） | 2.21 ns | 2.79 ns | 持平（噪声级） |
| pharoConsume.empty（tick 空闲态：stream().toList() vs isEmpty 门） | 50.19 ns | 2.52 ns | **19.92x** |
| pharoConsume.small3（tick 常载态：toList().get vs 直接迭代） | 66.84 ns | 14.75 ns | **4.53x** |

等价性断言：dye=true / faces=true / pick=true（缓存数组逐位一致；
直接迭代与 toList().get 同 rnd 同元素）。会话 COMPLETE=1、CH 插件
错误 0、watchdog 2 次（tunnelBore/loreApply 重负载旧变体批次固有，
同 r45）。

## 方法论发现：values() 克隆的逃逸边界

faceIter 实测**持平**（2.21 vs 2.79ns，差异噪声级）——纯迭代的
`values()` 克隆不逃逸，已被 JIT 逃逸分析整体消除（与 r19 Location
克隆、r38 判定前块快照的结论互证，构成第三次 EA 实证）。**族规则**：
`values()` 克隆仅在**数组逃逸**（传入 stream/返回/二次传递）时产生
实际成本；纯遍历形态无需修改。BalmySponge 变更保留为卫生性改动
（家族一致性，行为不变）。

## 判定

- 流式惯用法族（r44-45）+ 本轮相邻形态（values() 逃逸克隆 /
  Collection 双复制）清扫完毕，cast/tick 路径已知调用点闭合；
- 族矩阵增补两行：**枚举 values() 逃逸克隆**（实质，dyePick 7.85x）、
  **集合双复制 toList()**（实质，tick 路径 4.5-19.9x）；纯迭代克隆
  归入 JIT-EA 边界（免改）；
- 下一轮回到判定轮（族矩阵复核）或继续相邻形态探查。

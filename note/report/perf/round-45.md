# 性能优化第 45 轮：法术 cast 路径 stream 惯用法全库清扫（r44 族扩展）

日期：2026-08-17
域：**法术 cast 路径集合惯用法**——round-44 确立"流式惯用法"族后的
首轮全库清扫：三处法术（EasterEgg/Bobulate/HarmonysSonata）在 cast
回调内对标签集合做每调用重建或流包装取材。

## 实现（本轮提交 cab3668）

- `EasterEgg`：SPAWN_EGGS 列表静态缓存并移出名字循环——原实现每次
  迭代 `getValues()+stream+toList` 三重复制（名字字符数 × 全量复制）；
- `Bobulate`：六个常量标签（WOOL/TERRACOTTA/GLAZED_TERRACOTTA/
  CONCRETE_POWDERS/CONCRETE_BLOCKS/CARPETS）→ 静态列表备忘——原每
  处理一块重建一次取材集合（施法范围内逐块触发）；
- `HarmonysSonata`：`Tag.FLOWERS` 随机取材改直接迭代——原
  `stream().skip(n).findAny()` 的流分配 + spliterator + Optional 包装
  为纯开销（不可变集合迭代序确定，skip(n).findAny 与第 n 元素一致）。

## 量化（服务器内真实 Tag 集合，round-45-server.tsv）

| 基准 | 旧 | 新 | 提升 |
|------|----|----|------|
| spellCast.tagList（每次重建 vs 静态缓存引用） | 318.60 ns | 2.82 ns | **112.98x** |
| spellCast.randomPick（stream skip/findAny vs 直接迭代） | 258.34 ns | 36.62 ns | **7.05x** |

等价性断言：pick=true / list=true（同实体同序）；会话 COMPLETE=1、
CH 插件错误 0、SpellType 全量加载冒烟通过（69 法术含三处改动的
构造器在真实服务器实例化无异常）。

绝对量 ~220-316ns/次，事件级（施法触发，Bobulate 逐块放大），与
r44 同性质——单行惯用法级裂缝，长尾延续。

## 会话记录

watchdog 线程转储 2 次，定位为基准批次固有现象（tunnelBore r5 旧
O(n²) 变体 ~2.7ms/op 与 loreApply 拒绝变体批次连续占用主线程超
10s 触发 Paper Watchdog），非插件行为——对照 r42 会话 4 次 / r44
会话 5 次同源同位置；CH_ERRORS=0。

## 判定

"流式惯用法"族 cast 路径清扫完成：三处调用点全部消除，叠加 r44
的 T5 吸取路径，该族已知调用点闭合。族矩阵"流式惯用法"行标记
完成；下一轮回到判定轮（族探针复核）或新族探查。

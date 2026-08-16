# 性能优化第 24 轮：lore 展示写入组件化——试行后回退（阴性结果）

日期：2026-08-16
域：法杖 lore 写回（每次成功施法）与故事物品 lore 重建（每次提交/移除/提取）
的 legacy→Component 转换假设。
结论：**假设被实测双向推翻（等价性 false + 性能负收益），代码回退**，
基准证据保留。本报告记录完整过程与修正后的瓶颈归因。

## 假设（源自 round-15/23 实测的误读）

- `writePath.staveLoreRebuild` 静态片段化后仍 18.4µs（1.04x 残留）→
  推断瓶颈是 `setLore(List<String>)` 的逐行 legacy→Component 转换；
- 方案：预反序列化为组件常量/缓存，经 `ItemMeta.lore(List<Component>)`
  应用，"每次施法 20 行转换 → 1 行"。

## 实测推翻（Paper 1.21.11 build 132 真实 ItemMeta，最终会话 round-24-server.tsv）

| 基准 | 字符串路径（现行） | 组件路径（被否决） |
|------|----|----|
| loreApply.stave20（20 行） | 25,881.76 ns | **60,418.62 ns（慢 133%）** |
| loreApply.storyN（N 故事） | 24,329.48 ns | **36,075.31 ns（慢 48%）** |
| loreConvert.deserializeOnly（纯转换参照，~16 行） | 2,107.94 ns（~132ns/行） | 140.70 ns |

1. **性能负收益**：`lore(List<Component>)` 的组件列表应用路径在
   Paper 1.21.11 上**比** `setLore(List<String>)` 更重（组件深处理 vs
   字符串内部转换）；
2. **瓶颈归因修正**：纯转换仅 ~134ns/行——18-21µs 的主体是
   ItemMeta lore 字段应用机制本身（Paper 侧），**转换从来不是主导成本**，
   round-15 的 1.04x 残留实为已到 API 边界；
3. **等价性诊断（最终会话）**：`stave(strings)=true stave(components)=false
   stave(item)=false story=false`——legacy 字符串往返相等，但**存储组件
   不逐值等价**（Paper `setLore` 内部构造带额外装饰态处理，如 italic
   缺省语义），物品 NBT 层即已不同；按"用户体验一致"标准亦存疑
   （装饰态差异可能产生可见的斜体差异），且性能独立否决，回退不依赖
   等价性结论。

> **红线口径（用户澄清，2026-08-16）**：兼容性指**用户体验一致 +
> 对外（供其他插件调用的）API 不变**；内部实现/内部调用不必逐方法
> 等价，可自由重构。本轮回退依据是**性能负收益**（独立成立），
> 非内部 API 等价性。该口径自本轮起适用于所有后续轮次。

## 处置

- **代码回退**（三文件：Story/StoriesManager/InstanceStave，工作树还原，
  无需提交——回退即回到 0.4.0+round21-23 状态）；
- 基准保留：standalone（round-24.tsv，真实 adventure 序列化器，转换
  本身廉价的证据）+ 服务器内（组件 vs 字符串路径对打 + 三重等价诊断），
  addon 变体为自包含副本（不依赖被回退 API），可长期复现；
- 本域判定：**lore 应用成本由 Paper ItemMeta 机制构成，属 API 边界**，
  与三轮收敛判定的"setItemMeta 边界"合并。

## 等价性与回归

- 最终部署 jar 为回退后代码（22:06 构建），会话 COMPLETE、0 SEVERE；
- 等价性断言按设计如实施报 false——它验证的正是"被否决方案不等价"。

## 经验沉淀（写入 perf README 方法论）

1. **瓶颈归因须先测转换本体**：`deserializeOnly` 变体（纯转换 vs 带应用）
   应在对"转换成本"下结论前先行——本轮顺序颠倒导致一次完整试错的
   工程成本（实现→部署→实测→回退）；
2. **"新 API 形态更快"是假设非事实**：adventure 组件路径在
   Paper 1.21.11 的 ItemMeta 应用层反直觉地更慢，凡触及 Paper 内部
   序列化边界，必须以真实 ItemMeta 实测为准；
3. 等价性断言再次证明其价值：三重诊断（字符串往返/组件/物品 isSimilar）
   在性能数据之外独立拦截了不兼容方案。

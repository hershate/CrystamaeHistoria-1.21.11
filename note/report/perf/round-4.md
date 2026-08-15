# 性能优化第 4 轮：机械 tick 路径（单次元数据判定链 + 拾取点预计算）

日期：2026-08-15
基准数据：[benchmark/results/round-4-server.tsv](../../../benchmark/results/round-4-server.tsv)（服务器内实测，Paper 1.21.11 + Slimefun 5.0.0）
红线核查：安全性 ✅ 稳定性 ✅（服务器回归通过，0 异常）兼容性 ✅（`StoryUtils` 旧签名全部保留，新增重载）

## 问题

机械（记录者面板/现实祭坛/液化池）每 Slimefun tick 的固定成本：

1. **故事判定链多次读取物品元数据**：记录者面板稳态每 tick 调
   `canBeStoried(item, tier)`（内部 `getByItem` + `isStoried`→克隆 1）+
   `isStoried(item)`（克隆 2）+ `hasRemainingStorySlots(item)`（克隆 3 + Gson 解析
   故事上限 JSON）——**3 次 ItemMeta 克隆 + 1 次 JSON 解析每 tick 每台**。
   现实祭坛每 tick 2 次克隆。
2. **实体拾取扫描中心每 tick 重算**：三台机械的 `getLocation().clone().add(...)`
   （2 次 Location 分配/ tick /台）——机械位置放置后固定。

## 优化

| # | 变更 |
|---|------|
| 1 | `StoryUtils` 新增 ItemMeta 重载：`isStoried(ItemMeta)`、`hasRemainingStorySlots(ItemMeta)`、`getMaxStoryAmount(ItemMeta)`（失败关闭）、`canBeStoried(ItemStack, int, boolean storied)`（接受调用方已读标记） |
| 2 | 记录者面板 `process()`：单次 `getItemMeta` 贯穿判定链；新物品 `makeStoried` 后重取一次（仅首 tick） |
| 3 | 现实祭坛 `process()`：单次 `getItemMeta` 供两条判定 |
| 4 | 三台机械懒缓存 `pickupLocation` 字段（`getNearbyEntities` 不修改该实例，已核实） |

**语义安全论证**：
- 判定顺序与短路条件逐项保持（`canBeStoried` 先于 `makeStoried`；传入的 storied
  标记即 makeStoried 前的值，与旧时序一致）；
- `getMaxStoryAmount(ItemStack)` 旧签名实现原样保留（含 `getStoryLimits` 的
  随机初始化 fallback）；新 ItemMeta 重载对损坏数据按 0（失败关闭，符合审计红线
  方向）——仅影响 PDC 已损坏物品，正常物品路径值恒等。

## 量化（服务器内真实插件代码，真实已记录 STONE）

| 场景 | 旧 ns/tick/台 | 新 ns/tick/台 | 加速比 |
|------|---------------|---------------|--------|
| 记录者面板稳态判定链 | 2784.38 | 1243.04 | **2.24x** |
| 拾取扫描中心计算 | 4.32 | 2.86 | 另消除 2 次 Location 分配/tick/台的 GC 压力 |

剩余地板：`getMaxStoryAmount` 单次调用 1077ns（ItemMeta 克隆 + Gson 解析故事上限
JSON）——每 tick 每台仍在付，是后续轮次的候选（需重设计 JSON 缓存失效策略）。

## 稳定性验证

Paper 1.21.11 build 132 + Slimefun 5.0.0 实机（两次完整启动跑基准）：插件启用正常，
**全会话 0 异常**。
（过程自我纠错：初版 `pickupLocCompute` 新变体误测 `hashCode()` 而非字段读——
修正为 null 检查强迫字段读取后重跑，数据以修正版为准。）

## 变更文件

- [StoryUtils.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/utils/StoryUtils.java)
- [ChroniclerPanelCache.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/slimefun/items/mechanisms/chroniclerpanel/ChroniclerPanelCache.java)
- [RealisationAltarCache.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/slimefun/items/mechanisms/realisationaltar/RealisationAltarCache.java)
- [LiquefactionBasinCache.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/slimefun/items/mechanisms/liquefactionbasin/LiquefactionBasinCache.java)
- benchmark/server-addon（新增机械 tick 基准项）

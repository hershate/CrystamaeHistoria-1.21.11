# 性能优化第 13 轮：剩余全局监听器门控审计（17 监听器收官）

日期：2026-08-16
基准数据：[benchmark/results/round-13-server.tsv](../../../benchmark/results/round-13-server.tsv)（63 变体全量）
红线核查：安全性 ✅ 稳定性 ✅（跑分会话 0 插件异常）兼容性 ✅（门控与原判定严格等价，含运行时断言）

## 背景

第 10 轮处理了 SpellEffectListener 的世界级事件；本轮把**全部 17 个监听器**的
事件入口成本逐一审计收官，补齐遗漏门控。逐监听器审计表：

| 监听器 | 事件（频率） | 审计结论 |
|--------|-------------|---------|
| DisplayItemListener | 漏斗拾取/物品消失（高） | **不做**：实体 PDC 读实测 ~15ns（round-10 数据）；且 ExaltationStand/TrophyDisplay 直接 spawnDisplayItem 绕过 displayItems 表，索引门控有等价性风险 |
| SatchelListener | 实体拾取（高） | 已有 Player 门控 + getByItem（~20ns），足够 |
| MiscListener 放置×2/合成×2 | 方块放置/合成（高） | **做**：isStoried 前置 hasItemMeta 门控（5.8x） |
| MiscListener onPlaceCover | 方块放置（高） | **做**：PAPER 材质门控（1.9x） |
| MobCandleListener | 生物生成（刷怪场极高） | **做**：禁刷区空表早退 |
| EndermanInhibitorListener | 实体传送（高） | instanceof + containsKey 已优 |
| BlockRemovalListener ×4 | 方块破坏等（高） | hasMetadata("ch") 为 Bukkit 边界且便宜 |
| CrystalBreakListener | 方块破坏（高） | 材质首判已优；基座方位判定为必要位置逻辑 |
| SpellEffectListener | （round 10 已做） | — |
| 其余 8 个（ArmorStand/Maintenance/CrystaDowngrade/NetherDraining/PoseChanger/RefractingLens/ThaumaturgicSalts/SpellCast） | 低频或已门控 | 维持现状 |

## 本轮优化（commit 6a9df97）

### 1. isStoried 的 hasItemMeta 门控（4 处）

`onPlaceStoriedBlock`（**每次方块放置**）、`onBlockPlacerStoriedBlock`、
`onTryCraft(CraftItemEvent)`、`onTryCraft(AutoDisenchantEvent)` 原先无条件执行
`isStoried(itemStack)` = 整份 ItemMeta 克隆 + PDC 读取。建筑常态（无 meta 的
普通方块）现由 `hasItemMeta()` 前置短路。

**等价性论证**：Bukkit 契约 `hasItemMeta()==false ⟺ getItemMeta()==null`，
而 `isStoried(meta)` 对 null 返回 false——门控与原判定严格等价。
**运行时断言**（基准内置）：重命名石头（有 meta 无 PDC）新旧双 false；
真实故事石头（有 PDC）新旧双 true，均通过。

### 2. onPlaceCover 材质门控

全部方块隐藏器（货运/能源/网络）材质均为 `Material.PAPER`——非 PAPER 手持
物品免 `SlimefunItem.getByItem`。

### 3. MobCandleListener 禁刷区空表早退

`CreatureSpawnEvent` 在刷怪场每秒数百次；常态（无禁刷区）免去空 keySet 的
迭代器分配。

## 量化结果（Paper 1.21.11 b132 实机，同 JVM 对比）

| 场景 | 旧 ns/次 | 新 ns/次 | 加速比 |
|------|----------|----------|--------|
| 放置事件故事检查（无 meta 普通方块，每次放置） | 43.61 | 7.52 | **5.8x** |
| 放置事件隐藏器检查（非 PAPER 手持） | 11.90 | 6.20 | 1.9x |
| 禁刷区生成扫描（空表） | 3.39 | 2.83 | 1.2x |

## 验证

- `mvn package` 构建通过；跑分会话 COMPLETE、0 插件异常、优雅关停；
  63 变体全量复测历轮无回归。

## 变更文件

- [MiscListener.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/listeners/MiscListener.java)
- [MobCandleListener.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/listeners/MobCandleListener.java)
- benchmark/server-addon（benchRound13，6 变体 + 等价性断言）

## 收敛评估（下一轮）

世界级事件门控审计至此覆盖全部 17 个监听器；热路径（施法/机械/液化池/gadgets）、
冷路径（启动）、事件路径三域均已做完。剩余成本均由 Bukkit/Paper/Slimefun API
边界构成。下一轮应执行：全量终验 + 10 分钟 soak + 收敛判定 + 版本收口（0.3.0）。

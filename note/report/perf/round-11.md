# 性能优化第 11 轮：液化池路径全套优化

日期：2026-08-16
基准数据：[benchmark/results/round-11-server.tsv](../../../benchmark/results/round-11-server.tsv)（含历轮全量 57 变体复测）
红线核查：安全性 ✅ 稳定性 ✅（实机跑分会话 0 插件异常、0 次 "Can't keep up"）兼容性 ✅（终态等价分析见下）

## 本轮优化点（commit 6cd1401）

液化池是放置量最大的机械之一（按容量分级多种），`consumeItems` 每 Slimefun tick
对每台执行。上一循环第 4-5 轮覆盖了记录者面板/现实祭坛，液化池 tick 路径存在
五处未处理成本：

### 1. syncBlock 脏标记（最大项）

原实现在实体循环内对**每个附近实体**（含被弹开的无效物品——它们被 rejectItem
赋随机速度后仍会在拾取盒内停留数 tick）无条件调用 `syncBlock()` 全量写
BlockStorage（contentMap 至多 9 键）。内容未变化的 tick 也在反复落盘。
改为 `contentDirty` 标记：仅 `addCrystamae`（吸收）置脏，tick 结束时统一写一次；
`emptyBasin` 经 `clearBlockStorage` 清键后复位。**终态逐字节一致**。

### 2. 配方类型集索引（O(1)）

`getMatchingRecipe(Set, MagicalPlate)` 原线性扫描 69 个法术配方调
`recipeMatches`（`containsAll`）。新增 `RECIPES_SPELL_INDEX`：以配方 3 类型去重
`EnumSet` 为键（EnumSet 与查询集合按 Set 元素相等契约匹配，哈希一致）。语义
等价论证：查询集合恒为 3 元素（两个调用方均有 `size()==3` 守卫），3 元素集合
被 containsAll ⟺ 集合相等；含重复类型的配方（去重集 <3）天然落不进任何键——
与旧行为一致（List.containsAll 3 元素集永不真）。同键多配方按注册顺序保序
（现存 69 配方类型组合唯一）。

### 3. fillTopThree 单遍选取

三处催化剂处理（空白板/充能板/其他物品）原先各用 1-2 条
`entrySet().stream().sorted(comparingByValue().reversed()).limit(3)` 管线（其他
物品路径为双管线）。EnumMap 至多 9 项，单遍维护前三即可；并列值保持先见者
靠前（与稳定排序相对顺序一致），types/amounts 对齐顺序不变（RecipeItem 匹配
按下标对齐消费）。

### 4. getFillLevel 懒缓存

`consumeItems` 每 tick 调用；-1 哨兵失效模式，`onNewInstance` 恢复路径直写
contentMap 也安全。

### 5. tick 粒子前置分配消除

`LiquefactionBasin.tick` 原每 tick `new DustOptions` + 中心 Location 克隆偏移。
DustOptions 不可变且与位置无关 → 构造期缓存于物品实例；中心点归每方块 Cache
（原 pickupLocation 同点，更名 `centerLocation` 双用途）。**注意**：中心点若驻留
SlimefunItem 实例字段会被同类型多方块共享污染（维护要点 8 红线），已归入
每方块的 LiquefactionBasinCache。

## 量化结果（Paper 1.21.11 b132 + Slimefun 5.0.0 实机，同 JVM 对比）

| 场景 | 旧 ns/次 | 新 ns/次 | 加速比 |
|------|----------|----------|--------|
| syncBlock 写入（每 tick 每附近实体，9 键全量） | 1245.82 | 2.89（脏标记跳过） | **431x** |
| 催化剂 top-3 选取（双 stream 管线 → 单遍） | 676.39 | 90.02 | **7.5x** |
| 配方匹配·未命中（69 项线性扫描 → 索引） | 1270.76 | 9.08 | **140x** |
| 配方匹配·命中（HEAL 组合） | 554.11 | 20.26 | **27.3x** |
| tick 粒子前置（分配 → 缓存读） | 5.97 | 4.04 | 1.5x |

## 兼容性分析（红线）

1. **BlockStorage 终态一致**：脏标记只消除冗余写；`emptyBasin` 的清键路径与
   `onNewInstance` 的恢复路径不变。崩溃窗口内至多丢失当 tick 的最后一次吸收
   落盘（原实现同样不保证崩溃时已 flush——Slimefun BlockStorage 本身为脏标记
   批量落盘模型）。
2. **配方匹配语义**：见上文等价论证；`getMatchingRecipe(Set, MagicalPlate)`
   公共签名保留（委托 `lookupSpellRecipe(Set, int)` 静态方法）。
3. **不可变 List.of** 传入 `RecipeItem.recipeMatches`（只读消费，indexOf/get）。
4. RECIPES_SPELL 注册表保留（启动期 loadConfig 写入路径不变，索引为派生结构）。

## 验证

- `JAVA_HOME=F:/Java/21 mvn package` 构建通过；
- 实机跑分会话（全量 57 变体 + 优雅关停）：**0 插件异常、0 次 "Can't keep up"**；
  missSet 穷举验证（84 组合中确认非配方组合）与索引互验正常。

## 变更文件

- [LiquefactionBasinCache.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/slimefun/items/mechanisms/liquefactionbasin/LiquefactionBasinCache.java)
- [LiquefactionBasin.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/slimefun/items/mechanisms/liquefactionbasin/LiquefactionBasin.java)
- benchmark/server-addon（benchRound11，9 变体）

## 下一轮候选

- 启动路径分解（onEnable 各阶段耗时画像）
- `RECIPES_ITEMS` 物品配方匹配路径（条件复杂，量大时同样可索引）
- `ParticleDisplayRunnable` 方块扫描（低优先级，仅持勺玩家触发）

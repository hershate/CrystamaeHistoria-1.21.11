# 审计第 2 轮：机械缓存数据操作（复制/吞没/竞态/不可信持久化数据）

日期：2026-08-15
范围：`slimefun/items/mechanisms/`（液化池、记录者面板、现实祭坛、棱镜镀金器、法杖配置器、共享基类）+ `magic/spells/core/InstancePlate.java` + `listeners/SpellCastListener.java`

## 已修复（4 个 commit）

| commit | 问题 | 严重度 |
|--------|------|--------|
| `7d7d148` | `DisplayStandHolder.kill()` 先清 BlockStorage 再读展示架 UUID → 必然读不到 → 生成新盔甲架再删掉，真展示架永久残留（每破坏一台液化池泄漏一个实体） | 实体泄漏 |
| `7d7d148` | `getDisplayStand()`：UUID 损坏 IAE / 实体消失返回 null → 链式 NPE | 稳定性 |
| `d081833` | `LiquefactionBasin.onNewInstance` parseInt/valueOf 无防御 → 损坏键使机械**永久失效** | 数据可用性 |
| `d081833` | **`RecipeItem.recipeMatches` 玩家条件绕过**：activePlayer 缺失/离线时 `additionalRequirement` 被静默跳过 → 尊贵物品满级进度门槛形同虚设 | 进度安全 |
| `4b99b32` | 记录者面板：`Material.valueOf` 损坏数据机械失效；`pushOutItem` blockMiddle 为 null 时 NPE（T5 塞满故事物品即触发）；onBreak 缓存 null 时 NPE 中断 → **输入槽物品被吞** | 数据/物品安全 |
| `4b99b32` | 现实祭坛：定义缺失/故事列表空/activePlayer null 的 NPE 链（部分发生在"芽已放置、故事已移除"之后 → 状态不一致）；saveMap 故事查不到整体落盘失败；loadMap 空位置条目 | 数据一致性 |
| `4b99b32` | 镀金器：parseInt 损坏机械失效；gildItem 定义缺失 NPE | 稳定性 |
| `9d9849e` | 法杖配置器：**无 PDC 的充能板（作弊物品）→ EnumMap.put(null) NPE**；重复 super.onBreak | 不可信输入 |
| `9d9849e` | `InstancePlate.tryCastSpell` 先施法后扣费：回调异常 → 消耗/冷却不生效 → 零成本无限重试；异常穿透交互事件链。改为先结算后施法 + 断路器 | 经济安全 |
| `9d9849e` | 施法监听器副手重复事件：成功后又被副手事件弹"施法失败"覆盖提示 | 交互逻辑 |

另统一加固：三个缓存构造的 `activePlayer` UUID.fromString 防御（损坏数据不再阻断构造）。

## 核实为非问题（记录依据）

- `Item.getItemStack()` 返回 live mirror（CraftItemStack.asCraftMirror），液化池对掉落物栈的 setAmount/setItemMeta 修改会写回实体 —— 充能/消耗流程正确（[CraftItemStack 参考实现](https://github.com/squallblade/Spigot/blob/master/src/main/java/org/bukkit/craftbukkit/inventory/CraftItemStack.java)）。
- 相邻液化池/记录者面板的吸收盒（±0.3）不重叠，同一物品实体不会被两台机械同 tick 双消费。
- 液化池 GUI 开启即关闭 + MenuBlock 运输槽为空数组，货运无法向自由槽推入物品，无破坏吞物路径。
- 充能板充能无上限为设计（池容量即上限），非缺陷。

## 遗留观察

1. **Round 4**：`PersistentPlateDataType`/`PersistentStaveDataType`/`PersistentStoryChunkDataType` 反序列化对缺键/异常值的容错。
2. **Round 3**：法杖配置器 GUI 的 shift 点击/越界槽位行为；液化池 `canCraftSatchel` 死代码（未被调用，含 NPE 隐患）；`processBlankPlate` 中无用的 `SlimefunItem.getByItem(itemStack);` 语句。
3. **玩法观察（不改）**：镀金器 3 格牵引可能吸入相邻液化池要用的棱镜水晶；机械破坏惩罚（带液体爆炸、祭坛碎芽）为设计行为。
4. **已知行为变化**：尊贵物品配方的时间/天气/群系谓词现要求放置者在线（失败关闭的代价）。

## 验证

`JAVA_HOME=F:/Java/21 mvn -q package` 构建通过，产物 `target/CrystamaeHistoria-1.21.11-1.jar`。

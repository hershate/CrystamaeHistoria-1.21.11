# 性能优化第 3 轮：交互路径 ItemMeta 操作削减（材质预检 + 写回合批）

日期：2026-08-15
基准数据：[benchmark/results/round-3-server.tsv](../../../benchmark/results/round-3-server.tsv)（服务器内实测，Paper 1.21.11 + Slimefun 5.0.0）
红线核查：安全性 ✅ 稳定性 ✅（服务器回归通过，0 异常）兼容性 ✅（`buildLore()` 无参签名保留，新增重载）

## 问题

1. **`MiscListener.checkCooldown`（LOWEST，每次右键触发）**：对所有非空物品一律
   `getItemMeta()` 克隆 + PDC 读取（`GeneralUtils.isOnCooldown`）。而冷却 PDC
   （`Keys.PDC_ON_COOLDOWN`）的**唯一写入方**是折射透镜
   （[RefractingLensListener.java:53](../../../src/main/java/io/github/sefiraat/crystamaehistoria/listeners/RefractingLensListener.java)，
   材质 `SPYGLASS`）——其余所有物品的右键（吃饭/开箱/放方块…）全部白付。
2. **`MiscListener.onUseScoop`（LOWEST，每次交互触发，含空手/左键）**：一律
   `SlimefunItem.getByItem(主手)` 元数据查询。调光勺全部 4 档材质只有
   `LANTERN`/`SOUL_LANTERN`（CrystaStacks.java:1990-2015 逐一核实）。
3. **施法成功路径双重元数据往返**：`SpellCastListener` 先
   `getItemMeta→setCustom→setItemMeta`（PDC 写回），再 `buildLore()` 内部又
   `getItemMeta→setLore→setItemMeta`——多付一整轮克隆与应用
   （`StaveConfigurator` 两处 GUI 操作同型）。

## 优化（4 文件）

| # | 变更 |
|---|------|
| 1 | `checkCooldown` 增加 `Material.SPYGLASS` 预检 |
| 2 | `onUseScoop` 增加 `LANTERN/SOUL_LANTERN` 材质预检（仍经 `getByItem`+`instanceof` 双重确认，非 SF 物品语义不变） |
| 3 | `InstanceStave` 新增 `buildLore(ItemMeta)` 重载（lore 写入给定 meta，不触发往返）；无参旧签名保留并委托 |
| 4 | `SpellCastListener` 成功路径与 `StaveConfigurator` 两处：PDC 写回与 lore 写共用一次 `getItemMeta/setItemMeta` |

**安全性论证**：材质预检只是把"绝对不可能携带该 PDC/该 SF id 的物品"在读取元数据
之前排除——`getByItem`/`instanceof` 语义不变；冷却 PDC 是插件自用的便捷限流器而
非安全边界（改造客户端在自己物品上伪造冷却键只会取消自己的交互，跳过无害）。

## 量化（服务器内真实 ItemStack/真实插件代码）

| 场景 | 旧 ns/次 | 新 ns/次 | 加速比 |
|------|----------|----------|--------|
| 任意物品右键的冷却检查（**每次右键全局**） | 41.36 | 4.74 | **8.7x** |
| 任意交互的调光勺检查（**每次交互全局**） | 20.12 | 5.64 | **3.6x** |
| 施法成功路径法杖写回 | 21396.30 | 18766.90 | 1.14x（省 ~2.6μs/次成功施法） |

> 服务器宏观收益以冷却检查为最大：每位玩家每次右键（进食/开箱/放置等一切行为）
> 从 41ns 元数据操作降为 5ns 材质比较，且消除一次 ItemMeta 克隆的 GC 压力。
> 跨启动方差说明：单次启动内的旧/新同 JVM 对比有效；跨启动绝对值有 ±30% 漂移
> （JIT/GC 状态），表中为同一次启动内的对比。

## 稳定性验证

Paper 1.21.11 build 132 + Slimefun 5.0.0 实机（新插件 jar 部署）：插件启用正常，
全部基准变体运行完毕，**全会话 0 异常**。
（过程中的自我纠错：首次跑基准时编译失败中断了部署链，服务器残留旧 jar 导致
`NoSuchMethodError`——重新部署后全绿。）

## 变更文件

- [MiscListener.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/listeners/MiscListener.java)
- [InstanceStave.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/magic/spells/core/InstanceStave.java)
- [SpellCastListener.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/listeners/SpellCastListener.java)
- [StaveConfigurator.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/slimefun/items/mechanisms/staveconfigurator/StaveConfigurator.java)
- benchmark/server-addon（新增 interact/write-back 基准项）

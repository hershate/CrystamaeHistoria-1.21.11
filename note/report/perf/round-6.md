# 性能优化第 6 轮：法杖 PDC 单槽局部读取（施法交互路径）

日期：2026-08-15
基准数据：[benchmark/results/round-6-server.tsv](../../../benchmark/results/round-6-server.tsv)（服务器内实测，Paper 1.21.11 + Slimefun 5.0.0）
红线核查：安全性 ✅ 稳定性 ✅（服务器回归通过，0 异常）兼容性 ✅（PDC 编码零变更——仅读取方式变化；`InstanceStave(ItemStack)` 旧构造保留）

## 问题

施法交互（`SpellCastListener`）每次都对法杖做**全量** PDC 反序列化（4 槽法术板，
实测 3.3-4.1μs）——而冷却中/缺晶能/空槽的失败交互（战斗中高频出现）只需要
**本槽**那块板的数据。

## 优化（3 处，含一次设计迭代）

| # | 变更 |
|---|------|
| 1 | `PersistentStaveDataType.getSlotPlate(ItemMeta, SpellSlot)`：以 `TAG_CONTAINER_ARRAY` 读取原始容器数组，仅反序列化匹配槽位的法术板；损坏守卫与 `fromPrimitive` 同构 |
| 2 | `InstanceStave.forSlot(stack, slot, meta)` 单槽工厂 + `forWriteBack(stack, slot, mutatedPlate, meta)` 全量写回工厂（合并已扣减的本槽板；**全量重读失败返回 null 跳过写回**——绝不能以空映射覆写法杖清掉其余槽位）；新增 `InstanceStave(ItemStack, ItemMeta)` 公开构造 |
| 3 | 监听器：整个交互**仅一次** `getItemMeta` 克隆，前置读取与成功写回复用同一 meta 快照 |

**设计迭代（自我纠错）**：首版失败路径 1.72x 但成功路径读取翻倍（每次成功多一次
`getItemMeta` 克隆 + 单槽读，7419 vs 3598ns）——引入 meta 快照线程化后成功路径
读取仅 +5%（4382 vs 4159ns），且旧实现在成功路径本就要第二次 `getItemMeta` 写回，
新实现整个交互 meta 克隆 2→1 次，成功路径总成本实为净下降。

**语义安全论证**：
- 失败关闭同构：目标槽位损坏/缺键 → 同样的 `IllegalStateException` → 空法杖降级；
- 成功路径全量重读失败（其余槽位损坏）→ 跳过写回保持原 NBT（旧行为：整杖判空
  无法施法；新行为：合法槽位可施法但不持久化——仅影响已损坏物品，无数据丢失）；
- meta 快照复用前提：法术回调不触碰施法者手持物品元数据（各法术仅作用于世界/
  实体；主线程单线程无并发写）。

## 量化（服务器内真实插件代码，4 板满法杖）

| 场景 | 旧 ns | 新 ns | 变化 |
|------|-------|-------|------|
| 失败前置路径读取（冷却/缺晶能/空槽） | 3329.00 | 2253.84 | **1.48x**（上一次启动实测 1.72x） |
| 成功路径读取总量 | 4158.70 | 4381.55 | +5.4%，但 meta 克隆 2→1 次，监听器级总成本净下降 |

施法交互路径至此的三层复合：第 2 轮懒 raycast（失败 0 raycast）→ 第 3 轮写回合批 →
第 6 轮单槽读取 + 单次 meta 克隆。

## 稳定性验证

Paper 1.21.11 build 132 + Slimefun 5.0.0 实机（两次启动，含设计迭代前后各一次）：
插件启用正常，**全会话 0 异常**。

## 变更文件

- [PersistentStaveDataType.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/utils/datatypes/PersistentStaveDataType.java)
- [InstanceStave.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/magic/spells/core/InstanceStave.java)
- [SpellCastListener.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/listeners/SpellCastListener.java)
- benchmark/server-addon（新增 staveCast 读取变体）

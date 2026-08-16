# 性能优化第 28 轮：法杖存储 v2 扁平编码（第五轮循环第 3 轮）

日期：2026-08-16
域：**法杖 PDC 编码**——施法前置单槽读取（每次施法交互）、成功写回
（每次成功施法：全量反序列化 + 序列化）、法杖配置器装配。
红线（用户澄清口径）：用户体验一致 + 旧存档可读（同键双读）。

## 优化点

v1（`PersistentStaveDataType`）：每板一个子容器（槽位名 STRING + 板容器
内 4 键：tier/spell/crysta/cooldown）——4 板满配一次序列化 **24 次
PDC 键操作**。v2（新 `PersistentStaveV2DataType`）：单容器 5 键
（槽位名/法术 id 各自 NUL 连接 STRING + tier/crysta int[] +
cooldown long[]，均 PDC 内建原语），键操作 24 → 5。

## 实现（4b2b44e + da0d919）

- **同键双读**：与 v1 共用 `PDC_STAVE_STORAGE`（v2 原语 TAG_CONTAINER，
  v1 为 TAG_CONTAINER_ARRAY）——v2 类型读 v1 值抛 IAE 经
  `DataTypeMethods` 断路器转 null 后回退 v1；写 v2 即覆盖同键值，
  自动迁移，无第二键无残留。
- 单槽读取 `readSlotPlate`：槽位串定位索引仅构建目标板（保持 round-6
  的失败路径局部读取语义）；v1 值回退 `getSlotPlate`。
- 全部读写点切换：InstanceStave 三工厂（构造/forSlot/forWriteBack）、
  SpellCastListener 写回、StaveConfigurator 两处、TestWand。
- 损坏守卫：长度不匹配/未知槽位/未知法术 ISE 失败关闭（与 v1 同级，
  调用方既有 ISE 捕获降级不变）。

### 服务器测试拦截的红线级缺陷（已修，da0d919）

首轮会话在 v1 法杖上触发热路径异常：`readSlotPlate` 的裸
`PersistentDataContainer.get(key, TAG_CONTAINER)` 在 v1 值（ListTag）上
被 Paper 抛**裸 IllegalArgumentException**（InstanceStave.forSlot 仅捕获
ISE）——若未拦截，**所有 v1 法杖的施法前置路径将异常**。全量读取路径经
DataTypeMethods 断路器天然安全，本单槽路径为裸调用需显式 IAE 防御。
修复后回退 v1 读取，同键双读语义闭合。教训：**裸 PDC 调用必须走
断路器或自防御**，已作为编码域方法论。

## 量化（服务器内真实 PDC，round-28-server.tsv）

| 基准 | v1 | v2 | 提升 |
|------|----|----|------|
| stavePdc.deserialize4.r28（4 板全量反序列化） | 3,452.72 ns | 1,421.13 ns | **2.43x** |
| stavePdc.serialize4（4 板满配序列化） | 1,646.46 ns | 605.00 ns | **2.72x** |
| stavePdc.singleSlot（施法失败前置单槽读取） | 1,947.36 ns | 1,258.25 ns | **1.55x** |

施法路径影响：前置判定（每次交互）-0.7µs；成功写回（每次成功施法）
读+写共 -3.1µs（22µs 总路径中 PDC 份额减半以上）；法杖 NBT 负载同步
缩小（4 板：24 条目 → 5 条目）。

## 等价性与回归

- 服务器内断言：fullRead（v1/v2 物品全字段读数一致——tier/crysta/
  cooldown/spell 四板全覆盖）/ singleSlot（v1 回退与 v2 直接路径一致）/
  migration（写回覆盖后同键为容器类型且读数正确）**全 true**；
- 会话 COMPLETE、0 SEVERE、0 基准组失败。

## 兼容性

- v1 法杖完全可读（同键双读）；施法/配置器/命令任何写路径触碰后即迁移；
- 对外 API 零变更（InstanceStave 工厂签名不变）；
- 降级到旧版本无法读 v2 法杖（从未承诺的方向性，同 round-26/27）。

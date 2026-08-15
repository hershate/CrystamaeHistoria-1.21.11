# 审计第 4 轮：PDC 反序列化与不可信数据

日期：2026-08-15
范围：`utils/datatypes/` 全部类型 + 全部 PDC 读取调用方（InstanceStave、法杖配置器、液化池、回忆水晶格、姿态调整器、收纳袋）
威胁模型：**物品 NBT 可被改造客户端注入任意字节**（创造模式创造栏数据包携带完整 NBT，含 PublicBukkitValues）；区块/实体 PDC 可能损坏；`/sf cheat` 可产出缺键物品。

## 已修复（3 个 commit）

### `9db17c0` 基础类型（安全关键）

| 问题 | 修复 |
|------|------|
| **`LocationDataType` 无过滤 Java 反序列化（RCE 攻击面）**：用于物品 PDC（回忆水晶格），恶意客户端注入的字节流经 `BukkitObjectInputStream.readObject()` 可实例化 classpath 上任意 Serializable 类（gadget 链） | `FilteredObjectInputStream` 覆写 `resolveClass` 类白名单（java.lang/java.util/数组）。Location 以 ConfigurationSerializable Map 序列化，合法对象图全在白名单内；编码格式不变，兼容历史数据。另校验结果确为 Location |
| `DoubleArrayDataType` 长度字段无校验：负长度 → NegativeArraySizeException；超大长度 → **OOM 拒绝服务** | 长度非负且与实际字节量一致才接受；截断/损坏转带上下文的 ISE |

### `52e6e3c` 业务类型失败关闭

| 类型 | 原损坏行为 | 现行为 |
|------|-----------|--------|
| PersistentPlateDataType | 缺键拆箱 NPE、非法法术名 IAE | 带上下文 ISE（失败关闭） |
| PersistentStaveDataType | 槽位名缺失/非法 NPE/IAE、null 板入 EnumMap NPE | 带上下文 ISE |
| PersistentStoriesDataType | **null 故事塞入列表 → 下游连锁 NPE** | 跳过缺键/非法稀有度/已删除故事条目 |
| PersistentStoryChunkDataType | 拆箱 NPE、switch(null) NPE、世界缺失崩溃 | 逐字段校验，坏条目跳过 |
| PersistentSatchelInstanceType | 缺键拆箱 NPE、null 用户名致下次保存抛异常 | 保守默认值（id=0/tier≥1/未知） |
| PersistentPoseType | AIOOBE、布尔拆箱 NPE | 指明缺失分量的 ISE |

### `1746e4c` 断路器与调用方防御

- `DataTypeMethods.getCustom/hasCustom`：原语类型错配（伪造标签）的 IAE 按 无数据/false 失败关闭，不再穿透全部读取方。
- `InstanceStave`：法杖 PDC 损坏按空法杖降级（告警），不穿透施法事件链。
- 法杖配置器：异常充能板退还；液化池：异常充能板销毁并告警；回忆水晶格：读取/世界解析失败按"路标不可用"（含 world==null NPE 修复）；姿态调整器：姿态读取异常按无造型、枚举名非法回退 HEAD/RESET。

## 核实为非问题

- `BooleanDataType`（非 1 即 false）、`PersistentUUIDDataType`（长度 Preconditions 校验）已具备防御，未改。
- `DataType.INTEGER_ARRAY`（Bukkit 原生）无解析逻辑，配合 `SatchelInstance.setAmounts` 的第 3 轮加固已闭环。

## 兼容性说明

- LOCATION/DOUBLE_ARRAY 的**写入编码未变**（满足 note/README.md 维护要点 5 的历史数据兼容要求）；仅读取侧增加校验与白名单。合法历史数据读取路径不变。

## 验证

`JAVA_HOME=F:/Java/21 mvn -q package` 构建通过。

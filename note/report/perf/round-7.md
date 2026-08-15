# 性能优化第 7 轮：gadgets 每 tick 模式清扫（7 类全覆盖）

日期：2026-08-15
基准数据：[benchmark/results/round-7-server.tsv](../../../benchmark/results/round-7-server.tsv)（服务器内实测，Paper 1.21.11 + Slimefun 5.0.0）
红线核查：安全性 ✅ 稳定性 ✅（服务器回归通过，0 异常）兼容性 ✅（BlockStorage 持久化键/格式零变更；新增内存缓存均在破坏时清理）

## 问题（7 个 gadget 类的每 tick 固定成本）

1. **每 tick 字符串解析**：MobFan 每 tick 2 次 `BlockStorage.getLocationInfo` +
   `BlockFace.valueOf` + `UUID.fromString`；MobLamp 每 tick 1 次字符串读取 + UUID 解析
   （且推挤中心点在怪物循环内每怪物分配一次）。
2. **每 tick 重复 Location 分配**：各 gadget 每 tick 2-6 次 `block.getLocation()`
   （查表键/落盘/清理各来一次）。
3. **每 tick 新建粒子配置**：ExpCollector/MobMat/MobTrap 每 tick `new DustOptions`
   （ExpCollector 另有 `Color.fromRGB` 计算），无实体时也照常分配。
4. **每次变换复制数组**：MysteriousTicker 每次随机变换 `materials.toArray(new Material[]{})`
   复制整个集合。
5. **循环内重复查询**：MobMat 每**实体**做一次 `Bukkit.getPlayer(uuid)`。

## 优化

| 类 | 变更 |
|----|------|
| MobFan | 新增 `directionMap`/`ownerMap` 内存缓存（放置/GUI `setDirection`/`onNewInstance` 三处写入，破坏清理）；tick 改纯查表 |
| MobLamp | 所有者懒缓存（首个 tick 读 BlockStorage 一次，null=已解析但缺失/损坏→失败关闭）；推挤中心移出怪物循环；破坏清理 |
| ExpCollector | 静态 `DUST_OPTIONS`；单次 `getLocation` 复用（查表/写入/落盘共用键） |
| MobMat | 静态 `DUST_OPTIONS`；单次 `getLocation`；`Bukkit.getPlayer` 移出实体循环（每 tick 一次） |
| MobTrap | 静态 `DUST_OPTIONS`；单次 `getLocation` |
| CursedEarth | 单次 `getLocation`（计数表键/清理/写入共用；中心点另克隆一次防键污染） |
| MysteriousTicker | 构造期预生成 `Material[]`（集合构造后固定）；单次 `getLocation` |

**语义安全论证**：
- MobFan 缓存的失效集合完整：方向仅经 `setDirection`（GUI）/放置变更，两者都同步
  写缓存与 BlockStorage；所有者仅放置时写入；破坏时双清。缺键（BlockPlacer 放置）
  → 缓存缺失 → tick 失败关闭跳过，与原 BlockStorage 缺键语义一致。
- MobLamp 懒缓存：`CH_UUID` 放置后不变；`containsKey`+null 值区分"未解析"与
  "解析失败/缺失"，损坏数据仍失败关闭。
- 懒缓存/查表键均用同一 Location 语义（world+坐标 equals/hashCode）。

## 量化（服务器内真实 BlockStorage/真实方块）

| 场景 | 旧 ns/tick/个 | 新 ns/tick/个 | 加速比 |
|------|---------------|---------------|--------|
| MobFan 前缀（字符串读+解析 → 查表） | 97.18 | 41.85 | **2.32x** |
| MysteriousTicker 材质抽取（toArray → 预生成） | 44.04 | 7.44 | **5.92x** |
| 每 tick Location 分配模式（4+2 次 → 1+1 次） | 29.68 | 11.18 | **2.65x** |

另消除：每 tick 的 `DustOptions`(+`Color.fromRGB`) 分配 ×3 类、MobMat 每实体一次
`Bukkit.getPlayer`、MobLamp 每怪物一次中心点分配——大型装置群（100+ gadget）下
的 GC 压力削减为主收益。

## 稳定性验证

Paper 1.21.11 build 132 + Slimefun 5.0.0 实机：插件启用正常，全部基准完成，
**全会话 0 异常**。

## 变更文件

`slimefun/items/gadgets/` 下 7 类：[MobFan](../../../src/main/java/io/github/sefiraat/crystamaehistoria/slimefun/items/gadgets/MobFan.java)、
[MobLamp](../../../src/main/java/io/github/sefiraat/crystamaehistoria/slimefun/items/gadgets/MobLamp.java)、
[ExpCollector](../../../src/main/java/io/github/sefiraat/crystamaehistoria/slimefun/items/gadgets/ExpCollector.java)、
[MobMat](../../../src/main/java/io/github/sefiraat/crystamaehistoria/slimefun/items/gadgets/MobMat.java)、
[MobTrap](../../../src/main/java/io/github/sefiraat/crystamaehistoria/slimefun/items/gadgets/MobTrap.java)、
[CursedEarth](../../../src/main/java/io/github/sefiraat/crystamaehistoria/slimefun/items/gadgets/CursedEarth.java)、
[MysteriousTicker](../../../src/main/java/io/github/sefiraat/crystamaehistoria/slimefun/items/gadgets/MysteriousTicker.java)
（FragmentedVoid/GreenHouseGlass 勘察后确认现状已达标，未改动）

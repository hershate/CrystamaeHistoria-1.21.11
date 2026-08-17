# 性能优化第 63 轮：FallingBlock 的 BlockData 静态复用（卫生级清扫）

日期：2026-08-17
域：**BlockData 创建形态**——探针角度（与 r42-62 互异）：`createBlockData`
每调用新建 vs 静态缓存复用。全库唯一站点 `SpellUtils.summonMagicFallingBlock`
（`Material.createBlockData()` 每方块一次），唯一调用方 PlutosDecent
陨石大招（每方块随机选四黑石材质之一，高等级球体 ~2100 方块/施法）。

## 实现（本轮提交 d244d98）

- `SpellUtils.summonMagicFallingBlock` 新增 **BlockData 重载**（内部
  API，Material 版保留并委托——签名不变）；
- `PlutosDecent` 四材质 `BlockData[]` 静态缓存（`MATERIALS` 列表
  保留），spawnBlocks 直接以缓存实例生成；
- 复用安全性：`BlockData` 为不可变配置（Bukkit 文档形态），同一实例
  跨多次 `spawnFallingBlock` 复用正确（服务器内 3 连生成材质断言
  true）。

## 量化（服务器内，round-63-server.tsv）

| 基准 | 旧（每生成 createBlockData） | 新（缓存引用） | 提升 |
|------|----|----|------|
| fallingBlock.dataCreate | 65.09 ns | 3.93 ns | **16.56x** |

等价性：缓存实例与新建实例 `getAsString`/Material 一致 + 跨 3 次生成
复用正确 true。绝对量 ~61ns/方块——高等级陨石每次施法省 ~0.13ms，
低等级 ~3µs，**事件级边际卫生**。

## 边界分类

- `Cascada` 的 `block.getBlockData()` 为真实方块数据（必需语义）；
- 实体移动 API（pull/push）形态为 setVelocity 单次调用（必要语义）；
- 枚举 `getById` 为 6 元素线性循环（~ns，不属优化域）。

## 会话记录

COMPLETE=1、CH 插件错误 0、watchdog 2 次（基准批次固有）。

## 判定

族矩阵增补行：**BlockData 每生成创建——卫生级（16.56x，事件级边际）**，
可复用成员全部闭合（唯一站点）。第十二轮循环状态：r62 实质 + r63
卫生；下一轮判定轮换新角度。

# 性能优化第 76 轮：驻留条目每 tick 重复解析域（成熟晶簇）

日期：2026-08-18
域：**驻留条目的每 tick 重复解析形态**——探针角度（与 r1-75 互异）：
长期存活的映射条目在其生命周期内每 tick 重复执行的解析/构造工作。
命中成员：`RealisationAltarCache.tryGrow`——晶簇条目**存活至被破坏
为止**（成熟 LARGE_AMETHYST_BUD 长期驻留），每株每 tick：
`BlockPosition.getBlock()`（世界→chunk→Block 解析）+ 粒子中心
`getLocation().add()` 两次分配 + 镀金株 `new DustOptions`（r59 族）。
全部为位置恒定（构造即定）的重复解析——纯浪费。

族普查：`SpellMemory.blocksToRemove`（每秒 1 次节奏，r49 球面扫描可
瞬时数百条，但 1s 稀释后 ~30µs/s，边界不动）；`noSpawningAreas`
（事件级）；其余映射无每 tick 解析成员——族唯一热成员闭合。

## 实现（本轮提交 6b34964）

- `RealisedCrystalState` 增加驻留解析缓存字段 `cachedBlock` /
  `particleLocation`（tryGrow 首次访问时解析，位置恒定无失效协议）；
- 三个每 tick 粒子方法改收 `Location`（生成事件级调用保留 Block
  便捷重载）；
- 镀金 `Particle.DustOptions(Color.YELLOW, 2)` 静态常量化
  （不可变值对象跨调用共享，r59 族惯例）；
- `tryGrow` 补 `isEmpty` 早退（r1/r75 守卫模式一致性）。

**红线保持**：粒子调用的节拍、数量、类型完全不变（视觉语义）；
`getType()` 每 tick 仍读（生长转换与防御移除判定不变）；缓存 Block
引用为位置+世界包装，跨 chunk 卸载/重载安全（CraftBlock 语义）。

## 量化（服务器内，round-76.tsv）

| 基准 | 旧（每 tick 重复解析） | 新（缓存命中） | 提升 |
|------|----|----|------|
| crystalResidency.old_fullTickBody（成熟晶株每 tick 体） | 39.70 ns | 24.65 ns | **1.61x** |
| iso 块解析（BlockPosition.getBlock） | 10.74 ns | 4.36 ns | 2.46x |
| iso 粒子中心 Location | 15.73 ns | 6.21 ns | 2.53x |
| iso 镀金 DustOptions | 18.21 ns | 2.89 ns | 6.30x |

绝对量 ~15ns/株/tick——**卫生级**（r42/r47 先例；50 株驻留每 tick
省 ~0.75µs）。注：Paper 的 `getBlockAt` 实测 ~10ns（直接 chunk 访问，
比 NMS 直觉便宜一个量级）——域矩阵增补 Paper 块解析成本锚点。

等价性：缓存解析与新鲜构造的 Block/Location/材质/DustOptions
**逐位一致 true**。

## 会话记录

COMPLETE=1、CH 插件错误 0、watchdog 2 次（基准批固有伪象）、
**242 变体**（234+8），断言行 3 处 false 均为文档化预期
（round13/24/35）——实机回归通过。

## 判定

族矩阵增补行：**驻留条目每 tick 重复解析——卫生级（1.61x）**。
第十七轮循环开启轮：族唯一热成员一次闭合。下一轮判定轮换新角度。

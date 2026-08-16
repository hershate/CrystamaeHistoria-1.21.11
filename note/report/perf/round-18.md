# 性能优化第 18 轮：法术周期效果与周期任务路径收官

日期：2026-08-16
基准数据：[benchmark/results/round-18-server.tsv](../../../benchmark/results/round-18-server.tsv)（服务器实机，Paper 1.21.11 b132 + Slimefun 5.0.0）
红线核查：安全性 ✅ 稳定性 ✅（0 插件异常；看门狗 5 次归因见下）兼容性 ✅（无数据格式变更）

## 优化域：法术周期效果与周期任务（runnables 全域）

runnables/ 与 runnables/spells/ 共 6 个周期任务逐类精读：

| 任务 | 周期 | 核查结论 |
|------|------|---------|
| TunnelBoreRunnable | 每 tick × range×20 次/施法 | **O(n²) 块扫描去重（本轮修复）** |
| SpellTickRunnable | 每周期/施法 | 已是廉价形态（单次 getPlayer + 断路器） |
| FloatingHeadAnimation | 每 tick/工作面板 | 头部姿势增量（API 边界，视觉必需） |
| ParticleDisplayRunnable | 每 80 tick | 见不做项 |
| TemporaryEffectsRunnable | 每秒 | round-1 已零复制化；空表残余 ~60ns/s |
| SaveConfigRunnable | 每 10 分钟 | 冷路径 |

## 量化结果（ns/op，中位数）

| 基准 | 旧 | 新 | 提升 |
|------|---:|---:|-----:|
| tunnelBore.blockScan.r3（法杖等级 3，343 块） | 211,679 | 2,786 | **76.0x** |
| tunnelBore.blockScan.r5（法杖等级 5，1331 块） | 3,239,043 | 11,933 | **271.4x** |

TunnelBore 的 radius = 法杖等级（1-5）、iterations = range×20：等级 5 施法
期间旧实现每 tick 花费 ~3.24ms 于纯无效的 `List.contains` 去重（三重循环
坐标组合构造上互异）与 1331 个 BlockPosition 中间对象分配；新实现 ~12µs。

## 变更内容

1. **TunnelBoreRunnable.run**：BlockPosition 列表 + contains 去重 → 直接
   坐标遍历 `world.getBlockAt`，访问顺序与原集合构建序一致（与第 17 轮
   BatteringRamGoal 同型的既有上游模式，两处一并清理完毕）。
2. **过时注释修正**：TunnelBore/Runnable 类注释 "Removed due to issues"
   与事实不符（法术在 SpellType 注册且可达），更正为实际语义描述。

## 等价性验证

- 半径 3/5 两档：旧去重列表与新直接遍历的坐标序列**逐项一致（true）**。
- 破坏判定（blockCanBeBroken）与粒子效果两法同侧，未纳入计量。

## 域内不做项论证

- **ParticleDisplayRunnable 的调光勺 11³ 光源扫描**（每持有人每 4s）：
  特性本身要求枚举半径内全部 LIGHT 方块，Bukkit 无按类型区域查询 API，
  区块快照更重；仅持有调光勺的玩家触发（罕见路径），~200µs/4s 可接受。
- **TemporaryEffectsRunnable 12 项清理的空表早退**：合计残余 ~60ns/s，
  低于可测阈值。
- **SpellTickRunnable / FloatingHeadAnimation / SaveConfigRunnable**：
  核查无可测优化点（前者的离线判定已是单次 getPlayer 廉价形态）。

## 基准设施观察

看门狗 5 次：2× round-8 YAML 旧疾 + 1× round-15 计时批次连续占线程 +
2× round-18 **旧变体自身**（3.24ms/op × 600 次/批连测必然连续占主线程
~10s——恰为本轮移除的成本；游戏内单迭代仅 ~3ms，不触watchdog）。
全部为基准现象，数据有效。

## 变更文件

- src：runnables/spells/TunnelBoreRunnable、magic/spells/tier1/TunnelBore（注释）
- benchmark：CHPerfBench.benchRound18（r3/r5 四变体 + 坐标序列等价性断言）
- commit：见 git log（perf core + bench 合并提交）

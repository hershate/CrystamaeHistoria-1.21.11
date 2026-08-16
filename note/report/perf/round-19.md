# 性能优化第 19 轮：热循环 Location 分配消除（粒子路径）——域内实测收敛

日期：2026-08-16
基准数据：[benchmark/results/round-19-server.tsv](../../../benchmark/results/round-19-server.tsv)（服务器实机，Paper 1.21.11 b132 + Slimefun 5.0.0）
红线核查：安全性 ✅ 稳定性 ✅（0 插件异常；看门狗 5 次均为已知基准现象：2× round-8 YAML、round-15/18 计时批次与本轮球面计时）兼容性 ✅（无数据格式变更）

## 优化域：热循环 Location 分配消除

排查发现 `clone().add` 模式遍布代码库；仅**循环内**（每粒子/每点一次）的
实例属热点：ParticleUtils.displayParticleEffect 四方法（所有 per-tick 粒子
调用方共用）、ChroniclerPanelCache.summonParticles（每工作面板每 tick）、
ChillWind 施法球面（~840 点/次）。改造为单次克隆可复用坐标 + 基准坐标
`set`（独立偏移不累积，spawnParticle 同步读取无逃逸）。

## 量化结果（ns/op，中位数）——如实记录

| 基准 | 旧 | 新 | 提升 |
|------|---:|---:|-----:|
| particle.sphere840（ChillWind 球面） | 16,735 | 14,190 | **1.18x** |
| particle.burstAlloc（10 粒子爆发） | 161.25 | 159.44 | 1.01x（噪声级） |
| particle.panelSummon（面板每 tick 2 粒子） | 30.41 | 36.79 | 噪声级（±20% 波动） |

**域判定：分配削减已到可测阈值之下。** 无观察者时 spawnParticle 极廉价、
JIT 逃逸分析大量消除短命克隆，Location 分配消除在实测中接近零差值；
仅大循环（840 点）可见 1.18x。改动保留（结构性减少分配与 GC 压力原理上
有益，等价性已证），但本轮证明**继续扫荡同类模式的边际收益可忽略**。

## 等价性验证

固定偏移序列（10 组）下旧 clone().add 与新基准 set 的位置序列**逐项一致
（true）**；随机偏移语义不变（每粒子以基准 + 独立偏移计算，不累积）。

## 不做项论证

- **per-cast 单发克隆**（Animaniacs/BatteringRam/Cascada 等约 40 处）：
  每次施法一次、非循环，实测阈值之下（本轮 burstAlloc 已证）。
- **AirNova 120 弹射物生成的循环克隆**：summonMagicProjectile 实体生成
  （API 边界）主导，克隆占比可忽略。
- **spawnParticle 调用次数**：视觉契约（粒子数量即效果），红线不动。
- **drawLine/drawCube/getLine**：已是坐标式或单次分配形态。

## 变更文件

- src：utils/ParticleUtils、slimefun/items/mechanisms/chroniclerpanel/
  ChroniclerPanelCache、magic/spells/tier1/ChillWind
- benchmark：CHPerfBench.benchRound19（六变体 + 位置序列等价性断言）

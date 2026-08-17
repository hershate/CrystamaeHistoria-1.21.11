# 性能优化第 66 轮：判定轮——玩家视图克隆/teleport 形态探针（零发现）

日期：2026-08-17
性质：判定轮（第十三轮循环收敛计数 1/2，0.13.0 后用户再触发开启）。
探针角度与 r42-65 互异：**玩家集合视图克隆形态、teleport API 形态**。

## 探针结果（全库扫描 + 逐点复核，零可行动发现）

1. **`world.getPlayers()`（每调用新 ArrayList 克隆）**：全库**零命中**
   ——唯一的玩家迭代为 `ParticleDisplayRunnable` 的
   `Bukkit.getOnlinePlayers()`（不可变视图零克隆，每 80 tick 一次）；
2. **teleport 形态**：6 处调用点（AbstractVoid/Gyroscopic/LeechBomb/
   SummonGolem 召回、EndermansVeil/LavaLake 施法者传送）均为
   **同世界主线程施法级**调用——同步 teleport 正确且廉价；
   `teleportAsync` 语义为跨世界/区块加载的异步预备，同世界场景
   反而引入延迟时序变化（语义风险）；
3. 附带核验：`getCasterAsPlayer().teleport`（LavaLake）为即时施法
   回调（同 tick，施法者必然在线，r5 审计口径内）。

## 判定

本轮零发现（两组探针全空）。第十三轮循环收敛计数 **1/2**；下轮
（r67）以新角度复核，若再零发现则按 r44 准则宣告第十三轮循环收敛
并执行下一版本收口。

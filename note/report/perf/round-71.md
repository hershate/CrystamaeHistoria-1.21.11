# 性能优化第 71 轮：判定轮（重复状态应用 + 世界时间查询形态）

日期：2026-08-18
域：**判定轮**（第十五轮循环收敛 1/2）。探针角度（与 r1-70 互异）：
A. 重复状态应用形态——持续法术对同一实体的 `addPotionEffect` 等状态
重应用频率；B. 世界时间查询形态——gadgets 每 tick 的
`TimePeriod.isLight`/`world.getTime`。

## 角度 A：重复状态应用形态——零发现

全库 12 个 `applyPositive/NegativeEffects` 调用点逐一核对 tick 间隔
（`makeTickingSpell` 第 4 参）：

| 法术 | 形态 | 重应用节拍 | 分类 |
|------|------|-----------|------|
| TimeCompression | ticking interval=**20t** | 1 秒 | 玩法语义节拍（AOE 光环 1s 标准） |
| Quake | ticking interval=**20t** | 1 秒 | 同上 |
| TimeDilation | ticking interval=**20t** | 1 秒 | 同上 |
| ChillWind | ticking interval=**5t** | 0.25 秒 | 设计的反应性节拍（缓速云） |
| Shroud / HealingMist / Deity / Ravage / Prism / ImbueVoid | instant | 单次 | 非重复形态 |
| AntiPrism / EarthNova | projectile 命中 | 单次/命中 | 非 tick 重复 |

判定：**不存在逐 tick 状态重应用形态**——重应用节拍即光环玩法语义
（1 秒标准节拍，客户端 buff 计时行为与离开残留时长均由该节拍定义）。
节流化会改变 HUD buff 计时锯齿形态与离开后残留时长（±k tick），
属用户可感知差异——语义锁定，拒绝（先例：r37 权限缓存/r65 射线
复用同判）。剩余的 `PotionEffect` 构造（~10ns/次）与 `entrySet()`
迭代为噪声级（EA 边界内），`addPotionEffect` 本体为 API 边界。

## 角度 B：世界时间查询形态——零发现

`TimePeriod.isLight/isDay/isNight` 为 2-3 次长整数比较（~ns），唯一
每 tick 调用方 GreenHouseGlass 且 `testChance` 短路在前（节拍门控
通过才求值时间）——无优化空间。

## 判定

两个互异新角度均零发现——第十五轮循环收敛计数 **1/2**。r70 开启轮
命中后，域内再无相邻形态；下一轮判定轮继续轮换角度（收敛 2/2 后
按循环准则收口版本）。

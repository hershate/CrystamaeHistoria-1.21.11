# 性能优化第 78 轮：判定轮（getLocation 动态链 + 块解析残留形态）

日期：2026-08-18
域：**判定轮**（第十七轮循环收敛 2/2）。探针角度（与 r1-77 互异）：
C. 全库 tick 路径 `getLocation()` 克隆/偏移链残留复核（r76 同族收尾）；
D. `BlockPosition.getBlock()` 每 tick 重复解析残留复核。

## 角度 C：getLocation 链形态——零发现

全库 25 处 `getLocation().add/clone/subtract` 站点逐点分类：

| 类别 | 站点 | 判定 |
|------|------|------|
| 施法路径（玩家/实体位置） | Gyroscopic/LeechBomb/Tracer/EndermansVeil/HarvestMoon/ChillWind/Hellscape/Hearthstone/Compass/Cascada/EscapeRope/HolyCowGoal | **动态位置**——玩家/实体每 tick 移动，缓存语义不成立；施法事件级 |
| 事件级一次性 | RefractingLensListener/ExaltedSeaBreeze/Cascada spawn/ArmourStandUtils 重置/DisplayStandHolder 生成/RealisationAltarCache:277/281 | 冷 |
| 周期低频 | ParticleDisplayRunnable（4s 一次每光源块） | 冷 |
| 已缓存驻留 | pickupLocation（r4）/centerLocation（r11）/blockMiddle/晶簇 particleLocation（r76） | ✅ 闭合 |

判定：静态位置驻留者已全部缓存化，残留链全部为动态语义或事件级
——域闭合。

## 角度 D：块解析残留——零发现

`BlockPosition.getBlock()` 全库仅剩 2 处：`SpellMemory:293`（到期
条目一次性移除，1s 节奏）/`RealisationAltarCache:151`（r76 缓存
初始化，每条目一次）——无每 tick 重复解析成员。

## 判定

两个互异新角度均零发现——**r77+r78 连续判定轮零发现（角度互异），
按 round-44 准则宣告第十七轮循环收敛**。循环形态：r76 卫生清扫
1 轮 + 判定 2 轮。收口版本 0.18.0（终验 + soak 见
[note/release/0.18.0.md](../../release/0.18.0.md)）。

## 循环总账（第十七轮）

| 轮次 | 性质 | 结果 |
|------|------|------|
| 76 | 卫生清扫 | 驻留条目重复解析：成熟晶簇每 tick 体 1.61x + Paper 块解析成本锚点 |
| 77 | 判定 | 液化池 tick 全形态/gadget 扫描——零 |
| 78 | 判定 | getLocation 动态链/块解析残留——零 |

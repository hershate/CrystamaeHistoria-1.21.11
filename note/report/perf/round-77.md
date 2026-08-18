# 性能优化第 77 轮：判定轮（液化池 tick 全形态审计 + gadget 扫描复核）

日期：2026-08-18
域：**判定轮**（第十七轮循环收敛 1/2）。探针角度（与 r1-76 互异）：
A. 液化池 tick 全形态审计——面板（r70/73/76）、祭坛（r76）、镀金器
（r35）均已审计，液化池 tick 体自 r11/r40 后未整体复核；B. gadgets
每 tick 实体扫描站点复核（r7 之后的形态残留检查）。

## 角度 A：液化池 tick 全形态审计——零发现

`LiquefactionBasinCache.consumeItems()` 每 tick 体逐项核验：

| 步骤 | 现状 | 判定 |
|------|------|------|
| `getCenterLocation()` | 懒缓存（r11，一次构造） | ✅ |
| `getNearbyEntities(0.3³, Item)` | 每 tick 无条件扫描 | **玩法语义**——扫描即投入物品的拾取机制（液化池无输入槽，扔入即消费），节流=拾取延迟=用户可感知；空域成本 r35 已锚定"Paper 空域实体扫描近免费" |
| `SlimefunItemResolver.resolve` | WeakHashMap 弱缓存（r40，177x） | ✅ |
| `contentDirty` 判定 | 脏标记落盘（r11，431x） | ✅ |
| `getFillLevel()` | fillLevelCache 记忆化（r11） | ✅ |
| `summonBoilingParticles` | fill>0 且 1/5 概率——沸腾视觉语义 | 边界 |

## 角度 B：gadget 扫描复核——零发现

gadgets 全部 `getNearbyEntities*` 站点（CursedEarth 随机 tick 生成
门控 / MobLamp / EnderInhibitor / MobFan / ExpCollector /
FragmentedVoid）逐点核验：**扫描即这些机械的主动功能**（驱怪/收经验/
吸物品——不扫描即不工作），形态已由 r7（解析缓存/常量提举）/r11/
r40 优化，API 成本由 r35/r55 锚定——无形态残留，无节流空间（节流即
改变反应延迟=玩法语义）。

## 判定

两个互异新角度均零发现——第十七轮循环收敛计数 **1/2**。机械 tick
四件套（面板/祭坛/镀金器/液化池）+ gadgets 全部审计闭环。

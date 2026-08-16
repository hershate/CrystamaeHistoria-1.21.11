# 性能优化第 39 轮：展示架 afterTick 解析弱缓存化（复查轮实质发现）

日期：2026-08-17
域：**展示架 tick 路径**——`Stand.onTick` 每 tick 调 `afterTick`
（周期门控只覆盖位置检查，不覆盖 afterTick），两个子类
（`ExaltationStand` 尊崇架 / `TrophyDisplay` 战利品架）每 tick 每架
执行 `SlimefunItem.getByItem`——对 Slimefun 物品为完整 ItemMeta + PDC
读取。**round-7 gadget 清扫与 round-11 审计均未覆盖此路径**（复查
节奏的第三次实质回报：34/38/39）。

## 实现（d5952e2）

- `Stand` 基类新增 `resolveDisplayItem(Item)`：展示物品实体 → 已解析
  `SlimefunItem` 的 **WeakHashMap** 弱缓存；
- 生命周期闭合（零失效协议需求）：
  - 实体存续期内物品不替换（架位换物 = 新实体新键）；
  - 条目随实体回收自动清理（无 round-7 类无界增长）；
  - 主线程单线程访问；
- 两个子类的 afterTick 切换到缓存解析（`ExaltedItem.onExalt` 的
  位置 clone 保留——ExaltedHarvester/SeaBreeze 变异安全性所需）。

## 量化（服务器内真实掉落实体 + 真实 Slimefun 物品，round-39-server.tsv）

| 基准 | 旧（每 tick getByItem） | 新（弱缓存命中） | 提升 |
|------|----|----|------|
| standTick.resolveItem | 1,304.99 ns | **7.32 ns** | **178x** |

实际影响：每个放置物品的展示架每 tick 省 ~1.3µs（20 tps ≈ 26µs/s/架）；
装饰/自动化场景多架并存时累计可观。

## 等价性与回归

- 服务器内断言：缓存解析与直读一致（多次命中 + 移除后重解析）= true；
- 会话 COMPLETE、0 SEVERE、0 组失败。

## 过程记录

- addon 编译错误（`Slimefun` 类未限定）第三次因 `;` 链而非 `&&` 链
  绕过——**已三次同型失误**，build.sh 的 CI 化（pipefail + 显式退出
  检查）从"待办"升级为"应尽快做"；本轮起本人约定：build.sh 一律
  `> log 2>&1; echo BUILD=$?` 单独成步核对后再续链。

## 结论

复查节奏持续有效：34（~283,000x）/ 38（噪声级卫生）/ 39（178x）——
"穷尽判定需持续接受复查"的结论本身也在被持续验证。

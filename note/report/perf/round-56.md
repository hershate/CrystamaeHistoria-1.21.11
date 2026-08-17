# 性能优化第 56 轮：判定轮——循环内重复调用/字段重取探针（零发现，第十轮循环收敛）

日期：2026-08-17
性质：判定轮（第十轮收敛计数 2/2）。探针角度与 r42-55 互异：
**循环体内重复无参方法调用/字段重取**（全库脚本扫描 for 循环体内
同一调用 ≥2 次的站点，共命中 29 处，逐点分类）。

## 分类结果（全部零可行动发现）

1. **语义独立使用（非重复）**：Nova 系法术（Air/Earth/Fire/Frost/
   Poison/Hellscape/FanOfArrows）的 `middle.clone()` ×2——两次克隆
   各自独立加偏移生成不同粒子点位，非冗余；
2. **JIT 内联/提升的字段读（噪声级）**：`ThreadLocalRandom.current()`
   ×2-4（单例读）、`getCaster()`/`getWorld()`/`getItem()`/
   `getAmount()`/`getLevel()` 等 getter 链——C2 内联后为 1-2ns 字段
   读且可提升出循环（与 r19/r38/r46/r47/r50 的 JIT 边界结论五证
   一致）；
3. **已缓存/事件级冷路径**：FlexGroup 的 `getMaterial()`/`getSpell()`
   （r21 记忆化构造期）、PDC 序列化的 `getBlockPosition()`（r26/29
   扁平编码事件级）、EphemeralWorkBench/LuminescenceScoop/BlockVeil
   事件处理器；
4. **边界备注**：`PlutosDecent`/`Chaos` 的 `getLocation()`/`getBlock()`
   ×2——其一逃逸进 API（必需语义），另一为 EA 可消除的短命对象。

## 收敛宣告（第十轮循环，r44 准则达成）

- r54（BlockStorage/菜单/字符串角度）+ r56（循环重复调用角度）
  **连续两轮零发现且角度互异**；
- 第十轮循环总账（r53-56）：实质清扫 1 轮（53：事件级 getByItem
  材质门控 2.81-3.00x）、阴性边界 1 轮（55：ByType 切片，Paper 1.17+
  实体扁平化）、判定轮 2 轮（54/56 零发现）；
- **第十轮性能优化循环于 round-56 收敛闭合**，版本收口 0.11.0；
- JIT 边界证据链现累计五证（r19/r38/r46/r47 + r50/r56）——C2 下
  短命分配与字段重取的清扫价值为负，插件侧性能面维持"结构域已
  闭合、剩余为惯用法长尾 + 平台架构边界"的形态。

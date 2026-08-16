# 性能优化第 42 轮：getBlockData 双读归一（卫生性变更，族边界确认）

日期：2026-08-17
域：**tick 路径的 `getBlockData` 族**——第 41 轮总账展望的新族清扫。
全库 12 处调用点核验：法术效果（Cascada/HarmonysSonata——施法期短）、
工具事件（BalmySponge/Displacer/LuminescenceScoop）、视觉契约
（MobCandle/animateLight）、已单读（GreenHouseGlass）、召唤 AI
（BatteringRamGoal——r17 已扫路径的必要读）——唯一双重读形态：
**ExaltedHarvester.onExalt**（instanceof + cast 各一次）。

## 实现（本轮提交）

`ExaltedHarvester.onExalt` 双读归一为单次 `getBlockData`（每 tick 每
随机点少一次调用）；判定语义不变。

## 量化（服务器内真实作物方块，round-42-server.tsv）

| 基准 | 双读（旧） | 单读（新） | 结论 |
|------|----|----|------|
| harvesterTick.blockData | 58.29 ns | 59.85 ns | **持平（噪声内）** |

**如实定级：卫生性变更而非性能收益**——实测揭示 `getBlockData` 为
~30ns 的 NMS 数据引用读取（Paper 侧缓存），与 `getByItem` 的 ~1.3µs
（ItemMeta + PDC 全量读）不同族量级。单双读判定一致性断言 true。

## 族判定

`getBlockData` 族**确认边界**：单次读取本就接近免费，双读形态的
消除无实质收益。这延续并强化了长尾变薄的观察——复查发现的每代
新族（r34 落盘 ~283,000x → r39/40 解析 178x → r42 块数据持平）成本
量级递减，指示**性能面正逼近真实的地板**。

## 回归

会话 COMPLETE、0 SEVERE、0 组失败。

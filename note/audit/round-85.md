# 审计第 85 轮：types 与 machines 基类边界复查（判定轮 1/2）

日期：2026-08-19
范围：全新探针角度——本地移植基类层（`types/RefillableUseItem`、
`types/Stand`、`machines/MenuBlock/Preset/TickingMenuBlock`）的
边界复查（r34 曾读 MenuBlock 族；本轮补 types 族 + 边界一致性）。

## 结果

| 基类 | 核对 | 判定 |
|------|------|------|
| **RefillableUseItem**（LuminescenceScoop/SpiritualSilken 消费） | `refillItem`：PDC getOrDefault 带默认（伪造安全）+ 上限钳制 `Math.min(usesLeft+refill, max)`（**r26 溢出钳制语义在 refill 侧同样在位**）；updateLoreUSES_LEFT 行匹配替换/空 lore 初始化双路径 | ✓ |
| **Stand** | onFirstTick 损坏 UUID 不登记（r7 修复注释在案）+ random tick 种子；onTick 无物品空转守卫 + **跨世界 distance 守卫（r70 已核）**；itemMap 破坏清理（r7） | ✓ |
| **MenuBlock 族** | r34 全读（dropItems 无条件/transport 空数组）；r36/r58 补 BlockMenuPreset.clone 放行语义 | ✓（既往覆盖） |

**零发现**：基类层与既往修复/核验完全一致；refill 的钳制+默认值
防御此前未被单独点名，本轮确认在位。

## 判定

零发现（计数 1/2，与 r84 互异：基类边界形态 vs 物品族消费形态）。

## 验证

纯代码判定轮（无服务器启动/无进程占用）；业务端口 25565 未触碰。

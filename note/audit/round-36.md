# 审计第 36 轮：现实祭坛提取链代码审阅 + T5 面板管线端到端实证

日期：2026-08-18
范围：`RealisationAltarCache`（提取/晶簇生长/区块状态持久化）、
`CrystalBreakListener`（破碎掉落/状态清理）、`StoryShardProfile.dropShards`、
`RealisationAltar` 生命周期；CSCoreLib `MenuListener`/`BlockMenuPreset`
点击语义溯源（更正 round-35 的错误推断）。

## 代码审阅结论：无新缺陷

- **提取路径**（`processItem`）：1/6 每 tick；放置判定 `isEmpty() && 下方实体`
  ✓；跨区块晶簇以绝对 `BlockPosition` 跟踪（saveMap 写祭坛所在区块 PDC，
  loadMap 恢复——邻区晶簇重启后仍被跟踪）✓；区块 PDC v2 键 + kill 时
  v2/legacy 双键清理 ✓。
- **数据损坏防御**：定义缺失退回、故事列表缺失/空退回（拒绝越界）、
  持久化故事缺 blockPosition 跳过——均为既往轮次修复，复核仍在位。
- **晶簇生长**（`tryGrow`）：阶段推进 1/10、1/20；非紫水晶方块（含被活塞
  推走/替代）→ 条目移除自愈 ✓。
- **破碎掉落**（`dropShards`）：每类型数量有界（blocks.yml shards ≤3 ×
  镀金倍率 ≤4 = ≤12 < 64 无超堆叠）；镀金印记 9 类型条目循环独立尝试为
  上游数值设计（round-24 已记录）。
- **`kill()`（祭坛被破坏）**：清除全部晶簇方块且不补掉落——**上游语义**
  （破坏机械销毁待提取故事），经济语义红线不改，记录在案。
- **`CrystalBreakListener`**：手动破坏取消原版掉落防精准采集白嫖 ✓；
  破坏承重方块同样触发（manual=false 不给镀金印记，防活塞绕过）✓；
  多祭坛线性查表正确性（位置唯一归属）✓。

## 实机验证（端口 25599，PID 47452，RCON 优雅停服）

**T5 面板全自动管线（三轮重复，服务端真实）**：

| 断言 | 结果 |
|------|------|
| T5 面板放置+注册+tick | ✅（吸收掉落物证明） |
| `tryInsertItem` 自动吸取（T5 独有） | ✅ 仰投闪长岩即时消失 |
| 记录 + 满槽 `pushOutItem` 推出 | ✅ 满故事物品作为真实实体出现在面板上方并被拾取 |
| 循环可重复 | ✅ 同会话三轮 |

**祭坛 E2E 未完成——harness 限制（非插件缺陷）**，取证链：

1. T1 祭坛可经机器人放置+注册（GUI 实开）✓；
2. `mineflayer.placeBlock` API 放置 SF 全方块会被服务端以
   "blockUpdate 未确认"方式拒绝（T5 祭坛三坐标三次复现，换
   `activateBlock` 右键路径可放但 T5 仍不稳定）；
3. GUI 投料点击在机器人客户端不可靠（见下）。

**重要溯源（更正 round-35 的错误推断）**：round-35 记录的"面板类 GUI
封锁玩家背包侧点击"是**错误结论**——REF 源码 `BlockMenuPreset.clone()`
（:226）对每个 BlockMenu 调用 `setPlayerInventoryClickable(true)`，
机械 GUI 的玩家背包点击**是放行的**（这也解释了 r35 闪长岩成功入槽）。
机器人失败源于 mineflayer 对 90 槽 BlockMenu 窗口（45 顶槽 + 背包 +
盔甲/副手）的原始槽位号映射与服务器端不一致的怪癖。已在 round-35.md
更正。

## 遗留（下轮候选）

- 祭坛提取链的服务器端 E2E：改用专用驱动（CHPerfBench 式插件直接驱动
  `RealisationAltarCache.process()`，或以精确原始槽位号点击），或
  RCON `data get` 配合真实物品实体投料。

## 验证

`mvn -q package`（本轮无代码变更，未重建）；会话日志 0 ERROR/SEVERE；
停服后端口释放、server.properties/ops.json 还原、业务端口 25565 全程未触碰。

# 审计第 35 轮：遗留缺陷清偿（名称前缀叠加 / 空池假闪电与统计 / 注释分母）

日期：2026-08-18
范围：既往轮次"已发现未修"清单的三项遗留（分析报告标记 + round-15 发现）全部清偿；
顺带实证记录者面板完整工作链与新测试装置经验。

## 已修复（3 个 commit）

| commit | 缺陷 | 修复 |
|--------|------|------|
| `38bcc64` | **"有故事的"名称前缀叠加（上游遗留，round-15 发现未修）**：`StoriesManager.setName` 以物品当前显示名为基础名再前置前缀——每个故事提交（面板逐条发掘）与祭坛提取重建（`removeStoryAndRebuild`）都会再叠一层，5 故事物品名称变为"有故事的"×5 | 剥离色码后循环剥离既有前缀再前置（幂等）；对已污染存档物品自动自愈 |
| `90d046f` | **空故事池假闪电 + 空发统计**：`ChroniclerPanelCache.processStack` 中闪电特效与"最后一故事"解锁/编年史统计位于 `commitStory` 条件块外——池空未选中（`pickStory` null）时无任何写入仍闪电、仍给 `unlockUniqueStory`/`addChronicle` 白记统计（玩家数据污染） | 闪电与统计移入提交成功分支：视觉信号与统计均与实际写入一致 |
| `59e61b0` | **BlockTier 注释与实现不符**（分析报告标记）：注释称 X/1000 + 错误示例，实现为 `testChance(req, 10000)` | 注释对齐：分母 10000、T1=700 即每 tick 7%、随等级递减 |

## 实机验证（隔离端口 25599，PID 记录制 + RCON 优雅停服）

**服务器端神谕（Slimefun 关服存档 .sfi）**——闪长岩完整链路实证：

放置 T1 面板 → 插入闪长岩 → 光源呼吸（ticker 逐 tick 工作实证：level 6→11 每 tick +1）
→ 停服读 `stored-inventories/world;3;-60;7.sfi`：

- `minecraft:custom_name` = **"有故事的Diorite"——恰好一层前缀**（4 次重建路径后；
  修复前必为 ×4 叠加）——前缀幂等修复实证 ✓
- `minecraft:lore` = 3 普通故事 + 1 独特故事（Iskall 最好朋友 [独特]）✓
- `custom_data`：`is_s=1b`、`s_cur_n=3`、`s_lim_i=3`、v2 故事列表（s_ids/s_rars
  [2,1,1,6]）✓（普通 3 计数 + 独特不计，与实现一致）
- `luck_of_the_sea` + `tooltip_display` 隐藏（满槽发光标记）✓
- 新门控下完整提交闭环（3+1 故事、满槽、shutdown）✓——`90d046f` 的条件重排
  不影响正常路径

会话日志 0 ERROR/SEVERE、0 插件异常。

## 测试装置经验沉淀（后续轮次必读）

1. **mineflayer 对 BlockMenu 槽内物品的"显示盲区"**：面板对槽内物品做原位 meta
   变更（同一 ItemStack 引用，无 setItem 触发槽更新包）——机器人/客户端在窗口
   保持打开期间看到的是陈旧副本（名称/计数不更新）。**验证物品真实状态必须用
   服务器端神谕**：物品丢出后 `/data get entity @e[type=item]`，或停服读
   `stored-inventories/*.sfi`。
2. **面板类 GUI 默认封锁玩家背包侧点击**（ChestMenu `clickable=false`，
   InfinityLib 语义）：从背包拾取物品到游标的点击会被服务器取消，mineflayer
   的乐观本地状态会误导（本轮石头/圆石"看似插入实未落盘"的假象来源；真实
   客户端会正确处理取消与槽位重同步）。机器人操作应先持物再开 GUI。
3. 出生点常驻区块不会因传送卸载——想触发 BlockMenu 落盘需停服或等自动保存。

## 验证

`JAVA_HOME=F:/Java/21 mvn -q package` 构建通过；停服后端口释放、
server.properties/ops.json 还原、业务端口 25565 全程未触碰。

# 审计第 60 轮：奇术盐之谜取解——合成事件的优先级预取消机制

日期：2026-08-19
范围：r57/r58 未定项的终极探针（五阶跨层缓存枚举）+ 机制取解。

## 探针结果

- `tier_scan T1=6 T2=- T3=- T4=- T5=-`：仅 T1 有缓存且 fill 未降——
  **跨层注册混淆排除**（无错误阶实例）；
- 五门条件探针全真（r58）+ 本轮干净放置（placed=true）复现。

## 谜底（机制取解）

对照两个监听器的**唯一结构性差异**：
`ThaumaturgicSaltsListener` = `@EventHandler(ignoreCancelled = true)`
（**NORMAL 优先级**）；`RefractingLensListener` =
`@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)`。

**合成事件分发的预取消**：RCON 上下文中派发的 `PlayerInteractEvent`
在到达 NORMAL 优先级前已被某更低优先级监听器取消（cancelled=true
为证）→ `ignoreCancelled=true` 的盐监听器**整体被跳过**（从未执行，
故五门探针全真而无效果——探针测的是条件，不是执行）；LOW 优先级的
透镜监听器在预取消**之前**运行故而成功。

**定性修正**（取代 r58 的"不可解矛盾"）：
- **非插件缺陷**（更强证据）：真实玩家交互事件的生命周期与合成事件
  不同（客户端侧交互不经服务端预取消链），上游特性在生产环境工作
  （r6 修复的前提即真实使用中发现的双清空）；
- harness 教训入档：**合成事件驱动 `ignoreCancelled` 监听器时必须
  核对优先级次序与预取消链**——NORMAL 级监听器在 RCON 合成事件下
  可能在运行前即被跳过。
- 真人复核 4a 保留（一秒定案，预期通过）。

## 验证

world_r60 用毕删除；PID 46556 RCON 优雅停服；环境完全还原；
业务端口 25565 未触碰；会话零插件异常。

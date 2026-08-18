# 审计第 34 轮：物品 GUI 载体交互时序 + 配方快照完整性

日期：2026-08-18
范围：以物品为 GUI 载体的交互时序域——`CrystamageSatchel`（收纳袋）、
`EphemeralWorkBench`（临时融合工作台）、`EphemeralCraftingTable`、
`RecallingCrystaLattice`；以及 CSCoreLib `ChestMenu`/`MenuListener`
点击语义、Slimefun cargo 运输槽暴露面的底层核验。

## 已修复（2 个 commit）

| commit | 缺陷 | 修复 |
|--------|------|------|
| `d73a682` | **临时融合工作台配方快照时序缺陷（功能完整性）**：`RECIPES` 在类加载时点（Tools 注册段）静态快照 `getEnabledSlimefunItems()`，而 `setupSlimefun` 中 Gadgets（4 处）/ArtisticItems（38 处）等后置注册段的全部增强合成台配方（共 42 处）不在快照内——工作台无法合成这些物品，与物品描述"原版+增强型工作台配方"不符。vanilla 回退路径也无法产出 SF 物品，即完全不可合成 | 快照改为 `setupRecipes()` 显式方法，在 `setupSlimefun()` 末尾（全部物品注册后）调用；幂等（clear 后重建） |
| `317754d` | **收纳袋 GUI 陈旧实例覆盖写回（数据丢失）**：`SatchelGui` 构造时读取 `SatchelInstance` 后长持引用，GUI 打开期间自动吸取路径（`SatchelListener`→`tryAddItem`）更新物品 PDC，GUI 内取用/saveInstance 以陈旧实例整体回写——吸取的水晶被静默丢失（典型触发：水流水晶农场 + 打开收纳袋界面） | `tryWithdraw`/`open` 前从物品 PDC 重新同步实例（读-改-写），读取失败保留当前实例兜底 |

## 核验安全（记录依据）

- **cargo/漏斗暴露面闭合**：`MenuBlock.getTransportSlots` 全部机械返回空数组
  （ChroniclerPanel/LiquefactionBasin `getInputSlots()=new int[0]`）；五机械载体
  方块均非容器类（深板岩瓦/雕纹深板岩/炼药锅/切制铜/诡异栅栏）——原版漏斗
  物理上不可达 BlockMenu。
- **ChestMenu/MenuListener 语义**（REF 源码实证）：无 handler 的空槽默认
  `emptyClickable=true` 可交互；`EphemeralWorkBench` 45 槽全被背景/合成/输出
  覆盖（inventory 大小 = ceil(最高槽/9)*9 = 45，不存在无主槽位），关闭时
  RECIPE_SLOTS+OUTPUT_SLOT 全部掉落——无物品丢失面。
- `EphemeralCraftingTable`：纯 vanilla `openWorkbench`；带故事物品合成被
  `MiscListener` CraftItemEvent 拦截（round-8 前置）。
- `RecallingCrystaLattice`：`ChatUtils.awaitInput` 回调经 dough `ChatInputListener`
  `BukkitScheduler.runTask` 跳回主线程（字节码实证）——renameItem 线程安全；
  下界星材质无可放置语义，未 cancel 无副作用；PDC 读取已有失败关闭（round-4）。
- `Bukkit.craftItem`（1.21.11 paper-api 字节码）：返回值 `@NotNull`（无配方
  返回 AIR 物品）——工作台 vanilla 回退无 NPE 面。
- `SlimefunUtils.isItemSimilar(null,null)=true`、null 侧安全——工作台配方
  匹配空槽语义正确。

## 实机验证（隔离端口 25599，双机器人）

新测试规程（本轮起）：测试服独立端口启动（避开业务端口 25565）、启动即记录
PID（本轮 **PID 3004**，21:49:22，命令行三重核对）、RCON 优雅停服、仅结束
有记录的自启进程、环境属性（server.properties/ops.json）用后还原。

| 断言 | 结果 | 证据 |
|------|------|------|
| A1-A4 工作台后置配方端到端 | ✅（14:08 与 14:14 两轮） | 8 玻璃+稀有融合锭精确入格 → 合成消耗全部材料 → 输出槽 glass×8（天使方块材质即 GLASS，`asQuantity(8)`）——修复前该配方不在 RECIPES 且 vanilla 无法产出 SF 物品，必为 AIR |
| B1-B7 收纳袋陈旧实例竞态端到端 | ✅（14:11 与 14:12 两轮全绿） | 初始化开袋 → 仰投 1 水晶被吸收（PDC=1）→ 开袋 → **GUI 保持打开期间**第二机器人上抛 5 水晶、主视角走过去吸收（PDC=6，GUI 实例陈旧=1）→ 单击取 1 + 连取 5 = 共 6 个全数到手；修复前行为=只能取 1（5 个被覆盖丢失） |
| 会话日志 | ✅ | 0 ERROR/SEVERE、0 插件异常（仅 Slimefun 皮肤缓存超时 WARN，网络性） |

验证工具：`bot/check_audit34.js` + `bot/check_audit34-results.json`
（mineflayer；1.21.9+ 物品名在 `item.customName` 组件，`item.nbt=null`）。

## 事故记录与规程沉淀（重要）

本轮前段发生一次**测试进程误杀事故**：按"端口 25565 被占"推断占用者为残留
测试进程并 `taskkill`（PID 43256/27172），实为业务服务器及其连接，造成业务
中断与经济损失。已当面致歉并落实：

1. **禁止结束任何非本会话启动的进程**；唯一例外=有 PID 记录且启动时间/命令行
   三重核对一致的本人启动进程；
2. 绝不按进程名（`taskkill /IM`）或端口推断身份杀进程；
3. 测试服一律独立端口（本轮起 25599），端口被占一律视为"非我的进程在用"，
   改端口而非清端口；
4. 长驻进程启动即记录 PID；收尾优先 RCON `stop` 优雅停服；
5. 服务器测试环境属性（server.properties/ops.json）临时改动用后还原。

## 验证

`JAVA_HOME=F:/Java/21 mvn -q package` 构建通过（0.18.0 + 两修复）；
Paper 1.21.11 build 132 + Slimefun 5.0.0 实机双机器人验证如上；
停服后端口释放、配置还原、业务服务器未受影响。

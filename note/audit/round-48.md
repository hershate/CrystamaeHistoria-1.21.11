# 审计第 48 轮：法杖配置器（第五机械）驱动验证 + 组装链代码复核

日期：2026-08-19
范围：法杖配置器 E2E 驱动（放置注册 ✓ / 槽位填充 ✓ / ADD 按钮点击）+
组装/移除两处理器代码复核。

## 结果

| 断言 | 结果 |
|------|------|
| `place` 放置注册（CUT_COPPER，真实 BlockPlaceEvent） | ✅ menu 存在（fill 成功证明） |
| 槽位填充（法杖 19 + 充能板 14，生产菜单路径） | ✅ configurator_filled |
| 机器人打开 GUI + 点击 ADD_PLATES(30) | ❌ GUI 未开（harness 限制，见下） |
| 组装断言（stave PDC 含 PUSH/50 板 + 板槽清空） | 未触发（按钮未点击） |

**harness 限制（r36 已知家族）**：mineflayer 对 BlockMenu 的交互
非确定性——本会话面板/祭坛 GUI 可开可点，配置器两次尝试（含精确 look）
均未开窗。属测试装置边界，非插件缺陷判据：组装/移除处理器的**代码路径**
本轮逐行复核（防御完整）+ r2 修复其断路器/退还路径 + r24 组合序列推演
覆盖连点幂等；GUI 交互层建议真人客户端复核（同 r46 图鉴翻页）。

## 代码复核（ADD_PLATES/REMOVE_PLATES 处理器，无新缺陷）

- ADD：法杖类型/叠堆/空法杖守卫；损坏 PDC 充能板（IllegalStateException）
  与无 PDC 板（作弊产物）双路径**退还而非吞没** ✓；槽位全清 ✓。
- REMOVE：空条目跳过（防 NPE）、板逐槽返还、映射清空 + lore 重建 ✓。
- onBreak 五槽无条件掉落 ✓（r2 语义保持）。

## 驱动增强（入库）

`chdriver configurator <world> <x> <y> <z> fill|assert`：槽位填充与
组装结果 PDC 断言——GUI 点击问题解决后即可一键复验。

## 验证

全新世界 world_r48（用毕删除）；PID 53372 RCON 优雅停服（exit 0）；
环境完全还原；业务端口 25565 未触碰（停服期 Essentials 异步任务 Nag
为该插件已知关闭噪音，与本插件无关）。

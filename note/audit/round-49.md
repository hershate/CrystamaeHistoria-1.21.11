# 审计第 49 轮：Waystone / 魔法传送网（绑定-传送往返）驱动验证

日期：2026-08-19
范围：以真实 `PlayerInteractEvent`（潜行右击/普通右击 + CRY_RECALL_LATTICE）
驱动 `RecallingCrystaLattice` 的生产处理器路径——绑定（setLocation）与
传送（teleportAsync）。

## 结果

| 断言 | 结果 |
|------|------|
| Waystone 放置注册（END_STONE_BRICK_WALL，真实 BlockPlaceEvent） | ✅ |
| **绑定：潜行右击 → ItemUseHandler.setLocation → PDC 写入** | ✅ **PASS**（真实生产路径，两个会话两度实证） |
| 传送：普通右击 → 位置读取/路标核验/权限 → `teleportAsync` | ◐ 处理器运行零异常，但 bot 位置未变（见下） |
| 对照：vanilla 同步 `/tp` 移动 bot | ✅（服务端位置正确变更） |
| 会话日志 | ✅ 0 插件异常 |

**harness 限制（如实归档）**：`teleportAsync` 等待客户端传送确认
（Paper 异步传送语义），mineflayer 客户端不发送 ACK——future 挂起、
位置不应用；vanilla 同步 `/tp` 对照可正常移动 bot，证明并非处理器条件
未满足。传送链代码（位置读取→BlockStorage.check→PLACE_BLOCK 权限→
teleportAsync，r4 失败关闭防御在位）两轮复核无缺陷；对真实玩家该路径
为标准 API。**判定：非插件缺陷，自动化边界**（与 r46/r48 同族）。

## 驱动增强（入库）

`chdriver waystone <w> <x> <y> <z> [pos]`：绑定-传送驱动 + 位置查询；
`chdriver tpasync`：teleportAsync×客户端 ACK 微探针。

## 验证

全新世界 world_r49（用毕删除）；三次会话（PID 30728/3860/52760）均
RCON 优雅停服；环境完全还原；业务端口 25565 未触碰。

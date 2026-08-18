# 审计第 47 轮：统计写入链驱动验证（六写点 → 纪元缓存 → 落盘）

日期：2026-08-19
范围：以驱动 `stats` 子命令在真实服务器上驱动 PlayerStatistics 全部
六个写入点，经纪元缓存消费方读回断言，再走与关服一致的 force 落盘
（`saveAll(true)`）并核验磁盘文件。

## 六写点驱动矩阵

| 写点 | 调用 | 结果 |
|------|------|------|
| 使用计数 | `addUsage(uuid, PUSH)` | ✅ |
| 法术解锁 | `unlockSpell(uuid, PUSH)` | ✅ |
| 独特故事解锁 | `unlockUniqueStory(uuid, def)` | ✅ |
| 编年史累计 | `addChronicle(uuid, def)` | ✅ |
| 现实化累计 | `addRealisation(player, def)` | ✅ |
| 镀金解锁 | `unlockStoryGilded(uuid, def)` | ✅ |

零异常。

## 断言结果：PASS

| 断言 | 结果 |
|------|------|
| 读回（纪元缓存消费方）| ✅ usage=true / spell=true / unique=true |
| force 落盘（`saveAll(true)`，同关服路径） | ✅ |
| `player_stats.yml` 磁盘文件含该玩家 UUID 条目 | ✅ filePersisted=true |

**stats_done pass=true**——写入→缓存失效→读回→落盘全链实证（perf
r23 的纪元失效设计 + r34 周期落盘在当前构建复验成立）。

## 驱动增强（入库）

`chdriver stats`：六写点一键驱动 + 读回/落盘断言。

## 验证

测试用 player_stats.yml 已快照还原（35 行原样）；全新世界 world_r47
用毕删除；PID 55228 RCON 优雅停服（exit 0）；环境完全还原；业务端口
25565 未触碰（会话中一条 ERROR 为 Mojang 公钥 API 网络超时，离线模式
服务器的已知噪音，与本插件无关）。

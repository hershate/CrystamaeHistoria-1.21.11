# 审计第 38 轮：镀金器吸取/镀金路径 + 液化池合成链 服务器端 E2E

日期：2026-08-18
范围：第 37 轮 `7750049`（FLASH 粒子崩溃）修复的第二受影响面复验——
棱镜镀金器吸取路径；以及液化池吸取→催化合成链的驱动 E2E。
驱动插件扩展 `place`/`gilder`/`basin` 子命令（均走生产代码路径）。

## 驱动方法（本轮新增能力）

- `chdriver place <sfId> <world> <x> <y> <z>`：以**真实 `BlockPlaceEvent`** 放置
  并注册机械（与玩家放置完全同路径：Slimefun BlockPlaceHandler → 缓存注册），
  取代不可靠的机器人点击放置（第 36 轮 harness 结论的根治方案）。
- `chdriver gilder`：掉落棱镜水晶于镀金器中心 → `consumeItems()`（public）→
  `addCrystamae`（FLASH 路径）；再以满故事物品调 `gildItem`（DisplayItem/DUST 路径）。
- `chdriver basin`：枚举 3 类型组合经 `lookupSpellRecipe(set,1)` 找有效配方 →
  依次掉落 3 种 COMMON 水晶 → `consumeItems()`（吸收/展示架混色/液位）→
  掉落空白法术板（CRY_SPELL_PLATE_1）再 `consumeItems()`（催化合成 + 清池）。

## 结果：双链路 PASS（端口 25599，PID 11032/11996，RCON 优雅停服）

**镀金器（7750049 第二受影响面实证修复）**：

| 断言 | 结果 |
|------|------|
| BlockPlaceEvent 放置+注册（WARPED_FENCE） | ✅ placed=true cache_found=true |
| 棱镜水晶吸取（addCrystamae → FLASH 收尾粒子） | ✅ fill 0→1，**零异常**（修复前每次必抛 IAE） |
| 镀金（满故事闪长岩 + DisplayItem/DUST 路径） | ✅ fill 1→0、手持物品消耗、统计解锁 |
| **result** | **PASS** |

**液化池（吸取→催化合成链）**：

| 断言 | 结果 |
|------|------|
| BlockPlaceEvent 放置+注册（CAULDRON） | ✅ placed=true cache_found=true |
| 跨重启内容持久化（BlockStorage ch_lvl → onNewInstance 恢复） | ✅ 重启后 fill0=3（上轮投入） |
| 3 种 COMMON 水晶吸取（addCrystamae/updateDisplay/液位/展示架） | ✅ fill 3→6 |
| 有效配方查找（lookupSpellRecipe） | ✅ [ELEMENTAL, MECHANICAL, ALCHEMICAL] |
| 空白板催化：top-3 匹配 → 充能法术板掉落 + 清池 | ✅ fillAfter=0、空白板消耗、充能板（PAPER）实体掉落 |
| **result** | **PASS** |

## harness 附注

- 修复后 FLASH 粒子包携带 Color 数据广播——**mineflayer 1.21.11 协议解析器
  不识别该载荷会抛 PartialReadError**（机器人侧断连/刷屏，服务端无影响）。
  这从反面印证修复生效（数据已随包下发）。后续机器人测试应远离 FLASH 粒子源
  或升级 minecraft-data。
- 机器人直接放置 SF 方块不可靠的结论维持（第 36 轮）；本驱动 `place` 已根治。

## 验证

会话日志 0 ERROR/SEVERE；停服后驱动 jar 移出、server.properties/ops.json
还原、端口释放、业务端口 25565 全程未触碰。

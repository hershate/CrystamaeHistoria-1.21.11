# 审计第 55 轮：水晶燃烧降级 + 下界门脱水监听器驱动验证

日期：2026-08-19
范围：两个实体事件监听器的生产路径以**真实 Bukkit 事件**驱动——
`CrystaDowngradeListener`（水晶物品燃烧降级）与 `NetherDrainingListener`
（物品穿下界门脱水转换）。玩家邻区掉落实体即时注册（r41 实证前提）。

## 结果：双链 PASS

| 断言 | 结果 |
|------|------|
| RARE 水晶 + 真实 `EntityCombustByBlockEvent`（火方块） | ✅ **downgrade PASS**：物品换为 UNCOMMON 同型水晶、事件取消（免燃烧）、弹开速度设置（r6 修复的降级语义在位） |
| 第一个有效脱水配方 + 真实 `EntityPortalEnterEvent` | ✅ **drain PASS**：物品原位转换为配方产物（`isItemSimilar` 精确匹配断言） |

零插件异常。掉落物在断言后均清理（不留测试残留实体）。

## 边界复核（代码级，随读记录）

- 降级守卫：UNIQUE(6) 与 COMMON(1) 不降级（`id != 6 && id > 1`）✓；
  非 PLAYER_HEAD 材质免 getByItem（r53 族材质门控）✓。
- 脱水循环：null/AIR 配方键跳过 ✓；数量保持（`asQuantity(amount)`）✓。

## 驱动增强（入库）

`chdriver crysta`：燃烧降级 + 门脱水的真实事件双驱动。

## 验证

全新世界 world_r55（用毕删除）；PID 47856 RCON 优雅停服；环境完全
还原；业务端口 25565 未触碰。

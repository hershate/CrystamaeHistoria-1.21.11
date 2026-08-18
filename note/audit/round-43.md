# 审计第 43 轮：gadget tick 路径驱动验证（13 类全部 tick 型 gadget）

日期：2026-08-19
范围：以驱动插件 `gadgets` 子命令沿 +x 逐格放置全部 tick 类 gadget
（真实 `BlockPlaceEvent` 注册，玩家在线且邻近加载区块——第 42 轮规程），
随后 45 秒真实 Slimefun ticker 自然运行 + 异常监测。

## 覆盖清单（13/13 placed，0 cancelled，0 missing）

MobLamp₁ / MobFan₁ / MobDirt₁(CursedEarth) / MobPlate₁ / MobPlateTrap /
ExpCollector₁ / EnderInhibitor₁ / MobCandle₁ / CropGlass₁(GreenHouseGlass) /
MysteriousPottedPlant(MysteriousTickerNoInteraction) / TrophyDisplay₁ /
FragmentedVoid / Waystone

（各多级变体与同级共享同一 tick 实现类，逐类一代表 + round-10 修复的
原缺陷类全覆盖；非 tick 的使用型物品——AngelBlock/GlassOfMilk/
PhilosophersSpray/ExaltationStand——不属本域。）

## 结果：PASS

| 指标 | 结果 |
|------|------|
| 放置注册 | ✅ 13/13（全部 BlockPlaceEvent 接受） |
| 45s 真实 ticker 运行（玩家邻近） | ✅ |
| 异常 | **0 Exception / 0 ERROR / 0 SEVERE**（round-10 修复的每 tick NPE 类若有回归将在 45s 内出现数百次——零出现） |
| tick 健康 | **0 "Can't keep up" / 0 watchdog** |

## 驱动增强（入库）

`chdriver gadgets <world> <x> <y> <z>`：批量放置 gadget 阵列并报告
placed/cancelled/missing——供后续回归复用。

## 验证

全新世界 world_r43（用毕删除）；PID 41808 RCON 优雅停服（exit 0）；
环境完全还原；业务端口 25565 未触碰。

# 审计第 52 轮：Exalted 物品效果链驱动验证（冻结表登记/过期回收）

日期：2026-08-19
范围：`ExaltedItem.onExalt` 生产方法（ExaltationStand 每 tick 调用的
同一路径）驱动 ExaltedTime（黎明）与 ExaltedWeather（晴）——SpellMemory
冻结表登记、玩家状态应用、2s 过期回收全链。

## 结果：双链全 PASS

| 断言 | 时间冻结（CRY_EXALTED_DAWN） | 天气冻结（CRY_EXALTED_SUN） |
|------|------|------|
| onExalt 零异常 | ✅ | ✅ |
| SpellMemory 表登记 | ✅ frozenTimeTable=HIT（t+1s stat=1） | ✅ frozenWeatherTable=HIT（w+1s stat=1） |
| 玩家状态实际应用 | ✅ playerTime=6000（黎明时戳） | ✅ weather=CLEAR |
| **2s 过期 + 每秒回收**（TemporaryEffectsRunnable） | ✅ t+3.5s stat=0 | ✅ w+3.5s stat=0 |
| 交叉污染 | ✅ weather 表未动 | ✅ time 表未动 |

（ExaltedTime 半径 25 格玩家广播 + `System.currentTimeMillis()+2000`
绝对到期模型——r36 分析文档 §4.1 的统一过期模型在 Exalted 家族实机实证；
站体 tick 驱动（ExaltationStand→afterTick）由 r43 的 MysteriousTicker
类验证覆盖。）

## 附注（环境）

- 关服时 JVM 挂在关闭收尾（端口已释放、进程残留 11MB——Essentials
  异步任务/Balatro 关闭噪音家族），按规程对本轮记录 PID（21024）强制
  结束；服务端数据已在其前完成保存（stop 回执在案）。

## 驱动增强（入库）

`chdriver exalted time|weather`：onExalt 驱动 + 表命中/玩家状态报告。

## 验证

全新世界 world_r52（用毕删除）；环境完全还原；业务端口 25565 未触碰。

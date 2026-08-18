# 审计第 73 轮：调度器全景矩阵（全部 runTask* 调用点）

日期：2026-08-19
范围：全新探针角度——全库 `runTaskTimer`/`runTaskLater` 调用点
（6 处）的周期合法性与取消路径审查（判定轮计数 1/2 目标）。

## 全_callsite 矩阵

| 位置 | 形态 | 判定 |
|------|------|------|
| `RunnableManager:22/25/28` | 3 周期任务（20/12000/80 tick，常量） | ✓ 合法 |
| `SupportedPluginManager:47` | 延迟 1 tick 的附属检测（避免加载顺序） | ✓ 合法 |
| `Spell:142` | `runTaskTimer(plugin, 0, period)`——period 由 `spellCore.getTickInterval()` 派生：全 69 法术枚举核对**全部 ≥1**（1/2/3/10/20/40 族），`intervalMultiplied` 时 ×staveLevel(1-5) 仍 ≥1；period=0 非法态不可构造 | ✓ 安全 |
| `TunnelBore:54` | period=1，但 **TunnelBore 为未注册死代码**（r34 已记录） | ✓ 无运行时面 |

**取消路径**：SpellTickRunnable 自取消 + `tickingCastables` 注销
（r1 断路器 + r44 生命周期实证）；周期任务由 onDisable 隐式随插件
终止（Bukkit 语义）——r44/r54 soak 终态排空已实证。

## 判定：零发现（计数 1/2）

角度与 r71（注解参数组合）/r72（修复验证）互异。需再一轮互异角度
零发现（2/2）方可宣告收敛休眠。

## 验证

纯代码判定轮（无服务器启动/无进程占用）；业务端口 25565 未触碰。

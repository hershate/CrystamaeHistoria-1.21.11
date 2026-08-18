# 性能优化第 73 轮：每 tick 派发任务折叠域（FloatingHeadAnimation → process 内联）

日期：2026-08-18
域：**长期每 tick 循环任务形态**——探针角度（与 r1-72 互异）：全库
调度任务普查发现，r52 判定"tick 类零命中"对**施法级**任务成立，但
存在一类被漏计的形态——**随机械工作状态长期存活的 period=1 任务**：
`FloatingHeadAnimation`（每工作面板一个，自 setWorking 起每 tick 派发
直至 setNotWorking/关服，r47 仅移除其死分支未折叠）。面板的
`process()` 本身已由 Slimefun BlockTicker 每 tick 驱动，两路同节拍。

## 实现（本轮提交 2da7292）

- `ChroniclerPanelCache.startAnimation()` 不再创建任务：保留姿态复位，
  持有 `displayStand` 直接引用字段；
- `process()` 工作分支在 `animateLight()` 后直接调用
  `ArmourStandUtils.panelAnimationStep(displayStand, true)`；
- `setNotWorking()` 取消任务改为清空引用；**FloatingHeadAnimation 类
  删除**（全库唯一使用者，基准插件不引用该类，benchRound47 直接测
  ArmourStandUtils 函数不受影响）。

## 量化（服务器内，round-73.tsv）

| 基准 | 变体 | ns/op | 说明 |
|------|------|-------|------|
| headAnimFold | sched_dispatchProxy | 35.46 | 一次性同步任务入队+执行（每 tick 循环任务处理成本上界代理） |
| headAnimFold | step_body | 18.89 | 任务体（与 r47 基线 18.07ns 连续） |
| headAnimFold | stand_uuidLookup | 16.04 | UUID 注册表查（折叠后备路径形态） |
| headAnimFold | stand_directRef | 2.81 | 直接引用字段（折叠形态） |

**动画路径**：旧 = 派发 35.46 + 体 18.89 ≈ **54.35 ns/tick/面板** →
新 = 体 18.89 + 引用读 ≈ **21.7 ns（~2.5x）**。绝对量噪声级
（20 面板每 tick 省 ~0.66µs）——**卫生-结构混合级**（r42/r47 先例：
绝对量噪声 + 结构收益保留）。

等价性：任务驱动（真实 runTaskTimer(1) 跑 10 tick）vs 直接调用 10 次
头姿增量**双精度逐位一致 true**（0.9999998785147501 == 同值）；节拍
等价由两边同为每 Minecraft tick 驱动保证。

## 结构收益（本轮主项）

1. **消除全库唯一长期每 tick 任务类**——此后所有调度任务均为
   施法级/秒级/生命周期级（r52 判定域补齐）；
2. **卸载区块空转消除**：面板在已卸载区块保持 working 时（Slimefun
   ticker 不再 tick → 永不 setNotWorking），旧任务对无人可见的盔甲架
   持续每 tick setHeadPose 直至关服/区块重载；折叠后随 ticker 停止；
   （每面板上限 1 个任务，非无界泄漏，但属纯浪费）；
3. 任务对象/生命周期管理（创建-取消-复位三态）简化为单字段。

陈旧引用语义与原任务持引用完全一致：展示架被外部移除时同样静默
失效，下次 setWorking 经 getDisplayStand 重建。

## 边界分类

- `SpellTickRunnable`（施法级 period 任务）维持独立任务形态：数量随
  施法（事件级）而非随放置机械长期累积，r52 已判定；
- `TemporaryEffectsRunnable`（1s 全局）/`SaveConfigRunnable`（10min）/
  `ParticleDisplayRunnable`（4s）为全局常驻单例，无折叠意义。

## 会话记录

COMPLETE=1、CH 插件错误 0、watchdog 2 次（基准批固有伪象）、
**234 变体**（230+4），41+1 条断言行中 3 处 false 均为文档化预期
（round13/24/35）——折叠构建实机回归通过。

## 判定

族矩阵增补行：**长期每 tick 循环任务——结构折叠级（~2.5x 噪声级
绝对量 + 任务类消除）**。第十六轮循环开启轮：形态普查确认该族唯一
成员闭合。下一轮判定轮换新角度。

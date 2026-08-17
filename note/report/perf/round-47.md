# 性能优化第 47 轮：FloatingHeadAnimation 每 tick 死分支移除（r19 族漏网处）

日期：2026-08-17
域：**记录者面板头部动画每 tick 路径**——r19（热循环 Location 分配
消除）漏网调用点：`FloatingHeadAnimation`（每个 Working 面板每 1 tick
运行）的 directionUp 分支为上游遗留死逻辑。

## 死逻辑论证（等价性断言实证）

1. `ArmourStandUtils.panelAnimationStep(stand, directionUp)` **忽略
   方向参数**——两分支的可见行为完全相同（头部姿态 +0.1 rad 持续旋转）；
2. 展示架从不垂直位移，`getLocation().getY()` 恒等于 baseY，
   `>= baseY+0.2` / `<= baseY-0.2` **恒 false**——directionUp 永不
   翻转（服务器内 10 万步断言 branchInert=true）；
3. 移除分支后：双架归零同步步进 1000 步姿态完全一致（pose=true）。

（r15 分析文档所述"上下浮动动画 ±0.2 格往返"实为从未生效的描述——
上游遗留。）

## 实现（本轮提交 f6959f7）

- `run()` 收敛为单次 `panelAnimationStep(armorStand, true)`；
- 移除 `baseY`/`directionUp` 死字段与 `Y_DEVIANCY` 死常量
  （`SPEED` 保留——`runTaskTimer` 调用方在用）。

## 量化（服务器内真实盔甲架，round-47-server.tsv）

| 基准 | 旧 | 新 | 提升 |
|------|----|----|------|
| headAnim.step（步进+getLocation 比较 vs 仅步进） | 21.39 ns | 18.07 ns | **1.18x**（~3.3ns/tick/板块） |

绝对量噪声级偏上——**第四次 JIT 逃逸分析实证**：`getLocation()`
返回的 Location 分配已被 EA 消除（对象不逃逸，仅读 getY），残差为
NMS 位置读取与比较分支。变更价值以**死逻辑移除**（可维护性）为主、
微性能为辅。会话 COMPLETE=1、CH 插件错误 0、watchdog 2 次
（基准批次固有，同 r45/46）。

## 过程记录

首跑 pose=false 为**基准断言设计缺陷**（standA 先跑 10 万步分支惰性
验证，同步步进前未归零起点）——归零后复测通过。缺陷如实归档，
与 r35 dropItem 散落断言同类：断言本身也会错，失败先查断言设计。

## 判定

r19 Location 分配族调用点闭合（粒子路径 19 + 本轮动画路径 47）；
EA 四次实证（r19 / r38 / r46 faceIter / r47 getLocation）已构成
稳定边界结论：**不逃逸的短命对象分配在 C2 下近免费，清扫目标应锁定
逃逸形态（流包装/二次传递/跨调用保留）**。下一轮判定轮复核族矩阵。

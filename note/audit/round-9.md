# 审计第 9 轮：实际服务器回归验证（1.21.11-2）

日期：2026-08-15
环境：`F:/paper-test-1.21.11`（Paper 1.21.11 build 132 + Slimefun 5.0.0 + CrystamaeHistoria 1.21.11-2），Java 21，`-Xmx3G`，75 秒完整生命周期（启动→稳定运行→stop 优雅关停）

## 结果（与 1.21.11-1 基线逐项一致）

| 检查项 | 基线（1.21.11-1） | 本次（1.21.11-2） |
|--------|------------------|------------------|
| 插件加载/启用 | ✓ | ✓ `Loading/Enabling CrystamaeHistoria v1.21.11-2` |
| 方块故事加载 | 995 | **995** ✓ |
| Slimefun 物品注册 | 274（1 Addons） | **274（1 Addons）** ✓ |
| 全会话 ERROR/Exception | 0 | **0** ✓ |
| 优雅关停与数据保存 | ✓ | ✓（disable 时保存两次输出正常） |
| 进程退出码 | 0 | **0** ✓ |

## 结论

8 轮审计的 70+ 项修复在真实 Paper 1.21.11 + Slimefun 5.0.0 环境下无回归：物品注册数量与基线完全一致（无物品因防御性改动丢失），启用/运行/关停全链路零异常。

## 后续计划（循环续）

第二批审计方向（前 8 轮未深查的物品面）：
1. gadgets 深审（MobFan/MobLamp/MobMat/MobTrap/CursedEarth/GreenhouseGlass/MysteriousTicker/TrophyDisplay/ExaltationStand/Waystone/AngelBlock 等）
2. artistic（画笔耐久/姿态克隆）、exhalted（ExaltedTime/Weather/FertilityPharo/Harvester/SeaBreeze 的 tick 逻辑）、uniques（Trophy）
3. mobgoals AI 目标类（AbstractGoal 系）
4. 剩余 tier1 法术逐个复核（round-5 按模式抽查，未逐文件精读的约 40 个）

# 审计第 71 轮：事件优先级全景矩阵探针（发现并修复 1 项）

日期：2026-08-19
范围：全新探针角度——脚本化提取全部监听器的 `@EventHandler`
（priority, ignoreCancelled）参数，对 PlayerInteract 系事件按红线 4
（须 `ignoreCancelled=true`，checkCooldown 例外）全量合规判定。

## 矩阵结果（8 处 PlayerInteract 系处理器）

| 监听器 | 方法 | 优先级 | ic | 判定 |
|--------|------|--------|----|------|
| MiscListener | checkCooldown | LOWEST | — | ✓（红线例外：前置否决） |
| MiscListener | onUseScoop | LOWEST | ✓ | ✓ |
| PoseChangerListener | onInteract | LOW | ✓ | ✓ |
| PoseChangerListener | onPoseChange | LOW | ✓ | ✓ |
| **PoseChangerListener** | **onPoseClone** | LOW | **✗** | **违例 → 已修** |
| RefractingLensListener | onInteract | LOW | ✓ | ✓ |
| SpellCastListener | onInteract | NORMAL | ✓ | ✓ |
| ThaumaturgicSaltsListener | onInteract | NORMAL | ✓ | ✓ |

## 发现并修复（293ef90）

**`PoseChangerListener.onPoseClone` 缺 `ignoreCancelled = true`**：
被其他插件否决的 `PlayerInteractAtEntityEvent` 仍会进入处理器执行
姿态克隆与物品消耗——与同文件姊妹处理器 `onPoseChange`（有该参数）
不一致，属 **r25 同类缺陷（红线 4）的漏网处**（r25 修复 5 处时未含
此方法）。一行修复 + 重建通过。

**方法论价值**：脚本化全景矩阵一次性消除"逐文件精读遗漏"——
r10-19 的逐文件审计与 r25 的专项修复均未覆盖此参数组合角度，
印证判定轮必须轮换探针角度的既有结论。

## 验证

纯代码轮 + 修复构建（`mvn -q package` 通过，867058 字节）；无服务器
启动/无进程占用；业务端口 25565 未触碰。

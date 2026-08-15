# 审计第 26 轮：经济数值溢出边界 + plugin.yml 完整性

日期：2026-08-15

## 溢出边界全量复查结论

| 累积点 | 边界 | 结论 |
|--------|------|------|
| **InstancePlate.addCrysta** | 无上限（单次 ≤10000 但可无限次） | **极端长周期可越 int 上限变负 → 法术板永久不可用**——已钳制（`4270d22`） |
| SatchelInstance.addAmount | long 中间量 + MAX_VALUE 钳制 | round-3 已闭环 ✓ |
| 液化池 contentMap / 镀金器 fillAmount / 经验收集器 volume | maxVolume 上界检查 | ✓ |
| PlayerStatistics uses++（施法/发掘/现实化计数） | int 上限 21 亿，每 tick 一次也需 3 年+ | 不可达，不改 |
| KnowledgeShare 经验 | min(当前总量, 基数×等级) | ✓ 有界 |
| dropShards 数量 | shard(0-3) × 倍率(≤4) = ≤12 | ✓ |
| 冷却时间戳 | long | ✓ |

## plugin.yml 审计（`4270d22` 修正）

- 命令 usage 提示 `/historia <sub>` 非实际命令名（实际 `/crystamaehistoria`，别名 `/ch` `/hist`）——已修正描述与用法
- 无 permissions 声明：子命令权限为代码内 `isOp` 判定（声明节点而不接线无意义）——保持现状
- depend/softdepend/api-version 与发布说明一致 ✓

## 验证

`JAVA_HOME=F:/Java/21 mvn -q package` 构建通过（真实退出码验证）。

# 审计第 8 轮：功能与表述一致性 + 收口

日期：2026-08-15
范围：README.md 玩法声明比对、命令类参数边界、前几轮遗留观察复核、版本收尾

## README 声明比对结论（全部相符）

| 声明 | 实现 |
|------|------|
| 记录者从物品发掘故事、等级按物品类型 | `StoriesManager` 5 级 BlockTier + blocks.yml 定义 ✓ |
| 现实祭坛提取能量生成水晶簇、破坏获取水晶 | RealisationAltar + CrystalBreakListener ✓ |
| 液化池水晶液化、错误配方损失全部材料 | processBlankPlate/ChargedPlate 无匹配即 emptyBasin（惩罚设计）✓ |
| "80 多种可用组合" | C(9,3)=84 种水晶类型组合 ✓ |
| 法术板耗尽后用同配方 3 种水晶重新充能 | processChargedPlate 要求同法术+同 top-3 ✓ |
| 法杖 4 槽（左/右/潜行左/潜行右） | SpellSlot 枚举 + 法杖配置器 ✓ |

## 已修复（1 个 commit）

| commit | 问题 |
|--------|------|
| `9082276` | **round-5 遗留观察 2 更正**：施法时 50 格 raycast 并非死代码——StripMine 消费 `getTargetedBlockFaceOnCast/getTargetedBlockOnCast`，视线无方块时 tick 回调 NPE（施法失败+白扣充能）。test-spell 负强度可致负伤害（反而治疗目标）；两测试命令的非数字参数/非法法术名裸抛异常 |

## 核验安全

- OpenSpellCompendium/OpenStoryCompendium/GetRanks：纯只读/发消息 ✓。
- HistoriaCommand 分派：`canUse` 权限过滤 + tab 补全上限 ✓。
- plugin.yml depend/softdepend、config.yml 消息键、blocks.yml 995 键：与 1.21.11-1 发布说明一致 ✓。

## 版本收尾

- 版本号 `1.21.11-1` → `1.21.11-2`（pom.xml），发布说明 [note/release/1.21.11-2.md](../release/1.21.11-2.md) 汇总 8 轮 70+ 项修复。
- 8 轮计划全部完成，round-1/3/5/7 遗留观察全部闭合或更正。

## 验证

`JAVA_HOME=F:/Java/21 mvn -q package` 构建通过。

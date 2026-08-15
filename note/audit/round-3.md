# 审计第 3 轮：GUI 交互安全 + 图鉴表述一致性

日期：2026-08-15
范围：全部 GUI 面（5 台机械菜单、收纳袋 GUI、4 个 FlexItemGroup 图鉴、临时工作台×2）+ 支撑层（REF/Slimefun4.1 的 `MenuListener`/`ChestMenu`/`BlockMenuPreset` 点击语义）

## 基线核验（依据 REF/Slimefun4.1 源码）

- `drawBackground` 使用 `ChestMenuUtils.getEmptyClickHandler()`（返回 false）→ 背景槽位不可交互。
- `MenuListener`：无 handler 的**非空**槽位在 `emptyClickable=true`（默认）时可拿取——因此凡是展示真实物品的槽位必须注册 handler 或锁空槽。
- `MenuListener.onDrag` 对无 handler 槽位同样按 `emptyClickable` 处理，拖拽分流不能绕过。
- `BlockMenuPreset.clone` 设置 `setPlayerInventoryClickable(true)`——机械菜单底部玩家栏可操作（shift 点击进输入槽为设计行为）。

## 各 GUI 面结论

| GUI | 结论 |
|-----|------|
| 记录者面板/现实祭坛菜单 | 仅 INPUT_SLOT(22) 开放；背景+输入装饰槽均有 handler。安全 |
| 液化池/镀金器菜单 | 打开即关闭（`addMenuOpeningHandler(closeInventory)`），无交互面。安全 |
| 法杖配置器 | 开放槽=法杖槽+4 法术槽，均为合法输入位；按钮槽 handler 返回 false；破坏时 5 槽全掉落（第 2 轮已确认）。第 2 轮已修无效板守卫。安全 |
| 收纳袋 GUI | 54 槽全部 background+EmptyClickHandler，水晶槽 handler 返回 false；**玩家栏点击被 EmptyClickHandler 全锁**——无法在开袋时把收纳袋放入自身（自包含复制路径封死），Q 键丢弃同被拦。提现路径 `stored > 0` 前置 + 数量上限 64。安全 |
| 故事集/法术集/镀金集/主导航图鉴 | `setEmptySlotsClickable(false)` + 全部展示槽注册 false handler；翻页边界有守卫。无窃取面 |
| 临时融合工作台 | 配方槽开放（设计）；合成匹配后每槽消耗 1（`consumeItem`）；关闭时配方槽+输出槽全掉落；输出槽满则不消耗材料（`fits` 前置）。未见复制路径 |
| 临时工作台 | 仅打开原版工作台。安全 |

## 已修复（2 个 commit）

| commit | 问题 |
|--------|------|
| `b238d99` | **法术集图鉴 7 处表述与实现不符**（复制粘贴错误）：施法消耗/冷却/治疗量/tick 次数的"是否随法杖等级缩放"全部误读 `isDamageMultiplied`；治疗量数值误读伤害字段；范围生效判断误用击退值；弹射物击退数值与标志误用普通击退字段。玩家所见与法术实际行为不一致 |
| `f0be106` | 收纳袋 `setAmounts` 直接接受 PDC 反序列化的任意长度/负值数组（持久化数据不可信）→ 负库存污染提现计算；`removeAmount` 无下界。现长度/负值双重加固 |

## 遗留观察

1. **设计限制（不改，与上游一致）**：`EphemeralWorkBench.RECIPES` 在类加载的静态块中收集当时已注册的 ENHANCED_CRAFTING_TABLE 配方；注册顺序在其后的附属物品不会进入临时工作台配方表。
2. Round 7 待清理：`LiquefactionBasinCache.canCraftSatchel` 死代码。

## 验证

`JAVA_HOME=F:/Java/21 mvn -q package` 构建通过。

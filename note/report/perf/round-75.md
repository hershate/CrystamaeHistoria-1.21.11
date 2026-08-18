# 性能优化第 75 轮：判定轮（每秒清理链守卫一致性 + 运行期注册表遍历形态）

日期：2026-08-18
域：**判定轮**（第十六轮循环收敛 2/2）。探针角度（与 r1-74 互异）：
C. 每秒清理链空表守卫一致性（TemporaryEffectsRunnable 驱动的 12 个
remove*/enable* 是否全部具备 r1 建立的 isEmpty 早退）；D. 运行期
注册表遍历形态（getCachedValues/getRegistry/enabledSpells 等的
运行时循环是否全为命令/启动/GUI 事件级）。

## 角度 C：每秒清理链守卫一致性——零发现

`SpellMemory` 12/12 个清理方法（removeInvulnerable/Projectiles/
FallingBlocks/Entities/Blocks/Strikes/Flight/FrozenTime/FrozenWeather/
Enderman/DisplayItems + enableSpawningInArea）**全部**以
`if (map.isEmpty()) return;` 开头——空表稳态每秒 12 次调用均为
一次 isEmpty 判定（~ns），r1 零复制扫描模式完备无漏网成员。

## 角度 D：运行期注册表遍历形态——零发现

全库注册表迭代站点逐点分类：

| 站点 | 分类 |
|------|------|
| `ConfigManager:99` `SpellType.getCachedValues()` | 启动 loadConfig |
| `Materials:85/106` `StoryType/StoryRarity.getCachedValues()` | 物品注册期 |
| `EphemeralWorkBench:48` `getEnabledSlimefunItems()` | 类加载静态块 |
| `StoryRarity/StoryType` 静态块自遍历 | 类初始化 |
| `InfinitePaintbrush:42/46/54` `PaintProfile.getCachedValues()[i]` | 数组 O(1) 下标访问（非遍历），交互事件级 |
| PlayerStatistics 进度遍历（既有记录） | 图鉴/命令事件级 |

判定：运行期不存在对注册表（69 法术/9 类型/6 稀有度）的遍历扫描
——运行期访问全部经 r8/r11 建立的 O(1) 索引/哈希/下标形态。
注册表遍历域闭合。

## 判定

两个互异新角度均零发现——**r74+r75 连续判定轮零发现（角度互异），
按 round-44 准则宣告第十六轮循环收敛**。循环形态：r73 卫生-结构
1 轮（每 tick 派发任务折叠）+ 判定 2 轮。收口版本 0.17.0（终验 +
soak 见 [note/release/0.17.0.md](../../release/0.17.0.md)）。

## 循环总账（第十六轮）

| 轮次 | 性质 | 结果 |
|------|------|------|
| 73 | 卫生-结构清扫 | 每 tick 派发任务折叠：~2.5x 噪声级 + 全库唯一长期任务类消除 |
| 74 | 判定 | 运行时文本模式/数值解析——零 |
| 75 | 判定 | 清理链守卫一致性/注册表遍历——零 |

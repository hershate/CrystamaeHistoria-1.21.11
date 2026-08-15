# 审计第 11 轮：artistic / exhalted / materials 物品深审

日期：2026-08-15
范围：`slimefun/items/artistic/`（7 类）、`exhalted/`（7 类）、`materials/`（3 类）、`tools/Displacer`（校验先例核验）

## 已修复（2 个 commit）

| commit | 问题 |
|--------|------|
| `f9ed746` | **ImbuedStand 无领地校验**：在点击方块相邻处生成盔甲架，可在他人领地放置实体（绕过保护插件）。补 PLACE_BLOCK 校验（与 Displacer/荧光勺先例对齐）。**ExaltedHarvester/ExaltedSeaBreeze 随机点累积漂移**：循环内 `location.add(x, -1.5, z)` 原地修改基准——偏移逐次累积，收获者 y 每轮再下沉 1.5 格、作用范围严重漂移。改为每轮 clone 取点 |
| `03cb3dc` | **PowderedEssence 骨粉催熟无领地校验**：可在他人领地对作物施加骨粉（催熟他人作物）。补 INTERACT_BLOCK 校验 |

## 核验安全（记录依据）

- BasicPaintbrush/InfinitePaintbrush：LimitedUseItem 计数由 Slimefun 管理，damageItem 走配置与 `SlimefunItemUseEvent` 否决链 ✓
- MagicPaintbrush.tryPaint：目标判空+领地校验（round-6 已核）✓
- PoseCloner（防放置+纯数据）、Trophy（防放置+防食用）✓
- ExaltedTime/ExaltedWeather：玩家时间/天气冻结 2s 滚动续期，SpellMemory 过期重置闭环（round-1）✓
- ExaltedFertilityPharo：随机动物 setLoveModeTicks ✓
- ExaltedBeacon：效果类型静态集合，药水施加 ✓
- Displacer：物品路径完整校验（方块 BREAK_BLOCK/实体双校验），静态 convertBlock 排除 Slimefun 机械 ✓

## 记录（不改，上游设计）

1. Exalted 系展台周期效果（收获/海风转化周围方块）无逐方块领地校验——展台放置本身需 BREAK_BLOCK 权限，视为领地内合法装置（与上游一致）。
2. PhilosophersSpray 由发射器失败事件触发的转化无玩家上下文可校验——需在目标领地放发射器=已有建造权限，风险受限。

## 验证

`JAVA_HOME=F:/Java/21 mvn -q package` 构建通过。

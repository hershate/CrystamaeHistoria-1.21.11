# 审计第 69 轮：Runes / Uniques（奖杯）物品交互链审查（代码级 + 边界复核）

日期：2026-08-19
范围：`Runes.java`（8 符文，全部 `UnplaceableBlock` + ANCIENT_ALTAR
配方）与 `Uniques.java`/`Trophy`（10 奖杯）+ `TrophyDisplay` 交互链
的代码级审查（免服务器轮——纯注册型物品 + 已被 r43 gadget tick 验证
覆盖的展示架）。

## Runes（8 项）

- 全部为 `UnplaceableBlock`（不可放置纯材料）+ ANCIENT_ALTAR 配方——
  **无运行时交互面**（无 handler/监听器/tick）；作为其他物品的合成
  材料消费（grep 消费点与配方定义一致）。
- 判定：**无独立交互链可审**，材料语义由配方系统保证。

## Uniques / Trophy（10 项）

- `Trophy` 双防御 handler 复核在位：
  - `onBlockPlace` → **取消放置**（奖杯不可放世界，杜绝绕过
    TrophyDisplay 的自由放置）✓；
  - `onConsume` → **取消食用**（防食用绕过/消失）✓；
- `TrophyDisplay`（Stand 子类）交互链：
  - 右击放置/取回（背包满时 `dropItem` 兜底不吞物品）✓；
  - `displayConsumer` 在展示物品 tick 时应用视觉效果（:129-130 null
    守卫在位）✓；
  - Stand 族 tick/清理路径已由 **r43 gadget tick 验证**（13 类含
    TrophyDisplay）+ r7 双映射清理修复覆盖。
- 判定：**交互链防御完整，无新缺陷**。

## 结论

本轮零新发现（连续零发现轮 +1）。Runes/Uniques 为低交互面注册型
物品，其真实风险面（展示架 tick、玩家取放）均已在既往轮次覆盖。

## 验证

纯代码审查轮（无服务器启动/无进程占用）；业务端口 25565 未触碰。

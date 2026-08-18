# 审计第 46 轮：图鉴 GUI（三个 FlexGroup）打开/统计路径驱动验证

日期：2026-08-19
范围：以驱动 `compendium spells|stories|gilded` 子命令走**生产打开路径**
（`ItemGroups.*_COLLECTION.open(player, profile, SURVIVAL_MODE)`，与
`/ch spells` 子命令完全一致）+ `/ch rank` 统计读取。

## 结果：PASS（含一项如实归档的 harness 限制）

| 断言 | 结果 |
|------|------|
| 法术集（SPELL_COLLECTION）生产路径打开 | ✅ compendium_opened=spells，零异常 |
| 故事集（STORY_COLLECTION）生产路径打开 | ✅ compendium_opened=stories，零异常 |
| 镀金集（GILDING_COLLECTION）生产路径打开 | ✅ compendium_opened=gilded，零异常 |
| `/ch rank` 统计读取（图鉴统计子节消费方） | ✅（r39 已验，本轮复跑正常） |
| 会话日志 | ✅ 0 异常 / 0 ERROR / 0 SEVERE |

**harness 限制（如实归档）**：按名定位"下一页"按钮做高频翻页点击不可行
——Slimefun 指南类 GUI 的按钮物品名对 mineflayer 不可见（r34 已知的 SF
物品名显示盲区：customName 组件不暴露）。翻页**数据层**（页快照/排序/
解锁集合纪元缓存）已由 perf r21-23/31 的等价性断言覆盖；GUI 交互层的
真实翻页建议由真人客户端复核（非自动化缺口，harness 边界）。

## 驱动增强（入库）

`chdriver compendium <spells|stories|gilded>`：三图鉴生产路径打开——
供后续回归（GUI 打开路径涉及页快照构建 + 统计纪元缓存消费）。

## 验证

全新世界 world_r46（用毕删除）；PID 47596 RCON 优雅停服（exit 0）；
环境完全还原；业务端口 25565 未触碰。

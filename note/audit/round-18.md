# 审计第 18 轮：工具类实体覆盖收尾

日期：2026-08-15
范围：`utils/` 剩余工具类实体精读（TimePeriod/WorldUtils/GildingUtils/ArmourStandUtils/NameUtils/TextUtils）

## 已修复（1 个 commit）

| commit | 问题 |
|--------|------|
| `228b525` | `GildingUtils.makeGilded` 对 `getLore()` null 直接 add——伪造无 lore 物品（PDC 通过镀金前置）在棱镜镀金器右键路径 NPE。判空初始化 |

## 核验安全（记录依据）

- TimePeriod：游戏时间段区间判定（`time > 24000` 为永假防御分支，无害）✓
- WorldUtils/NameUtils/TextUtils：纯查询/格式化 ✓
- ArmourStandUtils：展示架 marker+invulnerable+不可碰撞，PDC 标记与 isDisplayStand 对称 ✓

## 验证

`JAVA_HOME=F:/Java/21 mvn -q package` 构建通过。

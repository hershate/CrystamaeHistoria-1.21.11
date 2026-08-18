# 审计第 83 轮：Artistic 剩余物品消费点核对（判定轮 1/2）

日期：2026-08-19
范围：全新探针角度——Artistic 族剩余物品的消费链闭环：
`InfinitePaintbrush`（无限刷）与 `MysticalPigmentato/Tintanno`
（合成材料）。

## 结果

| 物品 | 消费链 | 判定 |
|------|--------|------|
| **InfinitePaintbrush** | ItemUseHandler：潜行=循环选色（`PDC_PAINT_TYPE` int + 越界回绕 + action bar 反馈 + PotionMeta 颜色即时更新）；非潜行=`tryPaint(…, allowEntities=true)`（**实体涂色分支**——Shulker/羊/鹦鹉/美西螈，r5 领地校验前置 + 类型不匹配返回 false） | ✓ 零异常面；PDC 读取 `getInt(key, 0)` 带默认值（伪造物品安全）；选色越界回绕在位 |
| — 涂色不消耗 | **设计语义**（"无限"刷：无 damageItem 调用），与 LimitedUseItem 家族（BasicPaintbrush 100/1000 耗尽即毁，r53 实证）形成两档设计 | ✓ |
| **MysticalPigmentato/Tintanno** | 纯合成材料（ECT 配方：8×1000 级画笔 + 融合粉(EPIC)）——**注册即消费**形态（r82 同判定），无运行时 handler | ✓ |
| ImbuedStand/PoseChanger/PoseCloner/ExaltationStand | 已覆盖（r11 领地校验 / r71-72 参数修复+验证 / r43+r69） | ✓ |

**零发现**：Artistic 族消费链全部闭环——Basic 有限刷（r53 耗尽实证）、
Infinite 无限刷（本轮）、实体涂色（r34 复核 + 权限校验）、材料
（注册消费）。

## 判定

零发现（计数 1/2，与 r82 互异：交互物品族消费形态）。

## 验证

纯代码判定轮（无服务器启动/无进程占用）；业务端口 25565 未触碰。

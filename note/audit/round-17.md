# 审计第 17 轮：施法上下文与构建器终审

日期：2026-08-15
范围：`magic/CastInformation`（全量精读）、`magic/spells/core/SpellCoreBuilder`（全量精读）、`tools/stave/`（Stave/SpellInstance/SpellSlot）

## 已修复（1 个 commit）

| commit | 内容 |
|--------|------|
| `311ee3f` | 删除 `SpellInstance` 死代码（全库无实例化或类型引用——此前 grep 命中均为 InstanceStave 的 `getSpellInstanceMap` 方法名；运行时数据一律走 InstancePlate） |

## 核验安全（记录依据）

- CastInformation：六个事件槽 run 方法全部 null 判空；施法快照（位置 clone）；raycast 结果由 StripMine 空值守卫消费（round-8）✓
- SpellCoreBuilder：纯赋值流式构建器，无数值逻辑 ✓
- Stave：纯 level 数据 ✓；SpellSlot（round-2 已核）✓

## 验证

`JAVA_HOME=F:/Java/21 mvn -q package` 构建通过。

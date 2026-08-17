# 性能优化第 58 轮：判定轮——ItemStack 比较/声音 API/容器克隆探针（零发现）

日期：2026-08-17
性质：判定轮（第十一轮循环收敛计数 1/2）。探针角度与 r42-57 互异：
**ItemStack 等价比较形态、声音 API 形态、getContents 数组克隆、
玩家查询重复调用**。

## 探针结果（全库扫描 + 逐点复核，零可行动发现）

1. **ItemStack 重量级比较**：全库 `isSimilar`/`.equals`（物品形态）
   **零命中**——物品身份判定全部经由 Slimefun `getByItem` 解析
   （r53 门控族已覆盖），无直接元数据比较热点；
2. **声音 API**：`playSound` 全部位于图鉴菜单 `addMenuOpeningHandler`
   （每次开菜单一次，事件级冷路径）；
3. **`getContents()` 数组克隆**：全库唯一站点 `SatchelListener:50`
   ——已被 r53 PLAYER_HEAD 门控限定为水晶拾取事件级；
4. **玩家查询重复**：`Bukkit.getPlayer` 与 `getCasterAsPlayer` 的
   多命中均为**跨方法单次调用**（同一执行路径内无重复查询，
   r1/r5/r17 的结构化结论维持）。

## 审计转送（非本域，不改）

`StoryCollectionFlexGroup:164` / `SpellCollectionFlexGroup:164` 的
开菜单声音 lambda 写作 `(player) -> player.playSound(p.getLocation(),
...)`——捕获了**外层方法参数 `p`** 而非 lambda 自身的 `player`（对照
同文件 :114 的正确形态 `(p) -> p.playSound(p.getLocation(), ...)`）。
若菜单处理器按开启者派发则两者恒同（无害）；若菜单实例共享则声音
位置取自他人。属审计域正确性疑点，留待审计轮核验，性能域不处理。

## 判定

本轮零发现（四组探针全空）。第十一轮循环收敛计数 **1/2**；下轮以
新角度复核，若再零发现则按 r44 准则宣告第十一轮循环收敛并执行
下一版本收口。

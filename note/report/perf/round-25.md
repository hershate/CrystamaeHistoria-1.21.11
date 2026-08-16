# 性能优化第 25 轮：第四轮循环收口（v0.5.0 全量终验 + soak + 收敛判定）

日期：2026-08-16
性质：收口轮（同 round-9/14/20 模式）——勘察最后一个未审计每 tick 路径
（`DisplayStandHolder`：UUID 实体映射 O(1)，`Bukkit.getEntity` 查表，
无每 tick 实体扫描；展示架仅缺失时重建），确认插件侧可识别域已尽，
执行版本收口。

## 版本收口

- `pom.xml` 0.4.0 → **0.5.0**（bcd5748）；产物 `target/CrystamaeHistoria-0.5.0.jar`；
- 发布说明 [release/0.5.0.md](../../release/0.5.0.md)。

## v0.5.0 全量终验（Paper 1.21.11 build 132 + Slimefun 5.0.0）

- **134 个基准变体**全部执行（round-25-final.tsv，历轮全部组：
  施法/弹射物/交互/机械/法杖/gadgets/故事/统计/世界事件/液化池/启动/
  监听门控/写路径/展示行/召唤 AI/周期任务/热循环/图鉴/统计缓存/lore 对照）；
- **等价性断言全 true**：round-13（两组）/15（四组）/16（六稀有度）/
  17（四组）/18（r3/r5）/19/21（六组）/22（四组）/23（steady/
  invalidation/afterWrite/rank）——round-24 的 `components=false`
  为该轮文档化的否决结论（被否决方案不等价，正是回退依据）；
- 会话 COMPLETE、**0 SEVERE、0 tick 落后**（round-25-final.log）。

## 纯空闲 soak

- 10 分钟纯空闲（移除基准插件的 0.5.0 会话，soak_r25.log）：
  **0 SEVERE、0 tick 落后、0 watchdog、优雅启停**
  （启动 Done → 600s 空闲 → stop 优雅 Disable）。

## 收敛判定（round-25）

第四轮循环覆盖：图鉴展示（21）、统计读取（22-23：相对路径 + 纪元缓存）、
lore 展示写入（24：**实测到 API 边界**——Paper ItemMeta lore 应用机制
为主，组件化反直觉更慢，阴性回退）。叠加前三轮已闭合的读/判定/事件/
机械/启动/写路径/展示行/召唤 AI/周期任务/分配域，全部插件侧可识别域
均已做完或经实测判定为边界。剩余成本由 Bukkit/Paper/Slimefun API
边界构成（setItemMeta/lore 应用/spawnParticle/getNearbyEntities/
实体注册——次数由玩法与视觉契约固定），或经实测确认低于可测阈值。
**第四轮性能优化循环于 round-25 闭合，版本收口 0.5.0。**

## 后续展望（第五轮循环候选，如继续）

- 统计判定的纪元缓存推广到 `hasUnlocked*` 集合形态（当前页级相对读取
  5.7µs/页，收益边际但模式已验证）；
- PDC 故事列表的追加式编码（涉及物品数据格式变更，需双读兼容——
  违背"旧存档兼容"不变式，除非引入版本键双格式）；
- Paper 内部边界项（lore 应用/setItemMeta）无插件侧手段，仅随 Paper
  版本演进重测。

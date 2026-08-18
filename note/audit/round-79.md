# 审计第 79 轮：Runes 古坛依赖边界 + 配方消费链（判定轮 1/2）

日期：2026-08-19
范围：全新探针角度——26 个符文注册的 `RecipeType.ANCIENT_ALTAR` 依赖
边界与配方元素来源核对（26 注册 / 23 种 Slimefun 核心物品引用）。

## 分析结果

- **依赖边界**：`ANCIENT_ALTAR` 为 Slimefun 核心 `RecipeType` 常量
  （REF 实证：注册时即向古坛配方表 add）——属 `depend: Slimefun`
  硬依赖范围（本插件唯一硬依赖），**无软依赖边界问题**；古坛在
  Slimefun items.yml 被禁用时 RecipeType 仍为可用常量（配方注册
  no-op 化为 Slimefun 自身行为，非本插件面）。
- **配方元素**：23 种 `SlimefunItems.*` 引用全部为 Slimefun 核心物品
  （无第三方附属物品）；本地 `CrystaStacks.ARCANE_SIGIL/GILDED_PEARL`
  引用先于 Runes.setup 注册（Materials.setup 先于 Runes.setup，
  r34 序审计证）✓。
- **数组形状**：9 元素统一布局（3×3 展示），与 `AltarRecipe`
  `Arrays.asList(recipe)` 消费兼容（REF 实证无长度断言）；无 null
  占位 ✓。
- **运行时零交互面**（r69 已判定）：纯注册型。

## 判定

零发现（计数 1/2，与 r78 互异：外部依赖边界形态）。

## 验证

纯代码判定轮（无服务器启动/无进程占用）；业务端口 25565 未触碰。

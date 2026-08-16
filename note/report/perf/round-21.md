# 性能优化第 21 轮：图鉴 GUI 展示路径

日期：2026-08-16
域：**图鉴（法术集/故事集/镀金集三个 FlexGroup）展示路径**——打开/翻页/详情点击。
前三轮循环未覆盖该域（收敛判定所列"API 边界"外的展示层重复工作）。
红线：安全/稳定/兼容，无数据格式变更，行为终态等价（断言验证）。

## 优化点（每页/每次点击触发的重复工作）

| # | 热点 | 位置 | 旧成本形态 |
|---|------|------|-----------|
| 1 | 启用法术列表每页重排序 | `SpellCollectionFlexGroup.setupPage` | `Arrays.asList(共享数组)` + `sort(comparing(id))`——O(n log n) 字符串比较器，且**原地污染共享缓存数组** `SpellType.enabledSpells`（首开后所有调用方看到的顺序被改变） |
| 2 | 方块定义列表每页复制+排序 ×2 | `Story/GildedCollectionFlexGroup.setupPage` | `new ArrayList<>(map.values())`（n≈274）+ `sort(comparing(material.name()))` 每次翻页 |
| 3 | 法术主题堆每槽全量重建 | `Spell.getThemedStack` ×36/页 | 逐行 `ChatColors.color` + `new SlimefunItemStack` + ItemMeta 读改写往返（addItemFlags ×2） |
| 4 | 图标每槽重建 | `GuiElements.get*Icon` ×36/页 | `toTitleCase`（StringBuilder+4×replace）+ `themedItemStack`（新 list+逐行拼接）+ `MessageFormat` |
| 5 | 详情页 8 堆重建 | `SpellCollectionFlexGroup.displayDefinition` | ~25 次 `MessageFormat` + 8 个 `CustomItemStack` 每次点击（机制说明堆全常量也在内） |
| 6 | Title Case 无缓存 | `NameUtils`（3 方法） | 每次调用字符串重建 |

## 实现（d731bd0）

1. `SpellType.setupEnabledSpells()`：启动时一次性按 id 排序（翻页直接数组视图；
   同时消除共享数组原地污染——排序变为启动期发生而非首次翻页）。
2. `StoriesManager`：构造期构建 `blockDefinitionsSortedByMaterial` 不可变快照
   （`blockDefinitionMap` 仅启动写：唯一 put 在 `fillBlockDefinitions`）。
3. `Spell.getThemedStack()`：实例级懒缓存（lore/id/name/material 均为每法术常量；
   调用方仅 `.item()`（内部 clone），缓存实例不被修改）。
4. `SpellCollectionFlexGroup`：详情 7 堆按 SpellType 记忆化（EnumMap + 数组），
   `clone()` 返回保持每格独立实例语义；机制说明堆提为静态共享（展示只读）；
   去掉每页排序。
5. `StoryCollectionFlexGroup`：方块详情 2 堆按材质记忆化 + clone 返回；快照翻页。
6. `GildedCollectionFlexGroup`：快照翻页（图标已由 7 覆盖）。
7. `GuiElements`：五个图标构建器按材质/名称记忆化（EnumMap/HashMap + clone）。
8. `NameUtils`：Material/DyeColor 用 EnumMap、PotionEffectType 用 HashMap 记忆化
   （纯函数；主线程单线程模型与项目其他缓存一致）。

## 不做项论证

- `getStatsStack`/`getPlayerInfoStack`（图鉴统计堆）：输入含玩家统计
  （`getConfigurationSection` + 逐键 `getBoolean`），非纯函数，不缓存；
  页级仅 1 次构建，非热区。
- `PlayerStatistics.hasUnlocked*` 每槽 YAML 读（36/页）：键空间为
  玩家×物品，缓存需失效协议（解锁写入点分散），收益/风险比低，
  留待后续轮评估（不在本域终态断言范围内）。
- 水晶数量堆（`getItem().clone() + setAmount`）：需按方格变更数量，
  保持逐次构建（量 ≤9/详情页）。

## 量化（standalone + 服务器内真实路径）

### standalone（benchmark/results/round-21.tsv，3 fork 均值）

| 基准 | 旧 | 新 | 提升 |
|------|----|----|------|
| compendium.spellSort（每页，n=69，稳态重排） | 198.82 ns | 2.39 ns | 83.2x |
| compendium.blockSort（每页，n=274，复制+排序） | 29,646.85 ns | 3.08 ns | ~9626x |
| compendium.titleCase（每调用） | 174.74 ns | 1.79 ns | 97.6x |

等价性断言（每 fork）：排序终态 spells=true blocks=true；titleCase=true。

### 服务器内（Paper 1.21.11 build 132 + Slimefun 5.0.0，真实 ItemStack）

| 基准（每次触发） | 旧 (ns) | 新 (ns) | 提升 |
|------|----|----|------|
| compendium.themedStack（法术主题堆单槽） | 14,052.02 | 62.20 | **225.9x** |
| compendium.pageIcons36（故事集页 36 图标） | 172,250.05 | 946.67 | **182.0x** |
| compendium.detailStack（详情单堆：4×MessageFormat 重建 → clone） | 16,145.50 | 11.36 | **1,421.1x** |
| compendium.spellPageSort（法术集翻页排序） | 1,438.92 | 2.66 | **541.0x** |
| compendium.blockPageSort（故事/镀金集翻页复制+排序） | 56,485.80 | 5.16 | **10,946.9x** |
| compendium.titleCase（单次名称转换） | 206.04 | 45.94 | 4.5x |

页级/点击级汇总（按槽位数组合上表分量）：

| 场景 | 旧估算 | 新估算 | 提升 |
|------|--------|--------|------|
| 法术集整页（排序 + 36 槽主题堆全解锁态） | ≈508 µs | ≈2.3 µs | ~224x |
| 故事/镀金集整页（排序 + 36 图标） | ≈229 µs | ≈0.95 µs | ~240x |
| 法术详情点击（7 详情堆，机制堆另共享） | ≈113 µs | ≈0.08 µs | ~1400x |

（旧值为基准分量线性组合的估算，供域级直觉；逐分量实测见上表。）

watchdog 说明：全会话 5 次线程转储均发生于**旧轮遗留重型组**
（configParse 78ms/op 等，时间戳 20:58:31-20:59:31），与第 16-20 轮
历史会话基线一致（彼时 2-5 次，见 bench_r16~r20 日志）；本轮组
（20:59:35-38，共 3 秒）期间无转储。会话 0 SEVERE、0 基准组失败。

## 等价性与回归

- 服务器内等价性断言（round21 等价性日志行，会话 20:59:34）：
  `themed=true icon=true detail=true blockOrder=true spellOrder=true titleCase=true`；
- `SpellCollectionFlexGroup` 详情堆缓存构建调用与旧每次构建**同一构建方法**
  （`buildDetailStacks` → 原 `getBasicStack` 等七法），构建器为纯函数
  （仅读 SpellCore getter + 静态主题常量），一次构建 ≡ 每次构建；
- 语义保持：所有缓存路径以 `clone()` 返回（`.item()` 内部亦为 clone），
  每次翻页/点击仍获得独立实例（与旧每次新建一致）；
- 回归：CHPERFBENCH COMPLETE、0 SEVERE、0 基准组失败；
  会话日志 `benchmark/results/round-21-session.log`。

## 兼容性

- 无 PDC/BlockStorage/配置格式变更；`getEnabledSpells()` 顺序从
  "枚举序（首开图鉴后变 id 序）" 变为 "恒 id 序"——调用方
  （loadConfig 遍历、长度统计）顺序无关；图鉴分页与旧终态一致。

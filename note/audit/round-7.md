# 审计第 7 轮：无界集合 / 异常吞没 / 日志风暴

日期：2026-08-15
范围：全部 26 个静态集合字段与全部 Location→cache 实例映射清点；日志与异常路径巡检

## 集合清点结论

**启动期固定（无增长风险）**：DRAINING_RECIPES、DUMMY/CRYSTAL_MAP、metaBypass、CONVERSIONS×2、RECIPES(临时工作台)、RARITY_VALUE_MAP、VALID_MATERIALS、EFFECT_TYPES、TALL_FLOWERS、MATERIAL_MAP、MATERIALS×4、COLOR_MAP 等——均为 EnumMap/一次性填充。

**Location→cache 映射（随机器数量有界，破坏时须清理）**：

| 映射 | 破坏清理 |
|------|---------|
| ChroniclerPanel.CACHES / RealisationAltar.CACHES / LiquefactionBasin.cacheMap / PrismaticGilder.cacheMap | 原有 ✓ |
| ExpCollector.volumeMap/blockOwnerMap | round-6 补齐 |
| **TickingBlockNoGui.firstTickMap** | **本轮补齐（原从不移除）** |
| **Stand.itemMap/currentTickMap** | **本轮补齐（原从不移除）** |

**爆炸路径核验**（依据 REF/Slimefun4.1 `BlockBreakHandler(false, false)` 语义）：所有本附属机器 `allowExplosions=false`——Slimefun 会取消爆炸对机械方块的破坏，不存在"爆炸摧毁机器导致映射泄漏/库存虚空"路径。

**法术运行时映射**：round-1 已全量闭环（strikeMap 过期清理、离线条目移除等）。

## 已修复（2 个 commit）

| commit | 问题 |
|--------|------|
| `f26ae63` | **firstTickMap 与 Stand 双映射无界增长**（长期运行内存泄漏）；Stand 的损坏 UUID 每 tick 异常；跨世界 distance() IAE |
| `22dafe8` | **日志风暴**：施法断路器日志原每次施法一条 WARNING+堆栈，恶意高频施放可刷爆日志——改每法术仅首次记录（集合以法术总数为上界）。清理 canCraftSatchel 死代码（含 NPE 隐患）与无用 getByItem 语句 |

## 核验为非问题

- 断路器链路：SpellTickRunnable（异常即终止该次施法）、MagicProjectile.run（异常停用消费者）、TemporaryEffectsRunnable 各 remove* 的世界卸载守卫（round-1）——均不会每周期重复抛异常。
- 图鉴/GUI 无跨请求状态；SatchelGui 每次打开新建实例。

## 已知限制（记录不改）

1. **世界删除后** Location 键条目残留于各映射（Slimefun BlockStorage 生态通病；量级为每台机器一个条目，需管理员删世界才触发，重启清空）。
2. 临时工作台 RECIPES 静态快照不含后注册附属（round-3 已记录，上游一致）。

## 验证

`JAVA_HOME=F:/Java/21 mvn -q package` 构建通过。

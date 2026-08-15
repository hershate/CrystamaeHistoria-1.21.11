# 性能优化第 1 轮：施法前置校验缓存 + SpellMemory 周期清理零复制扫描

日期：2026-08-15
基准数据：[benchmark/results/round-1.tsv](../../../benchmark/results/round-1.tsv)
红线核查：安全性 ✅ 稳定性 ✅（服务器回归通过）兼容性 ✅（无任何公共 API/数据格式变更）

## 优化点 1：`ConfigManager.spellEnabled` 改读启动期已同步的字段

**热路径**：每次施法（`InstancePlate.tryCastSpell` 唯一调用方）。

**旧实现**：`spells.getBoolean(spell.getId())` —— `MemorySection.get(String)` 每次调用
对路径做 `split` 分配 + Map 树形查找 + `Boolean` 拆箱。而 `loadConfig()`（启动期）已把
同一值读进 `Spell.enabled` 字段，运行期 `spells.yml` 从不写入。

**新实现**：`spell.isEnabled()`（Lombok 字段读取）。

**安全性论证**：`spells.set` 全仓库仅 `loadConfig()` 一处（启动期），字段与 yml 在
运行期恒等；无管理员热重载入口，不存在失同步窗口。

### 量化（真实 paper-api `YamlConfiguration`，69 键，3 fork 均值）

| 变体 | ns/次调用 | 加速比 |
|------|-----------|--------|
| 旧 `yaml.getBoolean(id)` | 8.73 | 1x |
| 新 `spell.isEnabled()` | 0.30 | **29.1x** |

> 方法论注记：新路径以与 Lombok `@Getter` 生成字节码同形态的模型类测量
> （`SpellType` 枚举实例化需要 Slimefun 运行时，无法脱离服务器加载）。

## 优化点 2：`SpellMemory` 全部 11 个周期清理方法零复制扫描

**热路径**：`TemporaryEffectsRunnable` 每 20 tick（每秒）全局驱动，无论服务器状态。

**旧实现**：每个 `remove*` 方法无条件 `new HashSet<>(map.keySet()/entrySet())` 整表
复制（空表也分配），弹射物/下落方块/召唤物路径还逐 key 二次 `map.get(k)`；空表常态
下每秒固定 11 次 HashSet 分配。

**新实现**（语义逐分支保持一致，含离线降级/断路器/世界卸载守卫）：
1. `map.isEmpty()` 早退（空表零工作）；
2. 纯移除类（strikes/flight/time/weather/enderman/spawnAreas/blocks）直接
   `entrySet` 迭代器原地 `remove`，零复制；
3. 副作用类（projectiles/fallingBlocks/entities/displayItems——`kill()` 自移除映射
   或消费者可能改表）：entrySet 单遍扫描收集待处理项，扫描结束后统一执行——
   全存活常态下仅 2 个空 `ArrayList` 壳（JDK 惰性分配，无底层数组分配）；
4. `System.currentTimeMillis()` 每方法 1 次（旧代码逐条目调用）。

**修复过程中的自我纠错**：初版 `removeDisplayItems` 漏掉旧代码的显式
`displayItems.remove()`（`DisplayItem.kill()` 不自移除，会泄漏）——已修正并复核了
`MagicProjectile`/`MagicSummon`/`MagicFallingBlock` 三个 kill 的自移除语义。

### 量化（真实 java.util 操作序列，UUID 键，3 fork 均值）

| 场景 | 旧 ns/次 | 新 ns/次 | 加速比 |
|------|----------|----------|--------|
| 空表（服务器常态） | 4.44 | ≈0.00（isEmpty 早退） | **>4000x** |
| 100 条全存活（keySet 复制+get 形态） | 1544.90 | 199.79 | **7.7x** |
| 100 条全存活（entrySet 复制形态） | 1803.56 | 199.79 | **9.0x** |

**服务器宏观收益**：常态（多数映射表为空）下每秒 11 个方法从 11 次 HashSet 分配
（各 ~4.4ns + GC 压力）降为 11 次 isEmpty 读；战斗场景（弹射物+召唤物并发）单轮
清理从 ~10μs 级降至 ~2μs 级，且零临时 HashSet 垃圾。

> 方法论注记：实体句柄类（MagicProjectile 等）需 Bukkit 服务器运行时才能构造，
> 基准以 UUID 键 + 真实 java.util 容器测量新旧代码的完整操作序列（复制分配+遍历+
> 条件判断）；加速比由操作序列本身决定，与键类型无关。

## 稳定性验证

- `mvn package` 通过（Java 21）。
- Paper 1.21.11 build 132 + Slimefun 5.0.0 实际服务器回归：插件启用正常（16.75s
  完成启动），`TemporaryEffectsRunnable` 持续 tick 数分钟，**全会话 0 插件异常**
  （日志仅离线环境的版本获取/皮肤缓存超时噪音）。

## 变更文件

- [ConfigManager.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/managers/ConfigManager.java)（spellEnabled）
- [SpellMemory.java](../../../src/main/java/io/github/sefiraat/crystamaehistoria/SpellMemory.java)（11 个 remove* 方法）
- benchmark/（新增：Harness + 2 个基准 + run.sh，3 fork 聚合）

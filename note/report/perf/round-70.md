# 性能优化第 70 轮：方块写入标志与逐 tick BlockData 克隆域

日期：2026-08-18
域：**方块写入标志（physics）与逐 tick BlockData 克隆**——探针角度
（与 r1-69 互异）：`setBlockData(data)`/`setType(mat)` 单参形态在
Paper 中默认 `physics=true`（CraftBlock 字节码实证：单参委托
`setBlockData(data, true)` → NMS `setBlock(pos, state, flag 3)` =
邻居通知 + 观察者可见；`false` → flag 530 = 客户端更新 + 抑制掉落 +
观察者不可见）。插件内从未审计过这一维度——高频道站点为此支付邻居
通知级联成本，且代码库自身已有零星 `false` 惯例（HarmonysSonata 高花
路径 / Cascada）但不成体系。

触发：第十四轮循环 r68-69 连续纯判定收敛后用户再触发（第十五轮循环
开启）。休眠重启触发条件之三（外部触发）外，本轮为用户再触发先例
（第十/十一/十二轮同模式）。

## 全库族普查（write 站点全分类）

| 站点 | 频率 | 处置 |
|------|------|------|
| `ChroniclerPanelCache.animateLight`（光源呼吸动画） | **每 tick 每工作面板** | ✅ 改 `false`（本轮主项） |
| `HarmonysSonata` 花朵写入 ×4（bisected 半位写回 ×2 / DANDELION / 单体花） | 每 tick 施法中 | ✅ 改 `false`（族一致性，与同函数高花路径既有惯例对齐） |
| `GreenHouseGlass` 作物催熟 | rate 门控每 tick | ❌ 保留 physics（边界，见下） |
| `RealisationAltarCache` 晶簇生长 ×3 | 提取步骤级 | ❌ 保留 physics（边界） |
| AIR 移除点（SpellMemory/BlockRemoval/MiscListener/MobCandle/CrystalBreak ×5） | 到期/事件级 | ❌ 保留（附着物清理语义必需） |
| `LuminescenceScoop` 光源调节（用户工具直接写） | 交互级 | ❌ 保留（用户可感知工具语义） |
| 玩法材质写入（LavaLake 岩浆/AncientDefence/StripMine/Oviparous/Bobulate/PhilosophersStone/AngelBlock/MagicPaintbrush/Exalted×2） | 施法/交互级 | ❌ 保留（physics 反应属玩法语义，且全事件级冷） |

**语义红线判定口径**（本轮沉淀）：physics 标志差异的玩家可感知面 =
邻居通知与观察者可探测性（服务器内实证：physics 写后观察者 powered=
true，noPhysics 写后 false；与 NMS flag 3/530 语义一致）。判定：
插件**内部装饰效果**的写入（面板呼吸灯、法术自产物花朵放置）不构成
玩家契约——观察者探测一个装饰动画不是受支持机制，且代码库上游已用
`false` 处理同性质站点；**玩家可构建依赖的玩法语义**（作物生长可被
观察者农场探测、晶簇成长进度、临时方块消失时附着物掉落）保留
physics。

## 实现（本轮提交 178b152 / 15e7fc8）

1. `animateLight`：`setBlockData(light)` → `setBlockData(light, false)`。
   **保留每 tick 新读 `getBlockData`**——发光勺（LuminescenceScoop）
   可外部调节面板光源等级，缓存实例会在下一 tick 覆写玩家调节（失同步
   语义风险），28.42ns 克隆成本不值得（理论缓存上限 7.61x，发货形态
   ~6.9x，见量化）。
2. `HarmonysSonata`：4 处花朵写入补 `false`（`GRASS_BLOCK` 地面转换
   维持 physics——世界材质转换与插件自产物分层，同上游惯例）。

## 量化（服务器内，round-70.tsv）

| 基准 | 变体 | ns/op | 说明 |
|------|------|-------|------|
| lightAnim | old_getClone_physics（现形态全序列） | **1954.43** | 每 tick 实际执行体 |
| lightAnim | new_cached_nophysics（理论上限） | **256.70** | 缓存实例 + false（**7.61x**，未采纳：外部写者失同步风险） |
| lightAnim | iso_getBlockData | 28.42 | 克隆成本（发货形态保留项） |
| lightAnim | iso_set_physics | 755.08 | 写入隔离（12↔13 小光差） |
| lightAnim | iso_set_nophysics | 244.10 | **标志差 510.98ns** |
| cropAge | iso_set_physics | 675.78 | GreenHouseGlass 保留的语义成本对照 |
| cropAge | iso_set_nophysics | 163.17 | **标志差 512.61ns**（与 lightAnim 隔离差互证） |

**发货形态成本**：隔离差直接外推为 1954.43 − 510.98 ≈ 1443ns，但
全序列上下文中 physics 开销远大于隔离差（1954.43 − 256.70 − 28.42 ≈
1669ns）——三角波大光差（5↔15）下邻居通知级联与光照传播叠加放大。
**保守口径：发货形态 ≈ new_cached_nophysics + iso_getBlockData =
256.70 + 28.42 ≈ 285ns，~6.9x**（每工作面板每 tick 省 ~1.67µs；
20 面板同开每 tick 省 ~33µs）。

等价性：两形态自同初态 60 步 level 序列**逐位一致 true** + 新形态
终态读回一致 true；观察者语义差异实证归档（physics=true 探测 /
false 不探测）——装饰动画判定为非契约（见红线口径）。

## 会话记录

COMPLETE=1、CH 插件错误 0、watchdog 2 次（重基准批阻塞主线程固有
伪象，r49 起口径）。变更后 jar 启动回归通过（Done + CH/SF 启用 +
0 错误 + 0 watchdog，boot_r70.log）。

## 判定

族矩阵增补行：**方块写入 physics 标志——结构域（~6.9x，每 tick 每
工作面板）**。第十五轮循环开启轮命中：热成员（animateLight）为
全库唯一每 tick 方块写入站点（GreenHouseGlass 已 rate 门控且语义
保留），族一次闭合。下一轮判定/清扫轮换新角度。

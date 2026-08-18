# 性能优化第 72 轮：判定轮（集合快照分配 / instanceof 分派 / 消息格式化形态）

日期：2026-08-18
域：**判定轮**（第十五轮循环收敛 2/2）。探针角度（与 r1-71 互异）：
N. 集合快照分配形态（toArray/Arrays.copyOf/List.copyOf 等每调用
复制）；T. 事件处理器 instanceof 链分派形态；O. String.format/
MessageFormat 运行期站点复核（r9 已清统计路径，本轮查全域）。

## 角度 N：集合快照分配形态——零发现

全库 `grep '\.toArray(\|Arrays.copyOf\|List.copyOf\|Set.copyOf\|Map.copyOf'`
**零命中**——该家族在本代码库不存在（历轮清扫已消灭或从未存在；
`values()` 克隆已在 r46 处理）。

## 角度 T：instanceof 分派形态——零发现

监听器包 instanceof 密度：每文件最多 5 处（SpellEffectListener/
PoseChangerListener），逐点核验均为**单发类型检查**（FallingBlock/
Player/LivingEntity/WitherSkeleton 判别），无同事件多级 instanceof
级联、无重复同类型检查可合并——单次 instanceof 经 JIT 后 ~ns 级
（checkcast/branch），无优化空间。

## 角度 O：消息格式化站点——零发现

全库 `String.format`/`MessageFormat` 站点逐点分类：

| 站点 | 频率 | 分类 |
|------|------|------|
| `CrystamaeHistoria.java:196` | 启动期 | 冷路径 |
| `PoseChangerListener:160/181` | 姿态工具交互消息 | 事件级 |
| `CrystamageSatchel:67` | 收纳袋交互消息 | 事件级 |
| `GildedCollectionFlexGroup:158` | 图鉴 lore 构建 | 翻页级且 r21 快照缓存下游 |
| `Story.java:60` | 配置装载校验 | 启动期 |

tick 路径零命中——格式化域在 r9（统计路径 12.4x）后已闭合，本轮
全域复核维持判定。

## 判定

三个互异新角度均零发现——**r71+r72 连续判定轮零发现（角度互异），
按 round-44 准则宣告第十五轮循环收敛**。本轮循环形态：r70 实质
1 轮（方块写入标志域 ~6.9x）+ 判定 2 轮——与第十至十二轮"开启轮
命中后快速收敛"同形态。收口版本 0.16.0（终验 + soak 见
[note/release/0.16.0.md](../../release/0.16.0.md)）。

## 循环总账（第十五轮）

| 轮次 | 性质 | 结果 |
|------|------|------|
| 70 | 实质清扫 | 方块写入 physics 标志域：animateLight ~6.9x + HarmonysSonata 族一致性 |
| 71 | 判定 | 重复状态应用/世界时间查询——零 |
| 72 | 判定 | 集合快照分配/instanceof 分派/消息格式化——零 |

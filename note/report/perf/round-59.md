# 性能优化第 59 轮：回调内 NamespacedKey 构造改静态常量（卫生级清扫）

日期：2026-08-17
域：**回调内 NamespacedKey 构造形态**——探针角度（与 r42-58 互异）：
配置访问链 / `Keys.newKey()` 每调用构造 / 物品栏写入形态。全库 56 处
newKey 分类后命中 6 处回调内固定名构造（违反项目自身 `Keys.PDC_*`
常量惯例）。

## 实现（本轮提交 e69ee02）

- `Keys.java` 增补 `PDC_PRISM` / `PDC_ANTIPRISM` / `PDC_RECALL_LOCATION`
  常量（沿用 PDC_* 族工厂惯例）；
- `Prism`/`AntiPrism`（弹射物命中/施法对抗标记，每命中 2-3 次构造）
  与 `RecallingCrystaLattice`（路标位置写入 + 读取路径双构造）共 6 处
  调用点转静态常量引用。

**边界（不可常量化，保持原样）**：ItemGroups ×15（启动期静态块）、
`RealisationAltarCache` ×3（位置动态键名——按方块位置生成，构造上
必须每键新建）、`AbstractGoal`（每实例构造器一次）。配置静态链 32
处均为 JIT 内联 getter 链（r56 五证覆盖）；`addItem` 形态全事件级。

## 量化（服务器内，round-59-server.tsv）

| 基准 | 旧（每调用构造） | 新（静态引用） | 提升 |
|------|----|----|------|
| pdcKey.prismFlag | 57.10 ns | 3.43 ns | **16.65x** |

等价性：常量与原构造的 namespace/key 逐位一致 true。绝对量
~54ns/构造（Prism/AntiPrism 每命中省 ~110-160ns）——**事件级边际
卫生**，主要价值为 Keys.PDC_* 惯例一致性（该族存在的本意）。

## 会话记录

COMPLETE=1、CH 插件错误 0、watchdog 3 次（基准批次固有）。

## 判定

族矩阵增补行：**回调内 NamespacedKey 构造——卫生级（16.65x，事件级
边际）**，可转换成员全部闭合。**收敛计数重置**（r58 零发现被本轮
发现打断，r49/r44 同型）——下一轮起重新累计连续判定轮零发现。

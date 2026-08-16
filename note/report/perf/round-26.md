# 性能优化第 26 轮：故事列表 PDC v2 瘦编码（第五轮循环第 1 轮）

日期：2026-08-16
域：**故事列表 PDC 编码**——每次故事提交/提取的序列化/反序列化。
红线（按用户澄清口径）：用户体验一致 + 旧存档可读（v1 双读兼容）。

## 优化点

v1（`PersistentStoriesDataType`）：N 个子容器 × 2 键（每故事 2 次 PDC
读/写 + 1 次容器分配）；5 故事物品每次提交 10 读 + 10 写 + 5 分配。
v2（新 `PersistentStoriesV2DataType`）：单容器 2 键——id 以 NUL 连接的
单一 STRING + 稀有度 INTEGER_ARRAY（均 PDC 内建原语），键操作 2N → 2，
附带物品 NBT 负载缩小（5 故事：6 条目 → 2 条目）。

## 实现（e60f5d6）

- `Keys`：`PDC_STORIES_V2`（s_list2）+ 容器内两键（s_ids/s_rars）。
- v2 类型：写入守卫（id 含 NUL 拒绝编码——正常 YAML 配置名不可能）；
  读取结构损坏（缺键/长度不匹配，仅 crafted 可造）抛 ISE 由
  `StoryUtils.getAllStories` 捕获降级 v1，限次告警，不产生 tick 异常；
  逐条目失败关闭（非法稀有度/缺池跳过）与 v1 语义一致。
- **双读兼容（旧存档红线）**：读取 v2 优先、v1 回退；统一写路径
  `writeStories`（写 v2 + 移除残留 v1 键）——v1 物品一经任何写路径
  触碰即迁移为 v2，物品上不留双份编码。
- 信息量与 v1 完全一致（id + 稀有度 → 全局故事池实例解析）。

## 量化（服务器内真实 PDC，round-26-server.tsv）

| 基准 | v1 | v2 | 提升 |
|------|----|----|------|
| storyPdc.serialize5（5 故事序列化） | 900.15 ns | 431.40 ns | **2.09x** |
| storyPdc.deserialize5（5 故事反序列化） | 2,052.54 ns | 1,147.68 ns | **1.79x** |
| writePath.storyCommitV2.firstStory（0→1 首故事端到端） | 9,920.30 ns | 8,147.78 ns | 1.22x |
| writePath.storyCommitV2.fourthStory（3→4 端到端） | 21,710.50 ns | 21,456.76 ns | ~持平（+1.2%） |

端到端说明：提交路径由 lore/名称/meta 应用主导（round-24 已判定
Paper ItemMeta 应用为 API 边界，3-4 故事物品 ~19µs），PDC 份额的
组件级 2x 收益在端到端被稀释；firstStory（PDC 份额占比更高）1.22x。
提取路径（removeStoryAndRebuild：反序列化 N + 序列化 N-1）同享组件级
收益。基准过程中修正了一次副本漂移（旧编码对照缺计数/附魔簿记，
曾致端到端虚假负值——修正后为上表数据）。

## 等价性与回归

- 服务器内断言：v1/v2 各自往返解析为同一池实例列表（roundTrip）；
  v1 物品经双读正确（dualReadV1）；v1 物品迁移后读数一致且 v1 键
  从键集移除（migration）——**全 true**；
- 会话 COMPLETE、0 SEVERE；
- 行为等价：故事解析结果、提交/提取终态与 v1 一致；crafted 损坏
  数据降级语义与 v1 逐条目跳过同级。

## 兼容性

- **旧存档完全可读**（v1 键保留至首次写入迁移）；新物品只写 v2；
- 降级到旧版本插件无法读 v2 物品（从未承诺的方向性，发布说明注明）；
- 无对外 API 变更（`getAllStories` 等签名与语义不变）。

## 方法论

- PDC 域全部服务器内实测（无法脱离服务器运行时，无 standalone 变体）；
- 端到端对照副本必须逐行复刻真实路径的簿记（计数/附魔），
  否则对比失真——本轮实测教训。

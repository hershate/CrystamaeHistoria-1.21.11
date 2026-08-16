# 性能优化第 27 轮：故事上限 JSON → 扁平 int 键（第五轮循环第 2 轮）

日期：2026-08-16
域：**PDC_POTENTIAL_STORIES 编码**——故事上限（makeStoried 掷入的
潜力计数）以 JsonObject 存储，判定链（verdict refresh）与提交路径
（commitStory 的满槽判定）每物品实例多次 gson 解析。
红线（用户澄清口径）：用户体验一致 + 旧存档可读（JSON 双读回退）。

## 优化点

JSON 内容实为 **2 个数字**（JS_S_AS 计数 + JS_S_T 层级），且 **tier 只写
不读**（全库唯一消费值为 available 计数）——JsonObject + gson 逐次解析
纯属浪费。扁平化为单 int 键 `PDC_STORY_LIMIT`。

## 实现（dcccb01 + 直接掷值重构）

- `Keys.PDC_STORY_LIMIT`（s_lim_i，INTEGER）。
- 读侧 `getMaxStoryAmount(meta)` 双读：int 键优先（值类型不匹配按缺失，
  crafted 防御与 JSON 路径同级）；旧 JSON 回退（防御解析逐行保留，
  缺失/非数字/异常按 0）。ItemStack 重载归并到 meta 重载（原为重复解析）。
- 写侧 `makeStoried`：既有 int → 沿用（重复 makeStoried 保持原掷值）；
  否则既有 JSON（旧编码物品）解析迁移；否则**直接掷值**（不经 JSON
  构造——初版实现曾绕 JsonObject 一圈，自查后重构）；残留 JSON 键移除
  ——与故事列表 v2（round-26）相同的迁移语义。
- `getStoryLimits`/`getInitialStoryLimits` 保留（对外可见的公共方法，
  JSON 键语义不变）。

## 量化（服务器内真实 PDC + gson，round-27-server.tsv）

| 基准 | JSON（旧） | int（新） | 提升 |
|------|----|----|------|
| storyLimit.read（上限读取，判定链/提交每物品实例多次） | 1,258.16 ns | 651.15 ns | **1.93x** |
| storyLimit.makeStoried（潜力锁定端到端） | 2,254.70 ns | 1,483.57 ns | **1.52x** |

## 等价性与回归

- 服务器内断言：makeStoried（int 落盘 + JSON 键移除 + 读数 1-5）/ 
  jsonFallback（旧 JSON 物品双读 = 4）/ crafted（非数字 JSON → 0）
  **全 true**；
- 会话 COMPLETE、0 SEVERE。

## 兼容性

- 旧存档（JSON 编码物品）完全可读（双读回退），一经 makeStoried 触碰
  即迁移（正常流程中 storied 物品不再触发 makeStoried，旧编码物品的
  JSON 将保留至物品生命终点——读取侧永久兼容）；
- 降级到旧版本无法读 int 键物品（从未承诺的方向性，同 round-26）；
- 对外方法 `getStoryLimits`/`getInitialStoryLimits` 保留原语义。

## 方法论

- 编码扁平化前先核验**消费面**：本轮 tier 只写不读的发现使编码从
  2 数字降为 1（round-26 的 rarity 数组同理消费面核验先行）。

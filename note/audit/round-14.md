# 审计第 14 轮：交叉复审——故事系统核心与 tools 残余

日期：2026-08-15
范围：`utils/StoryUtils`（故事数据操作核心，全量精读）+ tools 残余类（RefactingLens/ConnectingCompass/BalmySponge/SpiritualSilken）

## 已修复（1 个 commit）

| commit | 问题 |
|--------|------|
| `489c23d` | **空故事池越界**：`addStory` 在某稀有度下无对应类型故事时 `nextInt(0, 0)` 抛 IAE——generic-stories.yml 可被编辑，记录者面板每 tick 报错（机械死亡）。**伪造 JsonObject**：`getMaxStoryAmount` 直接 `.get(key).getAsInt()`——改造客户端可伪造部分键/非数字/坏 JSON 的 PDC，`hasRemainingStorySlots` 每 tick 异常卡死机械。空集/异常一律失败关闭（按 0 槽位处理）；`requestNewStory`/`requestUniqueStory` 的定义缺失 NPE 防御 |

## 核验安全（记录依据）

- BalmySponge：水/岩浆吸收逐块 BREAK_BLOCK 校验；饱和态 PDC/BlockStorage 往返一致；破坏掉落重构 ✓
- SpiritualSilken：领地校验 + ItemSetting 白名单开关 ✓
- ConnectingCompass：跨世界引导线已有同世界判定；伪造磁石 world==null 时 equals 安全跳过 ✓
- RefactingLens：纯标记类（由监听器消费）✓

## 记录（不改）

1. `removeStory` 依赖 Story 注册表单例的引用相等（PersistentStoriesDataType 反序列化返回注册表共享实例，identity remove 恰好成立）——脆弱但正确的上游设计，改动需引入 equals 语义（影响面大），记录。

## 验证

`JAVA_HOME=F:/Java/21 mvn -q package` 构建通过。

# note

本目录存放项目要点文档。

## 版本发布记录

- [0.16.0](release/0.16.0.md)（当前）：第十五轮性能优化版——**方块写入
  标志域（r70-72）**：面板光源呼吸动画免 physics（1954.43→~285ns，
  ~6.9x，每 tick 每工作面板）+ HarmonysSonata 花朵写入族一致性；
  语义红线口径沉淀（内部装饰效果非契约 vs 玩法可构建依赖语义保留
  physics）；r71+r72 连续判定零发现收口；无数据格式变更，旧存档完全兼容。
- [0.15.0](release/0.15.0.md)：第十四轮性能优化版——**判定
  收敛版（r68-69）**：两轮判定轮连续零发现（枚举键容器/不可变集合
  构造 + Objects.hash/自定义 hashCode，角度互异），按循环准则收口；
  **本版无代码变更**（如实标注），兼容性与 0.14.0 完全一致；连续
  两轮纯判定收敛循环指示复查节奏边际产出稳定为零，**循环进入休眠**
  （soak 因用户终止收尾未跑满 10 分钟，已完成 ~6 分钟段全零异常，
  如实归档）。
- [0.14.0](release/0.14.0.md)：第十三轮性能优化版——**判定
  收敛版（r66-67）**：两轮判定轮连续零发现（玩家视图/teleport 形态
  + 异步/getRelative 形态，角度互异），按循环准则收口；**本版无代码
  变更**（如实标注），兼容性与 0.13.0 完全一致。
- [0.13.0](release/0.13.0.md)：第十二轮性能优化版——**生成
  预配置与复用域（r62-65）**（实体生成 consumer 预配置三路径 1.93x/
  1.54x + 元数据包 N→1 收益；FallingBlock BlockData 静态复用
  16.56x；r64+r65 连续零发现判定轮宣告循环收敛）；无数据格式变更，
  旧存档完全兼容。
- [0.12.0](release/0.12.0.md)：第十一轮性能优化版——**粒子
  批量化与常量化域（r57-61）**（随机云粒子批量化 n5 4.76x/n10
  13.36x + N 包→1 包收益；回调内 NamespacedKey 常量化 16.65x；
  r60+r61 连续零发现判定轮宣告循环收敛）；无数据格式变更，旧存档
  完全兼容。
- [0.11.0](release/0.11.0.md)：第十轮性能优化版——**事件级
  门控域（r53-56）**（事件级 getByItem 材质门控 8 处 2.81-3.00x /
  36 槽扫描 2.81x；ByType 切片阴性回退——Paper 1.17+ 实体扁平化
  边界；r54+r56 连续零发现判定轮宣告循环收敛）；无数据格式变更，
  旧存档完全兼容。
- [0.10.0](release/0.10.0.md)：第九轮性能优化版——**复查长尾域
  收官（r42-52）**（T5 吸取 8.97x；cast 路径 stream 惯用法 113x/7.05x；
  values() 逃逸克隆与集合双复制 7.85-19.9x；动画死分支移除；球面扫描
  O(n²) 去重消除 39.2x/152.8x——高等级每次命中省 ~3ms；sqrt 消除阴性
  回退边界确认；r51/52 连续零发现判定轮宣告循环收敛）；无数据格式变更，
  旧存档完全兼容。
- [0.9.0](release/0.9.0.md)：第八轮性能优化版——**复查节奏长尾域**
  （perf 第 34-40 轮：周期落盘 ~283,000x / 展示架与共享解析弱缓存 177-178x；
  第 41 轮循环闭合）；无数据格式变更，旧存档完全兼容。
- [0.8.0](release/0.8.0.md)：第七轮性能优化版——**落盘与持久化域**
  （perf 第 34 轮周期落盘脏判定 ~283,000x 稳态零磁盘写、第 35 轮镀金器
  扫描边界确认；第 36 轮循环闭合）；无数据格式变更，旧存档完全兼容。
- [0.7.0](release/0.7.0.md)：第六轮性能优化版——**统计读取域收官**
  （perf 第 31 轮解锁集合纪元缓存：法术集页 8.96x / 故事集页 25.3x，
  成员/计数纪元分离；第 32 轮循环闭合）；无数据格式变更，旧存档完全兼容。
- [0.6.0](release/0.6.0.md)：第五轮性能优化版——**物品/区块 PDC
  编码域全扁平化**（perf 第 26-29 轮：故事列表/故事上限/法杖存储/区块晶簇，
  键操作 O(N)→O(1)，组件级 1.5-3.2x；四组双读兼容，第 30 轮循环闭合）；
  旧存档/旧法杖/旧机械数据完全可读。
- [0.5.0](release/0.5.0.md)：第四轮性能优化版——图鉴展示/统计读取两域
  （perf 第 21-23 轮，第 24 轮 lore 组件化阴性回退，第 25 轮循环闭合）；
  红线口径澄清（用户体验一致 + 对外 API 不变）；无数据格式变更，旧存档兼容。
- [0.4.0](release/0.4.0.md)：第三轮性能优化版——写路径/展示行/召唤物 AI/
  周期任务/分配五域（perf 第 15-19 轮，第 20 轮循环闭合）；无数据格式变更，旧存档兼容。
- [0.3.0](release/0.3.0.md)：第二轮性能优化版——世界级事件/液化池/启动/
  监听器门控四域（perf 第 10-14 轮，循环闭合）；无数据格式变更，旧存档兼容。
- [0.2.0](release/0.2.0.md)：性能优化版——9 轮性能优化循环
  （量化对比见 [report/perf/](report/perf/README.md)，基准设施 [benchmark/](../benchmark/)）；
  无数据格式变更，旧存档兼容。
- [0.1.0](release/0.1.0.md)：**版本序列自 0.1.0 起算**。28 轮系统审计修订版（117+ 项
  稳定性/安全性/正确性修复，8 次服务器回归验证，详见 [audit/](audit/README.md)）；
  无数据格式变更，旧存档兼容。
- [1.21.11-1](release/1.21.11-1.md)（历史）：迁移至 Paper 1.21.11 + Slimefun 5.0.0（REF/Slimefun4.1），
  移除 bstats / GuizhanLibPlugin / InfinityLib / EffectLib / MorePersistentDataTypes 及全部第三方可选插件集成。

## 专项分析

- [代码库全量分析](analysis/2026-08-16/index.md)：项目根完整分析（264 个 Java 文件，v0.2.0）——
  5 层事件驱动架构、运行原理（施法/故事管线/SpellMemory 状态管理）、工作流、AI 替代方案评分，
  附 4 份 Skill Blueprint；索引见 [analysis/](analysis/README.md)（2026-08-16）。
- [法术系统结构化分析](spell-system-analysis.md)：`magic/`、`listeners/`、`runnables/` 包与 `SpellMemory.java` 的核心抽象、注册/触发/执行机制、69 个法术的数量与层级划分、监听器与定时任务职责、法术执行流程调用链（2026-08-15）。

## 性能优化轮次

持续性能优化（红线：安全/稳定/兼容，量化见 [benchmark/](../benchmark/)，
报告与索引见 [report/perf/](report/perf/README.md)）：
第一轮第 1-9 轮完成并**闭合**（收敛判定见 [round-9](report/perf/round-9.md)）——
施法前置校验 29x；SpellMemory 零复制 8x；施法触发懒 raycast；交互路径 ItemMeta
削减 8.9x；机械 tick 判定备忘录 1010x；法杖单槽 PDC 读取 1.6x；gadgets 每 tick
清扫 2.0-5.9x；故事选取索引 21x + 配置双解析消除 2.3x；统计路径 12.4x；
v0.2.0 全套基准 + 10 分钟 soak 终验 0 异常 0 tick 落后。
**第二轮循环**（完全重写授权）第 10-14 轮完成并**闭合**（收敛判定见
[round-14](report/perf/round-14.md)，版本收口 0.3.0）：第 10 轮世界级高频事件
路径 O(1) 化（弹射物/下落方块反查 20-38.5x、无敌内存注册表 4.3-5.7x、
召唤物类型门控 5.0x）；第 11 轮液化池路径全套（syncBlock 脏标记 431x、
配方索引 140x、top-3 单遍 7.5x）；第 12 轮启动路径（稳态零配置落盘 16x、
首启批量补键 63.8x、分段计时画像）；第 13 轮剩余全局监听器门控审计收官
（isStoried 元克隆门控 5.8x ×4 处）；第 14 轮全量终验 + soak + 收敛判定。
**第三轮循环**（写路径与提交域）第 15-17 轮完成（索引见
[report/perf/](report/perf/README.md) 第三轮循环表）：第 15 轮写路径单次
元数据往返归一——故事提交 8-10 次 ItemMeta 克隆归并为 1 次（2.47x，原子性
更优）、祭坛提取 1.30x、统计计数单路径 1.17x、法杖 lore 静态片段 1.08x，
四组终态等价性运行时断言全通过；发现上游遗留"有故事的"名称前缀叠加缺陷
（未修，留待审计轮）。第 16 轮展示行构建缓存——Story.getDisplayName/
getStoryLore 记忆化（displayName 49.5x / loreLines 40.6x / 4 条组装段 9.9x，
跨稀有度采样等价性 true）。第 17 轮召唤物 AI 每 tick 路径——冲撞车块扫描
13.5x（O(n²) contains 去重为构造上无效的工作）、主人在线查询 5.4x、
GoalType 集共享常量 3.1x，四组等价性断言 true。第 18 轮法术周期效果与
周期任务路径收官——TunnelBore 块扫描 O(n²) 去重消除（等级 5 施法每 tick
3.24ms → 12µs，271.4x），runnables 全域 6 任务逐类核查。第 19 轮热循环
Location 分配消除（粒子路径）——ChillWind 球面 1.18x、其余噪声级，判定
分配削减域已到可测阈值之下。第 20 轮全量终验 + 纯空闲 soak + 收敛判定，
版本收口 0.4.0——**第三轮性能优化循环闭合**。
**第四轮循环**（展示与图鉴域）第 21-22 轮完成（索引见
[report/perf/](report/perf/README.md) 第四轮循环表）：图鉴 GUI 展示路径——
三个 FlexGroup 翻页排序快照（故事/镀金集每页复制+排序 56.5µs → 5.2ns，
10,947x；法术集 541x，并消除对 `SpellType.enabledSpells` 共享缓存数组的
原地排序污染）、法术主题堆/页面图标/详情堆记忆化（225.9x / 182.0x /
1,421.1x）、NameUtils Title Case 缓存（4.5x）；六组服务器内等价性断言
全 true，会话 0 SEVERE。第 22 轮玩家统计读取路径——图鉴页级统计子节
单次解析 + 36 槽相对读取（页判定 2.06x）、三个解锁计数内层相对读取
（服务器内 2.23x/1.76x；standalone 计数比率 1.89-1.95x，原 6.3x 为
基准除数缺陷已更正）；逐键等价性断言（全路径 vs 相对/计数一致/缺失
语义）全 true。第 23 轮统计计数纪元缓存——全部 6 个统计写点递增纪元
（核验无外部写者，推翻 round-22 不做项前提），三个计数方法字段级纪元
缓存：服务器内故事键计数 68.9µs → 8.77ns 命中（~7,859x），液化池
rank 谓词稳态 11.17ns（>6,000x）；失效正确性经真实写方法断言全 true。
第 24 轮 lore 展示写入组件化——**试行后回退（阴性结果）**：
`lore(Components)` 实测比 `setLore(Strings)` 慢 48-133%，纯转换仅
~132ns/行（原瓶颈归因错误，18-21µs 主体为 Paper ItemMeta 应用机制，
属 API 边界）；基准证据保留。第 25 轮 v0.5.0 全量终验（134 变体，
round-13~23 全部等价性断言 true）+ 纯空闲 soak（0 SEVERE 0 tick 落后）
+ 收敛判定——**第四轮性能优化循环闭合，版本收口 0.5.0**。
**红线口径（用户澄清）**：兼容 = 用户体验一致 + 对外插件 API 不变；
内部实现/内部调用可自由重构，不必逐方法等价。
**第五轮循环**（编码与数据域）第 26 轮完成（索引见
[report/perf/](report/perf/README.md) 第五轮循环表）：故事列表 PDC v2
瘦编码——单容器两键（NUL 连接 id 串 + 稀有度 int[]）替代 N 子容器×2 键，
serialize5 2.09x / deserialize5 1.79x / 首故事提交端到端 1.22x，物品
NBT 负载缩小；v1 双读兼容（旧存档可读，一经写入即迁移），crafted
损坏降级语义与 v1 同级；往返/双读/迁移断言全 true。第 27 轮故事上限
JSON → 扁平 int 键（tier 只写不读，消费值仅 1 数字）——判定链/提交的
gson 逐次解析消除，read 1.93x / makeStoried 1.52x；JSON 双读回退 +
makeStoried 迁移，断言全 true。第 28 轮法杖存储 v2 扁平编码——单容器
五键（槽位/法术连接串 + tier/crysta 数组 + cooldown 长数组）替代每板
子容器×2 键（24→5 键操作，同键双读自动迁移）：deserialize4 2.43x /
serialize4 2.72x / 施法前置单槽读 1.55x；fullRead/singleSlot/migration
断言全 true。服务器测试拦截一处红线级缺陷（readSlotPlate 裸 PDC 读取
v1 值抛 IAE，已修）——裸 PDC 调用必须自防御，方法论沉淀。第 29 轮
区块晶簇故事状态 v2 扁平编码——单容器五键 + 共享世界 UUID（同区块键下
位置必然同世界）替代 N 子容器×5 键（5N→5 恒定，同键双读）：
serialize5 3.17x / deserialize5 2.83x，saveMap 每提取步骤 -1.1µs；
dualRead/migration 断言全 true。第 30 轮 v0.6.0 全量终验（156 变体，
round-13~29 等价性断言全 true）+ 纯空闲 soak（0 SEVERE 0 tick 落后
0 watchdog）+ 收敛判定——**第五轮性能优化循环闭合，版本收口 0.6.0**。
**第六轮循环**（统计读取域收官）第 31 轮完成：解锁集合纪元缓存
（成员资格纪元与计数纪元分离——计数写不失效集合，断言实证）——
图鉴页 36 槽 Set.contains：法术集页 8.96x / 故事集页 25.3x（vs 相对
读取），重建仅由解锁写触发；六项等价性断言全 true。过程沉淀：批量
文本替换后必须核对同名字段 check/set 配对（半应用缺陷由服务器断言
暴露）；基准设施三项慢宿主加固。第 32 轮 v0.7.0 全量终验（161 变体，
20 条等价性断言行全部符合预期）+ 纯空闲 soak（0 异常 0 tick 落后）
+ 收敛判定——**第六轮性能优化循环闭合，版本收口 0.7.0**。六轮循环
（32 轮）总账见 [report/perf/round-32](report/perf/round-32.md)。
第 33 轮域穷尽判定（无新优化）。**第七轮循环**（落盘与持久化域）
第 34 轮：周期落盘脏判定跳过（修订 round-33 判定——运行期周期路径
未被覆盖）——统计纪元水位线判定 player_stats 落盘，config 运行期
无写入方首周期后跳过：稳态 0.69ms 序列化+写盘 → 2.44ns（~283,000x）；
关服 force 无条件冲刷，落盘语义断言全 true。第 35 轮镀金器空载 tick 扫描减半
（首扫空跳过次扫，结构性蕴含）：1.52x 但绝对量噪声级——Paper 空域
实体扫描近免费，实测确认该域已在边界。第 36 轮 v0.8.0 全量终验
（165 变体，3 处 false 均为文档化预期）+ 空闲 soak + 收敛判定——
**第七轮性能优化循环闭合，版本收口 0.8.0**。七轮循环（36 轮）总账见
[report/perf/round-36](report/perf/round-36.md)。第 37 轮判定：伤害权限门控
为环境依赖边界（基础 ~百 ns；集成环境 µs-ms，缓存因 cast 中途语义
风险拒绝）。第 38 轮粒子展示任务玩家筛除（1.20x 噪声级，卫生性变更；
Proxy 玩家桩基准方法论沉淀）。第 39 轮展示架 afterTick 解析弱缓存化（复查轮实质发现，
r7 gadget 清扫未覆盖 Stand 子类）：每 tick 每 架 getByItem 全量
meta+PDC 读 → WeakHashMap 弱缓存——178x（1305→7.32ns，每架每 tick
-1.3µs）。第 40 轮共享 SF 解析弱缓存（r39 同族扩展）：
utils/SlimefunItemResolver 统一液化池满池滞留/镀金器拉取中物品/
Stand 三处 tick 路径——177x（1279→7.22ns）。第 41 轮 v0.9.0 全量终验（171 变体，
25 条断言行全部符合文档化预期，0 SEVERE）+ 空闲 soak + 收敛判定——
**第八轮性能优化循环闭合，版本收口 0.9.0**。八轮循环（41 轮）总账见
[report/perf/round-41](report/perf/round-41.md)。第 42 轮 getBlockData 族清扫
（唯一双读 ExaltedHarvester 归一）：实测持平（~30ns NMS 引用读，
族边界确认）——长尾代际递减指示逼近真实地板。第 43 轮族探针穷尽判定：
7 族矩阵 + 未探族零命中——**插件侧性能面已到可测地板**，后续以
判定轮为主除非外部触发（Paper 演进/玩法变更/集成画像）。第 44 轮 T5 吸取 stream().findFirst()
改直接迭代（r43 判定首轮复核即修订）：8.97x（24.7→2.75ns）——
地板下仍有单行惯用法级裂缝；复查最终形态为惯用法清扫与判定轮交替。
第 45 轮法术 cast 路径 stream 惯用法全库清扫（r44 族扩展，三处：
EasterEgg 静态缓存 / Bobulate 标签备忘 / HarmonysSonata 直接迭代）：
列表重建 112.98x（318.6→2.82ns）/ 随机取材 7.05x（258.3→36.6ns），
等价性全 true——"流式惯用法"族已知调用点闭合。第 46 轮枚举 values()
克隆与集合双复制（相邻形态）：Bobulate DyeColor 静态数组 7.85x /
ExaltedFertilityPharo tick 路径 toList() 二次复制改直接迭代
19.9x-4.5x / BalmySponge 实测持平（纯迭代克隆已被 JIT 逃逸分析消除，
第三次 EA 实证——values() 克隆仅在数组逃逸时有实际成本）。第 47 轮
FloatingHeadAnimation 每 tick 死分支移除（r19 族漏网处，
branchInert 断言实证 directionUp 恒不翻转）：1.18x 噪声级 +
死字段清除——第四次 EA 实证（getLocation 分配已被逃逸分析消除），
EA 四证构成边界结论：清扫目标应锁定逃逸形态（流包装/二次传递/
跨调用保留），不逃逸的短命分配在 C2 下近免费。第 48 轮惯用法清扫域
穷尽判定（r44-47 收口）：族矩阵五增补行 + 未探族零命中——代际递减
113x→7.85x→1.18x→零发现，地板判定维持并强化，后续预期判定轮为主
除非外部触发。第 49 轮球面扫描 O(n²) 去重消除（判定轮转清扫轮，
List.contains 新探针角度命中 r18 族漏网两成员 Cascada/PlutosDecent）：
r5 39.2x / r8 152.8x（2.99ms→19.5µs，高等级每次命中省 ~3ms，r44 族
以来最大单项），产物逐位一致——地板判定第四次修订（48→49），"连续
判定轮零发现"准则重置；方法论：判定轮必须轮换探针角度，单一角度
零发现不可作地板证据。第 50 轮球内判定 sqrt 消除——阴性结果（试行
后回退，r24 先例）：整数平方比较精确等价但实测持平偏差（sqrtsd
单指令近免费，成本由 getBlockAt 主导），基准证据保留，族矩阵增补
"循环内超越函数"边界行。第 51/52 轮连续零发现判定轮（角度互异：
嵌套复杂度/重复查找/复制 + 调度分配/异常构造/时钟读）——**复查循环
收敛宣告**，版本收口 0.10.0；r42-52 小节：实质 4 轮（44/45/46/49）、
阴性边界 1 轮（50）、卫生 1 轮（47）、判定 5 轮，总账见
[report/perf/round-52](report/perf/round-52.md)。第十轮循环（用户再触发，
2026-08-17 起）第 53 轮事件级 getByItem 材质门控（8 处高频处理器）：
miss 2.91-3.00x / 36 槽扫描 2.81x，等价性全 true；方法论修正——
getByItem miss 路径 ~19ns 廉价短路 vs hit 路径 1.3µs（r39）。第 54 轮
判定轮零发现（BlockStorage 读取/菜单逐槽/字符串拼接三角度，第十轮
收敛计数 1/2）。第 55 轮实体扫描类型切片化——阴性结果（试行 25 处
后回退）：Paper 1.17+ 实体系统扁平化，ByType 切片无益（living 回退
36.8%）——平台版本敏感性方法论沉淀。第 56 轮判定轮零发现（循环内
重复调用角度，29 处命中全分类：语义独立/JIT 内联/已缓存事件级）
——r54+r56 连续零发现，**第十轮循环收敛宣告**，版本收口 0.11.0；
总账见 [report/perf/round-56](report/perf/round-56.md)。第十一轮循环
（用户再触发）第 57 轮随机云粒子批量化：displayParticleEffect 两变体
count+offset 单次调用——n5 4.76x / n10 13.36x + N 包→1 包收益。第 58 轮
判定轮零发现（ItemStack 比较/声音/容器克隆/玩家查询四组探针全空，
第十一轮收敛计数 1/2）；FlexGroup 声音 lambda 捕获疑点转审计域。第 59 轮
回调内 NamespacedKey 构造改静态常量（Prism/AntiPrism/回忆水晶格 6 处）：
16.65x 事件级边际卫生 + Keys.PDC_* 惯例一致——收敛计数重置。第 60 轮
判定轮零发现（遗留集合/数据结构选型/掉落 API 三角度，第十一轮收敛
计数 1/2·重置后）。第 61 轮判定轮零发现（装箱键/日志守卫/Optional
三角度）——r60+r61 连续零发现（角度互异），**第十一轮循环收敛宣告**，
版本收口 0.12.0；总账见 [report/perf/round-61](report/perf/round-61.md)。
第十二轮循环（用户再触发）第 62 轮实体生成预配置 consumer 化：弹射物/
召唤物/展示物三路径配置随生成包广播——1.93x/1.54x + 元数据包 N→1 收益。
第 63 轮 FallingBlock 的 BlockData 静态复用（PlutosDecent 四材质缓存）：
16.56x 卫生级（高等级陨石每施法省 ~0.13ms）。第 64 轮判定轮零发现
（getBlockState 快照/区块操作/特效三角度，第十二轮收敛计数 1/2）。第 65 轮
判定轮零发现（射线/眼部形态角度）——r64+r65 连续零发现（角度互异），
**第十二轮循环收敛宣告**，版本收口 0.13.0；总账见
[report/perf/round-65](report/perf/round-65.md)。第十三轮循环（用户再
触发）第 66 轮判定轮零发现（玩家视图克隆/teleport 形态，收敛 1/2）。
第 67 轮判定轮零发现（异步形态/getRelative 遍历）——r66+r67 连续
零发现（角度互异），**第十三轮循环收敛宣告**，版本收口 0.14.0
（判定收敛版，无代码变更）；总账见
[report/perf/round-67](report/perf/round-67.md)。第十四轮循环（用户再
触发）第 68 轮判定轮零发现（枚举键容器选型/不可变集合构造，收敛 1/2）。
第 69 轮判定轮零发现（Objects.hash/自定义 hashCode/Map 键哈希）——
r68+r69 连续零发现（角度互异），**第十四轮循环收敛宣告**，版本收口
0.15.0（判定收敛版）；连续两轮纯判定收敛循环指示复查节奏边际产出
稳定为零，循环进入休眠（触发条件：Paper/Slimefun 演进、玩法变更、
集成画像）；总账见 [report/perf/round-69](report/perf/round-69.md)。
第十五轮循环（用户再触发，2026-08-18 起）第 70 轮方块写入标志域
（CraftBlock 字节码实证单参 setBlockData 默认 physics=true）：面板
光源呼吸动画免 physics——1954.43 → ~285ns（**~6.9x**，每 tick 每
工作面板，全库唯一每 tick 方块写入热站点）+ HarmonysSonata 花朵
写入族一致性；作物催熟/晶簇生长/AIR 移除判定保留 physics（**语义
红线口径沉淀**：内部装饰效果非契约，玩法可构建依赖语义保留），
60 步序列等价 true + 观察者实证归档；详见
[report/perf/round-70](report/perf/round-70.md)。第 71 轮判定——
重复状态应用/世界时间查询两角度零发现（光环重应用节拍即玩法语义，
收敛 1/2），见 [report/perf/round-71](report/perf/round-71.md)。第 72 轮判定
——集合快照分配（全库零命中）/instanceof 分派/消息格式化三角度零发现，
r71+r72 连续判定收敛，**第十五轮循环收口 0.16.0**；总账见
[report/perf/round-72](report/perf/round-72.md)。第十六轮循环（用户再
触发，2026-08-18 起）第 73 轮每 tick 派发任务折叠域：FloatingHeadAnimation
（每工作面板一个 period=1 长期任务，r52 施法级判定漏计的形态）折叠进
process() 同节拍驱动——~2.5x 噪声级 + **全库唯一长期每 tick 任务类
消除**（含卸载区块空转）；任务/直接驱动头姿 10 步逐位等价 true；详见
[report/perf/round-73](report/perf/round-73.md)。第 74/75 轮判定——
运行时文本模式/数值解析/清理链守卫一致性/注册表遍历四角度零发现，
r74+r75 连续判定收敛，**第十六轮循环收口 0.17.0**；总账见
[report/perf/round-75](report/perf/round-75.md)。

## 维护要点（改代码前必读）

1. **构建**：`mvn package`（需 Java 21）。Slimefun 依赖来自本地仓库的
   `com.github.slimefun:Slimefun:5.0.0`（由 `REF/Slimefun4.1/target/SlimeFun4.1-5.0.0.jar` 安装：
   `mvn install:install-file -Dfile=... -DgroupId=com.github.slimefun -DartifactId=Slimefun -Dversion=5.0.0 -Dpackaging=jar`）。
2. **SlimefunItemStack ≠ ItemStack**：Slimefun 5 中两者已分离，需要 `ItemStack` 时用 `.item()` 转换；
   `asQuantity(int)` 返回 `ItemStack`；物品注册、`RecipeType` 等上下文仍要求 `SlimefunItemStack`。
3. **机械基类**：本地 `slimefun/machines/MenuBlock`、`TickingMenuBlock`（等价移植自 InfinityLib），
   新增机械照抄 `ChroniclerPanel`/`RealisationAltar` 骨架。
4. **命令**：本地 `commands/SubCommand` + `HistoriaCommand`；新增子命令在主类 `setupCommands()` 挂载。
5. **PDC 数据类型**：`utils/datatypes/DataType`（BOOLEAN/DOUBLE_ARRAY/INTEGER_ARRAY/LOCATION），
   编码与原 MorePersistentDataTypes 一致，勿改编码格式（涉及历史数据兼容）。
6. **依赖红线**：`depend` 仅 Slimefun；运行时核心只依赖 paper-api（1.21.11）与 Slimefun 5.0.0。
   `softdepend` 仅允许 Slimefun 附属插件（ExoticGarden/Networks/Netheopoiesis/SlimeTinker/HeadLimiter），
   且所有附属集成必须有运行时守卫（参见 `SupportedPluginManager`），保证未安装时行为不变。
7. **验证环境**：`F:/paper-test-1.21.11` 存有 Paper 1.21.11 build 132 测试服务端
   （plugins 内已放 Slimefun 5.0.0 与本插件），可直接启动回归。
8. **不可信输入红线**（28 轮审计沉淀，改代码前必读，详见 [audit/](audit/README.md)）：
   - 物品/实体/区块 PDC 与 BlockStorage 一律视为不可信（改造客户端可注入任意 NBT）——
     解析必须失败关闭（拒绝/跳过/保守默认），禁止裸 `valueOf`/`fromString`/拆箱；
   - 事件回调（弹射物/闪电/落块命中、tick、召唤物周期）可晚于施法者下线——
     禁止链式 `getCasterAsPlayer().xxx`，用 UUID 重载或降级路径；
   - 周期回调必须带断路器（异常即停用/终止该次效果 + 限流日志），防日志风暴；
   - 直接监听 PlayerInteract 系事件须 `ignoreCancelled = true`
     （checkCooldown 例外——LOWEST 前置否决）；
   - 施法/消耗类操作先结算后执行（扣费在前，效果在后）；
   - Location 键缓存（cacheMap 等）必须在 onBreak 清理；共享状态禁止放
     SlimefunItem 实例字段（单例多方块污染）。

# 审计索引（/loop 持续审计）

目标：逻辑闭合、代码安全、可长期高负载多用户运行；不信任任何客户端输入。
每轮覆盖不同方面，轮次记录见 `round-<N>.md`。

## 轮次计划与状态

| 轮次 | 方面 | 状态 |
|------|------|------|
| 1 | 常驻定时器 + SpellMemory 生命周期 + 施法者离线路径（[round-1](round-1.md)） | ✅ 完成（8 项修复，4 commit：7133f1a/8d41424/c0d3b75/9d06e03） |
| 2 | 机械缓存数据操作（液化池/现实祭坛/记录者面板/镀金器/法杖配置器）（[round-2](round-2.md)） | ✅ 完成（10 项修复，4 commit：7d7d148/d081833/4b99b32/9d9849e） |
| 3 | GUI 交互安全 + 图鉴表述一致性（[round-3](round-3.md)） | ✅ 完成（全部 GUI 面核验安全；9 项修复，2 commit：b238d99/f0be106） |
| 4 | PDC 反序列化与不可信数据（[round-4](round-4.md)） | ✅ 完成（LOCATION 反序列化白名单等 3 commit：9db17c0/52e6e3c/1746e4c） |
| 5 | 法术系统安全（[round-5](round-5.md)） | ✅ 完成（弹射物消费者驱动缺失等 3 commit：2693226/3be2e43/39506dc；round-1 遗留项闭合） |
| 6 | 监听器闭合（[round-6](round-6.md)） | ✅ 完成（副手冷却绕过等 3 commit：a5d1041/c787589/80c6c1d；round-1 遗留项闭合） |
| 7 | 无界集合与内存增长、异常吞没、日志风暴（[round-7](round-7.md)） | ✅ 完成（2 commit：f26ae63/22dafe8） |
| 8 | 功能与表述一致性 + 收口（[round-8](round-8.md)） | ✅ 完成（1 commit：9082276；README 声明全部比对相符；版本升至 1.21.11-2） |
| 9 | 实际服务器回归验证（[round-9](round-9.md)） | ✅ 通过（274 物品/995 故事与基线一致，全会话 0 异常） |

## 第二批计划（循环续）

| 轮次 | 方面 | 状态 |
|------|------|------|
| 10 | gadgets 深审（[round-10](round-10.md)） | ✅ 完成（2 commit：b41eaf6/96e8671；诅咒之土状态污染、碎裂虚空物品不落盘等） |
| 11 | artistic/exhalted/materials 物品深审（[round-11](round-11.md)） | ✅ 完成（2 commit：f9ed746/03cb3dc） |
| 12 | mobgoals 召唤物 AI 目标类（[round-12](round-12.md)） | ✅ 完成（1 commit：1105890） |
| 13 | 剩余 tier1 法术逐文件精读（[round-13](round-13.md)） | ✅ 完成（1 commit：6aaf782；67 类全覆盖） |
| 14 | 交叉复审：故事系统核心 + tools 残余（[round-14](round-14.md)） | ✅ 完成（1 commit：489c23d） |
| 15 | 收尾覆盖：PlayerStatistics/ConfigManager/剩余 utils（[round-15](round-15.md)） | ✅ 完成（1 commit：339abbe；**全部 265 文件类别覆盖完毕**） |
| 16 | 核心引擎终审：Spell 分发/加载器（[round-16](round-16.md)） | ✅ 完成（1 commit：d65dbec） |
| 17 | 施法上下文与构建器终审（[round-17](round-17.md)） | ✅ 完成（1 commit：311ee3f，删 SpellInstance 死代码） |
| 18 | 工具类实体覆盖收尾（[round-18](round-18.md)） | ✅ 完成（1 commit：228b525） |
| 19 | 命令/集成/常量类终审（[round-19](round-19.md)） | ✅ 完成（纯核验轮，零新缺陷——**审计收敛**） |
| 20 | 自改回归复审 + 4 分钟 soak test（[round-20](round-20.md)） | ✅ 完成（41 个 catch 块审计无静默吞错；0 异常 0 tick 落后——**发布质量确认**） |
| 21 | 数据文件完整性校验（[round-21](round-21.md)） | ✅ 完成（995 键 tier 全合法、tags/colors 结构通过） |
| 22 | generic-stories 数据校验（[round-22](round-22.md)） | ✅ 完成（45 故事满覆盖矩阵；**代码/数据/运行时三面收官**） |
| 23 | 空闲基线性能观察（[round-23](round-23.md)） | ✅ 完成（第 6 次运行 0 异常，tick 健康；轻量模式附加项） |
| 24 | 组合/复杂操作序列安全（[round-24](round-24.md)） | ✅ 完成（10 类组合链推演无新缺陷；镀金印记 9 连击记录为上游数值设计） |
| 25 | 消费时序与事件否决回滚（[round-25](round-25.md)） | ✅ 完成（1 commit：9cc3759；5 处交互监听补 ignoreCancelled，被否决交互不再施法/白扣） |
| 26 | 经济数值溢出边界 + plugin.yml（[round-26](round-26.md)） | ✅ 完成（1 commit：4270d22；充能溢出钳制 + 命令帮助文本修正） |
| 27 | 构建配置与产物结构（[round-27](round-27.md)） | ✅ 完成（纯核验轮：第三方类零打入、filtering/结构正确） |
| 28 | 事件 API 评估 + 文档/git 一致性（[round-28](round-28.md)） | ✅ 完成（1 commit：0a95716；README 自动更新声明修正；上游 co-author 历史查明为继承） |
| 29 | 补回归缺口 + 版本序列重定（[round-29](round-29.md)） | ✅ 完成（**版本自 0.1.0 起算**（用户指令）；第 8/9 次回归通过） |
| 30 | 审计经验沉淀 + README 来源一致性（[round-30](round-30.md)） | ✅ 完成（1 commit：209782c；下载指引修正 + 维护要点第 8 条红线） |
| 31 | CI 工作流可用性（[round-31](round-31.md)） | ✅ 完成（1 commit：74a6a0f；**CI 三重缺陷修复**——依赖入库 vendored + JDK 固定 + gitignore 目录级排除陷阱） |
| 32 | CI 产物上传补全（[round-32](round-32.md)） | ✅ 完成（1 commit：7050b0c；README 下载双渠道闭环） |
| 34 | 物品 GUI 载体交互时序 + 配方快照完整性（[round-34](round-34.md)） | ✅ 完成（2 commit：d73a682/317754d；工作台后置配方缺失 42 处 + 收纳袋 GUI 陈旧实例覆盖丢水晶；双机器人实机验证；**含测试进程误杀事故记录与新规程**——测试服独立端口 25599、PID 记录制、仅结束自启进程） |
| 35 | 遗留缺陷清偿：前缀叠加/空池假闪电/注释分母（[round-35](round-35.md)） | ✅ 完成（3 commit：38bcc64/90d046f/59e61b0；.sfi 服务器端神谕实证单层前缀 + 完整提交闭环；沉淀 mineflayer 显示盲区/背包点击封锁两条测试装置经验） |
| 36 | 现实祭坛提取链审阅 + T5 面板管线 E2E（[round-36](round-36.md)） | ✅ 完成（代码审阅无新缺陷：跨区块晶簇/破碎掉落/kill 语义复核；T5 面板吸收→记录→满槽推出三轮实证；**更正 round-35"背包点击封锁"错误推断**——BlockMenuPreset.clone 实际放行；祭坛 E2E 因 harness 槽位映射怪癖留待专用驱动） |
| 37 | 祭坛提取链服务器端 E2E（专用驱动插件）（[round-37](round-37.md)） | ✅ 完成（**发现并修复迁移级缺陷 7750049：Paper 1.21.x FLASH 等 Color 数据粒子无数据重载必抛 IAE——祭坛每次成功提取与镀金器吸取路径全中**，中央修复补白色 Color；驱动 E2E 全绿：提取消耗/晶簇成熟/破碎取消/状态清理/碎片掉落） |
| 38 | 镀金器吸取/镀金 + 液化池合成链 E2E（[round-38](round-38.md)） | ✅ 完成（7750049 第二受影响面实证修复：吸取 FLASH 路径零异常+镀金 PASS；液化池吸收→跨重启持久化→催化合成充能板+清池 PASS；驱动新增 place 子命令以真实 BlockPlaceEvent 注册机械，根治机器人放置不可靠） |
| 39 | 命令层对抗性审计（op+非 op 双机器人）（[round-39](round-39.md)） | ✅ 完成（12 用例对抗矩阵全过：守卫/权限门控/帮助过滤/优雅拒绝零异常；**诚实更正 eca4568 为误报**——枚举名≡id 全 69 项实证，原 valueOf 安全，改动保留定位为防御性解耦；方法论教训入档） |
| 40 | 修复后全量工作负载 soak 回归（[round-40](round-40.md)） | ✅ 完成（7 轮周期驱动祭坛/镀金器/液化池 + 诊断会话：0 ERROR/0 异常/0 tick 落后/3 次优雅启停；开放观察如实归档：污染测试世界中驱动掉落物出生即死现象——生产路径以 r38 干净环境 PASS 为准；建议后续机械测试用全新世界） |
| 41 | 全新世界复验 basin 开放观察（[round-41](round-41.md)） | ✅ 完成（**开放观察闭合（r42 更正归因：真变量为玩家邻近度而非污染）**：同驱动代码在 world_r41 全链 PASS——valid=true/盒内可见/吸收/催化/充能板产出，与污染世界逐项对照；无插件缺陷；规程补充：机械驱动测试用全新世界且用毕还原，本轮已还原并删除临时世界） |
| 42 | 多机器人持续高负载压测（[round-42](round-42.md)） | ✅ 完成（3 bot×410s：268 命令/400 移动/6 驱动周期，0 掉线、日志全零指标、优雅停服；**驱动无界循环致主线程冻结至 OOM 的事故修复**（工程教训：驱动循环必须有界）；**更正 r41 归因**——r40 出生即死真变量为无玩家区块的实体注册行为，玩家邻近即 PASS，与插件无关） |
| 43 | gadget tick 路径驱动验证（[round-43](round-43.md)） | ✅ 完成（13 类全部 tick 型 gadget 经真实 BlockPlaceEvent 放置 + 45s 真实 ticker 玩家邻近运行：0 异常/0 tick 落后——round-10 修复类零回归；驱动新增 gadgets 批量子命令入库） |
| 44 | 法术施放链驱动验证（[round-44](round-44.md)） | ✅ 完成（10 法术全原型生产路径施放 0 异常：即时/弹射物/tick/混合/闪电/召唤/飞行；SpellMemory 十表生命周期实证——弹射物 5s 清零/滴答耗尽自注销/召唤物逐步过期，TemporaryEffectsRunnable 回收正常；驱动新增 spells cast|stat 子命令） |
| 45 | 旧存档兼容性复验（[round-45](round-45.md)） | ✅ 完成（当前构建上 v1 编码双读三组全 PASS：故事列表 3/3 逐项一致/法杖槽·法术·晶能一致/区块晶簇数量·id·位置一致——r26-29 迁移的兼容承诺经 11 轮修复后复验成立；驱动新增 legacy 子命令） |
| 46 | 图鉴 GUI 打开/统计路径验证（[round-46](round-46.md)） | ✅ 完成（三 FlexGroup 生产路径打开全成功零异常 + rank 统计正常；harness 限制如实归档：指南类 GUI 按钮名对 mineflayer 不可见，翻页点击不可自动驱动——翻页数据层已由 perf r21-23/31 覆盖，交互层建议真人复核；驱动新增 compendium 子命令） |
| 47 | 统计写入链驱动验证（[round-47](round-47.md)） | ✅ 完成（六写点零异常→纪元缓存读回全真→force 落盘→player_stats.yml 磁盘核验——写入/失效/读回/落盘全链实证；player_stats.yml 快照还原；驱动新增 stats 子命令） |
| 48 | 法杖配置器驱动验证 + 组装链复核（[round-48](round-48.md)） | ◐ 完成（放置注册/槽位填充 ✅；ADD/REMOVE 处理器逐行复核无新缺陷（损坏板退还双路径在位）；GUI 点击未成——mineflayer/BlockMenu 非确定性交互（r36 家族），建议真人复核；驱动新增 configurator fill|assert 子命令） |
| 49 | Waystone/传送网驱动验证（[round-49](round-49.md)） | ◐ 完成（**绑定生产路径 PASS**（真实 PlayerInteractEvent→PDC 写入，两度实证）；传送链零异常+代码复核无缺陷——teleportAsync 等待客户端 ACK 而 mineflayer 不发送（vanilla /tp 对照证明非处理器条件问题），如实归档为自动化边界；驱动新增 waystone/tpasync 子命令） |
| 50 | 液化池充能板三分支驱动验证（[round-50](round-50.md)） | ✅ 完成（再充能晶能算术精确吻合 10+3=13 且池清空；异法术液体全销毁+板存活为设计语义（断言误设已更正）；损坏 PDC 吞没+配置不正确告警两度实证（r2 失败关闭在位）；驱动新增 basinplate 子命令） |
| 51 | 配置文件损坏容错实测（[round-51](round-51.md)） | ✅ 完成（部分损坏：六段校验链实机实证——四类坏条目逐条跳过+留痕，插件完整降级启用；**致命损坏：jar 默认值合并自愈实证**——非法 YAML 后 2 行→715 行重建、995 故事加载完整启用，磁盘损坏不瘫痪附属（重要正向发现）） |
| 52 | Exalted 物品效果链驱动验证（[round-52](round-52.md)） | ✅ 完成（时间/天气双链：onExalt 零异常→冻结表登记 HIT→玩家状态实际应用（playerTime=6000/weather=CLEAR）→2s 过期每秒回收 stat=0——统一过期模型在 Exalted 族实机实证；驱动新增 exalted 子命令） |
| 53 | Artistic 画笔消耗链驱动验证（[round-53](round-53.md)） | ✅ 完成（黑色 100 款：涂色 100/100（wool 族语义）+ PDC uses 衰减 + 第 100 次耗尽堆清空（LimitedUseItem 用尽即毁实证）——零插件异常；驱动新增 brush 子命令） |
| 54 | 长时连续 soak（20 分钟混合负载）（[round-54](round-54.md)） | ✅ 完成（1199s×7 周期：70 法术施放零错误、祭坛 mapSize 2→11 持续生长、SpellMemory 稳态有界（63-75 波动）且终态全表排空零泄漏、0 掉线、日志全零指标、优雅停服——r40 soak 时长翻倍扩展） |
| 55 | 水晶燃烧降级+下界门脱水驱动验证（[round-55](round-55.md)） | ✅ 完成（真实 Bukkit 事件双驱动：RARE→UNCOMMON 降级+事件取消+弹开（r6 语义在位）、脱水配方原位转换精确匹配——零异常；驱动新增 crysta 子命令） |
| 56 | 真人 GUI 复核清单 + 修复相互作用矩阵（[round-56](round-56.md)） | ✅ 完成（harness 边界项归集为 [manual/gui-review-checklist.md](../manual/gui-review-checklist.md) 四项真人复核清单；7 项修复爆炸半径交叉审查——零冲突，同链修复已被 r37 E2E 同链验证） |
| 57 | 奇术盐+折射透镜真实事件驱动（[round-57](round-57.md)） | ◐ 完成（透镜 PASS：每内容类型一 DisplayItem 语义澄清 + 冷却护栏为真实客户端路径（合成事件连发绕过，harness 观察归档）；奇术盐未定：事件全条件到达但 emptyBasin 未执行，嫌疑为合成事件权限解析——代码与 r6 形态一致，列入真人复核；驱动新增 salts 子命令） |
| 58 | 奇术盐未定项深探（[round-58](round-58.md)） | ◐ 完成（五门条件逐一探针全真+监听器注册+零异常，与效果缺失构成不可解释矛盾；同构透镜监听器完全成功对照——维持非插件缺陷判定，移交真人复核 4a 一秒定案；驱动 place 幽灵方块注意明示） |
| 59 | 启动数据完整性回归（[round-59](round-59.md)） | ✅ 完成（25 轮修改后终验：995 独特故事与 r9 基线一致、启动 572ms 同量级、干净启用零异常——无数据格式/加载路径回归） |
| 60 | 奇术盐之谜取解（[round-60](round-60.md)） | ✅ 完成（**r57/58 未定项机制取解**：合成事件预取消使 NORMAL 优先级 ignoreCancelled 监听器整体跳过（盐），LOW 级（透镜）先于预取消运行故成功——跨层混淆经 tier_scan 排除；非插件缺陷证据强化；harness 教训入档：合成事件驱动须核对优先级预取消链） |
| 61 | 奇术盐方法级直调验证（[round-61](round-61.md)） | ✅ 完成（**r57-60 链条完整闭环**：直调生产监听器方法 → fill 3→0 + 盐消耗——生产逻辑端到端正确，分发层差异为唯一变量；最终定性非插件缺陷（方法级实证）；真人 4a 降级为形式确认） |
| 62 | 配置器 ADD 处理器方法级直调（[round-62](round-62.md)） | ✅ 完成（**r48 ◐ 正式闭环**：填充后直调 BlockMenu 注册的 ADD_PLATES 生产处理器 → assert PASS（plate=PUSH/50 + 板槽清空——组装完整语义方法级实证）；**harness 教训**：Paper .paper-remapped 同名 jar 缓存不随内容失效，更新驱动须清缓存（规程入档）） |
| 63 | HEAD 产物一致性验证（[round-63](round-63.md)） | ✅ 完成（mvn clean package 干净构建 vs 29 轮实测产物：六个修复承载类逐一类体一致 + 修复符号在位抽查 + 清缓存干净部署冒烟（995 故事/零异常）——仓库 HEAD 产物与实测同源，发布物可信度闭合） |
| 64 | 全驱动回归套件（[round-64](round-64.md)） | ✅ 完成（单会话 24 项顺序执行全过：19 自动判绿 + 5 已知语义判读——跨子系统密集交叉运行零异常；结果存 note/report/audit-suite64-results.json，后续改动可一键回归） |
| 65 | 根 README/plugin.yml 表述一致性终审（[round-65](round-65.md)） | ✅ 完成（七处玩家可见表述与实现一致（含 80+ 组合空间核算成立维持上游原文）；7 项修复零表述漂移——纯文档轮） |
| 66 | 循环收敛判定（[round-66](round-66.md)） | ✅ **审计循环收敛宣告**（42 提交/33 覆盖面/零悬案/连续三轮零发现——按项目准则休眠，触发条件与一键回归资产入档） |
| 67 | 0.18.1 发布收口（[round-67](round-67.md)） | ✅ 完成（按 AGENTS.md 常设指令执行：pom 0.18.1 + release/0.18.1.md + 索引 + 产物构建 + world_r67 冒烟（v0.18.1 启用/995 故事/零异常/优雅停服）；真人清单保留可选参考） |
| 68 | SleepingBag+临时合成台驱动验证（[round-68](round-68.md)） | ✅ 完成（睡袋 ItemUseHandler 方法级直调零异常——假玩家 sleep 失败→回滚路径实证（无床残留/未登记，r1 修复在位）；openWorkbench 打开 PASS） |
| 69 | Runes/Uniques 交互链审查（[round-69](round-69.md)） | ✅ 完成（Runes 8 项纯材料无运行时面；Trophy 双防御 handler（禁放/禁食）+ TrophyDisplay 取放兜底复核在位，展示架 tick 已由 r43 覆盖——零新发现，纯代码轮） |
| 70 | 跨维度边界判定轮（[round-70](round-70.md)） | ✅ **循环收敛休眠**（全新探针角度：全库 6 处 distance callsite 逐一审查——跨世界守卫全在位（r7/r12 修复），构造同世界 3 处安全——零发现，连续 2/2 角度互异，按 r44 准则休眠待触发） |
| 71 | 事件优先级全景矩阵（[round-71](round-71.md)） | ✅ 完成（**发现并修复 293ef90**：onPoseClone 缺 ignoreCancelled（红线 4 违例，r25 漏网处）——被否决交互仍克隆消耗，一行修复；脚本化全景矩阵证明判定轮换角度的价值：r10-19 逐文件与 r25 专项均未覆盖此参数组合） |
| 72 | 293ef90 修复实机验证（[round-72](round-72.md)） | ✅ 完成（消息哨兵双对照：未取消事件处理器执行（哨兵 #1 到达，功能不变）+ 预取消事件处理器跳过（无第二条）——修复精确实证；发现后计数重置为 0/2，继续角度互异判定） |
| 73 | 调度器全景矩阵（[round-73](round-73.md)） | ✅ 零发现 1/2（全部 6 处 runTask* 调用点：3 周期任务常量合法/附属延迟 1/Spell period 全 69 法术枚举 ≥1 非法态不可构造/TunnelBore 死代码；取消路径 r1/r44 已实证——角度互异判定轮） |
| 74 | setCancelled 时机矩阵（[round-74](round-74.md)） | ✅ **零发现 2/2——循环收敛休眠（第三次）**（33 处取消点扫描 + 7 标记点人工复核：3 设计语义（均有既往实证）+4 窗口误报；脚本矩阵误报率 4/7 印证需人工复核） |

## 已修复问题汇总

| 轮次 | 文件 | 问题 | 修复 | commit |
|------|------|------|------|--------|
| 1 | `runnables/ParticleDisplayRunnable.java` | 循环误用 return 提前终止 | 改 continue | `7133f1a` |
| 1 | 12 文件（listener/core/tier1×10） | 13 处 UUID `==`/`!=` 引用比较，自我豁免失效 | 改 `.equals()` | `8d41424` |
| 1 | `magic/spells/tier1/ChillWind.java` | negative 效果调用 applyPositiveEffects，效果永不生效 | 改 applyNegativeEffects | `8d41424` |
| 1 | `SpellMemory.java` | strikeMap 无过期清理；离线条目不删；removeBlocks 卸载世界抛异常中断全部清理；clearAll 遗漏 strikeMap | removeStrikes + 离线移除 + 异常守卫 + clearAll 补齐 | `c0d3b75` |
| 1 | `runnables/spells/SpellTickRunnable.java` | 施法者离线 tick 法术 NPE；异常每 tick 重刷 | 离线终止 + 断路器 | `c0d3b75` |
| 1 | `slimefun/items/tools/SleepingBag.java`、`listeners/MiscListener.java` | 睡袋刷床复制（下线残留/他人采集/爆炸掉落/sleep 失败不回滚） | 退出兜底清理 + 挖掘/爆炸守卫 + 回滚 | `9d06e03` |
| 2 | `mechanisms/DisplayStandHolder.java` | kill 时序漏洞致展示架实体永久泄漏；损坏 UUID/消失实体 NPE | 先取架再清信息；findDisplayStand 防御 + 缺失重建 | `7d7d148` |
| 2 | `liquefactionbasin/*` | 持久化数据损坏致机械永久失效；**玩家条件绕过尊贵物品进度门槛** | 防御解析 + 失败关闭 | `d081833` |
| 2 | `chroniclerpanel/*`、`realisationaltar/*`、`prismaticgilder/*` | 损坏数据机械失效、T5 面板 pushOut NPE、onBreak NPE 吞输入物品、祭坛状态不一致 NPE 链 | 全面空值/异常守卫 | `4b99b32` |
| 2 | `staveconfigurator/*`、`InstancePlate.java`、`SpellCastListener.java` | 作弊充能板 EnumMap NPE；施法先执行后扣费可零成本重试；副手事件覆盖成功提示 | 退还无效板 + 先结算后施法断路器 + 忽略副手 | `9d9849e` |
| 3 | `itemgroups/SpellCollectionFlexGroup.java` | 法术集图鉴 7 处表述与实现不符（缩放标志/治疗量/范围判断/弹射物击退全部误读字段） | 逐一改为正确字段 | `b238d99` |
| 3 | `tools/satchel/SatchelInstance.java` | PDC 反序列化数组无校验（长度/负值）→ 负库存污染；removeAmount 无下界 | 长度 9 校验 + 负值钳 0 | `f0be106` |
| 4 | `utils/datatypes/LocationDataType.java` | **物品 PDC 无过滤 Java 反序列化（RCE 攻击面）** | resolveClass 类白名单（编码不变） | `9db17c0` |
| 4 | `utils/datatypes/DoubleArrayDataType.java` | 长度字段无校验 → 负长度崩溃/超大长度 OOM | 长度与字节量一致性校验 | `9db17c0` |
| 4 | `utils/datatypes/Persistent{Plate,Stave,Stories,StoryChunk,SatchelInstance,Pose}*` | 缺键拆箱 NPE、非法值 IAE、null 故事入列表连锁 NPE | 失败关闭/坏条目跳过/保守默认 | `52e6e3c` |
| 4 | `DataTypeMethods.java` + 6 个读取方 | 类型错配 IAE 穿透全部调用方；回忆水晶格 world==null NPE | 断路器 + 逐点降级处理 | `1746e4c` |
| 5 | `SpellMemory.java`、`MagicProjectile.java` | **弹射物 tick 消费者从未被驱动**（StarFall/Chaos/Hellscape 拖尾效果缺失，上游遗留） | removeProjectiles 同构驱动 + 消失清理 + 断路器 | `2693226` |
| 5 | `tier1/{Hellscape,PlutosDecent,CallLightning,AntiPrism}.java` | 命中回调离线施法者 NPE 穿透事件链（round-1 遗留） | 位置降级/无源爆炸/UUID 权限 | `3be2e43` |
| 5 | `tier1/{Break,PhilosophersStone}.java`、`utils/GeneralUtils.java` | 视线无方块 NPE；颜色表缺项 NPE；零向量归一化 NaN | 空值守卫 + Number 安全读取 + 零长跳过 | `39506dc` |
| 6 | `listeners/{Misc,RefractingLens,ThaumaturgicSalts}Listener.java` | **副手固定读主手构成物品冷却绕过**；调光勺双跳变；透镜双展示；盐重复清池；三机械缓存缺失 NPE | event.getItem() + 忽略副手 + 判空 | `a5d1041` |
| 6 | `listeners/{CrystalBreak,Satchel}Listener.java` | 故事删除后碎晶 NPE 中断破坏链（不掉落不落盘）；收纳袋失败提示与实现不符 | 跳碎片仍清状态落盘 + 双原因提示 | `c787589` |
| 6 | `gadgets/ExpCollector.java` | volumeMap 三处拆箱缺键 NPE（机械每 tick 死亡）；UUID/parseInt 无防御；**onBreak 不清条目无界增长** | getOrDefault + try/catch + 破坏清理 | `80c6c1d` |
| 7 | `mechanisms/TickingBlockNoGui.java`、`types/Stand.java` | **firstTickMap 与 Stand 双映射从不移除（无界增长）**；损坏 UUID 每 tick 异常；跨世界 distance IAE | 破坏清理 + try/catch + 同世界判定 | `f26ae63` |
| 7 | `InstancePlate.java`、`liquefactionbasin/LiquefactionBasinCache.java` | 施法异常日志可被高频施放刷爆；canCraftSatchel 死代码（NPE 隐患） | 每法术仅首次记录 + 删除死代码 | `22dafe8` |
| 8 | `tier1/StripMine.java`、`commands/{TestSpell,TestWand}.java` | 视线空值 tick 回调 NPE（round-5 观察 2 更正：raycast 有消费方）；test-spell 负强度负伤害；命令参数裸抛异常 | 空值守卫 + 1-5/1-2 边界 + 友好提示 | `9082276` |
| 10 | `gadgets/CursedEarth.java`、`gadgets/FragmentedVoid.java` | **多方块共享刷怪计数器（频率随方块数失控）**；**吸收物品绕过脏标记不落盘（重启丢失）** | per-location 映射 + markDirty | `b41eaf6` |
| 10 | `gadgets/{MobFan,MobLamp,MobMat,MobTrap,GreenHouseGlass,MysteriousTicker,TrophyDisplay}.java` | BlockPlacer 缺键每 tick NPE×4 类；映射泄漏×4 类；TrophyDisplay 跨实例死状态 | 判空失败关闭 + 破坏清理 + 删死代码 | `96e8671` |
| 11 | `artistic/ImbuedStand.java`、`exhalted/{ExaltedHarvester,ExaltedSeaBreeze}.java` | 他人领地生成盔甲架；**随机点累积漂移（作用范围失控）** | 领地校验 + clone 基准 | `f9ed746` |
| 11 | `materials/PowderedEssence.java` | 他人领地骨粉催熟 | INTERACT_BLOCK 校验 | `03cb3dc` |
| 12 | `mobgoals/{AbstractGoal,HolyCowGoal,AbstractRidableGoal}.java` | 主人过传送门后 AI 每 tick 跨世界 distance IAE；他人骑乘时主人离线 getEyeLocation NPE | 同世界判定 + 离线退回常规 tick | `1105890` |
| 13 | `SpellMemory.java`、`MagicSummon.java`、`tier1/{SummonGolem,LeechBomb,GrowUp}.java` | **召唤物 tick 消费者离线 NPE 每秒中断整个法术清理链（泄漏级联）**；setSize 无上限越界异常 | removeEntities 离线清理 + onTick 兜底 + run 断路器 + 尺寸钳制 | `6aaf782` |

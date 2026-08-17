# 性能优化第 53 轮：事件级 getByItem 材质门控（第十轮循环开启，7 监听器收官）

日期：2026-08-17
域：**高频事件处理器的门控优先序**——r39/40 弱缓存覆盖 tick 路径、
r41 扫过 tick 级 getItemMeta 后，事件级 `SlimefunItem.getByItem` 调用
点从未被清扫。本轮探针角度：昂贵读取是否排在廉价材质判定之后。

## 实现（本轮提交 479fdef，8 处门控）

| 监听器 | 事件（频率） | 门控材质 | 目标类 |
|--------|-------------|---------|--------|
| SpellCastListener | PlayerInteractEvent（每次主手点击） | STICK | Stave ×3 阶 |
| SatchelListener | EntityPickupItemEvent（每次玩家拾取） | PLAYER_HEAD + 36 槽 EnumSet（六色混凝土） | Crystal / CrystamageSatchel |
| MiscListener.onShootPaintbrush | EntityShootBowEvent（每次弓弩射击） | TIPPED_ARROW | MagicPaintbrush 族 |
| ThaumaturgicSaltsListener | PlayerInteractEvent | REDSTONE | ThaumaturgicSalt |
| RefractingLensListener | PlayerInteractEvent | SPYGLASS | RefactingLens |
| CrystaDowngradeListener | EntityCombustByBlockEvent | PLAYER_HEAD | Crystal |
| PoseChangerListener ×3 | Interact / InteractAtEntity ×2 | BAMBOO / SEA_PICKLE | PoseChanger / PoseCloner |

门控均为纯前置短路（材质不等即返回），语义零变化；材质集从注册表
逐一核实（三阶法杖/全部水晶/六阶收纳袋均单材质族）。DisplayItemListener
复核排除——两事件对象为 `Item` 实体，实体 PDC 直读 ~15ns（r14）无
meta 克隆，已属最优。

## 量化（服务器内真实 Slimefun 注册表，round-53-server.tsv）

| 基准 | 旧（getByItem） | 新（材质判定） | 提升 |
|------|----|----|------|
| eventGate.getByItem（未注册材质 miss） | 18.56 ns | 6.37 ns | **2.91x** |
| eventGate.getByItemRegistered（原版 STICK） | 18.08 ns | 6.03 ns | **3.00x** |
| eventGate.getByItemRegistered（原版 PLAYER_HEAD） | 20.62 ns | 7.00 ns | **2.95x** |
| eventGate.satchelScan（36 槽扫描） | 547.50 ns | 194.74 ns | **2.81x** |

等价性断言全 true：各族真实注册物品（3 法杖 + 3 水晶取样 + 收纳袋
1/6 阶 + 盐/透镜）材质与注册一致且 getByItem 判定不变；原版样本
（含全部门材质本体）两路径一致不命中。会话 COMPLETE=1；watchdog 3
次与 CH_ERRORS=1 均为已知基准批次/计数器伪象（r49 同源，已核验归档）。

## 方法论发现：getByItem 双路径成本

Slimefun 5.0.0 的 `getByItem` **miss 路径为 ~18-21ns 廉价短路**（无
meta 克隆——未命中物品在材质/标识预检即返回 null），与 r39 实测的
**hit 路径 1.3µs**（真实 SF 物品 meta + PDC id 解析）是两条路径。
据此修正事件级门控的收益预期：每次事件省 **12-14ns**、每次水晶拾取
省 **~353ns**——实测为正但绝对量为事件级边际；门控价值兼具防御性
（未来 Slimefun 若调整 miss 路径成本，门控形态免疫）。

## 判定

- "事件级 getByItem 门控"族收官：全部 8 处调用点已门控，1 处复核
  排除（实体 PDC 途径）；
- 族矩阵增补行：**事件级 SF 身份解析门控——实质（小绝对量）**；
- 第十轮循环开启（0.10.0 后用户再触发），下一轮判定轮换新角度。

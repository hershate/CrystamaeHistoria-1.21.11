# 审计第 6 轮：监听器闭合

日期：2026-08-15
范围：`listeners/` 全部 17 个监听器逐一核查（SpellCast/SpellEffect/Misc/PoseChanger 已在 round 1/2/4/5 覆盖）+ 关联物品类（ExpCollector、MagicPaintbrush、CrystaRecipeTypes 下界祛魔配方）

## 已修复（3 个 commit）

| commit | 问题 |
|--------|------|
| `a5d1041` | **round-1 遗留项闭合（副手事件类）**：右键为主手/副手各派发一次事件——① `checkCooldown` 固定读主手 → **副手持冷却物品交互构成冷却绕过**（改用 `event.getItem()`）；② 调光勺每点击两次 adjustLight（亮度双重跳变）；③ 折射透镜双份展示物；④ 神秘盐重复清池。后三者忽略副手事件。同批含折射透镜对三台机械的**缓存缺失窗口判空**（BlockPlacer 放置/首 tick 前，液化池/镀金器 NPE、经验收集器拆箱 NPE 按 0 展示） |
| `c787589` | CrystalBreakListener：故事被配置删除后 `getStory` null → 链式 NPE **中断 BlockBreakEvent 处理链**（晶体不掉落、状态不落盘）。现跳过碎片产出但正常清状态落盘。SatchelListener：失败原因双义（未初始化/等级不足）但文案只提示前者——表述与实现不符 |
| `80c6c1d` | ExpCollector（gadget，经折射透镜关联发现）：`volumeMap.get()` 三处拆箱——条目缺失（BlockPlacer 放置/历史缺键）时**每 tick NPE 机械死亡**；onNewInstance 的 UUID/parseInt 无防御；**onBreak 不清内存条目 → 放置/破坏循环无界增长**（长期运行内存泄漏） |

## 核验安全（记录依据）

- MobCandleListener（禁刷区盒包含判定，集合有界）、EndermanInhibitorListener（抑制表有界）、DisplayItemListener（漏斗吸取取消 + 永续防消失；canPlayerPickup/pickupDelay 已锁玩家拾取）、MaintenanceListener（液化池坩埚水位锁）、ArmorStandInteract（展示架交互/发射器装备双拦截；展示架被杀后 getDisplayStand 重建路径 round-2 已闭环）、PhilosophersSprayListener（失败发射事件直通静态触发）、CrystaDowngradeListener（稀有度枚举域内换算 + mirror 写回）、NetherDrainingListener（配方图：9 神话水晶→空白水晶单跳，无链式转换）。
- MagicPaintbrush.tryPaint 已有 block/entity 判空与领地校验。
- BlockRemovalListener 覆盖破坏/取桶/形成/方块爆炸四路径；临时方块被爆炸摧毁由 tryGrow 兜底清条目。

## 遗留观察

1. **边缘（不改）**：临时方法方块被活塞推动时 metadata 不随方块移动——到期清理会作用在原位置（可能误清新块）。涉及法术极少（放置类临时方块），记录为上游已知限制。
2. 展示盔甲架可被玩家左击销毁——getDisplayStand 已有重建路径，仅视觉短暂缺失。

## 验证

`JAVA_HOME=F:/Java/21 mvn -q package` 构建通过。

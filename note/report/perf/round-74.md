# 性能优化第 74 轮：判定轮（运行时文本模式处理 + 数值解析形态）

日期：2026-08-18
域：**判定轮**（第十六轮循环收敛 1/2）。探针角度（与 r1-73 互异）：
A. 运行时文本与模式处理形态（Pattern/SimpleDateFormat/String.matches/
split/chars/BigDecimal）；B. 运行时数值解析形态（parseInt/parseLong/
parseDouble 非启动期站点）。

## 角度 A：运行时文本与模式处理——零发现

全库站点逐点分类：

| 站点 | 分类 |
|------|------|
| `EphemeralWorkBench:84` `s.matches("(.*)BACKPACK(.*)")` | **唯一** matches 站点；唯一调用链为类加载静态块的配方过滤（启动一次性，~N 项 × µs 级，r12 启动域口径下属噪声） |
| `RefillableUseItem:53` `PatternUtils.USES_LEFT_LORE.matcher(...)` | 预编译静态 Pattern（正则形态正确），物品使用事件级 |
| `String.split` / `SimpleDateFormat` / `.chars()` / `BigDecimal` | 全库零命中 |
| `replaceAll`/`replaceFirst` | 全库零命中 |

判定：运行期无正则编译、无逐次 Pattern 构造——文本模式域闭合
（r9 消息格式化/r21 TitleCase 后的最后一角）。唯一 matches 站点为
启动静态块冷路径，不构成变更对象（改 contains 收益 ~ms 级一次性，
低于变更门槛）。

## 角度 B：运行时数值解析——零发现

`Integer.parseInt` 等全库 5 站点：TestWand/TestSpell（命令级）、
ExpCollector/LiquefactionBasin/PrismaticGilder（BlockStorage 恢复
路径，onNewInstance 一次性）——全部冷路径，零热域发现。

## 判定

两个互异新角度均零发现——第十六轮循环收敛计数 **1/2**。

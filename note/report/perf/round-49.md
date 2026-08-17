# 性能优化第 49 轮：球面扫描 O(n²) 去重消除——r18 族漏网两成员（地板判定第四次修订）

日期：2026-08-17
性质：**判定轮转为清扫轮**——按与 r48 不同的探针角度（List.contains
O(n) 扫描嫌疑点）复核时命中实质优化点，48 的零发现判定被修订
（地板判定修订史：33→34、43→44、48→49）。

## 发现与论证

`Cascada`（弹射物命中回调）与 `PlutosDecent`（施法回调）的球面扫描：

```java
final List<Block> blocks = new ArrayList<>();
for y/x/z 三重循环（偏移 [-range, range)³）:
    球内判定 → block = getBlockAt(x+bx, y+by, z+bz)
    if (!blocks.contains(block) && hasPermission(...)) blocks.add(block);
```

三重循环的 (x,y,z) 偏移组合**构造上互异**，getBlockAt 由坐标决定，
结果必不重复——`contains` 去重恒 false，为**无效工作**（r18 对
TunnelBore 的同判；r18 当轮只扫了 runnables，两个漏网成员位于法术
命中/施法回调）。r49 探针角度（List.contains 全库扫描）将其暴露：
`List<Block>` 在 magic/ 包仅此两处。

## 实现（本轮提交 cf97edd）

- 两处 `!blocks.contains(block)` 移除（附结构互异论证注释）；
- `getWorld()` 提升出循环（r18 修复同形态）。

## 量化（服务器内真实方块球面扫描，round-49-server.tsv）

| 基准 | 旧（List.contains 去重） | 新（直接收集） | 提升 |
|------|----|----|------|
| sphereScan.r5（下界施法，n=512） | 184,341 ns | 4,701 ns | **39.2x** |
| sphereScan.r8（高等级法杖放大，n=2106） | 2,986,050 ns | 19,547 ns | **152.8x** |

等价性：两档扫描产物**逐位一致**（同序同坐标，n=512/2106 全比对
true）。**高等级法杖下每次 Cascada 命中/PlutosDecent 施法节省
~2.97ms**——r44 惯用法族以来最大单项，且位于弹射物命中热路径。

## 会话记录

- COMPLETE=1；真实插件异常 **0**（SEVERE=0）；
- watchdog 3 次：均为基准重负载批次固有（tunnelBore r5 旧变体 /
  loreApply 拒绝变体 / round15 写回旧变体批次跨越 10s 边界——历轮
  该批次 9.5s 逼近阈值，本次越界，属抖动）；CH_ERRORS=1 为计数器
  误报——watchdog 线程转储的堆栈帧含插件类名（InstanceStave.buildLore）
  被计入，已逐行核验非插件缺陷；
- 15 行 Exception 均为测试服离线访问 Mojang 皮肤 API 的 SSL 证书
  错误（环境噪声，与插件无关）。

## 判定

- **"连续判定轮零发现"收敛准则重置**（r48 零发现 → r49 实质发现）；
  下一轮起重新累计，需连续两轮零发现判定轮才宣告收敛；
- r18 族（O(n²) 无效去重）至此四成员闭合：TunnelBore（r18）+
  Cascada/PlutosDecent（本轮）——`List<Block>` + contains 形态全库
  清零；族矩阵"集合 O(n) 扫描"行标记完成；
- 方法论：**判定轮必须轮换探针角度**——r48（残余 stream/克隆/反
  模式）与 r49（List.contains）角度不同结论不同；单一角度的零发现
  不可作为地板证据。

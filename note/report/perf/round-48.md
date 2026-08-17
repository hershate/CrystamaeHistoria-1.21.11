# 性能优化第 48 轮：惯用法清扫域穷尽判定（r44-47 收口，无代码变更）

日期：2026-08-17
性质：判定轮——r44 确立的"流式惯用法"族经 44/45/46/47 四轮清扫后
的矩阵复核与地板确认。

## 族探针矩阵（r44 以来累计增补）

| 族 | 轮次 | 结果 | 量级 |
|----|------|------|------|
| 流式惯用法（findFirst/findAny/skip/toList） | 44/45 | 实质优化 | 8.97x / 113x / 7.05x |
| 枚举 values() 逃逸克隆 | 46 | 实质优化 | 7.85x |
| 集合双复制（stream().toList()） | 46 | 实质优化 | 19.9x（空）/ 4.5x（3 元素） |
| 不逃逸分配（纯迭代 values() 克隆 / getLocation） | 46/47 | **JIT-EA 边界（四证）** | 持平 |
| 每 tick 死分支 | 47 | 卫生（死逻辑移除为主） | 1.18x 噪声级 |

## 本轮未探族复核（全库 grep 实证）

- **残余 `.stream()` 调用点仅 2 处**：`StoriesManager.java:257`
  （fillBlockDefinitions 启动冷路径）、`FragmentedVoid.java:77`
  （事件级，r44 已判定豁免——`Optional<ItemStack>` 消费形态）；
- **`collect(Collectors.toList())` 4 处**：启动路径 ×2 / 图鉴 GUI
  构造（r21 已记忆化）/ `BlocksConfigGenerator`（dev 工具）——全冷路径；
- **tick 路径 getLocation/clone**：`ParticleDisplayRunnable`（4 秒
  周期 ×持勺玩家，r38 已判定噪声域；line 30 的 clone 逃逸进
  spawnParticle 为 API 必需语义）、`TunnelBoreRunnable`（未注册
  死代码）；`FloatingHeadAnimation` 已于 r47 移除；
- **map 迭代反模式（keySet+get）**：零命中——`SpellMemory:69`/
  `MobCandleListener:28` 均纯键迭代无回查（后者带空表早退），
  `RealisationAltarCache.kill` 的 keySet 迭代器用于 remove 为正确形态；
- **new Random / 非工厂 NamespacedKey / 循环内字符串拼接**：零命中。

## 地板判定

惯用法清扫域代际递减（113x → 7.85x → 1.18x → 本轮零发现），与
r42-43 的长尾递减曲线续接。**EA 四证（r19/r38/r46/r47）已确立边界
规则：不逃逸的短命分配在 C2 下近免费**——清扫目标从"一切分配"收敛
为"逃逸形态（流包装/二次传递/跨调用保留）"，而该形态的已知调用点
已全部闭合。剩余成本三类归属不变：API 边界（玩法必需）、事件级
边际、JIT-EA 边界。

**插件侧性能面地板判定维持并强化**：后续预期纯判定轮，除非外部
触发（Paper/Slimefun 演进、玩法变更、集成环境实测画像）。复查节奏
延续 r44 确立的形态：低成本惯用法清扫与判定轮交替，直至连续判定轮
零发现。

## 建议

维持 0.9.0（r45-47 为版后性能提交，累计待收口至下个版本）；循环
降频执行。

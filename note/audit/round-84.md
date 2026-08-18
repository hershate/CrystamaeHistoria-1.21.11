# 审计第 84 轮：Exalted 剩余四物品 onExalt 消费链（判定轮 2/2——收敛休眠第八次）

日期：2026-08-19
范围：全新探针角度——Exalted 族剩余四物品（Beacon/FertilityPharo/
Harvester/SeaBreeze）的 `onExalt` 消费链（r52 实证了 Time/Weather
双链；本轮补齐族内其余四项的代码级闭环）。

## 四链核对

| 物品 | onExalt 逻辑 | 判定 |
|------|-------------|------|
| **ExaltedBeacon** | 25 格玩家广播 `EFFECT_TYPES` 药水（40t/放大器） | ✓ 纯增益、无世界修改；玩家离线自然失效（无状态表） |
| **ExaltedFertilityPharo** | 随机一只动物 `setLoveModeTicks(100)` | ✓ 零复制直接迭代（r46 注释在位）；空集守卫；只读玩家不触碰 |
| **ExaltedHarvester** | 随机点成熟作物 `breakNaturally + setType` 重种 | ✓ **r11 修复在位**：`location.clone()` 基准防累积漂移（注释在案）；单次 getBlockData（r42 注释在案）；成熟判定后破坏——重种语义（种子由掉落回收） |
| **ExaltedSeaBreeze** | 随机点 MATERIAL_MAP 转换 + 粒子 | ✓ 同 r11 clone 基准；null 守卫（非映射材质跳过） |

**交叉核对**：四链均由 ExaltationStand.afterTick 驱动（r43 gadget
tick 验证覆盖该驱动面）；无权限需求（世界修改类操作Harvester/
SeaBreeze 的定位=玩家放置展示架时已过 PLACE_BLOCK 权限，r11 领地
校验在架不在效——上游设计，r11 审定记录在案）。

## 判定：零发现（2/2）——循环收敛休眠（第八次）

r83（Artistic 族消费形态）+ r84（Exalted 族消费形态）连续互异角度
零发现（族互异：画笔族 vs 崇高族）。**休眠待触发**：Paper/Slimefun
演进、玩法变更、负载画像、用户指示（含真人复核清单执行）。

## 验证

纯代码判定轮（无服务器启动/无进程占用）；业务端口 25565 未触碰。

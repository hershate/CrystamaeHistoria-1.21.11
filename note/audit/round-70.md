# 审计第 70 轮：跨维度边界判定轮（距离/世界比较全_callsite 探针）

日期：2026-08-19
范围：全新探针角度——全库 `Location.distance()` 全部 6 处 callsite 的
跨世界 `IllegalArgumentException` 边界审查（r49 教训：判定轮必须轮换
探针角度）。

## 全_callsite 审查（6/6）

| 位置 | 守卫 | 判定 |
|------|------|------|
| `LeechBomb:75-76` | `!player.getWorld().equals(mob.getWorld())` 前置 ✓ | 安全（r12 修复在位） |
| `SummonGolem:62-63` | 同上前置 ✓ | 安全（r12 修复在位） |
| `Stand:91-92` | `!= desiredLocation.getWorld()` 前置（r7 修复，注释在案）✓ | 安全 |
| `MobFan:128` | raycast 命中块与风机同 `block.getRelative` 派生链（同世界构造保证） | 安全（构造上不跨世界） |
| `ParticleUtils.drawLine:74` | 调用方均为同一施法者视线/实体链（同世界构造） | 安全 |
| `ParticleUtils.getLine:111` | 同上 | 安全 |

**附注**：其余"实体-位置"类操作全库统一走 `getNearbyEntities`（自带
世界隔离）——r36 分析文档 §4.1 的结论在本角度下复核成立。

## 判定：零发现（连续零发现 2/2）——循环收敛休眠

按 r44 准则（连续两轮零发现且角度互异：r69 注册型物品面 / r70 跨
维度距离面），宣告**循环再次收敛**，休眠待触发（条件不变：Paper/
Slimefun 演进、玩法变更、负载画像、用户指示）。

## 验证

纯代码判定轮（无服务器启动/无进程占用）；业务端口 25565 未触碰。

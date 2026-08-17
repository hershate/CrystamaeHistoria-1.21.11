# 性能优化第 55 轮：实体扫描类型切片化——阴性结果（试行后回退）

日期：2026-08-17
域：**实体扫描 API 重载形态**——探针角度（与 r42-54 互异）：无类型
`getNearbyEntities` + `instanceof` 过滤与谓词形态（`X.class::isInstance`）
共 ~29 处，假设 Paper 的 `getNearbyEntitiesByType`（ByType）走区块
类型切片可跳过非目标实体。**试行 25 处转换后实测回退。**

## 试行范围（已全部回退）

- 13 处法术 LivingEntity 扫描（含共享 AOE 路径 `Spell.getTargets`）；
- 12 处谓词/具体类站点（Item 拉取 ×3、经验球、Player、Breedable、
  Zombie 切片、Animals/Mob 切片、LivingEntity 装置 ×3）；
- 文档化排除 4 处：Bobulate（Colorable 不在 Entity 层级）、MobFan
  （推开全部实体，instanceof Player 仅为提前返回门）、CursedEarth
  占位检查（需"任意实体"语义）、WorldUtils（泛型工具）。

## 结果（服务器内混合实体场景：40 物品 + 15 僵尸 + 5 箭 + 3 经验球，round-55-server.tsv）

| 基准 | 旧（谓词/无类型扫描） | 新（ByType） | 差异 |
|------|----|----|------|
| entityScan.living（r6 盒，n=15 living） | 1609.12 ns | 2200.50 ns | **回退 -36.8%** |
| entityScan.item（r3 盒，n=30 item） | 2516.90 ns | 2591.35 ns | 持平偏差 -3.0% |
| entityScan.orb（r4 盒，n=3 orb） | 2081.41 ns | 2008.86 ns | 持平偏差 +3.5% |

等价性：两形态同盒集合逐 UUID 相等（living=15 / item=30，r4/r8 两档
true）——转换语义正确，但**无性能收益且 living 形态实测回退**。

## 判定：试行后回退（r24/r50 先例）

- **主源全部回退**（25 处恢复原形态），基准变体与数据保留为阴性
  证据；
- 归因：**Paper 1.17+ 实体系统重写**——区块实体存储已扁平化，
  1.16 及以前的 per-section `ClassInheritanceMultiMap` 类型切片架构
  不复存在；1.21.11 的 ByType 内部与谓词形态同为实体遍历 +
  类判定，ByType 还多一层泛型集合组装（living 场景 -36.8% 由此
  而来——本例谓词 short-circuit 早退更便宜）；
- **族矩阵增补行：实体扫描 ByType 切片——实测无益（架构前提在
  1.17+ 不成立）**；这是首次因"上游平台架构变迁"推翻教科书式
  优化假设，方法论：**平台版本敏感性——教科书结论必须按当前
  服务端实测复核**（与 r50 sqrtsd 边界同型）。

## 会话记录

COMPLETE=1、CH 插件错误 0、watchdog 3 次（重负载旧变体批次固有）。

## 循环状态

r54（零发现判定，1/2）→ r55（阴性边界轮）。阴性轮不重置收敛计数
（r50 同例——它不是判定轮也不是实质优化轮）；下一判定轮若零发现
即达"连续两轮"宣告第十轮循环收敛。

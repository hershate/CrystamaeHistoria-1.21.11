# 审计第 15 轮：收尾覆盖——PlayerStatistics / ConfigManager / 剩余 utils

日期：2026-08-15
范围：`player/PlayerStatistics`（全量精读）、`managers/ConfigManager`（全量精读）、剩余 utils（ParticleUtils/WorldUtils/TimePeriod/GildingUtils/ArmourStandUtils/Skulls/TextUtils/NameUtils 方法级扫描）

## 已修复（1 个 commit）

| commit | 问题 |
|--------|------|
| `339abbe` | **PlayerStatistics 两处复制粘贴错误**：`addRealisation(Player)` 误调 `addChronicle`（现实转化计数被记入发掘计数）；`getRealisation(Player)` 误调 `getChronicle`——**故事集图鉴"现实转化次数"列显示的是发掘次数**（表述与实现不符；UUID 重载正确，祭坛主路径与图鉴统计路径未受影响）。`getStoryRank` 除零防御（blocks.yml 清空时 NaN）。ConfigManager 资源缺失 NPE 防御 |

## 核验安全（记录依据）

- ConfigManager：损坏 YAML 有 catch+日志（以空配置降级，可恢复）；spells.yml 缺键自动补 true；saveAll 双文件落盘 ✓
- PlayerStatistics 的 YAML 路径拼接（uuid.SPELL/STORY.id.字段）读写对称 ✓
- 剩余 utils 为纯展示/查询函数，各调用方在前几轮已覆盖；ParticleUtils.drawLine 跨世界调用方已有同世界判定 ✓

## 全量审计覆盖状态（15 轮后）

全部 265 个 Java 文件的类别覆盖：法术 67 ✓、机械 5+基类 ✓、gadgets 18 ✓、artistic/exhalted/materials 17 ✓、mobgoals 10 ✓、监听器 17 ✓、GUI 全面板 ✓、PDC 全类型 ✓、managers 6 ✓、player ✓、commands 5 ✓、utils 核心 ✓、runnables 全部 ✓。

## 验证

`JAVA_HOME=F:/Java/21 mvn -q package` 构建通过。

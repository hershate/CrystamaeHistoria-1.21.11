# 审计第 37 轮：祭坛提取链服务器端 E2E（专用驱动）——发现并修复迁移级粒子缺陷

日期：2026-08-18
范围：第 36 轮遗留的现实祭坛提取链端到端验证；以专用驱动插件
（`benchmark/audit-driver/CHAuditDriver`）反射驱动生产代码
`RealisationAltarCache.process()`，RCON 键值对输出，全链路服务器端实证。

## 发现并修复：Color 数据粒子无数据调用崩溃（迁移级功能缺陷，7750049）

**缺陷**：Paper 1.21.x 起 `Particle.FLASH` 的数据类型契约为 `org.bukkit.Color`，
无数据重载 `spawnParticle(FLASH, loc, count, dx, dy, dz)` 直接抛
`IllegalArgumentException: missing required data class org.bukkit.Color`。
触发面（迁移遗留，1.21.11-1 迁移未复核粒子契约变更）：

- **现实祭坛每次成功提取故事**（`RealisationAltarCache.summonConsumeParticles`）
  ——提取/写盘/晶簇生成已完成，异常在收尾粒子处抛出并穿透 tick；
- **棱镜镀金器吸取路径**（`PrismaticGilderCache:182`）。

**修复**：`ParticleUtils.displayParticleEffect(Location, Particle, double, int)`
中央处理——`particle.getDataType() == Color.class` 时改走数据重载补
`Color.WHITE`（满足 API 契约，FLASH 视觉效果不变）。其余需数据粒子
（BLOCK/ITEM 等）本插件未使用，保持原有抛错行为（开发期显性暴露）。

**发现方法**：驱动首轮在 `process()` 第 2 次迭代捕获
`InvocationTargetException`，cause 链直指 `ParticleUtils:29`——正式 tick
路径中该异常被 Slimefun ticker 捕获生成错误报告并累计（错误报告文件
`plugins/Slimefun/error-reports/`），长期运行将按 Slimefun 策略停摆机械 tick。

## E2E 验证（端口 25599，PID 49264，RCON 优雅停服）

驱动流程（全部生产代码路径）与结果：

| 断言 | 结果 |
|------|------|
| 祭坛缓存跨重启恢复（chunk PDC → loadMap） | ✅ cache_found=true |
| 满故事物品构建（makeStoried + pickStory/commitStory） | ✅ limit=1 stories=2（常规+独特） |
| BlockMenu 槽位注入 | ✅ injected=true |
| 5000×process()：提取（1/6/tick）+ 物品完全消耗 | ✅ inputLeft=none |
| 晶簇生成与成熟（small→medium→large 全程） | ✅ mapSize=2 large=2 |
| 真实 BlockBreakEvent → CrystalBreakListener | ✅ cancelled=true（抑制原版掉落）blockNow=AIR |
| 破碎状态清理 | ✅ mapSize 2→1 |
| 碎片掉落（dropShards → 真实水晶实体） | ✅ droppedEntities=3（PLAYER_HEAD ×2/×1/×1） |
| 修复后 5000 次迭代零异常 | ✅（修复前第 2 次迭代即抛） |

## 工程沉淀

- 驱动插件技术要点：Bukkit 插件类加载器隔离下**同包不授予 protected 访问**
  （IllegalAccessError）——须反射 + setAccessible；RCON 命令在主线程执行 ✓；
  键值对输出（`AUDIT37|key=value`）便于自动化断言。
- 驱动已入库 `benchmark/audit-driver/`（build 产物 gitignore），可复用于
  后续轮次的机械链路验证。

## 验证

`JAVA_HOME=F:/Java/21 mvn -q package` 构建通过；修复后实机如上全绿；
停服后驱动 jar 移出、server.properties/ops.json 还原、端口释放、
业务端口 25565 全程未触碰；会话日志 0 ERROR/SEVERE（除修复前首轮的
预期错误报告）。

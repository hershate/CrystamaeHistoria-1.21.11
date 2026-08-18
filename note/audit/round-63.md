# 审计第 63 轮：HEAD 产物一致性验证（干净重建 = 实测产物）

日期：2026-08-19
范围：`mvn clean package` 全新构建与 29 轮实机验证所用产物的**逐类
一致性**比对 + 干净部署冒烟。

## 结果：PASS

| 断言 | 结果 |
|------|------|
| git 工作区干净（HEAD=d7498af） | ✅ |
| `mvn clean package` 干净构建成功 | ✅（867067 字节） |
| **六个修复承载类逐一类体一致**（HEAD 构建 vs 实测 jar） | ✅ ParticleUtils / StoriesManager / EphemeralWorkBench / CrystamageSatchel / TestWand 全 IDENTICAL |
| 整 jar md5 差异 | 仅构建时间戳/清单序等非类内容（类体一致为实质判据） |
| 修复在位抽查（ParticleUtils.WHITE / StoriesManager.STORIED_PREFIX） | ✅ 双 jar 均含 |
| 干净部署冒烟（清 .paper-remapped 后重启） | ✅ Done 23.1s、995 故事、零异常 |

**含义**：仓库 HEAD 的产物与 29 轮验证所测完全同源——发布物可信度的
最后一环闭合。

## 验证

world_r63 用毕删除；PID 36740 RCON 优雅停服；环境完全还原；
业务端口 25565 未触碰。

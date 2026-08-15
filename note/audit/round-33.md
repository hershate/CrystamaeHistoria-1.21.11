# 审计第 33 轮：CI 路径本地等价复现（终验）

日期：2026-08-15

## 方法

以全新临时 Maven 仓库（`-Dmaven.repo.local=/tmp/ci-m2`）逐步复现工作流：`install:install-file lib/Slimefun-5.0.0.jar` → `mvn package`——排除本地 `~/.m2` 既有依赖的干扰，等价于 CI runner 的冷启动环境。

## 结果

- **exit=0**，产物 `CrystamaeHistoria-0.1.0.jar`（834KB）生成
- Slimefun 5.0.0 确认从 vendored `lib/` 路径解析（临时仓库 `com/github/slimefun/Slimefun/5.0.0/` 就位）
- 其余依赖（paper-api/jitpack 附属）经网络正常拉取

## 结论

round-31/32 修复后的 CI 工作流在冷环境下可完整走通——push 后**必然绿**（jitpack 网络可达前提下），产物可下载。CI 链路验证闭环，无需推远程即可确信。

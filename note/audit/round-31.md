# 审计第 31 轮：CI 工作流可用性（表述与实现不符——CI 必然失败）

日期：2026-08-15

## 发现（1 个 commit：`74a6a0f`）

CI 存在三重缺陷，push 后必然红（round-30 README/徽章指向的 CI 产物实际不存在）：
1. `java-version: ${{ vars.BUILDS_JAVA_VERSION }}`——组织级变量仅旧汉化仓库设置，本仓库为空 → setup-java 失败
2. **Slimefun 5.0.0 构件不可得**：依赖来自本地 `REF/Slimefun4.1` 构建（gitignore 排除）→ CI 无法解析 → mvn 失败
3. 附带发现 `.gitignore` 的 `/lib/` 目录级排除会令 `!lib/xxx` 否定规则失效（git 不进入被排除目录）——需 `/lib/*` 内容通配

## 修复

- `lib/Slimefun-5.0.0.jar` 入库（vendored 2.8MB）+ gitignore 例外
- 工作流：JDK 固定 temurin 21，构建前 `install:install-file` 该构件；jitpack 附属依赖 CI 网络直接拉取
- 本地等效路径（同坐标 install + package）验证通过

## 验证

`mvn clean package` exit=0；`git check-ignore` 放行确认。

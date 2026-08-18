# 审计第 45 轮：旧存档兼容性复验（v1 编码双读）

日期：2026-08-19
范围：以驱动 `legacy` 子命令在**当前构建**（含 r34-44 全部修复）上执行
"v1 编码写入 → v2 双读断言"三组实测——复验 perf r26-29 迁移的双读兼容
在后续 11 轮修复后仍然成立。

## 结果：三组全 PASS（端口 25599，PID 43516，RCON 优雅停服）

| 组 | v1 写入（旧编码类） | 当前构建双读 | 断言 | 结果 |
|----|------|------|------|------|
| 故事列表 | `PersistentStoriesDataType`（PDC_STORIES 容器数组） | `StoryUtils.getAllStories` | 数量 3/3 + 逐项 id/稀有度一致 | **PASS** |
| 法杖存储 | `PersistentStaveDataType`（每板子容器） | `PersistentStaveV2DataType.readStaveMap`（v2 优先→v1 同键回退） | 槽数 1 + 法术 PUSH + 晶能 123 一致 | **PASS** |
| 区块晶簇 | `PersistentStoryChunkDataType`（容器数组，玩家所在区块） | `PersistentStoryChunkV2DataType.readChunkStories` | 数量/id/方块位置一致（测试键用毕清理） | **PASS** |

v1 读写器类经 11 轮修复后完好无损；会话 0 异常。

## 驱动增强（入库）

`chdriver legacy`：三组 v1→v2 双读断言，供每次涉及 PDC 编码的改动后回归。

## 验证

全新世界 world_r45（用毕删除）；环境完全还原；业务端口 25565 未触碰。

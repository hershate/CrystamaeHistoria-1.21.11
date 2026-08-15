# 审计第 21 轮：数据文件完整性校验

日期：2026-08-15
范围：`src/main/resources/`（blocks.yml 354KB、block_colors.yml 30KB、tags/*.json ×11）

## 校验结果

| 数据 | 校验项 | 结果 |
|------|--------|------|
| blocks.yml | 顶层键数 | 995（与服务器日志"已加载: 995 个独特的方块故事"一致） |
| blocks.yml | tier 值域 | 510×T1 + 378×T2 + 69×T3 + 23×T4 + 15×T5 = 995，全部 1-5 合法，零越界 |
| blocks.yml | 键有效性 | **实测证明**：加载器（round-16 修复后）对无效材质逐键跳过并告警，五次服务器启动零跳过日志——995 键全部为合法 Material |
| block_colors.yml | 898 键，flow 序列 `[r, g, b]` 格式统一 | 合法 YAML；PhilosophersStone 读取经 round-5 的 Number 防御 |
| tags/*.json | 11 个文件结构（values 非空列表）、材质名全大写规范 | 全部通过（16/4/4/4/4/4/4/4/4/16/66 条） |

## 备注

- 枚举级交叉比对工具（javap 提取）在当前环境未产出，但实际服务器加载即为逐键 `Material.getMaterial()` 验证，证明力更强。
- 995 键的 tier 分布与方块等级设计（T1 常见 → T5 稀有）单调递减一致，无异常聚集。

## 验证

数据质量与代码加载路径（round-16/14 防御）双向确认通过。

# 审计第 27 轮：构建配置与产物结构（纯核验轮）

日期：2026-08-15

## 核验结论（全部通过）

| 检查项 | 结果 |
|--------|------|
| **第三方类零打入** | jar 内无 `io/github/thebusybiscuit`、`com/destroystokyo`、`org/bukkit` 任何类——provided 依赖（paper-api/Slimefun/三附属）正确未捆绑，与"仅依赖 Slimefun"红线一致 |
| 资源 filtering | `plugin.yml` 的 `${project.version}` 正确替换为 `1.21.11-2`；本轮 usage 修正确认已打包；纯文本资源无二进制 filtering 风险 |
| jar 结构 | MANIFEST 存在（shade 的 META-INF 排除保留了清单）；数据文件（blocks/colors/stories/tags）全部在包内 |
| 运行验证 | 该结构已通过 7 次测试服加载（274 物品/995 故事） |

## 验证

`unzip -l/-p` 逐项核验产物内容。

# benchmark —— 性能基准测试

量化性能优化前后的独立基准测试程序（无需服务器运行时）。

## 运行

```bash
mvn compile   # 保证 target/classes 与 target/cp.txt 就绪（run.sh 会自动生成 cp.txt）
bash benchmark/run.sh benchmark/results/round-N.tsv [fork数，默认3]
```

- Java 21（`F:/Java/21`），输出 TSV：`bench\tvariant\tmedian_ns_op`（跨 fork 取各 fork
  中位数的均值）。
- 逐 fork 原始数据（median/min/p95）在临时文件中，聚合后即删；如需留存请改 run.sh。

## 基准清单

（每轮基准源码覆盖前轮变体；历轮数据见 `results/`，服务器内基准见 `server-addon/`。当前为第 23 轮变体。）

| 基准 | 文件 | 测量内容 |
|------|------|----------|
| `stats.countStories274`（全路径 / 相对无缓存 / 纪元命中）+ `stats.countAfterWrite`（失效重算） | `BenchStatsRead.java` | 统计计数三变体对打（真实 YamlConfiguration，纪元缓存为 PlayerStatistics 同构副本）；含稳态/失效/恢复等价性断言。**形态注意**：计数变体必须按 `size` 循环（每次迭代一次完整计数），否则 Harness 按 size 误除——round-22 曾因此失真，已更正 |

| 基准 | 文件 | 测量内容 |
|------|------|----------|


## 方法论边界（禁止误读）

- 需 Bukkit 服务器运行时的类（`SpellType` 枚举、实体句柄等）无法脱离服务器构造，
  相关基准以**同形态模型类 / 真实 java.util 操作序列**测量，报告中均已注明。
- 时间驱动预热（默认 3s）保证 JIT 稳定；多 fork 降低运行间漂移。
- 黑洞值经 stderr 汇出，防死码消除。

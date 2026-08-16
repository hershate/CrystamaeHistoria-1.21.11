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

（每轮基准源码覆盖前轮变体；历轮数据见 `results/`，服务器内基准见 `server-addon/`。当前为第 22 轮变体。）

| 基准 | 文件 | 测量内容 |
|------|------|----------|
| `stats.singleCheck` / `stats.pageCheck36` / `stats.countSpells69` / `stats.countStories274` | `BenchStatsRead.java` | 玩家统计读取（真实 YamlConfiguration，1 玩家 × 69 法术 × 274 故事）：全路径 getBoolean（旧）vs 页级子节单次解析 + 相对读取（新）；含逐键等价性与缺失语义断言 |

## 方法论边界（禁止误读）

- 需 Bukkit 服务器运行时的类（`SpellType` 枚举、实体句柄等）无法脱离服务器构造，
  相关基准以**同形态模型类 / 真实 java.util 操作序列**测量，报告中均已注明。
- 时间驱动预热（默认 3s）保证 JIT 稳定；多 fork 降低运行间漂移。
- 黑洞值经 stderr 汇出，防死码消除。

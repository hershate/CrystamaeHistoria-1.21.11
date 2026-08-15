#!/usr/bin/env bash
# 基准测试运行脚本（在仓库根目录执行：bash benchmark/run.sh [输出tsv路径] [fork数]）
# 依赖：Java 21 (F:/Java/21)、Maven 已能解析项目依赖（先 mvn compile 生成 target/cp.txt）
set -euo pipefail
cd "$(dirname "$0")/.."

JAVA=F:/Java/21/bin/java
JAVAC=F:/Java/21/bin/javac
OUT="${1:-benchmark/results/latest.tsv}"
FORKS="${2:-3}"

if [ ! -f target/cp.txt ]; then
    JAVA_HOME=F:/Java/21 mvn -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt
fi

mkdir -p benchmark/build benchmark/results
$JAVAC -proc:none -cp "target/classes;$(cat target/cp.txt)" -d benchmark/build \
    benchmark/src/bench/*.java

TMP=$(mktemp)
for i in $(seq 1 "$FORKS"); do
    $JAVA -Xms256m -Xmx256m -cp "benchmark/build;target/classes;$(cat target/cp.txt)" \
        bench.BenchRunner > "$TMP.fork$i"
done

# 聚合：跨 fork 取中位数的平均（每变体的多行 → 每行一个 fork 的中位数）
python - "$OUT" "$FORKS" "$TMP" <<'EOF'
import sys, statistics
out, forks, tmp = sys.argv[1], int(sys.argv[2]), sys.argv[3]
rows = {}
header = None
for i in range(1, forks + 1):
    with open(f"{tmp}.fork{i}") as f:
        lines = [l.rstrip("\n") for l in f if l.strip()]
    header = lines[0]
    for l in lines[1:]:
        p = l.split("\t")
        key = (p[0], p[1])
        rows.setdefault(key, []).append(float(p[2]))
with open(out, "w") as f:
    f.write(header + "\n")
    for (bench, variant), vals in sorted(rows.items()):
        f.write(f"{bench}\t{variant}\t{statistics.mean(vals):.2f}\n")
EOF
rm -f "$TMP".fork*
echo "==== 聚合结果（跨 $FORKS fork 均值，ns/op）===="
cat "$OUT"

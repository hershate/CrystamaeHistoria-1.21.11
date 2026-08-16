#!/usr/bin/env bash
# 服务器基准会话（在仓库根目录执行）：bash benchmark/run-server-bench.sh <日志名> [最大等待秒数]
# 流程：清理 25565 端口残留服务端 → 启动 Paper（基准插件延迟 5s 自动跑分）→ 到时发送 stop → 汇报状态
# 注意：仅清理确认监听 25565 的进程（paper 测试服）；Gradle/Maven 等其他 java 进程不受影响。
set -uo pipefail
SERVER_DIR="F:/paper-test-1.21.11"
LOG_NAME="${1:-bench-session.log}"
MAX_WAIT="${2:-240}"

cd "$SERVER_DIR" || exit 1

# 1) 清理残留服务端（仅监听 25565 者）
STALE_PID=$(netstat -ano | grep ":25565" | grep -i listening | awk '{print $NF}' | head -1)
if [ -n "${STALE_PID:-}" ]; then
  echo "killing stale server on :25565 (pid $STALE_PID)"
  taskkill //F //PID "$STALE_PID" || true
  sleep 3
fi

# 2) 启动并到时停机（sleep 从 JVM 启动计；COMPLETE 通常在 +120s 内）
rm -f plugins/CHPerfBench/results.tsv
(sleep "$MAX_WAIT"; echo stop) | F:/Java/21/bin/java -jar paper.jar nogui > "$LOG_NAME" 2>&1
EXIT=$?

# 3) 汇报
COMPLETE=$(grep -c "CHPERFBENCH COMPLETE" "$LOG_NAME" || true)
WATCHDOG=$(grep -c "not responded" "$LOG_NAME" || true)
CH_ERR=$(python -c "
data = open('$LOG_NAME','rb').read().decode('gbk', errors='replace')
lines = [l for l in data.splitlines() if l.startswith('[') and 'ERROR' in l and 'CrystamaeHistoria' in l and 'DO NOT REPORT' not in l]
print(len(lines))
" 2>/dev/null || echo "?")
echo "EXIT=$EXIT COMPLETE=$COMPLETE WATCHDOG_DUMPS=$WATCHDOG CH_ERRORS=$CH_ERR"
echo "results: $SERVER_DIR/plugins/CHPerfBench/results.tsv"

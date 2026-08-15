#!/usr/bin/env bash
# 构建服务器内基准插件（在仓库根目录执行）
set -euo pipefail
cd "$(dirname "$0")/../.."

JAVA=F:/Java/21/bin/java
JAVAC=F:/Java/21/bin/javac
JAR=F:/Java/21/bin/jar

if [ ! -f target/cp.txt ]; then
    JAVA_HOME=F:/Java/21 mvn -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt
fi

mkdir -p benchmark/server-addon/build
$JAVAC -proc:none -cp "target/classes;$(cat target/cp.txt)" \
    -d benchmark/server-addon/build \
    benchmark/server-addon/src/ch/perfbench/CHPerfBench.java
cp benchmark/server-addon/plugin.yml benchmark/server-addon/build/
rm -f benchmark/server-addon/CHPerfBench.jar
(cd benchmark/server-addon/build && $JAR cf ../CHPerfBench.jar .)
echo "built: benchmark/server-addon/CHPerfBench.jar"

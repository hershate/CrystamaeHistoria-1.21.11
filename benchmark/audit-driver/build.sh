#!/usr/bin/env bash
# 构建审计驱动插件（在仓库根目录执行）
set -euo pipefail
cd "$(dirname "$0")/../.."

JAVAC=F:/Java/21/bin/javac
JAR=F:/Java/21/bin/jar

if [ ! -f target/cp.txt ]; then
    JAVA_HOME=F:/Java/21 mvn -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt
fi

mkdir -p benchmark/audit-driver/build
$JAVAC -proc:none -cp "target/classes;$(cat target/cp.txt)" \
    -d benchmark/audit-driver/build \
    benchmark/audit-driver/src/io/github/sefiraat/crystamaehistoria/slimefun/items/mechanisms/realisationaltar/DriverPlugin.java
cp benchmark/audit-driver/plugin.yml benchmark/audit-driver/build/
rm -f benchmark/audit-driver/CHAuditDriver.jar
(cd benchmark/audit-driver/build && $JAR cf ../CHAuditDriver.jar .)
echo "built: benchmark/audit-driver/CHAuditDriver.jar"

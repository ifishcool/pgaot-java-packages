#!/bin/bash
# 按依赖顺序 install 到本地 ~/.m2，方便本地联调
# 用法: ./install-local.sh          # 全部模块
#       ./install-local.sh code-sql # 单个模块

set -e
ROOT="$(cd "$(dirname "$0")" && pwd)"

install_module() {
    echo "====> mvn install -DskipTests: $1"
    mvn -f "$ROOT/$1/pom.xml" install -DskipTests -q
    echo "        $1 → ~/.m2 ✓"
}

if [ $# -gt 0 ]; then
    install_module "$1"
else
    install_module code-sql
    install_module code-auth
    install_module code-datasheet
    echo ""
    echo "全部完成，下游模块可直接 mvn compile/test"
fi

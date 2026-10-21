#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
task_java="${JAVA_HOME:+$JAVA_HOME/bin/}"
mkdir -p build/core-tests
mapfile -t task_sources < <(find src/main/java src/coreTest/java -name '*.java' -type f | sort)
"${task_java}javac" -encoding UTF-8 -d build/core-tests "${task_sources[@]}"
"${task_java}java" -ea -cp build/core-tests fr.clax.aurel.ui.CoreChecks

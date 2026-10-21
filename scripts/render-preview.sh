#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
task_java="${JAVA_HOME:+$JAVA_HOME/bin/}"
mkdir -p build/preview-classes
mapfile -t task_sources < <(find src/main/java -name '*.java' -type f | sort)
"${task_java}javac" -encoding UTF-8 -d build/preview-classes "${task_sources[@]}" tools/PreviewRenderer.java
"${task_java}java" -Djava.awt.headless=true -cp build/preview-classes PreviewRenderer "${1:-build/preview.png}" "${2:-0}"

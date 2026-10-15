#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
kotlinc \
  app/src/main/java/com/coachlink/app/domain/NutritionMath.kt \
  app/src/main/java/com/coachlink/app/domain/WeeklyDashboard.kt \
  tools/weekly_stats_smoke.kt -include-runtime -d "$workdir/weekly-smoke.jar"
java -jar "$workdir/weekly-smoke.jar"

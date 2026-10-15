#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
kotlinc app/src/main/java/com/coachlink/app/domain/NutritionMath.kt \
  app/src/main/java/com/coachlink/app/domain/AdaptiveCoach.kt \
  app/src/main/java/com/coachlink/app/domain/NutritionCoach.kt \
  app/src/main/java/com/coachlink/app/domain/ShoppingPlanner.kt \
  tools/adaptive_coach_smoke.kt -include-runtime -d "$TMP/coach-tests.jar"
java -jar "$TMP/coach-tests.jar"

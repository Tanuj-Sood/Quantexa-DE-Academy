#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${SPARK_HOME:-}" ]]; then
  echo "SPARK_HOME is not set. Export SPARK_HOME and rerun."
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$PROJECT_ROOT"

# "Building jar with Gradle..."
#gradle clean jar

JAR_PATH="$(ls -t build/libs/*.jar | head -n 1)"
if [[ -z "$JAR_PATH" ]]; then
  echo "No jar found in build/libs after gradle jar."
  exit 1
fi

echo "Using jar: $JAR_PATH"

OUTPUT_BASE_PATH="$PROJECT_ROOT/src/main/resources"

CLASSES=(
  "com.quantexa.assessments.accounts.AccountAssessment"
  "com.quantexa.assessments.customerAddresses.CustomerAddress"
  "com.quantexa.assessments.scoringModel.ScoringModel"
)

for class_name in "${CLASSES[@]}"; do
  echo "Running $class_name"
  "$SPARK_HOME/bin/spark-submit" --class "$class_name" --master local[*] --driver-java-options "-Dqde.output.base.path=$OUTPUT_BASE_PATH" "$JAR_PATH"
done

echo "Done. Parquet outputs are under src/main/resources/"

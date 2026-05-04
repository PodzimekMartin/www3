#!/usr/bin/env sh
set -eu

BASE_URL="${BASE_URL:-http://localhost:8080}"

curl -fsS "$BASE_URL/actuator/health/readiness" >/dev/null
curl -fsS "$BASE_URL/api/state" >/dev/null

echo "Smoke test passed for $BASE_URL"

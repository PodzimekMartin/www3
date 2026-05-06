#!/usr/bin/env sh
set -eu

BASE_URL="${BASE_URL:-http://localhost:8080}"

curl -fsS "$BASE_URL/actuator/health/readiness" >/dev/null
TOKEN="$(curl -fsS "$BASE_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' \
  | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')"

test -n "$TOKEN"
curl -fsS "$BASE_URL/api/state" -H "X-Auth-Token: $TOKEN" >/dev/null

echo "Smoke test passed for $BASE_URL"

#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT="${MMPAY_SMOKE_PROJECT:-mmpay-smoke}"

export MMPAY_AUDIT_HMAC_KEY="${MMPAY_AUDIT_HMAC_KEY:-$(openssl rand -base64 32)}"
export MMPAY_HTTP_PORT="${MMPAY_SMOKE_HTTP_PORT:-18080}"
export MMPAY_POSTGRES_PORT="${MMPAY_SMOKE_POSTGRES_PORT:-15432}"
export MMPAY_REDIS_PORT="${MMPAY_SMOKE_REDIS_PORT:-16379}"

compose=(docker compose -p "$PROJECT" -f "$ROOT_DIR/deploy/docker-compose.yml")

cleanup() {
  "${compose[@]}" down --volumes --remove-orphans >/dev/null 2>&1 || true
}
trap cleanup EXIT

config="$("${compose[@]}" config)"
grep -Fq "SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/mmpay" <<<"$config"
grep -Fq "SPRING_DATASOURCE_USERNAME: mmpay" <<<"$config"
grep -Fq "SPRING_DATASOURCE_PASSWORD: replace-with-local-password" <<<"$config"

cleanup
"${compose[@]}" up --build -d

app_container="$("${compose[@]}" ps -q mmpay-app)"
if [[ -z "$app_container" ]]; then
  echo "mmpay-app container was not created" >&2
  exit 1
fi

docker inspect "$app_container" --format '{{range .Config.Env}}{{println .}}{{end}}' \
  | grep -Fq "SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/mmpay"

for _ in {1..60}; do
  if curl -fsS "http://127.0.0.1:${MMPAY_HTTP_PORT}/actuator/health" | grep -Fq '"status":"UP"'; then
    if "${compose[@]}" logs mmpay-app \
      | grep -Eq "Failed to configure a DataSource|Failed to determine a suitable driver class"; then
      echo "mmpay-app logs contain a datasource startup failure" >&2
      exit 1
    fi
    echo "mmpay docker compose smoke passed on http://127.0.0.1:${MMPAY_HTTP_PORT}"
    exit 0
  fi
  sleep 2
done

"${compose[@]}" logs mmpay-app >&2
echo "mmpay-app health did not become UP" >&2
exit 1

#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT="${MMPAY_SMOKE_PROJECT:-mmpay-smoke}"
STANDARD_COMPOSE="$ROOT_DIR/deploy/docker-compose.yml"
MINIMAL_COMPOSE="$ROOT_DIR/deploy/docker-compose.minimal.yml"
CUSTOM_COMPOSE="${MMPAY_SMOKE_COMPOSE_FILE:-}"
DATASOURCE_FAILURE_PATTERN="Failed to configure a DataSource|Failed to determine a suitable driver class"

export MMPAY_AUDIT_HMAC_KEY="${MMPAY_AUDIT_HMAC_KEY:-$(openssl rand -base64 32)}"
export MMPAY_HTTP_PORT="${MMPAY_SMOKE_HTTP_PORT:-18080}"
export MMPAY_POSTGRES_PORT="${MMPAY_SMOKE_POSTGRES_PORT:-15432}"
export MMPAY_REDIS_PORT="${MMPAY_SMOKE_REDIS_PORT:-16379}"

cleanup_project() {
  local project="$1"
  local compose_file="$2"

  if [[ -n "$compose_file" ]]; then
    docker compose -p "$project" -f "$compose_file" down --volumes --remove-orphans >/dev/null 2>&1 || true
  fi
}

cleanup() {
  cleanup_project "${PROJECT}-standard" "$STANDARD_COMPOSE"
  cleanup_project "${PROJECT}-minimal" "$MINIMAL_COMPOSE"
  cleanup_project "${PROJECT}-custom" "$CUSTOM_COMPOSE"
}

trap cleanup EXIT

assert_compose_config() {
  local compose_file="$1"
  local project="$2"
  local config
  local -a compose=(docker compose -p "$project" -f "$compose_file")

  config="$("${compose[@]}" config)"
  grep -Fq "SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/mmpay" <<<"$config"
  grep -Fq "SPRING_DATASOURCE_USERNAME: mmpay" <<<"$config"
  grep -Fq "SPRING_DATASOURCE_PASSWORD: replace-with-local-password" <<<"$config"
}

assert_bare_image_requires_runtime_env() {
  local output
  local status

  set +e
  output="$(timeout 30s docker run --rm mmpay-app:local 2>&1)"
  status=$?
  set -e

  if grep -Eq "$DATASOURCE_FAILURE_PATTERN" <<<"$output"; then
    echo "bare image startup unexpectedly reached Spring datasource auto-configuration" >&2
    echo "$output" >&2
    exit 1
  fi

  if [[ "$status" -ne 78 ]]; then
    echo "bare image startup returned $status instead of missing configuration exit 78" >&2
    echo "$output" >&2
    exit 1
  fi

  grep -Fq "startup configuration error" <<<"$output"
}

assert_container_datasource_env() {
  local app_container="$1"

  docker inspect "$app_container" --format '{{range .Config.Env}}{{println .}}{{end}}' \
    | grep -Fq "SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/mmpay"
}

wait_for_health() {
  local project="$1"
  local compose_file="$2"
  local -a compose=(docker compose -p "$project" -f "$compose_file")

  for _ in {1..60}; do
    if curl -fsS "http://127.0.0.1:${MMPAY_HTTP_PORT}/actuator/health" | grep -Fq '"status":"UP"'; then
      if "${compose[@]}" logs mmpay-app | grep -Eq "$DATASOURCE_FAILURE_PATTERN"; then
        echo "mmpay-app logs contain a datasource startup failure" >&2
        exit 1
      fi
      return 0
    fi
    sleep 2
  done

  "${compose[@]}" logs mmpay-app >&2
  echo "mmpay-app health did not become UP" >&2
  exit 1
}

run_compose_smoke() {
  local compose_file="$1"
  local project="$2"
  local label="$3"
  local app_container
  local -a compose=(docker compose -p "$project" -f "$compose_file")

  cleanup_project "$project" "$compose_file"
  assert_compose_config "$compose_file" "$project"
  "${compose[@]}" build mmpay-app
  assert_bare_image_requires_runtime_env
  "${compose[@]}" up -d

  app_container="$("${compose[@]}" ps -q mmpay-app)"
  if [[ -z "$app_container" ]]; then
    echo "mmpay-app container was not created for $label" >&2
    exit 1
  fi

  assert_container_datasource_env "$app_container"
  wait_for_health "$project" "$compose_file"
  cleanup_project "$project" "$compose_file"
  echo "mmpay $label docker compose smoke passed on http://127.0.0.1:${MMPAY_HTTP_PORT}"
}

if [[ -n "$CUSTOM_COMPOSE" ]]; then
  run_compose_smoke "$CUSTOM_COMPOSE" "${PROJECT}-custom" "custom"
else
  run_compose_smoke "$STANDARD_COMPOSE" "${PROJECT}-standard" "standard"
  run_compose_smoke "$MINIMAL_COMPOSE" "${PROJECT}-minimal" "minimal"
fi

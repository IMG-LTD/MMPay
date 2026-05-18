#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${MMPAY_HUIFU_ENV_FILE:-$HOME/.config/mmpay/huifu.env}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Huifu env file not found: $ENV_FILE" >&2
  exit 1
fi

mode="$(stat -c '%a' "$ENV_FILE")"
if [[ "$mode" != "600" && "$mode" != "400" ]]; then
  echo "Huifu env file must be chmod 600 or 400: $ENV_FILE" >&2
  exit 1
fi

set -a
# shellcheck source=/dev/null
. "$ENV_FILE"
set +a

required_vars=(
  HUIFU_SYS_ID
  HUIFU_PRODUCT_ID
  HUIFU_RSA_PUBLIC_KEY
  HUIFU_RSA_PRIVATE_KEY
  HUIFU_SKILL_SOURCE
  HUIFU_MERCHANT_ID
  HUIFU_NOTIFY_URL
  HUIFU_WEBHOOK_ENDPOINT_KEY
)

for name in "${required_vars[@]}"; do
  if [[ -z "${!name:-}" ]]; then
    echo "Missing required Huifu environment variable: $name" >&2
    exit 1
  fi
done

export MMPAY_HUIFU_ENV_SMOKE=true
timeout 60s mvn -f "$ROOT_DIR/backend/pom.xml" -pl mmpay-adapter-huifu -am \
  -Dtest=HuifuEnvironmentSmokeTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test

echo "huifu env smoke passed"

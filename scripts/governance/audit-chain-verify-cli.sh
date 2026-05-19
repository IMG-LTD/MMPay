#!/usr/bin/env bash
set -euo pipefail

from_id=""
to_id=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --from)
      from_id="${2:?--from requires a value}"
      shift 2
      ;;
    --to)
      to_id="${2:?--to requires a value}"
      shift 2
      ;;
    *)
      echo "unknown argument: $1" >&2
      exit 64
      ;;
  esac
done

: "${MMPAY_BASE_URL:?MMPAY_BASE_URL is required}"
: "${MMPAY_ADMIN_TOKEN:?MMPAY_ADMIN_TOKEN is required}"

query=""
if [[ -n "$from_id" || -n "$to_id" ]]; then
  query="?"
  [[ -n "$from_id" ]] && query="${query}from=${from_id}"
  [[ -n "$from_id" && -n "$to_id" ]] && query="${query}&"
  [[ -n "$to_id" ]] && query="${query}to=${to_id}"
fi

response="$(
  curl -fsS \
    -H "Authorization: Bearer ${MMPAY_ADMIN_TOKEN}" \
    -H "Accept: application/json" \
    "${MMPAY_BASE_URL%/}/api/admin/audit/verify${query}"
)"

printf '%s\n' "$response"
RESPONSE="$response" node <<'NODE'
const response = JSON.parse(process.env.RESPONSE);
if (response.ok !== true) {
  console.error('audit chain verification failed');
  process.exit(1);
}
NODE

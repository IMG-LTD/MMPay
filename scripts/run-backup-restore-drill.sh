#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

usage() {
  cat <<EOF >&2
Usage: $0 [options]

Performs a real Postgres backup-restore round-trip and emits canonical drill
facts. Two-stage signing (Ed25519 + PGP) is applied by sign-backup-restore-drill-evidence.sh.

Environment:
  MMPAY_DRILL_TAG                Required. v1.0.0 or v1.0.0-rc.N
  MMPAY_DRILL_SOURCE_PG          Source Postgres container name. Default: deploy-postgres-1
  MMPAY_DRILL_SOURCE_DB          Source database. Default: mmpay
  MMPAY_DRILL_SOURCE_USER        Source user. Default: mmpay
  MMPAY_DRILL_SOURCE_PASSWORD    Source password. Default: replace-with-local-password
  MMPAY_DRILL_TARGET_DB          Target database name. Default: mmpay_drill_<unix>
  MMPAY_DRILL_E2E_EVIDENCE_FILE  Required. Path to the GA e2e evidence file
  MMPAY_DRILL_OUTPUT_DIR         Where the backup tarball + canonical land. Default: drill-out/<timestamp>

Outputs (relative to MMPAY_DRILL_OUTPUT_DIR):
  backup.sql.gz   pg_dump custom-format archive that was actually restored
  drill.json      Canonical, deterministic drill JSON ready for signing
EOF
  exit 2
}

if [[ ${1:-} == "--help" || ${1:-} == "-h" ]]; then
  usage
fi

require_env() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    echo "Missing required env: $name" >&2
    exit 1
  fi
}

require_env MMPAY_DRILL_TAG
require_env MMPAY_DRILL_E2E_EVIDENCE_FILE

if [[ ! -f "$MMPAY_DRILL_E2E_EVIDENCE_FILE" ]]; then
  echo "E2E evidence file does not exist: $MMPAY_DRILL_E2E_EVIDENCE_FILE" >&2
  exit 1
fi

SOURCE_PG="${MMPAY_DRILL_SOURCE_PG:-deploy-postgres-1}"
SOURCE_DB="${MMPAY_DRILL_SOURCE_DB:-mmpay}"
SOURCE_USER="${MMPAY_DRILL_SOURCE_USER:-mmpay}"
SOURCE_PASSWORD="${MMPAY_DRILL_SOURCE_PASSWORD:-replace-with-local-password}"
TARGET_DB="${MMPAY_DRILL_TARGET_DB:-mmpay_drill_$(date +%s)}"
OUTPUT_DIR="${MMPAY_DRILL_OUTPUT_DIR:-$ROOT_DIR/drill-out/$(date +%Y%m%dT%H%M%SZ)}"

mkdir -p "$OUTPUT_DIR"
BACKUP_FILE="$OUTPUT_DIR/backup.sql.gz"
CANONICAL_FILE="$OUTPUT_DIR/drill.json"

if ! docker exec "$SOURCE_PG" pg_isready -U "$SOURCE_USER" -d "$SOURCE_DB" >/dev/null 2>&1; then
  echo "Source Postgres ($SOURCE_PG / $SOURCE_DB) is not reachable" >&2
  exit 1
fi

drill_id="$(uuidgen | tr '[:upper:]' '[:lower:]')"
restore_nonce="$(uuidgen | tr '[:upper:]' '[:lower:]')"
commit_sha="$(git -C "$ROOT_DIR" rev-parse HEAD)"

drill_seed_table="mmpay_drill_witness_${drill_id//-/_}"
echo "Seeding drill witness row in source DB: $drill_seed_table"
docker exec -i -e PGPASSWORD="$SOURCE_PASSWORD" "$SOURCE_PG" psql -U "$SOURCE_USER" -d "$SOURCE_DB" -v ON_ERROR_STOP=1 <<SQL
CREATE TABLE IF NOT EXISTS $drill_seed_table (
  id uuid PRIMARY KEY,
  nonce uuid NOT NULL,
  sealed_at timestamptz NOT NULL DEFAULT now()
);
INSERT INTO $drill_seed_table (id, nonce) VALUES ('$drill_id', '$restore_nonce');
SQL

echo "Dumping source DB into $BACKUP_FILE"
start_epoch=$(date -u +%s)
start_iso=$(date -u +%Y-%m-%dT%H:%M:%SZ)
docker exec -e PGPASSWORD="$SOURCE_PASSWORD" "$SOURCE_PG" pg_dump -U "$SOURCE_USER" -d "$SOURCE_DB" --format=plain --no-owner --no-acl \
  | gzip -n -9 > "$BACKUP_FILE"

echo "Provisioning target DB $TARGET_DB and restoring"
docker exec -e PGPASSWORD="$SOURCE_PASSWORD" "$SOURCE_PG" psql -U "$SOURCE_USER" -d postgres -v ON_ERROR_STOP=1 -c "DROP DATABASE IF EXISTS $TARGET_DB" >/dev/null
docker exec -e PGPASSWORD="$SOURCE_PASSWORD" "$SOURCE_PG" psql -U "$SOURCE_USER" -d postgres -v ON_ERROR_STOP=1 -c "CREATE DATABASE $TARGET_DB OWNER $SOURCE_USER" >/dev/null
gunzip -c "$BACKUP_FILE" | docker exec -i -e PGPASSWORD="$SOURCE_PASSWORD" "$SOURCE_PG" psql -U "$SOURCE_USER" -d "$TARGET_DB" -v ON_ERROR_STOP=1 >/dev/null

echo "Verifying drill witness row survived the restore"
restored=$(docker exec -e PGPASSWORD="$SOURCE_PASSWORD" "$SOURCE_PG" psql -U "$SOURCE_USER" -d "$TARGET_DB" -tAc "SELECT nonce::text FROM $drill_seed_table WHERE id = '$drill_id'" | tr -d '[:space:]')
if [[ "$restored" != "$restore_nonce" ]]; then
  echo "Restore witness mismatch: expected $restore_nonce, got '$restored'" >&2
  exit 1
fi

echo "Tearing down target DB $TARGET_DB"
docker exec -e PGPASSWORD="$SOURCE_PASSWORD" "$SOURCE_PG" psql -U "$SOURCE_USER" -d postgres -v ON_ERROR_STOP=1 -c "DROP DATABASE $TARGET_DB" >/dev/null
docker exec -e PGPASSWORD="$SOURCE_PASSWORD" "$SOURCE_PG" psql -U "$SOURCE_USER" -d "$SOURCE_DB" -v ON_ERROR_STOP=1 -c "DROP TABLE IF EXISTS $drill_seed_table" >/dev/null

end_epoch=$(date -u +%s)
completed_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)
elapsed_seconds=$(( end_epoch - start_epoch ))
rto_minutes=$(( (elapsed_seconds + 59) / 60 ))
if (( rto_minutes < 1 )); then
  rto_minutes=1
fi

backup_sha=$(sha256sum "$BACKUP_FILE" | awk '{print $1}')
e2e_sha=$(sha256sum "$MMPAY_DRILL_E2E_EVIDENCE_FILE" | awk '{print $1}')
operator_fp=$(awk '/^  - fingerprint:/ {print $3; exit}' "$ROOT_DIR/governance/operator-keys.yaml")
evidence_fp=$(awk '/^    evidence_key_fingerprint:/ {print $2; exit}' "$ROOT_DIR/governance/operator-keys.yaml")

mkdir -p "$ROOT_DIR/governance/restore-nonces"
nonce_file="$ROOT_DIR/governance/restore-nonces/${restore_nonce}.txt"
cat > "$nonce_file" <<EOF
restore_nonce: $restore_nonce
drill_id: $drill_id
sealed_at: $completed_at
EOF

python3 - <<PY > "$CANONICAL_FILE"
import json
from collections import OrderedDict
fields = OrderedDict([
    ("backup_file_sha256", "$backup_sha"),
    ("completed_at", "$completed_at"),
    ("commit_sha", "$commit_sha"),
    ("drill_id", "$drill_id"),
    ("e2e_evidence_sha256", "$e2e_sha"),
    ("evidence_key_fingerprint", "$evidence_fp"),
    ("operator_key_fingerprint", "$operator_fp"),
    ("restore_nonce", "$restore_nonce"),
    ("rto_minutes", $rto_minutes),
    ("tag", "$MMPAY_DRILL_TAG"),
])
print(json.dumps(fields, separators=(",", ":")), end="")
PY

echo "drill_id: $drill_id"
echo "restore_nonce: $restore_nonce"
echo "started_at: $start_iso"
echo "completed_at: $completed_at"
echo "rto_minutes: $rto_minutes"
echo "backup_sha256: $backup_sha"
echo "e2e_sha256: $e2e_sha"
echo "operator_key_fingerprint: $operator_fp"
echo "evidence_key_fingerprint: $evidence_fp"
echo "canonical: $CANONICAL_FILE"
echo "backup: $BACKUP_FILE"
echo "nonce_file: $nonce_file"

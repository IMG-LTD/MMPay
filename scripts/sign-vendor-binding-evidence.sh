#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

usage() {
  cat <<EOF >&2
Usage: $0 <vendor-pgp-fingerprint>

Computes sha256 over the three GA evidence files (image-digest, e2e,
backup-restore drill), assembles the canonical vendor-binding JSON in the
field order required by scripts/governance/verify-vendor-binding-evidence.mjs,
PGP-signs it with the supplied vendor key, and writes
docs/release/vendor-binding/v1.0.0-BINDING_OK.asc.

Environment overrides for evidence file paths:
  MMPAY_IMAGE_DIGEST_EVIDENCE_FILE  default docs/release/v1.0.0-image-digest-evidence.md
  MMPAY_E2E_EVIDENCE_FILE           default docs/release/v1.0.0-e2e-evidence.md
  MMPAY_DRILL_EVIDENCE_FILE         default docs/release/backup-restore-drill-evidence.md

The supplied fingerprint must match the binding-sign vendor key pinned in
governance/vendor-keys.yaml. The signing key must be available to the gpg
agent (or the GNUPGHOME env var).
EOF
  exit 2
}

if [[ $# -ne 1 ]]; then
  usage
fi

VENDOR_FP="$1"

PINNED_FP=$(awk '/^  - fingerprint:/ {print $3; exit}' "$ROOT_DIR/governance/vendor-keys.yaml")
if [[ "$VENDOR_FP" != "$PINNED_FP" ]]; then
  echo "vendor fingerprint $VENDOR_FP does not match pinned $PINNED_FP" >&2
  exit 1
fi

IMG_FILE="${MMPAY_IMAGE_DIGEST_EVIDENCE_FILE:-$ROOT_DIR/docs/release/v1.0.0-image-digest-evidence.md}"
E2E_FILE="${MMPAY_E2E_EVIDENCE_FILE:-$ROOT_DIR/docs/release/v1.0.0-e2e-evidence.md}"
DRILL_FILE="${MMPAY_DRILL_EVIDENCE_FILE:-$ROOT_DIR/docs/release/backup-restore-drill-evidence.md}"

for f in "$IMG_FILE" "$E2E_FILE" "$DRILL_FILE"; do
  [[ -f "$f" ]] || { echo "missing evidence file: $f" >&2; exit 1; }
done

img_sha=$(sha256sum "$IMG_FILE" | awk '{print $1}')
e2e_sha=$(sha256sum "$E2E_FILE" | awk '{print $1}')
drill_sha=$(sha256sum "$DRILL_FILE" | awk '{print $1}')
commit_sha=$(git -C "$ROOT_DIR" rev-parse HEAD)
signed_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)
tag="v1.0.0-rc.3"
if git -C "$ROOT_DIR" describe --tags --exact-match HEAD >/dev/null 2>&1; then
  tag=$(git -C "$ROOT_DIR" describe --tags --exact-match HEAD)
fi

WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT
CANONICAL="$WORKDIR/binding.json"
SIGNATURE="$WORKDIR/binding.sig.asc"

python3 - <<PY > "$CANONICAL"
import json
from collections import OrderedDict
fields = OrderedDict([
    ("commit_sha", "$commit_sha"),
    ("drill_evidence_sha256", "$drill_sha"),
    ("e2e_evidence_sha256", "$e2e_sha"),
    ("image_digest_evidence_sha256", "$img_sha"),
    ("signed_at", "$signed_at"),
    ("tag", "$tag"),
    ("vendor_key_fingerprint", "$VENDOR_FP"),
])
print(json.dumps(fields, separators=(",", ":")), end="")
PY

gpg --batch --yes --local-user "$VENDOR_FP" --armor --detach-sign --output "$SIGNATURE" "$CANONICAL"

OUT="$ROOT_DIR/docs/release/vendor-binding/v1.0.0-BINDING_OK.asc"
mkdir -p "$(dirname "$OUT")"
canonical_body=$(cat "$CANONICAL")
sig_body=$(cat "$SIGNATURE")

cat > "$OUT" <<EOF
# MMPay v1.0.0 Vendor Binding Evidence

Status: completed-external-evidence

This file binds the GA evidence stack (image-digest, e2e, backup-restore drill)
under a single PGP signature from the binding-sign vendor key pinned in
governance/vendor-keys.yaml.

-----BEGIN MMPAY VENDOR BINDING-----
$canonical_body
-----END MMPAY VENDOR BINDING-----

$sig_body
EOF

echo "Wrote $OUT"
echo "tag: $tag"
echo "signed_at: $signed_at"
echo "commit_sha: $commit_sha"
echo "image_digest_evidence_sha256: $img_sha"
echo "e2e_evidence_sha256: $e2e_sha"
echo "drill_evidence_sha256: $drill_sha"

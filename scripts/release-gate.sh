#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODE="auto"

if [[ $# -gt 1 ]]; then
  echo "Usage: $0 [--rc|--ga]" >&2
  exit 2
fi

if [[ $# -eq 1 ]]; then
  case "$1" in
    --rc) MODE="rc" ;;
    --ga) MODE="ga" ;;
    *) echo "unknown release gate mode: $1" >&2; exit 2 ;;
  esac
fi

if [[ -n "$(git -C "$ROOT_DIR" status --short)" ]]; then
  echo "release gate requires a clean worktree" >&2
  git -C "$ROOT_DIR" status --short >&2
  exit 1
fi

bash "$ROOT_DIR/scripts/validate-ci.sh"

tag_name() {
  git -C "$ROOT_DIR" describe --tags --exact-match HEAD 2>/dev/null || true
}

detect_mode() {
  local tag="$1"
  if [[ "$MODE" != "auto" ]]; then
    printf '%s' "$MODE"
    return
  fi
  if [[ "$tag" == "v1.0.0" ]]; then
    printf 'ga'
    return
  fi
  if [[ "$tag" =~ ^v1\.0\.0-rc\.[0-9]+$ ]]; then
    printf 'rc'
    return
  fi
  printf 'preview'
}

check_rc_cadence() {
  local tag="$1"
  if [[ ! "$tag" =~ ^v1\.0\.0-rc\.[0-9]+$ ]]; then
    echo "rc gate requires a v1.0.0-rc.N tag on HEAD" >&2
    exit 1
  fi
  if git -C "$ROOT_DIR" tag --points-at HEAD | grep -Evqx "$tag"; then
    echo "rc gate requires a single RC tag on HEAD" >&2
    exit 1
  fi
  grep -Fq "fresh-rc per change" "$ROOT_DIR/docs/release/rc-cadence.md"
  grep -Fq "evidence-refresh-no-op" "$ROOT_DIR/docs/release/rc-cadence.md"
}

require_file() {
  local path="$1"
  if [[ ! -f "$ROOT_DIR/$path" ]]; then
    echo "missing required GA artifact: $path" >&2
    exit 1
  fi
}

check_vendor_binding() {
  require_file "docs/release/vendor-binding/v1.0.0-BINDING_OK.asc"
  bash "$ROOT_DIR/scripts/governance/verify-vendor-binding-evidence.sh" \
    "$ROOT_DIR/docs/release/vendor-binding/v1.0.0-BINDING_OK.asc"
}

check_ga_promotion() {
  require_file "docs/release/v1.0.0-image-digest-evidence.md"
  require_file "docs/release/v1.0.0-e2e-evidence.md"
  require_file "docs/release/backup-restore-drill-evidence.md"
  require_file "docs/release/v1.0.0-v1-tag-ruleset-evidence.md"
  require_file "docs/release/v1.0.0-release-notes.md"
  bash "$ROOT_DIR/scripts/governance/verify-image-digest-evidence.sh" \
    "$ROOT_DIR/docs/release/v1.0.0-image-digest-evidence.md"
  bash "$ROOT_DIR/scripts/validate-e2e-evidence.sh" \
    "$ROOT_DIR/docs/release/v1.0.0-e2e-evidence.md"
  bash "$ROOT_DIR/scripts/governance/verify-v1-tag-ruleset-evidence.sh" \
    "$ROOT_DIR/docs/release/v1.0.0-v1-tag-ruleset-evidence.md"
  grep -Fq "Evidence status: completed-external-evidence" \
    "$ROOT_DIR/docs/release/backup-restore-drill-evidence.md" \
    || { echo "backup restore drill evidence is not complete" >&2; exit 1; }
  grep -Fq "Operator PGP signature: valid" \
    "$ROOT_DIR/docs/release/backup-restore-drill-evidence.md" \
    || { echo "backup restore drill evidence lacks operator PGP proof" >&2; exit 1; }
  check_vendor_binding
}

tag="$(tag_name)"
gate_mode="$(detect_mode "$tag")"

case "$gate_mode" in
  preview) ;;
  rc) check_rc_cadence "$tag" ;;
  ga) check_ga_promotion ;;
  *) echo "unknown computed release gate mode: $gate_mode" >&2; exit 2 ;;
esac

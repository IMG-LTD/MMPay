#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ -n "$(git -C "$ROOT_DIR" status --short)" ]]; then
  echo "release gate requires a clean worktree" >&2
  git -C "$ROOT_DIR" status --short >&2
  exit 1
fi

bash "$ROOT_DIR/scripts/validate-ci.sh"

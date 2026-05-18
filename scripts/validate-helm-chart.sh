#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CHART_DIR="$ROOT_DIR/deploy/helm/mmpay"

require_file() {
  local file="$1"
  if [[ ! -f "$CHART_DIR/$file" ]]; then
    echo "missing Helm chart file: deploy/helm/mmpay/$file" >&2
    exit 1
  fi
}

require_pattern() {
  local file="$1"
  local pattern="$2"
  local message="$3"
  if ! grep -Eq "$pattern" "$CHART_DIR/$file"; then
    echo "$message" >&2
    exit 1
  fi
}

reject_pattern() {
  local file="$1"
  local pattern="$2"
  local message="$3"
  if grep -Eq "$pattern" "$CHART_DIR/$file"; then
    echo "$message" >&2
    exit 1
  fi
}

require_file Chart.yaml
require_file values.yaml
require_file templates/_helpers.tpl
require_file templates/configmap.yaml
require_file templates/deployment.yaml
require_file templates/service.yaml
require_file templates/serviceaccount.yaml

require_pattern Chart.yaml '^apiVersion: v2$' "Helm chart must use apiVersion v2"
require_pattern Chart.yaml '^name: mmpay$' "Helm chart must be named mmpay"
require_pattern values.yaml 'repository: ghcr\.io/img-ltd/mmpay-app' "Helm chart must deploy the MMPay app image"
require_pattern values.yaml 'existingSecret: "replace-with-mmpay-secret"' "Helm values must use an external secret placeholder"
require_pattern templates/deployment.yaml 'secretKeyRef:' "Deployment must read runtime secrets from Kubernetes Secret refs"
require_pattern templates/deployment.yaml 'MMPAY_HUIFU_API_KEY' "Deployment must expose Huifu API key through env from secret"
require_pattern templates/deployment.yaml 'readOnlyRootFilesystem: true|toYaml \.Values\.securityContext' "Deployment must wire container security context"
require_pattern values.yaml '/actuator/health/readiness' "Helm values must define readiness probe path"
require_pattern values.yaml '/actuator/health/liveness' "Helm values must define liveness probe path"

reject_pattern values.yaml 'apiKey: "[^"]+"' "Helm values must not contain plaintext provider API keys"
reject_pattern values.yaml 'password: "[^"]+"' "Helm values must not contain plaintext passwords"
reject_pattern templates/deployment.yaml 'value: .*api-key' "Deployment must not inline provider API key values"

echo "mmpay helm chart static validation passed"

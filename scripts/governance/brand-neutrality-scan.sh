#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

node --input-type=module - "$ROOT_DIR" <<'NODE'
import { readdirSync, readFileSync, statSync } from 'node:fs';
import path from 'node:path';

const root = process.argv[2];
const forbidden = [/MMMail/i, /Soybean/i];
const allowed = [
  /^LICENSE$/,
  /^NOTICE$/,
  /^README\.md$/,
  /^frontend-admin\/LICENSE$/,
  /^frontend-admin\/UPSTREAM\.md$/,
  /^frontend-admin\/package\.json$/,
  /^frontend-admin\/pnpm-lock\.yaml$/,
  /^frontend-admin\/uno\.config\.ts$/,
  /^frontend-admin\/eslint\.config\.js$/,
  /^frontend-admin\/packages\//,
  /^docs\/architecture\//,
  /^docs\/commercial\//,
  /^docs\/integrations\//,
  /^docs\/ops\//,
  /^docs\/release\//,
  /^governance\/brand-allowlist\.yaml$/,
];
const scanRoots = ['frontend-admin/src', 'frontend-admin/package.json', 'frontend-admin/.env', 'docs', 'README.md'];
const hits = [];

for (const entry of scanRoots) {
  collect(path.join(root, entry));
}

if (hits.length > 0) {
  console.error(hits.join('\n'));
  process.exit(1);
}
console.log('brand-neutrality scan passed');

function collect(filePath) {
  const relative = path.relative(root, filePath).replaceAll(path.sep, '/');
  if (allowed.some(pattern => pattern.test(relative))) return;
  const stat = statSync(filePath);
  if (stat.isDirectory()) {
    for (const child of readdirSync(filePath)) collect(path.join(filePath, child));
    return;
  }
  if (!/\.(vue|ts|js|json|md|env)$/.test(relative) && !relative.endsWith('package.json')) return;
  const normalized = readFileSync(filePath, 'utf8').normalize('NFKC');
  for (const pattern of forbidden) {
    if (pattern.test(normalized)) hits.push(`${relative}: forbidden brand literal ${pattern}`);
  }
}
NODE

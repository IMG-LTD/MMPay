#!/usr/bin/env bash
set -euo pipefail
export LC_ALL=C

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

node --input-type=module - "$ROOT_DIR" <<'NODE'
import { readdirSync, readFileSync, statSync } from 'node:fs';
import path from 'node:path';

const root = process.argv[2];
const forbidden = [/MMMail/i, /Soybean/i];

// Read allowlist from governance/brand-allowlist.yaml — single source of truth (spec §1.1.9 /
// §8.6). YAML format:
//   literal_categories:
//     <name>:
//       paths: [ <pattern>, ... ]
//       rationale: <text>
const yamlPath = path.join(root, 'governance/brand-allowlist.yaml');
const yamlText = readFileSync(yamlPath, 'utf8');
const allowedPatterns = parseAllowlist(yamlText);

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
  if (allowedPatterns.some(pattern => pattern.test(relative))) return;
  let stat;
  try { stat = statSync(filePath); } catch { return; }
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

// Minimal YAML parser tuned to the brand-allowlist.yaml shape; no external dependency. Reads
// the `paths:` lists under literal_categories and treats each entry as a glob-to-regex pattern.
function parseAllowlist(text) {
  const patterns = [];
  // Always allow the allowlist file itself, plus a couple of canonical attribution roots.
  patterns.push(/^governance\/brand-allowlist\.yaml$/);
  const lines = text.split(/\r?\n/);
  let inPaths = false;
  for (const line of lines) {
    if (/^\s*paths:\s*$/.test(line)) { inPaths = true; continue; }
    if (inPaths) {
      const m = line.match(/^\s*-\s*(.+?)\s*$/);
      if (m) {
        patterns.push(globToRegex(m[1]));
      } else if (!/^\s+/.test(line)) {
        inPaths = false;
      } else if (/^\s*[a-zA-Z_]+:\s*/.test(line)) {
        inPaths = false;
      }
    }
  }
  return patterns;
}

function globToRegex(glob) {
  // Translate the small glob set used in the YAML:
  //   trailing /**       → "/.*"
  //   '*' inside a segment → "[^/]*"
  //   literal chars (path separators, dots) escaped
  const escaped = glob
    .replace(/[.+^${}()|]/g, '\\$&')
    .replace(/\/\*\*/g, '/.*')
    .replace(/\*/g, '[^/]*');
  return new RegExp('^' + escaped + '$');
}
NODE
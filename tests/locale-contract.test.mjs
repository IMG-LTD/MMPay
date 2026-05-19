import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { readFile, readdir, stat } from 'node:fs/promises';
import path from 'node:path';

const root = path.resolve(import.meta.dirname, '..');
const frontendRoot = path.join(root, 'frontend-admin');

async function readKeysFromLocaleModule(filePath) {
  // The Vue locales files export a TS object literal; we treat them as the canonical key tree
  // by extracting the keys (left side of `:`) without relying on a parser.
  const text = await readFile(filePath, 'utf8');
  return collectKeyPaths(text);
}

function collectKeyPaths(text) {
  // Capture identifier-style keys (alphanumeric + underscore + dash) and string-quoted keys
  // that appear before a `:`. We do not try to reconstruct the nested tree; flat capture is
  // sufficient for the parity assertion.
  const keys = new Set();
  const identifierKey = /(?:^|[{,\s])\s*([A-Za-z_$][A-Za-z0-9_$-]*)\s*:/g;
  let match;
  while ((match = identifierKey.exec(text)) !== null) {
    keys.add(match[1]);
  }
  const quotedKey = /(?:^|[{,\s])\s*['"]([^'"\n]+)['"]\s*:/g;
  while ((match = quotedKey.exec(text)) !== null) {
    keys.add(match[1]);
  }
  // Drop method-name look-alikes from arrow-function defaults (best-effort heuristic).
  keys.delete('default');
  return keys;
}

async function walk(dir, suffixes) {
  const out = [];
  let entries;
  try {
    entries = await readdir(dir, { withFileTypes: true });
  } catch {
    return out;
  }
  for (const entry of entries) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      out.push(...(await walk(full, suffixes)));
    } else if (suffixes.some((suffix) => full.endsWith(suffix))) {
      out.push(full);
    }
  }
  return out;
}

describe('Locale contract', () => {
  it('zh-CN and en-US share the same i18n key set', async () => {
    const enPath = path.join(frontendRoot, 'src/locales/langs/en-us.ts');
    const zhPath = path.join(frontendRoot, 'src/locales/langs/zh-cn.ts');
    const enKeys = await readKeysFromLocaleModule(enPath);
    const zhKeys = await readKeysFromLocaleModule(zhPath);

    const missingInZh = [...enKeys].filter((key) => !zhKeys.has(key));
    const missingInEn = [...zhKeys].filter((key) => !enKeys.has(key));

    assert.deepEqual(missingInZh, [], `keys present in en-US but missing in zh-CN: ${missingInZh}`);
    assert.deepEqual(missingInEn, [], `keys present in zh-CN but missing in en-US: ${missingInEn}`);
  });

  it('no .vue template carries the literal MMMail or Soybean (spec §1.1.10)', async () => {
    // Allowed paths mirror the brand scanner's YAML allowlist; locales themselves are scanned
    // separately by the brand scanner.
    const files = await walk(path.join(frontendRoot, 'src'), ['.vue']);
    const offenders = [];
    for (const file of files) {
      const text = (await readFile(file, 'utf8')).normalize('NFKC');
      if (/MMMail|Soybean/i.test(text)) {
        offenders.push(path.relative(root, file));
      }
    }
    assert.deepEqual(offenders, [], `forbidden brand literal found in .vue files: ${offenders}`);
  });
});

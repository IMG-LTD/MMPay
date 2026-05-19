#!/usr/bin/env node
import { execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { existsSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, isAbsolute, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const root = join(scriptDir, '..', '..');
const expectedKeys = [
  'commit_sha',
  'drill_evidence_sha256',
  'e2e_evidence_sha256',
  'image_digest_evidence_sha256',
  'signed_at',
  'tag',
  'vendor_key_fingerprint'
];

main();

function main() {
  const bindingFile = process.argv[2];
  if (!bindingFile || process.argv.length !== 3) {
    fail('Usage: verify-vendor-binding-evidence.mjs <v1.0.0-BINDING_OK.asc>');
  }

  const bindingText = readRequired(bindingFile, 'vendor binding evidence missing');
  rejectPlaceholders(bindingText, 'vendor binding evidence');
  const canonical = extractBlock(bindingText, 'MMPAY VENDOR BINDING');
  const signature = extractPgpSignature(bindingText);
  const binding = parseCanonicalJson(canonical);
  const vendorKeys = parseVendorKeys();
  const vendorKey = findVendorKey(vendorKeys, binding.vendor_key_fingerprint);

  verifyBindingFields(binding);
  verifyEvidenceHashes(binding);
  verifyVendorKey(vendorKey, binding.signed_at);
  verifyRevocationFeed(vendorKey.revocation_feed_url, binding.vendor_key_fingerprint);
  verifyDetachedSignature({ canonical, signature, vendorKey });

  console.log('vendor binding evidence verified');
}

function readRequired(path, missingMessage) {
  if (!existsSync(path)) {
    fail(`${missingMessage}: ${path}`);
  }
  return readFileSync(path, 'utf8');
}

function rejectPlaceholders(text, label) {
  const forbidden = /mock|dummy|fake|replace-with|required-before-ga|TBD|TODO|FIXME|XXX|<[^>]+>/i;
  if (forbidden.test(text)) {
    fail(`${label} contains placeholder or fake markers`);
  }
}

function extractBlock(text, blockName) {
  const pattern = new RegExp(`-----BEGIN ${blockName}-----\\n([\\s\\S]+?)\\n-----END ${blockName}-----`);
  const match = text.match(pattern);
  if (!match) {
    fail(`missing ${blockName} block`);
  }
  return match[1].trim();
}

function extractPgpSignature(text) {
  const match = text.match(/-----BEGIN PGP SIGNATURE-----[\s\S]+?-----END PGP SIGNATURE-----/);
  if (!match) {
    fail('missing detached PGP signature block');
  }
  return match[0];
}

function parseCanonicalJson(canonical) {
  let parsed;
  try {
    parsed = JSON.parse(canonical);
  } catch {
    fail('vendor binding canonical content is not valid JSON');
  }
  const actualKeys = Object.keys(parsed);
  if (actualKeys.join(',') !== expectedKeys.join(',')) {
    fail('vendor binding canonical keys are not sorted or complete');
  }
  if (JSON.stringify(parsed) !== canonical) {
    fail('vendor binding canonical JSON must be minified deterministic JSON');
  }
  return parsed;
}

function parseVendorKeys() {
  const path = process.env.MMPAY_VENDOR_KEYS_FILE ?? join(root, 'governance/vendor-keys.yaml');
  const yaml = readRequired(path, 'vendor keys file missing');
  rejectPlaceholders(yaml, 'vendor keys file');
  const entries = [];
  let current;
  for (const line of yaml.split('\n')) {
    if (line.startsWith('  - ')) {
      current = {};
      entries.push(current);
      assignYamlField(current, line.slice(4));
      continue;
    }
    if (current && line.startsWith('    ')) {
      assignYamlField(current, line.slice(4));
    }
  }
  return entries;
}

function assignYamlField(target, text) {
  const separator = text.indexOf(':');
  if (separator === -1) {
    return;
  }
  const key = text.slice(0, separator).trim();
  const value = text.slice(separator + 1).trim();
  target[key] = value;
}

function findVendorKey(vendorKeys, fingerprint) {
  const key = vendorKeys.find(entry => entry.fingerprint === fingerprint);
  if (!key) {
    fail('vendor_key_fingerprint is not pinned in governance/vendor-keys.yaml');
  }
  return key;
}

function verifyBindingFields(binding) {
  if (!/^v1\.0\.0(-rc\.[0-9]+)?$/.test(binding.tag)) {
    fail('vendor binding tag must be v1.0.0 or v1.0.0-rc.N');
  }
  if (!/^[0-9a-f]{40}$/.test(binding.commit_sha)) {
    fail('vendor binding commit_sha must be a 40-char lowercase hex SHA');
  }
  for (const name of expectedKeys.filter(key => key.endsWith('_sha256'))) {
    if (!/^[0-9a-f]{64}$/.test(binding[name])) {
      fail(`${name} must be a lowercase sha256 hex digest`);
    }
  }
  if (Number.isNaN(Date.parse(binding.signed_at))) {
    fail('vendor binding signed_at must be an ISO timestamp');
  }
}

function verifyEvidenceHashes(binding) {
  const evidence = {
    image_digest_evidence_sha256: evidencePath('MMPAY_IMAGE_DIGEST_EVIDENCE_FILE', 'docs/release/v1.0.0-image-digest-evidence.md'),
    e2e_evidence_sha256: evidencePath('MMPAY_E2E_EVIDENCE_FILE', 'docs/release/v1.0.0-e2e-evidence.md'),
    drill_evidence_sha256: evidencePath('MMPAY_DRILL_EVIDENCE_FILE', 'docs/release/backup-restore-drill-evidence.md')
  };
  for (const [field, file] of Object.entries(evidence)) {
    const actual = sha256(file);
    if (binding[field] !== actual) {
      fail(`${field} does not match ${file}`);
    }
  }
}

function evidencePath(envName, defaultPath) {
  return process.env[envName] ?? join(root, defaultPath);
}

function verifyVendorKey(vendorKey, signedAt) {
  for (const field of ['fingerprint', 'key_id', 'role', 'valid_from', 'valid_to', 'rotation_window_days', 'public_key_path', 'signed_by_root', 'revocation_feed_url', 'signing_sla_business_days', 'sub_keys']) {
    if (!vendorKey[field]) {
      fail(`vendor key missing required field: ${field}`);
    }
  }
  if (vendorKey.role !== 'binding-sign') {
    fail('vendor key role must be binding-sign');
  }
  if (!/^[1-9][0-9]*$/.test(vendorKey.signing_sla_business_days)) {
    fail('vendor signing_sla_business_days must be a positive integer');
  }
  if (!/^[1-9][0-9]*$/.test(vendorKey.rotation_window_days)) {
    fail('vendor rotation_window_days must be a positive integer');
  }
  const signedAtMs = Date.parse(signedAt);
  if (Date.parse(`${vendorKey.valid_from}T00:00:00Z`) > signedAtMs) {
    fail('vendor key is not valid yet at signed_at');
  }
  if (Date.parse(`${vendorKey.valid_to}T23:59:59Z`) < signedAtMs) {
    fail('vendor key is expired at signed_at');
  }
}

function verifyRevocationFeed(url, fingerprint) {
  let feed;
  if (url.startsWith('file://')) {
    feed = readRequired(url.slice('file://'.length), 'vendor revocation feed missing');
  } else {
    feed = execFileSync('curl', ['--fail', '--silent', '--show-error', '--max-time', '10', url], { encoding: 'utf8' });
  }
  if (feed.includes(fingerprint)) {
    fail('vendor key fingerprint is present in the revocation feed');
  }
}

function verifyDetachedSignature(options) {
  const publicKeyPath = resolvePath(options.vendorKey.public_key_path);
  readRequired(publicKeyPath, 'vendor public key missing');
  const workspace = mkdtempSync(join(tmpdir(), 'mmpay-vendor-verify-'));
  try {
    const gnupgHome = join(workspace, 'gnupg');
    execFileSync('mkdir', ['-p', gnupgHome]);
    execFileSync('chmod', ['700', gnupgHome]);
    const canonicalPath = join(workspace, 'binding.json');
    const signaturePath = join(workspace, 'binding.sig.asc');
    writeFileSync(canonicalPath, options.canonical);
    writeFileSync(signaturePath, options.signature);
    execFileSync('gpg', ['--batch', '--homedir', gnupgHome, '--import', publicKeyPath], { stdio: 'pipe' });
    const status = execFileSync('gpg', ['--batch', '--homedir', gnupgHome, '--status-fd', '1', '--verify', signaturePath, canonicalPath], { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] });
    if (!status.includes(`VALIDSIG ${options.vendorKey.fingerprint}`)) {
      fail('vendor PGP signature does not match the pinned fingerprint');
    }
  } finally {
    rmSync(workspace, { recursive: true, force: true });
  }
}

function resolvePath(path) {
  return isAbsolute(path) ? path : join(root, path);
}

function sha256(path) {
  return createHash('sha256').update(readFileSync(path)).digest('hex');
}

function fail(message) {
  console.error(message);
  process.exit(1);
}

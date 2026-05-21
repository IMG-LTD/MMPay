#!/usr/bin/env node
import { execFileSync } from 'node:child_process';
import { createHash, createPublicKey, verify as verifySignature } from 'node:crypto';
import { existsSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, isAbsolute, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const root = join(scriptDir, '..', '..');
const maxDrillAgeMs = 90 * 24 * 60 * 60 * 1000;
const maxFutureSkewMs = 5 * 60 * 1000;
const maxRtoMinutes = 8 * 60;
const expectedKeys = [
  'backup_file_sha256',
  'completed_at',
  'commit_sha',
  'drill_id',
  'e2e_evidence_sha256',
  'evidence_key_fingerprint',
  'operator_key_fingerprint',
  'restore_nonce',
  'rto_minutes',
  'tag'
];

main();

function main() {
  const evidenceFile = process.argv[2];
  if (!evidenceFile || process.argv.length !== 3) {
    fail('Usage: verify-backup-restore-drill-evidence.mjs <backup-restore-drill-evidence.md>');
  }

  const evidenceText = readRequired(evidenceFile, 'backup restore drill evidence missing');
  const canonical = extractBlock(evidenceText, 'MMPAY BACKUP RESTORE DRILL');
  const ed25519Signature = extractBlock(evidenceText, 'MMPAY DRILL ED25519 SIGNATURE');
  const pgpSignature = extractPgpSignature(evidenceText);
  rejectPlaceholders(stripSignatureBlocks(evidenceText), 'backup restore drill evidence');
  rejectPlaceholders(canonical, 'backup restore drill canonical payload');
  const evidence = parseCanonicalJson(canonical);
  const operatorKey = findOperatorKey(parseOperatorKeys(), evidence.operator_key_fingerprint);

  verifyEvidenceFields(evidence);
  verifyEvidenceFreshness(evidence.completed_at);
  verifyExternalHashes(evidence);
  verifyRestoreNonce(evidence.restore_nonce);
  verifyOperatorKey(operatorKey, evidence);
  verifyEd25519Signature({ canonical, signature: ed25519Signature, operatorKey });
  verifyPgpSignature({ canonical, signature: pgpSignature, operatorKey });

  console.log('backup restore drill evidence verified');
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

function stripSignatureBlocks(text) {
  return text
    .replace(
      /-----BEGIN MMPAY DRILL ED25519 SIGNATURE-----[\s\S]+?-----END MMPAY DRILL ED25519 SIGNATURE-----/g,
      '-----MMPAY DRILL ED25519 SIGNATURE REDACTED-----'
    )
    .replace(
      /-----BEGIN PGP SIGNATURE-----[\s\S]+?-----END PGP SIGNATURE-----/g,
      '-----PGP SIGNATURE REDACTED-----'
    );
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
    fail('missing operator PGP detached signature block');
  }
  return match[0];
}

function parseCanonicalJson(canonical) {
  let parsed;
  try {
    parsed = JSON.parse(canonical);
  } catch {
    fail('backup restore drill canonical content is not valid JSON');
  }
  if (Object.keys(parsed).join(',') !== expectedKeys.join(',')) {
    fail('backup restore drill canonical keys are not sorted or complete');
  }
  if (JSON.stringify(parsed) !== canonical) {
    fail('backup restore drill canonical JSON must be minified deterministic JSON');
  }
  return parsed;
}

function parseOperatorKeys() {
  const path = process.env.MMPAY_OPERATOR_KEYS_FILE ?? join(root, 'governance/operator-keys.yaml');
  const yaml = readRequired(path, 'operator keys file missing');
  rejectPlaceholders(yaml, 'operator keys file');
  const entries = [];
  let current;
  for (const line of yaml.split('\n')) {
    if (line.startsWith('  - ')) {
      current = {};
      entries.push(current);
      assignYamlField(current, line.slice(4));
    } else if (current && line.startsWith('    ')) {
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
  target[text.slice(0, separator).trim()] = text.slice(separator + 1).trim();
}

function findOperatorKey(operatorKeys, fingerprint) {
  const key = operatorKeys.find(entry => entry.fingerprint === fingerprint);
  if (!key) {
    fail('operator_key_fingerprint is not pinned in governance/operator-keys.yaml');
  }
  return key;
}

function verifyEvidenceFields(evidence) {
  if (!/^v1\.0\.0(-rc\.[0-9]+)?$/.test(evidence.tag)) {
    fail('backup restore drill tag must be v1.0.0 or v1.0.0-rc.N');
  }
  if (!/^[0-9a-f]{40}$/.test(evidence.commit_sha)) {
    fail('backup restore drill commit_sha must be a 40-char lowercase hex SHA');
  }
  for (const name of expectedKeys.filter(key => key.endsWith('_sha256'))) {
    if (!/^[0-9a-f]{64}$/.test(evidence[name])) {
      fail(`${name} must be a lowercase sha256 hex digest`);
    }
  }
  if (!isUuid(evidence.drill_id) || !isUuid(evidence.restore_nonce)) {
    fail('backup restore drill ids must be lowercase UUIDs');
  }
  if (!Number.isInteger(evidence.rto_minutes) || evidence.rto_minutes < 1) {
    fail('backup restore drill rto_minutes must be a positive integer');
  }
  if (evidence.rto_minutes > maxRtoMinutes) {
    fail('backup restore drill exceeded the 8 hour RTO');
  }
  if (Number.isNaN(Date.parse(evidence.completed_at))) {
    fail('backup restore drill completed_at must be an ISO timestamp');
  }
}

function verifyEvidenceFreshness(completedAt) {
  const completedAtMs = Date.parse(completedAt);
  const candidateMs = Date.parse(process.env.MMPAY_DRILL_CANDIDATE_DATE ?? new Date().toISOString());
  if (Number.isNaN(candidateMs)) {
    fail('MMPAY_DRILL_CANDIDATE_DATE must be an ISO timestamp');
  }
  if (completedAtMs - candidateMs > maxFutureSkewMs) {
    fail('backup restore drill completed_at is after the candidate date');
  }
  if (candidateMs - completedAtMs > maxDrillAgeMs) {
    fail('backup restore drill evidence is older than 90 days');
  }
}

function verifyExternalHashes(evidence) {
  const e2ePath = process.env.MMPAY_E2E_EVIDENCE_FILE ?? join(root, 'docs/release/v1.0.0-e2e-evidence.md');
  if (evidence.e2e_evidence_sha256 !== sha256(e2ePath)) {
    fail(`e2e_evidence_sha256 does not match ${e2ePath}`);
  }
}

function verifyRestoreNonce(nonce) {
  const noncesDir = process.env.MMPAY_RESTORE_NONCES_DIR ?? join(root, 'governance/restore-nonces');
  readRequired(join(noncesDir, `${nonce}.txt`), 'restore nonce evidence missing');
}

function verifyOperatorKey(operatorKey, evidence) {
  for (const field of requiredOperatorKeyFields()) {
    if (!operatorKey[field]) {
      fail(`operator key missing required field: ${field}`);
    }
  }
  if (operatorKey.role !== 'restore-attestation') {
    fail('operator key role must be restore-attestation');
  }
  verifyKeyWindow(operatorKey, evidence.completed_at);
  if (operatorKey.evidence_key_fingerprint !== evidence.evidence_key_fingerprint) {
    fail('evidence_key_fingerprint does not match the pinned operator key metadata');
  }
}

function requiredOperatorKeyFields() {
  return [
    'fingerprint',
    'key_id',
    'role',
    'valid_from',
    'valid_to',
    'public_key_path',
    'evidence_key_fingerprint',
    'evidence_public_key_path'
  ];
}

function verifyKeyWindow(operatorKey, completedAt) {
  const completedAtMs = Date.parse(completedAt);
  if (Date.parse(`${operatorKey.valid_from}T00:00:00Z`) > completedAtMs) {
    fail('operator key is not valid yet at completed_at');
  }
  if (Date.parse(`${operatorKey.valid_to}T23:59:59Z`) < completedAtMs) {
    fail('operator key is expired at completed_at');
  }
}

function verifyEd25519Signature(options) {
  const publicKey = readEd25519PublicKey(resolvePath(options.operatorKey.evidence_public_key_path));
  const signature = Buffer.from(options.signature, 'base64');
  if (!verifySignature(null, Buffer.from(options.canonical), publicKey.key, signature)) {
    fail('Ed25519 drill evidence signature is invalid');
  }
  if (publicKey.fingerprint !== options.operatorKey.evidence_key_fingerprint) {
    fail('Ed25519 public key fingerprint does not match operator metadata');
  }
}

function readEd25519PublicKey(path) {
  const key = createPublicKey(readRequired(path, 'evidence Ed25519 public key missing'));
  const fingerprint = createHash('sha256').update(key.export({ format: 'der', type: 'spki' })).digest('hex');
  return { fingerprint, key };
}

function verifyPgpSignature(options) {
  const publicKeyPath = resolvePath(options.operatorKey.public_key_path);
  readRequired(publicKeyPath, 'operator PGP public key missing');
  const workspace = mkdtempSync(join(tmpdir(), 'mmpay-drill-verify-'));
  try {
    const gnupgHome = join(workspace, 'gnupg');
    execFileSync('mkdir', ['-p', gnupgHome]);
    execFileSync('chmod', ['700', gnupgHome]);
    const canonicalPath = join(workspace, 'drill.json');
    const signaturePath = join(workspace, 'drill.sig.asc');
    writeFileSync(canonicalPath, options.canonical);
    writeFileSync(signaturePath, options.signature);
    execFileSync('gpg', ['--batch', '--homedir', gnupgHome, '--import', publicKeyPath], { stdio: 'pipe' });
    const status = execFileSync('gpg', ['--batch', '--homedir', gnupgHome, '--status-fd', '1', '--verify', signaturePath, canonicalPath], { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] });
    if (!status.includes(`VALIDSIG ${options.operatorKey.fingerprint}`)) {
      fail('operator PGP signature does not match the pinned fingerprint');
    }
  } finally {
    rmSync(workspace, { recursive: true, force: true });
  }
}

function isUuid(value) {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/.test(value);
}

function resolvePath(path) {
  return isAbsolute(path) ? path : join(root, path);
}

function sha256(path) {
  return createHash('sha256').update(readRequired(path, 'sha256 source file missing')).digest('hex');
}

function fail(message) {
  console.error(message);
  process.exit(1);
}

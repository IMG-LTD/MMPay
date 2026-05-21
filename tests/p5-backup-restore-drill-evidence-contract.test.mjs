import assert from 'node:assert/strict';
import { execFileSync, spawnSync } from 'node:child_process';
import { createHash, generateKeyPairSync, sign } from 'node:crypto';
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { readFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { describe, it } from 'node:test';

const root = process.cwd();

describe('P5 backup restore drill evidence contract', () => {
  it('wires signed drill evidence verification into GA release gates', async () => {
    const releaseGate = await read('scripts/release-gate.sh');
    const validateLocal = await read('scripts/validate-local.sh');

    assert.match(releaseGate, /verify-backup-restore-drill-evidence\.sh/);
    assert.match(validateLocal, /p5-backup-restore-drill-evidence-contract\.test\.mjs/);
    assert.match(validateLocal, /verify-backup-restore-drill-evidence\.sh/);
  });

  it('accepts a fresh drill evidence artifact with Ed25519 and operator PGP signatures', () => {
    const workspace = mkdtempSync(join(tmpdir(), 'mmpay-drill-evidence-'));
    try {
      const fixture = createSignedFixture(workspace);
      assertVerified(runVerifier(fixture));
    } finally {
      rmSync(workspace, { recursive: true, force: true });
    }
  });

  it('does not scan opaque signature armor for placeholder-like text', () => {
    const workspace = mkdtempSync(join(tmpdir(), 'mmpay-drill-evidence-'));
    try {
      const fixture = createSignedFixture(workspace);
      injectPgpArmorHeader(fixture.evidencePath, 'Comment: fake-marker-inside-verified-signature-armor');
      assertVerified(runVerifier(fixture));
    } finally {
      rmSync(workspace, { recursive: true, force: true });
    }
  });
});

function createSignedFixture(workspace) {
  const pgp = createPgpKey(workspace);
  const ed25519 = createEvidenceKey(workspace);
  const inputs = writeEvidenceInputs(workspace);
  const canonical = canonicalDrillPayload({ pgp, ed25519, e2ePath: inputs.e2ePath });
  const pgpSignaturePath = signCanonicalWithPgp({ workspace, pgp, canonical });
  const evidencePath = writeEvidenceFile({ workspace, canonical, ed25519, pgpSignaturePath });
  const operatorKeysPath = writeOperatorKeys({ workspace, pgp, ed25519 });
  return {
    evidencePath,
    env: fixtureEnv({ workspace, e2ePath: inputs.e2ePath, operatorKeysPath })
  };
}

function writeEvidenceInputs(workspace) {
  const e2ePath = join(workspace, 'e2e.md');
  const nonceId = '11111111-2222-4333-8444-555555555555';
  const noncesDir = join(workspace, 'restore-nonces');
  execFileSync('mkdir', ['-p', noncesDir]);
  writeFileSync(e2ePath, 'Provider environment: sandbox-with-real-money\n');
  writeFileSync(join(noncesDir, `${nonceId}.txt`), 'restore drill nonce\n');
  return { e2ePath, nonceId };
}

function canonicalDrillPayload(options) {
  return JSON.stringify({
    backup_file_sha256: sha256Text('backup archive bytes'),
    completed_at: '2026-05-19T07:40:00Z',
    commit_sha: 'b0deec83731bc8b046af6a053b3b996cca3692c5',
    drill_id: '22222222-3333-4444-8555-666666666666',
    e2e_evidence_sha256: sha256(options.e2ePath),
    evidence_key_fingerprint: options.ed25519.fingerprint,
    operator_key_fingerprint: options.pgp.fingerprint,
    restore_nonce: '11111111-2222-4333-8444-555555555555',
    rto_minutes: 120,
    tag: 'v1.0.0'
  });
}

function signCanonicalWithPgp(options) {
  const canonicalPath = join(options.workspace, 'drill.json');
  const pgpSignaturePath = join(options.workspace, 'drill.sig.asc');
  writeFileSync(canonicalPath, options.canonical);
  execFileSync('gpg', pgpSignArgs({ pgp: options.pgp, pgpSignaturePath, canonicalPath }), { stdio: 'pipe' });
  return pgpSignaturePath;
}

function pgpSignArgs(options) {
  return [
    '--batch',
    '--homedir',
    options.pgp.gnupgHome,
    '--armor',
    '--detach-sign',
    '--local-user',
    options.pgp.fingerprint,
    '--output',
    options.pgpSignaturePath,
    options.canonicalPath
  ];
}

function writeEvidenceFile(options) {
  const evidencePath = join(options.workspace, 'backup-restore-drill-evidence.md');
  writeFileSync(evidencePath, evidenceText(options));
  return evidencePath;
}

function evidenceText(options) {
  return [
    '-----BEGIN MMPAY BACKUP RESTORE DRILL-----',
    options.canonical,
    '-----END MMPAY BACKUP RESTORE DRILL-----',
    '-----BEGIN MMPAY DRILL ED25519 SIGNATURE-----',
    sign(null, Buffer.from(options.canonical), options.ed25519.privateKey).toString('base64'),
    '-----END MMPAY DRILL ED25519 SIGNATURE-----',
    readFileSync(options.pgpSignaturePath, 'utf8').trim(),
    ''
  ].join('\n');
}

function writeOperatorKeys(options) {
  const operatorKeysPath = join(options.workspace, 'operator-keys.yaml');
  writeFileSync(operatorKeysPath, operatorKeysYaml(options.pgp, options.ed25519));
  return operatorKeysPath;
}

function fixtureEnv(options) {
  return {
    MMPAY_DRILL_CANDIDATE_DATE: '2026-05-19T07:41:00Z',
    MMPAY_E2E_EVIDENCE_FILE: options.e2ePath,
    MMPAY_OPERATOR_KEYS_FILE: options.operatorKeysPath,
    MMPAY_RESTORE_NONCES_DIR: join(options.workspace, 'restore-nonces')
  };
}

function runVerifier(fixture) {
  return spawnSync('bash', ['scripts/governance/verify-backup-restore-drill-evidence.sh', fixture.evidencePath], {
    cwd: root,
    encoding: 'utf8',
    env: { ...process.env, ...fixture.env }
  });
}

function assertVerified(result) {
  assert.equal(result.status, 0, result.stderr);
  assert.match(result.stdout, /backup restore drill evidence verified/);
}

function injectPgpArmorHeader(path, header) {
  const text = readFileSync(path, 'utf8');
  writeFileSync(path, text.replace('-----BEGIN PGP SIGNATURE-----\n\n', `-----BEGIN PGP SIGNATURE-----\n${header}\n\n`));
}

function createPgpKey(workspace) {
  const gnupgHome = join(workspace, 'gnupg');
  execFileSync('mkdir', ['-p', gnupgHome]);
  execFileSync('chmod', ['700', gnupgHome]);
  execFileSync('gpg', [
    '--batch',
    '--homedir',
    gnupgHome,
    '--pinentry-mode',
    'loopback',
    '--passphrase',
    '',
    '--quick-gen-key',
    'MMPay Restore Operator <restore-operator@mmpay.local>',
    'rsa2048',
    'sign',
    '1y'
  ], { stdio: 'pipe' });
  const fingerprint = execFileSync('gpg', [
    '--batch',
    '--homedir',
    gnupgHome,
    '--with-colons',
    '--list-keys'
  ], { encoding: 'utf8' }).split('\n').find(line => line.startsWith('fpr:')).split(':')[9];
  const publicKeyPath = join(workspace, 'operator.asc');
  writeFileSync(publicKeyPath, execFileSync('gpg', [
    '--batch',
    '--homedir',
    gnupgHome,
    '--armor',
    '--export',
    fingerprint
  ]));
  return { fingerprint, gnupgHome, publicKeyPath };
}

function createEvidenceKey(workspace) {
  const pair = generateKeyPairSync('ed25519');
  const publicDer = pair.publicKey.export({ format: 'der', type: 'spki' });
  const publicKeyPath = join(workspace, 'evidence-ed25519-public.pem');
  writeFileSync(publicKeyPath, pair.publicKey.export({ format: 'pem', type: 'spki' }));
  return {
    fingerprint: createHash('sha256').update(publicDer).digest('hex'),
    privateKey: pair.privateKey,
    publicKeyPath
  };
}

function operatorKeysYaml(pgp, ed25519) {
  return [
    'operator_keys:',
    `  - fingerprint: ${pgp.fingerprint}`,
    '    key_id: operator-restore-attestation',
    '    role: restore-attestation',
    '    valid_from: 2026-01-01',
    '    valid_to: 2027-01-01',
    `    public_key_path: ${pgp.publicKeyPath}`,
    `    evidence_key_fingerprint: ${ed25519.fingerprint}`,
    `    evidence_public_key_path: ${ed25519.publicKeyPath}`,
    ''
  ].join('\n');
}

function sha256(path) {
  return createHash('sha256').update(readFileSync(path)).digest('hex');
}

function sha256Text(text) {
  return createHash('sha256').update(text).digest('hex');
}

function read(path) {
  return readFile(join(root, path), 'utf8');
}

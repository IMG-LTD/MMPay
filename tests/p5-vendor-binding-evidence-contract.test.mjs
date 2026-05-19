import assert from 'node:assert/strict';
import { execFileSync, spawnSync } from 'node:child_process';
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { readFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { describe, it } from 'node:test';
import { createHash } from 'node:crypto';

const root = process.cwd();

describe('P5 vendor binding evidence contract', () => {
  it('wires canonical vendor binding verification into GA release gates', async () => {
    const releaseGate = await read('scripts/release-gate.sh');
    const validateLocal = await read('scripts/validate-local.sh');

    assert.match(releaseGate, /verify-vendor-binding-evidence\.sh/);
    assert.match(validateLocal, /p5-vendor-binding-evidence-contract\.test\.mjs/);
    assert.match(validateLocal, /verify-vendor-binding-evidence\.sh/);
  });

  it('accepts a signed canonical BINDING_OK artifact with matching evidence hashes', () => {
    const workspace = mkdtempSync(join(tmpdir(), 'mmpay-vendor-binding-'));
    try {
      const fixture = createSignedFixture(workspace);
      const result = spawnSync('bash', ['scripts/governance/verify-vendor-binding-evidence.sh', fixture.bindingPath], {
        cwd: root,
        encoding: 'utf8',
        env: { ...process.env, ...fixture.env }
      });

      assert.equal(result.status, 0, result.stderr);
      assert.match(result.stdout, /vendor binding evidence verified/);
    } finally {
      rmSync(workspace, { recursive: true, force: true });
    }
  });
});

function createSignedFixture(workspace) {
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
    'MMPay Vendor Binding <vendor-binding@mmpay.local>',
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

  const publicKeyPath = join(workspace, 'vendor.asc');
  writeFileSync(publicKeyPath, execFileSync('gpg', [
    '--batch',
    '--homedir',
    gnupgHome,
    '--armor',
    '--export',
    fingerprint
  ]));

  const evidence = writeEvidenceFiles(workspace);
  const canonical = JSON.stringify({
    commit_sha: '795c059191f4ac02e3091bc5298f91d4a815a639',
    drill_evidence_sha256: evidence.drillHash,
    e2e_evidence_sha256: evidence.e2eHash,
    image_digest_evidence_sha256: evidence.imageHash,
    signed_at: '2026-05-19T07:18:17Z',
    tag: 'v1.0.0',
    vendor_key_fingerprint: fingerprint
  });
  const canonicalPath = join(workspace, 'binding.json');
  const signaturePath = join(workspace, 'binding.sig.asc');
  writeFileSync(canonicalPath, canonical);
  execFileSync('gpg', [
    '--batch',
    '--homedir',
    gnupgHome,
    '--armor',
    '--detach-sign',
    '--local-user',
    fingerprint,
    '--output',
    signaturePath,
    canonicalPath
  ], { stdio: 'pipe' });

  const bindingPath = join(workspace, 'v1.0.0-BINDING_OK.asc');
  writeFileSync(bindingPath, [
    '-----BEGIN MMPAY VENDOR BINDING-----',
    canonical,
    '-----END MMPAY VENDOR BINDING-----',
    readFileSync(signaturePath, 'utf8').trim(),
    ''
  ].join('\n'));

  const vendorKeysPath = join(workspace, 'vendor-keys.yaml');
  const revocationFeedPath = join(workspace, 'revocations.txt');
  writeFileSync(revocationFeedPath, '');
  writeFileSync(vendorKeysPath, vendorKeysYaml(fingerprint, publicKeyPath, revocationFeedPath));

  return {
    bindingPath,
    env: {
      MMPAY_VENDOR_KEYS_FILE: vendorKeysPath,
      MMPAY_IMAGE_DIGEST_EVIDENCE_FILE: evidence.imagePath,
      MMPAY_E2E_EVIDENCE_FILE: evidence.e2ePath,
      MMPAY_DRILL_EVIDENCE_FILE: evidence.drillPath
    }
  };
}

function writeEvidenceFiles(workspace) {
  const imagePath = join(workspace, 'image.md');
  const e2ePath = join(workspace, 'e2e.md');
  const drillPath = join(workspace, 'drill.md');
  writeFileSync(imagePath, 'Evidence status: completed-external-evidence\nTag: v1.0.0\n');
  writeFileSync(e2ePath, 'Provider environment: sandbox-with-real-money\n');
  writeFileSync(drillPath, 'Evidence status: completed-external-evidence\nOperator PGP signature: valid\n');
  return {
    imagePath,
    e2ePath,
    drillPath,
    imageHash: sha256(imagePath),
    e2eHash: sha256(e2ePath),
    drillHash: sha256(drillPath)
  };
}

function vendorKeysYaml(fingerprint, publicKeyPath, revocationFeedPath) {
  return [
    'vendor_keys:',
    `  - fingerprint: ${fingerprint}`,
    '    key_id: vendor-ga-binding-key',
    '    role: binding-sign',
    '    valid_from: 2026-01-01',
    '    valid_to: 2027-01-01',
    '    rotation_window_days: 30',
    `    public_key_path: ${publicKeyPath}`,
    '    signed_by_root: SHA256:vendor-root-fingerprint',
    `    revocation_feed_url: file://${revocationFeedPath}`,
    '    signing_sla_business_days: 5',
    '    sub_keys: []',
    ''
  ].join('\n');
}

function sha256(path) {
  return createHash('sha256').update(readFileSync(path)).digest('hex');
}

function read(path) {
  return readFile(join(root, path), 'utf8');
}

import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { mkdtempSync, readFileSync, rmSync } from 'node:fs';
import { readFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { describe, it } from 'node:test';

const root = process.cwd();

describe('PGP key governance contract', () => {
  it('pins the tracked vendor public key and keeps revocation state separate', () => {
    const fingerprint = pgpFingerprint('docs/security/public-key.asc');
    const vendorKeys = readText('governance/vendor-keys.yaml');
    const revocations = readText('governance/vendor-revocations.txt');

    assert.equal(fingerprint, '424C97E0550A270CCEB231393F66794D62C0C8A1');
    assert.match(vendorKeys, new RegExp(`fingerprint: ${fingerprint}`));
    assert.match(vendorKeys, /public_key_path: docs\/security\/public-key\.asc/);
    assert.match(vendorKeys, /revocation_feed_url: governance\/vendor-revocations\.txt/);
    assert.doesNotMatch(vendorKeys, /required-before-ga|example\.invalid|replace-with/i);
    assert.doesNotMatch(revocations, new RegExp(fingerprint));
  });

  it('keeps local PGP private and revocation material out of the repository', async () => {
    const gitignore = await read('.gitignore');
    const secretScan = await read('scripts/security-secret-scan.sh');
    const status = execFileSync('git', ['status', '--short', '--ignored', '--', 'docs/security'], {
      cwd: root,
      encoding: 'utf8'
    });

    assert.match(gitignore, /docs\/security\/private-key\.asc/);
    assert.match(gitignore, /docs\/security\/revocation-certificate\.asc/);
    assert.match(secretScan, /PGP PRIVATE KEY BLOCK/);
    assert.match(secretScan, /git -C "\$ROOT_DIR" ls-files --cached --others --exclude-standard -z/);
    assert.doesNotMatch(status, /^\?\? docs\/security\/private-key\.asc/m);
    assert.doesNotMatch(status, /^\?\? docs\/security\/revocation-certificate\.asc/m);
  });
});

function pgpFingerprint(path) {
  const workspace = mkdtempSync(join(tmpdir(), 'mmpay-pgp-key-'));
  try {
    const output = execFileSync('gpg', [
      '--batch',
      '--homedir',
      workspace,
      '--show-keys',
      '--with-colons',
      join(root, path)
    ], { encoding: 'utf8' });
    return output.split('\n').find(line => line.startsWith('fpr:')).split(':')[9];
  } finally {
    rmSync(workspace, { recursive: true, force: true });
  }
}

function readText(path) {
  return readFileSync(join(root, path), 'utf8');
}

function read(path) {
  return readFile(join(root, path), 'utf8');
}

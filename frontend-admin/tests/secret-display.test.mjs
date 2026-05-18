import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { maskSecretHandle, providerCredentialFields } from '../src/secretDisplay.js';

describe('provider credential display', () => {
  it('renders only masked secret handles', () => {
    assert.equal(maskSecretHandle('kms://huifu/sandbox/rsa-private-key'), 'kms://************-key');
  });

  it('does not expose plaintext credential fields', () => {
    assert.deepEqual(providerCredentialFields(), [
      { label: 'Sys ID', valueKind: 'secret-handle' },
      { label: 'Product ID', valueKind: 'secret-handle' },
      { label: 'RSA Public Key', valueKind: 'secret-handle' },
      { label: 'RSA Private Key', valueKind: 'secret-handle' },
      { label: 'Merchant ID', valueKind: 'secret-handle' },
      { label: 'Notify URL', valueKind: 'secret-handle' },
      { label: 'Webhook Endpoint Key', valueKind: 'secret-handle' },
      { label: 'Skill Source', valueKind: 'secret-handle' },
    ]);
  });
});

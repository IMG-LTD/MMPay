const VISIBLE_SUFFIX_LENGTH = 7;

/**
 * @param {string} handle
 * @returns {string}
 */
export function maskSecretHandle(handle) {
  if (!handle || handle.length <= VISIBLE_SUFFIX_LENGTH) {
    throw new Error('secret handle must be longer than the visible suffix');
  }
  return `${handle.slice(0, 6)}************${handle.slice(-VISIBLE_SUFFIX_LENGTH)}`;
}

/**
 * @returns {{ label: 'Merchant ID' | 'API Key' | 'Webhook Secret', valueKind: 'secret-handle' }[]}
 */
export function providerCredentialFields() {
  return [
    { label: 'Merchant ID', valueKind: 'secret-handle' },
    { label: 'API Key', valueKind: 'secret-handle' },
    { label: 'Webhook Secret', valueKind: 'secret-handle' },
  ];
}

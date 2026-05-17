import { credentialFieldLabels } from './i18n.js';

const VISIBLE_SUFFIX_LENGTH = 4;

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
 * @param {'en-US' | 'zh-CN'} [locale]
 * @returns {{ label: string, valueKind: 'secret-handle' }[]}
 */
export function providerCredentialFields(locale = 'en-US') {
  return credentialFieldLabels(locale).map((label) => ({ label, valueKind: 'secret-handle' }));
}

/**
 * @typedef {'en-US' | 'zh-CN'} AdminLocale
 */

/** @type {Readonly<Record<AdminLocale, Readonly<Record<string, string>>>>} */
export const adminMessages = Object.freeze({
  'en-US': Object.freeze({
    'admin.title': 'MMPay Admin',
    'credential.merchantId': 'Merchant ID',
    'credential.apiKey': 'API Key',
    'credential.webhookSecret': 'Webhook Secret',
    'table.providerField': 'Provider field',
    'table.valueKind': 'Value kind',
    'table.secretHandle': 'Secret handle',
  }),
  'zh-CN': Object.freeze({
    'admin.title': 'MMPay 管理后台',
    'credential.merchantId': '商户号',
    'credential.apiKey': 'API 密钥',
    'credential.webhookSecret': 'Webhook 密钥',
    'table.providerField': '提供方字段',
    'table.valueKind': '值类型',
    'table.secretHandle': '密钥句柄',
  }),
});

export const credentialFieldKeys = Object.freeze([
  'credential.merchantId',
  'credential.apiKey',
  'credential.webhookSecret',
]);

/**
 * @param {AdminLocale} locale
 * @param {string} key
 * @returns {string}
 */
export function t(locale, key) {
  const messages = adminMessages[locale];
  if (!messages || !Object.hasOwn(messages, key)) {
    throw new Error(`missing i18n message: ${locale}.${key}`);
  }
  return messages[key];
}

/**
 * @param {AdminLocale} [locale]
 * @returns {string[]}
 */
export function credentialFieldLabels(locale = 'en-US') {
  return credentialFieldKeys.map((key) => t(locale, key));
}

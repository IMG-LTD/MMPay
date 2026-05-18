/**
 * @typedef {'en-US' | 'zh-CN'} AdminLocale
 */

/** @type {Readonly<Record<AdminLocale, Readonly<Record<string, string>>>>} */
export const adminMessages = Object.freeze({
  'en-US': Object.freeze({
    'admin.title': 'MMPay Admin',
    'admin.subtitle': 'Payment operations',
    'credential.sysId': 'Sys ID',
    'credential.productId': 'Product ID',
    'credential.rsaPublicKey': 'RSA Public Key',
    'credential.rsaPrivateKey': 'RSA Private Key',
    'credential.merchantId': 'Merchant ID',
    'credential.notifyUrl': 'Notify URL',
    'credential.webhookEndpointKey': 'Webhook Endpoint Key',
    'credential.skillSource': 'Skill Source',
    'metric.activeMerchants': 'Active merchants',
    'metric.enabledChannels': 'Enabled channels',
    'metric.todayOrders': 'Today orders',
    'metric.pendingRefunds': 'Pending refunds',
    'nav.channels': 'Channels',
    'nav.invoices': 'Invoices',
    'nav.merchants': 'Merchants',
    'nav.orders': 'Orders',
    'nav.reconciliation': 'Reconciliation',
    'nav.refunds': 'Refunds',
    'nav.webhookLogs': 'Webhook logs',
    'nav.externalReadiness': 'External readiness',
    'table.amount': 'Amount',
    'table.channel': 'Channel',
    'table.credentials': 'Credentials',
    'table.event': 'Event',
    'table.item': 'Item',
    'table.merchant': 'Merchant',
    'table.orderRef': 'Order ref',
    'table.provider': 'Provider',
    'table.providerField': 'Provider field',
    'table.reason': 'Reason',
    'table.requiredEvidence': 'Required evidence',
    'table.status': 'Status',
    'table.valueKind': 'Value kind',
    'table.secretHandle': 'Secret handle',
    'table.updatedAt': 'Updated at',
  }),
  'zh-CN': Object.freeze({
    'admin.title': 'MMPay 管理后台',
    'admin.subtitle': '支付运营',
    'credential.sysId': '系统号',
    'credential.productId': '产品号',
    'credential.rsaPublicKey': 'RSA 公钥',
    'credential.rsaPrivateKey': 'RSA 私钥',
    'credential.merchantId': '商户号',
    'credential.notifyUrl': '通知地址',
    'credential.webhookEndpointKey': 'Webhook 终端密钥',
    'credential.skillSource': 'Skill Source',
    'metric.activeMerchants': '活跃商户',
    'metric.enabledChannels': '启用通道',
    'metric.todayOrders': '今日订单',
    'metric.pendingRefunds': '待处理退款',
    'nav.channels': '通道',
    'nav.invoices': '发票',
    'nav.merchants': '商户',
    'nav.orders': '订单',
    'nav.reconciliation': '对账',
    'nav.refunds': '退款',
    'nav.webhookLogs': 'Webhook 日志',
    'nav.externalReadiness': '外部就绪',
    'table.amount': '金额',
    'table.channel': '通道',
    'table.credentials': '凭证',
    'table.event': '事件',
    'table.item': '事项',
    'table.merchant': '商户',
    'table.orderRef': '订单号',
    'table.provider': '提供方',
    'table.providerField': '提供方字段',
    'table.reason': '原因',
    'table.requiredEvidence': '所需证据',
    'table.status': '状态',
    'table.valueKind': '值类型',
    'table.secretHandle': '密钥句柄',
    'table.updatedAt': '更新时间',
  }),
});

export const credentialFieldKeys = Object.freeze([
  'credential.sysId',
  'credential.productId',
  'credential.rsaPublicKey',
  'credential.rsaPrivateKey',
  'credential.merchantId',
  'credential.notifyUrl',
  'credential.webhookEndpointKey',
  'credential.skillSource',
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

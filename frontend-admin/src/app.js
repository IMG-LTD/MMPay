import { maskSecretHandle, providerCredentialFields } from './secretDisplay.js';
import { t } from './i18n.js';

/**
 * @typedef {'en-US' | 'zh-CN'} AdminLocale
 * @typedef {{ readonly key: string, readonly label: string }} NavigationItem
 * @typedef {{ readonly label: string, readonly value: string }} MetricCard
 * @typedef {{ readonly key: string, readonly label: string }} TableColumn
 * @typedef {Readonly<Record<string, string>>} TableRow
 * @typedef {{ readonly key: string, readonly title: string, readonly columns: readonly TableColumn[], readonly rows: readonly TableRow[] }} AdminTable
 * @typedef {{ readonly title: string, readonly subtitle: string, readonly navigation: readonly NavigationItem[], readonly metricCards: readonly MetricCard[], readonly tables: readonly AdminTable[] }} AdminDashboard
 */

const sampleHandles = {
  'Merchant ID': 'kms://huifu/sandbox/merchant-id',
  'API Key': 'kms://huifu/sandbox/api-key',
  'Webhook Secret': 'kms://mmpay/mmmail/webhook-secret',
};

/** @type {Readonly<Record<string, readonly TableRow[]>>} */
const tableRows = Object.freeze({
  channels: Object.freeze([
    Object.freeze({ merchant: 'MMMail', channel: 'Huifu sandbox', status: 'credentials-required', updatedAt: 'not connected' }),
  ]),
  orders: Object.freeze([]),
  refunds: Object.freeze([]),
  'webhook-logs': Object.freeze([]),
  reconciliation: Object.freeze([]),
});

/** @type {Readonly<Record<string, readonly string[]>>} */
const tableColumns = Object.freeze({
  credentials: Object.freeze(['table.providerField', 'table.valueKind', 'table.secretHandle']),
  channels: Object.freeze(['table.merchant', 'table.channel', 'table.status', 'table.updatedAt']),
  orders: Object.freeze(['table.orderRef', 'table.channel', 'table.amount', 'table.status']),
  refunds: Object.freeze(['table.orderRef', 'table.amount', 'table.status', 'table.reason']),
  'webhook-logs': Object.freeze(['table.event', 'table.status', 'table.updatedAt']),
  reconciliation: Object.freeze(['table.channel', 'table.amount', 'table.status', 'table.updatedAt']),
});

/** @type {readonly string[]} */
const navigationKeys = Object.freeze([
  'merchants',
  'channels',
  'orders',
  'refunds',
  'webhook-logs',
  'reconciliation',
]);

/** @type {readonly (readonly [string, string])[]} */
const metricKeys = Object.freeze([
  ['metric.activeMerchants', '0'],
  ['metric.enabledChannels', '0'],
  ['metric.todayOrders', '0'],
  ['metric.pendingRefunds', '0'],
]);

/**
 * @param {AdminLocale} [locale]
 * @returns {{ label: string, valueKind: string, displayValue: string }[]}
 */
export function renderProviderCredentialRows(locale = 'en-US') {
  return providerCredentialFields(locale).map((field, index) => ({
    label: field.label,
    valueKind: field.valueKind,
    displayValue: maskSecretHandle(Object.values(sampleHandles)[index]),
  }));
}

/**
 * @param {AdminLocale} [locale]
 * @returns {AdminDashboard}
 */
export function renderAdminDashboard(locale = 'en-US') {
  return {
    title: t(locale, 'admin.title'),
    subtitle: t(locale, 'admin.subtitle'),
    navigation: renderNavigation(locale),
    metricCards: metricKeys.map(([labelKey, value]) => ({ label: t(locale, labelKey), value })),
    tables: renderTables(locale),
  };
}

/**
 * @param {AdminLocale} locale
 * @returns {readonly NavigationItem[]}
 */
function renderNavigation(locale) {
  return navigationKeys.map((key) => ({ key, label: t(locale, `nav.${key === 'webhook-logs' ? 'webhookLogs' : key}`) }));
}

/**
 * @param {AdminLocale} locale
 * @returns {readonly AdminTable[]}
 */
function renderTables(locale) {
  return [
    renderTable(locale, 'credentials', renderProviderCredentialRows(locale)),
    renderTable(locale, 'channels', tableRows.channels),
    renderTable(locale, 'orders', tableRows.orders),
    renderTable(locale, 'refunds', tableRows.refunds),
    renderTable(locale, 'webhook-logs', tableRows['webhook-logs']),
    renderTable(locale, 'reconciliation', tableRows.reconciliation),
  ];
}

/**
 * @param {AdminLocale} locale
 * @param {string} key
 * @param {readonly TableRow[]} rows
 * @returns {AdminTable}
 */
function renderTable(locale, key, rows) {
  return {
    key,
    title: t(locale, key === 'credentials' ? 'table.credentials' : `nav.${key === 'webhook-logs' ? 'webhookLogs' : key}`),
    columns: tableColumns[key].map((labelKey) => ({ label: t(locale, labelKey), key: toColumnKey(labelKey) })),
    rows,
  };
}

/**
 * @param {string} labelKey
 * @returns {string}
 */
function toColumnKey(labelKey) {
  return labelKey.replace('table.', '').replace('providerField', 'label').replace('secretHandle', 'displayValue');
}

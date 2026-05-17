import { createApp, h } from 'vue';
import { createPinia } from 'pinia';
import {
  NCard,
  NConfigProvider,
  NDataTable,
  NGrid,
  NGi,
  NLayout,
  NLayoutContent,
  NLayoutSider,
  NMenu,
  NMessageProvider,
  NSpace,
  NStatistic,
  NTag,
} from 'naive-ui';
import { renderAdminDashboard } from './app.js';

export const soybeanAdminStack = Object.freeze({
  upstream: 'soybean-admin',
  ui: 'naive-ui',
  state: 'pinia',
  build: 'vite',
});

const statusTypes: Record<string, 'success' | 'warning' | 'info'> = {
  'credentials-required': 'warning',
};

export function createMmpayAdminApp() {
  return createApp({
    name: 'MmpaySoybeanAdminShell',
    setup() {
      const locale = 'en-US';
      const dashboard = renderAdminDashboard(locale);

      return () =>
        h(NConfigProvider, null, {
          default: () =>
            h(NMessageProvider, null, {
              default: () =>
                h(NLayout, { hasSider: true, style: 'min-height: 100vh' }, () => [
                  h(NLayoutSider, { bordered: true, width: 220 }, () =>
                    h(NMenu, { options: [...dashboard.navigation], value: 'orders' }),
                  ),
                  h(NLayoutContent, { style: 'padding: 24px' }, () => renderContent(dashboard)),
                ]),
            }),
        });
    },
  }).use(createPinia());
}

function renderContent(dashboard: ReturnType<typeof renderAdminDashboard>) {
  return h(NSpace, { vertical: true, size: 18 }, () => [
    h(NCard, { title: dashboard.title }, () => dashboard.subtitle),
    renderMetrics(dashboard.metricCards),
    ...dashboard.tables.map((table) => renderTable(table)),
  ]);
}

function renderMetrics(metricCards: ReturnType<typeof renderAdminDashboard>['metricCards']) {
  return h(NGrid, { cols: 4, xGap: 12 }, () =>
    metricCards.map((metric) =>
      h(NGi, { key: metric.label }, () => h(NCard, () => h(NStatistic, { label: metric.label, value: metric.value }))),
    ),
  );
}

function renderTable(table: ReturnType<typeof renderAdminDashboard>['tables'][number]) {
  return h(NCard, { title: table.title }, () =>
    h(NDataTable, {
      columns: table.columns.map((column) => renderColumn(column)),
      data: [...table.rows],
      pagination: false,
      singleLine: false,
    }),
  );
}

function renderColumn(column: { label: string; key: string }) {
  if (column.key !== 'status' && column.key !== 'valueKind') {
    return { title: column.label, key: column.key };
  }
  return {
    title: column.label,
    key: column.key,
    render: (row: Record<string, string>) => h(NTag, { type: statusTypes[row[column.key]] ?? 'info' }, () => row[column.key]),
  };
}

const mountPoint = document.querySelector('#app');
if (mountPoint) {
  createMmpayAdminApp().mount(mountPoint);
}

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useAppStore } from '@/store/modules/app';
import { type Dashboard, fetchAdminDashboard } from '@/service/api/admin';

const appStore = useAppStore();
const loading = ref(false);
const errorMessage = ref('');
const dashboard = ref<Dashboard | null>(null);

const gap = computed(() => (appStore.isMobile ? 0 : 16));
const emptyTables = computed(() => dashboard.value?.tables.filter(table => table.rows.length === 0) ?? []);
const visibleTables = computed(() => dashboard.value?.tables.filter(table => table.rows.length > 0) ?? []);

async function loadDashboard() {
  loading.value = true;
  errorMessage.value = '';

  try {
    dashboard.value = await fetchAdminDashboard();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Dashboard request failed';
  } finally {
    loading.value = false;
  }
}

onMounted(loadDashboard);
</script>

<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false" class="card-wrapper">
      <div class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 class="m-0 text-24px font-600 text-primary">MMPay Admin</h1>
          <p class="m-0 mt-2 text-14px text-#64748b dark:text-#94a3b8">
            汇付适配、商户通道、Webhook、License Relay 与外部闭环证据的统一控制台。
          </p>
        </div>
        <NSpace>
          <NButton :loading="loading" type="primary" @click="loadDashboard">刷新</NButton>
          <NTag type="warning" :bordered="false">Foundation pending</NTag>
        </NSpace>
      </div>
    </NCard>

    <NAlert v-if="errorMessage" title="控制台数据加载失败" type="error">
      {{ errorMessage }}
    </NAlert>

    <NGrid :x-gap="gap" :y-gap="16" responsive="screen" item-responsive>
      <NGi v-for="metric in dashboard?.metrics ?? []" :key="metric.label" span="24 s:12 m:6">
        <NCard :bordered="false" class="card-wrapper">
          <NStatistic :label="metric.label" :value="metric.value" />
        </NCard>
      </NGi>
    </NGrid>

    <NCard :bordered="false" class="card-wrapper">
      <template #header>业务导航</template>
      <NSpin :show="loading && !dashboard">
        <NEmpty v-if="!dashboard?.navigation?.length" description="暂无导航数据" />
        <NSpace v-else>
          <NTag v-for="item in dashboard.navigation" :key="item.key" type="info" :bordered="false">
            {{ item.label }}
          </NTag>
        </NSpace>
      </NSpin>
    </NCard>

    <NGrid :x-gap="gap" :y-gap="16" responsive="screen" item-responsive>
      <NGi v-for="table in visibleTables" :key="table.key" span="24 m:12">
        <NCard :bordered="false" class="card-wrapper">
          <template #header>{{ table.key }}</template>
          <NTable :bordered="false" :single-line="false" size="small">
            <thead>
              <tr>
                <th v-for="column in table.columns" :key="column">{{ column }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, rowIndex) in table.rows" :key="rowIndex">
                <td v-for="column in table.columns" :key="column">{{ row[column] ?? '-' }}</td>
              </tr>
            </tbody>
          </NTable>
        </NCard>
      </NGi>
    </NGrid>

    <NCard v-if="emptyTables.length" :bordered="false" class="card-wrapper">
      <template #header>未接入数据面</template>
      <NSpace>
        <NTag v-for="table in emptyTables" :key="table.key" type="default" :bordered="false">
          {{ table.key }}
        </NTag>
      </NSpace>
    </NCard>
  </NSpace>
</template>

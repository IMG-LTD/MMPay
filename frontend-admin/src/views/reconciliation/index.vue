<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ackReconciliationRun, fetchReconciliationRuns, type ReconciliationRun } from '@/service/api/admin';

const loading = ref(false);
const errorMessage = ref('');
const runs = ref<ReconciliationRun[]>([]);

async function loadRuns() {
  loading.value = true;
  errorMessage.value = '';
  try {
    runs.value = (await fetchReconciliationRuns()).items;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Failed to load reconciliation runs';
  } finally {
    loading.value = false;
  }
}

async function ackRun(id: number) {
  errorMessage.value = '';
  try {
    await ackReconciliationRun(id);
    await loadRuns();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Ack failed';
  }
}

onMounted(loadRuns);
</script>

<template>
  <NSpace vertical :size="16">
    <NAlert v-if="errorMessage" type="error" title="对账失败">{{ errorMessage }}</NAlert>
    <NCard :bordered="false" class="card-wrapper">
      <template #header>日终对账</template>
      <template #header-extra><NButton secondary :loading="loading" @click="loadRuns">刷新</NButton></template>
      <NSpin :show="loading">
        <NEmpty v-if="!runs.length" description="暂无对账运行记录" />
        <NTable v-else :bordered="false" :single-line="false" size="small">
          <thead>
            <tr>
              <th>ID</th>
              <th>日期</th>
              <th>通道</th>
              <th>结果</th>
              <th>匹配</th>
              <th>确认</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="run in runs" :key="run.id">
              <td><RouterLink :to="{ name: 'reconciliation-detail', params: { id: run.id } }">{{ run.id }}</RouterLink></td>
              <td>{{ run.run_date }}</td>
              <td>{{ run.channel_id }}</td>
              <td><NTag :bordered="false" :aria-label="`reconciliation outcome ${run.outcome}`">{{ run.outcome }}</NTag></td>
              <td>{{ run.matched_count }} / {{ run.ingest_count }}</td>
              <td>{{ run.ack_status }}</td>
              <td>
                <NPopconfirm v-if="run.ack_status === 'pending'" @positive-click="ackRun(run.id)">
                  <template #trigger><NButton size="small" type="primary">确认</NButton></template>
                  确认该对账运行结果？
                </NPopconfirm>
              </td>
            </tr>
          </tbody>
        </NTable>
      </NSpin>
    </NCard>
  </NSpace>
</template>

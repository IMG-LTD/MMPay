<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { fetchReconciliationRuns, type ReconciliationRun } from '@/service/api/admin';

const props = defineProps<{ id: string }>();
const router = useRouter();
const loading = ref(false);
const errorMessage = ref('');
const runs = ref<ReconciliationRun[]>([]);
const run = computed(() => runs.value.find(item => String(item.id) === props.id) || null);

async function loadRuns() {
  loading.value = true;
  errorMessage.value = '';
  try {
    runs.value = (await fetchReconciliationRuns()).items;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Failed to load reconciliation run';
  } finally {
    loading.value = false;
  }
}

onMounted(loadRuns);
</script>

<template>
  <NSpace vertical :size="16">
    <NButton text type="primary" @click="router.back()">返回</NButton>
    <NAlert v-if="errorMessage" type="error" title="对账详情失败">{{ errorMessage }}</NAlert>
    <NCard :bordered="false" class="card-wrapper">
      <template #header>对账详情</template>
      <template #header-extra><NButton secondary :loading="loading" @click="loadRuns">刷新</NButton></template>
      <NDescriptions v-if="run" bordered :column="2">
        <NDescriptionsItem label="ID">{{ run.id }}</NDescriptionsItem>
        <NDescriptionsItem label="结果">
          <NTag :bordered="false" :aria-label="`reconciliation outcome ${run.outcome}`">{{ run.outcome }}</NTag>
        </NDescriptionsItem>
        <NDescriptionsItem label="日期">{{ run.run_date }}</NDescriptionsItem>
        <NDescriptionsItem label="通道">{{ run.channel_id }}</NDescriptionsItem>
        <NDescriptionsItem label="入账">{{ run.ingest_count }}</NDescriptionsItem>
        <NDescriptionsItem label="匹配">{{ run.matched_count }}</NDescriptionsItem>
        <NDescriptionsItem label="未匹配">{{ run.unmatched_count }}</NDescriptionsItem>
        <NDescriptionsItem label="确认状态">{{ run.ack_status }}</NDescriptionsItem>
        <NDescriptionsItem label="确认人">{{ run.ack_actor || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="确认时间">{{ run.ack_at || '-' }}</NDescriptionsItem>
      </NDescriptions>
      <NEmpty v-else description="未找到对账运行记录" />
    </NCard>
  </NSpace>
</template>

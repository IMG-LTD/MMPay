<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { fetchDeliveryLog, redispatchDeliveryLog, type DeliveryLog } from '@/service/api/admin';

const props = defineProps<{ id: string }>();
const router = useRouter();
const loading = ref(false);
const errorMessage = ref('');
const log = ref<DeliveryLog | null>(null);

async function loadLog() {
  loading.value = true;
  errorMessage.value = '';
  try {
    log.value = await fetchDeliveryLog(props.id);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Delivery log load failed';
  } finally {
    loading.value = false;
  }
}

async function redispatch() {
  errorMessage.value = '';
  try {
    log.value = await redispatchDeliveryLog(props.id);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Redispatch failed';
  }
}

onMounted(loadLog);
</script>

<template>
  <NSpace vertical :size="16">
    <NButton text type="primary" @click="router.back()">返回</NButton>
    <NAlert v-if="errorMessage" type="error" title="Delivery log 操作失败">{{ errorMessage }}</NAlert>
    <NCard :bordered="false" class="card-wrapper">
      <template #header>Delivery log</template>
      <template #header-extra>
        <NSpace>
          <NButton secondary :loading="loading" @click="loadLog">刷新</NButton>
          <NPopconfirm @positive-click="redispatch">
            <template #trigger><NButton type="warning">单条重投</NButton></template>
            确认重投该 webhook event？
          </NPopconfirm>
        </NSpace>
      </template>
      <NSpin :show="loading">
        <NDescriptions v-if="log" bordered :column="2">
          <NDescriptionsItem label="Log ID">{{ log.id }}</NDescriptionsItem>
          <NDescriptionsItem label="Integration">{{ log.integration_id }}</NDescriptionsItem>
          <NDescriptionsItem label="Payment Intent">{{ log.payment_intent_id }}</NDescriptionsItem>
          <NDescriptionsItem label="Event">{{ log.event_id }}</NDescriptionsItem>
          <NDescriptionsItem label="Attempt">{{ log.attempt }}</NDescriptionsItem>
          <NDescriptionsItem label="Response">{{ log.response_status || '-' }}</NDescriptionsItem>
          <NDescriptionsItem label="Scheduled">{{ log.scheduled_at }}</NDescriptionsItem>
          <NDescriptionsItem label="Next retry">{{ log.next_retry_at || '-' }}</NDescriptionsItem>
          <NDescriptionsItem label="Dead letter">{{ log.dead_letter ? 'yes' : 'no' }}</NDescriptionsItem>
        </NDescriptions>
      </NSpin>
    </NCard>
  </NSpace>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { bulkRedispatch, createWebhookIntegration, fetchDeliveryLogs, type BulkRedispatchResult, type DeliveryLog } from '@/service/api/admin';

const errorMessage = ref('');
const successMessage = ref('');
const loading = ref(false);
const integrationSubmitting = ref(false);
const redispatchResult = ref<BulkRedispatchResult | null>(null);
const deliveryLogs = ref<DeliveryLog[]>([]);
const integration = reactive({ id: '', display_name: '', target_url: '', secret_ref: '' });
const redispatch = reactive({ integration_id: '', event_count: 0, rps: 10 });

async function loadDeliveryLogs() {
  loading.value = true;
  errorMessage.value = '';
  try {
    deliveryLogs.value = (await fetchDeliveryLogs()).items;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Delivery log load failed';
  } finally {
    loading.value = false;
  }
}

async function submitIntegration() {
  if (integrationSubmitting.value) return;
  integrationSubmitting.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    await createWebhookIntegration({ ...integration });
    successMessage.value = 'Webhook upstream saved';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Webhook integration failed';
  } finally {
    integrationSubmitting.value = false;
  }
}

async function submitBulkRedispatch() {
  errorMessage.value = '';
  successMessage.value = '';
  try {
    redispatchResult.value = await bulkRedispatch({ ...redispatch });
    successMessage.value = 'Bulk redispatch accepted';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Bulk redispatch failed';
  }
}

onMounted(loadDeliveryLogs);
</script>

<template>
  <NSpace vertical :size="16">
    <NAlert v-if="errorMessage" type="error" title="Webhook-out 操作失败">{{ errorMessage }}</NAlert>
    <NAlert v-if="successMessage" type="success" title="Webhook-out 操作完成">{{ successMessage }}</NAlert>

    <NGrid :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
      <NGi span="24 l:12">
        <NCard :bordered="false" class="card-wrapper">
          <template #header>Webhook upstream</template>
          <NForm :model="integration" label-placement="top">
            <NFormItem label="Integration ID"><NInput v-model:value="integration.id" placeholder="wh_billing" /></NFormItem>
            <NFormItem label="显示名称">
              <NInput v-model:value="integration.display_name" placeholder="Billing subscriber" />
            </NFormItem>
            <NFormItem label="HTTPS 目标"><NInput v-model:value="integration.target_url" placeholder="https://example.com/hooks/mmpay" /></NFormItem>
            <NFormItem label="Secret ref"><NInput v-model:value="integration.secret_ref" placeholder="env://WEBHOOK_SECRET" /></NFormItem>
            <NButton type="primary" :loading="integrationSubmitting" :disabled="integrationSubmitting" @click="submitIntegration">保存 upstream</NButton>
          </NForm>
        </NCard>
      </NGi>

      <NGi span="24 l:12">
        <NCard :bordered="false" class="card-wrapper">
          <template #header>批量重投</template>
          <NForm :model="redispatch" label-placement="top">
            <NFormItem label="Integration ID"><NInput v-model:value="redispatch.integration_id" placeholder="wh_billing" /></NFormItem>
            <NFormItem label="事件数量"><NInputNumber v-model:value="redispatch.event_count" class="w-full" :min="0" :max="10000" /></NFormItem>
            <NFormItem label="RPS"><NInputNumber v-model:value="redispatch.rps" class="w-full" :min="1" /></NFormItem>
            <NPopconfirm @positive-click="submitBulkRedispatch">
              <template #trigger><NButton type="warning">重投</NButton></template>
              确认按当前过滤范围重投？最大 10000 个事件。
            </NPopconfirm>
          </NForm>
          <NAlert v-if="redispatchResult" class="mt-4" type="info" title="预计耗时" aria-live="polite">
            {{ redispatchResult.event_count }} events / {{ redispatchResult.rps }} RPS ≈
            {{ redispatchResult.estimated_drain_seconds }}s
          </NAlert>
        </NCard>
      </NGi>
    </NGrid>

    <NCard :bordered="false" class="card-wrapper">
      <template #header>Delivery logs</template>
      <template #header-extra><NButton secondary :loading="loading" @click="loadDeliveryLogs">刷新</NButton></template>
      <NSpin :show="loading">
        <NEmpty v-if="!deliveryLogs.length" description="暂无投递日志" />
        <NTable v-else :bordered="false" :single-line="false" size="small">
          <thead>
            <tr>
              <th>ID</th>
              <th>Integration</th>
              <th>Event</th>
              <th>Attempt</th>
              <th>状态</th>
              <th>下次重试</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in deliveryLogs" :key="log.id">
              <td>
                <RouterLink :to="{ name: 'webhook-out-delivery-log-detail', params: { id: log.id } }">{{ log.id }}</RouterLink>
              </td>
              <td>{{ log.integration_id }}</td>
              <td>{{ log.event_id }}</td>
              <td>{{ log.attempt }}</td>
              <td>
                <NTag :bordered="false" :aria-label="`webhook delivery ${log.dead_letter ? 'dead letter' : 'active'}`">
                  {{ log.dead_letter ? 'dead_letter' : 'active' }}
                </NTag>
              </td>
              <td>{{ log.next_retry_at || '-' }}</td>
            </tr>
          </tbody>
        </NTable>
      </NSpin>
    </NCard>
  </NSpace>
</template>

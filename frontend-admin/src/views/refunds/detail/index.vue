<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { fetchRefund, type Refund } from '@/service/api/admin';
import { formatMinor } from '@/utils/money';

const props = defineProps<{ id: string }>();
const router = useRouter();
const loading = ref(false);
const errorMessage = ref('');
const refund = ref<Refund | null>(null);

async function loadRefund() {
  loading.value = true;
  errorMessage.value = '';
  try {
    refund.value = await fetchRefund(props.id);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Failed to load refund';
  } finally {
    loading.value = false;
  }
}

onMounted(loadRefund);
</script>

<template>
  <NSpace vertical :size="16">
    <NButton text type="primary" @click="router.back()">返回</NButton>
    <NAlert v-if="errorMessage" type="error" title="退款详情失败">{{ errorMessage }}</NAlert>
    <NCard :bordered="false" class="card-wrapper">
      <template #header>退款详情</template>
      <template #header-extra><NButton secondary :loading="loading" @click="loadRefund">刷新</NButton></template>
      <NDescriptions v-if="refund" bordered :column="2">
        <NDescriptionsItem label="ID">{{ refund.id }}</NDescriptionsItem>
        <NDescriptionsItem label="状态">
          <NTag :bordered="false" :aria-label="`refund status ${refund.status}`">{{ refund.status }}</NTag>
        </NDescriptionsItem>
        <NDescriptionsItem label="支付意图">{{ refund.payment_intent_id }}</NDescriptionsItem>
        <NDescriptionsItem label="商户">{{ refund.merchant_id }}</NDescriptionsItem>
        <NDescriptionsItem label="金额">{{ formatMinor(refund.amount_minor, refund.currency) }}</NDescriptionsItem>
        <NDescriptionsItem label="申请时间">{{ refund.requested_at }}</NDescriptionsItem>
      </NDescriptions>
    </NCard>
  </NSpace>
</template>

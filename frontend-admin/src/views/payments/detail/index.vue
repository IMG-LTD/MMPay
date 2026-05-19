<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { cancelPaymentIntent, fetchPaymentIntent, type PaymentIntent } from '@/service/api/admin';
import { formatMinor } from '@/utils/money';

const props = defineProps<{ id: string }>();
const router = useRouter();
const loading = ref(false);
const errorMessage = ref('');
const intent = ref<PaymentIntent | null>(null);

async function loadIntent() {
  loading.value = true;
  errorMessage.value = '';
  try {
    intent.value = await fetchPaymentIntent(props.id);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Failed to load payment intent';
  } finally {
    loading.value = false;
  }
}

async function cancelIntent() {
  errorMessage.value = '';
  try {
    intent.value = await cancelPaymentIntent(props.id);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Cancel failed';
  }
}

onMounted(loadIntent);
</script>

<template>
  <NSpace vertical :size="16">
    <NButton text type="primary" @click="router.back()">返回</NButton>
    <NAlert v-if="errorMessage" type="error" title="支付意图失败">{{ errorMessage }}</NAlert>

    <NCard :bordered="false" class="card-wrapper">
      <template #header>支付意图详情</template>
      <template #header-extra>
        <NSpace>
          <NButton secondary :loading="loading" @click="loadIntent">刷新</NButton>
          <RouterLink v-if="intent" :to="{ name: 'refund-new', query: { payment_intent_id: intent.id } }">
            <NButton secondary type="primary">发起退款</NButton>
          </RouterLink>
          <NPopconfirm v-if="intent?.status === 'pending'" @positive-click="cancelIntent">
            <template #trigger><NButton type="warning">取消支付</NButton></template>
            确认取消该 pending 支付意图？
          </NPopconfirm>
        </NSpace>
      </template>
      <NSpin :show="loading">
        <NDescriptions v-if="intent" bordered :column="2" label-placement="left">
          <NDescriptionsItem label="ID">{{ intent.id }}</NDescriptionsItem>
          <NDescriptionsItem label="状态">
            <NTag :bordered="false" :aria-label="`payment status ${intent.status}`">{{ intent.status }}</NTag>
          </NDescriptionsItem>
          <NDescriptionsItem label="商户">{{ intent.merchant_id }}</NDescriptionsItem>
          <NDescriptionsItem label="通道">{{ intent.channel_id }}</NDescriptionsItem>
          <NDescriptionsItem label="金额">{{ formatMinor(intent.amount_minor, intent.currency) }}</NDescriptionsItem>
          <NDescriptionsItem label="Provider Order">{{ intent.provider_order_id || '-' }}</NDescriptionsItem>
          <NDescriptionsItem label="订单引用">{{ intent.order_ref }}</NDescriptionsItem>
          <NDescriptionsItem label="更新时间">{{ intent.updated_at }}</NDescriptionsItem>
        </NDescriptions>
      </NSpin>
    </NCard>
  </NSpace>
</template>

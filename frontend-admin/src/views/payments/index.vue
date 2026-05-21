<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { createPaymentIntent, fetchPaymentIntents, type PaymentIntent } from '@/service/api/admin';
import { formatMinor } from '@/utils/money';

const router = useRouter();
const loading = ref(false);
const submitting = ref(false);
const errorMessage = ref('');
const intents = ref<PaymentIntent[]>([]);
const form = reactive({ channel_id: '', amount_minor: 0, currency: 'CNY', order_ref: '' });

async function loadIntents() {
  loading.value = true;
  errorMessage.value = '';
  try {
    intents.value = (await fetchPaymentIntents()).items;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Failed to load payment intents';
  } finally {
    loading.value = false;
  }
}

async function submitIntent() {
  submitting.value = true;
  errorMessage.value = '';
  try {
    const intent = await createPaymentIntent({ ...form });
    await router.push({ name: 'payment-detail', params: { id: intent.id } });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Payment intent creation failed';
  } finally {
    submitting.value = false;
  }
}

onMounted(loadIntents);
</script>

<template>
  <NSpace vertical :size="16">
    <NAlert type="warning" title="Live provider calls disabled">
      payment-intent creation will return an explicit 503 until MMPAY_LIVE_PROVIDER_CALLS is enabled and a live client is wired.
    </NAlert>

    <NAlert v-if="errorMessage" type="error" title="支付操作失败">{{ errorMessage }}</NAlert>

    <NGrid :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
      <NGi span="24 l:16">
        <NCard :bordered="false" class="card-wrapper">
          <template #header>支付</template>
          <template #header-extra>
            <NButton :loading="loading" secondary type="primary" @click="loadIntents">刷新</NButton>
          </template>
          <NSpin :show="loading">
            <NEmpty v-if="!intents.length" description="暂无支付记录" />
            <NTable v-else :bordered="false" :single-line="false" size="small">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>通道</th>
                  <th>金额</th>
                  <th>状态</th>
                  <th>订单</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="intent in intents" :key="intent.id">
                  <td><RouterLink :to="{ name: 'payment-detail', params: { id: intent.id } }">{{ intent.id }}</RouterLink></td>
                  <td>{{ intent.channel_id }}</td>
                  <td>{{ formatMinor(intent.amount_minor, intent.currency) }}</td>
                  <td><NTag :bordered="false" :aria-label="`payment status ${intent.status}`">{{ intent.status }}</NTag></td>
                  <td>{{ intent.order_ref }}</td>
                </tr>
              </tbody>
            </NTable>
          </NSpin>
        </NCard>
      </NGi>

      <NGi span="24 l:8">
        <NCard :bordered="false" class="card-wrapper">
          <template #header>创建支付</template>
          <NForm :model="form" label-placement="top">
            <NFormItem label="通道 ID"><NInput v-model:value="form.channel_id" placeholder="ch_huifu" /></NFormItem>
            <NFormItem label="金额 minor"><NInputNumber v-model:value="form.amount_minor" class="w-full" :min="1" /></NFormItem>
            <NFormItem label="币种"><NInput v-model:value="form.currency" placeholder="CNY" /></NFormItem>
            <NFormItem label="订单引用"><NInput v-model:value="form.order_ref" placeholder="order-001" /></NFormItem>
            <NPopconfirm @positive-click="submitIntent">
              <template #trigger>
                <NButton block type="primary" :loading="submitting">创建</NButton>
              </template>
              当前预览默认关闭 live provider calls，将返回 503 用于验证失败路径。
            </NPopconfirm>
          </NForm>
        </NCard>
      </NGi>
    </NGrid>
  </NSpace>
</template>

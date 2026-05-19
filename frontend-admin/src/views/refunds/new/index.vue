<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { createRefund } from '@/service/api/admin';

const route = useRoute();
const router = useRouter();
const submitting = ref(false);
const errorMessage = ref('');
const form = reactive({ payment_intent_id: '', amount_minor: 0 });

async function submitRefund() {
  submitting.value = true;
  errorMessage.value = '';
  try {
    const refund = await createRefund({ ...form });
    await router.push({ name: 'refund-detail', params: { id: refund.id } });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Refund creation failed';
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  const intentId = route.query.payment_intent_id;
  if (typeof intentId === 'string') {
    form.payment_intent_id = intentId;
  }
});
</script>

<template>
  <NSpace vertical :size="16">
    <NButton text type="primary" @click="router.back()">返回</NButton>
    <NAlert v-if="errorMessage" type="error" title="退款创建失败">{{ errorMessage }}</NAlert>
    <NCard :bordered="false" class="card-wrapper">
      <template #header>新建退款</template>
      <NForm :model="form" label-placement="top">
        <NFormItem label="支付意图 ID"><NInput v-model:value="form.payment_intent_id" placeholder="pi_001" /></NFormItem>
        <NFormItem label="退款金额 minor"><NInputNumber v-model:value="form.amount_minor" class="w-full" :min="1" /></NFormItem>
        <NPopconfirm @positive-click="submitRefund">
          <template #trigger><NButton type="primary" :loading="submitting">提交退款</NButton></template>
          当前预览会执行上限校验，live provider calls 关闭时返回显式 503。
        </NPopconfirm>
      </NForm>
    </NCard>
  </NSpace>
</template>

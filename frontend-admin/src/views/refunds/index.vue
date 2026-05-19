<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { fetchRefunds, type Refund } from '@/service/api/admin';
import { formatMinor } from '@/utils/money';

const loading = ref(false);
const errorMessage = ref('');
const refunds = ref<Refund[]>([]);

async function loadRefunds() {
  loading.value = true;
  errorMessage.value = '';
  try {
    refunds.value = (await fetchRefunds()).items;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Failed to load refunds';
  } finally {
    loading.value = false;
  }
}

onMounted(loadRefunds);
</script>

<template>
  <NSpace vertical :size="16">
    <NAlert v-if="errorMessage" type="error" title="退款加载失败">{{ errorMessage }}</NAlert>
    <NCard :bordered="false" class="card-wrapper">
      <template #header>退款</template>
      <template #header-extra>
        <NSpace>
          <RouterLink :to="{ name: 'refund-new' }"><NButton type="primary">新建退款</NButton></RouterLink>
          <NButton secondary :loading="loading" @click="loadRefunds">刷新</NButton>
        </NSpace>
      </template>
      <NSpin :show="loading">
        <NEmpty v-if="!refunds.length" description="暂无退款" />
        <NTable v-else :bordered="false" :single-line="false" size="small">
          <thead>
            <tr>
              <th>ID</th>
              <th>支付意图</th>
              <th>金额</th>
              <th>状态</th>
              <th>时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="refund in refunds" :key="refund.id">
              <td><RouterLink :to="{ name: 'refund-detail', params: { id: refund.id } }">{{ refund.id }}</RouterLink></td>
              <td>{{ refund.payment_intent_id }}</td>
              <td>{{ formatMinor(refund.amount_minor, refund.currency) }}</td>
              <td><NTag :bordered="false" :aria-label="`refund status ${refund.status}`">{{ refund.status }}</NTag></td>
              <td>{{ refund.requested_at }}</td>
            </tr>
          </tbody>
        </NTable>
      </NSpin>
    </NCard>
  </NSpace>
</template>

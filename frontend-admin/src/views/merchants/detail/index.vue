<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { fetchMerchant, fetchMerchantChannels, type Channel, type Merchant } from '@/service/api/admin';
import { useAuthStore } from '@/store/modules/auth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const loading = ref(false);
const errorMessage = ref('');
const merchant = ref<Merchant | null>(null);
const channels = ref<Channel[]>([]);

const merchantId = computed(() => String(route.params.id));
const isAdmin = computed(() => authStore.isStaticSuper || authStore.userInfo.roles.some(role => ['ADMIN', 'admin', 'R_ADMIN'].includes(role)));

async function loadDetail() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const [merchantResult, channelResult] = await Promise.all([
      fetchMerchant(merchantId.value),
      fetchMerchantChannels(merchantId.value)
    ]);
    merchant.value = merchantResult;
    channels.value = channelResult.items;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Failed to load merchant';
  } finally {
    loading.value = false;
  }
}

function goCreateChannel() {
  router.push({ name: 'merchant-channel-new', params: { id: merchantId.value } });
}

onMounted(loadDetail);
</script>

<template>
  <NSpace vertical :size="16">
    <NAlert v-if="errorMessage" type="error" title="商户详情加载失败">{{ errorMessage }}</NAlert>

    <NCard :bordered="false" class="card-wrapper">
      <NSpin :show="loading">
        <div v-if="merchant" class="flex flex-col gap-4">
          <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
            <div>
              <h1 class="m-0 text-22px font-600">{{ merchant.display_name }}</h1>
              <p class="m-0 mt-2 text-14px text-#64748b">{{ merchant.id }}</p>
            </div>
            <NSpace>
              <NTag type="success" :bordered="false">{{ merchant.status }}</NTag>
              <NButton v-if="isAdmin" type="primary" secondary @click="goCreateChannel">
                新建通道
              </NButton>
            </NSpace>
          </div>

          <NGrid :x-gap="16" :y-gap="12" responsive="screen" item-responsive>
            <NGi span="24 m:12">
              <NCard size="small" :bordered="false">
                <template #header>凭据引用</template>
                <NSpace align="center">
                  <NTag :bordered="false">{{ merchant.credential_ref }}</NTag>
                  <NTag type="info" :bordered="false" :aria-label="`credential fingerprint ${merchant.credential_fingerprint}`">
                    {{ merchant.credential_fingerprint }}
                  </NTag>
                </NSpace>
              </NCard>
            </NGi>
          </NGrid>
        </div>
      </NSpin>
    </NCard>

    <NCard :bordered="false" class="card-wrapper">
      <template #header>支付通道</template>
      <NEmpty v-if="!channels.length" description="暂无通道" />
      <NTable v-else :bordered="false" :single-line="false" size="small">
        <thead>
          <tr>
            <th>通道 ID</th>
            <th>Provider</th>
            <th>状态</th>
            <th>凭据引用</th>
            <th>指纹</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="channel in channels" :key="channel.id">
            <td>
              <RouterLink :to="{ name: 'channel-detail', params: { id: channel.id } }">{{ channel.id }}</RouterLink>
            </td>
            <td>{{ channel.provider_code }}</td>
            <td><NTag type="success" :bordered="false">{{ channel.status }}</NTag></td>
            <td><NTag :bordered="false">{{ channel.credential_ref }}</NTag></td>
            <td>{{ channel.credential_fingerprint }}</td>
          </tr>
        </tbody>
      </NTable>
    </NCard>
  </NSpace>
</template>

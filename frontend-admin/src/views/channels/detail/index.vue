<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { fetchChannel, type Channel } from '@/service/api/admin';

const route = useRoute();
const loading = ref(false);
const errorMessage = ref('');
const channel = ref<Channel | null>(null);

async function loadChannel() {
  loading.value = true;
  errorMessage.value = '';
  try {
    channel.value = await fetchChannel(String(route.params.id));
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Failed to load channel';
  } finally {
    loading.value = false;
  }
}

onMounted(loadChannel);
</script>

<template>
  <NSpace vertical :size="16">
    <NAlert v-if="errorMessage" type="error" title="通道详情加载失败">{{ errorMessage }}</NAlert>
    <NCard :bordered="false" class="card-wrapper">
      <NSpin :show="loading">
        <div v-if="channel" class="flex flex-col gap-4">
          <div>
            <h1 class="m-0 text-22px font-600">{{ channel.display_name }}</h1>
            <p class="m-0 mt-2 text-14px text-#64748b">{{ channel.id }}</p>
          </div>
          <NGrid :x-gap="16" :y-gap="12" responsive="screen" item-responsive>
            <NGi span="24 m:8"><NStatistic label="Provider" :value="channel.provider_code" /></NGi>
            <NGi span="24 m:8"><NStatistic label="状态" :value="channel.status" /></NGi>
            <NGi span="24 m:8"><NStatistic label="商户" :value="channel.merchant_id" /></NGi>
          </NGrid>
          <NCard size="small" :bordered="false">
            <template #header>凭据绑定</template>
            <NSpace align="center">
              <NTag :bordered="false">{{ channel.credential_ref }}</NTag>
              <NTag type="info" :bordered="false" :aria-label="`credential fingerprint ${channel.credential_fingerprint}`">
                {{ channel.credential_fingerprint }}
              </NTag>
            </NSpace>
          </NCard>
        </div>
      </NSpin>
    </NCard>
  </NSpace>
</template>

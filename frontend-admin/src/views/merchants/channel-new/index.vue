<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { createChannel } from '@/service/api/admin';
import { useAuthStore } from '@/store/modules/auth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const submitting = ref(false);
const errorMessage = ref('');
const form = reactive({ id: '', display_name: '', provider_code: 'huifu', credential_ref: '' });
const merchantId = computed(() => String(route.params.id));
const isAdmin = computed(() => authStore.isStaticSuper || authStore.userInfo.roles.some(role => ['ADMIN', 'admin', 'R_ADMIN'].includes(role)));

async function submitChannel() {
  submitting.value = true;
  errorMessage.value = '';
  try {
    const channel = await createChannel(merchantId.value, { ...form });
    await router.push({ name: 'channel-detail', params: { id: channel.id } });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Failed to create channel';
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  if (!isAdmin.value) {
    router.replace({ name: 'merchant-detail', params: { id: merchantId.value } });
  }
});
</script>

<template>
  <NCard :bordered="false" class="card-wrapper">
    <template #header>新建汇付通道</template>
    <NAlert v-if="errorMessage" class="mb-4" type="error" title="通道创建失败">{{ errorMessage }}</NAlert>
    <NForm :model="form" label-placement="top">
      <NFormItem label="通道 ID">
        <NInput v-model:value="form.id" placeholder="ch_huifu_default" />
      </NFormItem>
      <NFormItem label="通道名称">
        <NInput v-model:value="form.display_name" placeholder="Huifu default" />
      </NFormItem>
      <NFormItem label="Provider">
        <NSelect v-model:value="form.provider_code" :options="[{ label: 'Huifu', value: 'huifu' }]" />
      </NFormItem>
      <NFormItem label="凭据引用">
        <NInput v-model:value="form.credential_ref" placeholder="env://HUIFU_CHANNEL_KEY" />
      </NFormItem>
      <NButton type="primary" :loading="submitting" @click="submitChannel">创建通道</NButton>
    </NForm>
  </NCard>
</template>

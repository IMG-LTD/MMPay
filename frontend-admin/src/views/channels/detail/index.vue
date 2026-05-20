<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  archiveChannel,
  fetchChannel,
  updateChannel,
  verifyChannelBinding,
  type Channel
} from '@/service/api/admin';
import { useAuthStore } from '@/store/modules/auth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const loading = ref(false);
const saving = ref(false);
const binding = ref(false);
const verifying = ref(false);
const archiving = ref(false);
const errorMessage = ref('');
const noticeMessage = ref('');
const channel = ref<Channel | null>(null);
const editForm = reactive({ display_name: '', status: 'active' });
const credentialForm = reactive({ credential_ref: '' });
const statusOptions = [
  { label: 'Active', value: 'active' },
  { label: 'Suspended', value: 'suspended' }
];

const channelId = computed(() => String(route.params.id));
const isAdmin = computed(() => authStore.isStaticSuper || authStore.userInfo.roles.some(isAdminRole));

async function loadChannel() {
  loading.value = true;
  clearMessages();
  try {
    applyChannel(await fetchChannel(channelId.value));
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Failed to load channel';
  } finally {
    loading.value = false;
  }
}

async function saveChannel() {
  saving.value = true;
  clearMessages();
  try {
    applyChannel(await updateChannel(channelId.value, { ...editForm }));
    noticeMessage.value = '通道信息已保存';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Failed to update channel';
  } finally {
    saving.value = false;
  }
}

async function bindCredential() {
  binding.value = true;
  clearMessages();
  try {
    applyChannel(
      await updateChannel(channelId.value, {
        credential_ref: { type: 'Set', value: credentialForm.credential_ref }
      })
    );
    noticeMessage.value = '凭据引用已更新';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Failed to bind credential';
  } finally {
    binding.value = false;
  }
}

async function unbindCredential() {
  binding.value = true;
  clearMessages();
  try {
    applyChannel(await updateChannel(channelId.value, { credential_ref: { type: 'Unbind' } }));
    noticeMessage.value = '凭据引用已解绑';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Failed to unbind credential';
  } finally {
    binding.value = false;
  }
}

async function verifyBinding() {
  verifying.value = true;
  clearMessages();
  try {
    const result = await verifyChannelBinding(channelId.value);
    noticeMessage.value = `凭据校验通过：${result.resolved_fingerprint}`;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Failed to verify credential';
  } finally {
    verifying.value = false;
  }
}

async function archiveCurrentChannel() {
  if (!channel.value) {
    return;
  }
  archiving.value = true;
  clearMessages();
  try {
    const merchantId = channel.value.merchant_id;
    await archiveChannel(channelId.value);
    await router.push({ name: 'merchant-detail', params: { id: merchantId } });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Failed to archive channel';
  } finally {
    archiving.value = false;
  }
}

function applyChannel(value: Channel) {
  channel.value = value;
  editForm.display_name = value.display_name;
  editForm.status = value.status;
  credentialForm.credential_ref = value.credential_ref ?? '';
}

function clearMessages() {
  errorMessage.value = '';
  noticeMessage.value = '';
}

function isAdminRole(role: string) {
  return role === 'ADMIN';
}

onMounted(loadChannel);
</script>

<template>
  <NSpace vertical :size="16">
    <NAlert v-if="errorMessage" type="error" title="通道操作失败">{{ errorMessage }}</NAlert>
    <NAlert v-if="noticeMessage" type="success" title="操作完成">{{ noticeMessage }}</NAlert>

    <NCard :bordered="false" class="card-wrapper">
      <NSpin :show="loading">
        <div v-if="channel" class="flex flex-col gap-4">
          <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
            <div>
              <h1 class="m-0 text-22px font-600">{{ channel.display_name }}</h1>
              <p class="m-0 mt-2 text-14px text-#64748b">{{ channel.id }}</p>
            </div>
            <NSpace>
              <NTag type="success" :bordered="false">{{ channel.status }}</NTag>
              <NButton secondary :loading="loading" @click="loadChannel">刷新</NButton>
            </NSpace>
          </div>

          <NGrid :x-gap="16" :y-gap="12" responsive="screen" item-responsive>
            <NGi span="24 m:8"><NStatistic label="Provider" :value="channel.provider_code" /></NGi>
            <NGi span="24 m:8"><NStatistic label="商户" :value="channel.merchant_id" /></NGi>
            <NGi span="24 m:8"><NStatistic label="凭据指纹" :value="channel.credential_fingerprint || '未生成'" /></NGi>
          </NGrid>
        </div>
      </NSpin>
    </NCard>

    <NGrid v-if="isAdmin && channel" :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
      <NGi span="24 l:12">
        <NCard :bordered="false" class="card-wrapper">
          <template #header>编辑通道</template>
          <NForm :model="editForm" label-placement="top">
            <NFormItem label="通道名称">
              <NInput v-model:value="editForm.display_name" placeholder="Huifu default" />
            </NFormItem>
            <NFormItem label="状态">
              <NSelect v-model:value="editForm.status" :options="statusOptions" />
            </NFormItem>
            <NSpace justify="space-between">
              <NButton type="primary" :loading="saving" @click="saveChannel">保存</NButton>
              <NButton type="error" secondary :loading="archiving" @click="archiveCurrentChannel">归档通道</NButton>
            </NSpace>
          </NForm>
        </NCard>
      </NGi>
      <NGi span="24 l:12">
        <NCard :bordered="false" class="card-wrapper">
          <template #header>凭据绑定</template>
          <NForm :model="credentialForm" label-placement="top">
            <NFormItem label="env:// 引用">
              <NInput v-model:value="credentialForm.credential_ref" placeholder="env://HUIFU_CHANNEL_KEY" />
            </NFormItem>
            <NSpace>
              <NButton type="primary" :loading="binding" @click="bindCredential">绑定/更新</NButton>
              <NButton secondary :loading="binding" @click="unbindCredential">解绑</NButton>
              <NButton secondary :loading="verifying" @click="verifyBinding">校验绑定</NButton>
            </NSpace>
          </NForm>
        </NCard>
      </NGi>
    </NGrid>
  </NSpace>
</template>

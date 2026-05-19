<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  archiveMerchant,
  fetchMerchant,
  fetchMerchantChannels,
  updateMerchant,
  verifyMerchantBinding,
  type Channel,
  type Merchant
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
const merchant = ref<Merchant | null>(null);
const channels = ref<Channel[]>([]);
const editForm = reactive({ display_name: '', status: 'active' });
const credentialForm = reactive({ credential_ref: '' });
const statusOptions = [
  { label: 'Active', value: 'active' },
  { label: 'Suspended', value: 'suspended' }
];

const merchantId = computed(() => String(route.params.id));
const isAdmin = computed(() => authStore.isStaticSuper || authStore.userInfo.roles.some(isAdminRole));

async function loadDetail() {
  loading.value = true;
  clearMessages();
  try {
    const [merchantResult, channelResult] = await Promise.all([
      fetchMerchant(merchantId.value),
      fetchMerchantChannels(merchantId.value)
    ]);
    applyMerchant(merchantResult);
    channels.value = channelResult.items;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Failed to load merchant';
  } finally {
    loading.value = false;
  }
}

async function saveMerchant() {
  saving.value = true;
  clearMessages();
  try {
    applyMerchant(await updateMerchant(merchantId.value, { ...editForm }));
    noticeMessage.value = '商户信息已保存';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Failed to update merchant';
  } finally {
    saving.value = false;
  }
}

async function bindCredential() {
  binding.value = true;
  clearMessages();
  try {
    applyMerchant(
      await updateMerchant(merchantId.value, {
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
    applyMerchant(await updateMerchant(merchantId.value, { credential_ref: { type: 'Unbind' } }));
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
    const result = await verifyMerchantBinding(merchantId.value);
    noticeMessage.value = `凭据校验通过：${result.resolved_fingerprint}`;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Failed to verify credential';
  } finally {
    verifying.value = false;
  }
}

async function archiveCurrentMerchant() {
  archiving.value = true;
  clearMessages();
  try {
    await archiveMerchant(merchantId.value);
    await router.push({ name: 'merchants' });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Failed to archive merchant';
  } finally {
    archiving.value = false;
  }
}

function applyMerchant(value: Merchant) {
  merchant.value = value;
  editForm.display_name = value.display_name;
  editForm.status = value.status;
  credentialForm.credential_ref = value.credential_ref ?? '';
}

function goCreateChannel() {
  router.push({ name: 'merchant-channel-new', params: { id: merchantId.value } });
}

function clearMessages() {
  errorMessage.value = '';
  noticeMessage.value = '';
}

function isAdminRole(role: string) {
  return ['ADMIN', 'admin', 'R_ADMIN'].includes(role);
}

onMounted(loadDetail);
</script>

<template>
  <NSpace vertical :size="16">
    <NAlert v-if="errorMessage" type="error" title="商户操作失败">{{ errorMessage }}</NAlert>
    <NAlert v-if="noticeMessage" type="success" title="操作完成">{{ noticeMessage }}</NAlert>

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
              <NButton v-if="isAdmin" type="primary" secondary @click="goCreateChannel">新建通道</NButton>
              <NButton secondary :loading="loading" @click="loadDetail">刷新</NButton>
            </NSpace>
          </div>

          <NGrid :x-gap="16" :y-gap="12" responsive="screen" item-responsive>
            <NGi span="24 m:8">
              <NStatistic label="商户 ID" :value="merchant.id" />
            </NGi>
            <NGi span="24 m:8">
              <NStatistic label="凭据引用" :value="merchant.credential_ref || '未绑定'" />
            </NGi>
            <NGi span="24 m:8">
              <NStatistic label="凭据指纹" :value="merchant.credential_fingerprint || '未生成'" />
            </NGi>
          </NGrid>
        </div>
      </NSpin>
    </NCard>

    <NGrid v-if="isAdmin && merchant" :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
      <NGi span="24 l:12">
        <NCard :bordered="false" class="card-wrapper">
          <template #header>编辑商户</template>
          <NForm :model="editForm" label-placement="top">
            <NFormItem label="商户名称">
              <NInput v-model:value="editForm.display_name" placeholder="Acme" />
            </NFormItem>
            <NFormItem label="状态">
              <NSelect v-model:value="editForm.status" :options="statusOptions" />
            </NFormItem>
            <NSpace justify="space-between">
              <NButton type="primary" :loading="saving" @click="saveMerchant">保存</NButton>
              <NButton type="error" secondary :loading="archiving" @click="archiveCurrentMerchant">归档商户</NButton>
            </NSpace>
          </NForm>
        </NCard>
      </NGi>
      <NGi span="24 l:12">
        <NCard :bordered="false" class="card-wrapper">
          <template #header>凭据绑定</template>
          <NForm :model="credentialForm" label-placement="top">
            <NFormItem label="env:// 引用">
              <NInput v-model:value="credentialForm.credential_ref" placeholder="env://HUIFU_MERCHANT_KEY" />
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
            <td><NTag :bordered="false">{{ channel.credential_ref || '未绑定' }}</NTag></td>
            <td>{{ channel.credential_fingerprint || '-' }}</td>
          </tr>
        </tbody>
      </NTable>
    </NCard>
  </NSpace>
</template>

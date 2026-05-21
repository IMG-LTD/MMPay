<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { createMerchant, fetchMerchants, type Merchant } from '@/service/api/admin';
import { useAuthStore } from '@/store/modules/auth';

const router = useRouter();
const authStore = useAuthStore();
const loading = ref(false);
const submitting = ref(false);
const errorMessage = ref('');
const merchants = ref<Merchant[]>([]);
const form = reactive({ id: '', display_name: '', credential_ref: '' });

const isAdmin = computed(() => authStore.isStaticSuper || authStore.userInfo.roles.some(isAdminRole));

async function loadMerchants() {
  loading.value = true;
  errorMessage.value = '';
  try {
    merchants.value = (await fetchMerchants()).items;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Failed to load merchants';
  } finally {
    loading.value = false;
  }
}

async function submitMerchant() {
  submitting.value = true;
  errorMessage.value = '';
  try {
    const merchant = await createMerchant({ ...form });
    Object.assign(form, { id: '', display_name: '', credential_ref: '' });
    await router.push({ name: 'merchant-detail', params: { id: merchant.id } });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Failed to create merchant';
  } finally {
    submitting.value = false;
  }
}

function isAdminRole(role: string) {
  return role === 'ADMIN';
}

onMounted(loadMerchants);
</script>

<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false" class="card-wrapper">
      <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 class="m-0 text-22px font-600">商户管理</h1>
          <p class="m-0 mt-2 text-14px text-#64748b">绑定 env:// 凭据引用，维护可用支付商户。</p>
        </div>
        <NButton :loading="loading" secondary type="primary" @click="loadMerchants">刷新</NButton>
      </div>
    </NCard>

    <NAlert v-if="errorMessage" type="error" title="商户操作失败">{{ errorMessage }}</NAlert>

    <NGrid :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
      <NGi span="24 l:16">
        <NCard :bordered="false" class="card-wrapper">
          <template #header>商户列表</template>
          <NSpin :show="loading">
            <NEmpty v-if="!merchants.length" :description="isAdmin ? '创建你的第一个商户' : '暂无商户，请联系管理员'">
              <template v-if="isAdmin" #extra>
                <NText depth="3">使用右侧"新建商户"表单开始</NText>
              </template>
            </NEmpty>
            <NTable v-else :bordered="false" :single-line="false" size="small">
              <thead>
                <tr>
                  <th>商户 ID</th>
                  <th>名称</th>
                  <th>状态</th>
                  <th>凭据引用</th>
                  <th>指纹</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="merchant in merchants" :key="merchant.id">
                  <td>
                    <RouterLink :to="{ name: 'merchant-detail', params: { id: merchant.id } }">
                      {{ merchant.id }}
                    </RouterLink>
                  </td>
                  <td>{{ merchant.display_name }}</td>
                  <td><NTag type="success" :bordered="false">{{ merchant.status }}</NTag></td>
                  <td>{{ merchant.credential_ref || '-' }}</td>
                  <td>
                    <NText
                      class="break-all text-12px font-mono"
                      :depth="merchant.credential_fingerprint ? 1 : 3"
                      :aria-label="`credential fingerprint ${merchant.credential_fingerprint}`"
                    >
                      {{ merchant.credential_fingerprint || '-' }}
                    </NText>
                  </td>
                </tr>
              </tbody>
            </NTable>
          </NSpin>
        </NCard>
      </NGi>

      <NGi span="24 l:8">
        <NCard v-if="isAdmin" :bordered="false" class="card-wrapper">
          <template #header>创建商户</template>
          <NForm :model="form" label-placement="top">
            <NFormItem label="商户 ID">
              <NInput v-model:value="form.id" placeholder="m_acme" />
            </NFormItem>
            <NFormItem label="商户名称">
              <NInput v-model:value="form.display_name" placeholder="Acme" />
            </NFormItem>
            <NFormItem label="凭据引用">
              <NInput v-model:value="form.credential_ref" placeholder="env://YOUR_REFERENCE_NAME" />
            </NFormItem>
            <NButton type="primary" block :loading="submitting" @click="submitMerchant">
              创建商户
            </NButton>
          </NForm>
        </NCard>
      </NGi>
    </NGrid>
  </NSpace>
</template>

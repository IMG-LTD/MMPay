<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import {
  createIntegration,
  fetchIntegrations,
  testIntegration,
  type Integration,
  type IntegrationCreateInput,
  type LicenseRelayLog
} from '@/service/api/admin';

const loading = ref(false);
const saving = ref(false);
const errorMessage = ref('');
const successMessage = ref('');
const integrations = ref<Integration[]>([]);
const lastSyntheticLog = ref<LicenseRelayLog | null>(null);
const form = reactive<IntegrationCreateInput>({
  id: '',
  kind: 'relay',
  name: '',
  slug: '',
  target_url: '',
  secret_ref: ''
});

async function loadIntegrations() {
  loading.value = true;
  errorMessage.value = '';
  try {
    integrations.value = (await fetchIntegrations()).items;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Integrations load failed';
  } finally {
    loading.value = false;
  }
}

async function submitIntegration() {
  saving.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    await createIntegration({ ...form });
    successMessage.value = 'Relay upstream saved';
    await loadIntegrations();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Integration save failed';
  } finally {
    saving.value = false;
  }
}

async function runSynthetic(id: string) {
  errorMessage.value = '';
  successMessage.value = '';
  try {
    lastSyntheticLog.value = await testIntegration(id);
    successMessage.value = 'Synthetic relay check accepted';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Synthetic relay check failed';
  }
}

onMounted(loadIntegrations);
</script>

<template>
  <NSpace vertical :size="16">
    <NAlert v-if="errorMessage" type="error" title="Integration operation failed">{{ errorMessage }}</NAlert>
    <NAlert v-if="successMessage" type="success" title="Integration operation completed">{{ successMessage }}</NAlert>

    <NGrid :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
      <NGi span="24 l:8">
        <NCard :bordered="false" class="card-wrapper">
          <template #header>Relay upstream</template>
          <NForm :model="form" label-placement="top">
            <NFormItem label="Target ID"><NInput v-model:value="form.id" placeholder="relay-prod" /></NFormItem>
            <NFormItem label="Name"><NInput v-model:value="form.name" placeholder="License vendor gateway" /></NFormItem>
            <NFormItem label="Slug"><NInput v-model:value="form.slug" placeholder="license-vendor" /></NFormItem>
            <NFormItem label="Target URL">
              <NInput v-model:value="form.target_url" placeholder="https://vendor.example.com/license" />
            </NFormItem>
            <NFormItem label="Secret ref">
              <NInput v-model:value="form.secret_ref" placeholder="env://RELAY_UPSTREAM_SECRET" />
            </NFormItem>
            <NButton type="primary" :loading="saving" @click="submitIntegration">Save upstream</NButton>
          </NForm>
        </NCard>
      </NGi>

      <NGi span="24 l:16">
        <NCard :bordered="false" class="card-wrapper">
          <template #header>Configured upstreams</template>
          <template #header-extra><NButton secondary :loading="loading" @click="loadIntegrations">Refresh</NButton></template>
          <NSpin :show="loading">
            <NEmpty v-if="!integrations.length" description="No relay upstreams" />
            <NTable v-else :bordered="false" :single-line="false" size="small">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Slug</th>
                  <th>Target</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in integrations" :key="item.id">
                  <td>
                    <RouterLink :to="{ name: 'integration-detail', params: { id: item.id } }">
                      {{ item.name }}
                    </RouterLink>
                  </td>
                  <td>{{ item.slug }}</td>
                  <td>{{ item.target_url_masked }}</td>
                  <td><NTag :bordered="false">{{ item.status }}</NTag></td>
                  <td><NButton tertiary size="small" @click="runSynthetic(item.id)">Test</NButton></td>
                </tr>
              </tbody>
            </NTable>
          </NSpin>
        </NCard>
      </NGi>
    </NGrid>

    <NCard v-if="lastSyntheticLog" :bordered="false" class="card-wrapper">
      <template #header>Latest synthetic result</template>
      <NDescriptions :column="3" bordered size="small">
        <NDescriptionsItem label="Log ID">{{ lastSyntheticLog.id }}</NDescriptionsItem>
        <NDescriptionsItem label="Attempt">{{ lastSyntheticLog.attempt }}</NDescriptionsItem>
        <NDescriptionsItem label="Synthetic">{{ lastSyntheticLog.synthetic ? 'yes' : 'no' }}</NDescriptionsItem>
      </NDescriptions>
    </NCard>
  </NSpace>
</template>

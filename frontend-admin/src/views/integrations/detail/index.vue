<script setup lang="ts">
import { onMounted, ref } from 'vue';
import {
  fetchIntegration,
  redispatchLicenseRelayLog,
  testIntegration,
  type Integration,
  type LicenseRelayLog
} from '@/service/api/admin';

const props = defineProps<{ id: string }>();
const integration = ref<Integration | null>(null);
const relayActivity = ref<LicenseRelayLog | null>(null);
const loading = ref(false);
const actionLoading = ref(false);
const errorMessage = ref('');
const successMessage = ref('');

async function loadIntegration() {
  loading.value = true;
  errorMessage.value = '';
  try {
    integration.value = await fetchIntegration(props.id);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Integration detail load failed';
  } finally {
    loading.value = false;
  }
}

async function runSyntheticTest() {
  actionLoading.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    relayActivity.value = await testIntegration(props.id);
    successMessage.value = 'Synthetic relay check accepted';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Synthetic relay check failed';
  } finally {
    actionLoading.value = false;
  }
}

async function redispatchLastLog() {
  if (!relayActivity.value) return;
  actionLoading.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    relayActivity.value = await redispatchLicenseRelayLog(relayActivity.value.id);
    successMessage.value = 'Relay log redispatch accepted';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Relay log redispatch failed';
  } finally {
    actionLoading.value = false;
  }
}

onMounted(loadIntegration);
</script>

<template>
  <NSpace vertical :size="16">
    <NAlert v-if="errorMessage" type="error" title="Integration operation failed">{{ errorMessage }}</NAlert>
    <NAlert v-if="successMessage" type="success" title="Integration operation completed">{{ successMessage }}</NAlert>

    <NCard :bordered="false" class="card-wrapper">
      <template #header>Integration Detail</template>
      <template #header-extra><NButton secondary :loading="loading" @click="loadIntegration">Refresh</NButton></template>
      <NSpin :show="loading">
        <NEmpty v-if="!integration" description="No integration detail" />
        <NDescriptions v-else :column="2" bordered label-placement="left">
          <NDescriptionsItem label="ID">{{ integration.id }}</NDescriptionsItem>
          <NDescriptionsItem label="Kind">{{ integration.kind }}</NDescriptionsItem>
          <NDescriptionsItem label="Name">{{ integration.name }}</NDescriptionsItem>
          <NDescriptionsItem label="Slug">{{ integration.slug }}</NDescriptionsItem>
          <NDescriptionsItem label="Target">{{ integration.target_url_masked }}</NDescriptionsItem>
          <NDescriptionsItem label="Status">{{ integration.status }}</NDescriptionsItem>
        </NDescriptions>
      </NSpin>
    </NCard>

    <NCard :bordered="false" class="card-wrapper">
      <template #header>Relay Activity</template>
      <template #header-extra>
        <NSpace>
          <NButton type="primary" :loading="actionLoading" @click="runSyntheticTest">Run test</NButton>
          <NButton secondary :disabled="!relayActivity" :loading="actionLoading" @click="redispatchLastLog">
            Redispatch
          </NButton>
        </NSpace>
      </template>
      <NEmpty v-if="!relayActivity" description="No relay activity selected" />
      <NDescriptions v-else :column="3" bordered size="small">
        <NDescriptionsItem label="Log ID">{{ relayActivity.id }}</NDescriptionsItem>
        <NDescriptionsItem label="Attempt">{{ relayActivity.attempt }}</NDescriptionsItem>
        <NDescriptionsItem label="Request">{{ relayActivity.request_id }}</NDescriptionsItem>
        <NDescriptionsItem label="Payload SHA">{{ relayActivity.payload_sha256 }}</NDescriptionsItem>
        <NDescriptionsItem label="HTTP">{{ relayActivity.http_status || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="Synthetic">{{ relayActivity.synthetic ? 'yes' : 'no' }}</NDescriptionsItem>
      </NDescriptions>
    </NCard>
  </NSpace>
</template>

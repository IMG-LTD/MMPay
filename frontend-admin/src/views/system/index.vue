<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useAuthStore } from '@/store/modules/auth';
import {
  createUser,
  deleteUser,
  fetchUsers,
  updateUser,
  type AdminUser,
  type UserCreateInput,
  type UserPatchInput
} from '@/service/api/admin';

const authStore = useAuthStore();
const isAdmin = computed(() => authStore.userInfo.roles.some(r => r === 'ADMIN'));

const loading = ref(false);
const submitting = ref(false);
const errorMessage = ref('');
const successMessage = ref('');
const users = ref<AdminUser[]>([]);

const createForm = reactive<UserCreateInput>({ username: '', password: '', role: 'ops' });
const editTarget = ref<AdminUser | null>(null);
const editForm = reactive<UserPatchInput>({ role: '', password: '' });

const ROLES = ['admin', 'ops', 'finance', 'auditor'];

async function loadUsers() {
  loading.value = true;
  errorMessage.value = '';
  try {
    users.value = await fetchUsers();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载用户失败';
  } finally {
    loading.value = false;
  }
}

async function submitCreate() {
  submitting.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    await createUser({ ...createForm });
    Object.assign(createForm, { username: '', password: '', role: 'ops' });
    successMessage.value = '用户已创建';
    await loadUsers();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '创建用户失败';
  } finally {
    submitting.value = false;
  }
}

function startEdit(user: AdminUser) {
  editTarget.value = user;
  editForm.role = user.role.toLowerCase();
  editForm.password = '';
}

function cancelEdit() {
  editTarget.value = null;
  editForm.role = '';
  editForm.password = '';
}

async function submitEdit() {
  if (!editTarget.value) return;
  submitting.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    const patch: UserPatchInput = {};
    if (editForm.role) patch.role = editForm.role;
    if (editForm.password) patch.password = editForm.password;
    await updateUser(editTarget.value.username, patch);
    successMessage.value = '用户已更新';
    cancelEdit();
    await loadUsers();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '更新用户失败';
  } finally {
    submitting.value = false;
  }
}

async function confirmDelete(username: string) {
  if (!window.confirm(`确认删除用户 "${username}"？此操作不可撤销。`)) return;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    await deleteUser(username);
    successMessage.value = `用户 ${username} 已删除`;
    await loadUsers();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '删除用户失败';
  }
}

function roleTag(role: string): 'error' | 'warning' | 'info' | 'success' | 'default' {
  const map: Record<string, 'error' | 'warning' | 'info' | 'success' | 'default'> = {
    admin: 'error',
    ops: 'warning',
    finance: 'success',
    auditor: 'info'
  };
  return map[role.toLowerCase()] ?? 'default';
}

onMounted(loadUsers);
</script>

<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false" class="card-wrapper">
      <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 class="m-0 text-22px font-600">系统管理</h1>
          <p class="m-0 mt-2 text-14px text-#64748b">管理后台用户账号与角色分配。</p>
        </div>
        <NButton :loading="loading" secondary type="primary" @click="loadUsers">刷新</NButton>
      </div>
    </NCard>

    <NAlert v-if="errorMessage" type="error" title="操作失败" closable @close="errorMessage = ''">
      {{ errorMessage }}
    </NAlert>
    <NAlert v-if="successMessage" type="success" title="操作成功" closable @close="successMessage = ''">
      {{ successMessage }}
    </NAlert>

    <NGrid :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
      <NGi span="24 l:16">
        <NCard :bordered="false" class="card-wrapper">
          <template #header>用户列表</template>
          <NSpin :show="loading">
            <NEmpty v-if="!users.length" description="暂无用户" />
            <NTable v-else :bordered="false" :single-line="false" size="small">
              <thead>
                <tr>
                  <th>用户名</th>
                  <th>角色</th>
                  <th>创建时间</th>
                  <th v-if="isAdmin">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="user in users" :key="user.username">
                  <td>{{ user.username }}</td>
                  <td>
                    <NTag :type="roleTag(user.role)" :bordered="false" size="small">
                      {{ user.role.toLowerCase() }}
                    </NTag>
                  </td>
                  <td class="text-12px text-#64748b">
                    {{ user.created_at ? new Date(user.created_at).toLocaleString('zh-CN') : '—' }}
                  </td>
                  <td v-if="isAdmin">
                    <NSpace :size="8">
                      <NButton
                        tertiary
                        size="small"
                        :disabled="editTarget?.username === user.username"
                        @click="startEdit(user)"
                      >
                        编辑
                      </NButton>
                      <NButton
                        tertiary
                        size="small"
                        type="error"
                        :disabled="user.username === authStore.userInfo.userName"
                        @click="confirmDelete(user.username)"
                      >
                        删除
                      </NButton>
                    </NSpace>
                  </td>
                </tr>
              </tbody>
            </NTable>
          </NSpin>
        </NCard>
      </NGi>

      <NGi span="24 l:8">
        <NCard v-if="isAdmin && editTarget" :bordered="false" class="card-wrapper">
          <template #header>编辑用户：{{ editTarget.username }}</template>
          <NForm label-placement="top">
            <NFormItem label="角色">
              <NSelect v-model:value="editForm.role" :options="ROLES.map(r => ({ label: r, value: r }))" />
            </NFormItem>
            <NFormItem label="新密码（留空则不修改）">
              <NInput
                v-model:value="editForm.password"
                type="password"
                show-password-on="click"
                placeholder="至少 8 位"
              />
            </NFormItem>
            <NSpace>
              <NButton type="primary" :loading="submitting" @click="submitEdit">保存</NButton>
              <NButton secondary @click="cancelEdit">取消</NButton>
            </NSpace>
          </NForm>
        </NCard>

        <NCard v-if="isAdmin && !editTarget" :bordered="false" class="card-wrapper">
          <template #header>新建用户</template>
          <NForm :model="createForm" label-placement="top">
            <NFormItem label="用户名">
              <NInput v-model:value="createForm.username" placeholder="ops.alice" />
            </NFormItem>
            <NFormItem label="初始密码">
              <NInput
                v-model:value="createForm.password"
                type="password"
                show-password-on="click"
                placeholder="至少 8 位"
              />
            </NFormItem>
            <NFormItem label="角色">
              <NSelect
                v-model:value="createForm.role"
                :options="ROLES.map(r => ({ label: r, value: r }))"
              />
            </NFormItem>
            <NButton type="primary" block :loading="submitting" @click="submitCreate">
              创建用户
            </NButton>
          </NForm>
        </NCard>

        <NCard :bordered="false" class="card-wrapper mt-4">
          <template #header>角色说明</template>
          <NDescriptions :column="1" size="small">
            <NDescriptionsItem label="admin">
              <NTag type="error" :bordered="false" size="small">admin</NTag>
              全部权限，含用户管理
            </NDescriptionsItem>
            <NDescriptionsItem label="ops">
              <NTag type="warning" :bordered="false" size="small">ops</NTag>
              运营：商户、通道、支付、退款
            </NDescriptionsItem>
            <NDescriptionsItem label="finance">
              <NTag type="success" :bordered="false" size="small">finance</NTag>
              财务：对账、退款查看
            </NDescriptionsItem>
            <NDescriptionsItem label="auditor">
              <NTag type="info" :bordered="false" size="small">auditor</NTag>
              审计：只读审计日志
            </NDescriptionsItem>
          </NDescriptions>
        </NCard>
      </NGi>
    </NGrid>
  </NSpace>
</template>

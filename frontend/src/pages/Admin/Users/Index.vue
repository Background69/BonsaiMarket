<template>
  <AdminLayout>
    <div class="management-page">
      <header><h1>Người dùng</h1><p>Tìm kiếm, xem hồ sơ và khóa hoặc mở khóa tài khoản.</p></header>
      <form class="management-toolbar" @submit.prevent="apply">
        <label>Tìm người dùng<input v-model="term" :disabled="loading" maxlength="100" placeholder="Tên hoặc email" type="search"/></label>
        <label>Vai trò
          <select v-model="role" :disabled="loading">
            <option value="">Tất cả</option>
            <option v-for="(label, key) in roleLabels" :key="key" :value="key">{{ label }}</option>
          </select>
        </label>
        <label>Trạng thái
          <select v-model="active" :disabled="loading">
            <option value="">Tất cả</option>
            <option value="true">Đang hoạt động</option>
            <option value="false">Đã khóa</option>
          </select>
        </label>
        <button :disabled="loading" class="button button-primary">Lọc</button>
        <button :disabled="loading" class="button button-outline" type="button" @click="clear">Xóa bộ lọc</button>
      </form>
      <FormFeedback :error="error" :success="success" prefix="users"/>
      <ManagementTable :columns="columns" :error="listError" :loading="loading" :rows="data.content" caption="Người dùng: tên, vai trò, trạng thái và thao tác"
                       empty-title="Không có người dùng phù hợp." @retry="load">
        <template #name="{row}">{{ row.name }}<small>{{ row.email }}</small></template>
        <template #phone="{row}">{{ row.phone || '—' }}</template>
        <template #role="{row}">{{ roleLabels[row.role] || row.role }}</template>
        <template #active="{row}"><span class="management-status">{{ row.active ? 'Đang hoạt động' : 'Đã khóa' }}</span></template>
        <template #createdAt="{row}">{{ date(row.createdAt) }}</template>
        <template #actions="{row}">
          <div class="management-actions">
            <RouterLink :aria-label="'Xem người dùng ' + row.name" :to="'/admin/users/' + row.id" class="button button-outline">Xem</RouterLink>
            <button v-if="row.id !== currentId" :aria-label="(row.active ? 'Khóa tài khoản ' : 'Mở khóa tài khoản ') + row.name" :disabled="busy"
                    class="button button-outline" type="button" @click="toggle(row)">{{ row.active ? 'Khóa' : 'Mở khóa' }}</button>
          </div>
        </template>
      </ManagementTable>
      <ManagementPagination v-if="!listError" :busy="loading || busy" :count="data.content.length" :loading="loading" :page="data.page" :size="data.size"
                            :total-elements="data.totalElements" :total-pages="data.totalPages" noun="người dùng"/>
    </div>
  </AdminLayout>
</template>
<script setup>
import {computed, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import AdminLayout from '../../../layouts/AdminLayout.vue';
import FormFeedback from '../../../components/marketplace/FormFeedback.vue';
import ManagementTable from '../../../components/marketplace/ManagementTable.vue';
import ManagementPagination from '../../../components/marketplace/ManagementPagination.vue';
import {useServerList} from '../../../composables/useServerList.js';
import {useAuth} from '../../../composables/useAuth.js';
import {marketplaceRequest} from '../../../services/marketplace.js';

const route = useRoute(), router = useRouter(), {user} = useAuth();
const roleLabels = {ADMIN: 'Quản trị viên', SELLER: 'Người bán', CUSTOMER: 'Khách hàng'};
const columns = [
  {key: 'name', label: 'Người dùng'}, {key: 'phone', label: 'Điện thoại'}, {key: 'role', label: 'Vai trò'},
  {key: 'active', label: 'Trạng thái'}, {key: 'createdAt', label: 'Ngày tạo'}, {key: 'actions', label: 'Thao tác'},
];
const term = ref(''), role = ref(''), active = ref(''), busy = ref(false), error = ref(''), success = ref('');
const currentId = computed(() => user.value?.id);
const {data, loading, error: listError, load} = useServerList('/admin/users', q => ({search: q.search || '', role: q.role || '', active: q.active || ''}));
watch(() => route.query, q => {
  term.value = q.search || '';
  role.value = q.role || '';
  active.value = q.active || '';
}, {immediate: true});

const date = v => v ? new Date(v).toLocaleDateString('vi-VN') : '—';

function apply() {
  const query = {};
  if (term.value.trim()) query.search = term.value.trim();
  if (role.value) query.role = role.value;
  if (active.value) query.active = active.value;
  router.push({path: route.path, query});
}

function clear() {
  router.push({path: route.path, query: {}});
}

async function toggle(row) {
  if (busy.value) return;
  const next = !row.active;
  if (!window.confirm((next ? 'Mở khóa' : 'Khóa') + ' tài khoản “' + row.name + '”?' + (next ? '' : ' Người dùng sẽ bị đăng xuất và không thể đăng nhập.'))) return;
  busy.value = true;
  error.value = '';
  success.value = '';
  try {
    await marketplaceRequest('/admin/users/' + row.id + '/active', 'POST', {active: next});
    success.value = next ? 'Đã mở khóa tài khoản.' : 'Đã khóa tài khoản.';
    await load();
  } catch (e) {
    error.value = e.message || 'Không thể cập nhật tài khoản. Vui lòng thử lại.';
  } finally {
    busy.value = false;
  }
}
</script>

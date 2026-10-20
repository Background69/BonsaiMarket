<template>
  <AdminLayout>
    <div class="management-page">
      <header>
        <p><RouterLink to="/admin/users">← Danh sách người dùng</RouterLink></p>
        <h1>{{ detail?.user.name || 'Chi tiết người dùng' }}</h1>
      </header>
      <p v-if="loading && !detail" role="status">Đang tải người dùng…</p>
      <div v-else-if="loadError" class="management-error" role="alert">
        <p>{{ loadError }}</p>
        <button class="button button-outline" type="button" @click="load">Thử lại</button>
      </div>
      <template v-else-if="detail">
        <FormFeedback :error="error" :success="success" prefix="user"/>
        <div class="user-detail-grid">
          <section class="market-panel">
            <div class="section-heading"><h2>Hồ sơ</h2><span class="management-status">{{ detail.user.active ? 'Đang hoạt động' : 'Đã khóa' }}</span></div>
            <dl class="user-facts">
              <div><dt>Email</dt><dd>{{ detail.user.email }}</dd></div>
              <div><dt>Xác minh email</dt><dd>{{ detail.user.emailVerified ? 'Đã xác minh' : 'Chưa xác minh' }}</dd></div>
              <div><dt>Điện thoại</dt><dd>{{ detail.user.phone || '—' }}</dd></div>
              <div><dt>Vai trò</dt><dd>{{ roleLabels[detail.user.role] || detail.user.role }}</dd></div>
              <div><dt>Ngày tạo</dt><dd>{{ dateTime(detail.user.createdAt) }}</dd></div>
              <div v-if="detail.store"><dt>Gian hàng</dt><dd>{{ detail.store.name }}</dd></div>
              <div><dt>Số đơn đã mua</dt><dd>{{ detail.orderCount }}</dd></div>
            </dl>
            <div v-if="detail.user.id !== currentId" class="management-actions">
              <button :disabled="busy" class="button button-outline" type="button" @click="toggle">{{ detail.user.active ? 'Khóa tài khoản' : 'Mở khóa tài khoản' }}</button>
            </div>
          </section>
          <section class="market-panel">
            <div class="section-heading"><h2>Địa chỉ giao hàng</h2></div>
            <p v-if="!detail.addresses.length" class="muted">Người dùng chưa lưu địa chỉ.</p>
            <ul v-else class="user-addresses">
              <li v-for="a in detail.addresses" :key="a.id">
                <strong>{{ a.recipient }}</strong><span v-if="a.isDefault"> · Mặc định</span>
                <div>{{ a.phone }}</div><div>{{ a.text }}</div>
              </li>
            </ul>
          </section>
        </div>
        <section class="market-panel">
          <div class="section-heading"><h2>Đơn hàng gần đây</h2></div>
          <ManagementTable :columns="columns" :rows="detail.recentOrders" caption="Đơn hàng gần đây của người dùng" empty-title="Người dùng chưa có đơn hàng.">
            <template #code="{row}"><RouterLink :to="'/admin/orders/' + row.id">{{ row.code }}</RouterLink></template>
            <template #storeName="{row}">{{ row.storeName }}</template>
            <template #total="{row}">{{ money(row.total) }}</template>
            <template #status="{row}"><span class="management-status">{{ statusLabel(row.status.toLowerCase()) }}</span></template>
            <template #createdAt="{row}">{{ dateTime(row.createdAt) }}</template>
          </ManagementTable>
        </section>
      </template>
    </div>
  </AdminLayout>
</template>
<script setup>
import {computed, onBeforeUnmount, ref, watch} from 'vue';
import {useRoute} from 'vue-router';
import AdminLayout from '../../../layouts/AdminLayout.vue';
import FormFeedback from '../../../components/marketplace/FormFeedback.vue';
import ManagementTable from '../../../components/marketplace/ManagementTable.vue';
import {useAuth} from '../../../composables/useAuth.js';
import {marketplaceRequest} from '../../../services/marketplace.js';
import {formatPrice, statusLabel} from '../../../utils.js';

const route = useRoute(), {user} = useAuth();
const roleLabels = {ADMIN: 'Quản trị viên', SELLER: 'Người bán', CUSTOMER: 'Khách hàng'};
const columns = [
  {key: 'code', label: 'Mã đơn'}, {key: 'storeName', label: 'Gian hàng'}, {key: 'total', label: 'Tổng tiền', numeric: true},
  {key: 'status', label: 'Trạng thái'}, {key: 'createdAt', label: 'Ngày đặt'},
];
const detail = ref(null), loading = ref(true), loadError = ref(''), error = ref(''), success = ref(''), busy = ref(false);
const currentId = computed(() => user.value?.id);
const money = v => formatPrice(v) + ' ₫';
const dateTime = v => v ? new Date(v).toLocaleString('vi-VN') : '—';
let sequence = 0;

async function load() {
  if (!route.params.id) return;
  const id = ++sequence;
  loading.value = true;
  loadError.value = '';
  try {
    const result = await marketplaceRequest('/admin/users/' + encodeURIComponent(route.params.id));
    if (id === sequence) detail.value = result;
  } catch (e) {
    if (id === sequence) {
      detail.value = null;
      loadError.value = e.status === 404 ? 'Không tìm thấy người dùng.' : (e.message || 'Không thể tải người dùng. Vui lòng thử lại.');
    }
  } finally {
    if (id === sequence) loading.value = false;
  }
}

async function toggle() {
  if (busy.value) return;
  const next = !detail.value.user.active;
  if (!window.confirm((next ? 'Mở khóa' : 'Khóa') + ' tài khoản này?' + (next ? '' : ' Người dùng sẽ bị đăng xuất và không thể đăng nhập.'))) return;
  busy.value = true;
  error.value = '';
  success.value = '';
  try {
    const updated = await marketplaceRequest('/admin/users/' + detail.value.user.id + '/active', 'POST', {active: next});
    detail.value = {...detail.value, user: updated};
    success.value = next ? 'Đã mở khóa tài khoản.' : 'Đã khóa tài khoản.';
  } catch (e) {
    error.value = e.message || 'Không thể cập nhật tài khoản. Vui lòng thử lại.';
  } finally {
    busy.value = false;
  }
}

watch(() => route.params.id, load, {immediate: true});
onBeforeUnmount(() => sequence++);
</script>
<style scoped>
.user-detail-grid {display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px; margin-bottom: 20px;}
.user-facts {display: grid; gap: 10px; margin: 0 0 16px;}
.user-facts > div {display: flex; justify-content: space-between; gap: 16px;}
.user-facts dt {color: var(--bm-muted);}
.user-facts dd {margin: 0; text-align: right; overflow-wrap: anywhere;}
.user-addresses {margin: 0; padding-left: 18px; display: grid; gap: 12px;}
</style>

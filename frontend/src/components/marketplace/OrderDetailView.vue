<template>
  <div class="management-page">
    <header>
      <p><RouterLink :to="listPath">← Danh sách đơn hàng</RouterLink></p>
      <h1>Đơn hàng {{ order?.code || '' }}</h1>
    </header>
    <p v-if="loading && !order" role="status">Đang tải đơn hàng…</p>
    <div v-else-if="loadError" class="management-error" role="alert">
      <p>{{ loadError }}</p>
      <button class="button button-outline" type="button" @click="load">Thử lại</button>
    </div>
    <template v-else-if="order">
      <FormFeedback :error="error" :success="success" prefix="order"/>
      <div class="order-detail-grid">
        <section class="market-panel">
          <div class="section-heading"><h2>Thông tin đơn hàng</h2><span class="management-status">{{ statusLabel(order.status.toLowerCase()) }}</span></div>
          <dl class="order-facts">
            <div><dt>Mã đơn</dt><dd>{{ order.code }}</dd></div>
            <div><dt>Ngày đặt</dt><dd>{{ dateTime(order.createdAt) }}</dd></div>
            <div><dt>Cập nhật lần cuối</dt><dd>{{ dateTime(order.updatedAt) }}</dd></div>
            <div><dt>Gian hàng</dt><dd>{{ order.storeName }}</dd></div>
            <div><dt>Khách hàng</dt><dd>{{ order.buyer?.name || '—' }}</dd></div>
            <div v-if="order.buyer?.email"><dt>Email</dt><dd>{{ order.buyer.email }}</dd></div>
            <div v-if="order.buyer?.phone"><dt>Điện thoại</dt><dd>{{ order.buyer.phone }}</dd></div>
            <div><dt>Vận chuyển</dt><dd>{{ order.shippingMethod || '—' }}</dd></div>
          </dl>
          <h3>Địa chỉ giao hàng</h3>
          <ul v-if="addressLines.length" class="order-address"><li v-for="(line, i) in addressLines" :key="i">{{ line }}</li></ul>
          <p v-else class="muted">Không có thông tin địa chỉ.</p>
          <template v-if="order.notes"><h3>Ghi chú</h3><p>{{ order.notes }}</p></template>
        </section>
        <section class="market-panel">
          <div class="section-heading"><h2>Thanh toán</h2></div>
          <dl class="order-facts">
            <div><dt>Tạm tính</dt><dd>{{ money(order.subtotal) }}</dd></div>
            <div><dt>Phí vận chuyển</dt><dd>{{ money(order.shippingFee) }}</dd></div>
            <div><dt>Tổng cộng</dt><dd><strong>{{ money(order.total) }}</strong></dd></div>
          </dl>
          <h3>Xử lý đơn hàng</h3>
          <div v-if="order.actions.length" class="management-actions">
            <button v-for="action in order.actions" :key="action" :class="action === 'CANCELLED' ? 'button button-outline' : 'button button-primary'"
                    :disabled="busy" type="button" @click="change(action)">{{ actionLabels[action] || action }}</button>
          </div>
          <p v-else class="muted">Đơn hàng ở trạng thái này không còn thao tác nào.</p>
        </section>
      </div>
      <section class="market-panel order-items">
        <div class="section-heading"><h2>Sản phẩm trong đơn</h2></div>
        <ManagementTable :columns="columns" :rows="order.items" caption="Sản phẩm trong đơn hàng" empty-title="Đơn hàng chưa có sản phẩm.">
          <template #name="{row}">{{ row.name }}</template>
          <template #unitPrice="{row}">{{ money(row.unitPrice) }}</template>
          <template #quantity="{row}">{{ row.quantity }}</template>
          <template #lineTotal="{row}">{{ money(row.lineTotal) }}</template>
        </ManagementTable>
      </section>
    </template>
  </div>
</template>
<script setup>
import {computed, onBeforeUnmount, ref, watch} from 'vue';
import {useRoute} from 'vue-router';
import FormFeedback from './FormFeedback.vue';
import ManagementTable from './ManagementTable.vue';
import {marketplaceRequest} from '../../services/marketplace.js';
import {formatPrice, statusLabel} from '../../utils.js';

const props = defineProps({endpoint: {type: String, required: true}, listPath: {type: String, required: true}});
const route = useRoute();
const order = ref(null), loading = ref(true), loadError = ref(''), error = ref(''), success = ref(''), busy = ref(false);
const columns = [
  {key: 'name', label: 'Sản phẩm'}, {key: 'unitPrice', label: 'Đơn giá', numeric: true},
  {key: 'quantity', label: 'Số lượng', numeric: true}, {key: 'lineTotal', label: 'Thành tiền', numeric: true},
];
const actionLabels = {
  CONFIRMED: 'Xác nhận đơn', SHIPPING: 'Giao cho vận chuyển', DELIVERED: 'Xác nhận đã giao', COMPLETED: 'Hoàn tất đơn', CANCELLED: 'Hủy đơn',
};
const money = v => formatPrice(v) + ' ₫';
const dateTime = v => v ? new Date(v).toLocaleString('vi-VN') : '—';
let sequence = 0;

// The shipping address is stored as a snapshot: JSON when written by checkout, plain text otherwise.
const addressLines = computed(() => {
  const raw = order.value?.shippingAddress;
  if (!raw) return [];
  try {
    const value = JSON.parse(raw);
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      const pick = (...keys) => keys.map(k => value[k]).find(v => typeof v === 'string' && v.trim());
      return [pick('recipient', 'recipientName', 'name'), pick('phone'), pick('detail', 'addressDetail'), pick('ward', 'wardName'),
        pick('district', 'districtName'), pick('province', 'provinceName')].filter(Boolean);
    }
  } catch { /* Plain-text snapshot. */ }
  return [raw];
});

async function load() {
  if (!route.params.id) return;
  const id = ++sequence;
  loading.value = true;
  loadError.value = '';
  try {
    const result = await marketplaceRequest(props.endpoint + '/' + encodeURIComponent(route.params.id));
    if (id === sequence) order.value = result;
  } catch (e) {
    if (id === sequence) {
      order.value = null;
      loadError.value = e.status === 404 ? 'Không tìm thấy đơn hàng.' : (e.message || 'Không thể tải đơn hàng. Vui lòng thử lại.');
    }
  } finally {
    if (id === sequence) loading.value = false;
  }
}

async function change(action) {
  if (busy.value) return;
  if (action === 'CANCELLED' && !window.confirm('Hủy đơn hàng này? Hàng đã trừ kho sẽ được hoàn lại.')) return;
  busy.value = true;
  error.value = '';
  success.value = '';
  try {
    order.value = await marketplaceRequest(props.endpoint + '/' + encodeURIComponent(order.value.id) + '/status', 'POST', {status: action});
    success.value = 'Đã cập nhật trạng thái đơn hàng.';
  } catch (e) {
    error.value = e.message || 'Không thể cập nhật đơn hàng. Vui lòng thử lại.';
    if (e.status === 409) await load();
  } finally {
    busy.value = false;
  }
}

watch(() => route.params.id, load, {immediate: true});
onBeforeUnmount(() => sequence++);
</script>
<style scoped>
.order-detail-grid {display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px; margin-bottom: 20px;}
.order-facts {display: grid; gap: 10px; margin: 0 0 16px;}
.order-facts > div {display: flex; justify-content: space-between; gap: 16px;}
.order-facts dt {color: var(--bm-muted);}
.order-facts dd {margin: 0; text-align: right; overflow-wrap: anywhere;}
.order-address {margin: 0 0 16px; padding-left: 18px;}
.order-detail-grid h3 {margin: 16px 0 8px; font-size: 15px;}
</style>

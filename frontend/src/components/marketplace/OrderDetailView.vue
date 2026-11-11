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
      <p><RouterLink class="button button-outline" :to="endpoint.startsWith('/admin')?'/admin/disputes':'/support-cases?order='+order.id">{{ endpoint.startsWith('/admin')?'Xem hồ sơ tranh chấp':'Hỗ trợ / tranh chấp đơn hàng' }}</RouterLink></p>
      <FormFeedback :error="error" :success="success" prefix="order"/>
      <div class="order-detail-grid">
        <section class="market-panel">
          <div class="section-heading"><h2>Thông tin đơn hàng</h2><span class="management-status">{{ statusLabel(order.status) }}</span></div>
          <dl class="order-facts">
            <div><dt>Mã đơn</dt><dd>{{ order.code }}</dd></div>
            <div><dt>Ngày đặt</dt><dd>{{ dateTime(order.createdAt) }}</dd></div>
            <div><dt>Cập nhật lần cuối</dt><dd>{{ dateTime(order.updatedAt) }}</dd></div>
            <div><dt>Gian hàng</dt><dd>{{ order.storeName }}</dd></div>
            <div><dt>Khách hàng</dt><dd>{{ order.buyer?.name || '—' }}<CustomerReportDialog v-if="endpoint==='/seller/orders'" :key="order.id" :order-id="order.id"/></dd></div>
            <div v-if="order.buyer?.email"><dt>Email</dt><dd>{{ order.buyer.email }}</dd></div>
            <div v-if="order.buyer?.phone"><dt>Điện thoại</dt><dd>{{ order.buyer.phone }}</dd></div>
            <div><dt>Vận chuyển</dt><dd>{{ labelFor({GHN:'Giao hàng qua GHN',STANDARD:'Giao hàng tiêu chuẩn'},order.shippingMethod,'Phương thức cần kiểm tra') }}</dd></div>
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
            <template v-if="order.payment"><div><dt>Phương thức</dt><dd>Thanh toán khi nhận hàng (COD)</dd></div><div><dt>Thu hộ tiền hàng</dt><dd>{{ money(order.payment.codAmount) }}</dd></div><div><dt>Bên trả cước</dt><dd>Người nhận · {{ money(order.payment.shippingFee) }}</dd></div><div><dt>Thanh toán</dt><dd>{{ order.payment.status==='CANCELLED'?'Đã hủy yêu cầu thu':order.payment.status==='REVIEW_REQUIRED'?'Cần đối chiếu':'Chờ thu tiền' }}</dd></div><div><dt>Quyết toán</dt><dd>Chưa xác nhận tiền đã về tài khoản</dd></div></template>
          </dl>
          <h3>Xử lý đơn hàng</h3>
          <p v-for="blocker in order.actionBlockers" :key="blocker.status" class="order-action-blocker">{{ blocker.message }}</p>
          <button type="button" class="button button-outline" :disabled="busy || loading" @click="load">{{ loading ? 'Đang tải…' : 'Cập nhật dữ liệu đơn' }}</button>
          <p class="muted">Chỉ hủy trước khi giao. Tồn kho đã trừ sẽ được hoàn lại trong cùng giao dịch.</p>
          <p v-if="order.payment" class="order-payment-note">COD chưa được coi là đã thu hoặc quyết toán. Giao thành công không xác nhận tiền đã về tài khoản. Hoàn tiền cần bằng chứng và tích hợp riêng.</p>
          <p v-else class="order-payment-note">Thanh toán trực tuyến và hồ sơ hoàn tiền được đối chiếu riêng. Hủy đơn không xác nhận đã hoàn tiền; xem hồ sơ bên dưới để theo dõi.</p>
          <div v-if="order.actions.length" class="management-actions">
            <button v-for="action in order.actions" :key="action" :class="action === 'CANCELLED' ? 'button button-outline' : 'button button-primary'"
                    :disabled="busy" type="button" @click="change(action)">{{ labelFor(actionLabels,action,'Cập nhật đơn hàng') }}</button>
          </div>
          <p v-else-if="!order.actionBlockers?.length" class="muted">Đơn hàng ở trạng thái này không còn thao tác nào.</p>
        </section>
      </div>
      <OrderHistory :entries="order.history"/>
      <OrderRefundPanel :key="'refund-'+order.id" :endpoint="endpoint" :order-id="order.id"/>
      <OrderShipping :key="order.id" :endpoint="endpoint" :order-id="order.id" :status="order.status" :buyer="buyer"/>
      <OrderReturnPanel :key="'return-'+order.id" :endpoint="endpoint" :order-id="order.id" :status="order.status" :buyer="buyer"/>
      <section class="market-panel order-items">
        <div class="section-heading"><h2>Sản phẩm trong đơn</h2></div>
        <ManagementTable :columns="columns" :rows="order.items" caption="Sản phẩm trong đơn hàng" empty-title="Đơn hàng chưa có sản phẩm.">
          <template #name="{row}">{{ row.name }}</template>
          <template #unitPrice="{row}">{{ money(row.unitPrice) }}</template>
          <template #quantity="{row}">{{ row.quantity }}</template>
          <template #lineTotal="{row}">{{ money(row.lineTotal) }}</template>
        </ManagementTable>
      </section>
      <p v-if="buyer && order.status === 'COMPLETED'" class="market-panel">Bạn có thể đánh giá từng sản phẩm đã mua:
        <span v-for="item in order.items" :key="item.id"> <RouterLink :to="'/products/' + item.productId + '#product-reviews'">{{ item.name }}</RouterLink></span>
      </p>
    </template>
    <OrderCancelDialog v-model:open="cancelOpen" :code="order?.code" :busy="busy" :error="error" @confirm="reason=>change('CANCELLED',true,reason)"/>
  </div>
</template>
<script setup>
import CustomerReportDialog from './CustomerReportDialog.vue';
import {labelFor} from '../../services/presentation.js';

import {computed, onBeforeUnmount, ref, watch} from 'vue';
import {useRoute} from 'vue-router';
import FormFeedback from './FormFeedback.vue';
import ManagementTable from './ManagementTable.vue';
import OrderShipping from './OrderShipping.vue';
import OrderReturnPanel from './OrderReturnPanel.vue';
import OrderHistory from './OrderHistory.vue';
import OrderRefundPanel from './OrderRefundPanel.vue';
import OrderCancelDialog from './OrderCancelDialog.vue';
import {marketplaceRequest} from '../../services/marketplace.js';
import {formatPrice, statusLabel} from '../../utils.js';
import {useAuth} from '../../composables/useAuth.js';

const props = defineProps({endpoint: {type: String, required: true}, listPath: {type: String, required: true}, buyer: Boolean});
const route = useRoute();
const {user} = useAuth();
const cancelOpen = ref(false);
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

// Địa chỉ giao hàng là snapshot: checkout lưu JSON, dữ liệu cũ có thể là văn bản.
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
  } catch { /* Snapshot dạng văn bản thuần. */ }
  return [raw];
});

async function load() {
  if (!route.params.id) return;
  const id = ++sequence;
  const orderId = String(route.params.id), endpoint = props.endpoint;
  order.value = null; busy.value = false; error.value = ''; success.value = '';
  cancelOpen.value=false;
  loading.value = true;
  loadError.value = '';
  try {
    const result = await marketplaceRequest(endpoint + '/' + encodeURIComponent(orderId));
    if (id === sequence && String(result.id) === orderId) order.value = result;
  } catch (e) {
    if (id === sequence) {
      order.value = null;
      loadError.value = e.status === 404 ? 'Không tìm thấy đơn hàng.' : (e.message || 'Không thể tải đơn hàng. Vui lòng thử lại.');
    }
  } finally {
    if (id === sequence) loading.value = false;
  }
}

async function change(action, confirmed = false, reason = '') {
  if (busy.value || !order.value || !order.value.actions.includes(action)) return;
  if (action === 'CANCELLED' && !confirmed) {
    error.value='';cancelOpen.value=true;
    return;
  }
  const ticket = sequence, orderId = String(order.value.id), endpoint = props.endpoint;
  busy.value = true;
  error.value = '';
  success.value = '';
  try {
    const result = await marketplaceRequest(endpoint + '/' + encodeURIComponent(orderId) + (props.buyer ? '/cancel' : '/status'), 'POST', props.buyer ? {reason} : {status: action,reason}, false);
    if (ticket !== sequence || orderId !== String(route.params.id) || String(result.id) !== orderId) return;
    order.value = result;
    cancelOpen.value=false;
    success.value = action === 'CANCELLED' ? 'Đã hủy đơn. Tồn kho đã trừ được hoàn lại. Chưa xác nhận hoàn tiền.' : 'Đã cập nhật trạng thái đơn hàng.';
  } catch (e) {
    if (ticket !== sequence) return;
    error.value = e.message || 'Không thể cập nhật đơn hàng. Vui lòng thử lại.';
    if (e.status === 409 && action!=='CANCELLED') {
      const text = error.value;
      const refreshSequence = sequence + 1;
      await load();
      if (refreshSequence === sequence && endpoint === props.endpoint && orderId === String(route.params.id)) error.value = text;
    }
  } finally {
    if (ticket === sequence) busy.value = false;
  }
}

watch([() => route.params.id, () => props.endpoint, () => user.value?.id], load, {immediate: true});
onBeforeUnmount(() => sequence++);
</script>
<style scoped>
.order-detail-grid {display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 300px), 1fr)); gap: 20px; margin-bottom: 20px;}
.order-detail-grid > section {min-width:0;overflow-wrap:anywhere;}
.order-payment-note {font-size:14px;line-height:1.6;color:var(--bm-muted);margin-block:16px;}
.order-action-blocker {font-size:var(--bm-type-label);line-height:1.6;color:var(--bm-warning);}
.order-cancel-dialog {width:min(520px,calc(100% - 32px));max-height:calc(100dvh - 32px);overflow:auto;padding:24px;border:1px solid var(--bm-border);border-radius:var(--bm-radius);background:var(--bm-surface);color:var(--bm-ink);line-height:1.6;}
.order-cancel-dialog::backdrop {background:rgb(11 61 43 / .5);}
.order-cancel-dialog h2 {font-size:var(--bm-type-section);margin:0 0 12px;overflow-wrap:anywhere;}
.order-cancel-dialog .management-actions {margin-top:24px;}
.order-cancel-button {background:var(--bm-error);color:var(--bm-surface);border-color:var(--bm-error);}
.order-cancel-dialog :focus-visible {outline:3px solid var(--bm-forest);outline-offset:3px;}
.order-facts {display: grid; gap: 10px; margin: 0 0 16px;}
.order-facts > div {display: flex; justify-content: space-between; gap: 16px;}
.order-facts dt {color: var(--bm-muted);}
.order-facts dd {margin: 0; text-align: right; overflow-wrap: anywhere;}
.order-address {margin: 0 0 16px; padding-left: 18px;}
.order-detail-grid h3 {margin: 16px 0 8px; font-size: 15px;}
</style>

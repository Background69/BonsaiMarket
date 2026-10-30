<template>
  <MarketplaceLayout>
    <main class="market-container market-page buyer-page">
      <!-- Đường dẫn phân cấp và nút quay lại -->
      <div class="breadcrumb-nav">
        <RouterLink to="/orders" class="back-link">
          <span class="back-arrow">&larr;</span> Quay lại danh sách đơn hàng
        </RouterLink>
      </div>

      <!-- Trạng thái tải -->
      <div v-if="loading" class="state-container" role="status" aria-busy="true">
        <div class="spinner"></div>
        <p>Đang tải chi tiết đơn hàng...</p>
      </div>

      <!-- Trạng thái lỗi -->
      <div v-else-if="error" class="error-banner" role="alert">
        <p>{{ error }}</p>
        <RouterLink to="/orders" class="btn btn-secondary btn-sm">Về danh sách đơn</RouterLink>
      </div>

      <!-- Chi tiết đơn hàng -->
      <div v-else-if="order" class="order-detail-wrapper">
        <!-- Thông tin đầu đơn -->
        <section class="detail-header-card">
          <div class="header-left">
            <h1 class="order-main-code">Đơn hàng #{{ order.code }}</h1>
            <p class="order-time-text">Ngày đặt hàng: {{ formatDate(order.createdAt) }}</p>
          </div>
          <div class="header-right">
            <span class="status-pill" :class="statusClass(order.status)">
              {{ statusLabel(order.status) }}
            </span>
          </div>
        </section>

        <!-- Tiến độ và dòng thời gian -->
        <section class="tracker-card">
          <div class="tracker-steps">
            <div class="step-item" :class="{ active: isStepActive('PENDING'), done: isStepDone('PENDING') }">
              <div class="step-bullet">1</div>
              <span class="step-label">Đặt hàng</span>
            </div>
            <div class="step-line" :class="{ done: isStepDone('PENDING') }"></div>
            <div class="step-item" :class="{ active: isStepActive('CONFIRMED'), done: isStepDone('CONFIRMED') }">
              <div class="step-bullet">2</div>
              <span class="step-label">Xác nhận</span>
            </div>
            <div class="step-line" :class="{ done: isStepDone('CONFIRMED') }"></div>
            <div class="step-item" :class="{ active: isStepActive('SHIPPING'), done: isStepDone('SHIPPING') }">
              <div class="step-bullet">3</div>
              <span class="step-label">Đang giao</span>
            </div>
            <div class="step-line" :class="{ done: isStepDone('SHIPPING') }"></div>
            <div class="step-item" :class="{ active: isStepActive('DELIVERED') || isStepActive('COMPLETED'), done: isStepDone('DELIVERED') }">
              <div class="step-bullet">4</div>
              <span class="step-label">{{ order.status === 'CANCELLED' ? 'Đã hủy' : 'Hoàn tất' }}</span>
            </div>
          </div>
        </section>

        <!-- Nội dung chính hai cột -->
        <div class="detail-grid">
          <!-- Cột trái -->
          <div class="detail-left">
            <!-- Sản phẩm -->
            <section class="detail-section">
              <header class="section-heading">
                <h2>Danh sách cây cảnh & bonsai</h2>
                <span class="store-tag">{{ order.storeName || 'Gian hàng' }}</span>
              </header>
              <div class="items-table">
                <div v-for="item in order.items" :key="item.id" class="item-detail-row">
                  <div class="item-thumb-wrapper">
                    <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.productName" class="item-thumb" />
                    <div v-else class="item-thumb-placeholder">
                      {{ (item.productName || 'B').charAt(0).toUpperCase() }}
                    </div>
                  </div>
                  <div class="item-info-col">
                    <h3 class="product-title"><RouterLink v-if="item.productId" :to="'/products/'+item.productId">{{ item.productName }}</RouterLink><template v-else>{{ item.productName }}</template></h3>
                    <RouterLink v-if="order.status === 'COMPLETED' && item.productId" class="order-review-link" :to="'/products/'+item.productId+'#product-reviews'">Đánh giá sản phẩm</RouterLink>
                    <p class="unit-price-text">Đơn giá: {{ formatMoney(item.unitPrice) }}</p>
                  </div>
                  <div class="item-qty-col">
                    <span>x{{ item.quantity }}</span>
                  </div>
                  <div class="item-total-col">
                    <strong>{{ formatMoney(item.lineTotal || item.unitPrice * item.quantity) }}</strong>
                  </div>
                </div>
              </div>
            </section>

            <!-- Thông tin giao hàng -->
            <section class="detail-section">
              <header class="section-heading">
                <h2>Địa chỉ nhận hàng & Giao vận</h2>
              </header>
              <div class="info-content-box">
                <div class="info-row">
                  <span class="info-label">Người nhận:</span>
                  <span class="info-value">{{ shippingAddress.recipientName || shippingAddress.recipient || 'Khách hàng' }}</span>
                </div>
                <div class="info-row" v-if="shippingAddress.phone">
                  <span class="info-label">Số điện thoại:</span>
                  <span class="info-value">{{ shippingAddress.phone }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">Địa chỉ:</span>
                  <span class="info-value">{{ formattedAddress }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">Phương thức:</span>
                  <span class="info-value">{{ order.shippingMethod || 'Giao hàng tiêu chuẩn' }}</span>
                </div>
                <div v-if="order.notes" class="info-row">
                  <span class="info-label">Ghi chú:</span>
                  <span class="info-value font-italic">"{{ order.notes }}"</span>
                </div>
              </div>
            </section>

            <!-- Thanh toán và giao dịch -->
            <section class="detail-section" v-if="transactions.length">
              <header class="section-heading">
                <h2>Lịch sử thanh toán trực tuyến</h2>
              </header>
              <div class="transactions-list">
                <div v-for="t in transactions" :key="t.id" class="txn-row">
                  <div class="txn-info">
                    <strong>{{ t.paymentMethod }}</strong> - Mã GD: {{ t.transactionCode }}
                    <small class="muted-text block">{{ formatDate(t.createdAt) }}</small>
                  </div>
                  <div class="txn-meta">
                    <span class="status-pill" :class="t.status === 'SUCCESS' ? 'status-delivered' : 'status-pending'">
                      {{ t.status === 'SUCCESS' ? 'Thành công' : t.status }}
                    </span>
                    <span class="txn-amount">{{ formatMoney(t.amount) }}</span>
                  </div>
                </div>
              </div>
            </section>
          </div>

          <!-- Cột phải: tổng hợp và thao tác -->
          <div class="detail-right">
            <section class="summary-card">
              <header class="section-heading">
                <h2>Tổng kết đơn hàng</h2>
              </header>
              <div class="summary-rows">
                <div class="calc-row">
                  <span>Tiền sản phẩm:</span>
                  <span>{{ formatMoney(order.subtotal) }}</span>
                </div>
                <div class="calc-row">
                  <span>Phí vận chuyển:</span>
                  <span>{{ formatMoney(order.shippingFee) }}</span>
                </div>
                <div class="calc-divider"></div>
                <div class="calc-row total-calc-row">
                  <span>Tổng thanh toán:</span>
                  <strong class="total-big-val">{{ formatMoney(order.total) }}</strong>
                </div>
              </div>

              <!-- Nút thao tác -->
              <div class="action-buttons-stack">
                <RouterLink
                  v-if="order.canPay"
                  :to="'/payment?orderId=' + order.id"
                  class="btn btn-primary btn-block"
                >
                  Thanh toán trực tuyến ngay
                </RouterLink>
                <button
                  v-if="order.canCancel"
                  type="button"
                  class="btn btn-danger-outline btn-block"
                  @click="showCancelModal = true"
                >
                  Hủy đơn hàng này
                </button>
                <RouterLink to="/products" class="btn btn-outline btn-block">
                  Tiếp tục mua bonsai khác
                </RouterLink>
              </div>
            </section>
          </div>
        </div>
      </div>

      <p v-if="transactionError" role="alert">{{ transactionError }} <button type="button" class="button button-outline" @click="loadDetail">Thử tải lại</button></p>
      <template v-if="order && !loading && !error">
        <p v-if="order.payment" class="order-payment-note">{{ order.payment.method }} · {{ order.payment.status === 'CANCELLED' ? 'Đã hủy yêu cầu thu hộ.' : 'Chưa có xác nhận thu tiền hoặc tiền về tài khoản.' }}</p>
        <OrderShipping endpoint="/account/orders" :order-id="Number(order.id)" :status="order.status" buyer/>
        <OrderReturnPanel endpoint="/account/orders" :order-id="Number(order.id)" :status="order.status" buyer/>
      </template>
      <OrderCancelDialog v-model:open="showCancelModal" :code="order?.code" :busy="cancelSubmitting" :error="cancelError" @confirm="submitCancel"/>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue';
import { useRoute, RouterLink } from 'vue-router';
import MarketplaceLayout from '../../../layouts/MarketplaceLayout.vue';
import { fetchMyOrderDetail, cancelMyOrder } from '../../../services/orders.js';
import OrderCancelDialog from '../../../components/marketplace/OrderCancelDialog.vue';
import OrderShipping from '../../../components/marketplace/OrderShipping.vue';
import OrderReturnPanel from '../../../components/marketplace/OrderReturnPanel.vue';
import { fetchOrderTransactions } from '../../../services/payment.js';

const route = useRoute();
const orderId = computed(() => route.params.id);

const order = ref(null);
const transactions = ref([]), transactionError = ref(''); let sequence=0;
const loading = ref(true);
const error = ref('');

const showCancelModal = ref(false);
const cancelError = ref('');
watch(showCancelModal,open=>{if(open)cancelError.value='';});
const cancelSubmitting = ref(false);

const formatMoney = (val) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);

function formatDate(isoStr) {
  if (!isoStr) return '';
  const d = new Date(isoStr);
  return d.toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
}

function statusLabel(status) {
  switch (status) {
    case 'PENDING': return 'Chờ xử lý';
    case 'CONFIRMED': return 'Đã xác nhận';
    case 'SHIPPING': return 'Đang vận chuyển';
    case 'DELIVERED': return 'Đã giao hàng';
    case 'COMPLETED': return 'Hoàn thành';
    case 'CANCELLED': return 'Đã hủy';
    default: return status || 'Không rõ';
  }
}

function statusClass(status) {
  switch (status) {
    case 'PENDING': return 'status-pending';
    case 'CONFIRMED': return 'status-confirmed';
    case 'SHIPPING': return 'status-shipping';
    case 'DELIVERED': return 'status-delivered';
    case 'COMPLETED': return 'status-completed';
    case 'CANCELLED': return 'status-cancelled';
    default: return '';
  }
}

const shippingAddress = computed(() => {
  if (!order.value?.shippingAddressJson) return {};
  try {
    return typeof order.value.shippingAddressJson === 'object'
      ? order.value.shippingAddressJson
      : JSON.parse(order.value.shippingAddressJson);
  } catch {
    return { detail: order.value.shippingAddressJson };
  }
});

const formattedAddress = computed(() => {
  const addr = shippingAddress.value;
  return [
    addr.addressDetail || addr.detail,
    addr.wardName || addr.ward,
    addr.districtName || addr.district,
    addr.provinceName || addr.province
  ].filter(Boolean).join(', ') || 'Chưa cập nhật địa chỉ';
});

const stepOrder = ['PENDING', 'CONFIRMED', 'SHIPPING', 'DELIVERED', 'COMPLETED'];

function isStepActive(stepName) {
  return order.value?.status === stepName;
}

function isStepDone(stepName) {
  if (!order.value) return false;
  const currentIdx = stepOrder.indexOf(order.value.status);
  const stepIdx = stepOrder.indexOf(stepName);
  return currentIdx > stepIdx;
}

async function loadDetail() {
  const ticket=++sequence, id=orderId.value; order.value=null;transactions.value=[];transactionError.value='';
  loading.value = true;
  error.value = '';
  try {
    const result=await fetchMyOrderDetail(id);if(ticket!==sequence)return;order.value=result;
    try {
      const result=await fetchOrderTransactions(id);if(ticket===sequence)transactions.value=result;
    } catch (e) {
      if(ticket===sequence)transactionError.value=e.message || 'Không thể tải lịch sử giao dịch.';
    }
  } catch (err) {
    if(ticket===sequence)error.value = err.message || 'Không thể tải chi tiết đơn hàng.';
  } finally {
    if(ticket===sequence)loading.value = false;
  }
}

async function submitCancel(reason) {
  if(cancelSubmitting.value)return;
  const ticket=sequence,id=orderId.value;
  cancelSubmitting.value = true;
  try {
    const result=await cancelMyOrder(id,reason);if(ticket!==sequence)return;order.value=result;
    showCancelModal.value = false;
  } catch (err) {
    if(ticket===sequence)cancelError.value=err.message || 'Không thể hủy đơn hàng. Vui lòng thử lại.';
  } finally {
    cancelSubmitting.value = false;
  }
}

watch(orderId,()=>{showCancelModal.value=false;cancelSubmitting.value=false;loadDetail();},{immediate:true});
onBeforeUnmount(()=>sequence++);
</script>

<style scoped>
.buyer-page {
  max-width: 1080px;
  margin: 0 auto;
  padding: 24px 16px 64px;
}
.breadcrumb-nav {
  margin-bottom: 20px;
}
.back-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--bm-muted);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: color 0.2s;
}
.back-link:hover {
  color: var(--bm-dark);
}

/* Đầu trang chi tiết */
.detail-header-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--bm-surface);
  padding: 20px 24px;
  border-radius: var(--bm-radius);
  border: 1px solid var(--bm-border);
  margin-bottom: 20px;
  box-shadow: none;
}
.order-main-code {
  font-size: var(--bm-type-task);
  font-weight: 700;
  margin: 0 0 4px;
  color: var(--bm-ink);
}
.order-time-text {
  font-size: 13px;
  color: var(--bm-muted);
  margin: 0;
}

/* Theo dõi tiến độ */
.tracker-card {
  background: var(--bm-surface);
  padding: 24px;
  border-radius: var(--bm-radius);
  border: 1px solid var(--bm-border);
  margin-bottom: 24px;
}
.tracker-steps {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 720px;
  margin: 0 auto;
}
.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  z-index: 1;
}
.step-bullet {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--bm-soft);
  color: var(--bm-muted);
  display: grid;
  place-items: center;
  font-weight: 700;
  font-size: 13px;
  transition: background-color var(--bm-motion-fast), color var(--bm-motion-fast);
}
.step-item.active .step-bullet {
  background: var(--bm-forest);
  color: var(--bm-surface);
  box-shadow: none;
}
.step-item.done .step-bullet {
  background: var(--bm-forest);
  color: var(--bm-surface);
}
.step-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--bm-muted);
}
.step-item.active .step-label {
  color: var(--bm-dark);
  font-weight: 700;
}
.step-line {
  flex: 1;
  height: 3px;
  background: var(--bm-soft);
  margin: 0 8px -20px;
}
.step-line.done {
  background: var(--bm-forest);
}

/* Lưới hai cột */
.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 24px;
  align-items: start;
}
@media (max-width: 860px) {
  .detail-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

.detail-section {
  background: var(--bm-surface);
  border-radius: var(--bm-radius);
  border: 1px solid var(--bm-border);
  padding: 20px 24px;
  margin-bottom: 24px;
}
.section-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--bm-border);
}
.section-heading h2 {
  font-size: 16px;
  font-weight: 700;
  margin: 0;
  color: var(--bm-ink);
}
.store-tag {
  font-size: 12px;
  color: var(--bm-forest);
  background: var(--bm-soft);
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 600;
}

/* Bảng sản phẩm */
.items-table {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.item-detail-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 0;
  border-bottom: 1px solid var(--bm-soft);
}
.item-detail-row:last-child {
  border-bottom: none;
}
.item-thumb-wrapper {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  overflow: hidden;
  background: var(--bm-soft);
  flex-shrink: 0;
}
.item-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.item-thumb-placeholder {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  font-weight: 700;
  color: var(--bm-muted);
  background: var(--bm-soft);
}
.item-info-col {
  flex: 1;
  min-width: 0;
}
.product-title {
  font-size: var(--bm-type-body);
  font-weight: 600;
  margin: 0 0 4px;
  color: var(--bm-ink);
}
.unit-price-text {
  font-size: 13px;
  color: var(--bm-muted);
  margin: 0;
}
.item-qty-col {
  font-size: 14px;
  font-weight: 600;
  color: var(--bm-muted);
  padding: 0 12px;
}
.item-total-col {
  font-size: 14px;
  color: var(--bm-ink);
  text-align: right;
  min-width: 90px;
}

/* Khối thông tin */
.info-content-box {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.info-row {
  display: flex;
  gap: 12px;
  font-size: 14px;
}
.info-label {
  width: 120px;
  flex-shrink: 0;
  color: var(--bm-muted);
}
.info-value {
  color: var(--bm-ink);
  font-weight: 500;
}

/* Tóm tắt đơn */
.summary-card {
  background: var(--bm-surface);
  border-radius: var(--bm-radius);
  border: 1px solid var(--bm-border);
  padding: 24px;
}
.summary-rows {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
}
.calc-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: var(--bm-muted);
}
.calc-divider {
  height: 1px;
  background: var(--bm-soft);
  margin: 4px 0;
}
.total-calc-row {
  font-size: 16px;
  font-weight: 700;
  color: var(--bm-ink);
}
.total-big-val {
  color: var(--bm-price);
  font-size: 18px;
}
.action-buttons-stack {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.btn-block {
  width: 100%;
}

/* Giao dịch */
.transactions-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.txn-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: var(--bm-soft);
  border-radius: var(--bm-radius);
  border: 1px solid var(--bm-border);
}
.txn-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}
.txn-amount {
  font-weight: 700;
  color: var(--bm-ink);
  font-size: 14px;
}

/* Nhãn trạng thái */
.status-pill {
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: var(--bm-radius-control);
}
.status-pending { background: var(--bm-warning-surface); color: var(--bm-warning); }
.status-confirmed { background: var(--bm-soft); color: var(--bm-forest); }
.status-shipping { background: var(--bm-warning-surface); color: var(--bm-warning); }
.status-delivered { background: var(--bm-soft); color: var(--bm-forest); }
.status-completed { background: var(--bm-soft); color: var(--bm-forest); }
.status-cancelled { background: var(--bm-error-surface); color: var(--bm-error); }

/* Nút và trạng thái */
.state-container {
  text-align: center;
  padding: 60px 20px;
  background: var(--bm-surface);
  border-radius: var(--bm-radius);
  border: 1px solid var(--bm-border);
}
.spinner {
  width: 36px;
  height: 36px;
  margin: 0 auto 16px;
  border: 3px solid var(--bm-border);
  border-top-color: var(--bm-forest);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
.error-banner {
  padding: 16px;
  background: var(--bm-error-surface);
  color: var(--bm-error);
  border-radius: var(--bm-radius);
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.btn {
  min-height:44px;
  line-height:1.4;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
  transition: background-color var(--bm-motion-fast), border-color var(--bm-motion-fast), color var(--bm-motion-fast);
  border: 1px solid transparent;
  box-sizing: border-box;
}
.btn-primary {
  background: var(--bm-forest);
  color: var(--bm-surface);
}
.btn-primary:hover {
  background: var(--bm-dark);
}
.btn-outline {
  border-color: var(--bm-border);
  color: var(--bm-ink);
  background: var(--bm-surface);
}
.btn-outline:hover {
  background: var(--bm-soft);
}
.btn-danger-outline {
  border-color: var(--bm-error);
  color: var(--bm-error);
  background: var(--bm-surface);
}
.btn-danger-outline:hover {
  background: var(--bm-error-surface);
}
.btn-secondary {
  background: var(--bm-soft);
  color: var(--bm-ink);
}





.product-title a {color:var(--bm-ink);text-decoration:none;text-underline-offset:4px;}
.product-title a:hover {color:var(--bm-forest);text-decoration:underline;}
.order-review-link {display:inline-flex;align-items:center;min-height:44px;color:var(--bm-forest);font-size:var(--bm-type-label);text-underline-offset:4px;}
.item-price,.item-total-col,.calc-row,.total-amount,.total-big-val {font-variant-numeric:tabular-nums;}
@media(max-width:600px) {
  .item-detail-row {display:grid;grid-template-columns:44px minmax(0,1fr);gap:12px;}
  .item-thumb-wrapper {width:44px;height:44px;}
  .item-info-col {grid-column:2;}
  .item-qty-col {grid-column:1;padding:0;text-align:center;}
  .item-total-col {grid-column:2;min-width:0;text-align:left;overflow-wrap:anywhere;}
  .product-title {font-size:16px;line-height:1.5;}
  .section-heading {flex-wrap:wrap;gap:12px;}
  .detail-header-card {flex-wrap:wrap;gap:12px;}
}



</style>

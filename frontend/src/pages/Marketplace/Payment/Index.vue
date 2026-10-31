<template>
  <MarketplaceLayout>
    <main class="market-container market-page buyer-page">
      <div class="payment-header">
        <h1 class="page-title">Cổng thanh toán trực tuyến</h1>
        <p class="page-subtitle">VNPAY chỉ dùng sandbox khi đã cấu hình. MoMo và chuyển khoản chưa hỗ trợ xác thực thanh toán.</p>
      </div>

      <!-- Trạng thái tải -->
      <div v-if="loading" class="state-container">
        <div class="spinner"></div>
        <p>Đang chuẩn bị thông tin thanh toán...</p>
      </div>

      <!-- Trạng thái lỗi -->
      <div v-else-if="error" class="error-banner" role="alert">
        <p>{{ error }}</p>
        <RouterLink to="/orders" class="btn btn-secondary btn-sm">Quay lại đơn hàng</RouterLink>
      </div>

      <!-- Không tìm thấy đơn hoặc có nhiều đơn đang chờ -->
      <div v-else-if="!order" class="state-container">
        <div v-if="pendingOrders.length > 0" class="pending-picker-box">
          <div class="picker-header">
            <span class="picker-icon">📋</span>
            <h2>Chọn đơn hàng cần thanh toán</h2>
            <p>Bạn có <strong>{{ pendingOrders.length }}</strong> đơn hàng đang chờ thanh toán:</p>
          </div>
          <div class="pending-list">
            <div v-for="po in pendingOrders" :key="po.id" class="pending-card">
              <div class="pending-meta">
                <span class="order-code">Đơn #{{ po.code }}</span>
                <span class="store-badge">🏪 {{ po.storeName }}</span>
                <span class="item-count">{{ po.totalItems || po.itemsCount || 1 }} sản phẩm</span>
              </div>
              <div class="pending-bottom">
                <strong class="pending-price">{{ formatMoney(po.total) }}</strong>
                <RouterLink :to="'/payment?orderId=' + po.id" class="btn btn-primary btn-sm">
                  Thanh toán đơn này →
                </RouterLink>
              </div>
            </div>
          </div>
          <div style="margin-top: 24px; text-align: center;">
            <RouterLink to="/orders" class="btn btn-outline btn-sm">Xem tất cả lịch sử đơn hàng</RouterLink>
          </div>
        </div>
        <div v-else class="empty-state">
          <div class="empty-icon">⚠️</div>
          <h2>Không tìm thấy đơn hàng cần thanh toán</h2>
          <p>Trang thanh toán cần gắn với một đơn hàng cụ thể. Hiện tại bạn chưa chọn đơn hàng hoặc bạn chưa có đơn hàng nào đang chờ thanh toán.</p>
          <div style="display: flex; gap: 12px; justify-content: center; margin-top: 16px;">
            <RouterLink to="/checkout" class="btn btn-primary">Tới trang Đặt hàng</RouterLink>
            <RouterLink to="/orders" class="btn btn-outline">Xem đơn hàng của tôi</RouterLink>
          </div>
        </div>
      </div>

      <!-- Màn hình thanh toán chính -->
      <div v-else class="payment-grid">
        <!-- Bên trái: chọn phương thức thanh toán -->
        <div class="methods-col">
          <section class="section-card">
            <h2 class="card-heading">1. Chọn phương thức thanh toán</h2>
            <div class="method-options">
              <!-- Phương thức 1: VNPAY Sandbox -->
              <label class="method-card" :class="{ selected: selectedMethod === 'VNPAY' }">
                <input type="radio" v-model="selectedMethod" value="VNPAY" class="radio-input" />
                <div class="method-icon-box vnpay-box">
                  <span class="vnpay-logo-text">VNPAY</span>
                </div>
                <div class="method-details">
                  <div class="method-title-row">
                    <strong class="method-name">Cổng thanh toán VNPAY (Sandbox)</strong>
                    <span class="badge-recommend">Khuyên dùng</span>
                  </div>
                  <p class="method-desc">Quét mã VNPAY-QR, thẻ ATM nội địa 40+ ngân hàng hoặc Visa/Mastercard.</p>
                </div>
              </label>

              <!-- Chọn ngân hàng khi dùng VNPAY -->
              <div v-if="selectedMethod === 'VNPAY'" class="vnpay-bank-selector">
                <label for="bank-code-select" class="sub-label">Chọn ngân hàng thử nghiệm (Sandbox):</label>
                <select id="bank-code-select" v-model="selectedBankCode" class="select-input">
                  <option value="">Cổng VNPay (Tự chọn phương thức tại cổng)</option>
                  <option value="NCB">Ngân hàng NCB (Thử nghiệm nhanh)</option>
                  <option value="VIETCOMBANK">Vietcombank</option>
                  <option value="MBBANK">MB Bank</option>
                  <option value="TECHCOMBANK">Techcombank</option>
                  <option value="AGRIBANK">Agribank</option>
                  <option value="VISA">Thẻ quốc tế Visa / MasterCard</option>
                </select>
              </div>

              <!-- Phương thức 2: chuyển khoản VietQR -->
              <label class="method-card" :class="{ selected: selectedMethod === 'BANKING' }">
                <input type="radio" disabled v-model="selectedMethod" value="BANKING" class="radio-input" />
                <div class="method-icon-box qr-box">
                  <span class="qr-icon">📱</span>
                </div>
                <div class="method-details">
                  <strong class="method-name">Chuyển khoản ngân hàng qua VietQR</strong>
                  <p class="method-desc">Quét mã QR NAPAS 247 qua ứng dụng ngân hàng bất kỳ, tự điền nội dung.</p>
                </div>
              </label>

              <!-- Phương thức 3: MoMo Sandbox -->
              <label class="method-card" :class="{ selected: selectedMethod === 'MOMO' }">
                <input type="radio" disabled v-model="selectedMethod" value="MOMO" class="radio-input" />
                <div class="method-icon-box momo-box">
                  <span class="momo-text">MoMo</span>
                </div>
                <div class="method-details">
                  <strong class="method-name">Ví điện tử MoMo (Sandbox)</strong>
                  <p class="method-desc">Thanh toán qua ví MoMo thử nghiệm.</p>
                </div>
              </label>

              <!-- Phương thức 4: COD -->
              <label class="method-card" :class="{ selected: selectedMethod === 'COD' }">
                <input type="radio" v-model="selectedMethod" value="COD" class="radio-input" />
                <div class="method-icon-box cod-box">
                  <span class="cod-icon">💵</span>
                </div>
                <div class="method-details">
                  <strong class="method-name">Thanh toán khi nhận hàng (COD)</strong>
                  <p class="method-desc">Thanh toán tiền mặt cho nhân viên giao hàng khi nhận cây cảnh.</p>
                </div>
              </label>
            </div>
          </section>

          <!-- Hướng dẫn thử nghiệm sandbox -->
          <section class="test-helper-card">
            <div class="test-helper-header">
              <span class="test-tag">Chế độ Sandbox Thử Nghiệm</span>
            </div>
            <p class="test-text">
              VNPAY sandbox cần được cấu hình trên máy chủ. Kết quả phải được xác minh bằng chữ ký và số tiền; không có thao tác tự xác nhận thanh toán. MoMo và chuyển khoản chưa được hỗ trợ.
            </p>
          </section>
        </div>

        <!-- Bên phải: tổng hợp đơn và thao tác -->
        <div class="summary-col">
          <section class="section-card summary-box">
            <h2 class="card-heading">2. Thông tin đơn hàng</h2>
            <div class="order-brief">
              <div class="brief-row">
                <span class="brief-label">Mã đơn hàng:</span>
                <strong class="brief-value text-primary">#{{ order.code }}</strong>
              </div>
              <div class="brief-row">
                <span class="brief-label">Gian hàng:</span>
                <span class="brief-value">{{ order.storeName }}</span>
              </div>
              <div class="brief-row">
                <span class="brief-label">Tiền sản phẩm:</span>
                <span class="brief-value">{{ formatMoney(order.subtotal) }}</span>
              </div>
              <div class="brief-row">
                <span class="brief-label">Phí giao hàng:</span>
                <span class="brief-value">{{ formatMoney(order.shippingFee) }}</span>
              </div>
              <div class="brief-divider"></div>
              <div class="brief-row total-highlight">
                <span class="brief-label">Tổng thanh toán:</span>
                <strong class="brief-val-big">{{ formatMoney(order.total) }}</strong>
              </div>
            </div>

            <div class="submit-stack">
              <!-- Bắt đầu thanh toán -->
              <button
                type="button"
                class="btn btn-primary btn-block btn-lg"
                :disabled="submitting"
                @click="handlePay"
              >
                {{ submitting ? 'Đang chuyển hướng...' : submitButtonText }}
              </button>


              <RouterLink :to="'/orders/' + order.id" class="btn btn-outline btn-block">
                Xem lại chi tiết đơn hàng
              </RouterLink>
            </div>
          </section>
        </div>
      </div>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue';
import { useRoute, useRouter, RouterLink } from 'vue-router';
import MarketplaceLayout from '../../../layouts/MarketplaceLayout.vue';
import { fetchMyOrders, fetchMyOrderDetail } from '../../../services/orders.js';
import { initPayment } from '../../../services/payment.js';

const route = useRoute();
const router = useRouter();

const orderId = computed(() => route.query.orderId);
const order = ref(null);
const pendingOrders = ref([]);
const loading = ref(true);
const error = ref('');
const submitting = ref(false);
let sequence=0;

const selectedMethod = ref('VNPAY');
const selectedBankCode = ref('');

const formatMoney = (val) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);

const submitButtonText = computed(() => {
  switch (selectedMethod.value) {
    case 'VNPAY': return 'Thanh toán qua cổng VNPAY';
    case 'BANKING': return 'Xem thông tin chuyển khoản VietQR';
    case 'MOMO': return 'Thanh toán qua ví MoMo';
    case 'COD': return 'Xác nhận đặt hàng COD';
    default: return 'Tiếp tục';
  }
});

async function loadOrder() {
  const ticket=++sequence,id=orderId.value;
  loading.value = true;
  error.value = '';
  order.value = null;

  if (!orderId.value) {
    try {
      const res = await fetchMyOrders('PENDING', 0, 10);
      if(ticket!==sequence)return;
      pendingOrders.value = res?.content || [];
      if (pendingOrders.value.length === 1) {
        // Tự động chuyển tới đơn hàng duy nhất đang chờ thanh toán
        await router.replace({ path: '/payment', query: { orderId: pendingOrders.value[0].id } });
        return;
      }
    } catch (err) {
      if(ticket===sequence)error.value = err.message || 'Không thể tải danh sách đơn hàng.';
    }
    if(ticket===sequence)loading.value = false;
    return;
  }

  try {
    const result=await fetchMyOrderDetail(id);if(ticket!==sequence)return;order.value=result;
    if (order.value.payment?.method === 'COD') { error.value = 'Đơn đã chọn COD. Thanh toán khi nhận hàng; chưa hỗ trợ đổi phương thức sau khi đặt.'; }
    else if (order.value.status !== 'PENDING') {
      error.value = 'Đơn hàng này không ở trạng thái chờ thanh toán (Trạng thái hiện tại: ' + order.value.status + ').';
    }
  } catch (err) {
    if(ticket===sequence)error.value = err.message || 'Không thể tải thông tin đơn hàng để thanh toán.';
  } finally {
    if(ticket===sequence)loading.value = false;
  }
}

async function handlePay() {
  if (submitting.value || !order.value) return;
  const ticket=sequence;
  submitting.value = true;
  try {
    const returnUrl = window.location.origin + '/payment/callback';
    const res = await initPayment({
      orderId: order.value.id,
      paymentMethod: selectedMethod.value,
      bankCode: selectedBankCode.value || undefined,
      returnUrl
    });
    if(ticket!==sequence)return;

    if (selectedMethod.value === 'BANKING') {
      await router.push({ path: '/payment/banking', query: { orderId: order.value.id, txnCode: res.transactionCode } });
    } else if (selectedMethod.value === 'COD') {
      await router.push({ path: '/orders/' + order.value.id });
    } else if (res.paymentUrl) {
      if (res.paymentUrl.startsWith('http://') || res.paymentUrl.startsWith('https://')) {
        window.location.href = res.paymentUrl;
      } else {
        await router.push(res.paymentUrl.replace(window.location.origin, ''));
      }
    }
  } catch (err) {
    alert(err.message || 'Khởi tạo thanh toán thất bại.');
  } finally {
    submitting.value = false;
  }
}

watch(() => route.query.orderId, () => {
  loadOrder();
}, {immediate:true});
onBeforeUnmount(()=>sequence++);
</script>

<style scoped>
.buyer-page {
  max-width: 1040px;
  margin: 0 auto;
  padding: 32px 16px 64px;
}
.payment-header {
  margin-bottom: 24px;
}
.page-title {
  font-size: 26px;
  font-weight: 700;
  color: var(--ag-text-primary, #1b382b);
  margin: 0 0 6px;
}
.page-subtitle {
  font-size: 14px;
  color: var(--ag-text-secondary, #59635b);
  margin: 0;
}

/* Lưới hai cột */
.payment-grid {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 24px;
  align-items: start;
}
@media (max-width: 860px) {
  .payment-grid {
    grid-template-columns: 1fr;
  }
}

.section-card {
  background: white;
  border-radius: 16px;
  border: 1px solid var(--ag-border, #e2e8e0);
  padding: 24px;
  margin-bottom: 20px;
}
.card-heading {
  font-size: 17px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f3f4f6;
}

/* Các phương thức thanh toán */
.method-options {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.method-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-radius: 12px;
  border: 2px solid #e5e7eb;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #fafafa;
}
.method-card:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
}
.method-card.selected {
  border-color: var(--ag-primary-500, #2e7d32);
  background: #f4fbf5;
}
.radio-input {
  accent-color: var(--ag-primary-500, #2e7d32);
  width: 18px;
  height: 18px;
}
.method-icon-box {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-weight: 800;
  font-size: 13px;
  flex-shrink: 0;
}
.vnpay-box {
  background: #e8f0fe;
  color: #005baa;
}
.vnpay-logo-text {
  font-weight: 900;
  letter-spacing: -0.5px;
}
.qr-box {
  background: #fef3c7;
  font-size: 22px;
}
.momo-box {
  background: #fce7f3;
  color: #a21caf;
  font-size: 12px;
}
.cod-box {
  background: #dcfce7;
  font-size: 22px;
}
.method-details {
  flex: 1;
}
.method-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.method-name {
  font-size: 15px;
  color: #1f2937;
}
.badge-recommend {
  font-size: 10px;
  font-weight: 700;
  background: #dcfce7;
  color: #15803d;
  padding: 2px 6px;
  border-radius: 4px;
  text-transform: uppercase;
}
.method-desc {
  font-size: 13px;
  color: #6b7280;
  margin: 4px 0 0;
}

/* Chọn ngân hàng VNPAY */
.vnpay-bank-selector {
  margin-top: 10px;
  padding: 14px;
  background: #f8fafc;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
}
.sub-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 8px;
}
.select-input {
  width: 100%;
  padding: 10px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 13px;
  background: white;
}

/* Khối hướng dẫn */
.test-helper-card {
  background: #f0fdf4;
  border: 1px dashed #86efac;
  border-radius: 12px;
  padding: 16px;
}
.test-tag {
  font-size: 11px;
  font-weight: 700;
  color: #166534;
  background: #dcfce7;
  padding: 3px 8px;
  border-radius: 4px;
  text-transform: uppercase;
}
.test-text {
  font-size: 13px;
  color: #374151;
  margin: 8px 0 0;
  line-height: 1.5;
}

/* Tóm tắt đơn */
.order-brief {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
}
.brief-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #4b5563;
}
.brief-value {
  color: #1f2937;
  font-weight: 500;
}
.text-primary {
  color: var(--ag-primary-600, #205c3b);
}
.brief-divider {
  height: 1px;
  background: #e5e7eb;
  margin: 4px 0;
}
.total-highlight {
  font-size: 16px;
  color: #111827;
  font-weight: 700;
}
.brief-val-big {
  color: #dc2626;
  font-size: 20px;
}

/* Nút thao tác */
.submit-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.btn-block {
  width: 100%;
}
.btn-lg {
  padding: 14px 20px;
  font-size: 15px;
}
.btn-secondary-green {
  background: #e8f5e9;
  color: #1b5e20;
  border: 1px solid #a5d6a7;
}
.btn-secondary-green:hover {
  background: #c8e6c9;
}

/* Trạng thái và nút dùng chung */
.state-container {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 16px;
  border: 1px solid var(--ag-border, #e2e8e0);
}
.spinner {
  width: 36px;
  height: 36px;
  margin: 0 auto 16px;
  border: 3px solid #e0e0e0;
  border-top-color: var(--ag-primary-500, #2e7d32);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
.error-banner {
  padding: 16px;
  background: #fde8e8;
  color: #c81e1e;
  border-radius: 12px;
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.empty-state .empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}
.empty-state h2 {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
}
.empty-state p {
  color: var(--ag-text-secondary, #59635b);
  margin-bottom: 20px;
}
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 18px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
  box-sizing: border-box;
}
.btn-primary {
  background: var(--ag-primary-500, #2e7d32);
  color: white;
}
.btn-primary:hover {
  background: var(--ag-primary-600, #205c3b);
}
.btn-outline {
  border-color: #d1d5db;
  color: #374151;
  background: white;
}
.btn-outline:hover {
  background: #f9fafb;
}
.btn-secondary {
  background: #e5e7eb;
  color: #374151;
}
.pending-picker-box {
  max-width: 600px;
  margin: 0 auto;
  text-align: left;
}
.picker-header {
  text-align: center;
  margin-bottom: 24px;
}
.picker-icon {
  font-size: 40px;
  display: block;
  margin-bottom: 8px;
}
.picker-header h2 {
  font-size: 22px;
  color: #1b4d3e;
  margin-bottom: 6px;
}
.pending-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.pending-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border: 1px solid #c8d8c6;
  border-radius: 12px;
  background: #fbfdfa;
  transition: box-shadow .2s;
}
.pending-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.06);
}
.pending-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.pending-meta .order-code {
  font-weight: 700;
  font-size: 15px;
  color: #174c35;
}
.pending-meta .store-badge {
  font-size: 13px;
  color: #555;
}
.pending-meta .item-count {
  font-size: 12px;
  color: #777;
}
.pending-bottom {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}
.pending-price {
  font-size: 16px;
  color: #2e7d32;
  font-weight: 700;
}
</style>

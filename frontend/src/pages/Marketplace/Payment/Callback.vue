<template>
  <MarketplaceLayout>
    <main class="market-container market-page buyer-page">
      <div class="callback-card">
        <!-- Đang xác minh -->
        <div v-if="verifying" class="state-box">
          <div class="spinner"></div>
          <h2>Đang xác thực kết quả thanh toán...</h2>
          <p class="muted-text">Vui lòng đợi giây lát, hệ thống đang đồng bộ giao dịch với ngân hàng.</p>
        </div>

        <!-- Kết quả thành công -->
        <div v-else-if="result && result.success" class="state-box success-box">
          <div class="result-icon success-icon">✓</div>
          <h1 class="result-title">Thanh toán thành công!</h1>
          <p class="result-subtitle">{{ result.message }}</p>

          <div class="receipt-details">
            <div class="receipt-row">
              <span>Mã đơn hàng:</span>
              <strong>#{{ result.orderCode || orderId }}</strong>
            </div>
            <div class="receipt-row">
              <span>Mã giao dịch:</span>
              <span class="font-mono">{{ result.transactionCode || txnCode }}</span>
            </div>
            <div class="receipt-row">
              <span>Số tiền đã thanh toán:</span>
              <strong class="price-val">{{ formatMoney(result.amount) }}</strong>
            </div>
            <div class="receipt-row" v-if="result.payDate">
              <span>Thời gian giao dịch:</span>
              <span>{{ formatPayDate(result.payDate) }}</span>
            </div>
          </div>

          <div class="action-buttons">
            <RouterLink v-if="result.orderId" :to="'/orders/' + result.orderId" class="btn btn-primary">
              Xem chi tiết đơn hàng
            </RouterLink>
            <RouterLink to="/orders" class="btn btn-outline">
              Về danh sách đơn hàng
            </RouterLink>
            <RouterLink to="/products" class="btn btn-secondary">
              Tiếp tục mua sắm
            </RouterLink>
          </div>
        </div>

        <!-- Kết quả thất bại -->
        <div v-else class="state-box failure-box">
          <div class="result-icon failure-icon">✕</div>
          <h1 class="result-title">Thanh toán không thành công</h1>
          <p class="result-subtitle">{{ result?.message || error || 'Giao dịch bị hủy hoặc xảy ra lỗi trong quá trình xử lý.' }}</p>

          <div class="action-buttons">
            <RouterLink v-if="result?.orderId || orderId" :to="'/payment?orderId=' + (result?.orderId || orderId)" class="btn btn-primary">
              Thử thanh toán lại
            </RouterLink>
            <RouterLink v-if="result?.orderId || orderId" :to="'/orders/' + (result?.orderId || orderId)" class="btn btn-outline">
              Xem đơn hàng
            </RouterLink>
            <RouterLink to="/orders" class="btn btn-secondary">
              Về danh sách đơn hàng
            </RouterLink>
          </div>
        </div>
      </div>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, RouterLink } from 'vue-router';
import MarketplaceLayout from '../../../layouts/MarketplaceLayout.vue';
import { verifyVnpayCallback } from '../../../services/payment.js';

const route = useRoute();
const verifying = ref(true);
const result = ref(null);
const error = ref('');

const orderId = computed(() => route.query.orderId);
const txnCode = computed(() => route.query.txnCode);

const formatMoney = (val) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);

function formatPayDate(str) {
  if (!str || str.length < 14) return str;
  // yyyyMMddHHmmss
  const year = str.substring(0, 4);
  const month = str.substring(4, 6);
  const day = str.substring(6, 8);
  const hour = str.substring(8, 10);
  const min = str.substring(10, 12);
  const sec = str.substring(12, 14);
  return `${day}/${month}/${year} ${hour}:${min}:${sec}`;
}

async function verifyPaymentResult() {
  verifying.value = true;
  error.value = '';

  // Thu thập các tham số query.
  const params = {};
  for (const [key, value] of Object.entries(route.query)) {
    params[key] = String(value);
  }

  // Chỉ dùng kết quả đã xác minh trên máy chủ.
  if (route.query.vnp_ResponseCode || route.query.vnp_TxnRef) {
    try {
      result.value = await verifyVnpayCallback(params);
    } catch (err) {
      error.value = err.message || 'Không thể xác thực giao dịch với máy chủ.';
    } finally {
      verifying.value = false;
    }
  } else {
    // Không có tham số phản hồi phù hợp.
    error.value = 'Không tìm thấy dữ liệu phản hồi thanh toán.';
    verifying.value = false;
  }
}

onMounted(() => {
  verifyPaymentResult();
});
</script>

<style scoped>
.buyer-page {
  max-width: 680px;
  margin: 0 auto;
  padding: 48px 16px 80px;
}
.callback-card {
  background: white;
  border-radius: 20px;
  border: 1px solid var(--ag-border, #e2e8e0);
  padding: 40px 32px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.06);
}
.state-box {
  text-align: center;
}
.spinner {
  width: 44px;
  height: 44px;
  margin: 0 auto 20px;
  border: 4px solid #e0e0e0;
  border-top-color: var(--ag-primary-500, #2e7d32);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
.result-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 36px;
  font-weight: 700;
  margin: 0 auto 20px;
}
.success-icon {
  background: #dcfce7;
  color: #16a34a;
}
.failure-icon {
  background: #fee2e2;
  color: #dc2626;
}
.result-title {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 8px;
}
.result-subtitle {
  font-size: 15px;
  color: #6b7280;
  margin: 0 0 28px;
}
.receipt-details {
  background: #f9fafb;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  text-align: left;
  margin-bottom: 32px;
  border: 1px solid #f3f4f6;
}
.receipt-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #4b5563;
}
.price-val {
  color: #16a34a;
  font-size: 16px;
}
.font-mono {
  font-family: monospace;
  font-size: 13px;
}
.action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
}
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 22px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
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
</style>

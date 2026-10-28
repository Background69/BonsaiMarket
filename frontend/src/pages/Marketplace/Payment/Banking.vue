<template>
  <MarketplaceLayout>
    <main class="market-container market-page buyer-page">
      <div class="breadcrumb-nav">
        <RouterLink :to="'/payment?orderId=' + orderId" class="back-link">
          &larr; Chọn phương thức thanh toán khác
        </RouterLink>
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="state-container">
        <div class="spinner"></div>
        <p>Đang tải thông tin chuyển khoản VietQR...</p>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="error-banner" role="alert">
        <p>{{ error }}</p>
        <RouterLink to="/orders" class="btn btn-secondary btn-sm">Quay lại đơn hàng</RouterLink>
      </div>

      <!-- Main Banking Card -->
      <div v-else-if="bankInfo" class="banking-card">
        <header class="banking-header">
          <h1 class="banking-title">Thanh toán qua VietQR</h1>
          <p class="banking-subtitle">Quét mã QR bằng ứng dụng ngân hàng bất kỳ để chuyển khoản nhanh 24/7.</p>
        </header>

        <div class="banking-body">
          <!-- QR Code Preview -->
          <div class="qr-col">
            <div class="qr-frame">
              <img :src="bankInfo.qrUrl" alt="Mã VietQR thanh toán" class="qr-image" />
            </div>
            <p class="qr-caption">Hỗ trợ tất cả ngân hàng Việt Nam & ví điện tử.</p>
          </div>

          <!-- Transfer Details -->
          <div class="details-col">
            <h2 class="sub-heading">Thông tin tài khoản thụ hưởng</h2>
            <div class="info-list">
              <div class="info-item">
                <span class="item-label">Ngân hàng:</span>
                <strong class="item-value">{{ bankInfo.bankName }} ({{ bankInfo.bankCode }})</strong>
              </div>
              <div class="info-item">
                <span class="item-label">Số tài khoản:</span>
                <div class="item-copy-group">
                  <strong class="item-value font-mono">{{ bankInfo.accountNumber }}</strong>
                  <button type="button" class="copy-btn" @click="copyText(bankInfo.accountNumber)">Sao chép</button>
                </div>
              </div>
              <div class="info-item">
                <span class="item-label">Chủ tài khoản:</span>
                <strong class="item-value">{{ bankInfo.accountHolder }}</strong>
              </div>
              <div class="info-item">
                <span class="item-label">Số tiền cần chuyển:</span>
                <strong class="item-value price-highlight">{{ formatMoney(bankInfo.amount) }}</strong>
              </div>
              <div class="info-item highlight-box">
                <span class="item-label">Nội dung chuyển khoản (bắt buộc):</span>
                <div class="item-copy-group">
                  <strong class="item-value font-mono text-danger">{{ bankInfo.transferContent }}</strong>
                  <button type="button" class="copy-btn btn-danger-copy" @click="copyText(bankInfo.transferContent)">Sao chép</button>
                </div>
              </div>
            </div>

            <div class="action-stack">
              <button
                type="button"
                class="btn btn-primary btn-block"
                :disabled="confirming"
                @click="confirmTransferred"
              >
                {{ confirming ? 'Đang xác nhận...' : 'Tôi đã hoàn tất chuyển khoản' }}
              </button>

              <RouterLink :to="'/orders/' + orderId" class="btn btn-outline btn-block">
                Xem lại đơn hàng
              </RouterLink>
            </div>
          </div>
        </div>
      </div>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter, RouterLink } from 'vue-router';
import MarketplaceLayout from '../../../layouts/MarketplaceLayout.vue';
import { fetchBankTransferInfo, mockConfirmPayment } from '../../../services/payment.js';

const route = useRoute();
const router = useRouter();

const orderId = computed(() => route.query.orderId);
const txnCode = computed(() => route.query.txnCode);

const bankInfo = ref(null);
const loading = ref(true);
const error = ref('');
const confirming = ref(false);

const formatMoney = (val) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);

async function loadBanking() {
  if (!orderId.value) {
    error.value = 'Thiếu mã đơn hàng để tạo thông tin thanh toán.';
    loading.value = false;
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    bankInfo.value = await fetchBankTransferInfo(orderId.value);
  } catch (err) {
    error.value = err.message || 'Không thể tải thông tin chuyển khoản.';
  } finally {
    loading.value = false;
  }
}

function copyText(text) {
  if (!text) return;
  navigator.clipboard.writeText(text);
  alert('Đã sao chép: ' + text);
}

async function confirmTransferred() {
  confirming.value = true;
  try {
    if (txnCode.value) {
      await mockConfirmPayment(txnCode.value);
    }
    await router.push({
      path: '/payment/callback',
      query: {
        orderId: orderId.value,
        txnCode: txnCode.value || 'BANKING',
        status: 'success'
      }
    });
  } catch {
    await router.push({ path: '/orders/' + orderId.value });
  } finally {
    confirming.value = false;
  }
}

onMounted(() => {
  loadBanking();
});
</script>

<style scoped>
.buyer-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 32px 16px 64px;
}
.breadcrumb-nav {
  margin-bottom: 20px;
}
.back-link {
  color: var(--ag-text-secondary, #59635b);
  text-decoration: none;
  font-size: 14px;
}
.back-link:hover {
  color: var(--ag-primary-600, #205c3b);
}

.banking-card {
  background: white;
  border-radius: 20px;
  border: 1px solid var(--ag-border, #e2e8e0);
  padding: 32px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
}
.banking-header {
  margin-bottom: 28px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f3f4f6;
}
.banking-title {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 6px;
}
.banking-subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.banking-body {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 36px;
  align-items: start;
}
@media (max-width: 768px) {
  .banking-body {
    grid-template-columns: 1fr;
  }
}

/* QR Frame */
.qr-frame {
  background: #f9fafb;
  border: 2px dashed #d1d5db;
  border-radius: 16px;
  padding: 16px;
  text-align: center;
}
.qr-image {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  display: block;
  margin: 0 auto;
}
.qr-caption {
  font-size: 12px;
  color: #6b7280;
  text-align: center;
  margin-top: 10px;
}

/* Info List */
.sub-heading {
  font-size: 16px;
  font-weight: 700;
  margin: 0 0 16px;
  color: #111827;
}
.info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
}
.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: #f9fafb;
  border-radius: 10px;
  font-size: 14px;
  flex-wrap: wrap;
  gap: 8px;
}
.item-label {
  color: #6b7280;
}
.item-value {
  color: #1f2937;
}
.font-mono {
  font-family: monospace;
  font-size: 15px;
}
.price-highlight {
  color: #16a34a;
  font-size: 16px;
}
.text-danger {
  color: #dc2626;
}
.highlight-box {
  background: #fef2f2;
  border: 1px solid #fecaca;
}
.item-copy-group {
  display: flex;
  align-items: center;
  gap: 8px;
}
.copy-btn {
  padding: 3px 8px;
  border: 1px solid #d1d5db;
  background: white;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.copy-btn:hover {
  background: #f3f4f6;
}
.btn-danger-copy {
  border-color: #fca5a5;
  color: #dc2626;
}

/* Action */
.action-stack {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.btn-block {
  width: 100%;
}
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 12px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.2s;
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

/* General states */
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
</style>

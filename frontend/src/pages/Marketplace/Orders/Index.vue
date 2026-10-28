<template>
  <MarketplaceLayout>
    <main class="market-container market-page buyer-page">
      <div class="orders-header">
        <div>
          <h1 class="page-title">Đơn hàng của tôi</h1>
          <p class="page-subtitle">Theo dõi trạng thái và lịch sử tất cả các đơn mua cây cảnh, bonsai.</p>
        </div>
      </div>

      <!-- Tab bộ lọc -->
      <nav class="order-tabs" aria-label="Lọc theo trạng thái đơn hàng">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          type="button"
          :class="['tab-btn', { active: currentTab === tab.key }]"
          @click="selectTab(tab.key)"
        >
          {{ tab.label }}
        </button>
      </nav>

      <!-- Trạng thái tải -->
      <div v-if="loading" class="state-container">
        <div class="spinner"></div>
        <p>Đang tải danh sách đơn hàng...</p>
      </div>

      <!-- Trạng thái lỗi -->
      <div v-else-if="error" class="error-banner" role="alert">
        <p>{{ error }}</p>
        <button class="btn btn-secondary btn-sm" @click="loadOrders">Thử lại</button>
      </div>

      <!-- Danh sách đơn -->
      <div v-else-if="orders.length" class="orders-list">
        <article v-for="order in orders" :key="order.id" class="order-card">
          <!-- Đầu thẻ -->
          <header class="order-card-header">
            <div class="store-info">
              <span class="store-badge">Gian hàng</span>
              <strong class="store-name">{{ order.storeName || 'Cửa hàng Bonsai' }}</strong>
            </div>
            <div class="order-meta">
              <span class="order-code">Mã: #{{ order.code }}</span>
              <span class="status-pill" :class="statusClass(order.status)">
                {{ statusLabel(order.status) }}
              </span>
            </div>
          </header>

          <!-- Nội dung thẻ: sản phẩm xem trước -->
          <div class="order-card-body">
            <div v-for="item in order.items" :key="item.id" class="order-item-row">
              <div class="item-img-wrapper">
                <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.productName" class="item-img" />
                <div v-else class="item-placeholder">
                  <span>{{ (item.productName || 'B').charAt(0).toUpperCase() }}</span>
                </div>
              </div>
              <div class="item-details">
                <h3 class="item-name">{{ item.productName }}</h3>
                <p class="item-qty">Số lượng: x{{ item.quantity }}</p>
              </div>
              <div class="item-price">
                <span class="price-val">{{ formatMoney(item.lineTotal || item.unitPrice * item.quantity) }}</span>
              </div>
            </div>
          </div>

          <!-- Cuối thẻ -->
          <footer class="order-card-footer">
            <div class="footer-time">
              <small class="muted-text">Ngày đặt: {{ formatDate(order.createdAt) }}</small>
            </div>
            <div class="footer-summary">
              <span class="total-label">Tổng thanh toán:</span>
              <strong class="total-amount">{{ formatMoney(order.total) }}</strong>
            </div>
            <div class="footer-actions">
              <RouterLink :to="'/orders/' + order.id" class="btn btn-outline btn-sm">
                Chi tiết
              </RouterLink>
              <RouterLink
                v-if="order.status === 'PENDING'"
                :to="'/payment?orderId=' + order.id"
                class="btn btn-primary btn-sm"
              >
                Thanh toán ngay
              </RouterLink>
              <button
                v-if="order.status === 'PENDING'"
                type="button"
                class="btn btn-danger-outline btn-sm"
                @click="openCancelModal(order)"
              >
                Hủy đơn
              </button>
            </div>
          </footer>
        </article>

        <!-- Phân trang -->
        <div v-if="totalPages > 1" class="pagination-bar">
          <button
            class="btn btn-secondary btn-sm"
            :disabled="page === 0"
            @click="changePage(page - 1)"
          >
            Trang trước
          </button>
          <span class="page-counter">Trang {{ page + 1 }} / {{ totalPages }}</span>
          <button
            class="btn btn-secondary btn-sm"
            :disabled="page >= totalPages - 1"
            @click="changePage(page + 1)"
          >
            Trang sau
          </button>
        </div>
      </div>

      <!-- Trạng thái trống -->
      <div v-else class="state-container empty-state">
        <div class="empty-icon">🌱</div>
        <h2>Chưa có đơn hàng nào</h2>
        <p>Bạn chưa có đơn đặt hàng nào trong trạng thái này.</p>
        <RouterLink to="/products" class="btn btn-primary">
          Khám phá cây cảnh ngay
        </RouterLink>
      </div>

      <!-- Hộp thoại xác nhận hủy -->
      <div v-if="showCancelModal" class="modal-backdrop" @click.self="closeCancelModal">
        <div class="modal-card">
          <h2 class="modal-title">Xác nhận hủy đơn hàng</h2>
          <p class="modal-desc">
            Bạn có chắc chắn muốn hủy đơn hàng <strong>#{{ cancelTarget?.code }}</strong> không?
          </p>
          <div class="form-group">
            <label for="cancel-reason">Lý do hủy (không bắt buộc):</label>
            <textarea
              id="cancel-reason"
              v-model="cancelReason"
              rows="3"
              placeholder="Nhập lý do muốn hủy đơn hàng..."
              class="textarea-input"
            ></textarea>
          </div>
          <div class="modal-actions">
            <button
              type="button"
              class="btn btn-secondary"
              :disabled="cancelSubmitting"
              @click="closeCancelModal"
            >
              Đóng
            </button>
            <button
              type="button"
              class="btn btn-danger"
              :disabled="cancelSubmitting"
              @click="submitCancel"
            >
              {{ cancelSubmitting ? 'Đang hủy...' : 'Xác nhận hủy' }}
            </button>
          </div>
        </div>
      </div>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import { ref, watch, onBeforeUnmount } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import MarketplaceLayout from '../../../layouts/MarketplaceLayout.vue';
import { fetchMyOrders, cancelMyOrder } from '../../../services/orders.js';

const tabs = [
  { key: 'ALL', label: 'Tất cả' },
  { key: 'PENDING', label: 'Chờ xử lý' },
  { key: 'CONFIRMED', label: 'Đã xác nhận' },
  { key: 'SHIPPING', label: 'Đang giao' },
  { key: 'DELIVERED', label: 'Đã giao' },
  { key: 'COMPLETED', label: 'Hoàn tất' },
  { key: 'CANCELLED', label: 'Đã hủy' }
];

const currentTab = ref('ALL');
const route=useRoute(),router=useRouter(); let listSequence=0;
const orders = ref([]);
const page = ref(0);
const totalPages = ref(1);
const loading = ref(true);
const error = ref('');

const showCancelModal = ref(false);
const cancelTarget = ref(null);
const cancelReason = ref('');
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

async function loadOrders() {
  const ticket=++listSequence;
  loading.value = true;
  error.value = '';
  try {
    const res = await fetchMyOrders(currentTab.value, page.value, 10, route.query);
    if(ticket!==listSequence)return;
    orders.value = res.content || [];
    totalPages.value = res.totalPages || 1;
  } catch (err) {
    if(ticket===listSequence)error.value = err.message || 'Không thể tải danh sách đơn hàng. Vui lòng thử lại.';
  } finally {
    if(ticket===listSequence)loading.value = false;
  }
}

function selectTab(key) {
  router.push({path:'/orders',query:{...route.query,status:key==='ALL'?undefined:key,page:0}});
}

function changePage(newPage) {
  router.push({path:'/orders',query:{...route.query,page:newPage}});
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function openCancelModal(order) {
  cancelTarget.value = order;
  cancelReason.value = '';
  showCancelModal.value = true;
}

function closeCancelModal() {
  if (cancelSubmitting.value) return;
  showCancelModal.value = false;
  cancelTarget.value = null;
}

async function submitCancel() {
  if (!cancelTarget.value) return;
  cancelSubmitting.value = true;
  try {
    await cancelMyOrder(cancelTarget.value.id, cancelReason.value);
    showCancelModal.value = false;
    await loadOrders();
  } catch (err) {
    alert(err.message || 'Không thể hủy đơn hàng.');
  } finally {
    cancelSubmitting.value = false;
  }
}

watch(()=>route.query,query=>{
  currentTab.value=tabs.some(t=>t.key===query.status)?query.status:'ALL';
  const requested=Number(query.page||0);page.value=Number.isInteger(requested)&&requested>=0&&requested<=10000?requested:0;
  loadOrders();
},{immediate:true});
onBeforeUnmount(()=>listSequence++);
</script>

<style scoped>
.buyer-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 32px 16px 64px;
}
.orders-header {
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

/* Các tab */
.order-tabs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 8px;
  margin-bottom: 24px;
  border-bottom: 1px solid var(--ag-border, #e2e8e0);
}
.tab-btn {
  padding: 10px 18px;
  border: none;
  background: transparent;
  color: var(--ag-text-secondary, #59635b);
  font-weight: 600;
  font-size: 14px;
  border-radius: 9999px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s ease;
}
.tab-btn:hover {
  background: var(--ag-neutral-100, #f0f4ef);
  color: var(--ag-primary-600, #205c3b);
}
.tab-btn.active {
  background: var(--ag-primary-500, #2e7d32);
  color: white;
}

/* Các trạng thái */
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

/* Thẻ đơn hàng */
.orders-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.order-card {
  background: white;
  border: 1px solid var(--ag-border, #e2e8e0);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  transition: box-shadow 0.2s ease;
}
.order-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.06);
}
.order-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #f9fbf8;
  border-bottom: 1px solid #eef2ec;
}
.store-info {
  display: flex;
  align-items: center;
  gap: 8px;
}
.store-badge {
  font-size: 11px;
  font-weight: 700;
  background: #e8f5e9;
  color: #2e7d32;
  padding: 2px 8px;
  border-radius: 4px;
  text-transform: uppercase;
}
.store-name {
  font-size: 14px;
  color: var(--ag-text-primary, #1b382b);
}
.order-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}
.order-code {
  font-size: 13px;
  font-weight: 600;
  color: #4b5563;
}

/* Nhãn trạng thái */
.status-pill {
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 9999px;
}
.status-pending { background: #fef3c7; color: #b45309; }
.status-confirmed { background: #e0f2fe; color: #0369a1; }
.status-shipping { background: #f3e8ff; color: #7e22ce; }
.status-delivered { background: #dcfce7; color: #15803d; }
.status-completed { background: #dcfce7; color: #166534; }
.status-cancelled { background: #fee2e2; color: #b91c1c; }

/* Các sản phẩm */
.order-card-body {
  padding: 16px 20px;
}
.order-item-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 0;
  border-bottom: 1px solid #f3f4f6;
}
.order-item-row:last-child {
  border-bottom: none;
}
.item-img-wrapper {
  width: 56px;
  height: 56px;
  border-radius: 10px;
  overflow: hidden;
  background: #f3f4f6;
  flex-shrink: 0;
}
.item-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.item-placeholder {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  font-weight: 700;
  color: #9ca3af;
  background: #e5e7eb;
}
.item-details {
  flex: 1;
  min-width: 0;
}
.item-name {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 4px;
  color: #1f2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-qty {
  font-size: 13px;
  color: #6b7280;
  margin: 0;
}
.item-price {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

/* Chân trang */
.order-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  background: #fdfdfd;
  border-top: 1px solid #eef2ec;
  flex-wrap: wrap;
  gap: 12px;
}
.footer-time {
  font-size: 13px;
  color: #6b7280;
}
.footer-summary {
  display: flex;
  align-items: center;
  gap: 8px;
}
.total-label {
  font-size: 13px;
  color: #4b5563;
}
.total-amount {
  font-size: 16px;
  font-weight: 700;
  color: #dc2626;
}
.footer-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Nút */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}
.btn-sm {
  padding: 6px 12px;
  font-size: 12px;
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
.btn-danger-outline {
  border-color: #fca5a5;
  color: #dc2626;
  background: white;
}
.btn-danger-outline:hover {
  background: #fef2f2;
}
.btn-secondary {
  background: #e5e7eb;
  color: #374151;
}
.btn-danger {
  background: #dc2626;
  color: white;
}

/* Phân trang */
.pagination-bar {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
}
.page-counter {
  font-size: 14px;
  color: #4b5563;
}

/* Hộp thoại */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: grid;
  place-items: center;
  z-index: 1000;
  padding: 16px;
}
.modal-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  max-width: 440px;
  width: 100%;
  box-shadow: 0 10px 25px rgba(0,0,0,0.15);
}
.modal-title {
  font-size: 18px;
  font-weight: 700;
  margin: 0 0 8px;
}
.modal-desc {
  font-size: 14px;
  color: #4b5563;
  margin: 0 0 16px;
}
.form-group {
  margin-bottom: 20px;
}
.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 6px;
}
.textarea-input {
  width: 100%;
  padding: 10px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-family: inherit;
  font-size: 13px;
  box-sizing: border-box;
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>

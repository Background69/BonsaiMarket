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
          :aria-pressed="currentTab === tab.key"
          @click="selectTab(tab.key)"
        >
          {{ tab.label }}
        </button>
      </nav>

      <p v-if="orderActionError" class="order-action-error" role="alert">{{ orderActionError }}</p>
      <!-- Trạng thái tải -->
      <div v-if="loading" class="state-container" role="status" aria-busy="true">
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
                <h3 class="item-name"><RouterLink v-if="item.productId" :to="'/products/'+item.productId">{{ item.productName }}</RouterLink><template v-else>{{ item.productName }}</template></h3>
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
                v-if="['PENDING','CONFIRMED'].includes(order.status)"
                :disabled="checkingCancel !== null"
                type="button"
                class="btn btn-danger-outline btn-sm"
                @click="openCancelModal(order, $event)"
              >
                {{ checkingCancel === order.id ? 'Đang kiểm tra…' : 'Hủy đơn' }}
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
        <div class="empty-icon"><MarketIcon name="leaf"/></div>
        <h2>Chưa có đơn hàng nào</h2>
        <p>Bạn chưa có đơn đặt hàng nào trong trạng thái này.</p>
        <RouterLink to="/products" class="btn btn-primary">
          Khám phá cây cảnh ngay
        </RouterLink>
      </div>

      <OrderCancelDialog v-model:open="showCancelModal" :code="cancelTarget?.code" :busy="cancelSubmitting" :error="cancelError" @confirm="submitCancel"/>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import { ref, watch, onBeforeUnmount, nextTick } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import MarketplaceLayout from '../../../layouts/MarketplaceLayout.vue';
import OrderCancelDialog from '../../../components/marketplace/OrderCancelDialog.vue';
import MarketIcon from '../../../components/marketplace/MarketIcon.vue';
import { fetchMyOrders, fetchMyOrderDetail, cancelMyOrder } from '../../../services/orders.js';

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
const cancelError = ref(''), orderActionError = ref(''), checkingCancel = ref(null);
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
    if(page.value>=totalPages.value){await router.replace({path:route.path,query:{...route.query,page:String(totalPages.value-1)},hash:route.hash});}
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
  window.scrollTo({ top: 0, behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth' });
}

async function openCancelModal(order, event) {
  if(checkingCancel.value !== null)return;
  const ticket=listSequence, trigger=event.currentTarget;
  checkingCancel.value=order.id;orderActionError.value='';cancelError.value='';
  try {
    const detail=await fetchMyOrderDetail(order.id);
    if(ticket!==listSequence)return;
    if(!detail.canCancel){orderActionError.value='Đơn #'+order.code+' hiện không thể hủy. Mở chi tiết đơn để xem trạng thái mới nhất.';return;}
    cancelTarget.value=detail;
    checkingCancel.value=null;
    await nextTick();
    if(ticket!==listSequence)return;
    trigger.focus({preventScroll:true});
    showCancelModal.value=true;
  }catch(error){if(ticket===listSequence)orderActionError.value=error.message || 'Chưa kiểm tra được đơn hàng. Vui lòng thử lại.';}
  finally{checkingCancel.value=null;}
}

async function submitCancel(reason) {
  if (!cancelTarget.value || cancelSubmitting.value) return;
  const ticket=listSequence;
  cancelSubmitting.value = true;
  cancelError.value='';
  try {
    await cancelMyOrder(cancelTarget.value.id, reason);
    if(ticket!==listSequence)return;
    showCancelModal.value = false;
    await loadOrders();
  } catch (err) {
    if(ticket===listSequence)cancelError.value=err.message || 'Không thể hủy đơn hàng. Vui lòng thử lại.';
  } finally {
    cancelSubmitting.value = false;
  }
}

watch(()=>route.query,query=>{
  showCancelModal.value=false;orderActionError.value='';
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
  font-size: var(--bm-type-task);
  font-weight: 700;
  color: var(--bm-ink);
  margin: 0 0 6px;
}
.page-subtitle {
  font-size: 14px;
  color: var(--bm-muted);
  margin: 0;
}

/* Các tab */
.order-tabs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 8px;
  margin-bottom: 24px;
  border-bottom: 1px solid var(--bm-border);
}
.tab-btn {
  min-height:44px;
  padding: 10px 18px;
  border: none;
  background: transparent;
  color: var(--bm-muted);
  font-weight: 600;
  font-size: 14px;
  border-radius: var(--bm-radius-control);
  cursor: pointer;
  white-space: nowrap;
  transition: background-color var(--bm-motion-fast), border-color var(--bm-motion-fast), color var(--bm-motion-fast);
}
.tab-btn:hover {
  background: var(--bm-soft);
  color: var(--bm-dark);
}
.tab-btn.active {
  background: var(--bm-forest);
  color: var(--bm-surface);
}

/* Các trạng thái */
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
  color: var(--bm-muted);
  margin-bottom: 20px;
}

/* Thẻ đơn hàng */
.orders-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.order-card {
  background: var(--bm-surface);
  border: 1px solid var(--bm-border);
  border-radius: var(--bm-radius);
  overflow: hidden;
  box-shadow: none;
}

.order-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: var(--bm-soft);
  border-bottom: 1px solid var(--bm-border);
}
.store-info {
  display: flex;
  align-items: center;
  gap: 8px;
}
.store-badge {
  font-size: 11px;
  font-weight: 700;
  background: var(--bm-soft);
  color: var(--bm-forest);
  padding: 2px 8px;
  border-radius: 4px;
  text-transform: none;
}
.store-name {
  font-size: 14px;
  color: var(--bm-ink);
}
.order-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}
.order-code {
  font-size: 13px;
  font-weight: 600;
  color: var(--bm-muted);
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

/* Các sản phẩm */
.order-card-body {
  padding: 16px 20px;
}
.order-item-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 0;
  border-bottom: 1px solid var(--bm-border);
}
.order-item-row:last-child {
  border-bottom: none;
}
.item-img-wrapper {
  width: 56px;
  height: 56px;
  border-radius: var(--bm-radius);
  overflow: hidden;
  background: var(--bm-soft);
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
  color: var(--bm-muted);
  background: var(--bm-soft);
}
.item-details {
  flex: 1;
  min-width: 0;
}
.item-name {
  font-size: var(--bm-type-body);
  font-weight: 600;
  margin: 0 0 4px;
  color: var(--bm-ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: normal;
}
.item-qty {
  font-size: 13px;
  color: var(--bm-muted);
  margin: 0;
}
.item-price {
  font-size: 14px;
  font-weight: 600;
  color: var(--bm-ink);
}

/* Chân trang */
.order-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  background: var(--bm-surface);
  border-top: 1px solid var(--bm-border);
  flex-wrap: wrap;
  gap: 12px;
}
.footer-time {
  font-size: 13px;
  color: var(--bm-muted);
}
.footer-summary {
  display: flex;
  align-items: center;
  gap: 8px;
}
.total-label {
  font-size: 13px;
  color: var(--bm-muted);
}
.total-amount {
  font-size: 16px;
  font-weight: 700;
  color: var(--bm-price);
}
.footer-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Nút */
.btn {
  min-height:44px;
  line-height:1.4;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
  transition: background-color var(--bm-motion-fast), border-color var(--bm-motion-fast), color var(--bm-motion-fast);
  border: 1px solid transparent;
}
.btn-sm {
  padding: 6px 12px;
  font-size: var(--bm-type-label);
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
  color: var(--bm-muted);
}




.item-name a {color:var(--bm-ink);text-decoration:none;text-underline-offset:4px;}
.item-name a:hover {color:var(--bm-forest);text-decoration:underline;}

.item-price,.item-total-col,.calc-row,.total-amount,.total-big-val {font-variant-numeric:tabular-nums;}
.order-action-error {color:var(--bm-error);font-size:16px;line-height:1.6;}
.empty-icon svg {width:48px;height:48px;color:var(--bm-leaf);}
@media(max-width:600px){.order-tabs {flex-wrap:wrap;overflow:visible;gap:4px;}.tab-btn {padding:10px 12px;}.order-card-header {display:grid;gap:12px;}.store-info,.order-meta {flex-wrap:wrap;}.store-info {min-width:0;}.status-pill,.store-badge {white-space:nowrap;}.footer-actions {flex-wrap:wrap;}.item-price {max-width:40%;overflow-wrap:anywhere;}}
</style>

<template>
  <MarketplaceLayout>
    <main class="os-main">
      <!-- Header -->
      <header class="os-header">
        <Link :href="route('agriverse.shop.orders.index')" class="os-back">
          <span class="material-symbols-outlined" style="font-size:18px">arrow_back</span>
          Đơn hàng của tôi
        </Link>
        <div class="os-header-row">
          <div>
            <h1 class="os-title">Đơn hàng #{{ order.id }}</h1>
            <p class="os-date">
              <span class="material-symbols-outlined" style="font-size:15px">schedule</span>
              {{ formatDate(order.created_at) }}
            </p>
          </div>
          <span class="os-status" :class="statusClass(order.status)">{{ statusLabel(order.status) }}</span>
        </div>
      </header>

      <div class="os-grid">
        <!-- Left -->
        <div class="os-left">
          <!-- Product -->
          <section class="os-card">
            <h2 class="os-card-title">
              <span class="material-symbols-outlined os-card-icon">inventory_2</span>
              Sản phẩm
            </h2>
            <div class="os-product">
              <div class="os-product-thumb">{{ order.product?.name?.charAt(0)?.toUpperCase() }}</div>
              <div class="os-product-info">
                <h3 class="os-product-name">{{ order.product?.name }}</h3>
                <p class="os-product-store">
                  <span class="material-symbols-outlined" style="font-size:14px">storefront</span>
                  {{ order.store?.name || '—' }}
                </p>
              </div>
              <div class="os-product-price">
                <span class="os-price-main">{{ formatPrice(order.unit_price) }}₫</span>
                <span class="os-price-qty">x{{ order.quantity }}</span>
              </div>
            </div>
          </section>

          <!-- Shipping Info -->
          <section class="os-card">
            <h2 class="os-card-title">
              <span class="material-symbols-outlined os-card-icon">local_shipping</span>
              Thông tin giao hàng
            </h2>
            <div class="os-info-grid">
              <div class="os-info-box">
                <div class="os-info-head">
                  <span class="material-symbols-outlined" style="font-size:16px;color:var(--ag-primary-500)">location_on</span>
                  <span class="os-info-label">Địa chỉ nhận</span>
                </div>
                <p class="os-info-text">{{ order.shipping_address }}</p>
              </div>
              <div class="os-info-box" v-if="order.shipping_method || order.tracking_number">
                <div class="os-info-head">
                  <span class="material-symbols-outlined" style="font-size:16px;color:var(--ag-primary-500)">local_shipping</span>
                  <span class="os-info-label">Vận chuyển</span>
                </div>
                <p class="os-info-text">{{ order.shipping_method || 'Giao hàng tiết kiệm' }}</p>
                <span v-if="order.tracking_number" class="os-tag">MVD: {{ order.tracking_number }}</span>
                <a v-if="order.tracking_url" :href="order.tracking_url" target="_blank" class="os-link mt-2 inline-flex">
                  Theo dõi hành trình <span class="material-symbols-outlined" style="font-size:14px">open_in_new</span>
                </a>
              </div>
              <div v-if="order.notes" class="os-info-box os-info-note">
                <span class="material-symbols-outlined" style="font-size:16px;color:#B48100">edit_note</span>
                <div>
                  <span class="os-info-label" style="color:#B48100">Ghi chú</span>
                  <p class="os-info-text italic">"{{ order.notes }}"</p>
                </div>
              </div>
              <div v-if="order.cancel_reason" class="os-info-box os-info-danger">
                <span class="material-symbols-outlined" style="font-size:16px;color:var(--ag-danger)">cancel</span>
                <div>
                  <span class="os-info-label" style="color:var(--ag-danger)">Lý do hủy</span>
                  <p class="os-info-text">{{ order.cancel_reason }}</p>
                </div>
              </div>
            </div>
          </section>

          <!-- Timeline -->
          <section v-if="statuses.length" class="os-card">
            <h2 class="os-card-title">
              <span class="material-symbols-outlined os-card-icon">timeline</span>
              Lịch sử trạng thái
            </h2>
            <div class="os-timeline">
              <div v-for="(s, i) in statuses" :key="s.id" class="os-tl-item">
                <div class="os-tl-dot" :class="{ 'os-tl-dot--active': i === 0 }">
                  <div class="os-tl-dot-inner" :class="{ 'os-tl-dot-inner--active': i === 0 }"></div>
                </div>
                <div class="os-tl-content">
                  <span class="os-tl-label">{{ statusLabel(s.status) }}</span>
                  <span class="os-tl-time">{{ formatDate(s.created_at) }}</span>
                  <p v-if="s.note" class="os-tl-note">{{ s.note }}</p>
                </div>
              </div>
            </div>
          </section>

          <!-- Actions -->
          <div v-if="canCancel || canRefund || canConfirmReceived" class="os-actions">
            <button v-if="canCancel" @click="showCancel = true" class="os-btn os-btn-danger">
              <span class="material-symbols-outlined" style="font-size:18px">close</span>
              Hủy đơn hàng
            </button>
            <button v-if="canRefund" @click="showRefund = true" class="os-btn os-btn-warning">
              <span class="material-symbols-outlined" style="font-size:18px">currency_exchange</span>
              Hoàn tiền
            </button>
            <button v-if="canConfirmReceived" @click="handleConfirmReceived" class="os-btn os-btn-success">
              <span class="material-symbols-outlined" style="font-size:18px">check_circle</span>
              Đã nhận hàng
            </button>
          </div>
        </div>

        <!-- Right -->
        <div class="os-right">
          <!-- Summary -->
          <div class="os-card os-summary">
            <h2 class="os-card-title">
              <span class="material-symbols-outlined os-card-icon">receipt_long</span>
              Tổng quan
            </h2>
            <div class="os-summary-rows">
              <div class="os-sum-row">
                <span>Tạm tính</span>
                <span>{{ formatPrice(order.total_price) }}₫</span>
              </div>
              <div v-if="order.shipping_fee > 0" class="os-sum-row">
                <span>Vận chuyển</span>
                <span>{{ formatPrice(order.shipping_fee) }}₫</span>
              </div>
              <div v-if="order.discount_amount > 0" class="os-sum-row os-sum-discount">
                <span>Giảm giá</span>
                <span>-{{ formatPrice(order.discount_amount) }}₫</span>
              </div>
              <div v-if="order.commission_fee > 0" class="os-sum-row">
                <span>Phí giao dịch</span>
                <span>{{ formatPrice(order.commission_fee) }}₫</span>
              </div>
            </div>
            <div class="os-sum-total">
              <span>Thành tiền</span>
              <span class="os-sum-total-val">{{ formatPrice(order.total_amount) }}₫</span>
            </div>

            <div class="os-payment-note">
              <span class="material-symbols-outlined" style="font-size:18px;color:var(--ag-primary-500)">handshake</span>
              <div>
                <p class="os-payment-title">Thanh toán trực tiếp</p>
                <p class="os-payment-desc">Liên hệ người bán để thống nhất phương thức thanh toán.</p>
              </div>
            </div>

            <Link v-if="order.contract" :href="route('agriverse.shop.contracts.show', order.contract.id)"
              class="os-btn os-btn-outline w-full justify-center mt-4">
              <span class="material-symbols-outlined" style="font-size:18px">contract</span>
              Xem hợp đồng
            </Link>
          </div>

          <!-- Passport -->
          <div v-if="order.product?.passport_logs?.length" class="os-card os-passport">
            <div class="os-passport-head">
              <div class="os-passport-icon">
                <span class="material-symbols-outlined" style="font-size:20px">qr_code_scanner</span>
              </div>
              <div>
                <h3 class="os-passport-title">Hộ chiếu số</h3>
                <p class="os-passport-desc">Theo dõi vòng đời sản phẩm trên blockchain.</p>
              </div>
            </div>
            <button class="os-btn os-btn-outline w-full justify-center">
              Truy xuất nguồn gốc
            </button>
          </div>
        </div>

        <!-- Chat -->
        <div class="os-chat-section">
          <div class="os-card" style="overflow:hidden">
            <div class="os-chat-header">
              <span class="material-symbols-outlined" style="font-size:22px;color:var(--ag-primary-500)">forum</span>
              <div>
                <h2 class="os-card-title" style="margin-bottom:0">Trao đổi với người bán</h2>
                <p class="os-chat-sub">Lưu trữ bảo mật làm bằng chứng giao dịch</p>
              </div>
            </div>
            <div class="os-chat-body">
              <ChatBox :order-id="order.id" />
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- Cancel Modal -->
    <Teleport to="body">
      <Transition name="os-modal">
        <div v-if="showCancel" class="os-overlay" @click.self="showCancel = false">
          <div class="os-modal">
            <div class="os-modal-icon os-modal-icon--danger">
              <span class="material-symbols-outlined">warning</span>
            </div>
            <h3 class="os-modal-title">Hủy đơn hàng</h3>
            <p class="os-modal-desc">Bạn có chắc muốn hủy đơn hàng này?</p>
            <textarea v-model="cancelReason" placeholder="Nhập lý do hủy..." class="os-textarea" />
            <div class="os-modal-btns">
              <button @click="showCancel = false" class="os-btn os-btn-outline flex-1">Đóng</button>
              <button @click="handleCancel" class="os-btn os-btn-danger flex-1">Xác nhận hủy</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Refund Modal -->
    <Teleport to="body">
      <Transition name="os-modal">
        <div v-if="showRefund" class="os-overlay" @click.self="showRefund = false">
          <div class="os-modal">
            <div class="os-modal-icon os-modal-icon--warning">
              <span class="material-symbols-outlined">currency_exchange</span>
            </div>
            <h3 class="os-modal-title">Yêu cầu hoàn tiền</h3>
            <select v-model="refundReason" class="os-select">
              <option value="">Chọn lý do hoàn tiền...</option>
              <option value="Sản phẩm không đúng mô tả">Sản phẩm không đúng mô tả</option>
              <option value="Sản phẩm bị hư hỏng">Sản phẩm bị hư hỏng</option>
              <option value="Giao hàng sai">Giao hàng sai</option>
              <option value="Không còn nhu cầu">Không còn nhu cầu</option>
              <option value="Khác">Khác</option>
            </select>
            <textarea v-model="refundDesc" placeholder="Mô tả chi tiết..." class="os-textarea" rows="3" />
            <div class="os-modal-btns">
              <button @click="showRefund = false" class="os-btn os-btn-outline flex-1">Đóng</button>
              <button @click="handleRefund" class="os-btn os-btn-warning flex-1">Gửi yêu cầu</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </MarketplaceLayout>
</template>

<script setup>
import { ref, computed } from 'vue';
import { Link, router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';
import { formatPrice, statusLabel, statusClass } from '@agriverse/utils';
import ChatBox from '@agriverse/Components/ChatBox.vue';
import { useToast } from 'primevue/usetoast';

const toast = useToast();
const props = defineProps({ order: Object });

const statuses = computed(() => props.order?.statuses || []);
const canCancel = computed(() => ['pending', 'confirmed'].includes(props.order?.status));
const canConfirmReceived = computed(() => props.order?.status === 'delivered');
const canRefund = computed(() => ['delivered', 'completed'].includes(props.order?.status) && !hasPendingRefund.value);
const hasPendingRefund = computed(() => props.order?.refunds?.some(r => ['pending', 'approved'].includes(r.status)));

const showCancel = ref(false);
const showRefund = ref(false);
const cancelReason = ref('');
const refundReason = ref('');
const refundDesc = ref('');

function formatDate(dateString) {
  if (!dateString) return '—';
  return new Date(dateString).toLocaleString('vi-VN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).replace(',', ' lúc');
}

function handleCancel() {
  if (!cancelReason.value) { toast.add({ severity: 'error', summary: 'Vui lòng nhập lý do hủy', life: 3000 }); return; }
  router.post(route('agriverse.api.orders.cancel', props.order.id), { reason: cancelReason.value }, {
    preserveState: true, preserveScroll: true,
    onSuccess: () => { showCancel.value = false; cancelReason.value = ''; },
    onError: () => toast.add({ severity: 'error', summary: 'Hủy đơn thất bại', life: 3000 }),
  });
}

function handleConfirmReceived() {
  router.post(route('agriverse.api.orders.confirm-received', props.order.id), {}, {
    preserveState: true, preserveScroll: true,
    onError: () => toast.add({ severity: 'error', summary: 'Xác nhận thất bại', life: 3000 }),
  });
}

function handleRefund() {
  if (!refundReason.value) { toast.add({ severity: 'error', summary: 'Vui lòng chọn lý do', life: 3000 }); return; }
  router.post(route('agriverse.api.orders.refund', props.order.id), { reason: refundReason.value, description: refundDesc.value }, {
    preserveState: true, preserveScroll: true,
    onSuccess: () => { showRefund.value = false; refundReason.value = ''; refundDesc.value = ''; },
    onError: () => toast.add({ severity: 'error', summary: 'Gửi yêu cầu thất bại', life: 3000 }),
  });
}
</script>

<style scoped>
.os-main { max-width: 1100px; margin: 0 auto; padding: 40px 24px 80px; }
@media (max-width: 768px) { .os-main { padding: 24px 16px 60px; } }

.os-back {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 8px 16px; border-radius: 999px;
  background: white; border: 1px solid rgba(116,121,108,0.1);
  font-size: 13px; font-weight: 600; color: var(--ag-text-secondary);
  text-decoration: none; transition: all 0.2s; margin-bottom: 20px;
}
.os-back:hover { color: var(--ag-primary-500); border-color: var(--ag-primary-500); }

.os-header-row { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.os-title {
  font-family: var(--ag-font-display); font-size: 28px; font-weight: 500;
  color: var(--ag-text-primary); letter-spacing: -0.02em; line-height: 1.2;
}
.os-date {
  display: flex; align-items: center; gap: 4px;
  font-size: 13px; color: var(--ag-text-secondary); font-weight: 500; margin-top: 6px;
}
.os-status {
  display: inline-flex; padding: 6px 16px; border-radius: 999px;
  font-size: 13px; font-weight: 600; white-space: nowrap; flex-shrink: 0;
}

.os-grid { display: grid; grid-template-columns: 1fr; gap: 20px; margin-top: 32px; }
@media (min-width: 768px) { .os-grid { grid-template-columns: 1fr 340px; } }
.os-left { display: flex; flex-direction: column; gap: 16px; }
.os-right { display: flex; flex-direction: column; gap: 16px; }

.os-card {
  background: white; border-radius: 16px; padding: 24px;
  border: 1px solid rgba(116,121,108,0.08);
}
.os-card-title {
  display: flex; align-items: center; gap: 8px;
  font-family: var(--ag-font-display); font-size: 17px; font-weight: 500;
  color: var(--ag-text-primary); margin-bottom: 20px;
}
.os-card-icon {
  font-size: 18px; color: var(--ag-primary-500);
  width: 32px; height: 32px; display: flex; align-items: center; justify-content: center;
  background: color-mix(in srgb, var(--ag-primary-500) 8%, transparent);
  border-radius: 8px;
}

.os-product { display: flex; align-items: center; gap: 16px; }
.os-product-thumb {
  width: 64px; height: 64px; border-radius: 14px; flex-shrink: 0;
  background: var(--ag-surface); display: flex; align-items: center; justify-content: center;
  font-size: 24px; font-weight: 500; color: var(--ag-neutral-300); font-family: var(--ag-font-display);
}
.os-product-info { flex: 1; min-width: 0; }
.os-product-name { font-size: 16px; font-weight: 600; color: var(--ag-text-primary); }
.os-product-store {
  display: flex; align-items: center; gap: 4px;
  font-size: 13px; color: var(--ag-text-secondary); font-weight: 500; margin-top: 4px;
}
.os-product-price { text-align: right; flex-shrink: 0; }
.os-price-main { display: block; font-size: 17px; font-weight: 700; color: var(--ag-text-primary); }
.os-price-qty {
  display: inline-block; margin-top: 4px;
  font-size: 12px; color: var(--ag-text-secondary); font-weight: 600;
  background: var(--ag-surface); padding: 2px 10px; border-radius: 999px;
}

.os-info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
@media (max-width: 640px) { .os-info-grid { grid-template-columns: 1fr; } }
.os-info-box {
  padding: 16px; border-radius: 12px;
  background: var(--ag-surface); border: 1px solid rgba(116,121,108,0.05);
}
.os-info-head { display: flex; align-items: center; gap: 6px; margin-bottom: 8px; }
.os-info-label { font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.06em; color: var(--ag-text-secondary); }
.os-info-text { font-size: 14px; font-weight: 500; color: var(--ag-text-primary); line-height: 1.5; }
.os-tag {
  display: inline-flex; margin-top: 8px; padding: 4px 10px; border-radius: 6px;
  font-size: 12px; font-weight: 600; color: var(--ag-primary-600);
  background: color-mix(in srgb, var(--ag-primary-500) 10%, transparent);
  border: 1px solid color-mix(in srgb, var(--ag-primary-500) 15%, transparent);
}
.os-link {
  font-size: 12px; font-weight: 600; color: var(--ag-primary-500); text-decoration: none;
  align-items: center; gap: 4px; transition: opacity 0.2s;
}
.os-link:hover { opacity: 0.7; }
.os-info-note { background: color-mix(in srgb, var(--ag-warning) 6%, transparent); border-color: color-mix(in srgb, var(--ag-warning) 12%, transparent); grid-column: 1 / -1; }
.os-info-danger { background: color-mix(in srgb, var(--ag-danger) 5%, transparent); border-color: color-mix(in srgb, var(--ag-danger) 10%, transparent); grid-column: 1 / -1; }
.italic { font-style: italic; }

/* Timeline */
.os-timeline { position: relative; padding-left: 28px; }
.os-timeline::before {
  content: ''; position: absolute; left: 9px; top: 6px; bottom: 6px; width: 2px;
  background: rgba(116,121,108,0.12); border-radius: 1px;
}
.os-tl-item { display: flex; gap: 14px; padding-bottom: 20px; position: relative; }
.os-tl-item:last-child { padding-bottom: 0; }
.os-tl-dot {
  width: 20px; height: 20px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  background: white; border: 2px solid rgba(116,121,108,0.2);
  position: relative; z-index: 1; margin-left: -28px;
}
.os-tl-dot--active { border-color: var(--ag-primary-500); }
.os-tl-dot-inner { width: 8px; height: 8px; border-radius: 50%; background: rgba(116,121,108,0.25); }
.os-tl-dot-inner--active { background: var(--ag-primary-500); }
.os-tl-content { padding-top: 1px; }
.os-tl-label { display: block; font-size: 14px; font-weight: 600; color: var(--ag-text-primary); }
.os-tl-time { display: block; font-size: 12px; color: var(--ag-text-secondary); font-weight: 500; margin-top: 2px; }
.os-tl-note {
  margin-top: 8px; padding: 10px 14px; border-radius: 10px;
  font-size: 13px; line-height: 1.5; color: var(--ag-text-secondary); font-weight: 500;
  background: var(--ag-surface); border: 1px solid rgba(116,121,108,0.05);
}

/* Summary */
.os-summary { position: sticky; top: 24px; }
.os-summary-rows { display: flex; flex-direction: column; gap: 10px; }
.os-sum-row {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 14px; font-weight: 500; color: var(--ag-text-secondary);
}
.os-sum-row span:last-child { color: var(--ag-text-primary); font-weight: 600; }
.os-sum-discount { color: var(--ag-primary-500); }
.os-sum-discount span:last-child { color: var(--ag-primary-500); }
.os-sum-total {
  display: flex; justify-content: space-between; align-items: center;
  margin-top: 16px; padding-top: 16px; border-top: 1px solid rgba(116,121,108,0.1);
}
.os-sum-total span:first-child { font-size: 15px; font-weight: 700; color: var(--ag-text-primary); }
.os-sum-total-val {
  font-family: var(--ag-font-display); font-size: 22px; font-weight: 600;
  color: var(--ag-primary-600);
}
.os-payment-note {
  display: flex; align-items: flex-start; gap: 10px; margin-top: 20px; padding: 14px;
  border-radius: 12px; background: color-mix(in srgb, var(--ag-primary-500) 5%, transparent);
  border: 1px solid color-mix(in srgb, var(--ag-primary-500) 10%, transparent);
}
.os-payment-title { font-size: 13px; font-weight: 700; color: var(--ag-primary-700); }
.os-payment-desc { font-size: 12px; color: var(--ag-text-secondary); font-weight: 500; margin-top: 2px; line-height: 1.5; }

/* Passport */
.os-passport { background: color-mix(in srgb, var(--ag-surface) 60%, white); }
.os-passport-head { display: flex; align-items: flex-start; gap: 12px; margin-bottom: 16px; }
.os-passport-icon {
  width: 40px; height: 40px; border-radius: 10px; flex-shrink: 0;
  background: white; display: flex; align-items: center; justify-content: center;
  color: var(--ag-primary-500); box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.os-passport-title { font-size: 16px; font-weight: 600; color: var(--ag-text-primary); }
.os-passport-desc { font-size: 13px; color: var(--ag-text-secondary); font-weight: 500; margin-top: 2px; }

/* Chat */
.os-chat-section { grid-column: 1 / -1; margin-top: 4px; }
.os-chat-header {
  display: flex; align-items: center; gap: 12px; padding: 20px 24px;
  border-bottom: 1px solid rgba(116,121,108,0.06); background: var(--ag-surface);
}
.os-chat-sub { font-size: 12px; color: var(--ag-text-secondary); font-weight: 500; margin-top: 2px; }
.os-chat-body { padding: 24px; }

/* Buttons */
.os-actions { display: flex; flex-wrap: wrap; gap: 10px; }
.os-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 10px 20px; border-radius: 10px;
  font-size: 14px; font-weight: 600; cursor: pointer;
  transition: all 0.2s; border: none; font-family: var(--ag-font-body);
}
.os-btn-primary { background: var(--ag-primary-500); color: white; }
.os-btn-primary:hover { background: var(--ag-primary-600); }
.os-btn-success { background: #16a34a; color: white; box-shadow: 0 2px 10px rgba(22,163,74,0.25); }
.os-btn-success:hover { background: #15803d; }
.os-btn-danger { background: white; color: var(--ag-danger); border: 1px solid color-mix(in srgb, var(--ag-danger) 20%, transparent); }
.os-btn-danger:hover { background: color-mix(in srgb, var(--ag-danger) 5%, white); }
.os-btn-warning { background: white; color: #d97706; border: 1px solid rgba(217,119,6,0.2); }
.os-btn-warning:hover { background: rgba(217,119,6,0.05); }
.os-btn-outline { background: white; color: var(--ag-text-primary); border: 1px solid rgba(116,121,108,0.15); }
.os-btn-outline:hover { border-color: var(--ag-primary-500); color: var(--ag-primary-500); }

/* Modal */
.os-overlay {
  position: fixed; inset: 0; z-index: 50;
  display: flex; align-items: center; justify-content: center;
  background: rgba(0,0,0,0.4); backdrop-filter: blur(4px); padding: 16px;
}
.os-modal {
  background: white; border-radius: 16px; padding: 28px;
  max-width: 420px; width: 100%;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
}
.os-modal-icon {
  width: 44px; height: 44px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 16px;
}
.os-modal-icon--danger { background: color-mix(in srgb, var(--ag-danger) 10%, white); color: var(--ag-danger); }
.os-modal-icon--warning { background: color-mix(in srgb, var(--ag-warning) 10%, white); color: #d97706; }
.os-modal-title {
  font-family: var(--ag-font-display); font-size: 20px; font-weight: 500;
  color: var(--ag-text-primary); margin-bottom: 6px;
}
.os-modal-desc { font-size: 14px; color: var(--ag-text-secondary); margin-bottom: 16px; }
.os-modal-btns { display: flex; gap: 10px; margin-top: 20px; }

.os-textarea {
  width: 100%; min-height: 80px; padding: 12px 16px; border-radius: 12px;
  border: 1px solid rgba(116,121,108,0.15); background: var(--ag-surface);
  font-size: 14px; font-family: var(--ag-font-body); color: var(--ag-text-primary);
  outline: none; resize: vertical; transition: border-color 0.2s;
}
.os-textarea:focus { border-color: var(--ag-primary-500); }
.os-select {
  width: 100%; height: 48px; padding: 0 16px; border-radius: 12px;
  border: 1px solid rgba(116,121,108,0.15); background: var(--ag-surface);
  font-size: 14px; font-weight: 600; font-family: var(--ag-font-body);
  color: var(--ag-text-primary); outline: none; transition: border-color 0.2s;
}
.os-select:focus { border-color: var(--ag-primary-500); }

.os-modal-enter-active, .os-modal-leave-active { transition: opacity 0.2s ease; }
.os-modal-enter-from, .os-modal-leave-to { opacity: 0; }
</style>

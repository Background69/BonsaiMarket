<template>
  <MarketplaceLayout>
    <main class="sc-main">
      <!-- Hero Success -->
      <section class="sc-hero">
        <div class="sc-hero-ring">
          <div class="sc-hero-ring-inner">
            <span class="material-symbols-outlined sc-hero-check">check</span>
          </div>
        </div>
        <h1 class="sc-hero-title">Đặt hàng thành công!</h1>
        <p class="sc-hero-sub">Đơn hàng <strong>#{{ orderId }}</strong> đã được ghi nhận. Vui lòng liên hệ người bán để thanh toán và xác nhận.</p>
      </section>

      <!-- Info Grid -->
      <section class="sc-grid">
        <!-- Left Column -->
        <div class="sc-col-left">
          <!-- Order Info -->
          <div class="sc-card">
            <div class="sc-card-header">
              <span class="material-symbols-outlined sc-card-icon">receipt_long</span>
              <h2 class="sc-card-title">Đơn hàng của bạn</h2>
            </div>
            <div class="sc-order-rows">
              <div class="sc-order-row">
                <span class="sc-order-label">Sản phẩm</span>
                <span class="sc-order-val">{{ product?.name || '—' }}</span>
              </div>
              <div class="sc-order-row">
                <span class="sc-order-label">Tổng cộng</span>
                <span class="sc-order-val sc-order-price">{{ formatPrice(total) }}₫</span>
              </div>
              <div class="sc-order-row" v-if="shippingMethod">
                <span class="sc-order-label">Vận chuyển</span>
                <span class="sc-order-val">Giao hàng tiết kiệm</span>
              </div>
            </div>
          </div>

          <!-- Seller Contact -->
          <div class="sc-card" v-if="seller || store">
            <div class="sc-card-header">
              <span class="material-symbols-outlined sc-card-icon">storefront</span>
              <h2 class="sc-card-title">Thông tin người bán</h2>
            </div>
            <div class="sc-contact-list">
              <div class="sc-contact-item">
                <span class="material-symbols-outlined sc-contact-icon">badge</span>
                <div>
                  <span class="sc-contact-label">Cửa hàng</span>
                  <span class="sc-contact-val">{{ store?.name || '—' }}</span>
                </div>
              </div>
              <div class="sc-contact-item">
                <span class="material-symbols-outlined sc-contact-icon">person</span>
                <div>
                  <span class="sc-contact-label">Người bán</span>
                  <span class="sc-contact-val">{{ seller?.name || '—' }}</span>
                </div>
              </div>
              <div class="sc-contact-item" v-if="seller?.phone || store?.phone">
                <span class="material-symbols-outlined sc-contact-icon">call</span>
                <div>
                  <span class="sc-contact-label">Điện thoại</span>
                  <a :href="'tel:' + (seller?.phone || store?.phone)" class="sc-contact-val sc-link">{{ seller?.phone || store?.phone }}</a>
                </div>
              </div>
              <div class="sc-contact-item" v-if="seller?.email">
                <span class="material-symbols-outlined sc-contact-icon">mail</span>
                <div>
                  <span class="sc-contact-label">Email</span>
                  <a :href="'mailto:' + seller.email" class="sc-contact-val sc-link">{{ seller.email }}</a>
                </div>
              </div>
            </div>
          </div>

          <!-- Payment Notice -->
          <div class="sc-notice">
            <span class="material-symbols-outlined sc-notice-icon">info</span>
            <div>
              <h4 class="sc-notice-title">Thanh toán trực tiếp</h4>
              <p class="sc-notice-desc">Nền tảng không thu hộ tiền. Vui lòng liên hệ người bán để thống nhất phương thức thanh toán.</p>
            </div>
          </div>
        </div>

        <!-- Right Column: Steps -->
        <div class="sc-col-right">
          <div class="sc-card">
            <div class="sc-card-header">
              <span class="material-symbols-outlined sc-card-icon">route</span>
              <h2 class="sc-card-title">Các bước tiếp theo</h2>
            </div>
            <div class="sc-steps">
              <div class="sc-step" v-for="(step, i) in steps" :key="i">
                <div class="sc-step-num">{{ i + 1 }}</div>
                <div class="sc-step-body">
                  <h4 class="sc-step-title">{{ step.title }}</h4>
                  <p class="sc-step-desc">{{ step.desc }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Actions -->
      <section class="sc-actions">
        <Link :href="route('agriverse.shop.orders.show', orderId)" class="sc-btn sc-btn-primary">
          <span class="material-symbols-outlined">visibility</span>
          Xem đơn hàng
        </Link>
        <Link :href="route('agriverse.shop.orders.index')" class="sc-btn sc-btn-outline">
          <span class="material-symbols-outlined">receipt_long</span>
          Tất cả đơn hàng
        </Link>
        <Link :href="route('agriverse.shop.home')" class="sc-btn sc-btn-ghost">
          <span class="material-symbols-outlined">home</span>
          Về trang chủ
        </Link>
      </section>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import { Link } from '@inertiajs/vue3';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';
import { formatPrice } from '@agriverse/utils';

const props = defineProps({
  orderId: { type: String, default: '' },
  seller: { type: Object, default: null },
  store: { type: Object, default: null },
  product: { type: Object, default: null },
  total: { type: Number, default: 0 },
  shippingMethod: { type: String, default: '' },
});

const steps = [
  { title: 'Liên hệ người bán', desc: 'Gọi hoặc nhắn tin cho người bán để thông báo đơn hàng và thống nhất phương thức thanh toán.' },
  { title: 'Thanh toán', desc: 'Chuyển khoản, tiền mặt khi nhận hàng, hoặc hình thức khác theo thỏa thuận.' },
  { title: 'Xác nhận & giao hàng', desc: 'Người bán xác nhận và tiến hành đóng gói, giao hàng cho bạn.' },
  { title: 'Nhận hàng & đánh giá', desc: 'Kiểm tra hàng khi nhận, xác nhận trên hệ thống và đánh giá người bán.' },
];
</script>

<style scoped>
.sc-main {
  padding-top: 40px;
  max-width: 1000px;
  margin: 0 auto;
  padding-left: 24px;
  padding-right: 24px;
  padding-bottom: 80px;
}
@media (min-width: 768px) {
  .sc-main { padding-left: 48px; padding-right: 48px; }
}

/* Hero */
.sc-hero { text-align: center; padding: 48px 0 40px; }
.sc-hero-ring {
  width: 80px; height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, color-mix(in srgb, var(--ag-primary-500) 12%, transparent), color-mix(in srgb, var(--ag-primary-500) 4%, transparent));
  display: inline-flex; align-items: center; justify-content: center;
  margin-bottom: 24px;
  animation: sc-pulse 2s ease-in-out infinite;
}
@keyframes sc-pulse {
  0%, 100% { transform: scale(1); box-shadow: 0 0 0 0 color-mix(in srgb, var(--ag-primary-500) 20%, transparent); }
  50% { transform: scale(1.05); box-shadow: 0 0 0 16px transparent; }
}
.sc-hero-ring-inner {
  width: 56px; height: 56px; border-radius: 50%;
  background: var(--ag-primary-500);
  display: flex; align-items: center; justify-content: center;
}
.sc-hero-check { color: white; font-size: 30px; font-variation-settings: 'FILL' 1; }
.sc-hero-title {
  font-family: var(--ag-font-display);
  font-size: 28px; font-weight: 500; letter-spacing: -0.02em;
  color: var(--ag-text-primary); margin-bottom: 10px;
}
.sc-hero-sub {
  font-size: 15px; line-height: 22px; color: var(--ag-text-secondary);
  max-width: 480px; margin: 0 auto;
}
.sc-hero-sub strong { color: var(--ag-primary-500); font-weight: 600; }

/* Grid */
.sc-grid { display: grid; grid-template-columns: 1fr; gap: 20px; }
@media (min-width: 768px) { .sc-grid { grid-template-columns: 1fr 1fr; gap: 24px; } }

.sc-col-left { display: flex; flex-direction: column; gap: 16px; }
.sc-col-right { display: flex; flex-direction: column; }

/* Card */
.sc-card {
  background: white; border-radius: 16px; padding: 24px;
  border: 1px solid rgba(116,121,108,0.08);
}
.sc-card-header { display: flex; align-items: center; gap: 10px; margin-bottom: 20px; }
.sc-card-icon {
  font-size: 20px; color: var(--ag-primary-500);
  width: 36px; height: 36px; display: flex; align-items: center; justify-content: center;
  background: color-mix(in srgb, var(--ag-primary-500) 8%, transparent);
  border-radius: 10px;
}
.sc-card-title {
  font-family: var(--ag-font-display);
  font-size: 18px; font-weight: 500; color: var(--ag-text-primary);
}

/* Order rows */
.sc-order-rows { display: flex; flex-direction: column; }
.sc-order-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid rgba(116,121,108,0.06);
}
.sc-order-row:last-child { border-bottom: none; }
.sc-order-label { font-size: 14px; color: var(--ag-text-secondary); }
.sc-order-val { font-size: 14px; font-weight: 600; color: var(--ag-text-primary); max-width: 60%; text-align: right; }
.sc-order-price { color: var(--ag-primary-500); font-size: 16px; }

/* Contact */
.sc-contact-list { display: flex; flex-direction: column; gap: 0; }
.sc-contact-item {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(116,121,108,0.06);
}
.sc-contact-item:last-child { border-bottom: none; }
.sc-contact-icon {
  font-size: 18px; color: var(--ag-primary-400);
  width: 32px; height: 32px; display: flex; align-items: center; justify-content: center;
  background: color-mix(in srgb, var(--ag-primary-500) 6%, transparent);
  border-radius: 8px; flex-shrink: 0;
}
.sc-contact-label { display: block; font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.06em; color: var(--ag-text-secondary); margin-bottom: 1px; }
.sc-contact-val { font-size: 14px; font-weight: 600; color: var(--ag-text-primary); }
.sc-link { text-decoration: none; color: var(--ag-primary-500); }
.sc-link:hover { text-decoration: underline; }

/* Notice */
.sc-notice {
  display: flex; align-items: flex-start; gap: 12px;
  padding: 16px 20px; border-radius: 14px;
  background: color-mix(in srgb, var(--ag-warning) 6%, transparent);
  border: 1px solid color-mix(in srgb, var(--ag-warning) 12%, transparent);
}
.sc-notice-icon { font-size: 20px; color: var(--ag-warning); margin-top: 1px; flex-shrink: 0; }
.sc-notice-title { font-size: 13px; font-weight: 700; color: var(--ag-warning); margin-bottom: 2px; }
.sc-notice-desc { font-size: 12px; line-height: 17px; color: var(--ag-text-secondary); }

/* Steps */
.sc-steps { display: flex; flex-direction: column; gap: 0; }
.sc-step {
  display: flex; gap: 14px; padding: 16px 0;
  border-bottom: 1px solid rgba(116,121,108,0.06);
}
.sc-step:last-child { border-bottom: none; }
.sc-step-num {
  width: 32px; height: 32px; border-radius: 50%; flex-shrink: 0;
  background: var(--ag-primary-500); color: white;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 700;
  font-family: var(--ag-font-body);
}
.sc-step-title { font-size: 14px; font-weight: 600; color: var(--ag-text-primary); margin-bottom: 3px; }
.sc-step-desc { font-size: 13px; line-height: 18px; color: var(--ag-text-secondary); }

/* Actions */
.sc-actions {
  display: flex; flex-wrap: wrap; gap: 12px; justify-content: center;
  margin-top: 48px;
}
.sc-btn {
  display: inline-flex; align-items: center; gap: 8px;
  padding: 12px 24px; border-radius: 12px;
  font-size: 14px; font-weight: 600; text-decoration: none;
  transition: all 0.2s; font-family: var(--ag-font-body);
}
.sc-btn-primary {
  background: var(--ag-primary-500); color: white;
}
.sc-btn-primary:hover { background: var(--ag-primary-600); transform: translateY(-1px); box-shadow: 0 4px 12px color-mix(in srgb, var(--ag-primary-500) 25%, transparent); }
.sc-btn-outline {
  background: white; color: var(--ag-text-primary);
  border: 1px solid rgba(116,121,108,0.2);
}
.sc-btn-outline:hover { border-color: var(--ag-primary-500); color: var(--ag-primary-500); }
.sc-btn-ghost {
  background: transparent; color: var(--ag-text-secondary);
}
.sc-btn-ghost:hover { color: var(--ag-primary-500); }
</style>

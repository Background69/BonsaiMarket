<template>
  <MarketplaceLayout>
    <main style="padding-top: 80px;">
      <div v-if="cartItems.length" class="cart-layout">
        <div class="cart-collection">
          <div class="cart-collection-header">
            <h1 class="cart-collection-title">Bộ sưu tập của bạn</h1>
            <span class="cart-collection-count">{{ cartItems.length }} Sản phẩm</span>
          </div>

          <div class="cart-items">
            <div v-for="item in cartItems" :key="item.id" class="cart-item">
              <div class="cart-item-image">
                <img v-if="item.product?.image" :src="item.product.image" :alt="item.product.name" class="w-full h-full object-cover" />
                <span v-else class="cart-item-placeholder">{{ item.product?.name?.charAt(0)?.toUpperCase() }}</span>
              </div>
              <div class="cart-item-details">
                <div class="cart-item-top">
                  <div>
                    <Link :href="route('agriverse.shop.products.show', item.product?.id)" class="cart-item-name">
                      {{ item.product?.name }}
                    </Link>
                    <p class="cart-item-variant" v-if="item.variant">Large • 4ft • Ceramic White</p>
                  </div>
                  <span class="cart-item-price">{{ formatPrice(item.product?.price) }}₫</span>
                </div>
                <div class="cart-item-actions">
                  <div class="cart-qty">
                    <button @click="updateQty(item.id, item.quantity - 1)" class="cart-qty-btn" :disabled="item.quantity <= 1">
                      <span class="material-symbols-outlined" style="font-size: 18px;">remove</span>
                    </button>
                    <span class="cart-qty-value">{{ item.quantity }}</span>
                    <button @click="updateQty(item.id, item.quantity + 1)" class="cart-qty-btn">
                      <span class="material-symbols-outlined" style="font-size: 18px;">add</span>
                    </button>
                  </div>
                  <button @click="removeItem(item.id)" class="cart-remove-btn">
                    <span class="material-symbols-outlined" style="font-size: 16px;">delete</span>
                    Xóa
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div class="cart-eco-banner">
            <div class="cart-eco-icon">
              <span class="material-symbols-outlined">eco</span>
            </div>
            <div class="cart-eco-text">
              <h4 class="cart-eco-title">Hậu cần trung hòa Carbon</h4>
              <p class="cart-eco-desc">Mỗi lô hàng đều được tính toán và bù đắp thông qua quan hệ đối tác trồng rừng của chúng tôi, đảm bảo hành trình thực vật của bạn luôn xanh như những cây mới.</p>
            </div>
          </div>
        </div>

        <div class="cart-summary">
          <div class="cart-summary-card">
            <h2 class="cart-summary-title">Tổng cộng</h2>
            <div class="cart-summary-rows">
              <div class="cart-summary-row">
                <span>Tạm tính</span>
                <span class="font-semibold">{{ formatPrice(subtotal) }}₫</span>
              </div>
              <div class="cart-summary-row">
                <span>Phí vận chuyển</span>
                <span class="font-semibold">—</span>
              </div>
              <div class="cart-summary-row">
                <span>Bù đắp Carbon</span>
                <span class="font-semibold">—</span>
              </div>
              <div class="cart-summary-divider"></div>
              <div class="cart-summary-row cart-summary-total">
                <span>Tổng tiền</span>
                <span class="cart-summary-total-value">{{ formatPrice(subtotal) }}₫</span>
              </div>
            </div>
            <Link :href="route('agriverse.shop.checkout.index')" class="cart-checkout-btn">
              <span class="material-symbols-outlined" style="font-size: 20px;">lock</span>
              Tiến hành thanh toán
            </Link>
            <Link :href="route('agriverse.shop.products.index')" class="cart-continue-link">
              ← Tiếp tục mua sắm
            </Link>
          </div>
        </div>
      </div>

      <div v-else class="cart-empty">
        <div class="cart-empty-icon">
          <span class="material-symbols-outlined text-5xl" style="font-variation-settings: 'FILL' 1;">shopping_bag</span>
        </div>
        <h3 class="cart-empty-title">Giỏ hàng trống</h3>
        <p class="cart-empty-desc">Khám phá bộ sưu tập các loài thực vật quý hiếm của chúng tôi và tìm người bạn đồng hành xanh tiếp theo của bạn.</p>
        <Link :href="route('agriverse.shop.products.index')" class="cart-empty-btn">
          Khám phá ngay
        </Link>
      </div>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import { computed } from 'vue';
import { Link, router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';
import { formatPrice } from '@agriverse/utils';
import { useToast } from 'primevue/usetoast';

const toast = useToast();
const props = defineProps({
  cartItems: { type: Array, default: () => [] },
});

const subtotal = computed(() =>
  props.cartItems.reduce((sum, item) => sum + (item.product?.price || 0) * item.quantity, 0)
);

function updateQty(id, qty) {
  if (qty < 1) return;
  router.put(route('agriverse.api.cart.update', id), { quantity: qty }, {
    preserveScroll: true,
    onSuccess: () => toast.add({ severity: 'success', summary: 'Đã cập nhật', life: 2000 }),
    onError: () => toast.add({ severity: 'error', summary: 'Cập nhật thất bại', life: 2000 }),
  });
}

function removeItem(id) {
  router.delete(route('agriverse.api.cart.remove', id), {
    preserveState: true,
    preserveScroll: true,
    onSuccess: () => toast.add({ severity: 'success', summary: 'Đã xóa', life: 2000 }),
    onError: () => toast.add({ severity: 'error', summary: 'Xóa thất bại', life: 2000 }),
  });
}
</script>

<style scoped>
.cart-layout {
  max-width: var(--ag-container-max, 1280px);
  margin: 0 auto;
  padding: 0 var(--ag-margin-desktop, 64px) 80px;
  display: grid;
  grid-template-columns: 1fr;
  gap: 64px;
}
@media (min-width: 1024px) {
  .cart-layout { grid-template-columns: 7fr 5fr; gap: 48px; }
}
@media (max-width: 768px) {
  .cart-layout { padding: 0 var(--ag-margin-mobile, 20px) 48px; gap: 40px; }
}
@media (max-width: 640px) {
  .cart-layout { gap: 32px; }
}

.cart-collection-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  border-bottom: 1px solid rgba(116, 121, 108, 0.1);
  padding-bottom: 16px;
  margin-bottom: 24px;
}
.cart-collection-title {
  font-family: var(--ag-font-display);
  font-size: 36px;
  font-weight: 500;
  color: var(--ag-text-primary);
  letter-spacing: -0.01em;
  line-height: 42px;
}
@media (min-width: 768px) {
  .cart-collection-title { font-size: 40px; }
}
.cart-collection-count {
  font-family: var(--ag-font-body);
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--ag-text-secondary);
}

.cart-items { display: flex; flex-direction: column; }
.cart-item {
  display: flex;
  gap: 24px;
  padding: 24px 0;
  border-bottom: 1px solid rgba(116, 121, 108, 0.05);
  align-items: center;
}
.cart-item:last-child { border-bottom: none; }
.cart-item-image {
  width: 112px;
  height: 144px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--ag-surface-container);
  display: flex;
  align-items: center;
  justify-content: center;
}
.cart-item-placeholder {
  font-family: var(--ag-font-display);
  font-size: 36px;
  color: color-mix(in srgb, var(--ag-text-secondary) 25%, transparent);
  font-weight: 500;
}
@media (max-width: 640px) {
  .cart-item-image { width: 80px; height: 104px; }
  .cart-item-placeholder { font-size: 28px; }
}

.cart-item-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 144px;
}
@media (max-width: 640px) {
  .cart-item-details { min-height: 104px; }
}

.cart-item-top { display: flex; justify-content: space-between; align-items: flex-start; }
@media (max-width: 400px) {
  .cart-item-top { flex-direction: column; gap: 4px; }
  .cart-item-price { align-self: flex-start; }
}
.cart-item-name {
  display: inline-block;
  font-family: var(--ag-font-display);
  font-size: 24px;
  font-weight: 500;
  color: var(--ag-text-primary);
  text-decoration: none;
  transition: color 0.2s;
  line-height: 32px;
}
.cart-item-name:hover { color: var(--ag-primary-500); }
@media (max-width: 640px) {
  .cart-item-name { font-size: 18px; }
}
.cart-item-variant {
  font-family: var(--ag-font-body);
  font-size: 14px;
  color: var(--ag-text-secondary);
  font-style: italic;
  margin-top: 4px;
}
.cart-item-price {
  font-family: var(--ag-font-body);
  font-size: 16px;
  font-weight: 600;
  color: var(--ag-primary-500);
  white-space: nowrap;
}

.cart-item-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
}
.cart-qty {
  display: flex;
  align-items: center;
  border: 1px solid rgba(116, 121, 108, 0.2);
  border-radius: 8px;
  overflow: hidden;
  background: white;
}
.cart-qty-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: var(--ag-text-muted);
  cursor: pointer;
  transition: all 0.2s;
}
.cart-qty-btn:hover { background: rgba(72, 103, 48, 0.08); color: var(--ag-primary-500); }
.cart-qty-btn:disabled { opacity: 0.3; cursor: not-allowed; }
.cart-qty-value {
  width: 32px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--ag-font-body);
  font-size: 14px;
  font-weight: 600;
  color: var(--ag-text-primary);
  border-left: 1px solid rgba(116, 121, 108, 0.2);
  border-right: 1px solid rgba(116, 121, 108, 0.2);
}
.cart-remove-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: transparent;
  color: var(--ag-text-secondary);
  font-family: var(--ag-font-body);
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.02em;
  cursor: pointer;
  transition: all 0.2s;
  padding: 4px 8px;
  border-radius: 6px;
}
.cart-remove-btn:hover { color: var(--ag-danger); background: rgba(186, 26, 26, 0.06); }

.cart-eco-banner {
  background: rgba(72, 103, 48, 0.04);
  border-radius: 16px;
  padding: 32px;
  border: 1px solid rgba(72, 103, 48, 0.12);
  display: flex;
  gap: 20px;
  align-items: flex-start;
  margin-top: 24px;
}
@media (max-width: 400px) {
  .cart-eco-banner { flex-direction: column; align-items: center; text-align: center; padding: 24px; }
  .cart-eco-title { font-size: 18px; }
}
.cart-eco-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(72, 103, 48, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ag-primary-500);
  flex-shrink: 0;
}
.cart-eco-title {
  font-family: var(--ag-font-display);
  font-size: 20px;
  font-weight: 500;
  color: var(--ag-primary-500);
  line-height: 28px;
  margin-bottom: 4px;
}
.cart-eco-desc {
  font-family: var(--ag-font-body);
  font-size: 14px;
  line-height: 22px;
  color: rgba(28, 28, 28, 0.7);
}

.cart-summary-card {
  background: white;
  border-radius: 16px;
  border: 1px solid rgba(116, 121, 108, 0.08);
  box-shadow: 0 1px 3px rgba(0,0,0,0.03);
  padding: 32px;
  position: sticky;
  top: 100px;
}
@media (max-width: 640px) {
  .cart-summary-card { padding: 20px; }
  .cart-summary-total-value { font-size: 22px; }
  .cart-collection-title { font-size: 28px; }
}
.cart-summary-title {
  font-family: var(--ag-font-display);
  font-size: 24px;
  font-weight: 500;
  color: var(--ag-text-primary);
  margin-bottom: 24px;
}
.cart-summary-rows { display: flex; flex-direction: column; gap: 12px; }
.cart-summary-row {
  display: flex;
  justify-content: space-between;
  font-family: var(--ag-font-body);
  font-size: 14px;
  color: var(--ag-text-secondary);
}
.cart-summary-divider { height: 1px; background: rgba(116, 121, 108, 0.1); margin: 8px 0; }
.cart-summary-row.cart-summary-total { font-size: 18px; }
.cart-summary-row.cart-summary-total span:first-child { font-weight: 600; color: var(--ag-text-primary); }
.cart-summary-total-value {
  font-family: var(--ag-font-display);
  font-size: 28px;
  font-weight: 500;
  color: var(--ag-primary-500);
  line-height: 36px;
}

.cart-checkout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  margin-top: 24px;
  padding: 14px 24px;
  background: var(--ag-primary-500);
  color: white;
  border: none;
  border-radius: 9999px;
  font-family: var(--ag-font-body);
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(72, 103, 48, 0.15);
}
.cart-checkout-btn:hover { background: var(--ag-primary-600); transform: translateY(-1px); box-shadow: 0 6px 20px rgba(72, 103, 48, 0.2); }
.cart-checkout-btn:active { transform: scale(0.98); }

.cart-continue-link {
  display: block;
  text-align: center;
  margin-top: 12px;
  font-family: var(--ag-font-body);
  font-size: 14px;
  font-weight: 500;
  color: var(--ag-text-secondary);
  text-decoration: none;
  transition: color 0.2s;
}
.cart-continue-link:hover { color: var(--ag-primary-500); }

.cart-empty {
  max-width: var(--ag-container-max, 1280px);
  margin: 0 auto;
  padding: 120px var(--ag-margin-desktop, 64px);
  text-align: center;
}
@media (max-width: 768px) {
  .cart-empty { padding: 80px var(--ag-margin-mobile, 20px); }
}
.cart-empty-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: rgba(72, 103, 48, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
  color: var(--ag-primary-500);
}
.cart-empty-title {
  font-family: var(--ag-font-display);
  font-size: 28px;
  font-weight: 500;
  color: var(--ag-text-primary);
  margin-bottom: 8px;
}
.cart-empty-desc {
  font-family: var(--ag-font-body);
  font-size: 16px;
  line-height: 24px;
  color: var(--ag-text-secondary);
  max-width: 400px;
  margin: 0 auto 32px;
}
.cart-empty-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 14px 32px;
  background: var(--ag-primary-500);
  color: white;
  border-radius: 9999px;
  font-family: var(--ag-font-body);
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.3s;
}
.cart-empty-btn:hover { background: var(--ag-primary-600); transform: translateY(-1px); }
</style>

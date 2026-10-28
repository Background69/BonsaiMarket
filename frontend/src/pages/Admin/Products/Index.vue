<template>
  <MarketplaceLayout>
    <main class="market-container market-page buyer-page cart-page">
      <nav class="breadcrumbs" aria-label="Đường dẫn điều hướng">
        <RouterLink to="/">Trang chủ</RouterLink>
        <span aria-hidden="true">/</span><span aria-current="page">Giỏ hàng</span>
      </nav>
      <header class="market-page-heading">
        <h1>Giỏ hàng của bạn</h1>
        <p>Xem lại sản phẩm và gian hàng trước khi đặt mua.</p>
      </header>

      <p class="cart-sr-only" role="status" aria-live="polite">{{ announcement }}</p>
      <p v-if="actionError" ref="errorBox" class="integration-error" role="alert" tabindex="-1">{{ actionError }}</p>

      <div v-if="loading" class="cart-state" role="status">Đang tải giỏ hàng...</div>

      <div v-else-if="loadError" class="integration-error" role="alert">
        <p>{{ loadError }}</p>
        <button class="button button-outline" type="button" @click="reload">Thử lại</button>
      </div>

      <div v-else-if="items.length" class="buyer-cart-layout">
        <section class="market-panel" aria-label="Sản phẩm trong giỏ hàng">
          <div v-for="group in storeGroups" :key="group.key" class="cart-store-group">
            <h2>
              <RouterLink v-if="group.id" :to="'/stores/' + group.id">{{ group.name }}</RouterLink>
              <span v-else>{{ group.name }}</span>
            </h2>

            <article v-for="item in group.items" :key="item.cartItemId" class="buyer-cart-item"
                     :class="{'is-pending': isPending(item), 'is-problem': problemOf(item)}">
              <RouterLink :to="'/products/' + item.productId" class="cart-item-image" tabindex="-1" aria-hidden="true">
                <CatalogImage :alt="item.name" :src="item.imageUrl"/>
              </RouterLink>

              <div class="cart-item-info">
                <RouterLink :to="'/products/' + item.productId">{{ item.name }}</RouterLink>
                <p class="cart-item-price">{{ money(item.unitPrice) }}</p>
                <p v-if="problemOf(item)" class="cart-item-problem" role="alert">{{ problemOf(item) }}</p>
              </div>

              <div class="cart-item-controls">
                <div class="cart-qty" role="group" :aria-label="'Số lượng của ' + item.name">
                  <button type="button" aria-label="Giảm số lượng"
                          :disabled="isPending(item) || item.quantity <= 1"
                          @click="changeQuantity(item, item.quantity - 1)">−
                  </button>
                  <input :value="item.quantity" :disabled="isPending(item)" :max="maxFor(item)"
                         aria-label="Số lượng" inputmode="numeric" min="1" type="number"
                         @change="onQuantityInput(item, $event)"/>
                  <button type="button" aria-label="Tăng số lượng"
                          :disabled="isPending(item) || item.quantity >= maxFor(item)"
                          @click="changeQuantity(item, item.quantity + 1)">+
                  </button>
                </div>
                <strong class="cart-line-total">{{ money(lineOf(item)) }}</strong>
                <button class="cart-remove" type="button" :disabled="isPending(item)"
                        :aria-label="'Xóa ' + item.name + ' khỏi giỏ hàng'" @click="removeItem(item)">Xóa
                </button>
              </div>
            </article>
          </div>
        </section>

        <aside class="market-panel cart-summary" aria-label="Tóm tắt giỏ hàng">
          <h2>Tóm tắt giỏ hàng</h2>
          <p class="cart-summary-row"><span>Số sản phẩm</span><strong>{{ totalQuantity }}</strong></p>
          <p class="cart-summary-row"><span>Tạm tính</span><strong>{{ money(subtotal) }}</strong></p>
          <p class="muted">Phí vận chuyển theo từng gian hàng được tính ở bước thanh toán.</p>
          <p v-if="blockedCount" class="cart-item-problem" role="alert">
            Có {{ blockedCount }} sản phẩm cần điều chỉnh trước khi thanh toán.
          </p>
          <RouterLink v-if="canCheckout" class="button button-primary" to="/checkout">Tiến hành thanh toán
          </RouterLink>
          <button v-else class="button button-primary" disabled type="button">Tiến hành thanh toán</button>
          <RouterLink class="text-link" to="/products">Tiếp tục khám phá</RouterLink>
        </aside>
      </div>

      <section v-else class="market-empty buyer-empty">
        <MarketIcon name="cart"/>
        <h2>Giỏ hàng đang trống</h2>
        <p>Khám phá sản phẩm từ các gian hàng và tìm cây bạn yêu thích.</p>
        <div class="button-row">
          <RouterLink class="button button-primary" to="/products">Khám phá sản phẩm</RouterLink>
          <RouterLink class="button button-outline" to="/stores">Khám phá gian hàng</RouterLink>
        </div>
      </section>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import {computed, nextTick, onBeforeUnmount, onMounted, reactive, ref} from 'vue';
import {RouterLink} from 'vue-router';
import MarketplaceLayout from '../../../layouts/MarketplaceLayout.vue';
import CatalogImage from '../../../components/marketplace/CatalogImage.vue';
import MarketIcon from '../../../components/marketplace/MarketIcon.vue';
import {
  MAX_CART_QUANTITY, cartItems, loadCart, removeCartItem, syncCartCount, updateCartItem,
} from '../../../services/cart.js';

const items = ref([]);
const loading = ref(true), loadError = ref(''), actionError = ref(''), announcement = ref('');
const pending = reactive({});
const errorBox = ref(null);
let disposed = false;

const currency = new Intl.NumberFormat('vi-VN', {style: 'currency', currency: 'VND'});
const money = value => currency.format(Number(value) || 0);

const hasStock = item => Number.isInteger(item.stock) && item.stock >= 0;
const maxFor = item => (hasStock(item) ? Math.min(item.stock, MAX_CART_QUANTITY) : MAX_CART_QUANTITY);
const lineOf = item => Number(item.lineTotal ?? (Number(item.unitPrice) || 0) * item.quantity) || 0;
const isPending = item => !!pending[item.cartItemId];

// Lý do một dòng không thể thanh toán (null nếu hợp lệ).
function problemOf(item) {
  if (item.available === false || (hasStock(item) && item.stock === 0)) return 'Sản phẩm này hiện không còn để bán. Hãy xóa khỏi giỏ hàng.';
  if (hasStock(item) && item.quantity > item.stock) return `Chỉ còn ${item.stock} sản phẩm. Hãy giảm số lượng.`;
  return null;
}

const storeGroups = computed(() => {
  const groups = new Map();
  for (const item of items.value) {
    const id = item.storeId ?? null;
    const key = id ?? 'none';
    if (!groups.has(key)) groups.set(key, {key, id, name: item.storeName || 'Chưa có thông tin gian hàng', items: []});
    groups.get(key).items.push(item);
  }
  return [...groups.values()];
});
const subtotal = computed(() => items.value.reduce((sum, item) => sum + lineOf(item), 0));
const totalQuantity = computed(() => items.value.reduce((sum, item) => sum + (Number(item.quantity) || 0), 0));
const blockedCount = computed(() => items.value.filter(problemOf).length);
const anyPending = computed(() => Object.values(pending).some(Boolean));
const canCheckout = computed(() => items.value.length > 0 && !blockedCount.value && !anyPending.value);

function apply(cart, message = '') {
  if (disposed) return;
  items.value = cartItems(cart);
  syncCartCount(cart);
  if (message) announcement.value = message;
}

async function showError(error) {
  actionError.value = error?.message || 'Không thể cập nhật giỏ hàng. Vui lòng thử lại.';
  await nextTick();
  errorBox.value?.focus();
}

async function reload() {
  loading.value = true;
  loadError.value = '';
  try {
    apply(await loadCart());
  } catch (error) {
    if (!disposed) loadError.value = error.message || 'Không thể tải giỏ hàng. Vui lòng thử lại.';
  } finally {
    if (!disposed) loading.value = false;
  }
}

// Chạy một thao tác trên một dòng; chặn thao tác trùng lặp và đồng bộ lại khi dữ liệu đã lệch.
async function mutate(item, action, message) {
  const id = item.cartItemId;
  if (pending[id]) return;
  pending[id] = true;
  actionError.value = '';
  try {
    const result = await action();
    if (Array.isArray(result?.items)) apply(result, message);
    else apply(await loadCart(), message);
  } catch (error) {
    await showError(error);
    if (error.status === 404 || error.status === 409) {
      try {
        apply(await loadCart());
      } catch { /* giữ thông báo lỗi ban đầu */
      }
    }
  } finally {
    delete pending[id];
  }
}

function changeQuantity(item, quantity) {
  if (!Number.isInteger(quantity) || quantity < 1 || quantity === item.quantity) return;
  if (quantity > maxFor(item)) {
    actionError.value = hasStock(item) ? `Chỉ còn ${item.stock} sản phẩm trong kho.` : 'Số lượng vượt quá giới hạn cho phép.';
    return;
  }
  return mutate(item, () => updateCartItem(item.cartItemId, quantity), `Đã cập nhật số lượng ${item.name}.`);
}

function onQuantityInput(item, event) {
  const value = Number(event.target.value);
  if (!Number.isInteger(value) || value < 1) {
    event.target.value = item.quantity;
    actionError.value = 'Số lượng phải là số nguyên từ 1 trở lên.';
    return;
  }
  const target = Math.min(value, maxFor(item));
  event.target.value = target;
  changeQuantity(item, target);
}

function removeItem(item) {
  return mutate(item, () => removeCartItem(item.cartItemId), `Đã xóa ${item.name} khỏi giỏ hàng.`);
}

onMounted(reload);
onBeforeUnmount(() => {
  disposed = true;
});
</script>

<style scoped>
.cart-sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  margin: -1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
}

.cart-state {
  padding: 48px 0;
  text-align: center;
  color: var(--bm-muted, #5b6b5f);
}

.buyer-cart-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 24px;
  align-items: start;
}

.cart-store-group + .cart-store-group {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid var(--bm-border, #e3e8e3);
}

.cart-store-group > h2 {
  margin: 0 0 12px;
  font-size: 1.05rem;
}

.buyer-cart-item {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
  padding: 12px 0;
}

.buyer-cart-item.is-pending {
  opacity: .6;
}

.cart-item-image {
  display: block;
  width: 88px;
}

.cart-item-info {
  min-width: 0;
}

.cart-item-info > a {
  font-weight: 600;
  overflow-wrap: anywhere;
}

.cart-item-price {
  margin: 4px 0 0;
  color: var(--bm-muted, #5b6b5f);
}

.cart-item-problem {
  margin: 6px 0 0;
  color: #b3261e;
  font-size: .9rem;
}

.cart-item-controls {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.cart-qty {
  display: inline-flex;
  align-items: stretch;
  border: 1px solid var(--bm-border, #cfd8cf);
  border-radius: 8px;
  overflow: hidden;
}

.cart-qty button, .cart-qty input {
  min-width: 40px;
  min-height: 40px;
  border: 0;
  background: transparent;
  font: inherit;
  text-align: center;
}

.cart-qty button {
  cursor: pointer;
}

.cart-qty button:disabled {
  cursor: not-allowed;
  opacity: .4;
}

.cart-qty input {
  width: 56px;
  border-inline: 1px solid var(--bm-border, #cfd8cf);
  appearance: textfield;
  -moz-appearance: textfield;
}

.cart-qty input::-webkit-outer-spin-button, .cart-qty input::-webkit-inner-spin-button {
  appearance: none;
  margin: 0;
}

.cart-remove {
  border: 0;
  background: none;
  padding: 4px 0;
  color: #b3261e;
  text-decoration: underline;
  cursor: pointer;
}

.cart-remove:disabled {
  opacity: .4;
  cursor: not-allowed;
}

.cart-summary {
  position: sticky;
  top: 96px;
  display: grid;
  gap: 12px;
}

.cart-summary h2 {
  margin: 0;
}

.cart-summary p {
  margin: 0;
}

.cart-summary-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.cart-summary .button {
  justify-content: center;
  text-align: center;
}

@media (max-width: 860px) {
  .buyer-cart-layout {
    grid-template-columns: 1fr;
  }

  .cart-summary {
    position: static;
  }
}

@media (max-width: 520px) {
  .buyer-cart-item {
    grid-template-columns: 72px minmax(0, 1fr);
  }

  .cart-item-image {
    width: 72px;
  }

  .cart-item-controls {
    grid-column: 1 / -1;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
  }
}
</style>
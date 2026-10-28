<script setup>
import {computed, onMounted, onUnmounted, ref, nextTick, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import MarketplaceLayout from '../../../layouts/MarketplaceLayout.vue';
import AddressManagement from '../../../components/marketplace/AddressManagement.vue';
import {productPage, usableImage} from '../../../services/catalog.js';
import {apiGet} from '../../../services/api.js';
import {loadCheckout, createQuote, placeOrder, loadQuote, loadReceipt} from '../../../services/checkout.js';

const route = useRoute(), router = useRouter();
const loading = ref(true), busy = ref(false), error = ref(''), errorBox = ref(null);
const data = ref({items: [], addresses: [], shippingFeePerStore: 0});
const selectedStore = ref(null), selectedShipping = ref(null);
const source = ref('CART'), selectedAddress = ref(null), notes = ref('');
const quote = ref(null), receipt = ref(null), addressEditor = ref(false), uncertain = ref(false);
const search = ref(''), products = ref([]), selectedProduct = ref(null), quantity = ref(1), searching = ref(false);
const showSuggestions = ref(false), highlightedIndex = ref(-1);
const review = computed(() => quote.value?.review);
const rate = computed(() => quote.value?.shippingQuotes?.find(q => q.id === selectedShipping.value));
const stores = computed(() => [...new Map(data.value.items.map(i => [i.storeId, {id: i.storeId, name: i.storeName}])).values()]);
const money = value => new Intl.NumberFormat('vi-VN', {style: 'currency', currency: 'VND'}).format(value || 0);
const addressText = address => [address.addressDetail, address.wardName, address.districtName, address.provinceName].filter(Boolean).join(', ');
const selectedItems = computed(() => source.value === 'CART' ? data.value.items.filter(i => !selectedStore.value || i.storeId === selectedStore.value) : selectedProduct.value
    ? [{productId: selectedProduct.value.id, name: selectedProduct.value.name, imageUrl: selectedProduct.value.imageUrl,
        quantity: quantity.value, unitPrice: selectedProduct.value.price, storeName: selectedProduct.value.store?.name}] : []);

async function showError(e) {error.value = e.message || 'Không thể xử lý yêu cầu.'; await nextTick(); errorBox.value?.focus();}
async function refresh() {
    data.value = await loadCheckout();
    const requestedStore=Number(route.query.store);
    if(stores.value.some(s=>s.id===requestedStore))selectedStore.value=requestedStore;
    if (!stores.value.some(s => s.id === selectedStore.value)) selectedStore.value = stores.value.length === 1 ? stores.value[0].id : null;
    if (!data.value.addresses.some(a => a.id === selectedAddress.value))
        selectedAddress.value = (data.value.addresses.find(a => a.isDefault) || data.value.addresses[0])?.id ?? null;
}
async function initialize() {
    loading.value = true; error.value = '';
    try {
        if (route.query.receipt) receipt.value = await loadReceipt(String(route.query.receipt));
        else if (route.query.quote) {
            // Khôi phục đơn khi phản hồi thành công có thể đã bị mất.
            try {receipt.value = await loadReceipt(String(route.query.quote));}
            catch (e) {if (e.status !== 404) throw e;}
            if (!receipt.value) quote.value = await loadQuote(String(route.query.quote));
        } else {
            await refresh();
            if (route.query.productId) {
                source.value = 'BUY_NOW';
                selectedProduct.value = await apiGet('/products/' + encodeURIComponent(String(route.query.productId)));
                const count = Number(route.query.quantity || 1);
                quantity.value = Number.isInteger(count) && count >= 1 && count <= 1000000 ? count : 1;
            }
        }
    } catch (e) {await showError(e);} finally {loading.value = false;}
}
async function findProducts() {
    searching.value = true; error.value = '';
    try {
        products.value = (await productPage({search: search.value, in_stock: true, size: 12})).content;
        showSuggestions.value = true;
        highlightedIndex.value = -1;
    }
    catch (e) {await showError(e);} finally {searching.value = false;}
}
async function switchSource(value) {
    source.value = value; error.value = '';
    if (value === 'BUY_NOW' && !products.value.length) await findProducts();
}
async function finishAddresses() {
    busy.value = true; error.value = '';
    try {await refresh(); addressEditor.value = false;} catch (e) {await showError(e);} finally {busy.value = false;}
}
async function reviewOrder() {
    if (busy.value) return;
    busy.value = true; error.value = '';
    try {
        const body = {source: source.value, addressId: selectedAddress.value, notes: notes.value, storeId: selectedStore.value};
        if (source.value === 'BUY_NOW') Object.assign(body, {productId: selectedProduct.value?.id, quantity: quantity.value});
        quote.value = await createQuote(body); selectedShipping.value = null;
        await router.replace({path: '/checkout', query: {quote: quote.value.quoteId}});
        window.scrollTo({top: 0, behavior: 'smooth'});
    } catch (e) {await showError(e);} finally {busy.value = false;}
}
async function confirm() {
    if (busy.value || !quote.value || !rate.value) return;
    busy.value = true; error.value = ''; uncertain.value = true;
    try {
        receipt.value = await placeOrder(quote.value.quoteId, selectedShipping.value);
        uncertain.value = false;
        await router.replace({path: '/checkout', query: {receipt: receipt.value.quoteId}});
    } catch (e) {
        // Chưa rõ kết quả thì mọi lần thử lại phải dùng cùng báo phí đã lưu.
        if (e.status && e.status < 500) uncertain.value = false;
        await showError(e);
    } finally {busy.value = false;}
}
async function editOrder() {
    if (busy.value || uncertain.value) return;
    busy.value = true; error.value = '';
    try {
        if (quote.value) {
            try {receipt.value = await loadReceipt(quote.value.quoteId); return;}
            catch (e) {if (e.status !== 404) throw e;}
        }
        await refresh(); quote.value = null;
        await router.replace({path: '/checkout'});
    } catch (e) {await showError(e);} finally {busy.value = false;}
}
function selectProduct(product) {
    selectedProduct.value = product;
    search.value = product.name;
    showSuggestions.value = false;
    highlightedIndex.value = -1;
}
function clearSelection() {
    selectedProduct.value = null;
    search.value = '';
    products.value = [];
    showSuggestions.value = false;
}
function onSearchKeydown(e) {
    if (!showSuggestions.value || !products.value.length) {
        if (e.key === 'Enter') { e.preventDefault(); findProducts(); }
        return;
    }
    if (e.key === 'ArrowDown') { e.preventDefault(); highlightedIndex.value = Math.min(highlightedIndex.value + 1, products.value.length - 1); }
    else if (e.key === 'ArrowUp') { e.preventDefault(); highlightedIndex.value = Math.max(highlightedIndex.value - 1, 0); }
    else if (e.key === 'Enter') { e.preventDefault(); if (highlightedIndex.value >= 0) selectProduct(products.value[highlightedIndex.value]); else findProducts(); }
    else if (e.key === 'Escape') { showSuggestions.value = false; }
}

// Chờ người dùng ngừng gõ rồi tìm kiếm.
let debounceTimer = null;
watch(search, (val) => {
    if (selectedProduct.value && selectedProduct.value.name === val) return;
    selectedProduct.value = null;
    clearTimeout(debounceTimer);
    if (!val.trim()) { products.value = []; showSuggestions.value = false; return; }
    debounceTimer = setTimeout(findProducts, 320);
});

const autocompleteRef = ref(null);
function handleClickOutside(e) {
    if (autocompleteRef.value && !autocompleteRef.value.contains(e.target)) {
        showSuggestions.value = false;
    }
}

onMounted(() => {
    initialize();
    document.addEventListener('click', handleClickOutside);
});
onUnmounted(() => {
    clearTimeout(debounceTimer);
    document.removeEventListener('click', handleClickOutside);
});
</script>

<template>
  <MarketplaceLayout>
    <main class="market-container market-page checkout-page" :aria-busy="loading || busy">
      <header class="market-page-heading">
        <p class="eyebrow">Đơn mua của bạn</p>
        <h1>{{ receipt ? 'Đặt hàng thành công' : review ? 'Xác nhận đơn hàng' : 'Đặt hàng' }}</h1>
        <p>{{ receipt ? 'Đơn đã được gửi đến cửa hàng để xác nhận.' : 'Chọn sản phẩm, địa chỉ nhận hàng và xem lại trước khi đặt.' }}</p>
      </header>
      <div v-if="error" ref="errorBox" class="checkout-error" role="alert" tabindex="-1">
        <p>{{ error }}</p>
        <button v-if="!quote && !receipt" class="button button-outline" :disabled="loading || busy" @click="initialize">Tải lại</button>
      </div>
      <p v-if="loading" role="status">Đang tải thông tin đặt hàng…</p>
      <section v-else-if="receipt" class="checkout-card success" aria-label="Kết quả đặt hàng">
        <div class="success-mark" aria-hidden="true">✓</div>
        <h2>Cảm ơn bạn đã đặt hàng</h2>
        <p>Đơn đã ghi nhận COD và đang chờ gian hàng xác nhận. Sản phẩm của gian hàng khác vẫn ở trong giỏ.</p>
        <ul class="receipt-list">
          <li v-for="order in receipt.orders" :key="order.id">
            <strong>{{ order.storeName }}</strong><span class="order-code">{{ order.code }}</span>
            <span>Chờ xác nhận</span><strong>{{ money(order.total) }}</strong>
          </li>
        </ul>
        <p class="total-row"><span>Tổng giá trị đơn hàng</span><strong>{{ money(receipt.total) }}</strong></p>
        <div class="receipt-actions">
          <RouterLink class="button button-outline" to="/orders">
            Xem đơn hàng của tôi
          </RouterLink>
          <RouterLink class="button button-secondary" to="/products">
            Tiếp tục mua sắm
          </RouterLink>
        </div>
      </section>
      <template v-else-if="review">
        <div class="checkout-grid">
          <section class="checkout-card">
            <h2>Thông tin nhận hàng</h2>
            <p><strong>{{ review.address.recipientName }}</strong> · {{ review.address.phone }}</p>
            <p>{{ addressText(review.address) }}</p>
            <p v-if="review.notes">Ghi chú: {{ review.notes }}</p>
            <h2 class="section-gap">Sản phẩm đã chọn</h2>
            <ul class="item-list">
              <li v-for="item in review.items" :key="item.productId">
                <div><strong>{{ item.name }}</strong><p>{{ item.storeName }} · {{ item.quantity }} × {{ money(item.unitPrice) }}</p></div>
                <strong>{{ money(item.lineTotal) }}</strong>
              </li>
            </ul>
          </section>
          <section class="checkout-card totals">
            <h2>Dịch vụ giao hàng</h2>
            <fieldset class="checkout-options" :disabled="busy || uncertain">
              <legend>Chọn dịch vụ GHN thử nghiệm</legend>
              <label v-for="option in quote.shippingQuotes" :key="option.id" class="shipping-option">
                <input v-model="selectedShipping" type="radio" name="checkout-shipping" :value="option.id"/>
                <span>{{ option.serviceName }} · {{ money(option.shippingFee) }}</span>
              </label>
            </fieldset>
            <h2>Tổng thanh toán</h2>
            <div v-for="store in review.stores" :key="store.storeId" class="store-total">
              <strong>{{ store.storeName }}</strong>
              <p><span>Tiền sản phẩm</span><span>{{ money(store.subtotal) }}</span></p>
              <p><span>Phí GHN đã chọn</span><span>{{ rate ? money(rate.shippingFee) : 'Chưa chọn dịch vụ' }}</span></p>
            </div>
            <p class="total-row"><span>Tổng cộng</span><strong>{{ rate ? money(rate.total) : 'Chọn dịch vụ để xem tổng' }}</strong></p>
            <p class="muted">Thanh toán COD khi nhận hàng. Tổng tiền và tồn kho được kiểm tra trên máy chủ khi đặt. Thu hộ và đối soát là các bước riêng.</p>
            <p v-if="uncertain && !busy" role="status">Chưa nhận được kết quả. Bấm thử lại để kiểm tra và tiếp tục cùng yêu cầu đặt hàng.</p>
            <button class="button button-primary full-width" :disabled="busy || !rate" @click="confirm">{{ busy ? 'Đang xử lý…' : uncertain ? 'Thử lại xác nhận' : 'Xác nhận đặt hàng' }}</button>
            <button class="button button-outline full-width" :disabled="busy || uncertain" @click="editOrder">Quay lại chỉnh sửa</button>
          </section>
        </div>
      </template>
      <template v-else-if="!loading">
        <section v-if="addressEditor" class="checkout-card">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; padding-bottom: 14px; border-bottom: 1px solid #dce3da;">
            <button class="button button-outline" type="button" :disabled="busy" @click="finishAddresses">← Quay lại đặt hàng</button>
            <button class="button button-primary" type="button" :disabled="busy" @click="finishAddresses">✓ Đã xong — Sử dụng địa chỉ này</button>
          </div>
          <AddressManagement/>
          <div style="margin-top: 24px; padding-top: 16px; border-top: 1px solid #dce3da;">
            <button class="button button-primary" type="button" :disabled="busy" @click="finishAddresses">✓ Đã lưu địa chỉ — Quay lại đặt hàng</button>
          </div>
        </section>
        <form v-else class="checkout-grid" @submit.prevent="reviewOrder">
          <section class="checkout-card">
            <h2>1. Chọn sản phẩm</h2>
            <div v-if="source === 'CART' && stores.length" class="store-selection">
              <label for="checkout-store">Gian hàng đặt lần này</label>
              <select id="checkout-store" v-model="selectedStore" :disabled="busy" required><option :value="null" disabled>Chọn gian hàng</option><option v-for="store in stores" :key="store.id" :value="store.id">{{ store.name }}</option></select>
              <p class="muted">Mỗi lần đặt một gian hàng; các sản phẩm khác vẫn ở trong giỏ.</p>
            </div>
            <div class="source-switch" role="group" aria-label="Nguồn sản phẩm">
              <button type="button" :aria-pressed="source === 'CART'" :disabled="busy" @click="switchSource('CART')">Từ giỏ hàng</button>
              <button type="button" :aria-pressed="source === 'BUY_NOW'" :disabled="busy" @click="switchSource('BUY_NOW')">Mua ngay</button>
            </div>
            <div v-if="source === 'BUY_NOW'" class="buy-now">
              <label for="checkout-search">Tìm cây bạn muốn mua</label>
              <div ref="autocompleteRef" class="autocomplete-wrap">
                <div class="search-row">
                  <div class="search-input-wrap">
                    <input id="checkout-search" v-model="search" maxlength="160"
                      :placeholder="searching ? 'Đang tìm…' : 'Nhập tên sản phẩm…'"
                      :disabled="busy" autocomplete="off"
                      @keydown="onSearchKeydown"
                      @focus="showSuggestions = products.length > 0"/>
                    <button v-if="selectedProduct || search" type="button" class="clear-btn" aria-label="Xóa" @click="clearSelection">✕</button>
                    <span v-if="searching" class="spin-icon" aria-hidden="true">⟳</span>
                  </div>
                </div>
                <!-- Danh sách gợi ý tìm kiếm -->
                <ul v-if="showSuggestions && products.length" class="suggestion-list" role="listbox">
                  <li v-for="(product, idx) in products" :key="product.id"
                      role="option" :aria-selected="idx === highlightedIndex"
                      :class="['suggestion-item', {highlighted: idx === highlightedIndex}]"
                      @mousedown.prevent="selectProduct(product)">
                    <img v-if="usableImage(product.imageUrl)" :src="usableImage(product.imageUrl)" :alt="product.name" class="sug-img"/>
                    <span v-else class="sug-img sug-img-placeholder">🌿</span>
                    <span class="sug-info">
                      <span class="sug-name">{{ product.name }}</span>
                      <span class="sug-store">{{ product.store?.name }}</span>
                    </span>
                    <span class="sug-price">{{ money(product.price) }}</span>
                  </li>
                </ul>
                <p v-else-if="!searching && search && !products.length" class="muted no-result">Không tìm thấy sản phẩm phù hợp.</p>
                <p v-else-if="!search && !selectedProduct" class="muted">Gõ tên cây để tìm sản phẩm đang có hàng.</p>
              </div>
              <!-- Sản phẩm đã chọn -->
              <div v-if="selectedProduct" class="selected-product-card">
                <img v-if="usableImage(selectedProduct.imageUrl)" :src="usableImage(selectedProduct.imageUrl)" :alt="selectedProduct.name"/>
                <span v-else class="sug-img-placeholder" style="font-size:28px">🌿</span>
                <div class="sel-info">
                  <strong>{{ selectedProduct.name }}</strong>
                  <span>{{ selectedProduct.store?.name }} · {{ money(selectedProduct.price) }}</span>
                </div>
              </div>
              <label for="checkout-quantity">Số lượng</label>
              <input id="checkout-quantity" v-model.number="quantity" type="number" min="1" max="1000000" step="1" required :disabled="busy"/>
            </div>
            <p v-if="!selectedItems.length && source === 'CART'" class="empty-cart">Giỏ hàng chưa có sản phẩm. Bạn có thể chọn “Mua ngay” để đặt một sản phẩm.</p>
            <ul class="item-list">
              <li v-for="item in selectedItems" :key="item.productId">
                <img v-if="usableImage(item.imageUrl)" :src="usableImage(item.imageUrl)" :alt="item.name" @error="$event.target.hidden = true"/>
                <div><strong>{{ item.name }}</strong><p>{{ item.storeName }} · Số lượng: {{ item.quantity }}</p></div>
                <span>{{ money(item.unitPrice) }}</span>
              </li>
            </ul>
          </section>
          <section class="checkout-card">
            <h2>2. Địa chỉ nhận hàng</h2>
            <p v-if="!data.addresses.length" class="muted">Thêm địa chỉ nhận hàng trước khi tiếp tục.</p>
            <label v-for="address in data.addresses" :key="address.id" class="address-option" :class="{selected: selectedAddress === address.id}">
              <input v-model="selectedAddress" type="radio" name="checkout-address" :value="address.id" required :disabled="busy"/>
              <span><strong>{{ address.recipientName }}</strong> · {{ address.phone }}<small>{{ addressText(address) }}</small></span>
            </label>
            <button class="button button-outline" type="button" :disabled="busy" @click="addressEditor = true">Thêm hoặc sửa địa chỉ</button>
            <label for="checkout-notes" class="section-gap">Ghi chú cho người bán (tùy chọn)</label>
            <textarea id="checkout-notes" v-model="notes" maxlength="1000" rows="3" :disabled="busy"/>
            <p class="muted">Phí GHN được lấy theo địa chỉ và kiện hàng đã đo. Chỉ đặt được khi gian hàng đủ điều kiện COD thử nghiệm.</p>
            <button class="button button-primary full-width" :disabled="busy || !selectedAddress || !selectedItems.length || (source === 'CART' && !selectedStore)" type="submit">{{ busy ? 'Đang kiểm tra…' : 'Xem lại đơn hàng' }}</button>
          </section>
        </form>
      </template>
    </main>
  </MarketplaceLayout>
</template>

<style scoped>
.shipping-option {display:flex;gap:12px;align-items:center;min-height:44px;padding-block:8px;}.checkout-options {border:0;padding:0;margin:0 0 24px;}.checkout-options input {accent-color:var(--bm-forest);width:20px;height:20px;min-height:20px;flex-shrink:0;margin:0;appearance:auto;}.checkout-page {max-width: 1180px; padding-bottom: 64px;}
.eyebrow {text-transform: uppercase; letter-spacing: .12em; font-size: 12px; color: var(--ag-primary-500);}
.checkout-grid {display: grid; grid-template-columns: 1.15fr 1fr; gap: 24px; align-items: start;}
.checkout-card {padding: 28px; border: 1px solid var(--ag-border, #dce3da); border-radius: 20px; background: var(--ag-surface, white);}
.checkout-card h2 {font-size: 22px; margin: 0 0 20px;}
.checkout-card p {line-height: 1.6;}
.muted, small {color: var(--ag-text-secondary, #59635b); font-size: 14px;}
.checkout-error {padding: 16px 20px; margin-bottom: 20px; border-radius: 12px; background: #fff0ed; color: #922e20;}
.source-switch, .search-row {display: flex; gap: 10px; margin-bottom: 20px;}
.source-switch button {flex: 1; padding: 12px; border: 1px solid #ccd8cc; border-radius: 10px; background: transparent; cursor: pointer; font: inherit;}
.source-switch button[aria-pressed=true] {background: #e7efe4; color: #174c35; border-color: #52764e; font-weight: 600;}
.buy-now {display: grid; gap: 10px;}
.buy-now input[type=number] {max-width: 120px;}
input:not([type=radio]), select, textarea {width: 100%; box-sizing: border-box; padding: 12px; border: 1px solid #cbd5cb; border-radius: 9px; font: inherit; background: var(--ag-surface, white); color: inherit;}
input:focus-visible, select:focus-visible, textarea:focus-visible, button:focus-visible {outline: 2px solid #366646; outline-offset: 3px;}
.search-row input {min-width: 0;}
.autocomplete-wrap {position: relative; margin-bottom: 6px;}
.search-input-wrap {position: relative; width: 100%;}
.search-input-wrap input {padding-right: 36px;}
.clear-btn {position: absolute; right: 10px; top: 50%; transform: translateY(-50%); background: none; border: none; font-size: 14px; cursor: pointer; color: #888; padding: 4px 8px; border-radius: 50%;}
.clear-btn:hover {color: #333; background: #eee;}
.spin-icon {position: absolute; right: 38px; top: 50%; transform: translateY(-50%); animation: spin 1s linear infinite; font-size: 16px; color: #52764e;}
@keyframes spin {100% {transform: translateY(-50%) rotate(360deg);}}
.suggestion-list {position: absolute; top: calc(100% + 4px); left: 0; right: 0; z-index: 50; background: var(--ag-surface, #fff); border: 1px solid #c7d4c5; border-radius: 12px; box-shadow: 0 10px 25px rgba(0,0,0,0.12); max-height: 280px; overflow-y: auto; list-style: none; margin: 0; padding: 6px 0;}
.suggestion-item {display: flex; align-items: center; gap: 12px; padding: 10px 14px; cursor: pointer; transition: background .15s;}
.suggestion-item:hover, .suggestion-item.highlighted {background: #f0f6ee;}
.sug-img {width: 44px; height: 44px; border-radius: 8px; object-fit: cover;}
.sug-img-placeholder {display: inline-grid; place-items: center; width: 44px; height: 44px; border-radius: 8px; background: #eef2ec;}
.sug-info {flex: 1; min-width: 0; display: flex; flex-direction: column;}
.sug-name {font-weight: 600; font-size: 14px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;}
.sug-store {font-size: 12px; color: var(--ag-text-secondary, #666);}
.sug-price {font-weight: 700; color: #285b3e; font-size: 14px; white-space: nowrap;}
.no-result {padding: 8px 4px; font-style: italic;}
.selected-product-card {display: flex; align-items: center; gap: 14px; padding: 12px 16px; background: #f3f7ef; border: 1px solid #cbdcc8; border-radius: 12px; margin: 8px 0 14px;}
.selected-product-card img {width: 52px; height: 52px; border-radius: 10px; object-fit: cover;}
.sel-info {display: flex; flex-direction: column; gap: 2px;}
.sel-info strong {font-size: 15px; color: #174c35;}
.sel-info span {font-size: 13px; color: #496353;}
.item-list, .receipt-list {list-style: none; padding: 0; margin: 20px 0;}
.item-list li {display: flex; align-items: center; gap: 14px; padding: 16px 0; border-bottom: 1px solid #e4e9e1;}
.item-list li div {flex: 1; min-width: 0;}
.item-list li p {margin: 6px 0 0; font-size: 14px; color: var(--ag-text-secondary);}
.item-list img {width: 64px; height: 64px; border-radius: 10px; object-fit: cover;}
.address-option {display: flex; align-items: flex-start; gap: 10px; padding: 16px; border: 1px solid #d9e0d7; border-radius: 12px; margin-bottom: 12px; cursor: pointer;}
.address-option.selected {border-color: #52764e; background: #f3f7ef;}
.address-option input {margin-top: 5px; accent-color: #315c3e;}
.address-option small {display: block; margin-top: 8px; line-height: 1.5;}
.section-gap {display: block; margin-top: 28px !important; margin-bottom: 10px;}
.full-width {width: 100%; margin-top: 12px;}
.store-total {border-bottom: 1px solid #e4e9e1; padding: 14px 0;}
.store-total p, .total-row {display: flex; justify-content: space-between; gap: 16px;}
.total-row {padding: 18px 0; font-size: 19px;}
.total-row strong {color: #285b3e;}
.success {max-width: 760px; margin: auto;}
.success-mark {display: grid; place-items: center; width: 56px; height: 56px; border-radius: 50%; background: #e4f0df; color: #285b3e; font-size: 30px; margin-bottom: 20px;}
.receipt-list li {display: grid; grid-template-columns: 1fr auto; gap: 10px; border-bottom: 1px solid #e4e9e1; padding: 18px 0;}
.receipt-actions {display: flex; flex-wrap: wrap; gap: 12px; margin-top: 20px;}
.order-code {font-size: 12px; overflow-wrap: anywhere; grid-column: 1 / -1;}
button:disabled {opacity: .55; cursor: not-allowed;}
@media(max-width: 800px) {.checkout-grid {grid-template-columns: 1fr;} .checkout-card {padding: 20px;} .item-list li {flex-wrap: wrap;} }
</style>

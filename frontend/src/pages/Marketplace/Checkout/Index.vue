<script setup>
import {computed, onMounted, onUnmounted, ref, nextTick, watch} from 'vue';
import {useRoute, useRouter, onBeforeRouteUpdate, onBeforeRouteLeave} from 'vue-router';
import MarketplaceLayout from '../../../layouts/MarketplaceLayout.vue';
import MarketIcon from '../../../components/marketplace/MarketIcon.vue';
import CatalogImage from '../../../components/marketplace/CatalogImage.vue';
import AddressManagement from '../../../components/marketplace/AddressManagement.vue';
import {productPage, usableImage} from '../../../services/catalog.js';
import {apiGet} from '../../../services/api.js';
import {loadCheckout, createQuote, placeOrder, loadQuote, loadReceipt} from '../../../services/checkout.js';

const route = useRoute(), router = useRouter();
const loading = ref(true), busy = ref(false), error = ref(''), errorBox = ref(null), ready=ref(false);
const data = ref({items: [], addresses: [], shippingFeePerStore: 0});
const selectedStore = ref(null), selectedShipping = ref(null);
const source = ref('CART'), selectedAddress = ref(null), notes = ref('');
const quote = ref(null), receipt = ref(null), addressEditor = ref(false), uncertain = ref(false);
const search = ref(''), products = ref([]), selectedProduct = ref(null), quantity = ref(1), searching = ref(false);
const showSuggestions = ref(false), highlightedIndex = ref(-1);
const searchError=ref(''),searched=ref(false);
let searchSequence=0;
let initializationSequence=0,ownNavigation=false;
async function updateUrl(query){ownNavigation=true;try{await router.replace({path:'/checkout',query});}finally{ownNavigation=false;}}
onBeforeRouteUpdate(()=>!busy.value || ownNavigation);
onBeforeRouteLeave(()=>!busy.value);
const review = computed(() => quote.value?.review);
const rate = computed(() => quote.value?.shippingQuotes?.find(q => q.id === selectedShipping.value));
const stores = computed(() => [...new Map(data.value.items.map(i => [i.storeId, {id: i.storeId, name: i.storeName}])).values()]);
const money = value => typeof value==='number'&&Number.isFinite(value) ? new Intl.NumberFormat('vi-VN', {style: 'currency', currency: 'VND'}).format(value) : 'Chưa xác định';
const maximumQuantity=computed(()=>Number.isInteger(selectedProduct.value?.stock)&&selectedProduct.value.stock>=0?Math.min(selectedProduct.value.stock,1000000):1000000);
const validQuantity=computed(()=>Number.isSafeInteger(quantity.value)&&quantity.value>=1&&quantity.value<=maximumQuantity.value);
const quoteExpiry=computed(()=>{
    const time=new Date(quote.value?.expiresAt||'');
    return Number.isFinite(time.getTime())?time.toLocaleString('vi-VN',{timeZone:'Asia/Ho_Chi_Minh'}):'';
});
const addressText = address => [address.addressDetail, address.wardName, address.districtName, address.provinceName].filter(Boolean).join(', ');
const selectedItems = computed(() => source.value === 'CART' ? data.value.items.filter(i => !selectedStore.value || i.storeId === selectedStore.value) : selectedProduct.value
    ? [{productId: selectedProduct.value.id, name: selectedProduct.value.name, imageUrl: selectedProduct.value.imageUrl,
        quantity: quantity.value, unitPrice: selectedProduct.value.price, storeName: selectedProduct.value.store?.name}] : []);

async function showError(e) {error.value = e.message || 'Không thể xử lý yêu cầu.'; await nextTick(); errorBox.value?.focus();}
async function refresh(ticket=initializationSequence) {
    const result=await loadCheckout();
    if(ticket!==initializationSequence)return;
    data.value=result;ready.value=true;
    const requestedStore=Number(route.query.store);
    if(selectedStore.value===null&&stores.value.some(s=>s.id===requestedStore))selectedStore.value=requestedStore;
    if (!stores.value.some(s => s.id === selectedStore.value)) selectedStore.value = stores.value.length === 1 ? stores.value[0].id : null;
    if (!data.value.addresses.some(a => a.id === selectedAddress.value))
        selectedAddress.value = (data.value.addresses.find(a => a.isDefault) || data.value.addresses[0])?.id ?? null;
}
async function initialize() {
    const ticket=++initializationSequence,query={...route.query};
    loading.value = true; error.value = '';ready.value=false;
    searchSequence++;clearTimeout(debounceTimer);searching.value=false;searchError.value='';searched.value=false;showSuggestions.value=false;
    quote.value=null;receipt.value=null;selectedProduct.value=null;selectedShipping.value=null;source.value='CART';uncertain.value=false;
    try {
        if (query.receipt) {const result=await loadReceipt(String(query.receipt));if(ticket===initializationSequence)receipt.value=result;}
        else if (query.quote) {
            // Khôi phục đơn khi phản hồi thành công có thể đã bị mất.
            try {const result=await loadReceipt(String(query.quote));if(ticket===initializationSequence)receipt.value=result;}
            catch (e) {if (e.status !== 404) throw e;}
            if (ticket===initializationSequence && !receipt.value) {
                const result=await loadQuote(String(query.quote));
                if(ticket===initializationSequence){quote.value=result;notes.value=result.review?.notes||'';}
            }
        } else {
            await refresh(ticket);
            if(ticket!==initializationSequence)return;
            if (query.productId) {
                source.value = 'BUY_NOW';
                const product=await apiGet('/products/' + encodeURIComponent(String(query.productId)));
                if(ticket!==initializationSequence)return;
                selectedProduct.value = product;
                search.value=product.name;
                const count = Number(query.quantity || 1);
                quantity.value = Number.isInteger(count) && count >= 1 && count <= 1000000 ? count : 1;
            }
        }
    } catch (e) {if(ticket===initializationSequence){ready.value=false;await showError(e);}} finally {if(ticket===initializationSequence)loading.value = false;}
}
async function retryCheckout(){
    if(busy.value||loading.value)return;
    if(!ready.value){await initialize();return;}
    // Giữ sản phẩm mua ngay, số lượng, địa chỉ và ghi chú khi kết nối phục hồi.
    busy.value=true;error.value='';
    try{await refresh();}catch(e){await showError(e);}finally{busy.value=false;}
}
async function findProducts() {
    if(busy.value||loading.value)return;
    const ticket=++searchSequence;
    searching.value = true;searchError.value='';searched.value=false;showSuggestions.value=false;
    try {
        const result=await productPage({search: search.value, in_stock: true, size: 12});
        if(ticket!==searchSequence)return;
        products.value = result.content;
        searched.value=true;
        showSuggestions.value = true;
        highlightedIndex.value = -1;
    }
    catch (e) {if(ticket===searchSequence){searchError.value=e.message||'Chưa tìm được sản phẩm. Thử lại với từ khóa đang nhập.';products.value=[];}} finally {if(ticket===searchSequence)searching.value = false;}
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
    if(source.value==='BUY_NOW'&&(!selectedProduct.value||!validQuantity.value)){await showError(new Error('Chọn sản phẩm và nhập số lượng hợp lệ trước khi xem lại đơn hàng.'));return;}
    searchSequence++;clearTimeout(debounceTimer);searching.value=false;showSuggestions.value=false;
    busy.value = true; error.value = '';
    try {
        const body = {source: source.value, addressId: selectedAddress.value, notes: notes.value, storeId: selectedStore.value};
        if (source.value === 'BUY_NOW') Object.assign(body, {productId: selectedProduct.value?.id, quantity: quantity.value});
        quote.value = await createQuote(body); selectedShipping.value = null;
        await updateUrl({quote: quote.value.quoteId});
        await focusHeading();
    } catch (e) {await showError(e);} finally {busy.value = false;}
}
async function confirm() {
    if (busy.value || !quote.value || !rate.value) return;
    busy.value = true; error.value = ''; uncertain.value = true;
    try {
        receipt.value = await placeOrder(quote.value.quoteId, selectedShipping.value);
        uncertain.value = false;
        await updateUrl({receipt: receipt.value.quoteId});
        await focusHeading();
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
        await updateUrl({});
    } catch (e) {await showError(e);} finally {busy.value = false;}
}
function selectProduct(product) {
    searchSequence++;searching.value=false;
    selectedProduct.value = product;
    search.value = product.name;
    showSuggestions.value = false;
    highlightedIndex.value = -1;
    quantity.value=1;searchError.value='';
}
function clearSelection() {
    if(busy.value)return;
    searchSequence++;searching.value=false;clearTimeout(debounceTimer);
    selectedProduct.value = null;
    search.value = '';
    products.value = [];
    showSuggestions.value = false;
    searchError.value='';searched.value=false;
    document.getElementById('checkout-search')?.focus();
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
    if(['ArrowDown','ArrowUp'].includes(e.key))nextTick(()=>document.getElementById('checkout-option-'+highlightedIndex.value)?.scrollIntoView({block:'nearest'}));
}
async function focusHeading(){await nextTick();document.getElementById('checkout-title')?.focus();}

// Chờ người dùng ngừng gõ rồi tìm kiếm.
let debounceTimer = null;
watch(search, (val) => {
    if (selectedProduct.value && selectedProduct.value.name === val) return;
    selectedProduct.value = null;
    clearTimeout(debounceTimer);
    searchSequence++;searching.value=false;
    searchError.value='';searched.value=false;
    if (!val.trim()) { products.value = []; showSuggestions.value = false; return; }
    debounceTimer = setTimeout(findProducts, 320);
});

const autocompleteRef = ref(null);
function handleClickOutside(e) {
    if (autocompleteRef.value && !autocompleteRef.value.contains(e.target)) {
        showSuggestions.value = false;
    }
}

watch(()=>route.fullPath,()=>{if(!ownNavigation)initialize();},{immediate:true});
onMounted(() => {
    document.addEventListener('click', handleClickOutside);
});
onUnmounted(() => {
    searchSequence++;
    initializationSequence++;
    clearTimeout(debounceTimer);
    document.removeEventListener('click', handleClickOutside);
});
</script>

<template>
  <MarketplaceLayout>
    <main class="market-container market-page checkout-page" :aria-busy="loading || busy">
      <header class="market-page-heading">
        <h1 id="checkout-title" tabindex="-1">{{ receipt ? 'Đặt hàng thành công' : review ? 'Xác nhận đơn hàng' : 'Đặt hàng' }}</h1>
        <p>{{ receipt ? 'Đơn đã được gửi đến cửa hàng để xác nhận.' : 'Chọn sản phẩm, địa chỉ nhận hàng và xem lại trước khi đặt.' }}</p>
      </header>
      <ol class="checkout-progress" aria-label="Tiến trình đặt hàng"><li :aria-current="!review && !receipt?'step':undefined"><span aria-hidden="true">1</span>Sản phẩm & địa chỉ</li><li :aria-current="review && !receipt?'step':undefined"><span aria-hidden="true">2</span>Vận chuyển & COD</li><li :aria-current="receipt?'step':undefined"><span aria-hidden="true">3</span>Đơn đã ghi nhận</li></ol>
      <div v-if="error" ref="errorBox" class="checkout-error" role="alert" tabindex="-1">
        <p>{{ error }}</p>
        <button v-if="!quote && !receipt" class="button button-outline" :disabled="loading || busy" @click="retryCheckout">{{ ready?'Cập nhật thông tin đặt hàng':'Thử tải lại' }}</button>
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
            <p v-if="!quote.shippingQuotes.length" class="checkout-warning" role="status">Chưa có dịch vụ giao hàng cho lựa chọn này. Quay lại kiểm tra địa chỉ hoặc thử lấy báo phí mới.</p>
            <fieldset class="checkout-options" :disabled="busy || uncertain">
              <legend>Chọn dịch vụ GHN thử nghiệm</legend>
              <label v-for="option in quote.shippingQuotes" :key="option.id" class="shipping-option">
                <input v-model="selectedShipping" type="radio" name="checkout-shipping" :value="option.id"/>
                <span>{{ option.serviceName }} · {{ money(option.shippingFee) }}</span>
              </label>
            </fieldset>
            <p v-if="quoteExpiry" class="quote-validity">Báo phí có hiệu lực đến <time :datetime="quote.expiresAt">{{ quoteExpiry }}</time> (giờ Việt Nam). Hệ thống kiểm tra lại hiệu lực khi xác nhận.</p>
            <h2>Tổng thanh toán</h2>
            <div v-for="store in review.stores" :key="store.storeId" class="store-total">
              <strong>{{ store.storeName }}</strong>
              <p><span>Tiền sản phẩm</span><span>{{ money(store.subtotal) }}</span></p>
              <p><span>Phí GHN đã chọn</span><span>{{ rate ? money(rate.shippingFee) : 'Chưa chọn dịch vụ' }}</span></p>
            </div>
            <p class="total-row"><span>Tổng cộng</span><strong>{{ rate ? money(rate.total) : 'Chọn dịch vụ để xem tổng' }}</strong></p>
            <p class="muted">Thanh toán COD khi nhận hàng. Tổng tiền và tồn kho được kiểm tra trên máy chủ khi đặt. Thu hộ và đối soát là các bước riêng.</p>
            <p v-if="uncertain && !busy" role="status">Chưa nhận được kết quả. Bấm thử lại để kiểm tra và tiếp tục cùng yêu cầu đặt hàng.</p>
            <button class="button button-primary full-width" :disabled="busy || !rate" :aria-busy="busy" @click="confirm">{{ busy ? 'Đang xử lý…' : uncertain ? 'Thử lại xác nhận' : 'Xác nhận đặt hàng COD' }}</button>
            <button class="button button-outline full-width" :disabled="busy || uncertain" @click="editOrder">Quay lại chỉnh sửa</button>
          </section>
        </div>
      </template>
      <template v-else-if="ready">
        <section v-if="addressEditor" class="checkout-card">
          <div class="checkout-address-actions">
            <button class="button button-outline" type="button" :disabled="busy" @click="finishAddresses">← Quay lại đặt hàng</button>
            <button class="button button-primary" type="button" :disabled="busy" @click="finishAddresses">Quay lại chọn địa chỉ nhận hàng</button>
          </div>
          <AddressManagement/>
          <div class="checkout-address-footer">
            <button class="button button-primary" type="button" :disabled="busy" @click="finishAddresses">Quay lại đặt hàng</button>
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
                      :disabled="busy" autocomplete="off" :aria-describedby="searchError?'checkout-search-error':'checkout-search-help'"
                      role="combobox" :aria-expanded="showSuggestions && products.length>0" aria-controls="checkout-suggestions" aria-autocomplete="list" :aria-activedescendant="showSuggestions && highlightedIndex>=0?'checkout-option-'+highlightedIndex:undefined"
                      @keydown="onSearchKeydown"
                      @focus="showSuggestions = products.length > 0"/>
                    <button v-if="selectedProduct || search" type="button" class="clear-btn" aria-label="Xóa sản phẩm và từ khóa tìm kiếm" :disabled="busy" @click="clearSelection"><MarketIcon name="close"/></button>
                    <span v-if="searching" class="spin-icon" aria-hidden="true">⟳</span>
                  </div>
                </div>
                <!-- Danh sách gợi ý tìm kiếm -->
                <ul v-if="showSuggestions && products.length" id="checkout-suggestions" class="suggestion-list" role="listbox" aria-label="Sản phẩm đang có hàng">
                  <li v-for="(product, idx) in products" :key="product.id"
                      :id="'checkout-option-'+idx" role="option" :aria-selected="idx === highlightedIndex"
                      :class="['suggestion-item', {highlighted: idx === highlightedIndex}]"
                      @click="selectProduct(product)">
                    <CatalogImage :src="usableImage(product.imageUrl)" :alt="product.name" class="sug-img" compact fit="cover"/>
                    <span class="sug-info">
                      <span class="sug-name">{{ product.name }}</span>
                      <span class="sug-store">{{ product.store?.name }}</span>
                    </span>
                    <span class="sug-price">{{ money(product.price) }}</span>
                  </li>
                </ul>
                <div v-if="searchError" id="checkout-search-error" class="checkout-search-error" role="alert"><p>{{ searchError }}</p><button type="button" class="button button-outline" :disabled="busy||searching" @click="findProducts">Tìm lại</button></div>
                <p v-else-if="searching" role="status">Đang tìm sản phẩm…</p>
                <p v-else-if="searched && search && !products.length && !selectedProduct" class="muted no-result" role="status">Không tìm thấy sản phẩm đang có hàng với từ khóa này. Thử tên cây hoặc từ khóa ngắn hơn.</p>
                <p id="checkout-search-help" class="muted">Gõ tên để tìm, dùng phím mũi tên và Enter để chọn; Escape để đóng gợi ý.</p>
              </div>
              <!-- Sản phẩm đã chọn -->
              <div v-if="selectedProduct" class="selected-product-card">
                <CatalogImage :src="usableImage(selectedProduct.imageUrl)" :alt="selectedProduct.name" class="selected-product-image" compact fit="cover"/>
                <div class="sel-info">
                  <strong>{{ selectedProduct.name }}</strong>
                  <span>{{ selectedProduct.store?.name }} · {{ money(selectedProduct.price) }}</span>
                </div>
              </div>
              <label for="checkout-quantity">Số lượng</label>
              <input id="checkout-quantity" v-model.number="quantity" type="number" inputmode="numeric" min="1" :max="maximumQuantity" step="1" required :disabled="busy" :aria-invalid="!validQuantity" :aria-describedby="!validQuantity?'checkout-quantity-error':undefined"/>
              <p v-if="!validQuantity" id="checkout-quantity-error" class="checkout-search-error">{{ maximumQuantity===0?'Sản phẩm hiện đã hết hàng. Chọn sản phẩm khác để tiếp tục.':'Nhập số nguyên từ 1 đến '+maximumQuantity+'.' }}</p>
            </div>
            <p v-if="!selectedItems.length && source === 'CART'" class="empty-cart">Giỏ hàng chưa có sản phẩm. Bạn có thể chọn “Mua ngay” để đặt một sản phẩm.</p>
            <ul class="item-list">
              <li v-for="item in selectedItems" :key="item.productId">
                <CatalogImage :src="usableImage(item.imageUrl)" :alt="item.name" class="checkout-item-image" compact fit="cover"/>
                <div><strong>{{ item.name }}</strong><p>{{ item.storeName }} · Số lượng: {{ item.quantity }}</p></div>
                <span>{{ money(item.unitPrice) }}</span>
              </li>
            </ul>
          </section>
          <section class="checkout-card">
            <h2>2. Địa chỉ nhận hàng</h2>
            <p v-if="!data.addresses.length" class="muted">Thêm địa chỉ nhận hàng trước khi tiếp tục.</p>
            <fieldset class="checkout-options" :disabled="busy"><legend class="sr-only">Chọn địa chỉ nhận hàng</legend><label v-for="address in data.addresses" :key="address.id" class="address-option" :class="{selected: selectedAddress === address.id}">
              <input v-model="selectedAddress" type="radio" name="checkout-address" :value="address.id" required :disabled="busy"/>
              <span><strong>{{ address.recipientName }}</strong> · {{ address.phone }}<small>{{ addressText(address) }}</small><small v-if="address.isDefault">Địa chỉ mặc định</small></span>
            </label></fieldset>
            <button class="button button-outline" type="button" :disabled="busy" @click="addressEditor = true">Thêm hoặc sửa địa chỉ</button>
            <label for="checkout-notes" class="section-gap">Ghi chú cho người bán (tùy chọn)</label>
            <textarea id="checkout-notes" v-model="notes" maxlength="1000" rows="3" :disabled="busy" aria-describedby="checkout-notes-help"/>
            <p id="checkout-notes-help" class="muted">{{ notes.length }}/1.000 ký tự. Ghi chú được giữ khi yêu cầu chưa thành công.</p>
            <p class="muted">Phí GHN được lấy theo địa chỉ và kiện hàng đã đo. Chỉ đặt được khi gian hàng đủ điều kiện COD thử nghiệm.</p>
            <button class="button button-primary full-width" :disabled="busy || !selectedAddress || !selectedItems.length || (source === 'CART' && !selectedStore) || (source==='BUY_NOW' && !validQuantity)" type="submit">{{ busy ? 'Đang kiểm tra…' : 'Xem phí giao hàng & kiểm tra đơn' }}</button>
          </section>
        </form>
      </template>
    </main>
  </MarketplaceLayout>
</template>

<style scoped>
.shipping-option {display:flex;gap:12px;align-items:center;min-height:44px;padding-block:8px;}.checkout-options {border:0;padding:0;margin:0 0 24px;}.checkout-options input {accent-color:var(--bm-forest);width:20px;height:20px;min-height:20px;flex-shrink:0;margin:0;appearance:auto;}.checkout-page {max-width: 1180px; padding-bottom: 64px;}
.checkout-progress {display:flex;flex-wrap:wrap;gap:12px 24px;list-style:none;margin:0 0 28px;padding:20px 0;border-block:1px solid var(--bm-border);}
.checkout-progress li {display:flex;align-items:center;gap:8px;font-size:14px;color:var(--bm-muted);}
.checkout-progress li>span {display:grid;place-items:center;width:28px;height:28px;border:1px solid var(--bm-border);border-radius:50%;font-weight:700;background:var(--bm-surface);}
.checkout-progress [aria-current=step] {color:var(--bm-forest);font-weight:700;}
.checkout-progress [aria-current=step]>span {background:var(--bm-forest);border-color:var(--bm-forest);color:var(--bm-surface);}
.checkout-grid {display:grid;grid-template-columns:minmax(0,1.15fr) minmax(0,1fr);gap:24px;align-items:start;}
.checkout-card {min-width:0;padding:28px;border:1px solid var(--bm-border);border-radius:var(--bm-radius);background:var(--bm-surface);overflow-wrap:anywhere;}
.checkout-card h2 {font-size: 22px; margin: 0 0 20px;}
.checkout-card p {line-height: 1.6;}
.muted, small {color: var(--bm-muted); font-size: var(--bm-type-label);}
.checkout-address-actions {display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:var(--bm-space-3);margin-bottom:var(--bm-space-5);padding-bottom:var(--bm-space-4);border-bottom:1px solid var(--bm-border);}
.checkout-address-footer {margin-top:var(--bm-space-6);padding-top:var(--bm-space-4);border-top:1px solid var(--bm-border);}
.checkout-search-error {color:var(--bm-error);line-height:1.6;}
.checkout-search-error p {margin-top:0;}
.checkout-warning {padding:var(--bm-space-4);background:var(--bm-warning-surface);color:var(--bm-warning);border-radius:var(--bm-radius-control);}
.quote-validity {color:var(--bm-muted);font-size:var(--bm-type-label);line-height:1.6;}
.store-selection>label {display:block;font-weight:600;margin-bottom:var(--bm-space-2);}
.checkout-error {padding:16px 20px;margin-bottom:20px;border-radius:var(--bm-radius);background:var(--bm-error-surface);color:var(--bm-error);}
.source-switch, .search-row {display: flex; gap: 10px; margin-bottom: 20px;}
.source-switch button {flex:1;min-height:var(--bm-control-height);padding:var(--bm-space-3);border:1px solid var(--bm-control-border);border-radius:var(--bm-radius-control);background:transparent;cursor:pointer;font:inherit;}
.source-switch button[aria-pressed=true] {background:var(--bm-soft);color:var(--bm-forest);border-color:var(--bm-forest);font-weight:600;}
.buy-now {display: grid; gap: 10px;}
.buy-now input[type=number] {max-width: 120px;}
input:not([type=radio]), select, textarea {width:100%;min-width:0;min-height:var(--bm-control-height);box-sizing:border-box;padding:var(--bm-space-3);border:1px solid var(--bm-control-border);border-radius:var(--bm-radius-control);font:inherit;background:var(--bm-surface);color:inherit;}
textarea {resize:vertical;line-height:1.6;}
input:not([type=radio]):disabled,select:disabled,textarea:disabled {background:var(--bm-disabled-surface);color:var(--bm-disabled);cursor:not-allowed;}
:focus-visible {outline:var(--bm-focus-width) solid var(--bm-focus);outline-offset:var(--bm-focus-offset);}
.search-row input {min-width: 0;}
.autocomplete-wrap {position: relative; margin-bottom: 6px;}
.search-input-wrap {position: relative; width: 100%;}
.search-input-wrap input {padding-right:88px;}
.clear-btn {position:absolute;right:2px;top:50%;transform:translateY(-50%);display:grid;place-items:center;width:44px;height:44px;background:transparent;border:0;cursor:pointer;color:var(--bm-muted);padding:0;border-radius:var(--bm-radius-control);}
.clear-btn:hover:not(:disabled) {color:var(--bm-forest);background:var(--bm-soft);}
.spin-icon {position:absolute;right:54px;top:50%;transform:translateY(-50%);animation:spin 1s linear infinite;font-size:16px;color:var(--bm-forest);}
@keyframes spin {100% {transform: translateY(-50%) rotate(360deg);}}
.suggestion-list {position:absolute;top:calc(100% + 4px);left:0;right:0;z-index:50;background:var(--bm-surface);border:1px solid var(--bm-control-border);border-radius:var(--bm-radius);box-shadow:var(--bm-shadow-popover);max-height:280px;overflow-y:auto;overscroll-behavior:contain;list-style:none;margin:0;padding:var(--bm-space-2) 0;}
.suggestion-item {display:flex;align-items:center;gap:var(--bm-space-3);padding:var(--bm-space-3) var(--bm-space-4);cursor:pointer;transition:background-color var(--bm-motion-fast);}
.suggestion-item:hover, .suggestion-item.highlighted {background:var(--bm-soft);}
.sug-img {width:44px;height:44px;flex:none;border-radius:var(--bm-radius-control);}
.sug-info {flex: 1; min-width: 0; display: flex; flex-direction: column;}
.sug-name {font-weight:600;font-size:var(--bm-type-label);overflow-wrap:anywhere;}
.sug-store {font-size:var(--bm-type-meta);color:var(--bm-muted);}
.sug-price {font-weight:700;color:var(--bm-price);font-size:var(--bm-type-label);font-variant-numeric:tabular-nums;}
.no-result {padding: 8px 4px; font-style: italic;}
.selected-product-card {display:flex;align-items:center;gap:var(--bm-space-3);padding:var(--bm-space-4);background:var(--bm-soft);border:1px solid var(--bm-border);border-radius:var(--bm-radius);margin:var(--bm-space-2) 0 var(--bm-space-4);}
.selected-product-image {width:52px;height:52px;flex:none;border-radius:var(--bm-radius-control);}
.sel-info {display:flex;flex-direction:column;min-width:0;gap:var(--bm-space-1);overflow-wrap:anywhere;}
.sel-info strong {font-size:var(--bm-type-body);color:var(--bm-dark);}
.sel-info span {font-size:var(--bm-type-label);color:var(--bm-muted);}
.item-list, .receipt-list {list-style: none; padding: 0; margin: 20px 0;}
.item-list li {display:flex;align-items:center;gap:var(--bm-space-3);padding:var(--bm-space-4) 0;border-bottom:1px solid var(--bm-border);}
.item-list li div {flex: 1; min-width: 0;}
.item-list li p {margin: 6px 0 0; font-size: 14px; color: var(--ag-text-secondary);}
.item-list .checkout-item-image {width:64px;height:64px;flex:none;border-radius:var(--bm-radius-control);}
.address-option {display:flex;align-items:flex-start;gap:var(--bm-space-3);padding:var(--bm-space-4);border:1px solid var(--bm-control-border);border-radius:var(--bm-radius);margin-bottom:var(--bm-space-3);cursor:pointer;}
.address-option.selected {border-color:var(--bm-forest);background:var(--bm-soft);}
.address-option input {margin-top:var(--bm-space-1);accent-color:var(--bm-forest);}
.address-option small {display: block; margin-top: 8px; line-height: 1.5;}
.section-gap {display:block;margin-top:var(--bm-space-6);margin-bottom:var(--bm-space-3);}
.full-width {width: 100%; margin-top: 12px;}
.store-total {border-bottom:1px solid var(--bm-border);padding:var(--bm-space-4) 0;}
.store-total p, .total-row {display: flex; justify-content: space-between; gap: 16px;}
.total-row {padding: 18px 0; font-size: 19px;}
.total-row strong {color:var(--bm-price);font-variant-numeric:tabular-nums;text-align:right;}
.success {max-width: 760px; margin: auto;}
.success-mark {display:grid;place-items:center;width:56px;height:56px;border-radius:50%;background:var(--bm-soft);color:var(--bm-forest);font-size:30px;margin-bottom:var(--bm-space-5);}
.receipt-list li {display:grid;grid-template-columns:minmax(0,1fr) auto;gap:var(--bm-space-3);border-bottom:1px solid var(--bm-border);padding:var(--bm-space-4) 0;}
.receipt-actions {display: flex; flex-wrap: wrap; gap: 12px; margin-top: 20px;}
.order-code {font-size: 12px; overflow-wrap: anywhere; grid-column: 1 / -1;}
button:disabled {opacity: .55; cursor: not-allowed;}
@media(max-width: 800px) {.checkout-grid {grid-template-columns: 1fr;} .checkout-card {padding: 20px;} .item-list li {flex-wrap: wrap;} }
@media(prefers-reduced-motion:reduce){.spin-icon {animation:none;}.suggestion-item {transition:none;}}
</style>

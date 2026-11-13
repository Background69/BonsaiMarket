<script setup>
import {computed, onMounted, onUnmounted, ref, nextTick, watch} from 'vue';
import {useRoute, useRouter, onBeforeRouteUpdate, onBeforeRouteLeave} from 'vue-router';
import MarketplaceLayout from '../../../layouts/MarketplaceLayout.vue';
import MarketIcon from '../../../components/marketplace/MarketIcon.vue';
import CatalogImage from '../../../components/marketplace/CatalogImage.vue';
import AddressManagement from '../../../components/marketplace/AddressManagement.vue';
import {productPage, usableImage} from '../../../services/catalog.js';
import {apiGet} from '../../../services/api.js';
import {loadCheckout, createQuote, placeOrder, loadQuote, loadReceipt, saveShippingSelection} from '../../../services/checkout.js';
import {MAX_CART_QUANTITY} from '../../../services/cart.js';
import {useAuth} from '../../../composables/useAuth.js';
import {checkoutDraft, rememberCheckoutDraft, clearCheckoutDraft} from '../../../services/checkoutDraft.js';
import WalletCheckoutOption from '../../../components/marketplace/WalletCheckoutOption.vue';
import CheckoutEstimate from '../../../components/marketplace/CheckoutEstimate.vue';

const route = useRoute(), router = useRouter();
const {user}=useAuth();
const loading = ref(true), busy = ref(false), error = ref(''), errorBox = ref(null), ready=ref(false);
const data = ref({items: [], addresses: [], shippingFeePerStore: 0});
const selectedStore = ref(null), selectedShipping = ref(null);
const source = ref('CART'), selectedAddress = ref(null), notes = ref('');
const paymentMethod=ref('COD');
const quote = ref(null), receipt = ref(null), addressEditor = ref(false), uncertain = ref(false);
const previousReview=ref(null),clockNow=ref(Date.now()),clockOffset=ref(0),recoveryWarning=ref('');
const requiresRequote=ref(false);
const sourceNotice=ref('');
const allItemsExpanded=ref(false);
let expiryTimer;
// Only request identity is stored; address, notes and product data come from the server quote.
const pendingKey=()=> 'bonsai:checkout-pending:'+user.value?.id;
function pendingIdentity(){try{const value=JSON.parse(sessionStorage.getItem(pendingKey())||'null');return /^[a-f0-9-]{36}$/.test(value?.id)&&/^[a-f0-9-]{36}$/.test(value?.shipping)?value:null;}catch{return null;}}
function rememberPending(id,shipping){try{sessionStorage.setItem(pendingKey(),JSON.stringify({id,shipping,paymentMethod:paymentMethod.value}));}catch{/* The same in-memory identity still guards retries. */}}
function forgetPending(){try{sessionStorage.removeItem(pendingKey());}catch{/* Storage may be disabled. */}}
const expired=computed(()=>quote.value?.status==='EXPIRED'||!Number.isFinite(Date.parse(quote.value?.expiresAt||''))||clockNow.value+clockOffset.value>=Date.parse(quote.value.expiresAt));
const itemChanges=computed(()=>{
    const previous=new Map((previousReview.value?.items||[]).map(item=>[item.productId,item]));
    return (review.value?.items||[]).flatMap(item=>{const old=previous.get(item.productId);return old&&(old.unitPrice!==item.unitPrice||old.quantity!==item.quantity)?[{name:item.name,before:old,after:item}]:[];});
});
const totalsChanged=computed(()=>previousReview.value&&review.value&&(itemChanges.value.length||previousReview.value.subtotal!==review.value.subtotal||previousReview.value.shippingFee!==review.value.shippingFee));
function acceptQuote(result){quote.value=result;if(result.originalRequest?.paymentMethod)paymentMethod.value=result.originalRequest.paymentMethod;requiresRequote.value=false;selectedShipping.value=result.selectedShippingQuoteId||null;const server=Date.parse(result.serverNow);clockOffset.value=Number.isFinite(server)?server-Date.now():0;clockNow.value=Date.now();}
const search = ref(''), products = ref([]), selectedProduct = ref(null), quantity = ref(1), searching = ref(false);
const showSuggestions = ref(false), highlightedIndex = ref(-1);
const searchError=ref(''),searched=ref(false);
let searchSequence=0;
let initializationSequence=0,ownNavigation=0;
async function updateUrl(query){ownNavigation++;try{await router.replace({path:'/checkout',query});}finally{ownNavigation--;}}
onBeforeRouteUpdate(()=>ownNavigation||(!busy.value&&!uncertain.value));
onBeforeRouteLeave(to=>['login','forbidden'].includes(to.name)||(!busy.value&&!uncertain.value));
const review = computed(() => quote.value?.review);
const rate = computed(() => quote.value?.shippingQuotes?.find(q => q.id === selectedShipping.value));
const stores = computed(() => [...new Map(data.value.items.map(i => [i.storeId, {id: i.storeId, name: i.storeName}])).values()]);
const money = value => typeof value==='number'&&Number.isFinite(value) ? new Intl.NumberFormat('vi-VN', {style: 'currency', currency: 'VND'}).format(value) : 'Chưa xác định';
const maximumQuantity=computed(()=>Number.isSafeInteger(selectedProduct.value?.stock)&&selectedProduct.value.stock>=0?Math.min(selectedProduct.value.stock,MAX_CART_QUANTITY):0);
const validQuantity=computed(()=>Number.isSafeInteger(quantity.value)&&quantity.value>=1&&quantity.value<=maximumQuantity.value);
const quoteExpiry=computed(()=>{
    const time=new Date(quote.value?.expiresAt||'');
    return Number.isFinite(time.getTime())?time.toLocaleString('vi-VN',{timeZone:'Asia/Ho_Chi_Minh'}):'';
});
const addressText = address => [address.addressDetail, address.wardName, address.districtName, address.provinceName].filter(Boolean).join(', ');
const selectedItems = computed(() => source.value === 'CART' ? data.value.items.filter(i => i.storeId === selectedStore.value) : selectedProduct.value
    ? [{productId: selectedProduct.value.id, name: selectedProduct.value.name, imageUrl: selectedProduct.value.imageUrl,
        quantity: quantity.value, unitPrice: selectedProduct.value.price, storeName: selectedProduct.value.store?.name}] : []);
const visibleItems=computed(()=>allItemsExpanded.value?selectedItems.value:selectedItems.value.slice(0,5));
const itemCount=computed(()=>selectedItems.value.reduce((sum,item)=>sum+item.quantity,0));
const estimate=computed(()=>selectedItems.value.length?selectedItems.value.reduce((sum,item)=>sum+(item.lineTotal??item.unitPrice*item.quantity),0):null);
const cartLink=computed(()=>({path:'/cart',query:selectedStore.value?{store:selectedStore.value}:{}}));
function saveDraft(){if(ready.value&&!receipt.value)rememberCheckoutDraft(user.value?.id,{notes:notes.value,addressId:selectedAddress.value});}
function sourceQuery(){return source.value==='BUY_NOW'?{source:'BUY_NOW',...(selectedProduct.value?{productId:selectedProduct.value.id,quantity:quantity.value}:{})}:selectedStore.value?{store:selectedStore.value}:{};}
async function syncSourceUrl(push=false){ownNavigation++;try{await router[push?'push':'replace']({path:'/checkout',query:sourceQuery()});}finally{ownNavigation--;}}

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
    saveDraft();
    loading.value = true; error.value = '';ready.value=false;
    searchSequence++;clearTimeout(debounceTimer);searching.value=false;searchError.value='';searched.value=false;showSuggestions.value=false;
    quote.value=null;receipt.value=null;selectedProduct.value=null;selectedShipping.value=null;previousReview.value=null;source.value=query.source==='BUY_NOW'||query.productId?'BUY_NOW':'CART';uncertain.value=false;
    try {
        const pending=pendingIdentity();paymentMethod.value=pending?.paymentMethod||'COD';
        if(pending&&!query.receipt){query.quote=pending.id;await updateUrl({quote:pending.id});}
        if (query.receipt) {const result=await loadReceipt(String(query.receipt));if(ticket===initializationSequence){receipt.value=result;if(pendingIdentity()?.id===result.quoteId)forgetPending();}}
        else if (query.quote) {
            // Khôi phục đơn khi phản hồi thành công có thể đã bị mất.
            try {const result=await loadReceipt(String(query.quote));if(ticket===initializationSequence){receipt.value=result;if(pendingIdentity()?.id===result.quoteId)forgetPending();}}
            catch (e) {if (e.status !== 404) throw e;}
            if (ticket===initializationSequence && !receipt.value) {
                const result=await loadQuote(String(query.quote));
                if(ticket===initializationSequence){
                    acceptQuote(result);notes.value=result.originalRequest?.notes??result.review?.notes??'';
                    if(result.originalRequest){source.value=result.originalRequest.source;selectedAddress.value=result.originalRequest.addressId;selectedStore.value=result.originalRequest.storeId??result.review.stores[0]?.storeId??null;if(result.originalRequest.quantity!=null)quantity.value=result.originalRequest.quantity;}
                    const pending=pendingIdentity();uncertain.value=pending?.id===result.quoteId;
                    if(uncertain.value)selectedShipping.value=pending.shipping;
                    if(result.status==='COMPLETED'){receipt.value=await loadReceipt(result.quoteId);if(ticket!==initializationSequence)return;forgetPending();uncertain.value=false;}
                }
            }
        } else {
            const draft=checkoutDraft(user.value?.id);
            if(draft){notes.value=draft.notes;selectedAddress.value=draft.addressId;}
            selectedStore.value=null;
            await refresh(ticket);
            if(ticket!==initializationSequence)return;
            if (query.productId) {
                source.value = 'BUY_NOW';
                if(!/^[1-9]\d*$/.test(String(query.productId))||!Number.isSafeInteger(Number(query.productId)))throw new Error('Mã sản phẩm mua ngay không hợp lệ.');
                const product=await apiGet('/products/' + encodeURIComponent(String(query.productId)));
                if(ticket!==initializationSequence)return;
                selectedProduct.value = product;
                search.value=product.name;
                const count = Number(query.quantity || 1);
                if(!Number.isSafeInteger(count)||count<1||count>MAX_CART_QUANTITY)throw new Error('Số lượng mua ngay không hợp lệ.');
                quantity.value=count;
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
    if(busy.value||loading.value||uncertain.value||quote.value||source.value===value)return;
    source.value = value; error.value = '';
    previousReview.value=null;
    sourceNotice.value=value==='BUY_NOW'?'Đã chọn Mua ngay. Giỏ hàng của bạn được giữ nguyên.':'Đã chọn sản phẩm từ giỏ hàng. Mỗi lần đặt một gian hàng.';
    await syncSourceUrl(true);
    if (value === 'BUY_NOW' && !products.value.length) await findProducts();
}
async function finishAddresses() {
    busy.value = true; error.value = '';
    try {await refresh(); addressEditor.value = false;} catch (e) {await showError(e);} finally {busy.value = false;}
}
async function reviewOrder() {
    if (busy.value || uncertain.value) return;
    if(source.value==='BUY_NOW'&&(!Number.isSafeInteger(selectedProduct.value?.id)||selectedProduct.value.id<1||!validQuantity.value)){await showError(new Error('Chọn sản phẩm và nhập số lượng hợp lệ trước khi xem lại đơn hàng.'));return;}
    searchSequence++;clearTimeout(debounceTimer);searching.value=false;showSuggestions.value=false;
    busy.value = true; error.value = '';
    try {
        const body = {source: source.value, addressId: selectedAddress.value, notes: notes.value,paymentMethod:paymentMethod.value, storeId: source.value==='BUY_NOW'?selectedProduct.value?.store?.id:selectedStore.value};
        if (source.value === 'BUY_NOW') Object.assign(body, {productId: selectedProduct.value?.id, quantity: quantity.value});
        acceptQuote(await createQuote(body));
        await updateUrl({quote: quote.value.quoteId});
        await focusHeading();
    } catch (e) {await showError(e);} finally {busy.value = false;}
}
async function chooseShipping(){
    if(busy.value||uncertain.value||expired.value)return;
    busy.value=true;error.value='';
    try{acceptQuote(await saveShippingSelection(quote.value.quoteId,selectedShipping.value,quote.value.selectionRevision));}
    catch(e){selectedShipping.value=quote.value.selectedShippingQuoteId||null;await showError(e);}
    finally{busy.value=false;}
}
async function confirm() {
    if (busy.value || !quote.value || !rate.value || ((expired.value||requiresRequote.value)&&!uncertain.value)) return;
    busy.value = true; error.value = '';
    try {
        if(uncertain.value){
            try{receipt.value=await loadReceipt(quote.value.quoteId);}catch(e){if(e.status!==404)throw e;}
            if(receipt.value){uncertain.value=false;forgetPending();await updateUrl({receipt:receipt.value.quoteId});await focusHeading();return;}
        }else{
            const chosen=selectedShipping.value;
            const result=await saveShippingSelection(quote.value.quoteId,chosen,quote.value.selectionRevision);
            acceptQuote(result);
            if(expired.value)throw new Error('Báo phí đã hết hạn. Lấy báo phí mới trước khi xác nhận.');
            uncertain.value=true;rememberPending(quote.value.quoteId,chosen);
        }
        receipt.value = await placeOrder(quote.value.quoteId, selectedShipping.value, pendingIdentity()?.paymentMethod||paymentMethod.value);
        clearCheckoutDraft();
        uncertain.value = false;forgetPending();
        await updateUrl({receipt: receipt.value.quoteId});
        await focusHeading();
    } catch (e) {
        // Chưa rõ kết quả thì mọi lần thử lại phải dùng cùng báo phí đã lưu.
        if(['QUOTE_USED','IDEMPOTENCY_CONFLICT'].includes(e.code)){
            try{receipt.value=await loadReceipt(quote.value.quoteId);uncertain.value=false;forgetPending();await updateUrl({receipt:receipt.value.quoteId});return;}catch{/* Keep the identity if receipt cannot be read. */}
        }
        // Only a definitive business rejection ends the pending identity. Auth/transport errors may follow an earlier successful attempt.
        if(['QUOTE_EXPIRED','QUOTE_CHANGED','CART_CHANGED','ADDRESS_CHANGED','OUT_OF_STOCK','PRODUCT_UNAVAILABLE','CHECKOUT_PACKAGE_REQUIRED','SELLER_UNAVAILABLE','PRICE_INVALID','VALUE_UNSUPPORTED','WALLET_INSUFFICIENT_FUNDS','WALLET_DISABLED','WALLET_INELIGIBLE'].includes(e.code)){
            uncertain.value=false;requiresRequote.value=true;forgetPending();
        }
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
            const latest=await loadQuote(quote.value.quoteId);
            if(latest.status==='COMPLETED'){receipt.value=await loadReceipt(latest.quoteId);return;}
            const original=latest.originalRequest;
            if(!original)throw new Error('Báo phí cũ chưa có thông tin khôi phục. Không thể tự suy đoán sản phẩm hoặc địa chỉ.');
            source.value=original.source;selectedAddress.value=original.addressId;selectedStore.value=original.storeId??latest.review.stores[0]?.storeId??null;notes.value=original.notes??'';
            const priorRate=latest.shippingQuotes.find(option=>option.id===latest.selectedShippingQuoteId);
            previousReview.value={...latest.review,shippingFee:priorRate?.shippingFee??latest.review.shippingFee};
            if(original.source==='BUY_NOW'){
                selectedProduct.value=await apiGet('/products/'+encodeURIComponent(original.productId));quantity.value=original.quantity;search.value=selectedProduct.value.name;
            }
        }
        const address=selectedAddress.value;await refresh();
        if(!data.value.addresses.some(a=>a.id===address)){selectedAddress.value=null;recoveryWarning.value='Địa chỉ của báo phí không còn trong sổ địa chỉ. Chọn lại trước khi lấy báo phí mới.';}
        quote.value = null;selectedShipping.value=null;
        await updateUrl(source.value==='BUY_NOW'?{productId:selectedProduct.value.id,quantity:quantity.value}:selectedStore.value?{store:selectedStore.value}:{});
    } catch (e) {await showError(e);} finally {busy.value = false;}
}
function selectProduct(product) {
    previousReview.value=null;
    searchSequence++;searching.value=false;
    selectedProduct.value = product;
    search.value = product.name;
    showSuggestions.value = false;
    highlightedIndex.value = -1;
    quantity.value=1;searchError.value='';
    syncSourceUrl();
}
function clearSelection() {
    if(busy.value)return;
    searchSequence++;searching.value=false;clearTimeout(debounceTimer);
    selectedProduct.value = null;
    search.value = '';
    products.value = [];
    showSuggestions.value = false;
    searchError.value='';searched.value=false;
    syncSourceUrl();
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
    const hadProduct=!!selectedProduct.value;
    selectedProduct.value = null;
    if(hadProduct&&ready.value&&!loading.value&&!busy.value&&!quote.value&&source.value==='BUY_NOW')syncSourceUrl();
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
    expiryTimer=setInterval(()=>clockNow.value=Date.now(),1000);
});
onUnmounted(() => {
    if(!receipt.value)saveDraft();
    searchSequence++;
    initializationSequence++;
    clearTimeout(debounceTimer);
    clearInterval(expiryTimer);
    document.removeEventListener('click', handleClickOutside);
});
watch([selectedStore,quantity],()=>{if(ready.value&&!loading.value&&!quote.value&&!receipt.value&&!busy.value){previousReview.value=null;syncSourceUrl();}});
watch(receipt,value=>{if(value)clearCheckoutDraft();});
watch(()=>user.value?.id,()=>{clearCheckoutDraft();notes.value='';selectedAddress.value=null;});
</script>

<template>
  <MarketplaceLayout>
    <div class="checkout-botanical">
    <main class="market-container market-page checkout-page" :aria-busy="loading || busy">
      <header class="market-page-heading">
        <h1 id="checkout-title" tabindex="-1">{{ receipt ? 'Đặt hàng thành công' : review ? 'Xác nhận đơn hàng' : 'Thanh toán' }}</h1>
        <p>{{ receipt ? 'Đơn đã được gửi đến cửa hàng để xác nhận.' : 'Hoàn tất thông tin đơn hàng để nhận những cây bonsai yêu thích từ BonsaiMarket.' }}</p>
      </header>
      <ol class="checkout-progress" aria-label="Tiến trình đặt hàng"><li :class="{completed:review||receipt}" :aria-current="!review && !receipt?'step':undefined"><span aria-hidden="true"><MarketIcon v-if="review||receipt" name="check"/><template v-else>1</template></span>Sản phẩm & địa chỉ</li><li :class="{completed:receipt,blocked:review&&(expired||requiresRequote)}" :aria-current="review && !receipt?'step':undefined"><span aria-hidden="true"><MarketIcon v-if="receipt" name="check"/><template v-else>2</template></span>Vận chuyển & thanh toán</li><li :aria-current="receipt?'step':undefined"><span aria-hidden="true">3</span>{{ receipt?'Đơn đã ghi nhận':'Xác nhận đơn' }}</li></ol>
      <div v-if="error" ref="errorBox" class="checkout-error" role="alert" tabindex="-1">
        <p>{{ error }}</p>
        <button v-if="!quote && !receipt" class="button button-outline" :disabled="loading || busy" @click="retryCheckout">{{ ready?'Cập nhật thông tin đặt hàng':'Thử tải lại' }}</button>
      </div>
      <p v-if="recoveryWarning" class="checkout-warning" role="status">{{ recoveryWarning }}</p>
      <p v-if="loading" role="status">Đang tải thông tin đặt hàng…</p>
      <section v-else-if="receipt" class="checkout-card success" aria-label="Kết quả đặt hàng">
        <div class="success-mark" aria-hidden="true">✓</div>
        <h2>Cảm ơn bạn đã đặt hàng</h2>
        <p>Đơn đã được ghi nhận và đang chờ gian hàng xác nhận. Trạng thái thanh toán được theo dõi riêng trong chi tiết đơn. Sản phẩm của gian hàng khác vẫn ở trong giỏ.</p>
        <ul class="receipt-list">
          <li v-for="order in receipt.orders" :key="order.id">
            <strong>{{ order.storeName }}</strong><span class="order-code">{{ order.code }}</span>
            <RouterLink :to="'/orders/'+order.id">Xem đơn và thanh toán</RouterLink>
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
        <div v-if="totalsChanged" class="checkout-warning" role="status"><p>Giá, số lượng hoặc phí vận chuyển đã thay đổi. Xem lại trước khi xác nhận.</p><ul v-if="itemChanges.length"><li v-for="change in itemChanges" :key="change.after.productId">{{ change.name }}: {{ change.before.quantity }} × {{ money(change.before.unitPrice) }} → {{ change.after.quantity }} × {{ money(change.after.unitPrice) }}.</li></ul><p>Tiền sản phẩm: {{ money(previousReview.subtotal) }} → {{ money(review.subtotal) }}.</p><p>Phí vận chuyển: {{ money(previousReview.shippingFee) }} → {{ money(review.shippingFee) }} (dịch vụ báo phí mới).</p></div>
        <div class="checkout-grid">
          <section class="checkout-card">
            <h2>Thông tin nhận hàng</h2>
            <p><strong>{{ review.address.recipientName }}</strong> · {{ review.address.phone }}</p>
            <p>{{ addressText(review.address) }}</p>
            <p v-if="review.notes">Ghi chú: {{ review.notes }}</p>
            <h2 class="section-gap">Sản phẩm đã chọn</h2>
            <p v-if="quote.originalRequest" class="muted">{{ quote.originalRequest.source==='BUY_NOW'?'Mua ngay · Giỏ hàng được giữ nguyên':'Đặt từ giỏ hàng theo gian hàng đã chọn' }}</p>
            <ul class="item-list">
              <li v-for="item in review.items" :key="item.productId">
                <div><strong>{{ item.name }}</strong><p>{{ item.storeName }} · {{ item.quantity }} × {{ money(item.unitPrice) }}</p></div>
                <strong>{{ money(item.lineTotal) }}</strong>
              </li>
            </ul>
          </section>
          <section class="checkout-card totals">
            <h2 class="checkout-section-title"><MarketIcon name="truck"/>Vận chuyển & thanh toán</h2>
            <p v-if="!quote.shippingQuotes.length" class="checkout-warning" role="status">Chưa có dịch vụ giao hàng cho lựa chọn này. Quay lại kiểm tra địa chỉ hoặc thử lấy báo phí mới.</p>
            <fieldset class="checkout-options" :disabled="busy || uncertain || expired || requiresRequote">
              <legend>Chọn dịch vụ GHN thử nghiệm</legend>
              <label v-for="option in quote.shippingQuotes" :key="option.id" class="shipping-option">
                <input v-model="selectedShipping" type="radio" name="checkout-shipping" :value="option.id" @change="chooseShipping"/>
                <span>{{ option.serviceName }} · {{ money(option.shippingFee) }}</span>
              </label>
            </fieldset>
            <p v-if="quoteExpiry" class="quote-validity">Báo phí có hiệu lực đến <time :datetime="quote.expiresAt">{{ quoteExpiry }}</time> (giờ Việt Nam). Hệ thống kiểm tra lại hiệu lực khi xác nhận.</p>
            <p v-if="expired" class="checkout-warning" role="status">Báo phí đã hết hạn. Quay lại kiểm tra thông tin và lấy báo phí mới. Khi chưa rõ kết quả đặt đơn, hãy tra cứu cùng yêu cầu trước.</p>
            <p v-else-if="requiresRequote" class="checkout-warning" role="status">Thông tin sản phẩm, giá, tồn kho hoặc địa chỉ đã thay đổi. Quay lại kiểm tra và lấy báo phí mới trước khi xác nhận.</p>
            <WalletCheckoutOption v-model="paymentMethod" :total="rate?.total" :disabled="busy||uncertain||!!quote"/><h2 class="checkout-section-title"><MarketIcon name="list"/>Tóm tắt đơn hàng</h2>
            <div v-for="store in review.stores" :key="store.storeId" class="store-total">
              <strong>{{ store.storeName }}</strong>
              <p><span>Tiền sản phẩm</span><span>{{ money(store.subtotal) }}</span></p>
              <p><span>Phí GHN đã chọn</span><span>{{ rate ? money(rate.shippingFee) : 'Chưa chọn dịch vụ' }}</span></p>
            </div>
            <p class="total-row"><span>{{ expired||requiresRequote?'Báo giá không còn hiệu lực':'Tổng cần thanh toán' }}</span><strong>{{ expired||requiresRequote?'Cần báo giá mới':rate ? money(rate.total) : 'Chọn dịch vụ để xem tổng' }}</strong></p>
            <p class="muted">{{ paymentMethod==='VNPAY'?'Thanh toán qua VNPAY Sandbox sau khi ghi nhận đơn. Chỉ IPN hoặc đối soát xác minh kết quả.':paymentMethod==='WALLET_SANDBOX'?'Thanh toán bằng tiền mô phỏng trong QA. Không chuyển tiền thật và không tạo vận đơn bên ngoài.':'Thanh toán COD khi nhận hàng. Tổng tiền và tồn kho được kiểm tra trên máy chủ khi đặt. Thu hộ và đối soát là các bước riêng.' }}</p>
            <p v-if="uncertain && !busy" role="status">Chưa nhận được kết quả. Bấm thử lại để kiểm tra và tiếp tục cùng yêu cầu đặt hàng.</p>
            <button class="button button-primary full-width" :disabled="busy || !rate || ((expired || requiresRequote) && !uncertain)" :aria-busy="busy" @click="confirm">{{ busy ? 'Đang xử lý…' : uncertain ? 'Tra cứu & tiếp tục cùng yêu cầu' : paymentMethod==='VNPAY'?'Đặt đơn để thanh toán VNPAY':paymentMethod==='WALLET_SANDBOX'?'Xác nhận đặt hàng bằng ví thử nghiệm':'Xác nhận đặt hàng COD' }}</button>
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
          <section class="checkout-card checkout-products">
            <div class="checkout-card-heading"><span class="checkout-heading-icon"><MarketIcon name="cart"/></span><div><h2>1. Sản phẩm đã chọn</h2><p>Kiểm tra sản phẩm, gian hàng và số lượng trước khi tiếp tục.</p></div></div>
            <div v-if="source === 'CART' && stores.length" class="store-selection">
              <label :for="stores.length>1?'checkout-store':undefined">Gian hàng đặt lần này</label>
              <div v-if="stores.length===1" class="checkout-single-store"><MarketIcon name="store"/>{{ stores[0].name }}</div>
              <select v-else id="checkout-store" v-model="selectedStore" :disabled="busy" required><option :value="null" disabled>Chọn gian hàng</option><option v-for="store in stores" :key="store.id" :value="store.id">{{ store.name }}</option></select>
              <p v-if="stores.length>1" class="muted">Mỗi lần đặt một gian hàng; các sản phẩm khác vẫn ở trong giỏ.</p>
            </div>
            <div class="source-switch" role="group" aria-label="Nguồn sản phẩm">
              <button type="button" :aria-pressed="source === 'CART'" :disabled="busy" @click="switchSource('CART')"><MarketIcon name="cart"/>Từ giỏ hàng</button>
              <button type="button" :aria-pressed="source === 'BUY_NOW'" :disabled="busy" @click="switchSource('BUY_NOW')"><MarketIcon name="bag"/>Mua ngay</button>
            </div>
            <p v-if="sourceNotice" class="source-notice" role="status">{{ sourceNotice }}</p>
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
                    <span v-if="searching" class="spin-icon" aria-hidden="true"><MarketIcon name="clock"/></span>
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
              <label v-if="selectedProduct" for="checkout-quantity">Số lượng mua ngay</label>
              <template v-if="selectedProduct">
              <input id="checkout-quantity" v-model.number="quantity" type="number" inputmode="numeric" min="1" :max="maximumQuantity" step="1" required :disabled="busy" :aria-invalid="!validQuantity" :aria-describedby="!validQuantity?'checkout-quantity-error':undefined"/>
              <p v-if="!validQuantity" id="checkout-quantity-error" class="checkout-search-error">{{ !Number.isSafeInteger(selectedProduct?.stock)||selectedProduct.stock<0?'Chưa có thông tin tồn kho hợp lệ. Chọn hoặc tải lại sản phẩm để kiểm tra.':maximumQuantity===0?'Sản phẩm hiện đã hết hàng. Chọn sản phẩm khác để tiếp tục.':'Nhập số nguyên từ 1 đến '+maximumQuantity+'.' }}</p>
              </template>
            </div>
            <p v-if="!selectedItems.length && source === 'CART'" class="empty-cart">{{ stores.length?'Chọn gian hàng để xem sản phẩm đặt lần này.':'Giỏ hàng chưa có sản phẩm. Bạn có thể chọn “Mua ngay” để đặt một sản phẩm.' }}</p>
            <ul class="item-list" aria-label="Sản phẩm đặt lần này">
              <li v-for="item in visibleItems" :key="item.productId">
                <CatalogImage :src="usableImage(item.imageUrl)" :alt="item.name" class="checkout-item-image" compact fit="cover"/>
                <div><strong>{{ item.name }}</strong><p>{{ item.storeName }}</p><p class="checkout-item-unit">{{ money(item.unitPrice) }} / sản phẩm</p></div>
                <span class="checkout-item-quantity">SL: {{ item.quantity }}</span>
                <strong class="checkout-line-total">{{ money(item.lineTotal??item.unitPrice*item.quantity) }}</strong>
              </li>
            </ul>
            <button v-if="selectedItems.length>5" type="button" class="checkout-text-button" :aria-expanded="allItemsExpanded" @click="allItemsExpanded=!allItemsExpanded">{{ allItemsExpanded?'Thu gọn':'Xem đủ '+selectedItems.length+' loại sản phẩm' }}</button>
            <RouterLink v-if="source==='CART'" class="checkout-edit-cart" :to="cartLink"><MarketIcon name="chevronLeft"/>Chỉnh sửa trong giỏ hàng</RouterLink>
          </section>
          <div class="checkout-right">
          <section class="checkout-card checkout-delivery">
            <div class="checkout-card-heading"><span class="checkout-heading-icon"><MarketIcon name="pin"/></span><div><h2>2. Địa chỉ nhận hàng</h2><p>Chọn địa chỉ giao hàng hoặc thêm địa chỉ mới.</p></div></div>
            <p v-if="!data.addresses.length" class="muted">Thêm địa chỉ nhận hàng trước khi tiếp tục.</p>
            <fieldset class="checkout-options" :disabled="busy"><legend class="sr-only">Chọn địa chỉ nhận hàng</legend><label v-for="address in data.addresses" :key="address.id" class="address-option" :class="{selected: selectedAddress === address.id}">
              <input v-model="selectedAddress" type="radio" name="checkout-address" :value="address.id" required :disabled="busy"/>
              <span class="checkout-address-copy"><strong>{{ address.recipientName }}</strong> · {{ address.phone }}<small><MarketIcon name="pin"/>{{ addressText(address) }}</small></span><span v-if="address.isDefault" class="checkout-default"><MarketIcon name="star"/>Địa chỉ mặc định</span>
            </label></fieldset>
            <button class="button button-outline checkout-address-button" type="button" :disabled="busy" @click="addressEditor = true"><MarketIcon name="plus"/>Thêm hoặc sửa địa chỉ</button>
            <label for="checkout-notes" class="section-gap"><MarketIcon name="leaf"/>Ghi chú cho người bán (tùy chọn)</label>
            <textarea id="checkout-notes" v-model="notes" maxlength="1000" rows="2" placeholder="Ví dụ: Giao giờ hành chính, đóng gói cẩn thận..." :disabled="busy" aria-describedby="checkout-notes-help"/>
            <p id="checkout-notes-help" class="muted checkout-note-help"><MarketIcon name="info"/>{{ notes.length }}/1.000 ký tự. Ghi chú giúp người bán chuẩn bị đơn hàng tốt hơn.</p>
            <WalletCheckoutOption v-model="paymentMethod" :total="estimate" :disabled="busy||loading"/>
            <button class="button button-primary full-width checkout-quote-button" :disabled="busy || !selectedAddress || !selectedItems.length || (source === 'CART' && !selectedStore) || (source==='BUY_NOW' && !validQuantity)" :aria-busy="busy" type="submit"><MarketIcon :name="busy?'clock':'truck'"/><span>{{ busy ? 'Đang kiểm tra…' : 'Xem phí giao hàng & kiểm tra đơn' }}</span><MarketIcon v-if="!busy" name="arrow"/></button>
          </section>
          <CheckoutEstimate :subtotal="estimate" :count="itemCount"/>
          </div>
        </form>
      </template>
    </main>
    </div>
  </MarketplaceLayout>
</template>

<style scoped>
.shipping-option {display:flex;gap:12px;align-items:center;min-height:44px;padding-block:8px;}.checkout-options {border:0;padding:0;margin:0 0 12px;}.checkout-options input {accent-color:var(--bm-forest);width:18px;height:18px;min-height:18px;flex-shrink:0;margin:0;appearance:auto;}.checkout-page {position:relative;z-index:1;max-width:1168px;padding-top:20px!important;padding-bottom:36px!important;}
.checkout-botanical{position:relative;isolation:isolate;background:url('/images/checkout/botanical-waves.svg') center bottom/100% 96px no-repeat,radial-gradient(ellipse at 95% 20%,rgb(227 237 217 / 38%),transparent 50%),#fafbf8;overflow:clip;color:var(--bm-ink)}
.checkout-botanical::before,.checkout-botanical::after{content:'';position:absolute;pointer-events:none;z-index:0;background:url('/images/checkout/botanical-leaves.svg') left top/contain no-repeat;width:180px;height:180px;top:0;left:-30px;opacity:.7}.checkout-botanical::after{left:auto;right:-20px;transform:scaleX(-1);opacity:.22;width:220px;height:220px}
.market-page-heading{margin:0 0 8px;padding-inline:34px}#checkout-title{color:var(--bm-dark);font:700 38px/1.2 var(--bm-font);font-size:38px!important;letter-spacing:-.025em;margin:0 0 4px}.market-page-heading p{font-size:16px;line-height:1.5;margin:0;color:var(--bm-muted)}
.checkout-progress {display:flex;justify-content:center;list-style:none;margin:14px auto 24px;padding:0;max-width:800px;min-height:36px;}
.checkout-progress li {position:relative;display:flex;align-items:center;gap:12px;font-size:14px;color:var(--bm-muted);white-space:nowrap;}
.checkout-progress li+li::before{content:'';height:1px;width:clamp(24px,6vw,100px);background:var(--bm-control-border);opacity:.45;margin-inline:14px}
.checkout-progress li>span {display:grid;place-items:center;width:36px;height:36px;border:1px solid var(--bm-border);border-radius:50%;font-weight:700;background:var(--bm-surface);flex:none;}
.checkout-progress [aria-current=step] {color:var(--bm-forest);font-weight:700;}
.checkout-progress [aria-current=step]>span {background:var(--bm-forest);border-color:var(--bm-forest);color:var(--bm-surface);}
.checkout-progress li.completed>span{color:var(--bm-forest);background:var(--bm-soft);border-color:var(--bm-forest)}.checkout-progress li.blocked>span{background:var(--bm-warning-surface);color:var(--bm-warning);border-color:currentColor}
.checkout-grid {display:grid;grid-template-columns:minmax(0,1.035fr) minmax(0,1fr);gap:20px;align-items:start;}
.checkout-card {min-width:0;padding:22px 24px;border:1px solid var(--bm-border);border-radius:16px;background:var(--bm-surface);box-shadow:0 4px 24px rgb(11 61 43 / 4%);overflow-wrap:anywhere;}
.checkout-card h2 {font-size:20px;font-weight:700;line-height:1.3;margin:0 0 12px;color:#17251b;}
.checkout-products{align-self:stretch;min-height:510px}.checkout-right{display:grid;gap:10px;min-width:0}.checkout-delivery{padding:14px 22px}.checkout-card-heading{display:flex;align-items:center;gap:14px;margin-bottom:22px}.checkout-card-heading h2{margin:0 0 4px}.checkout-card-heading p{margin:0;color:var(--bm-muted);font-size:14px;line-height:1.4}.checkout-heading-icon{display:grid;place-items:center;width:44px;height:44px;flex:none;border-radius:10px;background:#e3efdf;color:var(--bm-forest)}.checkout-heading-icon svg{width:26px;height:26px}.checkout-delivery .checkout-card-heading{margin-bottom:12px}.checkout-delivery h2{font-size:18px}.checkout-section-title{display:flex;align-items:center;gap:10px}.checkout-section-title svg{color:var(--bm-forest)}
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
.source-switch, .search-row {display:flex;gap:0;margin:24px 0 10px;}
.source-switch button {display:flex;align-items:center;justify-content:center;gap:10px;flex:1;min-height:44px;padding:8px;border:1px solid var(--bm-border);border-radius:8px;background:transparent;cursor:pointer;font:inherit;}
.source-switch button[aria-pressed=true] {background:var(--bm-soft);color:var(--bm-forest);border-color:var(--bm-forest);font-weight:600;}
.source-notice{font-size:13px;color:var(--bm-muted);margin:8px 0}.checkout-single-store{display:flex;align-items:center;gap:12px;border:1px solid var(--bm-control-border);padding:10px 12px;border-radius:8px;min-height:40px;font-size:15px}.checkout-single-store svg{color:var(--bm-forest)}.checkout-edit-cart{display:inline-flex;align-items:center;gap:6px;font-size:14px;color:var(--bm-forest);min-height:44px;text-underline-offset:3px}.checkout-text-button{border:0;background:none;color:var(--bm-forest);font:600 14px var(--bm-font);padding:12px;cursor:pointer}
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
.item-list, .receipt-list {list-style:none;padding:0;margin:10px 0;}
.item-list li {display:flex;align-items:center;gap:var(--bm-space-3);padding:var(--bm-space-4) 0;border-bottom:1px solid var(--bm-border);}
.item-list li div {flex: 1; min-width: 0;}
.item-list li p {margin: 6px 0 0; font-size: 14px; color: var(--ag-text-secondary);}
.item-list .checkout-item-image {width:76px;height:76px;flex:none;border-radius:8px;}
.checkout-item-quantity{flex:none;white-space:nowrap;border:1px solid var(--bm-border);padding:4px 10px;border-radius:8px;font-size:14px;font-variant-numeric:tabular-nums}.checkout-line-total{min-width:104px;text-align:right;color:var(--bm-forest);font-size:17px;font-variant-numeric:tabular-nums;white-space:nowrap}.item-list li .checkout-item-unit{font-size:13px;margin-top:4px}.item-list li:first-child{border-top:1px solid var(--bm-border)}
.address-option {position:relative;display:flex;align-items:center;gap:12px;padding:10px 14px;border:1px solid var(--bm-control-border);border-radius:8px;margin-bottom:8px;cursor:pointer;font-size:14px;}
.address-option.selected {border-color:var(--bm-forest);background:var(--bm-soft);}
.address-option input {margin-top:var(--bm-space-1);accent-color:var(--bm-forest);}
.address-option small{display:flex;align-items:start;gap:4px;margin-top:4px;font-size:12px;line-height:1.5}.address-option small svg{width:14px;height:14px;flex:none;margin-top:2px}.checkout-address-copy{flex:1;min-width:0}.checkout-default{display:inline-flex;align-items:center;gap:4px;padding:3px 5px;border:1px solid #a8cba1;background:#eef6e9;color:var(--bm-forest);border-radius:5px;font-size:11px;white-space:nowrap;align-self:start}.checkout-default svg{width:12px;height:12px}
.section-gap {display:flex;align-items:center;gap:10px;margin-top:16px;margin-bottom:8px;font-weight:600;font-size:14px;}.section-gap svg{width:20px;height:20px;color:var(--bm-forest)}.checkout-address-button{display:inline-flex;align-items:center;gap:10px;min-height:36px;padding:6px 12px;font-size:14px}.checkout-note-help{display:flex;align-items:start;gap:8px;font-size:12px;margin:6px 0}.checkout-note-help svg{width:14px;height:14px;flex:none;margin-top:3px}.checkout-quote-button{display:flex;align-items:center;justify-content:center;gap:10px;min-height:42px;font-size:14px;padding:10px 12px;border-radius:8px}.checkout-quote-button svg{width:20px;height:20px}.checkout-delivery textarea{font-size:14px;padding:10px 14px;min-height:60px}
.full-width {width: 100%; margin-top: 12px;}
.store-total {border-bottom:1px solid var(--bm-border);padding:var(--bm-space-4) 0;}
.store-total p, .total-row {display: flex; justify-content: space-between; gap: 16px;}
.total-row {padding:12px;border-radius:8px;background:var(--bm-soft);font-size:18px;}
.total-row strong {color:var(--bm-price);font-variant-numeric:tabular-nums;text-align:right;}
.success {max-width: 760px; margin: auto;}
.success-mark {display:grid;place-items:center;width:56px;height:56px;border-radius:50%;background:var(--bm-soft);color:var(--bm-forest);font-size:30px;margin-bottom:var(--bm-space-5);}
.receipt-list li {display:grid;grid-template-columns:minmax(0,1fr) auto;gap:var(--bm-space-3);border-bottom:1px solid var(--bm-border);padding:var(--bm-space-4) 0;}
.receipt-actions {display: flex; flex-wrap: wrap; gap: 12px; margin-top: 20px;}
.order-code {font-size: 12px; overflow-wrap: anywhere; grid-column: 1 / -1;}
button:disabled {opacity: .55; cursor: not-allowed;}
@media(max-width:1100px){.checkout-page{width:calc(100% - 48px)}.checkout-card{padding:20px}.checkout-default{position:static;white-space:normal;max-width:80px}.item-list li{gap:10px;flex-wrap:wrap}.item-list .checkout-item-image{width:64px;height:64px}.checkout-line-total{margin-left:auto}.checkout-progress li+li::before{width:30px;margin-inline:10px}.market-page-heading{padding-inline:0}}
@media(max-width:800px){.checkout-grid{grid-template-columns:1fr}.checkout-products{min-height:0}.checkout-page{width:calc(100% - 32px);padding-top:24px}.checkout-progress{justify-content:space-between;margin:24px 0;align-items:start}.checkout-progress li{flex:1;flex-direction:column;text-align:center;white-space:normal;gap:8px;font-size:12px;line-height:1.4;padding-inline:4px}.checkout-progress li+li::before{position:absolute;top:18px;left:-30%;width:60%;margin:0}#checkout-title{font-size:32px!important}.market-page-heading p{font-size:14px}.checkout-botanical::before{opacity:.12;width:140px}.checkout-botanical::after{opacity:.1;width:150px}.checkout-default{max-width:90px}.checkout-card-heading{gap:10px;margin-bottom:18px}.checkout-card h2{font-size:18px}.checkout-item-quantity{font-size:12px}.checkout-line-total{font-size:16px}.checkout-note-help{font-size:12px}}
@media(max-width:390px){.checkout-card{padding:16px}.address-option{padding:10px;flex-wrap:wrap}.checkout-default{max-width:none;margin-left:30px}.checkout-line-total{flex-basis:100%;text-align:right}.checkout-quote-button{font-size:13px;gap:6px}}
@media(prefers-reduced-motion:reduce){.spin-icon {animation:none;}.suggestion-item {transition:none;}}
</style>

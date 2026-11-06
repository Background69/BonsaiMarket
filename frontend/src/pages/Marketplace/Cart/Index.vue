<script setup>
import {computed, nextTick, onMounted, onBeforeUnmount, ref} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import MarketplaceLayout from '../../../layouts/MarketplaceLayout.vue';
import MarketIcon from '../../../components/marketplace/MarketIcon.vue';
import CartLine from '../../../components/cart/CartLine.vue';
import CartSummary from '../../../components/cart/CartSummary.vue';
import {loadCart, updateCartItem, removeCartItem, syncCartCount} from '../../../services/cart.js';
import '../../../css/cart.css';
const route = useRoute(), router = useRouter();
const data = ref(null), loading = ref(true), busy = ref(false), pendingId = ref(null), error = ref(''), message = ref(''), needsRefresh = ref(false);
let active=true,requestSequence=0;
const groups = computed(() => {
  const map = new Map();
  for (const item of data.value?.items || []) {
    if (!map.has(item.storeId)) map.set(item.storeId, {id: item.storeId, name: item.storeName, items: []});
    map.get(item.storeId).items.push(item);
  }
  return [...map.values()];
});
const selected = computed(() => groups.value.find(group => group.id === Number(route.query.store)) || groups.value[0]);
function selectStore(id) {if (!busy.value) router.replace({query: {...route.query, store: id}});}
function apply(result) {data.value = result; syncCartCount(result); needsRefresh.value = false;}
async function load() {
  if (busy.value || (loading.value && data.value)) return;
  const ticket=++requestSequence;
  loading.value = true; error.value = ''; message.value = '';
  try {const result=await loadCart();if(active&&ticket===requestSequence)apply(result);} catch (cause) {if(active&&ticket===requestSequence){error.value = cause.message; needsRefresh.value = true;}}
  finally {if(active&&ticket===requestSequence)loading.value = false;}
}
async function mutate(line, quantity) {
  if (busy.value || loading.value || needsRefresh.value) return;
  busy.value = true; pendingId.value = line.cartItemId; error.value = ''; message.value = '';
  try {
    apply(await (quantity === null ? removeCartItem(line.cartItemId) : updateCartItem(line.cartItemId, quantity)));
    message.value = quantity === null ? `Đã xóa ${line.name} khỏi giỏ hàng.` : `Đã cập nhật số lượng ${line.name}.`;
    if (quantity === null) {await nextTick(); (document.querySelector('input[id^="cart-qty-"]') || document.getElementById('cart-title'))?.focus({preventScroll: true});}
  } catch (cause) {
    error.value = cause.message;
    // Reconcile server values after stock/price/conflict or a lost response; never fabricate success.
    try {apply(await loadCart());} catch {needsRefresh.value = true;}
  } finally {busy.value = false; pendingId.value = null;}
}
onMounted(load);
onBeforeUnmount(()=>{active=false;requestSequence++;});
</script>
<template>
  <MarketplaceLayout>
    <main class="cart-page" :aria-busy="loading">
      <header class="cart-hero"><div class="market-container cart-container cart-hero-inner"><span class="cart-hero-icon"><MarketIcon name="cart"/></span><div><h1 id="cart-title" tabindex="-1">Giỏ hàng</h1><p>Đặt riêng theo từng gian hàng để giữ đúng phí giao nhận và người bán.</p></div></div></header>
      <div class="market-container cart-container cart-content">
        <RouterLink class="cart-continue" to="/products"><MarketIcon name="chevronLeft"/>Tiếp tục mua sắm</RouterLink>
        <div v-if="error" class="cart-feedback cart-feedback-error" role="alert"><p>{{ error }}</p><button type="button" :disabled="busy||loading" @click="load">Tải lại giỏ hàng</button></div>
        <p v-if="message" class="cart-feedback" role="status">{{ message }}</p>
        <div v-if="loading" class="cart-resource" role="status"><MarketIcon name="cart"/><p>Đang tải giỏ hàng…</p></div>
        <div v-else-if="data?.items.length" class="cart-grid">
          <div class="cart-stores"><p v-if="groups.length>1" class="cart-group-hint">Giỏ hàng có {{ groups.length }} gian hàng. Chọn gian hàng để xem tóm tắt và đặt riêng.</p>
            <section v-for="group in groups" :key="group.id" class="cart-store" :class="{'is-selected':selected?.id===group.id}" :aria-labelledby="'cart-store-'+group.id">
              <header class="cart-store-heading"><span class="cart-store-icon"><MarketIcon name="store"/></span><div><h2 :id="'cart-store-'+group.id"><RouterLink :to="'/stores/'+group.id">{{ group.name }}</RouterLink></h2><p>{{ group.items.length }} loại sản phẩm · {{ group.items.reduce((sum,line)=>sum+line.quantity,0) }} sản phẩm</p></div>
                <button v-if="groups.length>1" class="cart-store-select" type="button" :aria-pressed="selected?.id===group.id" :disabled="busy||needsRefresh" @click="selectStore(group.id)">{{ selected?.id===group.id?'Đang chọn':'Chọn gian hàng' }}</button>
              </header>
              <ul class="cart-products"><CartLine v-for="line in group.items" :key="line.cartItemId" :line="line" :busy="busy||needsRefresh" :pending="pendingId===line.cartItemId" @quantity="mutate(line,$event)" @remove="mutate(line,null)"/></ul>
            </section>
          </div>
          <CartSummary v-if="selected" :group="selected" :disabled="busy||needsRefresh" :needs-refresh="needsRefresh"/>
        </div>
        <section v-else-if="data&&!error" class="cart-resource cart-empty"><MarketIcon name="cart"/><h2>Giỏ hàng đang trống</h2><p>Khám phá cây cảnh và chọn sản phẩm từ các gian hàng độc lập.</p><RouterLink class="cart-checkout" to="/products">Khám phá sản phẩm<MarketIcon name="arrow"/></RouterLink></section>
      </div>
    </main>
  </MarketplaceLayout>
</template>

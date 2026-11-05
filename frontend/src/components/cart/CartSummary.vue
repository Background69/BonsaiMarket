<script setup>
import {computed} from 'vue';
import MarketIcon from '../marketplace/MarketIcon.vue';
import {formatPrice} from '../../utils.js';
const props = defineProps({group: Object, disabled: Boolean});
const subtotal = computed(() => props.group.items.reduce((sum, line) => sum + Number(line.lineTotal), 0));
const quantity = computed(() => props.group.items.reduce((sum, line) => sum + line.quantity, 0));
const unavailable = computed(() => props.group.items.some(line => !line.available || line.stock < line.quantity));
const money = value => formatPrice(value) + ' ₫';
</script>
<template>
  <aside class="cart-summary" aria-labelledby="cart-summary-title">
    <header class="cart-summary-heading"><span class="cart-summary-icon"><MarketIcon name="leaf"/></span><div><h2 id="cart-summary-title">Tóm tắt đơn hàng</h2><p>Đơn hàng từ {{ group.name }}</p></div></header>
    <div class="cart-summary-body">
      <dl class="cart-summary-lines"><div><dt><MarketIcon name="bag"/>Tạm tính ({{ quantity }} sản phẩm)</dt><dd>{{ money(subtotal) }}</dd></div><div class="cart-shipping-line"><dt><MarketIcon name="truck"/>Phí vận chuyển</dt><dd>Xác định khi đặt hàng</dd></div></dl>
      <div class="cart-summary-subtotal"><strong>Giá trị sản phẩm</strong><strong>{{ money(subtotal) }}</strong></div>
      <p class="cart-summary-note"><MarketIcon name="info"/>Phí vận chuyển được xác định khi đặt hàng. Giá và tồn kho sẽ được kiểm tra lại trước khi xác nhận.</p>
      <p v-if="unavailable" class="cart-line-error" role="status">Gian hàng có sản phẩm chưa đủ điều kiện đặt hàng. Hãy cập nhật giỏ trước khi tiếp tục.</p>
      <button v-if="disabled||unavailable" class="cart-checkout" type="button" disabled><MarketIcon name="bag"/>{{ disabled?'Đang cập nhật giỏ hàng…':'Kiểm tra lại sản phẩm' }}</button>
      <RouterLink v-else class="cart-checkout" :to="{path:'/checkout',query:{store:group.id}}"><MarketIcon name="bag"/><span>Đặt hàng từ {{ group.name }}</span><MarketIcon name="arrow"/></RouterLink>
      <p class="cart-store-contract"><MarketIcon name="store"/>Mỗi lần đặt hàng dành cho một gian hàng. Sản phẩm khác vẫn được giữ trong giỏ.</p>
    </div>
  </aside>
</template>

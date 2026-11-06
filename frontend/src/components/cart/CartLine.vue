<script setup>
import CatalogImage from '../marketplace/CatalogImage.vue';
import MarketIcon from '../marketplace/MarketIcon.vue';
import CartQuantity from './CartQuantity.vue';
import {formatPrice} from '../../utils.js';
const props=defineProps({line: Object, busy: Boolean, pending: Boolean});
const emit=defineEmits(['quantity', 'remove']);
const money = value => formatPrice(value) + ' ₫';
function remove(){if(!props.busy&&window.confirm('Xóa “'+props.line.name+'” khỏi giỏ hàng?'))emit('remove');}
</script>
<template>
  <li class="cart-product" :data-cart-item="line.cartItemId" :aria-busy="pending">
    <RouterLink class="cart-product-image" :to="'/products/'+line.productId" tabindex="-1" aria-hidden="true"><CatalogImage :src="line.imageUrl" alt="" compact fit="cover"/></RouterLink>
    <div class="cart-product-info">
      <RouterLink class="cart-product-name" :to="'/products/'+line.productId">{{ line.name }}</RouterLink>
      <p>{{ money(line.unitPrice) }} × {{ line.quantity }}</p>
      <p v-if="!line.available||line.stock<line.quantity" class="cart-line-error" role="status">Sản phẩm không còn được bán hoặc không đủ tồn kho. Hãy cập nhật hoặc xóa khỏi giỏ.</p>
      <p v-if="pending" class="cart-pending" role="status">Đang cập nhật…</p>
    </div>
    <CartQuantity :id="line.cartItemId" :name="line.name" :quantity="line.quantity" :stock="line.stock" :disabled="busy||!line.available" @change="$emit('quantity',$event)"/>
    <strong class="cart-line-total"><span class="sr-only">Thành tiền: </span>{{ money(line.lineTotal) }}</strong>
    <button class="cart-remove" type="button" :aria-label="'Xóa '+line.name+' khỏi giỏ hàng'" :disabled="busy" @click="remove"><MarketIcon name="trash"/></button>
  </li>
</template>

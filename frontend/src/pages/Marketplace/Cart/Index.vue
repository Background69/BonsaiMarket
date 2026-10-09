<template><MarketplaceLayout><main class="market-container market-page buyer-page"><div class="breadcrumbs"><Link href="/">Trang chủ</Link><span>/</span><span>Giỏ hàng</span></div><header class="market-page-heading"><h1>Giỏ hàng của bạn</h1><p>Xem lại sản phẩm và gian hàng trước khi đặt mua.</p></header><p class="development-note">Quản lý giỏ hàng đang được phát triển. Hiện chưa có API giỏ hàng kết nối với tài khoản.</p><div v-if="cartItems.length" class="buyer-cart-layout"><section class="market-panel"><div v-for="group in storeGroups" :key="group.id" class="cart-store-group"><h2><Link v-if="group.id" :href="'/stores/'+group.id">{{ group.name }}</Link><span v-else>{{ group.name }}</span></h2><article v-for="item in group.items" :key="item.id" class="buyer-cart-item"><CatalogImage :src="item.product?.image" :alt="item.product?.name"/><div><Link :href="'/products/'+item.product?.id">{{ item.product?.name }}</Link><p>{{ formatPrice(item.product?.price) }} ₫ × {{ item.quantity }}</p></div><div class="button-row"><button disabled @click="updateQty(item.id,item.quantity-1)" aria-label="Giảm số lượng">−</button><span>{{ item.quantity }}</span><button disabled @click="updateQty(item.id,item.quantity+1)" aria-label="Tăng số lượng">+</button><button disabled @click="removeItem(item.id)">Xóa</button></div></article></div></section><aside class="market-panel"><h2>Tóm tắt giỏ hàng</h2><p>Tạm tính <strong>{{ formatPrice(subtotal) }} ₫</strong></p><p class="muted">Phí vận chuyển và thanh toán chưa được kết nối.</p><button class="button button-primary" disabled>Tiến hành thanh toán</button><Link href="/products" class="text-link">Tiếp tục khám phá</Link></aside></div><section v-else class="market-empty buyer-empty"><MarketIcon name="cart"/><h2>Giỏ hàng đang trống</h2><p>Khám phá sản phẩm từ các gian hàng và tìm cây bạn yêu thích.</p><div class="button-row"><Link href="/products" class="button button-primary">Khám phá sản phẩm</Link><Link href="/stores" class="button button-outline">Khám phá gian hàng</Link></div></section></main></MarketplaceLayout></template>
<script setup>
import CatalogImage from '../../../components/marketplace/CatalogImage.vue';
import MarketIcon from '../../../components/marketplace/MarketIcon.vue';
import {computed} from 'vue';
import {Link, router} from '@inertiajs/vue3';
import {route} from 'ziggy-js';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';
import {formatPrice} from '@agriverse/utils';
import {useToast} from 'primevue/usetoast';

const toast = useToast();
const props = defineProps({
  cartItems: {type: Array, default: () => []},
});

const storeGroups = computed(() => {const groups = new Map();for (const item of props.cartItems) {const id=item.product?.store?.id||null;if(!groups.has(id))groups.set(id,{id,name:item.product?.store?.name||'Chưa có thông tin gian hàng',items:[]});groups.get(id).items.push(item)}return [...groups.values()]});
const subtotal = computed(() =>
    props.cartItems.reduce((sum, item) => sum + (item.product?.price || 0) * item.quantity, 0)
);

function updateQty(id, qty) {
  if (qty < 1) return;
  router.put(route('agriverse.api.cart.update', id), {quantity: qty}, {
    preserveScroll: true,
    onSuccess: () => toast.add({severity: 'success', summary: 'Đã cập nhật', life: 2000}),
    onError: () => toast.add({severity: 'error', summary: 'Cập nhật thất bại', life: 2000}),
  });
}

function removeItem(id) {
  router.delete(route('agriverse.api.cart.remove', id), {
    preserveState: true,
    preserveScroll: true,
    onSuccess: () => toast.add({severity: 'success', summary: 'Đã xóa', life: 2000}),
    onError: () => toast.add({severity: 'error', summary: 'Xóa thất bại', life: 2000}),
  });
}
</script>

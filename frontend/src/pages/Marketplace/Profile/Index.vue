<template><MarketplaceLayout><main class="market-container market-page buyer-page"><header class="market-page-heading"><p class="eyebrow">KHU VỰC NGƯỜI MUA</p><h1>Tài khoản của bạn</h1><p>Thông tin cá nhân và hành trình mua hàng trên BonsaiMarket.</p></header><p class="development-note">Tài khoản và hồ sơ chưa kết nối backend. Thông tin cá nhân sẽ hiển thị sau khi luồng đăng nhập được hoàn thiện.</p><div class="buyer-account-grid"><section class="market-panel"><MarketIcon name="user"/><h2>{{ user.email?user.name:'Chưa đăng nhập' }}</h2><p v-if="user.email">{{ user.email }}</p><p v-if="user.phone">{{ user.phone }}</p><p v-if="user.created_at" class="muted">Thành viên từ {{ memberSince }}</p><Link href="/login" class="button button-primary">Đăng nhập</Link></section><section class="market-panel"><h2>Quản lý mua sắm</h2><nav class="buyer-account-nav"><Link href="/cart"><MarketIcon name="cart"/><span>Giỏ hàng<small>Xem lại sản phẩm đã chọn · đang phát triển</small></span><MarketIcon name="arrow"/></Link><Link href="/orders"><MarketIcon name="bag"/><span>Đơn mua<small>Lịch sử và trạng thái đơn hàng · đang phát triển</small></span><MarketIcon name="arrow"/></Link><Link href="/wishlist"><MarketIcon name="heart"/><span>Sản phẩm yêu thích<small>Danh sách đã lưu · đang phát triển</small></span><MarketIcon name="arrow"/></Link></nav></section></div><section v-if="sellerStore" class="market-panel"><h2>Gian hàng của bạn</h2><p>{{ sellerStore.name }}</p><Link href="/seller" class="text-link">Mở kênh người bán</Link></section><section v-if="recentOrders.length" class="market-panel"><h2>Đơn mua gần đây</h2><div v-for="order in recentOrders" :key="order.id" class="dashboard-list-row"><strong>#{{ order.id }} · {{ order.product?.name }}</strong><span>{{ formatPrice(order.total_amount) }} ₫ · {{ statusLabel(order.status) }}</span></div></section></main></MarketplaceLayout></template>
<script setup>
import MarketIcon from '../../../components/marketplace/MarketIcon.vue';
import {computed} from 'vue';
import {Link} from '@inertiajs/vue3';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';

const props = defineProps({
  user: {type: Object, required: true},
  sellerStore: {type: Object, default: null},
  totalOrdersReceived: {type: Number, default: 0},
  recentOrders: {type: Array, default: () => []},
});

const isSeller = computed(() => props.user?.role === 'seller');

const memberSince = computed(() => {
  const d = props.user?.created_at;
  if (!d) return '2024';
  return new Date(d).getFullYear();
});

function formatPrice(price) {
  return new Intl.NumberFormat('vi-VN').format(price || 0);
}

function statusLabel(status) {
  const map = {
    pending: 'Chờ xác nhận',
    confirmed: 'Đã xác nhận',
    shipped: 'Đang giao',
    delivered: 'Đã giao',
    completed: 'Hoàn thành',
    cancelled: 'Đã hủy',
  };
  return map[status] || status;
}
</script>

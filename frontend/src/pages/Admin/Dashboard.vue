<template><AdminLayout><header class="workspace-page-heading"><p class="eyebrow">ĐIỀU HÀNH MARKETPLACE</p><h1>Tổng quan sàn BonsaiMarket</h1><p>Giám sát người dùng, gian hàng, sản phẩm và đơn hàng trên toàn sàn.</p></header><p class="development-note">Đây là giao diện phát triển. Các chỉ số và nghiệp vụ quản trị chưa kết nối API; dấu “—” biểu thị dữ liệu chưa khả dụng.</p><div class="dashboard-stats"><article v-for="card in statCards" :key="card.label"><span>{{ card.label }}</span><MarketIcon :name="card.icon"/><strong>{{ dataAvailable?card.value:'—' }}</strong><small>{{ dataAvailable?'Dữ liệu hệ thống':'Chưa kết nối dữ liệu' }}</small></article></div><div class="workspace-quick-links"><Link href="/admin/users"><MarketIcon name="user"/><span><strong>Người dùng</strong><small>Tài khoản và quyền truy cập</small></span><MarketIcon name="arrow"/></Link><Link href="/admin/products"><MarketIcon name="leaf"/><span><strong>Catalog sản phẩm</strong><small>Xem sản phẩm công khai theo gian hàng</small></span><MarketIcon name="arrow"/></Link><Link href="/admin/orders"><MarketIcon name="bag"/><span><strong>Đơn hàng</strong><small>Theo dõi hoạt động giao dịch</small></span><MarketIcon name="arrow"/></Link></div><DashboardCharts :available="dataAvailable" :series="chartLabels.map((label,i)=>({date:label,label,value:chartData[i]}))" :statuses="ordersByStatus"/><section class="market-panel dashboard-orders"><div class="section-heading"><h2>Đơn hàng gần đây</h2><Link href="/admin/orders" class="text-link">Xem đơn hàng</Link></div><div class="workspace-table-scroll"><table><thead><tr><th>Sản phẩm</th><th>Người mua</th><th>Tổng tiền</th><th>Trạng thái</th></tr></thead><tbody><tr v-for="order in recentOrders" :key="order.id"><td>{{ order.product?.name }}</td><td>{{ order.buyer?.name }}</td><td>{{ formatPrice(order.total_amount) }} ₫</td><td>{{ statusLabel(order.status) }}</td></tr><tr v-if="!recentOrders.length"><td colspan="4" class="table-empty">{{ dataAvailable?'Chưa có đơn hàng.':'Dữ liệu đơn hàng chưa được kết nối.' }}</td></tr></tbody></table></div></section></AdminLayout></template>
<script setup>
import MarketIcon from '../../components/marketplace/MarketIcon.vue';
import DashboardCharts from '../../components/marketplace/DashboardCharts.vue';
import {computed} from 'vue';
import {Link} from '@inertiajs/vue3';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';
import {formatPrice, statusLabel, statusClass} from '@agriverse/utils';

const props = defineProps({
  dataAvailable: Boolean, storesCount: Number, productsCount: Number, usersCount: Number, ordersCount: Number,
  revenueThisMonth: Number, recentOrders: Array, topProducts: Array,
  chartLabels: Array, chartData: Array, ordersByStatus: Object,
});

const maxChart = computed(() => Math.max(...props.chartData, 1));

const statCards = computed(() => [
  {label: 'Người dùng', value: props.usersCount, icon: 'users', iconClass: 'text-amber-600'},
  {label: 'Gian hàng', value: props.storesCount, icon: 'store', iconClass: 'text-emerald-600'},
  {label: 'Sản phẩm', value: props.productsCount, icon: 'box', iconClass: 'text-blue-600'},
  {label: 'Đơn hàng', value: props.ordersCount, icon: 'bag', iconClass: 'text-violet-600'},
]);
</script>

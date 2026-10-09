<template><SellerLayout><header class="workspace-page-heading"><p class="eyebrow">SELLER WORKSPACE</p><h1>Tổng quan gian hàng</h1><p>{{ store.name || 'Sản phẩm, đơn hàng và doanh thu của người bán.' }}</p></header><p class="development-note">Các API riêng cho người bán đang được phát triển. Chỉ số “—” chưa có dữ liệu; trang sản phẩm hiện là bản xem catalog công khai.</p><div class="dashboard-stats"><article v-for="card in [{label:'Sản phẩm',value:stats.total_products,icon:'box'},{label:'Đơn hàng',value:stats.total_orders,icon:'bag'},{label:'Chờ xử lý',value:stats.pending_orders,icon:'clock'},{label:'Doanh thu',value:stats.revenue_this_month,icon:'wallet'}]" :key="card.label"><span>{{ card.label }}</span><MarketIcon :name="card.icon"/><strong>{{ dataAvailable?(card.label==='Doanh thu'?formatCurrency(card.value):card.value??'—'):'—' }}</strong><small>{{ dataAvailable?'Dữ liệu gian hàng':'Chưa kết nối dữ liệu' }}</small></article></div><div class="workspace-quick-links"><Link href="/seller/products"><MarketIcon name="leaf"/><span><strong>Sản phẩm</strong><small>Xem catalog · bản phát triển</small></span><MarketIcon name="arrow"/></Link><Link href="/seller/orders"><MarketIcon name="bag"/><span><strong>Xử lý đơn hàng</strong><small>Giao diện theo dõi đơn bán</small></span><MarketIcon name="arrow"/></Link><button disabled><MarketIcon name="store"/><span><strong>Gian hàng của tôi</strong><small>Chờ API quản lý gian hàng</small></span><MarketIcon name="arrow"/></button></div><div id="revenue"><DashboardCharts :available="dataAvailable" :series="revenueSeries" :statuses="ordersByStatus"/></div><div class="seller-dashboard-lists"><section class="market-panel"><div class="section-heading"><h2>Đơn hàng gần đây</h2><Link href="/seller/orders" class="text-link">Xem đơn hàng</Link></div><div v-if="!recentOrders.length" class="chart-empty">{{ dataAvailable?'Chưa có đơn hàng.':'Chưa kết nối dữ liệu đơn hàng của gian hàng.' }}</div><div v-for="order in recentOrders" :key="order.id" class="dashboard-list-row"><div><strong>{{ order.product?.name }}</strong><p>{{ order.buyer?.name }} · {{ formatCurrency(order.total_amount) }}</p></div><span class="status-badge">{{ statusLabel(order.status) }}</span></div></section><section class="market-panel"><div class="section-heading"><h2>Tồn kho cần chú ý</h2></div><div v-if="!lowStockProducts.length" class="chart-empty">{{ dataAvailable?'Không có sản phẩm sắp hết hàng.':'Chưa kết nối dữ liệu tồn kho của gian hàng.' }}</div><div v-for="p in lowStockProducts" :key="p.id" class="dashboard-list-row"><strong>{{ p.name }}</strong><span>Tồn kho: {{ p.stock }}</span></div><Link href="/seller/products" class="text-link">Xem sản phẩm</Link></section></div></SellerLayout></template>
<script setup>
import MarketIcon from '../../../components/marketplace/MarketIcon.vue';
import DashboardCharts from '../../../components/marketplace/DashboardCharts.vue';
import {Link} from '@inertiajs/vue3'
import SellerLayout from './SellerLayout.vue'

const props = defineProps({
  dataAvailable: Boolean,
  revenueSeries: {type:Array,default:()=>[]},
  ordersByStatus: {type:Object,default:()=>({})},
  store: {type: Object, default: () => ({})},
  stats: {type: Object, default: () => ({})},
  recentOrders: {type: Array, default: () => []},
  lowStockProducts: {type: Array, default: () => []},
  topProducts: {type: Array, default: () => []},
})

function formatCurrency(value) {
  return new Intl.NumberFormat('vi-VN', {style: 'currency', currency: 'VND'}).format(value || 0)
}

function orderStatusClass(status) {
  const map = {
    pending: 'bg-yellow-50 text-yellow-700',
    confirmed: 'bg-blue-50 text-blue-700',
    shipping: 'bg-purple-50 text-purple-700',
    delivered: 'bg-green-50 text-green-700',
    completed: 'bg-green-50 text-green-700',
    cancelled: 'bg-red-50 text-red-700',
  }
  return map[status] || 'bg-gray-50 text-gray-700'
}

function statusLabel(status) {
  const map = {
    pending: 'Chờ xác nhận',
    confirmed: 'Đã xác nhận',
    shipping: 'Đang giao',
    delivered: 'Đã giao',
    completed: 'Hoàn thành',
    cancelled: 'Đã hủy',
  }
  return map[status] || status
}
</script>

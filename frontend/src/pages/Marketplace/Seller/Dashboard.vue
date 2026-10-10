<template>
  <SellerLayout>
    <header class="workspace-page-heading"><h1>Tổng quan gian hàng</h1>
      <p>{{ store.name || 'Sản phẩm, đơn hàng và doanh thu của người bán.' }}</p></header>
    <p class="development-note">Các nghiệp vụ riêng của gian hàng đang được phát triển. Trang sản phẩm hiện chỉ xem
      catalog công khai.</p>
    <nav aria-label="Công việc của người bán" class="workspace-quick-links">
      <Link href="/seller/products">
        <MarketIcon name="leaf"/>
        <span><strong>Xem sản phẩm</strong><small>Catalog công khai · chưa lọc theo người bán</small></span>
        <MarketIcon name="arrow"/>
      </Link>
      <Link href="/seller/orders">
        <MarketIcon name="bag"/>
        <span><strong>Theo dõi đơn hàng</strong><small>Dữ liệu đơn bán đang được kết nối</small></span>
        <MarketIcon name="arrow"/>
      </Link>
      <button disabled>
        <MarketIcon name="store"/>
        <span><strong>Quản lý gian hàng</strong><small>Chưa khả dụng</small></span>
        <MarketIcon name="arrow"/>
      </button>
    </nav>
    <template v-if="dataAvailable">
      <div class="dashboard-stats">
        <article
            v-for="card in [{label:'Sản phẩm',value:stats.total_products,icon:'box'},{label:'Đơn hàng',value:stats.total_orders,icon:'bag'},{label:'Chờ xử lý',value:stats.pending_orders,icon:'clock'},{label:'Doanh thu',value:stats.revenue_this_month,icon:'wallet'}]"
            :key="card.label"><span>{{ card.label }}</span>
          <MarketIcon :name="card.icon"/>
          <strong>{{ card.label === 'Doanh thu' ? formatCurrency(card.value) : card.value ?? '—' }}</strong><small>Dữ
            liệu gian hàng</small></article>
      </div>
      <div id="revenue">
        <DashboardCharts :available="dataAvailable" :series="revenueSeries" :statuses="ordersByStatus"/>
      </div>
    </template>
    <section v-else id="revenue" aria-labelledby="seller-data-title" class="workspace-availability">
      <MarketIcon name="chart"/>
      <div><h2 id="seller-data-title">Thống kê chưa khả dụng</h2>
        <p>Doanh thu, số đơn và tồn kho sẽ hiển thị khi có dữ liệu của gian hàng.</p></div>
    </section>
    <div class="seller-dashboard-lists">
      <section class="market-panel">
        <div class="section-heading"><h2>Đơn hàng gần đây</h2>
          <Link class="text-link" href="/seller/orders">Xem đơn hàng</Link>
        </div>
        <p v-if="!recentOrders.length" class="workspace-empty-copy">
          {{ dataAvailable ? 'Chưa có đơn hàng.' : 'Chưa kết nối dữ liệu đơn hàng của gian hàng.' }}</p>
        <div v-for="order in recentOrders" :key="order.id" class="dashboard-list-row">
          <div><strong>{{ order.product?.name }}</strong>
            <p>{{ order.buyer?.name }} · {{ formatCurrency(order.total_amount) }}</p></div>
          <span class="status-badge">{{ statusLabel(order.status) }}</span></div>
      </section>
      <section class="market-panel">
        <div class="section-heading"><h2>Tồn kho cần chú ý</h2>
          <Link class="text-link" href="/seller/products">Xem sản phẩm</Link>
        </div>
        <p v-if="!lowStockProducts.length" class="workspace-empty-copy">
          {{ dataAvailable ? 'Không có sản phẩm sắp hết hàng.' : 'Chưa kết nối dữ liệu tồn kho của gian hàng.' }}</p>
        <div v-for="p in lowStockProducts" :key="p.id" class="dashboard-list-row"><strong>{{ p.name }}</strong><span>Tồn kho: {{
            p.stock
          }}</span></div>
      </section>
    </div>
  </SellerLayout>
</template>
<script setup>
import MarketIcon from '../../../components/marketplace/MarketIcon.vue';
import DashboardCharts from '../../../components/marketplace/DashboardCharts.vue';
import {Link} from '@inertiajs/vue3'
import SellerLayout from './SellerLayout.vue'

const props = defineProps({
  dataAvailable: Boolean,
  revenueSeries: {type: Array, default: () => []},
  ordersByStatus: {type: Object, default: () => ({})},
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

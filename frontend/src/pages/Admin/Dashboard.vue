<template>
  <AdminLayout>
    <header class="workspace-page-heading"><h1>Tổng quan sàn BonsaiMarket</h1>
      <p>Người dùng, sản phẩm và đơn hàng trên toàn sàn.</p></header>
    <p class="development-note">Các nghiệp vụ quản trị chưa kết nối API. Catalog sản phẩm đang ở chế độ chỉ xem dữ liệu
      công khai.</p>
    <nav aria-label="Công việc quản trị" class="workspace-quick-links">
      <Link href="/admin/users">
        <MarketIcon name="user"/>
        <span><strong>Người dùng</strong><small>Tài khoản và quyền truy cập · đang phát triển</small></span>
        <MarketIcon name="arrow"/>
      </Link>
      <Link href="/admin/products">
        <MarketIcon name="leaf"/>
        <span><strong>Catalog sản phẩm</strong><small>Xem sản phẩm và gian hàng công khai</small></span>
        <MarketIcon name="arrow"/>
      </Link>
      <Link href="/admin/orders">
        <MarketIcon name="bag"/>
        <span><strong>Đơn hàng</strong><small>Theo dõi giao dịch · đang phát triển</small></span>
        <MarketIcon name="arrow"/>
      </Link>
    </nav>
    <template v-if="dataAvailable">
      <div class="dashboard-stats">
        <article v-for="card in statCards" :key="card.label"><span>{{ card.label }}</span>
          <MarketIcon :name="card.icon"/>
          <strong>{{ card.value }}</strong><small>Dữ liệu hệ thống</small></article>
      </div>
      <DashboardCharts :available="dataAvailable"
                       :series="chartLabels.map((label,i)=>({date:label,label,value:chartData[i]}))"
                       :statuses="ordersByStatus"/>
    </template>
    <section v-else aria-labelledby="admin-data-title" class="workspace-availability">
      <MarketIcon name="chart"/>
      <div><h2 id="admin-data-title">Thống kê sàn chưa khả dụng</h2>
        <p>Số liệu và biểu đồ sẽ hiển thị khi API quản trị được kết nối.</p></div>
    </section>
    <section class="market-panel dashboard-orders">
      <div class="section-heading"><h2>Đơn hàng gần đây</h2>
        <Link class="text-link" href="/admin/orders">Xem đơn hàng</Link>
      </div>
      <p v-if="!recentOrders.length" class="workspace-empty-copy">
        {{ dataAvailable ? 'Chưa có đơn hàng.' : 'Dữ liệu đơn hàng chưa được kết nối.' }}</p>
      <div v-else class="workspace-table-scroll">
        <table>
          <thead>
          <tr>
            <th scope="col">Sản phẩm</th>
            <th scope="col">Người mua</th>
            <th scope="col">Tổng tiền</th>
            <th scope="col">Trạng thái</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="order in recentOrders" :key="order.id">
            <td>{{ order.product?.name }}</td>
            <td>{{ order.buyer?.name }}</td>
            <td>{{ formatPrice(order.total_amount) }} ₫</td>
            <td>{{ statusLabel(order.status) }}</td>
          </tr>
          </tbody>
        </table>
      </div>
    </section>
  </AdminLayout>
</template>
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

<template>
  <SellerLayout>
    <div class="space-y-8">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-semibold" style="color: var(--ag-on-surface); font-family: var(--ag-font-display);">Tổng quan</h1>
          <p class="text-sm mt-1" style="color: var(--ag-on-surface-variant);">Chào mừng trở lại, {{ $page.props.auth?.user?.name }}</p>
        </div>
      </div>

      <!-- Stats Grid -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
        <div class="rounded-2xl p-5 border" style="background: white; border-color: var(--ag-border);">
          <div class="flex items-center justify-between mb-3">
            <span class="text-xs font-semibold uppercase tracking-wider" style="color: var(--ag-text-muted);">Sản phẩm</span>
            <span class="w-9 h-9 rounded-xl flex items-center justify-center" style="background: color-mix(in srgb, var(--ag-primary-500) 10%, transparent); color: var(--ag-primary-500);">
              <span class="material-symbols-outlined text-lg">inventory_2</span>
            </span>
          </div>
          <p class="text-3xl font-bold" style="color: var(--ag-on-surface);">{{ stats.total_products }}</p>
        </div>
        <div class="rounded-2xl p-5 border" style="background: white; border-color: var(--ag-border);">
          <div class="flex items-center justify-between mb-3">
            <span class="text-xs font-semibold uppercase tracking-wider" style="color: var(--ag-text-muted);">Đơn hàng</span>
            <span class="w-9 h-9 rounded-xl flex items-center justify-center" style="background: color-mix(in srgb, #2563eb 10%, transparent); color: #2563eb;">
              <span class="material-symbols-outlined text-lg">receipt_long</span>
            </span>
          </div>
          <p class="text-3xl font-bold" style="color: var(--ag-on-surface);">{{ stats.total_orders }}</p>
        </div>
        <div class="rounded-2xl p-5 border" style="background: white; border-color: var(--ag-border);">
          <div class="flex items-center justify-between mb-3">
            <span class="text-xs font-semibold uppercase tracking-wider" style="color: var(--ag-text-muted);">Chờ xử lý</span>
            <span class="w-9 h-9 rounded-xl flex items-center justify-center" style="background: color-mix(in srgb, #ca8a04 10%, transparent); color: #ca8a04;">
              <span class="material-symbols-outlined text-lg">pending</span>
            </span>
          </div>
          <p class="text-3xl font-bold" style="color: var(--ag-on-surface);">{{ stats.pending_orders }}</p>
        </div>
        <div class="rounded-2xl p-5 border" style="background: white; border-color: var(--ag-border);">
          <div class="flex items-center justify-between mb-3">
            <span class="text-xs font-semibold uppercase tracking-wider" style="color: var(--ag-text-muted);">Doanh thu</span>
            <span class="w-9 h-9 rounded-xl flex items-center justify-center" style="background: color-mix(in srgb, #16a34a 10%, transparent); color: #16a34a;">
              <span class="material-symbols-outlined text-lg">paid</span>
            </span>
          </div>
          <p class="text-3xl font-bold" style="color: var(--ag-on-surface);">{{ formatCurrency(stats.revenue) }}</p>
        </div>
      </div>

      <!-- Charts Row -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- Recent Orders -->
        <div class="rounded-2xl border p-6" style="background: white; border-color: var(--ag-border);">
          <h3 class="text-base font-semibold mb-4" style="color: var(--ag-on-surface);">Đơn hàng gần đây</h3>
          <div v-if="recentOrders.length === 0" class="text-sm py-8 text-center" style="color: var(--ag-text-muted);">Chưa có đơn hàng nào.</div>
          <div v-else class="space-y-3">
            <div v-for="order in recentOrders" :key="order.id" class="flex items-center justify-between py-2">
              <div class="flex items-center gap-3 min-w-0">
                <div class="w-10 h-10 rounded-lg flex items-center justify-center text-xs font-bold shrink-0" style="background: var(--ag-primary-500); color: white;">
                  {{ order.product?.name?.charAt(0) || '?' }}
                </div>
                <div class="min-w-0">
                  <p class="text-sm font-medium truncate" style="color: var(--ag-on-surface);">{{ order.product?.name || 'Sản phẩm' }}</p>
                  <p class="text-xs" style="color: var(--ag-text-muted);">{{ order.buyer?.name }} — {{ formatCurrency(order.total_amount) }}</p>
                </div>
              </div>
              <span class="text-xs px-2.5 py-1 rounded-full font-semibold shrink-0" :class="orderStatusClass(order.status)">{{ statusLabel(order.status) }}</span>
            </div>
          </div>
          <Link :href="route('agriverse.shop.seller.orders.index')" class="mt-4 inline-block text-sm font-medium" style="color: var(--ag-primary-500);">
            Xem tất cả đơn hàng →
          </Link>
        </div>

        <!-- Low Stock Products -->
        <div class="rounded-2xl border p-6" style="background: white; border-color: var(--ag-border);">
          <h3 class="text-base font-semibold mb-4" style="color: var(--ag-on-surface);">Sản phẩm sắp hết hàng</h3>
          <div v-if="lowStockProducts.length === 0" class="text-sm py-8 text-center" style="color: var(--ag-text-muted);">Tất cả sản phẩm đều có hàng.</div>
          <div v-else class="space-y-3">
            <div v-for="product in lowStockProducts" :key="product.id" class="flex items-center justify-between py-2">
              <div class="min-w-0 flex-1">
                <p class="text-sm font-medium truncate" style="color: var(--ag-on-surface);">{{ product.name }}</p>
                <p class="text-xs" style="color: var(--ag-text-muted);">Tồn kho: {{ product.stock }}</p>
              </div>
              <span class="text-xs font-semibold text-red-600">Cần nhập thêm</span>
            </div>
          </div>
          <Link :href="route('agriverse.shop.seller.products.index')" class="mt-4 inline-block text-sm font-medium" style="color: var(--ag-primary-500);">
            Quản lý sản phẩm →
          </Link>
        </div>
      </div>

      <!-- Revenue This Month -->
      <div class="rounded-2xl border p-6" style="background: white; border-color: var(--ag-border);">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-base font-semibold" style="color: var(--ag-on-surface);">Doanh thu tháng này</h3>
          <span class="text-2xl font-bold" style="color: var(--ag-primary-500);">{{ formatCurrency(stats.revenue_this_month) }}</span>
        </div>
      </div>
    </div>
  </SellerLayout>
</template>

<script setup>
import { Link } from '@inertiajs/vue3'
import SellerLayout from './SellerLayout.vue'

const props = defineProps({
  store: { type: Object, default: () => ({}) },
  stats: { type: Object, default: () => ({}) },
  recentOrders: { type: Array, default: () => [] },
  lowStockProducts: { type: Array, default: () => [] },
  topProducts: { type: Array, default: () => [] },
})

function formatCurrency(value) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value || 0)
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

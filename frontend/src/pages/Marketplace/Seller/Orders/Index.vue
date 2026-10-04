<template>
  <SellerLayout>
    <div class="space-y-6">
      <h1 class="text-2xl font-semibold" style="color: var(--ag-on-surface); font-family: var(--ag-font-display);">Đơn hàng</h1>

      <div class="rounded-2xl border overflow-hidden" style="background: white; border-color: var(--ag-border);">
        <table class="w-full text-sm">
          <thead>
            <tr style="background: var(--ag-bg); color: var(--ag-text-muted);">
              <th class="p-4 font-medium text-xs text-left">Mã ĐH</th>
              <th class="p-4 font-medium text-xs text-left">Sản phẩm</th>
              <th class="p-4 font-medium text-xs text-left">Người mua</th>
              <th class="p-4 font-medium text-xs text-left">Tổng tiền</th>
              <th class="p-4 font-medium text-xs text-left">Trạng thái</th>
              <th class="p-4 font-medium text-xs text-left">Ngày đặt</th>
              <th class="p-4 font-medium text-xs text-left"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in orders.data" :key="order.id" class="border-t" style="border-color: var(--ag-border);">
              <td class="p-4 font-mono text-xs" style="color: var(--ag-text-muted);">#{{ order.id }}</td>
              <td class="p-4">
                <div class="flex items-center gap-3">
                  <img v-if="order.product?.image" :src="order.product.image" class="w-10 h-10 rounded-lg object-cover">
                  <div v-else class="w-10 h-10 rounded-lg" style="background: var(--ag-bg);"></div>
                  <span class="font-medium" style="color: var(--ag-on-surface);">{{ order.product?.name }}</span>
                </div>
              </td>
              <td class="p-4" style="color: var(--ag-text-secondary);">{{ order.buyer?.name }}</td>
              <td class="p-4 font-medium" style="color: var(--ag-on-surface);">{{ formatCurrency(order.total_amount) }}</td>
              <td class="p-4">
                <span class="text-[11px] px-2.5 py-1 rounded-full font-semibold" :class="statusClass(order.status)">{{ statusLabel(order.status) }}</span>
              </td>
              <td class="p-4 text-xs" style="color: var(--ag-text-muted);">{{ formatDate(order.created_at) }}</td>
              <td class="p-4">
                <Link :href="route('agriverse.shop.seller.orders.show', order.id)" class="text-sm font-medium" style="color: var(--ag-primary-500);">Chi tiết</Link>
              </td>
            </tr>
            <tr v-if="orders.data?.length === 0">
              <td colspan="7" class="p-12 text-center text-sm" style="color: var(--ag-text-muted);">Chưa có đơn hàng nào.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </SellerLayout>
</template>

<script setup>
import { Link } from '@inertiajs/vue3'
import SellerLayout from '../SellerLayout.vue'

const props = defineProps({
  orders: { type: Object, default: () => ({ data: [] }) },
})

function formatCurrency(value) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value || 0)
}

function formatDate(date) {
  if (!date) return ''
  return new Date(date).toLocaleDateString('vi-VN')
}

function statusClass(status) {
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

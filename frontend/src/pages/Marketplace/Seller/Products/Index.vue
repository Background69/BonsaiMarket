<template>
  <SellerLayout>
    <div class="space-y-6">
      <div class="flex items-center justify-between">
        <h1 class="text-2xl font-semibold" style="color: var(--ag-on-surface); font-family: var(--ag-font-display);">Sản phẩm</h1>
        <Link :href="route('agriverse.shop.seller.products.create')"
          class="px-5 py-2.5 rounded-xl text-sm font-semibold text-white transition-all active:scale-[0.97]"
          style="background: var(--ag-primary-500);">
          + Thêm sản phẩm
        </Link>
      </div>

      <div class="rounded-2xl border overflow-hidden" style="background: white; border-color: var(--ag-border);">
        <table class="w-full text-sm">
          <thead>
            <tr style="background: var(--ag-bg); color: var(--ag-text-muted);">
              <th class="p-4 font-medium text-xs text-left">Sản phẩm</th>
              <th class="p-4 font-medium text-xs text-left">Giá</th>
              <th class="p-4 font-medium text-xs text-left">Tồn kho</th>
              <th class="p-4 font-medium text-xs text-left">Trạng thái</th>
              <th class="p-4 font-medium text-xs text-left"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="product in products.data" :key="product.id" class="border-t" style="border-color: var(--ag-border);">
              <td class="p-4">
                <div class="flex items-center gap-3">
                  <img v-if="product.image" :src="product.image" class="w-10 h-10 rounded-lg object-cover">
                  <div v-else class="w-10 h-10 rounded-lg" style="background: var(--ag-bg);"></div>
                  <span class="font-medium" style="color: var(--ag-on-surface);">{{ product.name }}</span>
                </div>
              </td>
              <td class="p-4" style="color: var(--ag-text-secondary);">{{ formatCurrency(product.price) }}</td>
              <td class="p-4" style="color: var(--ag-text-secondary);">{{ product.stock }}</td>
              <td class="p-4">
                <span class="text-[11px] px-2.5 py-1 rounded-full font-semibold" :class="statusClass(product.status)">
                  {{ statusLabel(product.status) }}
                </span>
              </td>
              <td class="p-4">
                <Link :href="route('agriverse.shop.seller.products.edit', product.id)" class="text-sm font-medium" style="color: var(--ag-primary-500);">Sửa</Link>
              </td>
            </tr>
            <tr v-if="products.data?.length === 0">
              <td colspan="5" class="p-12 text-center text-sm" style="color: var(--ag-text-muted);">
                Chưa có sản phẩm nào. <Link :href="route('agriverse.shop.seller.products.create')" style="color: var(--ag-primary-500);">Thêm sản phẩm đầu tiên</Link>
              </td>
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
  products: { type: Object, default: () => ({ data: [] }) },
})

function formatCurrency(value) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value || 0)
}

function statusClass(status) {
  const map = {
    published: 'bg-green-50 text-green-700',
    pending_review: 'bg-yellow-50 text-yellow-700',
    rejected: 'bg-red-50 text-red-700',
    draft: 'bg-gray-50 text-gray-700',
  }
  return map[status] || 'bg-gray-50 text-gray-700'
}

function statusLabel(status) {
  const map = {
    published: 'Đã duyệt',
    pending_review: 'Chờ duyệt',
    rejected: 'Từ chối',
    draft: 'Nháp',
  }
  return map[status] || status
}
</script>

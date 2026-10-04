<template>
  <SellerLayout>
    <div class="space-y-6">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-semibold" style="color: var(--ag-on-surface); font-family: var(--ag-font-display);">Đánh giá</h1>
          <p class="text-sm mt-1" style="color: var(--ag-on-surface-variant);">Đánh giá từ khách hàng trên sản phẩm của bạn</p>
        </div>
      </div>

      <!-- Stats -->
      <div class="grid grid-cols-3 gap-4">
        <div class="rounded-2xl border p-5 text-center" style="background: white; border-color: var(--ag-border);">
          <p class="text-3xl font-bold" style="color: var(--ag-on-surface);">{{ stats.total }}</p>
          <p class="text-xs mt-1" style="color: var(--ag-text-muted);">Tổng đánh giá</p>
        </div>
        <div class="rounded-2xl border p-5 text-center" style="background: white; border-color: var(--ag-border);">
          <p class="text-3xl font-bold" style="color: var(--ag-on-surface);">{{ (stats.average_rating || 0).toFixed(1) }}</p>
          <p class="text-xs mt-1" style="color: var(--ag-text-muted);">Điểm trung bình</p>
        </div>
        <div class="rounded-2xl border p-5 text-center" style="background: white; border-color: var(--ag-border);">
          <p class="text-3xl font-bold" style="color: #ca8a04;">{{ stats.pending }}</p>
          <p class="text-xs mt-1" style="color: var(--ag-text-muted);">Chờ duyệt</p>
        </div>
      </div>

      <!-- Reviews List -->
      <div class="rounded-2xl border overflow-hidden" style="background: white; border-color: var(--ag-border);">
        <div v-if="reviews.data?.length === 0" class="p-12 text-center text-sm" style="color: var(--ag-text-muted);">Chưa có đánh giá nào.</div>
        <div v-else v-for="review in reviews.data" :key="review.id" class="p-5 border-b last:border-0" style="border-color: var(--ag-border);">
          <div class="flex items-start justify-between mb-3">
            <div class="flex items-center gap-3">
              <div class="w-9 h-9 rounded-full flex items-center justify-center text-xs font-bold" style="background: var(--ag-primary-500); color: white;">
                {{ review.user?.name?.charAt(0)?.toUpperCase() || '?' }}
              </div>
              <div>
                <p class="text-sm font-medium" style="color: var(--ag-on-surface);">{{ review.user?.name }}</p>
                <p class="text-xs" style="color: var(--ag-text-muted);">{{ review.product?.name }} — {{ formatDate(review.created_at) }}</p>
              </div>
            </div>
            <div class="flex items-center gap-0.5">
              <span v-for="r in 5" :key="r" class="material-symbols-outlined text-sm" :class="r <= review.rating ? 'text-yellow-500' : 'text-gray-200'" style="font-variation-settings: 'FILL' 1;">star</span>
            </div>
          </div>
          <p v-if="review.comment" class="text-sm" style="color: var(--ag-text-secondary);">{{ review.comment }}</p>
        </div>
      </div>
    </div>
  </SellerLayout>
</template>

<script setup>
import SellerLayout from '../SellerLayout.vue'

const props = defineProps({
  reviews: { type: Object, default: () => ({ data: [] }) },
  stats: { type: Object, default: () => ({}) },
})

function formatDate(date) {
  if (!date) return ''
  return new Date(date).toLocaleDateString('vi-VN')
}
</script>

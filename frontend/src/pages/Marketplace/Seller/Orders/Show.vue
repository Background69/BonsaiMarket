<template>
  <SellerLayout>
    <div class="max-w-3xl">
      <Link :href="route('agriverse.shop.seller.orders.index')" class="inline-flex items-center gap-1 text-sm mb-6" style="color: var(--ag-primary-500);">
        <span class="material-symbols-outlined text-base">arrow_back</span> Quay lại
      </Link>

      <div class="flex items-center justify-between mb-8">
        <div>
          <h1 class="text-2xl font-semibold" style="color: var(--ag-on-surface); font-family: var(--ag-font-display);">Đơn hàng #{{ order.id }}</h1>
          <p class="text-sm mt-1" style="color: var(--ag-on-surface-variant);">Đặt ngày {{ formatDate(order.created_at) }}</p>
        </div>
        <span class="text-sm px-3 py-1.5 rounded-full font-semibold" :class="statusClass(order.status)">{{ statusLabel(order.status) }}</span>
      </div>

      <!-- Order Info -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
        <div class="rounded-2xl border p-5" style="background: white; border-color: var(--ag-border);">
          <h3 class="text-xs font-semibold uppercase tracking-wider mb-3" style="color: var(--ag-text-muted);">Thông tin sản phẩm</h3>
          <div class="flex items-center gap-4">
            <img v-if="order.product?.image" :src="order.product.image" class="w-16 h-16 rounded-xl object-cover">
            <div v-else class="w-16 h-16 rounded-xl" style="background: var(--ag-bg);"></div>
            <div>
              <p class="font-semibold" style="color: var(--ag-on-surface);">{{ order.product?.name }}</p>
              <p class="text-sm" style="color: var(--ag-text-secondary);">SL: {{ order.quantity }} x {{ formatCurrency(order.unit_price) }}</p>
            </div>
          </div>
        </div>

        <div class="rounded-2xl border p-5" style="background: white; border-color: var(--ag-border);">
          <h3 class="text-xs font-semibold uppercase tracking-wider mb-3" style="color: var(--ag-text-muted);">Thông tin người mua</h3>
          <p class="font-medium" style="color: var(--ag-on-surface);">{{ order.buyer?.name }}</p>
          <p class="text-sm" style="color: var(--ag-text-secondary);">{{ order.shipping_address || '—' }}</p>
        </div>
      </div>

      <!-- Summary -->
      <div class="rounded-2xl border p-5 mb-8" style="background: white; border-color: var(--ag-border);">
        <h3 class="text-xs font-semibold uppercase tracking-wider mb-3" style="color: var(--ag-text-muted);">Tổng thanh toán</h3>
        <div class="space-y-2 text-sm">
          <div class="flex justify-between"><span style="color: var(--ag-text-secondary);">Tạm tính</span><span>{{ formatCurrency(order.total_amount) }}</span></div>
          <div class="flex justify-between"><span style="color: var(--ag-text-secondary);">Phí vận chuyển</span><span>{{ formatCurrency(order.shipping_fee || 0) }}</span></div>
          <div class="flex justify-between pt-2 border-t font-semibold text-base" style="border-color: var(--ag-border); color: var(--ag-on-surface);">
            <span>Tổng cộng</span><span>{{ formatCurrency((order.total_amount || 0) + (order.shipping_fee || 0)) }}</span>
          </div>
        </div>
      </div>

      <!-- Actions -->
      <div class="flex items-center gap-3">
        <button v-if="order.status === 'pending'" @click="confirmOrder" class="px-5 py-2.5 rounded-xl text-sm font-semibold text-white" style="background: var(--ag-primary-500);">Xác nhận đơn hàng</button>
        <button v-if="order.status === 'confirmed'" @click="shipOrder" class="px-5 py-2.5 rounded-xl text-sm font-semibold text-white" style="background: #2563eb;">Đã giao cho vận chuyển</button>
        <button v-if="order.status === 'shipping'" @click="deliverOrder" class="px-5 py-2.5 rounded-xl text-sm font-semibold text-white" style="background: #16a34a;">Xác nhận đã giao</button>
        <button v-if="['pending', 'confirmed'].includes(order.status)" @click="showCancel = true" class="px-5 py-2.5 rounded-xl text-sm font-semibold border" style="color: var(--ag-danger); border-color: var(--ag-border);">Hủy đơn</button>
      </div>

      <!-- Cancel Modal -->
      <div v-if="showCancel" class="fixed inset-0 z-50 flex items-center justify-center bg-black/30" @click="showCancel = false">
        <div class="bg-white rounded-2xl p-6 w-full max-w-md mx-4" @click.stop>
          <h3 class="text-lg font-semibold mb-4">Hủy đơn hàng</h3>
          <textarea v-model="cancelReason" placeholder="Lý do hủy..." rows="3" class="w-full px-4 py-3 rounded-xl border text-sm outline-none resize-none mb-4" style="border-color: var(--ag-border);"></textarea>
          <div class="flex gap-3 justify-end">
            <button @click="showCancel = false" class="px-4 py-2 rounded-xl text-sm font-medium" style="color: var(--ag-text-secondary);">Đóng</button>
            <button @click="cancelOrder" class="px-4 py-2 rounded-xl text-sm font-semibold text-white" style="background: var(--ag-danger);">Xác nhận hủy</button>
          </div>
        </div>
      </div>
    </div>
  </SellerLayout>
</template>

<script setup>
import { ref } from 'vue'
import { Link, router } from '@inertiajs/vue3'
import SellerLayout from '../SellerLayout.vue'

const props = defineProps({
  order: { type: Object, required: true },
})

const showCancel = ref(false)
const cancelReason = ref('')

function formatCurrency(value) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value || 0)
}

function formatDate(date) {
  if (!date) return ''
  return new Date(date).toLocaleDateString('vi-VN', { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

function confirmOrder() {
  router.post(route('agriverse.shop.seller.orders.confirm', props.order.id), {}, { preserveScroll: true })
}
function shipOrder() {
  router.post(route('agriverse.shop.seller.orders.ship', props.order.id), {}, { preserveScroll: true })
}
function deliverOrder() {
  router.post(route('agriverse.shop.seller.orders.deliver', props.order.id), {}, { preserveScroll: true })
}
function cancelOrder() {
  router.post(route('agriverse.shop.seller.orders.cancel', props.order.id), { reason: cancelReason.value }, {
    preserveScroll: true,
    onSuccess: () => { showCancel.value = false; cancelReason.value = '' },
  })
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

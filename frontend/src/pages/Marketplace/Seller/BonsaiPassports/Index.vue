<template>
  <SellerLayout>
    <div class="space-y-6">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-semibold" style="color: var(--ag-on-surface); font-family: var(--ag-font-display);">Hộ chiếu cây</h1>
          <p class="text-sm mt-1" style="color: var(--ag-on-surface-variant);">Làm hộ chiếu NFT (Soulbound) cho cây chưa có, chờ admin duyệt để cấp trên blockchain.</p>
        </div>
        <Link :href="route('agriverse.shop.seller.bonsai-passports.create')"
          class="px-5 py-2.5 rounded-xl text-sm font-semibold text-white transition-all active:scale-[0.97]"
          style="background: var(--ag-primary-500);">
          + Làm hộ chiếu
        </Link>
      </div>

      <div v-if="flash.success" class="px-4 py-3 rounded-xl text-sm font-semibold" style="background: #ecfdf5; color: #047857; border: 1px solid #a7f3d0;">{{ flash.success }}</div>
      <div v-if="flash.error" class="px-4 py-3 rounded-xl text-sm font-semibold" style="background: #fef2f2; color: #b91c1c; border: 1px solid #fecaca;">{{ flash.error }}</div>

      <div class="rounded-2xl border overflow-hidden" style="background: white; border-color: var(--ag-border);">
        <table class="w-full text-sm">
          <thead>
            <tr style="background: var(--ag-bg); color: var(--ag-text-muted);">
              <th class="p-4 font-medium text-xs text-left">Cây</th>
              <th class="p-4 font-medium text-xs text-left">Giá</th>
              <th class="p-4 font-medium text-xs text-left">Hộ chiếu</th>
              <th class="p-4 font-medium text-xs text-left"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="product in products.data" :key="product.id" class="border-t" style="border-color: var(--ag-border);">
              <td class="p-4">
                <div class="flex items-center gap-3">
                  <img v-if="product.image" :src="product.image" class="w-11 h-11 rounded-lg object-cover">
                  <div v-else class="w-11 h-11 rounded-lg" style="background: var(--ag-bg);"></div>
                  <div>
                    <div class="font-medium" style="color: var(--ag-on-surface);">{{ product.name }}</div>
                    <div class="text-xs mt-0.5" style="color: var(--ag-on-surface-variant);">{{ product.status }}</div>
                  </div>
                </div>
              </td>
              <td class="p-4" style="color: var(--ag-text-secondary);">{{ formatCurrency(product.price) }}</td>
              <td class="p-4">
                <template v-if="product.passport">
                  <span class="text-[11px] px-2.5 py-1 rounded-full font-semibold" :class="passportStatusClass(product.passport)">
                    {{ passportStatusLabel(product.passport) }}
                  </span>
                  <div v-if="product.passport.status === 'rejected' && product.passport.reject_reason" class="text-[11px] mt-1" style="color: var(--ag-text-secondary);">
                    Lý do: {{ product.passport.reject_reason }}
                  </div>
                </template>
                <span v-else class="text-[11px] px-2.5 py-1 rounded-full font-semibold bg-gray-50 text-gray-500">Chưa có hộ chiếu</span>
              </td>
              <td class="p-4 text-right">
                <template v-if="!product.passport">
                  <Link :href="route('agriverse.shop.seller.bonsai-passports.create', { product_id: product.id })" class="text-sm font-medium" style="color: var(--ag-primary-500);">Làm hộ chiếu</Link>
                </template>
                <template v-else-if="product.passport.status === 'rejected'">
                  <Link :href="route('agriverse.shop.seller.bonsai-passports.edit', product.passport.id)" class="text-sm font-medium" style="color: var(--ag-primary-500);">Sửa & gửi lại</Link>
                </template>
                <template v-else>
                  <Link :href="route('agriverse.shop.seller.bonsai-passports.show', product.passport.id)" class="text-sm font-medium" style="color: var(--ag-primary-500);">Xem yêu cầu</Link>
                </template>
              </td>
            </tr>
            <tr v-if="products.data?.length === 0">
              <td colspan="4" class="p-12 text-center text-sm" style="color: var(--ag-text-muted);">
                Bạn chưa có sản phẩm nào.
                <Link :href="route('agriverse.shop.seller.bonsai-passports.create')" style="color: var(--ag-primary-500);">Làm hộ chiếu cho cây đầu tiên</Link>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </SellerLayout>
</template>

<script setup>
import { computed } from 'vue'
import { Link, usePage } from '@inertiajs/vue3'
import SellerLayout from '../SellerLayout.vue'

const props = defineProps({
  products: { type: Object, default: () => ({ data: [] }) },
})

const page = usePage()
const flash = computed(() => page.props.flash || {})

function formatCurrency(value) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value || 0)
}

function passportStatusLabel(p) {
  if (p.is_minted) return `Đã cấp #${p.token_id}`
  return { pending: 'Chờ admin duyệt', rejected: 'Bị từ chối', approved: 'Đã duyệt', draft: 'Nháp' }[p.status] || p.status
}

function passportStatusClass(p) {
  if (p.is_minted) return 'bg-green-50 text-green-700'
  return { pending: 'bg-yellow-50 text-yellow-700', rejected: 'bg-red-50 text-red-700', approved: 'bg-green-50 text-green-700', draft: 'bg-gray-50 text-gray-500' }[p.status] || 'bg-gray-50 text-gray-500'
}
</script>

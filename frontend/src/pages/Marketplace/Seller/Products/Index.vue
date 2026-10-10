<template>
  <SellerLayout>
    <div class="space-y-6">
      <div class="flex items-center justify-between seller-products-heading">
        <h1 class="text-2xl font-semibold" style="color: var(--ag-on-surface); font-family: var(--ag-font-display);">Sản
          phẩm</h1>
        <span class="text-xs" style="color: var(--ag-text-muted);">Chỉ xem · toàn bộ sản phẩm công khai</span>
      </div>

      <div class="rounded-2xl border overflow-hidden" style="background: white; border-color: var(--ag-border);">
        <div aria-label="Bảng sản phẩm, cuộn ngang để xem thêm" class="overflow-x-auto" role="region" tabindex="0">
          <table class="w-full text-sm" style="min-width: 660px;">
            <thead>
            <tr style="background: var(--ag-bg); color: var(--ag-text-secondary);">
              <th class="p-4 font-medium text-xs text-left" scope="col">Sản phẩm</th>
              <th class="p-4 font-medium text-xs text-left" scope="col">Danh mục</th>
              <th class="p-4 font-medium text-xs text-left" scope="col">Cửa hàng</th>
              <th class="p-4 font-medium text-xs text-left" scope="col">Giá</th>
              <th class="p-4 font-medium text-xs text-left" scope="col">Tồn kho</th>
              <th class="p-4 font-medium text-xs text-left" scope="col">Trạng thái</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="product in products.data" :key="product.id" class="border-t"
                style="border-color: var(--ag-border);">
              <td class="p-4">
                <div class="flex items-center gap-3">
                  <CatalogImage :alt="product.name" :src="product.image" class="workspace-product-image"/>
                  <RouterLink :to="'/products/'+product.id" class="workspace-product-link">{{
                      product.name
                    }}
                  </RouterLink>
                </div>
              </td>
              <td class="p-4" style="color: var(--ag-text-secondary);">{{ product.category || '—' }}</td>
              <td class="p-4" style="color: var(--ag-text-secondary);">
                <RouterLink v-if="product.store?.id" :to="'/stores/'+product.store.id">{{
                    product.store.name
                  }}
                </RouterLink>
                <span v-else>—</span></td>
              <td class="p-4" style="color: var(--ag-text-secondary);">{{ formatCurrency(product.price) }}</td>
              <td class="p-4" style="color: var(--ag-text-secondary);">{{ product.stock }}</td>
              <td class="p-4">
                <span :class="statusClass(product.status)" class="text-[11px] px-2.5 py-1 rounded-full font-semibold">
                  {{ statusLabel(product.status) }}
                </span>
              </td>
            </tr>
            <tr v-if="products.data?.length === 0 && !isLoading && !apiError">
              <td class="p-12 text-center text-sm" colspan="6" style="color: var(--ag-text-secondary);">
                Chưa có sản phẩm công khai nào.
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </SellerLayout>
</template>

<script setup>
import CatalogImage from '../../../../components/marketplace/CatalogImage.vue';
import SellerLayout from '../SellerLayout.vue'

const props = defineProps({
  products: {type: Object, default: () => ({data: []})},
  isLoading: Boolean,
  apiError: Boolean,
})

function formatCurrency(value) {
  return new Intl.NumberFormat('vi-VN', {style: 'currency', currency: 'VND'}).format(value || 0)
}

function statusClass(status) {
  const map = {
    active: 'bg-green-50 text-green-700',
    published: 'bg-green-50 text-green-700',
    pending_review: 'bg-yellow-50 text-yellow-700',
    rejected: 'bg-red-50 text-red-700',
    draft: 'bg-gray-50 text-gray-700',
  }
  return map[status] || 'bg-gray-50 text-gray-700'
}

function statusLabel(status) {
  const map = {
    active: 'Đang bán',
    published: 'Đã duyệt',
    pending_review: 'Chờ duyệt',
    rejected: 'Từ chối',
    draft: 'Nháp',
  }
  return map[status] || status
}
</script>

<style scoped>
.seller-products-heading {
  flex-wrap: wrap;
  gap: 8px;
}
</style>

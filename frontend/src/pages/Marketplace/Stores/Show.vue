<template>
  <MarketplaceLayout>
    <section class="max-w-[1320px] mx-auto px-5 py-8">
      <!-- Store Info -->
      <div class="bg-white rounded-2xl border border-[var(--ag-border)] p-6 mb-6">
        <div class="flex items-center gap-5">
          <div v-if="store.logo" class="w-16 h-16 rounded-xl overflow-hidden shrink-0">
            <img :src="store.logo" :alt="store.name" class="w-full h-full object-cover" />
          </div>
          <div v-else class="w-16 h-16 rounded-xl bg-gradient-to-br from-[var(--ag-primary-500)] to-[var(--ag-primary-600)] text-white flex items-center justify-center text-xl font-bold shrink-0 shadow-sm">
            {{ store.name?.charAt(0)?.toUpperCase() }}
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-3 flex-wrap">
              <h1 class="text-xl font-bold text-[var(--ag-text-primary)] tracking-tight">{{ store.name }}</h1>
              <span v-if="sellerVerifiedAt" class="inline-flex items-center gap-1 text-xs font-medium bg-[color-mix(in srgb, var(--ag-success) 10%, transparent)] text-[var(--ag-success)] px-2.5 py-0.5 rounded-full">
                <span class="material-symbols-outlined text-sm">verified</span>
                Đã xác thực
              </span>
              <span v-if="sellerType" class="inline-flex items-center gap-1 text-xs font-medium bg-[color-mix(in srgb, var(--ag-primary-500) 10%, transparent)] text-[var(--ag-primary-500)] px-2.5 py-0.5 rounded-full">
                {{ sellerType === 'professional' ? 'Chuyên nghiệp' : 'Cá nhân' }}
              </span>
            </div>
            <div class="flex items-center gap-3 mt-1">
              <span class="text-sm text-[var(--ag-text-secondary)]">{{ store.products_count || 0 }} sản phẩm</span>
              <span v-if="store.status === 'active'" class="inline-flex items-center gap-1 text-xs font-medium text-[var(--ag-primary-500)] bg-[var(--ag-primary-500)]/10 px-2.5 py-0.5 rounded-full">
                <span class="w-1.5 h-1.5 rounded-full bg-[var(--ag-primary-500)]" />Đang hoạt động
              </span>
            </div>
          </div>
        </div>
        <p v-if="store.description" class="mt-4 text-sm text-[var(--ag-text-secondary)] leading-relaxed">{{ store.description }}</p>
        <div v-if="store.address || store.phone" class="flex flex-wrap gap-4 mt-4 text-sm text-[var(--ag-text-secondary)]">
          <div v-if="store.address" class="flex items-center gap-1.5">
            <span class="material-symbols-outlined text-base text-[var(--ag-primary-500)]">location_on</span>
            {{ store.address }}
          </div>
          <div v-if="store.phone" class="flex items-center gap-1.5">
            <span class="material-symbols-outlined text-base text-[var(--ag-primary-500)]">call</span>
            {{ store.phone }}
          </div>
        </div>
      </div>

      <!-- Products -->
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-base font-bold text-[var(--ag-text-primary)]">Sản phẩm của cửa hàng</h2>
      </div>

      <div v-if="products.data?.length" class="grid grid-cols-6 md:grid-cols-12 gap-4">
        <div v-for="product in products.data" :key="product.id" class="col-span-6 md:col-span-4">
          <ProductCard :product="product" />
        </div>
      </div>
      <div v-else class="text-sm text-[var(--ag-text-secondary)] text-center py-10 bg-white rounded-2xl border border-[var(--ag-border)]">
        Cửa hàng chưa có sản phẩm nào.
      </div>
    </section>
  </MarketplaceLayout>
</template>

<script setup>
import { Link } from '@inertiajs/vue3';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';
import ProductCard from '@agriverse/Components/ProductCard.vue';

const props = defineProps({
  store: Object,
  products: Object,
  sellerVerifiedAt: String,
  sellerType: String,
});
</script>

<template>
  <MarketplaceLayout>
    <main class="storefront-page max-w-[1320px] mx-auto px-5 py-8">
      <!-- Store Info -->
      <section class="storefront-header bg-white rounded-2xl border border-[var(--ag-border)] p-6 mb-6">
        <p class="storefront-eyebrow">Gian hàng trên BonsaiMarket</p>
        <div class="flex items-center gap-5">
          <div v-if="store.logo" class="storefront-avatar rounded-xl overflow-hidden shrink-0">
            <img :alt="store.name" :src="store.logo" class="w-full h-full object-cover" @error="store.logo = null"/>
          </div>
          <div v-else class="storefront-avatar rounded-xl flex items-center justify-center text-xl font-bold shrink-0 shadow-sm">
            {{ store.name?.charAt(0)?.toUpperCase() }}
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-3 flex-wrap">
              <h1 class="text-xl font-bold text-[var(--ag-text-primary)] tracking-tight">{{ store.name }}</h1>
            </div>
            <div class="flex items-center gap-3 mt-1">
              <span class="text-sm text-[var(--ag-text-secondary)]">{{ store.products_count || 0 }} sản phẩm</span>
              <span v-if="store.status === 'active'"
                    class="inline-flex items-center gap-1 text-xs font-medium text-[var(--ag-primary-500)] bg-[var(--ag-primary-500)]/10 px-2.5 py-0.5 rounded-full">
                <span class="w-1.5 h-1.5 rounded-full bg-[var(--ag-primary-500)]"/>Đang hoạt động
              </span>
            </div>
          </div>
        </div>
        <p v-if="store.description" class="mt-4 text-sm text-[var(--ag-text-secondary)] leading-relaxed">
          {{ store.description }}</p>
        <div v-if="store.address || store.phone"
             class="flex flex-wrap gap-4 mt-4 text-sm text-[var(--ag-text-secondary)]">
          <div v-if="store.address" class="flex items-center gap-1.5">
            <span class="material-symbols-outlined text-base text-[var(--ag-primary-500)]">location_on</span>
            {{ store.address }}
          </div>
          <div v-if="store.phone" class="flex items-center gap-1.5">
            <span class="material-symbols-outlined text-base text-[var(--ag-primary-500)]">call</span>
            {{ store.phone }}
          </div>
        </div>
        <a href="#store-products" class="storefront-cta">Xem sản phẩm của gian hàng <span aria-hidden="true">→</span></a>
      </section>

      <!-- Products -->
      <div id="store-products" class="flex items-center justify-between mb-4 storefront-products-heading">
        <h2 class="text-base font-bold text-[var(--ag-text-primary)]">Sản phẩm của gian hàng</h2>
      </div>

      <div v-if="products.data?.length" class="grid grid-cols-6 md:grid-cols-12 gap-4">
        <div v-for="product in products.data" :key="product.id" class="col-span-6 md:col-span-4">
          <ProductCard :product="product"/>
        </div>
      </div>
      <div v-else
           class="text-sm text-[var(--ag-text-secondary)] text-center py-10 bg-white rounded-2xl border border-[var(--ag-border)]">
        Gian hàng chưa có sản phẩm nào.
      </div>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';
import ProductCard from '@agriverse/Components/ProductCard.vue';

const props = defineProps({
  store: Object,
  products: Object,
});
</script>

<style scoped>
.storefront-page { padding-top: 56px; padding-bottom: 80px; }
.storefront-header { padding: clamp(24px, 4vw, 48px); background: var(--ag-bg-card); }
.storefront-eyebrow { margin-bottom: 18px; color: var(--ag-primary-500); font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: .08em; }
.storefront-avatar { display: grid; place-items: center; width: 64px; height: 64px; background: var(--ag-primary-500); color: var(--ag-on-primary); }
.storefront-avatar img { width: 100%; height: 100%; object-fit: cover; }
.storefront-header h1 { font-size: clamp(24px, 3vw, 38px); }
.storefront-cta { display: inline-flex; gap: 12px; align-items: center; margin-top: 24px; padding: 12px 16px; border-radius: 10px; background: var(--ag-primary-500); color: var(--ag-on-primary); font-weight: 700; }
.storefront-cta:hover { background: var(--ag-primary-600); }
.storefront-cta:focus-visible { outline: 2px solid var(--ag-primary-500); outline-offset: 2px; }
.storefront-products-heading { scroll-margin-top: 140px; }
@media (max-width: 600px) { .storefront-page { padding-top: 32px; padding-bottom: 56px; } .storefront-header > div:first-of-type { align-items: flex-start; } }
</style>

<template>
  <MarketplaceLayout>
    <main class="stores-page">
      <header class="stores-header">
        <h1 class="stores-title">Khám phá gian hàng</h1>
        <p class="stores-desc">{{ stores.total }} gian hàng đang đăng bán sản phẩm cây cảnh trên BonsaiMarket.</p>
      </header>

      <div v-if="stores.data?.length" class="stores-grid">
        <Link v-for="store in stores.data" :key="store.id"
              :href="route('agriverse.shop.stores.show', store.id)"
              class="store-card">
          <div class="store-card-avatar">
            <img v-if="store.logo" :src="store.logo" :alt="`Logo ${store.name}`" @error="store.logo = null"/>
            <span v-else class="store-card-letter">{{ store.name.charAt(0).toUpperCase() }}</span>
          </div>
          <div class="store-card-body">
            <h2 class="store-card-name">{{ store.name }}</h2>
            <p v-if="store.description" class="store-card-description">{{ store.description }}</p>
            <p v-if="store.address" class="store-card-address">{{ store.address }}</p>
            <div class="store-card-meta">
              <span class="store-card-count">{{ store.products_count || 0 }} sản phẩm</span>
              <span v-if="store.status === 'active'" class="store-card-badge">Đang hoạt động</span>
            </div>
          </div>
          <span class="store-card-cta">Xem gian hàng <span aria-hidden="true">→</span></span>
        </Link>
      </div>

      <div v-else-if="!isLoading && !apiError" class="stores-empty">
        <div class="stores-empty-icon">
          <span class="material-symbols-outlined text-5xl">storefront</span>
        </div>
        <h2 class="stores-empty-title">Chưa có cửa hàng nào</h2>
        <p class="stores-empty-desc">Hãy quay lại sau.</p>
      </div>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import {Link} from '@inertiajs/vue3';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';

defineProps({
  stores: Object,
  isLoading: Boolean,
  apiError: Boolean,
});
</script>

<style scoped>
.stores-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: 128px 64px 80px;
}

@media (max-width: 768px) {
  .stores-page {
    padding: 100px 24px 60px;
  }
}

.stores-header {
  margin-bottom: 48px;
}

.stores-title {
  font-family: var(--ag-font-display);
  font-size: 48px;
  font-weight: 500;
  line-height: 56px;
  letter-spacing: -0.02em;
  color: var(--ag-text-primary);
  margin-bottom: 8px;
}

@media (max-width: 768px) {
  .stores-title {
    font-size: 36px;
    line-height: 42px;
  }
}

.stores-desc {
  font-family: var(--ag-font-body);
  font-size: 18px;
  line-height: 28px;
  color: var(--ag-text-secondary);
}

.stores-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}

@media (min-width: 640px) {
  .stores-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (min-width: 1024px) {
  .stores-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

.store-card {
  display: flex;
  align-items: flex-start;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
  background: var(--ag-surface-container-lowest);
  border-radius: 16px;
  border: 1px solid rgba(116, 121, 108, 0.06);
  text-decoration: none;
  transition: all 0.3s ease;
  min-width: 0;
}
.store-card:focus-visible { outline: 2px solid var(--ag-primary-500); outline-offset: 2px; }
.store-card-avatar img { width: 100%; height: 100%; object-fit: cover; }
.store-card-description { display: -webkit-box; overflow: hidden; -webkit-box-orient: vertical; -webkit-line-clamp: 2; margin-bottom: 8px; color: var(--ag-text-secondary); font-size: 13px; line-height: 1.5; }
.store-card-address { margin-bottom: 8px; color: var(--ag-text-secondary); font-size: 12px; }
.store-card-cta { display: flex; justify-content: space-between; width: 100%; margin-top: auto; padding-top: 12px; border-top: 1px solid var(--ag-border); color: var(--ag-primary-600); font-size: 13px; font-weight: 700; }

.store-card:hover {
  box-shadow: 0 10px 30px -8px rgba(44, 44, 44, 0.06);
  border-color: color-mix(in srgb, var(--ag-primary-500) 20%, transparent);
}

.store-card:hover .store-card-avatar {
  background: var(--ag-primary-500);
  color: white;
}

.store-card:hover .store-card-arrow {
  color: var(--ag-primary-500);
  transform: translateX(4px);
}

.store-card-avatar {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: linear-gradient(135deg, var(--ag-primary-500), var(--ag-primary-600));
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.3s;
}

.store-card-letter {
  font-family: var(--ag-font-display);
  font-size: 24px;
  font-weight: 500;
}

.store-card-body {
  flex: 1;
  min-width: 0;
}

.store-card-name {
  font-family: var(--ag-font-body);
  font-size: 16px;
  font-weight: 600;
  color: var(--ag-text-primary);
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.2s;
}

.store-card:hover .store-card-name {
  color: var(--ag-primary-500);
}

.store-card-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.store-card-count {
  font-family: var(--ag-font-body);
  font-size: 12px;
  color: var(--ag-text-secondary);
}

.store-card-badge {
  font-family: var(--ag-font-body);
  font-size: 11px;
  font-weight: 600;
  color: var(--ag-primary-500);
  background: color-mix(in srgb, var(--ag-primary-500) 10%, transparent);
  padding: 2px 10px;
  border-radius: 9999px;
}

.store-card-arrow {
  color: var(--ag-neutral-300);
  font-size: 20px;
  transition: all 0.3s;
}

.stores-empty {
  text-align: center;
  padding: 80px 24px;
  background: white;
  border-radius: 16px;
  border: 1px solid rgba(116, 121, 108, 0.06);
}

.stores-empty-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--ag-neutral-100);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  color: var(--ag-neutral-400);
}

.stores-empty-title {
  font-family: var(--ag-font-display);
  font-size: 24px;
  font-weight: 500;
  color: var(--ag-text-primary);
  margin-bottom: 8px;
}

.stores-empty-desc {
  font-family: var(--ag-font-body);
  font-size: 14px;
  color: var(--ag-text-secondary);
}
</style>

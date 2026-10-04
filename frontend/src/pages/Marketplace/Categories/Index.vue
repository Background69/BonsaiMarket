<template>
  <MarketplaceLayout>
    <main class="categories-page">
      <header class="categories-header">
        <h1 class="categories-title">Danh mục sản phẩm</h1>
        <p class="categories-desc">Khám phá cây cảnh bonsai theo từng danh mục, từ bonsai cổ thụ đến cây cảnh mini để bàn.</p>
      </header>

      <div v-if="categories.length" class="categories-grid">
        <div v-for="cat in categories" :key="cat.id" class="category-card">
          <Link :href="route('agriverse.shop.products.index', { category: cat.slug })" class="category-card-link">
            <div class="category-card-icon">
              <span class="category-card-letter">{{ cat.name.charAt(0) }}</span>
            </div>
            <div class="category-card-body">
              <h3 class="category-card-name">{{ cat.name }}</h3>
              <span class="category-card-count">{{ cat.products_count || 0 }} sản phẩm</span>
            </div>
            <span class="material-symbols-outlined category-card-arrow">chevron_right</span>
          </Link>
          <div v-if="cat.children?.length" class="category-children">
            <Link v-for="child in cat.children" :key="child.id"
              :href="route('agriverse.shop.products.index', { category: child.slug })"
              class="category-child-link">
              {{ child.name }}
            </Link>
          </div>
        </div>
      </div>

      <div v-else class="categories-empty">
        <div class="categories-empty-icon">
          <span class="material-symbols-outlined text-5xl">category</span>
        </div>
        <h3 class="categories-empty-title">Chưa có danh mục nào</h3>
        <p class="categories-empty-desc">Các danh mục sẽ xuất hiện tại đây.</p>
      </div>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import { Link } from '@inertiajs/vue3';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';

defineProps({
  categories: Array,
});
</script>

<style scoped>
.categories-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: 128px 64px 80px;
}
@media (max-width: 768px) {
  .categories-page { padding: 100px 24px 60px; }
}

.categories-header {
  margin-bottom: 48px;
}
.categories-title {
  font-family: var(--ag-font-display);
  font-size: 48px;
  font-weight: 500;
  line-height: 56px;
  letter-spacing: -0.02em;
  color: var(--ag-text-primary);
  margin-bottom: 16px;
}
@media (max-width: 768px) {
  .categories-title { font-size: 36px; line-height: 42px; }
}
.categories-desc {
  font-family: var(--ag-font-body);
  font-size: 18px;
  line-height: 28px;
  color: var(--ag-text-secondary);
  max-width: 640px;
}

.categories-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}
@media (min-width: 640px) {
  .categories-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (min-width: 1024px) {
  .categories-grid { grid-template-columns: repeat(3, 1fr); }
}

.category-card {
  background: var(--ag-surface-container-lowest);
  border-radius: 16px;
  border: 1px solid rgba(116, 121, 108, 0.06);
  overflow: hidden;
  transition: all 0.3s ease;
}
.category-card:hover {
  box-shadow: 0 10px 30px -8px rgba(44, 44, 44, 0.06);
  border-color: color-mix(in srgb, var(--ag-primary-500) 20%, transparent);
}
.category-card-link {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  text-decoration: none;
  transition: all 0.3s;
}
.category-card-link:hover .category-card-arrow {
  color: var(--ag-primary-500);
  transform: translateX(4px);
}
.category-card-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: color-mix(in srgb, var(--ag-primary-500) 8%, transparent);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.category-card-letter {
  font-family: var(--ag-font-display);
  font-size: 20px;
  font-weight: 500;
  color: var(--ag-primary-500);
}
.category-card-body {
  flex: 1;
  min-width: 0;
}
.category-card-name {
  font-family: var(--ag-font-body);
  font-size: 16px;
  font-weight: 600;
  color: var(--ag-text-primary);
  margin-bottom: 2px;
}
.category-card-count {
  font-family: var(--ag-font-body);
  font-size: 12px;
  color: var(--ag-text-muted);
}
.category-card-arrow {
  color: var(--ag-neutral-300);
  font-size: 20px;
  transition: all 0.3s;
}

.category-children {
  padding: 0 20px 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.category-child-link {
  font-family: var(--ag-font-body);
  font-size: 12px;
  color: var(--ag-text-secondary);
  text-decoration: none;
  padding: 2px 8px;
  border-radius: 9999px;
  background: var(--ag-surface-container-low);
  transition: all 0.2s;
}
.category-child-link:hover {
  color: var(--ag-primary-500);
  background: color-mix(in srgb, var(--ag-primary-500) 8%, transparent);
}

.categories-empty {
  text-align: center;
  padding: 80px 24px;
  background: white;
  border-radius: 16px;
  border: 1px solid rgba(116, 121, 108, 0.06);
}
.categories-empty-icon {
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
.categories-empty-title {
  font-family: var(--ag-font-display);
  font-size: 24px;
  font-weight: 500;
  color: var(--ag-text-primary);
  margin-bottom: 8px;
}
.categories-empty-desc {
  font-family: var(--ag-font-body);
  font-size: 14px;
  color: var(--ag-text-secondary);
}
</style>

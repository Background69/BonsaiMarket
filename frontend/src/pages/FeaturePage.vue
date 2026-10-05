<template>
  <MarketplaceLayout>
    <main class="feature-page">
      <span class="material-symbols-outlined">potted_plant</span>
      <h1>{{ title }}</h1>
      <p>Chức năng đang được phát triển.</p>
      <RouterLink to="/products">Khám phá sản phẩm</RouterLink>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import MarketplaceLayout from '../layouts/MarketplaceLayout.vue';
defineProps({ title: { type: String, default: 'BonsaiMarket' } });
</script>

<style scoped>
.feature-page { min-height: 55vh; padding: 160px 24px 80px; text-align: center; color: var(--ag-text-primary); }
.feature-page .material-symbols-outlined { font-size: 56px; color: var(--ag-primary-500); }
.feature-page h1 { font: 500 2.5rem var(--ag-font-display); margin: 16px 0; }
.feature-page p { color: var(--ag-text-secondary); margin-bottom: 24px; }
.feature-page a { display: inline-block; padding: 12px 24px; color: white; background: var(--ag-primary-500); border-radius: 24px; }
</style>

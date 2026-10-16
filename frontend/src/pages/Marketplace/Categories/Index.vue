<template>
  <MarketplaceLayout>
    <main class="market-container market-page marketplace-surface">
      <div class="breadcrumbs">
        <Link href="/">Trang chủ</Link>
        <span>/</span><span>Danh mục</span></div>
      <header class="market-page-heading"><h1>Danh mục sản phẩm</h1>
        <p>Duyệt cây cảnh, bonsai và phụ kiện từ các gian hàng trên BonsaiMarket.</p></header>
      <ResourceState v-if="isLoading || apiError || !categories.length" resource="danh mục" :loading="isLoading" :error="apiError" @retry="$emit('retry')"/>
      <div v-else class="refined-category-directory">
        <Link v-for="category in categories" :key="category.id"
              :href="route('agriverse.shop.products.index',{category:category.slug})"
              class="refined-category-link">
          <h2>{{ category.name }}</h2>
          <p v-if="category.description">{{ category.description }}</p><span
            class="text-link">Xem sản phẩm
          <MarketIcon name="arrow"/>
        </span></Link>
      </div>
    </main>
  </MarketplaceLayout>
</template>
<script setup>
import ResourceState from '../../../components/marketplace/ResourceState.vue';
import MarketIcon from '../../../components/marketplace/MarketIcon.vue';
import {Link} from '@inertiajs/vue3';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';

defineProps({
  categories: Array,
  isLoading: Boolean,
  apiError: Boolean,
});
defineEmits(['retry']);
</script>
<style scoped>
.refined-category-directory {display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:0 32px;}
.refined-category-link {display:block;padding:24px 0;border-top:1px solid var(--bm-border);text-decoration:none;color:var(--bm-ink);}
.refined-category-link h2 {font-size:20px;margin:0 0 8px;color:var(--bm-forest);}
.refined-category-link p {margin:0 0 12px;color:var(--bm-muted);}
.refined-category-link .text-link {display:inline-flex;align-items:center;gap:12px;padding:0;min-height:44px;}
.refined-category-link:hover h2 {text-decoration:underline;text-underline-offset:4px;}
@media(max-width:1023px){.refined-category-directory {grid-template-columns:repeat(2,minmax(0,1fr));}}
@media(max-width:767px){.refined-category-directory {grid-template-columns:1fr;}}
</style>

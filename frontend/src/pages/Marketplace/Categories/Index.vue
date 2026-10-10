<template>
  <MarketplaceLayout>
    <main class="market-container market-page">
      <div class="breadcrumbs">
        <Link href="/">Trang chủ</Link>
        <span>/</span><span>Danh mục</span></div>
      <header class="market-page-heading"><h1>Danh mục sản phẩm</h1>
        <p>Duyệt cây cảnh, bonsai và phụ kiện từ các gian hàng trên BonsaiMarket.</p></header>
      <div v-if="categories.length" class="category-directory-grid">
        <Link v-for="category in categories" :key="category.id"
              :href="route('agriverse.shop.products.index',{category:category.slug})"
              class="market-panel category-directory-card">
          <CatalogImage :alt="category.name" :src="categoryVisuals[category.id]" label="Ảnh danh mục"/>
          <h2>{{ category.name }}</h2>
          <p v-if="category.description">{{ category.description }}</p><span>{{ category.products_count }} sản phẩm công khai</span><strong
            class="text-link">Xem sản phẩm
          <MarketIcon name="arrow"/>
        </strong></Link>
      </div>
      <div v-else-if="!isLoading && !apiError" class="market-empty"><h2>Chưa có danh mục công khai</h2>
        <p>Các danh mục sẽ xuất hiện khi được công bố trên sàn.</p></div>
    </main>
  </MarketplaceLayout>
</template>
<script setup>
import CatalogImage from '../../../components/marketplace/CatalogImage.vue';
import MarketIcon from '../../../components/marketplace/MarketIcon.vue';
import {Link} from '@inertiajs/vue3';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';
import HomeImage from '../../../components/home/HomeImage.vue';
import {homeVisuals, categoryVisuals} from '../../../config/homeVisuals.js';

defineProps({
  categories: Array,
  isLoading: Boolean,
  apiError: Boolean,
});
</script>

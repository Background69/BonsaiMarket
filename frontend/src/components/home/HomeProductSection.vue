<template>
  <section :class="{'bm-soft': horizontal}" :data-section="horizontal ? 'featured' : 'products'" class="bm-section">
    <div class="bm-container">
      <HomeSectionHeading :accent="accent" :center="horizontal" :eyebrow="eyebrow" :title="title">
        <RouterLink v-if="!horizontal" class="bm-button" to="/products">Xem tất cả <span aria-hidden="true">↗</span>
        </RouterLink>
      </HomeSectionHeading>
      <div v-if="products.length" :class="{'home-product-grid--horizontal': horizontal}" class="home-product-grid">
        <ProductCard v-for="product in products" :key="product.id" :horizontal="horizontal" :product="product"/>
      </div>
      <div v-else :aria-busy="loading" class="home-catalog-state">
        <template v-if="loading">
          <div v-for="i in (horizontal ? 3 : 4)" :key="i" class="home-product-skeleton"/>
          <p role="status">Đang tải catalog…</p></template>
        <p v-else>{{
            error ? 'Catalog hiện chưa tải được. Vui lòng thử lại sau.' : 'Chưa có sản phẩm trong catalog.'
          }}</p>
      </div>
    </div>
  </section>
</template>
<script setup>
import {RouterLink} from 'vue-router';
import HomeSectionHeading from './HomeSectionHeading.vue';
import ProductCard from '../ProductCard.vue';

defineProps({
  products: {type: Array, default: () => []},
  eyebrow: String,
  title: String,
  accent: String,
  horizontal: Boolean,
  loading: Boolean,
  error: Boolean
});
</script>

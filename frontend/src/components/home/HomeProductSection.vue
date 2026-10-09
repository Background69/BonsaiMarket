<template>
  <section class="bm-section" :class="{'bm-soft': horizontal}" :data-section="horizontal ? 'featured' : 'products'">
    <div class="bm-container">
      <HomeSectionHeading :eyebrow="eyebrow" :title="title" :accent="accent" :center="horizontal">
        <RouterLink v-if="!horizontal" to="/products" class="bm-button">Xem tất cả <span aria-hidden="true">↗</span></RouterLink>
      </HomeSectionHeading>
      <div v-if="products.length" class="home-product-grid" :class="{'home-product-grid--horizontal': horizontal}">
        <ProductCard v-for="product in products" :key="product.id" :product="product" :horizontal="horizontal"/>
      </div>
      <div v-else class="home-catalog-state" :aria-busy="loading">
        <template v-if="loading"><div v-for="i in (horizontal ? 3 : 4)" :key="i" class="home-product-skeleton"/><p role="status">Đang tải catalog…</p></template>
        <p v-else>{{ error ? 'Catalog hiện chưa tải được. Vui lòng thử lại sau.' : 'Chưa có sản phẩm trong catalog.' }}</p>
      </div>
    </div>
  </section>
</template>
<script setup>
import {RouterLink} from 'vue-router';
import HomeSectionHeading from './HomeSectionHeading.vue';
import ProductCard from '../ProductCard.vue';
defineProps({products: {type: Array, default: () => []}, eyebrow: String, title: String, accent: String, horizontal: Boolean, loading: Boolean, error: Boolean});
</script>

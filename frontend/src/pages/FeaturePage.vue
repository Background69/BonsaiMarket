<template>
  <MarketplaceLayout>
    <main class="market-container market-page marketplace-surface">
      <div class="breadcrumbs">
        <RouterLink to="/">Trang chủ</RouterLink>
        <span>/</span><span>{{ title }}</span></div>
      <section class="feature-state">
        <h1>{{ notFound?'404 — ':'' }}{{ title }}</h1>
        <p>{{ message }}</p>
        <div class="button-row">
          <RouterLink class="button button-primary" to="/products">Khám phá sản phẩm</RouterLink>
          <RouterLink class="button button-outline" :to="notFound?'/':returnTo">{{ notFound?'Về trang chủ':returnLabel }}</RouterLink>
        </div>
      </section>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import MarketplaceLayout from '../layouts/MarketplaceLayout.vue';

defineProps({
  title: {type: String, default: 'BonsaiMarket'},
  message: {type: String, default: 'Khám phá sản phẩm và gian hàng trên BonsaiMarket.'},
  notFound:Boolean,
  returnTo:{type:String,default:'/stores'},
  returnLabel:{type:String,default:'Khám phá gian hàng'},
});
</script>

<style scoped>
.feature-state {max-width:65ch;padding-block:24px;}
.feature-state p {font-size:16px;line-height:1.6;color:var(--bm-muted);margin:16px 0 24px;}
</style>

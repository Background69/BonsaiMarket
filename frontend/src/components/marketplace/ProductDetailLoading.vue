<template>
  <MarketplaceLayout>
    <main class="market-container market-page catalog-detail product-detail-loading" aria-busy="true">
      <h1 role="status">Đang tải sản phẩm…</h1>
      <div class="product-detail-hero" aria-hidden="true">
        <div class="product-detail-loading-media"></div>
        <div class="product-detail-loading-info"><span></span><span></span><span></span><span></span></div>
      </div>
    </main>
  </MarketplaceLayout>
</template>
<script setup>
import MarketplaceLayout from '../../layouts/MarketplaceLayout.vue';
</script>

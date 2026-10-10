<template>
  <section aria-label="Thông tin gian hàng" class="seller-identity">
    <div class="seller-identity-head">
      <CatalogImage :alt="store.name" :src="store.logo || store.logoUrl || storeVisuals[store.id]?.avatar" avatar/>
      <div><p class="seller-label">Gian hàng bán sản phẩm</p>
        <h2>{{ store.name || 'Chưa có thông tin gian hàng' }}</h2>
        <p v-if="store.address" class="muted">{{ store.address }}</p></div>
    </div>
    <p v-if="store.description" class="store-description">{{ store.description }}</p>
    <RouterLink v-if="store.id" :to="'/stores/'+store.id" class="button button-outline">Xem gian hàng
      <MarketIcon name="arrow"/>
    </RouterLink>
    <p class="seller-context">Gian hàng đăng bán và cung cấp sản phẩm. BonsaiMarket là nền tảng kết nối người mua với
      người bán.</p></section>
</template>
<script setup>
import CatalogImage from './CatalogImage.vue';
import MarketIcon from './MarketIcon.vue';
import {storeVisuals} from '../../config/homeVisuals.js';

defineProps({store: {type: Object, default: () => ({})}});
</script>

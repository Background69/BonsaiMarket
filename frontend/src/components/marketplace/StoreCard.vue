<template>
  <article class="market-store-card">
    <RouterLink :aria-label="'Khám phá '+store.name" :to="'/stores/'+store.id" class="store-card-cover">
      <CatalogImage :alt="'Ảnh gian hàng '+store.name"
                    :src="store.coverUrl || store.cover || storeVisuals[store.id]?.cover || previewImage" fit="cover" label="Ảnh gian hàng" ratio="3 / 1"/>
    </RouterLink>
    <div class="store-card-body">
      <div class="store-card-heading">
        <CatalogImage :alt="store.name" :src="store.logo || store.logoUrl || storeVisuals[store.id]?.avatar" avatar/>
        <h3>
          <RouterLink :to="'/stores/'+store.id">{{ store.name }}</RouterLink>
        </h3>
      </div>
      <p class="store-description">{{ store.description || 'Khám phá sản phẩm được đăng bán tại gian hàng này.' }}</p>
      <p v-if="store.address" class="store-location">
        <MarketIcon name="pin"/>
        {{ store.address }}
      </p>
      <p class="store-public-info">
        <MarketIcon name="store"/>
        <span v-if="Number.isInteger(store.products_count)">{{ store.products_count }} sản phẩm công khai</span><span
          v-else>Gian hàng độc lập</span></p>
      <RouterLink :to="'/stores/'+store.id" class="store-detail-link">Xem gian hàng</RouterLink>
    </div>
  </article>
</template>
<script setup>
import CatalogImage from './CatalogImage.vue';
import MarketIcon from './MarketIcon.vue';
import {storeVisuals} from '../../config/homeVisuals.js';

defineProps({store: {type: Object, required: true}, previewImage: String});
</script>

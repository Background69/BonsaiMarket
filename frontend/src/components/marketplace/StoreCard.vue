<template>
  <article class="market-store-card">
    <RouterLink v-if="store.coverUrl || store.cover" :aria-label="'Khám phá '+store.name" :to="'/stores/'+store.id" class="store-card-cover">
      <CatalogImage :alt="'Ảnh gian hàng '+store.name"
                    :src="store.coverUrl || store.cover" fit="cover" label="Chưa có ảnh gian hàng" ratio="3 / 1"/>
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
      <RouterLink :to="'/stores/'+store.id" class="store-detail-link">Xem gian hàng <MarketIcon name="arrow"/></RouterLink>
    </div>
  </article>
</template>
<script setup>
import CatalogImage from './CatalogImage.vue';
import MarketIcon from './MarketIcon.vue';
import {storeVisuals} from '../../config/homeVisuals.js';

defineProps({store: {type: Object, required: true}, previewImage: String});
</script>
<style scoped>
.market-store-card {min-width:0;background:var(--bm-surface);border:1px solid var(--bm-border);border-radius:var(--bm-radius-store);overflow:hidden;}
.store-card-cover {display:block;}
.store-card-body {padding:20px;}
.store-card-heading {display:flex;align-items:center;gap:12px;}
.store-card-heading>.catalog-image {width:48px;flex-shrink:0;}
.store-card-heading h3 {min-width:0;margin:0;font-size:18px;line-height:1.4;}
.store-card-heading a {color:var(--bm-forest);text-decoration:none;}
.store-card-heading a:hover {text-decoration:underline;}
.store-description {font-size:16px;line-height:1.6;margin:16px 0 12px;color:var(--bm-muted);}
.store-location {display:flex;align-items:start;gap:8px;font-size:14px;color:var(--bm-muted);}
.store-location svg {width:16px;height:16px;flex-shrink:0;margin-top:3px;}
.store-detail-link {display:flex;align-items:center;justify-content:space-between;gap:12px;min-height:44px;color:var(--bm-forest);font-size:14px;text-decoration:none;}
.store-detail-link:hover {text-decoration:underline;}
.store-detail-link svg {width:16px;height:16px;}
</style>

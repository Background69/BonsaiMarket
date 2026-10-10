<template>
  <article :class="{'market-product-card--compact':compact}" class="market-product-card">
    <RouterLink :aria-label="'Xem '+product.name" :to="'/products/'+product.id" class="market-product-photo">
      <CatalogImage :alt="product.name" :src="product.image || productVisuals[product.id] || product.imageUrl"
                    fit="cover" ratio="6 / 5"/>
      <span v-if="!compact && Number.isFinite(product.stock)" :class="{'product-availability--empty':product.stock===0}"
            class="product-availability">{{
          product.stock === 0 ? 'Hết hàng' : 'Còn hàng'
        }}</span>
    </RouterLink>
    <div class="market-product-body">
      <h3>
        <RouterLink :to="'/products/'+product.id" class="market-product-name">{{ product.name }}</RouterLink>
      </h3>
      <p class="market-product-price">{{ formatPrice(product.price) }}₫</p>
      <RouterLink v-if="product.store?.id" :to="'/stores/'+product.store.id" class="product-seller-link">
        <CatalogImage :alt="product.store.name"
                      :src="product.store.logo || product.store.logoUrl || storeVisuals[product.store.id]?.avatar" avatar/>
        <span>{{ product.store.name }}</span>
      </RouterLink>
      <p v-else class="seller-missing">Chưa có thông tin gian hàng</p>
      <RouterLink v-if="!compact" :to="'/products/'+product.id" class="product-detail-link">Xem chi tiết
        <MarketIcon name="arrow"/>
      </RouterLink>
    </div>
  </article>
</template>
<script setup>
import CatalogImage from './marketplace/CatalogImage.vue';
import MarketIcon from './marketplace/MarketIcon.vue';
import {productVisuals, storeVisuals} from '../config/homeVisuals.js';

defineProps({product: {type: Object, required: true}, horizontal: Boolean, compact: Boolean});
const formatPrice = p => typeof p === 'number' && Number.isFinite(p) ? new Intl.NumberFormat('vi-VN').format(p) : '—';
</script>

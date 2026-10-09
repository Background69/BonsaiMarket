<template>
  <article class="market-product-card" :class="{'market-product-card--compact':compact}">
    <RouterLink :to="'/products/'+product.id" class="market-product-photo" :aria-label="'Xem '+product.name">
      <CatalogImage :src="product.image || productVisuals[product.id] || product.imageUrl" :alt="product.name" ratio="6 / 5" fit="cover" />
      <span v-if="!compact && Number.isFinite(product.stock)" class="product-availability" :class="{'product-availability--empty':product.stock===0}">{{ product.stock===0?'Hết hàng':'Còn hàng' }}</span>
    </RouterLink>
    <div class="market-product-body">
      <h3><RouterLink class="market-product-name" :to="'/products/'+product.id">{{ product.name }}</RouterLink></h3>
      <p class="market-product-price">{{ formatPrice(product.price) }}₫</p>
      <RouterLink v-if="product.store?.id" :to="'/stores/'+product.store.id" class="product-seller-link">
        <CatalogImage :src="product.store.logo || product.store.logoUrl || storeVisuals[product.store.id]?.avatar" :alt="product.store.name" avatar />
        <span>{{ product.store.name }}</span>
      </RouterLink>
      <p v-else class="seller-missing">Chưa có thông tin gian hàng</p>
      <RouterLink v-if="!compact" :to="'/products/'+product.id" class="product-detail-link">Xem chi tiết</RouterLink>
    </div>
  </article>
</template>
<script setup>
import CatalogImage from './marketplace/CatalogImage.vue';
import {productVisuals,storeVisuals} from '../config/homeVisuals.js';
defineProps({product:{type:Object,required:true},horizontal:Boolean,compact:Boolean});
const formatPrice = p => typeof p === 'number' && Number.isFinite(p) ? new Intl.NumberFormat('vi-VN').format(p) : '—';
</script>

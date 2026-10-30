<template>
  <article :class="{'market-product-card--compact':compact,'product-card':streamlined,'market-product-card--botanical':botanical}" class="market-product-card">
    <RouterLink :aria-label="'Xem '+product.name" :to="'/products/'+product.id" class="market-product-photo">
      <CatalogImage :alt="product.name" :src="product.image || product.imageUrl || productVisuals[product.id]"
                    fit="cover" ratio="6 / 5"/>
    </RouterLink>
    <div class="market-product-body">
      <h3>
        <RouterLink :to="'/products/'+product.id" class="market-product-name">{{ product.name }}</RouterLink>
      </h3>
      <p class="market-product-price">{{ formatPrice(product.price) }}</p>
      <RouterLink v-if="product.store?.id" :to="'/stores/'+product.store.id" class="product-seller-link">
        <CatalogImage v-if="product.store.logo || product.store.logoUrl || storeVisuals[product.store.id]?.avatar" :alt="product.store.name"
                      :src="product.store.logo || product.store.logoUrl || storeVisuals[product.store.id]?.avatar" avatar/>
        <span>{{ product.store.name }}</span>
      </RouterLink>
      <p v-else class="seller-missing">Chưa có thông tin gian hàng</p>
      <p class="catalog-availability" :class="{'catalog-sold-out':product.stock===0}">{{ Number.isFinite(product.stock) ? product.stock===0?'Hết hàng':'Còn hàng' : 'Chưa có thông tin tồn kho' }}</p>
    </div>
  </article>
</template>
<script setup>
import CatalogImage from './marketplace/CatalogImage.vue';
import {productVisuals, storeVisuals} from '../config/homeVisuals.js';

// Vẫn nhận các cờ cũ; compact chỉ đổi mật độ, streamlined dùng cùng ý nghĩa.
defineProps({product: {type: Object, required: true}, horizontal: Boolean, compact: Boolean, streamlined:Boolean, botanical:Boolean});
const formatPrice = p => typeof p === 'number' && Number.isFinite(p) ? new Intl.NumberFormat('vi-VN', {style:'currency',currency:'VND'}).format(p) : 'Chưa có giá';
</script>
<style scoped>
.market-product-card {min-width:0;height:100%;display:flex;flex-direction:column;overflow:hidden;border:1px solid var(--bm-border);border-radius:var(--bm-radius-product);background:var(--bm-surface);transition:border-color var(--bm-motion-fast),transform var(--bm-motion-standard) var(--bm-motion-ease);}
.market-product-card:hover,.market-product-card:focus-within {border-color:var(--bm-control-border);}
.market-product-photo {display:block;overflow:hidden;}
.market-product-photo :deep(img) {transition:transform var(--bm-motion-standard) var(--bm-motion-ease);}
.market-product-photo:focus-visible {outline:3px solid var(--bm-forest);outline-offset:-3px;}
.market-product-photo>.catalog-image {border-radius:0;}
.market-product-body {display:flex;flex:1;flex-direction:column;gap:0;padding:var(--bm-space-4);}
.market-product-body h3 {font-size:16px;line-height:1.5;font-weight:600;margin:0;min-block-size:3em;}
.market-product-name {display:block;color:var(--bm-ink);text-decoration:none;overflow-wrap:anywhere;}
.market-product-name:hover {color:var(--bm-forest);text-decoration:underline;text-underline-offset:3px;}
.market-product-price {font-size:20px;font-weight:700;line-height:1.4;color:var(--bm-price);margin:8px 0 0;font-variant-numeric:tabular-nums;}
.product-seller-link {display:flex;align-items:center;gap:8px;min-height:44px;color:var(--bm-muted);font-size:14px;line-height:1.5;text-decoration:none;}
.product-seller-link:hover {text-decoration:underline;text-underline-offset:3px;}
.product-seller-link>.catalog-image {width:24px;flex-shrink:0;}
.product-seller-link>span {min-width:0;overflow-wrap:anywhere;}
.seller-missing {margin:8px 0;font-size:14px;color:var(--bm-muted);}
.catalog-availability {display:flex;align-items:center;gap:6px;font-size:14px;line-height:1.5;margin:4px 0 0;color:var(--bm-forest);}
.catalog-availability::before {content:'';width:6px;height:6px;border-radius:50%;background:currentColor;flex:none;}
.catalog-sold-out {color:var(--bm-error);}
.market-product-card--compact .market-product-body {padding:12px;}
.market-product-card--botanical {height:100%;border:0;border-radius:0;background:transparent;transition:transform var(--bm-motion-standard) var(--bm-motion-ease);}
.market-product-card--botanical .market-product-photo {overflow:hidden;border-radius:12px;}
.market-product-card--botanical .market-product-photo :deep(img) {transition:transform var(--bm-motion-standard) var(--bm-motion-ease);}
.market-product-card--botanical .market-product-body {padding:14px 0 0;}
.market-product-card--botanical .market-product-body h3 {font-size:var(--home-type-product,16px);line-height:1.5;letter-spacing:normal;font-weight:700;}
.market-product-card--botanical .market-product-price {margin-top:8px;font-size:var(--home-type-price,20px);line-height:1.4;overflow-wrap:anywhere;}
.market-product-card--botanical .product-seller-link {margin-top:2px;font-size:var(--home-type-meta,14px);line-height:1.5;}
.market-product-card--botanical .catalog-sold-out {font-size:var(--home-type-meta,14px);}
@media(hover:hover) and (pointer:fine) and (prefers-reduced-motion:no-preference){.market-product-card:hover .market-product-photo :deep(img) {transform:scale(1.025);}}
@media(max-width:767px){.market-product-body {padding:12px;}.market-product-price {font-size:18px;}}
</style>

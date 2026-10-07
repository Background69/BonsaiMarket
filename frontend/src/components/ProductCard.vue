<template>
  <article class="market-product-card">
    <Link :href="route('agriverse.shop.products.show', product.id)" class="market-product-image">
      <img v-if="product.image" :src="product.image" :alt="product.name" loading="lazy" @error="product.image = null"/>
      <span v-else class="market-product-letter" aria-hidden="true">{{ product.name?.charAt(0)?.toUpperCase() }}</span>
      <span v-if="product.compare_price && product.compare_price > product.price" class="market-product-discount">-{{ discountPercent }}%</span>
    </Link>
    <button class="market-product-wishlist" type="button" :aria-label="`Thêm ${product.name} vào yêu thích (đang phát triển)`" title="Yêu thích (đang phát triển)" @click="pending">
      <span class="material-symbols-outlined" aria-hidden="true">favorite</span>
    </button>
    <div class="market-product-body">
      <Link :href="route('agriverse.shop.products.show', product.id)" class="market-product-name">{{ product.name }}</Link>
      <div class="market-product-prices">
        <strong>{{ formatPrice(product.price) }} ₫</strong>
        <del v-if="product.compare_price && product.compare_price > product.price">{{ formatPrice(product.compare_price) }} ₫</del>
      </div>
      <div v-if="product.store?.name" class="market-product-store">
        <span>Bán bởi</span>
        <Link v-if="product.store.id" :href="route('agriverse.shop.stores.show', product.store.id)">{{ product.store.name }} <span aria-hidden="true">→</span></Link>
        <strong v-else>{{ product.store.name }}</strong>
      </div>
      <p v-if="product.stock != null" class="market-product-stock">{{ product.stock > 0 ? `Còn ${product.stock} sản phẩm` : 'Tạm hết hàng' }}</p>
      <Link :href="route('agriverse.shop.products.show', product.id)" class="market-product-detail">Xem chi tiết <span aria-hidden="true">→</span></Link>
    </div>
  </article>
</template>

<script setup>
import {computed} from 'vue';
import {Link} from '@inertiajs/vue3';
import {route} from 'ziggy-js';
import {formatPrice} from '@agriverse/utils';

const props = defineProps({product: {type: Object, required: true}});
const discountPercent = computed(() => Math.round((1 - props.product.price / props.product.compare_price) * 100));
function pending() { window.dispatchEvent(new CustomEvent('bonsai:pending')); }
</script>

<style scoped>
.market-product-card { position: relative; display: flex; flex-direction: column; height: 100%; min-width: 0; overflow: hidden; background: var(--ag-bg-card); border: 1px solid var(--ag-border); border-radius: 16px; transition: box-shadow .2s, border-color .2s; }
.market-product-card:hover { border-color: var(--ag-primary-300); box-shadow: var(--ag-shadow-md); }
.market-product-image { position: relative; display: grid; place-items: center; aspect-ratio: 4 / 3; overflow: hidden; background: var(--ag-primary-50); }
.market-product-image img { width: 100%; height: 100%; object-fit: cover; }
.market-product-letter { font: 600 64px var(--ag-font-display); color: var(--ag-primary-300); }
.market-product-discount { position: absolute; top: 12px; left: 12px; padding: 4px 8px; border-radius: 8px; color: var(--ag-on-error); background: var(--ag-danger); font-size: 12px; font-weight: 700; }
.market-product-wishlist { position: absolute; top: 12px; right: 12px; display: grid; place-items: center; width: 36px; height: 36px; border-radius: 10px; background: var(--ag-bg-card); color: var(--ag-text-secondary); cursor: pointer; }
.market-product-body { display: flex; flex: 1; flex-direction: column; gap: 12px; padding: 18px; }
.market-product-name { display: -webkit-box; min-height: 42px; overflow: hidden; -webkit-box-orient: vertical; -webkit-line-clamp: 2; color: var(--ag-text-primary); font-weight: 650; line-height: 1.45; }
.market-product-name:hover, .market-product-store a:hover { color: var(--ag-primary-500); }
.market-product-prices { display: flex; align-items: baseline; flex-wrap: wrap; gap: 6px 10px; }
.market-product-prices strong { color: var(--ag-danger); font-size: 19px; }
.market-product-prices del { color: var(--ag-text-secondary); font-size: 12px; }
.market-product-store { display: flex; flex-direction: column; gap: 3px; padding-top: 12px; border-top: 1px solid var(--ag-border); font-size: 13px; }
.market-product-store > span { color: var(--ag-text-secondary); }
.market-product-store a, .market-product-store strong { width: fit-content; color: var(--ag-primary-600); font-weight: 700; }
.market-product-stock { color: var(--ag-text-secondary); font-size: 12px; }
.market-product-detail { display: flex; align-items: center; justify-content: space-between; margin-top: auto; padding: 10px 12px; border: 1px solid var(--ag-primary-500); border-radius: 10px; color: var(--ag-primary-600); font-size: 13px; font-weight: 700; }
.market-product-detail:hover { background: var(--ag-primary-50); }
.market-product-card :is(a, button):focus-visible { outline: 2px solid var(--ag-primary-500); outline-offset: 2px; }
</style>

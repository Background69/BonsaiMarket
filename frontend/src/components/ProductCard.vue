<template>
  <div class="ag-product-card">
    <Link :href="route('agriverse.shop.products.show', product.id)">
      <div class="ag-product-img flex items-center justify-center overflow-hidden">
        <img v-if="product.image" :alt="product.name" :src="product.image" class="w-full h-full object-cover absolute inset-0"
             @error="product.image = null"/>
        <span v-if="product.model_3d_path"
              class="absolute top-3 right-3 w-8 h-8 rounded-xl ag-glass flex items-center justify-center z-10"
              title="Có mô hình 3D">
          <span class="material-symbols-outlined text-sm" style="color: var(--ag-primary-500);">view_in_ar</span>
        </span>
        <span v-if="!product.image"
              class="text-4xl font-bold group-hover:scale-125 transition-transform duration-700 ease-out"
              style="color: var(--ag-neutral-300);">
          {{ product.name.charAt(0).toUpperCase() }}
        </span>
        <button :title="product.wishlisted ? 'Bỏ yêu thích' : 'Thêm yêu thích'"
                class="absolute top-3 right-3 w-8 h-8 rounded-xl ag-glass flex items-center justify-center z-10 transition-all duration-200 hover:scale-110"
                @click.prevent="toggleWishlist">
          <span :class="product.wishlisted ? 'text-[var(--ag-danger)]' : 'text-[var(--ag-text-muted)]'"
                :style="`font-variation-settings: 'FILL' ${product.wishlisted ? 1 : 0}`"
                class="material-symbols-outlined text-sm">
            favorite
          </span>
        </button>
        <span v-if="product.compare_price && product.compare_price > product.price"
              class="absolute top-3 left-3 text-white text-[10px] font-bold px-2.5 py-1 rounded-lg shadow-sm z-10"
              style="background: var(--ag-danger);">
          -{{ discountPercent }}%
        </span>
        <span v-if="product.has_approved_passport === false"
              class="absolute bottom-2 left-2 text-white text-[10px] font-bold px-2.5 py-1 rounded-lg shadow-sm z-10"
              style="background: rgba(217,119,6,0.92);"
              title="Cây chưa được cấp hộ chiếu NFT">
          Chưa có hộ chiếu
        </span>
      </div>
    </Link>
    <div class="ag-product-body">
      <Link :href="route('agriverse.shop.products.show', product.id)"
            :style="{ color: 'var(--ag-text-primary)' }"
            class="text-sm font-semibold ag-line-clamp-2 leading-snug min-h-[2.5rem] transition-colors duration-300"
            @mouseenter="$event.target.style.color = 'var(--ag-primary-500)'"
            @mouseleave="$event.target.style.color = 'var(--ag-text-primary)'">
        {{ product.name }}
      </Link>
      <div class="flex items-baseline gap-2 mt-2">
        <span class="ag-price text-base" style="color: var(--ag-danger);">{{ formatPrice(product.price) }}<span
            class="text-xs underline">₫</span></span>
        <span v-if="product.compare_price" class="ag-price-original">{{ formatPrice(product.compare_price) }}₫</span>
      </div>
      <div class="flex items-center justify-between mt-3 pt-3"
           style="border-top: 1px solid color-mix(in srgb, var(--ag-border) 60%, transparent);">
        <div class="flex items-center gap-1">
          <button :style="{ color: isComparing ? 'var(--ag-primary-500)' : 'var(--ag-text-muted)', background: isComparing ? 'rgba(72,103,48,0.08)' : '' }"
                  :title="isComparing ? 'Bỏ so sánh' : 'Thêm so sánh'"
                  class="w-8 h-8 rounded-lg flex items-center justify-center transition-all duration-200"
                  @click.stop="toggleCompare">
            <span class="material-symbols-outlined text-sm">compare_arrows</span>
          </button>
          <span class="text-xs font-medium" style="color: var(--ag-text-muted);">Đã bán {{
              product.sold_count ?? 0
            }}</span>
        </div>
        <button class="w-9 h-9 rounded-xl flex items-center justify-center transition-all duration-300 active:scale-90"
                style="border: 1px solid var(--ag-border); color: var(--ag-text-muted);"
                @mouseenter="$event.target.style.background = 'var(--ag-primary-500)'; $event.target.style.color = 'white'; $event.target.style.borderColor = 'var(--ag-primary-500)'"
                @mouseleave="$event.target.style.background = ''; $event.target.style.color = 'var(--ag-text-muted)'; $event.target.style.borderColor = 'var(--ag-border)'"
                @click.stop="addToCart">
          <span class="material-symbols-outlined text-base">add_shopping_cart</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import {computed} from 'vue';
import {Link} from '@inertiajs/vue3';
import {route} from 'ziggy-js';
import {formatPrice} from '@agriverse/utils';
import {useToast} from 'primevue/usetoast';
import {useCompare} from '@agriverse/Composables/useCompare';

const toast = useToast();
const {isComparing, toggle} = useCompare();
const props = defineProps({product: Object});

const discountPercent = computed(() => {
  if (!props.product?.compare_price) return 0;
  return Math.round((1 - props.product.price / props.product.compare_price) * 100);
});

function addToCart() {
  window.dispatchEvent(new CustomEvent('bonsai:pending'));
}

function toggleWishlist() {
  window.dispatchEvent(new CustomEvent('bonsai:pending'));
}

function toggleCompare() {
  window.dispatchEvent(new CustomEvent('bonsai:pending'));
}
</script>

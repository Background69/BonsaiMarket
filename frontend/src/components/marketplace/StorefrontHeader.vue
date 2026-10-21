<template>
  <header class="storefront-header" :class="{'storefront-header--compact':compact}">
    <CatalogImage v-if="!compact&&banner" class="storefront-banner" :src="banner" :alt="'Không gian giới thiệu '+store.name" ratio="4 / 1" fit="cover" priority label="Ảnh bìa chưa hiển thị được"/>
    <div class="storefront-summary"><CatalogImage :src="usableImage(store.logoUrl)||undefined" :alt="store.name" avatar priority/>
      <div class="storefront-name"><h1>{{ store.name }}</h1><p v-if="store.description">{{ store.description }}</p><p v-if="store.address" class="storefront-location"><MarketIcon name="pin"/>{{ store.address }}</p></div>
      <RouterLink v-if="!compact" :to="'/stores/'+store.id+'/products'" class="button button-primary">Xem sản phẩm<MarketIcon name="arrow"/></RouterLink>
    </div>
    <nav class="storefront-navigation" aria-label="Điều hướng gian hàng"><RouterLink :to="'/stores/'+store.id" :aria-current="compact?undefined:'page'">Trang chủ</RouterLink><RouterLink :to="'/stores/'+store.id+'/products'" :aria-current="compact?'page':undefined">Sản phẩm</RouterLink><a v-if="!compact&&(store.aboutShop||store.description)" href="#about-shop">Giới thiệu gian hàng</a></nav>
  </header>
</template>
<script setup>
import {computed} from 'vue';import CatalogImage from './CatalogImage.vue';import MarketIcon from './MarketIcon.vue';import {usableImage} from '../../services/catalog.js';
const props=defineProps({store:{type:Object,required:true},compact:Boolean});
const banner=computed(()=>usableImage(props.store.bannerUrl)||undefined);
</script>

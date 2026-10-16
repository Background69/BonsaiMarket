<template>
  <MarketplaceLayout><main class="market-container market-page marketplace-surface catalog-detail">
    <nav class="breadcrumbs" aria-label="Đường dẫn"><RouterLink to="/">Trang chủ</RouterLink><span>/</span><RouterLink to="/products">Sản phẩm</RouterLink><span>/</span><span>{{ product.name }}</span></nav>
    <section class="product-detail-grid">
      <div class="product-detail-media"><CatalogImage :alt="product.name" :src="product.image" priority @load="imageLoaded=true" @error="imageLoaded=false"/><p v-if="imageLoaded" class="image-note">Ảnh sản phẩm từ gian hàng.</p></div>
      <div class="product-detail-summary"><p v-if="product.category" class="detail-category">{{ product.category }}</p><h1>{{ product.name }}</h1><p class="detail-price">{{ money(product.price) }}</p>
        <p class="catalog-detail-stock" :class="{'catalog-sold-out':product.stock===0}">{{ Number.isInteger(product.stock) ? product.stock>0?'Còn hàng · '+product.stock+' sản phẩm':'Hết hàng' : 'Chưa có thông tin tồn kho' }}</p>
        <div class="catalog-purchase-status"><p v-if="product.stock===0">Sản phẩm hiện đã hết hàng. Khám phá thêm sản phẩm tại gian hàng.</p>
          <RouterLink :to="product.store?.id?'/stores/'+product.store.id:'/products'" class="button button-primary">{{ product.store?.id?product.stock===0?'Xem sản phẩm của gian hàng':'Xem gian hàng':'Khám phá sản phẩm khác' }}</RouterLink>
        </div>
        <section class="catalog-detail-seller" aria-label="Người bán"><p class="muted">Được đăng bán bởi</p><h2>{{ product.store?.name || 'Chưa có thông tin gian hàng' }}</h2><p v-if="product.store?.address">{{ product.store.address }}</p></section>
      </div>
    </section>
    <section class="detail-description"><h2>Mô tả sản phẩm</h2><p class="description-text">{{ product.description || 'Gian hàng chưa bổ sung mô tả cho sản phẩm này.' }}</p></section>
    <ReportForm v-if="product.id" :key="product.id" target-type="PRODUCT" :target-id="product.id"/>
    <section v-if="relatedProducts.length" class="market-section"><div class="section-heading"><h2>Sản phẩm khác từ cùng gian hàng</h2><RouterLink v-if="product.store?.id" :to="'/stores/'+product.store.id" class="text-link">Xem tất cả</RouterLink></div><div class="market-product-grid"><ProductCard v-for="p in relatedProducts" :key="p.id" :product="p" streamlined/></div></section>
  </main></MarketplaceLayout>
</template>
<script setup>
import MarketplaceLayout from '../../../layouts/MarketplaceLayout.vue';
import CatalogImage from '../../../components/marketplace/CatalogImage.vue';
import ProductCard from '../../../components/ProductCard.vue';
import ReportForm from '../../../components/marketplace/ReportForm.vue';
import {ref,watch} from 'vue';
const props=defineProps({product:{type:Object,default:()=>({})},relatedProducts:{type:Array,default:()=>[]}});
const imageLoaded=ref(false);
watch(()=>props.product.image,()=>imageLoaded.value=false);
const money=value=>typeof value==='number'?new Intl.NumberFormat('vi-VN',{style:'currency',currency:'VND'}).format(value):'Chưa có giá';
</script>

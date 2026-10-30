<template>
  <MarketplaceLayout><main class="market-container market-page marketplace-surface storefront-page">
    <nav class="storefront-breadcrumbs" aria-label="Đường dẫn điều hướng"><RouterLink to="/">Trang chủ</RouterLink><MarketIcon name="chevron"/><span aria-current="page">{{ store.name||'Gian hàng' }}</span></nav>
    <h1 v-if="isLoading||apiError">{{ isLoading?'Đang tải gian hàng…':'Gian hàng chưa khả dụng' }}</h1>
    <ResourceState v-if="isLoading||apiError" resource="gian hàng" :loading="isLoading" :error="apiError" :error-message="errorMessage" :not-found="notFound" recovery-to="/stores" recovery-label="Khám phá gian hàng" @retry="$emit('retry')"/>
    <template v-else-if="store.id">
      <StorefrontHeader :store="store"/>
      <section v-if="store.aboutShop||store.description" id="about-shop" class="storefront-about" aria-labelledby="about-shop-title"><div><h2 id="about-shop-title">Giới thiệu gian hàng</h2><p v-if="store.address" class="muted">{{ store.address }}</p><p v-if="store.phone">Liên hệ công khai: {{ store.phone }}</p></div><SellerText :text="store.aboutShop||store.description" collapsible/></section>
      <section class="storefront-products" aria-labelledby="store-products-title"><div class="section-heading"><h2 id="store-products-title">Sản phẩm của gian hàng</h2><RouterLink :to="'/stores/'+store.id+'/products'" class="text-link">Xem tất cả<MarketIcon name="arrow"/></RouterLink></div>
        <p v-if="previewError" role="alert">Chưa tải được sản phẩm. <button class="text-link" type="button" @click="$emit('retry')">Thử lại</button></p>
        <div v-else-if="products.data.length" class="market-product-grid"><ProductCard v-for="p in products.data" :key="p.id" :product="p" streamlined/></div>
        <p v-else class="muted">Gian hàng chưa có sản phẩm công khai. Bạn có thể quay lại sau.</p>
      </section>
      <StorePublicLocation :key="store.id" :store-id="store.id"/>
      <ReportForm :key="store.id" target-type="STORE" :target-id="store.id"/>
    </template>
  </main></MarketplaceLayout>
</template>
<script setup>
import StorePublicLocation from '../../../components/marketplace/StorePublicLocation.vue';
import MarketplaceLayout from '../../../layouts/MarketplaceLayout.vue';import StorefrontHeader from '../../../components/marketplace/StorefrontHeader.vue';import SellerText from '../../../components/marketplace/SellerText.vue';import ResourceState from '../../../components/marketplace/ResourceState.vue';import MarketIcon from '../../../components/marketplace/MarketIcon.vue';import ProductCard from '../../../components/ProductCard.vue';import ReportForm from '../../../components/marketplace/ReportForm.vue';
defineProps({store:{type:Object,default:()=>({})},products:{type:Object,default:()=>({data:[]})},isLoading:Boolean,apiError:Boolean,previewError:Boolean,errorMessage:String,notFound:Boolean});defineEmits(['retry']);
</script>

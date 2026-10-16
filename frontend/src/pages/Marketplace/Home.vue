<template>
  <MarketplaceLayout :categories="categories" :load-navigation-categories="false">
    <main class="refined-home market-container">
      <header class="home-introduction" data-section="hero">
        <h1>Tìm cây cho góc xanh của bạn</h1>
        <p>Bonsai, cây cảnh, chậu và phụ kiện từ những gian hàng độc lập trên BonsaiMarket.</p>
        <RouterLink class="button button-primary" to="/products">Khám phá sản phẩm <MarketIcon name="arrow"/></RouterLink>
      </header>
      <section class="home-section" data-section="categories" aria-labelledby="home-categories-title">
        <div class="section-heading"><h2 id="home-categories-title">Khám phá theo danh mục</h2><RouterLink class="text-link" to="/categories">Tất cả danh mục <MarketIcon name="arrow"/></RouterLink></div>
        <ResourceState v-if="categoryLoading || categoryError || !categories.length" resource="danh mục" :loading="categoryLoading" :error="categoryError" @retry="retryCategories"/>
        <nav v-else class="home-category-links" aria-label="Danh mục sản phẩm"><RouterLink v-for="category in categories" :key="category.id" :to="{path:'/products',query:{category:category.slug}}">{{ category.name }} <MarketIcon name="arrow"/></RouterLink></nav>
      </section>
      <section class="home-section" data-section="products" aria-labelledby="home-products-title">
        <div class="section-heading"><h2 id="home-products-title">Sản phẩm từ các gian hàng</h2><RouterLink class="text-link" to="/products">Tất cả sản phẩm <MarketIcon name="arrow"/></RouterLink></div>
        <ResourceState v-if="productLoading || productError || !products.length" resource="sản phẩm" :loading="productLoading" :error="productError" @retry="retryProducts"/>
        <div v-else class="home-products"><ProductCard v-for="product in products" :key="product.id" :product="product"/></div>
      </section>
      <section class="home-section" data-section="stores" aria-labelledby="home-stores-title">
        <div class="section-heading"><div><h2 id="home-stores-title">Khám phá gian hàng</h2><p class="muted">Tìm hiểu người bán và các sản phẩm trong từng gian hàng.</p></div><RouterLink class="text-link" to="/stores">Tất cả gian hàng <MarketIcon name="arrow"/></RouterLink></div>
        <ResourceState v-if="storeLoading || storeError || !stores.length" resource="gian hàng" :loading="storeLoading" :error="storeError" @retry="retryStores"/>
        <div v-else class="home-stores"><StoreCard v-for="store in stores.slice(0,3)" :key="store.id" :store="store"/></div>
      </section>
      <aside v-if="isSeller" class="home-seller-entry" data-section="seller-entry"><div><h2>Gian hàng của bạn</h2><p>Kiểm tra và cập nhật số lượng sản phẩm đang có.</p></div><RouterLink class="button button-outline" to="/seller/products">Quản lý tồn kho <MarketIcon name="arrow"/></RouterLink></aside>
    </main>
  </MarketplaceLayout>
</template>
<script setup>
import MarketplaceLayout from '../../layouts/MarketplaceLayout.vue';
import ProductCard from '../../components/ProductCard.vue';
import StoreCard from '../../components/marketplace/StoreCard.vue';
import ResourceState from '../../components/marketplace/ResourceState.vue';
import MarketIcon from '../../components/marketplace/MarketIcon.vue';
import api from '../../services/api.js';
import {productPage} from '../../services/catalog.js';
import {usePublicResource} from '../../composables/usePublicResource.js';
import {useAuth} from '../../composables/useAuth.js';
const {isSeller} = useAuth();
const {data:categories,loading:categoryLoading,error:categoryError,retry:retryCategories} = usePublicResource(async()=>(await api.get('/categories')).data);
const {data:products,loading:productLoading,error:productError,retry:retryProducts} = usePublicResource(async()=>(await productPage({size:8})).data);
const {data:stores,loading:storeLoading,error:storeError,retry:retryStores} = usePublicResource(async()=>(await api.get('/stores')).data);
</script>
<style scoped>
.refined-home {padding-block:32px 48px;}
.home-introduction {max-width:760px;padding-block:8px 32px;}
.home-introduction h1 {font-size:var(--bm-type-hero);line-height:1.2;letter-spacing:-.025em;color:var(--bm-dark);margin:0 0 16px;text-wrap:balance;}
.home-introduction p {font-size:18px;line-height:1.6;color:var(--bm-muted);max-width:56ch;margin:0 0 24px;}
.home-section {margin-top:32px;}
.home-section .section-heading {margin-bottom:20px;}
.home-section h2,.home-seller-entry h2 {font-size:var(--bm-type-section);line-height:1.3;margin:0;letter-spacing:-.015em;}
.home-category-links {display:flex;flex-wrap:wrap;gap:12px;}
.home-category-links a {display:flex;align-items:center;justify-content:space-between;gap:24px;min-height:48px;padding:12px 16px;border:1px solid var(--bm-border);border-radius:var(--bm-radius-control);color:var(--bm-forest);text-decoration:none;font-weight:600;}
.home-category-links a:hover {background:var(--bm-soft);border-color:var(--bm-control-border);}
.home-category-links svg {width:16px;height:16px;}
.home-products {display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:24px;align-items:start;}
.home-stores {display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:24px;}
.home-seller-entry {display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:20px;margin-top:48px;padding-top:24px;border-top:1px solid var(--bm-border);}
.home-seller-entry p {margin:8px 0 0;color:var(--bm-muted);}
@media(max-width:1023px){.home-products {grid-template-columns:repeat(3,minmax(0,1fr));gap:16px;}.home-stores {grid-template-columns:repeat(2,minmax(0,1fr));gap:16px;}}
@media(max-width:767px){.refined-home {padding-block:24px 32px;}.home-introduction {padding-block:0 8px;}.home-introduction p {font-size:16px;}.home-products {grid-template-columns:repeat(2,minmax(0,1fr));gap:12px;}.home-stores {grid-template-columns:1fr;}.home-section .section-heading {align-items:start;flex-wrap:wrap;gap:8px;}.home-category-links {display:grid;grid-template-columns:repeat(2,minmax(0,1fr));}.home-category-links a {gap:8px;padding:12px;font-size:14px;}}
</style>

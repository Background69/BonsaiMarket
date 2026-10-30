<template>
  <MarketplaceLayout :categories="categories" :load-navigation-categories="false" :categories-loading="categoryLoading" :categories-error="Boolean(categoryError)" @retry-categories="retryCategories">
    <main class="refined-home botanical-home market-container">
      <section class="home-introduction" data-section="hero" aria-labelledby="home-hero-title">
        <div class="home-hero-copy">
          <h1 id="home-hero-title">Tìm cây cho<br/><em>góc xanh</em> của bạn</h1>
          <p>Khám phá bonsai, cây cảnh, chậu và phụ kiện từ những gian hàng độc lập trên BonsaiMarket.</p>
          <div class="home-hero-actions">
            <RouterLink class="button button-primary" to="/products">Khám phá sản phẩm <MarketIcon name="arrow"/></RouterLink>
            <RouterLink class="home-link" to="/stores">Ghé thăm gian hàng <MarketIcon name="arrow"/></RouterLink>
          </div>
        </div>
        <figure v-if="heroProduct && !heroFailed" class="home-hero-visual botanical-arrive botanical-arrive--visual">
          <CatalogImage :src="heroProduct?.image" fallback="/images/hero-bonsai.svg" :alt="heroProduct && !heroFailed ? heroProduct.name : 'Minh họa cây bonsai trong chậu'" fit="cover" ratio="6 / 5" priority @error="heroFailed=true"/>
          <figcaption v-if="heroProduct && !heroFailed"><RouterLink :to="'/products/'+heroProduct.id">{{ heroProduct.name }} <MarketIcon name="arrow"/></RouterLink></figcaption>
          <figcaption v-else class="home-hero-illustration">Một góc xanh bắt đầu từ một chậu cây.<small>Minh họa bonsai</small></figcaption>
        </figure>
        <aside v-else class="home-hero-catalog" aria-labelledby="hero-catalog-title">
          <h2 id="hero-catalog-title">Bắt đầu từ một chậu cây</h2>
          <ResourceState v-if="productLoading || productError || !products.length" resource="sản phẩm" :loading="productLoading" :error="productError" @retry="retryProducts"/>
          <ul v-else><li v-for="product in products.slice(0,3)" :key="product.id"><RouterLink :to="'/products/'+product.id"><span><strong>{{ product.name }}</strong><small>{{ product.store?.name }}</small></span><span class="home-preview-price">{{ money(product.price) }}<MarketIcon name="arrow"/></span></RouterLink></li></ul>
          <RouterLink class="home-link" to="/products?sort=latest">Xem sản phẩm mới đăng <MarketIcon name="arrow"/></RouterLink>
        </aside>
      </section>

      <section class="home-section home-collections" data-section="categories" aria-labelledby="home-categories-title">
        <div class="section-heading"><div><h2 id="home-categories-title">Góc xanh, theo cách của bạn</h2><p>Bắt đầu khám phá từ danh mục bạn yêu thích.</p></div><RouterLink class="home-link" to="/categories">Tất cả danh mục <MarketIcon name="arrow"/></RouterLink></div>
        <ResourceState v-if="categoryLoading || categoryError || !categories.length" resource="danh mục" :loading="categoryLoading" :error="categoryError" @retry="retryCategories"/>
        <nav v-else class="home-category-links" aria-label="Danh mục sản phẩm"><RouterLink v-for="category in categories" :key="category.id" :to="{path:'/products',query:{categoryId:category.id}}"><span>{{ category.name }}</span><MarketIcon name="arrow"/></RouterLink></nav>
      </section>

      <section class="home-section" data-section="products" aria-labelledby="home-products-title">
        <div class="section-heading"><div><h2 id="home-products-title">Sản phẩm mới nhất</h2><p>Những sản phẩm mới được đăng bán công khai.</p></div><RouterLink class="home-link" to="/products?sort=latest">Xem tất cả <MarketIcon name="arrow"/></RouterLink></div>
        <ResourceState v-if="productLoading || productError || !products.length" resource="sản phẩm" :loading="productLoading" :error="productError" @retry="retryProducts"/>
        <div v-else class="home-products"><ProductCard v-for="product in products" :key="product.id" :product="product" botanical/></div>
      </section>

      <section class="home-botanical-break home-section" aria-labelledby="home-discovery-title" data-section="editorial">
        <div><h2 id="home-discovery-title">Tìm hiểu gian hàng<br/>trước khi chọn cây.</h2></div>
        <div class="home-editorial-copy"><p>Xem sản phẩm và phần giới thiệu của người bán để tìm cây cảnh, chậu và phụ kiện phù hợp với góc xanh của bạn.</p><RouterLink class="home-link" to="/stores">Xem các gian hàng <MarketIcon name="arrow"/></RouterLink><RouterLink class="home-community-link" to="/community">Trao đổi cách chăm cây ở cộng đồng <MarketIcon name="arrow"/></RouterLink></div>
      </section>

      <ProductSuggestions v-if="!productLoading && !productError" :exclude="products.map(p=>p.id)" botanical/>

      <section class="home-section" data-section="stores" aria-labelledby="home-stores-title">
        <div class="section-heading"><div><h2 id="home-stores-title">Khám phá gian hàng</h2><p>Gặp gỡ những gian hàng độc lập và khám phá lựa chọn của từng người bán.</p></div><RouterLink class="home-link" to="/stores">Tất cả gian hàng <MarketIcon name="arrow"/></RouterLink></div>
        <ResourceState v-if="storeLoading || storeError || !stores.length" resource="gian hàng" :loading="storeLoading" :error="storeError" @retry="retryStores"/>
        <div v-else class="home-stores"><StoreCard v-for="store in stores" :key="store.id" :store="store" botanical/></div>
      </section>
      <aside v-if="isSeller" class="home-seller-entry" data-section="seller-entry"><div><h2>Gian hàng của bạn</h2><p>Kiểm tra và cập nhật số lượng sản phẩm đang có.</p></div><RouterLink class="button button-outline" to="/seller/products">Quản lý tồn kho <MarketIcon name="arrow"/></RouterLink></aside>
    </main>
  </MarketplaceLayout>
</template>
<script setup>
import {computed,ref,watch} from 'vue';
import MarketplaceLayout from '../../layouts/MarketplaceLayout.vue';
import ProductCard from '../../components/ProductCard.vue';
import ProductSuggestions from '../../components/marketplace/ProductSuggestions.vue';
import StoreCard from '../../components/marketplace/StoreCard.vue';
import CatalogImage from '../../components/marketplace/CatalogImage.vue';
import ResourceState from '../../components/marketplace/ResourceState.vue';
import MarketIcon from '../../components/marketplace/MarketIcon.vue';
import api from '../../services/api.js';
import {productPage,storePage} from '../../services/catalog.js';
import {usePublicResource} from '../../composables/usePublicResource.js';
import {useAuth} from '../../composables/useAuth.js';
const {isSeller} = useAuth();
const money=value=>new Intl.NumberFormat('vi-VN',{style:'currency',currency:'VND'}).format(value);
const heroFailed=ref(false);
const {data:categories,loading:categoryLoading,error:categoryError,retry:retryCategories} = usePublicResource(async()=>(await api.get('/categories')).data);
const {data:products,loading:productLoading,error:productError,retry:retryProducts} = usePublicResource(async()=>(await productPage({size:8,sort:'latest'})).data);
const {data:stores,loading:storeLoading,error:storeError,retry:retryStores} = usePublicResource(async()=>(await storePage({size:3})).data);
const heroProduct=computed(()=>products.value.find(product=>product.image));
watch(()=>heroProduct.value?.image,()=>heroFailed.value=false);
</script>
<style scoped>
/* Tự phục vụ Lora thường/nghiêng; giấy phép và hash nguồn tại public/fonts/lora/. */
@font-face {font-family:'Lora';font-style:italic;font-weight:400;font-display:swap;src:url('/fonts/lora/lora-vietnamese-italic.woff2') format('woff2');unicode-range:U+20-7e, U+a0-ff, U+102-103, U+110-111, U+128-129, U+168-169, U+1a0-1a1, U+1af-1b0, U+300-303, U+306, U+308-309, U+31b, U+323, U+1ea0-1ef9, U+2013-2014, U+2018-2019, U+201c-201d, U+2026, U+20ab-20ac;}
@font-face {font-family:'Lora';font-style:normal;font-weight:400;font-display:swap;src:url('/fonts/lora/lora-vietnamese-normal.woff2') format('woff2');unicode-range:U+20-7e, U+a0-ff, U+102-103, U+110-111, U+128-129, U+168-169, U+1a0-1a1, U+1af-1b0, U+300-303, U+306, U+308-309, U+31b, U+323, U+1ea0-1ef9, U+2013-2014, U+2018-2019, U+201c-201d, U+2026, U+20ab-20ac;}
.botanical-home {--home-font-display:'Lora','Times New Roman',serif;--home-type-display:clamp(2.25rem,4.2vw,4rem);--home-type-section:clamp(1.625rem,2.3vw,2.125rem);--home-type-editorial:clamp(1.875rem,3vw,2.75rem);--home-type-body:1rem;--home-type-product:1rem;--home-type-price:1.25rem;--home-type-store:1.125rem;--home-type-meta:.875rem;--home-type-action:.9375rem;--home-section-space:clamp(48px,5vw,72px);padding-block:32px var(--home-section-space);color:var(--bm-ink);}
.home-introduction {display:grid;grid-template-columns:1.1fr 1fr;align-items:center;gap:clamp(32px,5vw,80px);padding:24px 0 40px;}
.home-hero-catalog {min-width:0;padding:28px 32px;background:var(--bm-surface);border:1px solid var(--bm-border);border-radius:var(--bm-radius);}
/* Reserve the three-product preview while its API loads; keep the desktop hero from shifting. */
@media(min-width:1024px){.home-hero-catalog {min-block-size:428px;}}
.home-hero-catalog h2 {font:400 clamp(22px,2vw,28px)/1.4 var(--home-font-display);color:var(--bm-dark);margin:0 0 12px;}
.home-hero-catalog ul {list-style:none;padding:0;margin:0 0 16px;}
.home-hero-catalog li {border-bottom:1px solid var(--bm-border);}
.home-hero-catalog li>a {display:flex;align-items:center;justify-content:space-between;gap:16px;padding:18px 0;color:var(--bm-ink);text-decoration:none;}
.home-hero-catalog li>a>span:first-child {min-width:0;}
.home-hero-catalog li>a:hover strong {color:var(--bm-forest);text-decoration:underline;text-underline-offset:4px;}
.home-hero-catalog strong {display:block;font-size:16px;line-height:1.5;overflow-wrap:anywhere;}
.home-hero-catalog small {display:block;color:var(--bm-muted);font-size:14px;line-height:1.5;margin-top:4px;}
.home-preview-price {display:flex;align-items:center;gap:12px;flex-shrink:0;color:var(--bm-forest);font-weight:700;font-size:15px;}
.home-preview-price svg {width:18px;height:18px;}
@media(max-width:767px){.home-hero-catalog {padding:20px;width:100%;}.home-hero-catalog li>a {align-items:start;flex-direction:column;gap:8px;}.home-preview-price {width:100%;justify-content:space-between;}}
.home-hero-copy h1 {font-family:var(--home-font-display);font-synthesis:none;font-size:var(--home-type-display);font-weight:400;line-height:1.22;letter-spacing:-.015em;color:var(--bm-dark);margin:0;max-width:100%;text-wrap:balance;}
.home-hero-copy h1 em {font-style:italic;color:var(--bm-leaf);}
.home-hero-copy>p {font-size:18px;line-height:1.7;color:var(--bm-muted);max-width:43ch;margin:24px 0 28px;}
.home-hero-actions {display:flex;align-items:center;flex-wrap:wrap;gap:12px 24px;}
.home-hero-actions .button {min-height:48px;padding:14px 20px;}
.home-hero-visual {min-width:0;margin:0;}
.home-hero-visual>.catalog-image {border-radius:var(--bm-radius);aspect-ratio:3 / 2 !important;}
.home-hero-visual figcaption {position:relative;padding:12px 0 0;font-size:var(--home-type-action);line-height:1.5;}
.home-hero-visual figcaption small {display:block;color:var(--bm-muted);font-size:var(--home-type-meta);margin-bottom:4px;}
.home-hero-visual figcaption a {display:inline-flex;align-items:center;gap:12px;color:var(--bm-forest);font-weight:600;text-decoration:none;}
.home-hero-visual figcaption svg {width:18px;height:18px;}
.home-hero-illustration {color:var(--bm-forest);}
.home-hero-illustration small {margin-top:4px;}
.home-section {margin-top:var(--home-section-space);}
.home-section .section-heading {display:flex;align-items:end;justify-content:space-between;gap:20px;margin-bottom:28px;}
.home-section h2,.home-seller-entry h2 {font-size:var(--home-type-section);font-weight:700;line-height:1.3;margin:0;letter-spacing:-.015em;text-wrap:balance;}
.home-section .section-heading p {margin:10px 0 0;max-width:58ch;font-size:var(--home-type-body);line-height:1.625;color:var(--bm-muted);}
.home-link {display:inline-flex;align-items:center;justify-content:space-between;gap:12px;min-height:44px;color:var(--bm-forest);font-size:var(--home-type-action);line-height:1.5;font-weight:700;text-decoration:none;}
.home-link svg,.home-category-links svg {width:18px;height:18px;transition:transform var(--bm-motion-fast) var(--bm-motion-ease);}
.home-link:hover {text-decoration:underline;text-underline-offset:5px;}
.home-link:hover svg,.home-category-links a:hover svg {transform:translateX(3px);}
.home-collections {margin-top:16px;padding:32px 0;border-block:1px solid var(--bm-border);}
.home-category-links {display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,240px),1fr));gap:0 32px;}
.home-category-links a {display:flex;align-items:center;justify-content:space-between;gap:16px;min-height:88px;padding:20px 0;border-bottom:1px solid var(--bm-border);color:var(--bm-forest);text-decoration:none;transition:color var(--bm-motion-fast),border-color var(--bm-motion-fast);}
.home-category-links a>span {font-size:clamp(20px,1.8vw,24px);font-weight:700;line-height:1.4;overflow-wrap:anywhere;}
.home-category-links a:hover {text-decoration:underline;text-underline-offset:5px;color:var(--bm-dark);border-color:var(--bm-forest);}
.home-products {display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:32px 24px;align-items:stretch;}
.home-stores {display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:24px;}
.home-botanical-break {display:grid;grid-template-columns:1fr 1fr;gap:40px;align-items:center;padding:clamp(28px,4vw,48px);background:var(--bm-dark);color:var(--bm-surface);border-radius:12px;}
.home-botanical-break h2 {font-family:var(--home-font-display);font-synthesis:none;font-size:var(--home-type-editorial);font-weight:400;line-height:1.25;letter-spacing:-.01em;color:var(--bm-surface);}
.home-editorial-copy>p {font-size:var(--home-type-body);line-height:1.75;color:#d2e1d7;margin:0 0 16px;max-width:48ch;}
.home-editorial-copy .home-link {color:var(--bm-yellow);margin-top:8px;}
.home-community-link {display:flex;align-items:center;gap:12px;width:fit-content;min-height:44px;font-size:var(--home-type-action);line-height:1.6;color:#d2e1d7;text-decoration:underline;text-underline-offset:4px;}
.home-community-link svg {width:16px;height:16px;}
.botanical-home :deep(.product-suggestions--botanical) {margin-top:var(--home-section-space);padding:32px;background:var(--bm-soft);border-radius:12px;}
.botanical-home :deep(.product-suggestions--botanical h2) {font-size:var(--home-type-section);font-weight:700;line-height:1.3;letter-spacing:-.015em;}
.botanical-home :deep(.product-suggestions--botanical .section-heading) {margin:0 0 28px;}
.botanical-home :deep(.product-suggestions--botanical .muted) {margin:10px 0 0;font-size:var(--home-type-body);line-height:1.625;}
.botanical-home :deep(.product-suggestions--botanical .suggestions-grid) {align-items:stretch;margin:0;}
.home-seller-entry {display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:20px;margin-top:var(--home-section-space);padding-top:24px;border-top:1px solid var(--bm-border);}
.home-seller-entry p {margin:8px 0 0;color:var(--bm-muted);}
.botanical-home :is(a,button):focus-visible {outline:2px solid var(--bm-forest);outline-offset:4px;}
.home-botanical-break a:focus-visible {outline-color:var(--bm-yellow);}
.botanical-home .button {font-size:var(--home-type-action);line-height:1.5;transition:background-color var(--bm-motion-fast),transform var(--bm-motion-fast);}
.botanical-home .button:active {transform:translateY(1px);}
.botanical-home .button:disabled {opacity:.55;cursor:default;}
@media(max-width:1023px){.home-products {grid-template-columns:repeat(3,minmax(0,1fr));gap:28px 16px;}.home-stores {gap:16px;}.home-introduction {gap:28px;padding-top:16px;}.home-hero-actions {gap:8px;}.home-botanical-break {gap:32px;}.home-category-links {grid-template-columns:repeat(2,minmax(0,1fr));}.botanical-home :deep(.product-suggestions--botanical) {padding:24px;}}
@media(max-width:767px){.botanical-home {padding-top:24px;}.home-introduction {grid-template-columns:1fr;gap:28px;padding:0 0 32px;}.home-hero-copy>p {font-size:16px;margin:20px 0;max-width:48ch;}.home-hero-actions {gap:8px 20px;}.home-hero-visual {width:min(100%,440px);justify-self:center;}.home-hero-visual>.catalog-image {aspect-ratio:3 / 2!important;}.home-products {grid-template-columns:repeat(2,minmax(0,1fr));gap:28px 16px;}.home-stores {grid-template-columns:1fr;}.home-section .section-heading {align-items:start;flex-wrap:wrap;gap:8px;}.home-collections {padding:24px 0;}.home-category-links {gap:0 20px;}.home-category-links a {min-height:80px;gap:12px;}.home-category-links a>span {font-size:20px;}.home-botanical-break {grid-template-columns:1fr;gap:24px;}.botanical-home :deep(.product-suggestions--botanical) {padding:24px 16px;}.botanical-home :deep(.product-suggestions--botanical .suggestions-grid) {gap:28px 16px;}}
@media(prefers-reduced-motion:reduce){.botanical-home .button:active,.home-link:hover svg,.home-category-links a:hover svg {transform:none;}}
</style>

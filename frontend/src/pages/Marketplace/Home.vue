<template>
  <MarketplaceLayout :categories="categories">
    <main class="marketplace-home reference-marketplace-home">
      <section class="market-hero" data-section="hero">
        <CatalogImage class="hero-landscape" :src="marketplaceVisuals.heroImage" :fallback="marketplaceVisuals.heroFallback" alt="Bonsai trong không gian thiên nhiên" ratio="auto" priority fit="cover" />
        <div class="market-container hero-inner">
          <div class="market-hero-copy">
            <h1>Khám phá bonsai & cây cảnh <br />từ nhiều gian hàng</h1>
            <p>Kết nối với những người bán cây cảnh độc lập.<br class="desktop-break" /> Khám phá sản phẩm và tìm khu vườn phù hợp với bạn.</p>
            <div class="button-row">
              <RouterLink to="/products" class="button button-primary">Khám phá sản phẩm <MarketIcon name="arrow" /></RouterLink>
              <RouterLink to="/stores" class="button button-outline">Khám phá gian hàng</RouterLink>
            </div>
            <div class="hero-highlights">
              <span><MarketIcon name="store" />Gian hàng độc lập</span>
              <span><MarketIcon name="leaf" />Bonsai & cây cảnh</span>
              <span><MarketIcon name="sprout" />Kết nối người yêu cây</span>
            </div>
          </div>
          <p class="hero-handwriting" aria-hidden="true">Kết nối<br /><span>những người yêu cây</span></p>
        </div>
      </section>
      <div class="market-container home-catalog">
        <section class="market-section category-section" data-section="categories">
          <div class="section-heading"><h2>Danh mục nổi bật</h2><RouterLink to="/categories" class="text-link">Xem tất cả danh mục <MarketIcon name="arrow" /></RouterLink></div>
          <div v-if="categories.length" class="market-category-grid">
            <RouterLink v-for="category in categories" :key="category.id" class="market-category" :to="{path:'/products',query:{category:category.slug}}">
              <CatalogImage :src="category.imageUrl || category.image || categoryVisuals[category.id] || categoryProductImage(category)" :alt="category.name" label="Ảnh danh mục" fit="cover" />
              <span><strong>{{ category.name }}</strong><small v-if="category.description">{{ category.description }}</small><small v-else-if="Number.isInteger(category.products_count)">{{ category.products_count }} sản phẩm</small></span>
              <MarketIcon name="chevron" />
            </RouterLink>
          </div>
          <p v-else-if="!isLoading && !apiError" class="market-empty">Chưa có danh mục công khai.</p>
        </section>
        <section class="market-section" data-section="products">
          <div class="section-heading"><div><h2>Khám phá sản phẩm</h2><p class="muted">Sản phẩm từ các gian hàng trên BonsaiMarket</p></div><RouterLink to="/products" class="text-link">Xem tất cả sản phẩm <MarketIcon name="arrow" /></RouterLink></div>
          <div v-if="featuredProducts.length" class="market-product-grid home-primary-products"><ProductCard v-for="product in featuredProducts.slice(0,6)" :key="product.id" :product="product" /></div>
          <p v-else-if="!isLoading && !apiError" class="market-empty">Chưa có sản phẩm công khai. Bạn có thể quay lại sau.</p>
        </section>
        <section class="market-section" data-section="stores">
          <div class="section-heading"><div><h2>Khám phá gian hàng</h2><p class="muted">Mỗi người bán, một khu vườn và phong cách riêng</p></div><RouterLink to="/stores" class="text-link">Xem tất cả gian hàng <MarketIcon name="arrow" /></RouterLink></div>
          <div v-if="stores.length" class="market-store-grid home-store-grid"><StoreCard v-for="store in stores.slice(0,5)" :key="store.id" :store="store" :preview-image="storeProductImage(store)" /></div>
          <p v-else-if="!isLoading && !apiError" class="market-empty">Chưa có gian hàng công khai.</p>
        </section>
        <section class="market-section" data-section="recommendations">
          <div class="section-heading"><div><h2>Có thể bạn quan tâm</h2><p class="muted">Khám phá thêm những sản phẩm còn hàng</p></div></div>
          <div v-if="suggestions.length" class="market-product-grid home-recommendations"><ProductCard v-for="product in suggestions" :key="product.id" :product="product" compact /></div>
          <p v-else-if="!isLoading && !apiError" class="market-empty">Các sản phẩm còn hàng khác sẽ xuất hiện tại đây.</p>
        </section>
        <div class="home-feature-pair">
          <section class="market-seller-entry home-feature-banner" data-section="seller-entry">
            <div class="feature-icon"><MarketIcon name="store" /></div>
            <div class="feature-copy"><h2>Dành cho người bán</h2><h3>Mở gian hàng trên BonsaiMarket</h3><p>Kết nối với người yêu cây, giới thiệu sản phẩm và xây dựng gian hàng của riêng bạn.</p><span class="feature-pending">Đăng ký gian hàng đang được phát triển</span><button class="button button-yellow" disabled>Mở gian hàng · Sắp ra mắt <MarketIcon name="arrow" /></button></div>
            <CatalogImage v-if="marketplaceVisuals.sellerImage" class="feature-artwork" :src="marketplaceVisuals.sellerImage" alt="Minh họa gian hàng cây cảnh" ratio="1" fit="contain" />
            <div v-else class="seller-banner-art" aria-hidden="true"><MarketIcon name="sprout" /><div class="mini-store"><MarketIcon name="store" /><span>BonsaiMarket</span></div></div>
          </section>
          <section class="market-ai-entry home-feature-banner" data-section="ai">
            <div class="feature-icon"><MarketIcon name="robot" /></div>
            <div class="feature-copy"><h2>Trợ lý AI BonsaiMarket</h2><h3>Hỏi đáp, tư vấn chăm sóc cây cảnh</h3><p>Nhận gợi ý chọn cây và tìm hiểu cách chăm sóc cùng trợ lý AI.</p><button class="button button-primary" @click="openAI">Hỏi AI ngay <MarketIcon name="arrow" /></button></div>
            <div class="ai-question-examples" aria-hidden="true"><span>Cây mai bị vàng lá phải làm sao?</span><span>Nên chọn bonsai nào để bàn?</span><span>Cách chăm sen đá mùa mưa?</span><MarketIcon name="sprout" /></div>
          </section>
        </div>
        <section class="market-section home-community" data-section="community">
          <div class="section-heading"><div><h2>Cộng đồng BonsaiMarket</h2><p class="muted">Chia sẻ kinh nghiệm và kết nối cùng những người yêu cây cảnh</p></div><span class="community-status">Sắp ra mắt</span></div>
          <div class="community-introduction"><MarketIcon name="chat" /><p>Không gian thảo luận về bonsai và cây cảnh đang được chuẩn bị.<br /><span>Bạn có thể hỏi trợ lý AI về cách chọn và chăm cây ngay hôm nay.</span></p><button class="text-link" @click="openAI">Hỏi trợ lý AI <MarketIcon name="arrow" /></button></div>
        </section>
      </div>
    </main>
  </MarketplaceLayout>
</template>
<script setup>
import {computed} from 'vue';
import MarketplaceLayout from '../../layouts/MarketplaceLayout.vue';
import ProductCard from '../../components/ProductCard.vue';
import StoreCard from '../../components/marketplace/StoreCard.vue';
import CatalogImage from '../../components/marketplace/CatalogImage.vue';
import MarketIcon from '../../components/marketplace/MarketIcon.vue';
import {categoryVisuals, productVisuals, marketplaceVisuals} from '../../config/homeVisuals.js';
const props = defineProps({categories:{type:Array,default:()=>[]},featuredProducts:{type:Array,default:()=>[]},stores:{type:Array,default:()=>[]},isLoading:Boolean,apiError:Boolean});
// Catalog order and the existing in-stock rule; discovery, not personalization.
const suggestions = computed(() => props.featuredProducts.slice(6).filter(p => p.stock > 0).slice(0,6));
function productImage(p) {return p?.image || productVisuals[p?.id] || p?.imageUrl;}
function categoryProductImage(category) {return productImage(props.featuredProducts.find(p => (typeof p.category === 'object' ? p.category?.name : p.category) === category.name && productImage(p)));}
function storeProductImage(store) {return productImage(props.featuredProducts.find(p => p.store?.id === store.id && productImage(p)));}
function openAI() {window.dispatchEvent(new CustomEvent('agriverse-open-ai-expert'));}
</script>

<template>
  <MarketplaceLayout>
    <main class="market-container market-page marketplace-surface catalog-detail">
      <nav class="product-detail-breadcrumbs" aria-label="Đường dẫn điều hướng">
        <RouterLink to="/">Trang chủ</RouterLink><MarketIcon name="chevron"/>
        <template v-if="storeLink"><RouterLink :to="storeLink">{{ product.store.name }}</RouterLink><MarketIcon name="chevron"/><RouterLink :to="storeLink+'/products'">Sản phẩm</RouterLink><MarketIcon name="chevron"/></template>
        <template v-else><RouterLink to="/products">Sản phẩm</RouterLink><MarketIcon name="chevron"/></template>
        <span aria-current="page">{{ product.name }}</span>
      </nav>
      <section class="product-detail-hero" aria-labelledby="product-title">
        <figure class="product-detail-media" :class="{'product-detail-media--missing':imageUnavailable}">
          <div class="product-detail-gallery-main">
            <CatalogImage :alt="product.name" :src="product.image" :ratio="imageUnavailable?'4 / 3':'1'"
                          fit="contain" priority @load="imageLoaded=true" @error="imageLoaded=false; imageFailed=true"/>
            <button v-if="imageLoaded" ref="zoomTrigger" class="product-detail-zoom" type="button"
                    aria-haspopup="dialog" aria-controls="product-image-dialog" @click="imageDialog.showModal()">
              <MarketIcon name="search"/>Phóng to ảnh
            </button>
          </div>
          <figcaption v-if="imageLoaded" class="image-note"><MarketIcon name="leaf"/>Ảnh sản phẩm từ gian hàng.</figcaption>
          <figcaption v-else-if="imageUnavailable" class="product-detail-media-note">Gian hàng chưa cung cấp ảnh có thể hiển thị.</figcaption>
        </figure>
        <div class="product-detail-info">
          <header>
            <p v-if="product.category" class="product-detail-category">{{ product.category }}</p>
            <h1 id="product-title">{{ product.name }}</h1>
            <p class="product-detail-price">{{ money(product.price) }}</p>
            <p class="product-detail-availability" :class="{'is-sold-out':soldOut,'is-unknown':!knownStock}">
              <MarketIcon :name="knownStock? soldOut?'box':'check':'info'"/>
              {{ knownStock ? soldOut?'Hết hàng':'Còn hàng' : 'Chưa có thông tin tồn kho' }}
            </p>
          </header>
          <p v-if="description" class="product-detail-preview"><template v-if="longDescription">{{ descriptionPreview }}… </template><a href="#product-description" @click.prevent="information?.openDescription()">Đọc mô tả đầy đủ</a></p>
          <p v-else class="product-detail-preview product-detail-muted">Gian hàng chưa bổ sung mô tả cho sản phẩm này.</p>
          <dl v-if="knownStock&&!soldOut || product.store?.address" class="product-detail-attributes" aria-label="Thông tin sản phẩm">
            <div v-if="knownStock&&!soldOut"><dt><MarketIcon name="box"/>Số lượng hiện có</dt><dd>{{ product.stock }} sản phẩm</dd></div>
            <div v-if="product.store?.address"><dt><MarketIcon name="pin"/>Gian hàng tại</dt><dd>{{ product.store.address }}</dd></div>
          </dl>
          <section class="product-detail-store" aria-label="Gian hàng bán sản phẩm">
            <div class="product-detail-store-identity">
              <CatalogImage :alt="product.store?.name || 'Gian hàng'" :src="storeLogo" avatar/>
              <div><h2>{{ product.store?.name || 'Chưa có thông tin gian hàng' }}</h2><p v-if="product.store?.description">{{ product.store.description }}</p></div>
            </div>
            <RouterLink v-if="storeLink" :to="storeLink" class="button" :class="soldOut?'button-outline':'button-primary'">Xem gian hàng<MarketIcon name="arrow"/></RouterLink>
          </section>
          <div class="product-detail-actions">
            <RouterLink v-if="!soldOut && product.id" :to="'/checkout?productId=' + product.id" class="button button-primary product-buy-now-btn"><MarketIcon name="cart"/><span>Đặt mua ngay</span></RouterLink>
            <p v-if="soldOut" class="product-detail-sold-out-note">Sản phẩm hiện đã hết hàng. Bạn có thể khám phá những sản phẩm khác.</p>
            <a v-if="sameStoreProducts.length" class="product-detail-discover" href="#product-related" @click.prevent="focusSection('product-related-title')">
              <MarketIcon name="leaf"/><span>Xem sản phẩm cùng gian hàng</span><MarketIcon name="chevron"/>
            </a>
            <RouterLink v-else-if="soldOut || !storeLink" class="product-detail-discover" :to="storeLink ? storeLink+'/products' : '/products'">
              <MarketIcon name="leaf"/><span>{{ storeLink?'Xem sản phẩm cùng gian hàng':'Khám phá sản phẩm khác' }}</span><MarketIcon name="chevron"/>
            </RouterLink>
            <ReportForm v-if="product.id" :key="product.id" target-type="PRODUCT" :target-id="product.id"/>
          </div>
        </div>
      </section>
      <ProductInformationTabs ref="information" :product="product"/>
      <ProductReviews v-if="product.id" :key="product.id" :product-id="product.id"/>
      <ProductQuestions v-if="product.id" :key="product.id" :product-id="product.id"/>
      <ProductSuggestions v-if="product.id" :key="product.id" :product-id="product.id"/>
      <section v-if="sameStoreProducts.length" id="product-related" class="product-detail-related" aria-labelledby="product-related-title">
        <div class="product-detail-section-heading">
          <div><h2 id="product-related-title" tabindex="-1">Sản phẩm khác từ cùng gian hàng</h2><p v-if="product.store?.name">Khám phá thêm từ {{ product.store.name }}.</p></div>
          <RouterLink :to="storeLink+'/products'" class="text-link">Xem tất cả<MarketIcon name="arrow"/></RouterLink>
        </div>
        <div class="product-detail-related-grid"><ProductCard v-for="p in sameStoreProducts" :key="p.id" :product="p" streamlined/></div>
      </section>
      <dialog id="product-image-dialog" ref="imageDialog" class="product-detail-lightbox" aria-label="Ảnh sản phẩm phóng to"
              @close="zoomTrigger?.focus()" @click="closeOnBackdrop" @keydown.tab.prevent="imageDialog.querySelector('button').focus()">
        <button type="button" class="button button-outline" autofocus @click="imageDialog.close()"><MarketIcon name="close"/>Đóng ảnh</button>
        <img v-if="imageLoaded" :src="product.image" :alt="product.name"/>
      </dialog>
    </main>
  </MarketplaceLayout>
</template>
<script setup>
import {computed, ref, watch} from 'vue';
import MarketplaceLayout from '../../../layouts/MarketplaceLayout.vue';
import CatalogImage from '../../../components/marketplace/CatalogImage.vue';
import ProductCard from '../../../components/ProductCard.vue';
import ReportForm from '../../../components/marketplace/ReportForm.vue';
import MarketIcon from '../../../components/marketplace/MarketIcon.vue';
import ProductInformationTabs from '../../../components/marketplace/ProductInformationTabs.vue';
import ProductReviews from '../../../components/marketplace/ProductReviews.vue';
import ProductQuestions from '../../../components/marketplace/ProductQuestions.vue';
import ProductSuggestions from '../../../components/marketplace/ProductSuggestions.vue';
import {usableImage} from '../../../services/catalog.js';

const props=defineProps({product:{type:Object,default:()=>({})},relatedProducts:{type:Array,default:()=>[]}});
const imageLoaded=ref(false), imageFailed=ref(false), imageDialog=ref(null), zoomTrigger=ref(null);
const information=ref(null);
const imageUnavailable=computed(()=>!usableImage(props.product.image)||imageFailed.value);
const knownStock=computed(()=>Number.isInteger(props.product.stock)&&props.product.stock>=0);
const soldOut=computed(()=>knownStock.value&&props.product.stock===0);
const storeLink=computed(()=>Number.isSafeInteger(props.product.store?.id)&&props.product.store.id>0?'/stores/'+props.product.store.id:null);
const storeLogo=computed(()=>usableImage(props.product.store?.logoUrl)||undefined);
const description=computed(()=>typeof props.product.description==='string'?props.product.description.trim():'');
const longDescription=computed(()=>description.value.length>220);
const descriptionPreview=computed(()=>{
  // Không cắt giữa từ; mô tả đầy đủ và xuống dòng vẫn giữ bên dưới.
  const words=description.value.replace(/^\s*#{1,3}\s+.*$/gm,'').split(/\s+/).filter(Boolean), preview=[];
  let length=0;
  for(const word of words){if(length>=220)break;preview.push(word);length+=word.length+1;}
  return preview.join(' ');
});
const sameStoreProducts=computed(()=>storeLink.value?props.relatedProducts.filter(p=>p.id!==props.product.id&&p.store?.id===props.product.store.id).slice(0,4):[]);
const currency=new Intl.NumberFormat('vi-VN',{style:'currency',currency:'VND'});
const money=value=>typeof value==='number'&&Number.isFinite(value)&&value>=0?currency.format(value):'Chưa có giá';
watch(()=>[props.product.id,props.product.image],()=>{imageLoaded.value=false;imageFailed.value=false;imageDialog.value?.close();});
function focusSection(id){
  const heading=document.getElementById(id);
  if(!heading)return;
  heading.focus({preventScroll:true});
  heading.scrollIntoView({block:'start',behavior:'instant'});
}
function closeOnBackdrop(event){
  if(event.target!==imageDialog.value)return;
  const rect=imageDialog.value.getBoundingClientRect();
  if(event.clientX<rect.left||event.clientX>rect.right||event.clientY<rect.top||event.clientY>rect.bottom)imageDialog.value.close();
}
</script>

<style scoped>
.product-buy-now-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 14px 24px;
  font-size: 16px;
  font-weight: 700;
  text-decoration: none;
  border-radius: 12px;
  margin-bottom: 12px;
}
</style>

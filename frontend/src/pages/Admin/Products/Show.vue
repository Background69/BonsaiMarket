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
            <RouterLink v-if="product.category && categoryLink" :to="categoryLink" class="product-detail-category product-detail-category--link">{{ product.category }}</RouterLink>
            <p v-else-if="product.category" class="product-detail-category">{{ product.category }}</p>
            <h1 id="product-title">{{ product.name }}</h1>
            <p class="product-detail-price">
              {{ money(product.price) }}
              <template v-if="discountPercent">
                <s class="product-detail-compare" :aria-label="'Giá gốc '+money(product.compare_price)">{{ money(product.compare_price) }}</s>
                <span class="product-detail-discount">-{{ discountPercent }}%</span>
              </template>
            </p>
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
            <div v-if="canBuy" class="product-quantity">
              <label for="product-quantity-input">Số lượng</label>
              <div class="product-quantity-control">
                <button type="button" aria-label="Giảm số lượng" :disabled="quantity<=1" @click="setQuantity(quantity-1)">−</button>
                <input id="product-quantity-input" type="number" inputmode="numeric" min="1" :max="maxQuantity" :value="quantity"
                       @change="setQuantity($event.target.value); $event.target.value=quantity"/>
                <button type="button" aria-label="Tăng số lượng" :disabled="quantity>=maxQuantity" @click="setQuantity(quantity+1)">+</button>
              </div>
              <span v-if="knownStock" class="muted">Tối đa {{ maxQuantity }}</span>
            </div>
            <RouterLink v-if="canBuy" :to="{path:'/checkout',query:{productId:product.id,quantity}}" class="button button-primary product-buy-now-btn"><MarketIcon name="cart"/><span>Đặt mua ngay</span></RouterLink>
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
import {computed, onBeforeUnmount, ref, watch} from 'vue';
import MarketplaceLayout from '../../../layouts/MarketplaceLayout.vue';
import CatalogImage from '../../../components/marketplace/CatalogImage.vue';
import ProductCard from '../../../components/ProductCard.vue';
import ReportForm from '../../../components/marketplace/ReportForm.vue';
import MarketIcon from '../../../components/marketplace/MarketIcon.vue';
import ProductInformationTabs from '../../../components/marketplace/ProductInformationTabs.vue';
import ProductQuestions from '../../../components/marketplace/ProductQuestions.vue';
import ProductSuggestions from '../../../components/marketplace/ProductSuggestions.vue';
import {usableImage} from '../../../services/catalog.js';

const props=defineProps({product:{type:Object,default:()=>({})},relatedProducts:{type:Array,default:()=>[]}});
const imageLoaded=ref(false), imageFailed=ref(false), imageDialog=ref(null), zoomTrigger=ref(null);
const information=ref(null);
const imageUnavailable=computed(()=>!usableImage(props.product.image)||imageFailed.value);
const knownStock=computed(()=>Number.isInteger(props.product.stock)&&props.product.stock>=0);
const soldOut=computed(()=>knownStock.value&&props.product.stock===0);
// Giá gốc chỉ hiển thị khi cao hơn giá bán (compare_price > price) để tránh gạch giá vô nghĩa.
const hasPrice=v=>typeof v==='number'&&Number.isFinite(v)&&v>=0;
const discountPercent=computed(()=>{
  const {price,compare_price:compare}=props.product;
  return hasPrice(price)&&hasPrice(compare)&&compare>price?Math.round((1-price/compare)*100):0;
});
const categoryLink=computed(()=>Number.isSafeInteger(props.product.categoryId)&&props.product.categoryId>0?{path:'/products',query:{categoryId:props.product.categoryId}}:null);
// Số lượng mua: 1..tồn kho (tối đa 1.000.000 theo CheckoutDtos). Checkout vẫn kiểm tra lại tồn kho ở backend.
const quantity=ref(1);
const maxQuantity=computed(()=>knownStock.value?Math.min(props.product.stock,1000000):1000000);
const canBuy=computed(()=>Boolean(props.product.id)&&!soldOut.value);
function setQuantity(value){
  const n=Math.trunc(Number(value));
  quantity.value=Number.isFinite(n)?Math.min(Math.max(n,1),maxQuantity.value):1;
}
const storeLink=computed(()=>Number.isSafeInteger(props.product.store?.id)&&props.product.store.id>0?'/stores/'+props.product.store.id:null);
const storeLogo=computed(()=>usableImage(props.product.store?.logoUrl)||undefined);
const description=computed(()=>typeof props.product.description==='string'?props.product.description.trim():'');
const longDescription=computed(()=>description.value.length>220);
const descriptionPreview=computed(()=>{
  // Preserve whole words; the complete seller text stays below, including newlines.
  const words=description.value.replace(/^\s*#{1,3}\s+.*$/gm,'').split(/\s+/).filter(Boolean), preview=[];
  let length=0;
  for(const word of words){if(length>=220)break;preview.push(word);length+=word.length+1;}
  return preview.join(' ');
});
const sameStoreProducts=computed(()=>storeLink.value?props.relatedProducts.filter(p=>p.id!==props.product.id&&p.store?.id===props.product.store.id).slice(0,4):[]);
const currency=new Intl.NumberFormat('vi-VN',{style:'currency',currency:'VND'});
const money=value=>typeof value==='number'&&Number.isFinite(value)&&value>=0?currency.format(value):'Chưa có giá';
watch(()=>[props.product.id,props.product.image],()=>{imageLoaded.value=false;imageFailed.value=false;imageDialog.value?.close();});
// Đổi sản phẩm (hoặc tồn kho thay đổi) thì đặt lại số lượng hợp lệ.
watch(()=>[props.product.id,props.product.stock],()=>setQuantity(1));
// Tiêu đề tab trình duyệt theo tên sản phẩm; trả lại tiêu đề cũ khi rời trang.
const previousTitle=typeof document!=='undefined'?document.title:'';
watch(()=>props.product.name,name=>{if(name&&typeof document!=='undefined')document.title=name+' | BonsaiMarket';},{immediate:true});
onBeforeUnmount(()=>{if(typeof document!=='undefined')document.title=previousTitle;});
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
.product-detail-category--link{text-decoration:none}
.product-detail-category--link:hover{text-decoration:underline}
.product-detail-compare{margin-left:12px;font-size:.55em;font-weight:500;color:var(--bm-muted)}
.product-detail-discount{margin-left:8px;padding:2px 8px;border-radius:var(--bm-radius-control);background:var(--bm-soft);color:var(--bm-forest);font-size:.5em;font-weight:700;vertical-align:middle}
.product-quantity{display:flex;align-items:center;flex-wrap:wrap;gap:12px;margin-bottom:12px}
.product-quantity label{font-weight:600}
.product-quantity-control{display:inline-flex;align-items:stretch;border:1px solid var(--bm-border);border-radius:var(--bm-radius-control);overflow:hidden}
.product-quantity-control button{min-width:44px;min-height:44px;border:0;background:var(--bm-soft);color:var(--bm-forest);font-size:20px;cursor:pointer}
.product-quantity-control button:disabled{opacity:.45;cursor:not-allowed}
.product-quantity-control input{width:72px;border:0;text-align:center;font:inherit;font-variant-numeric:tabular-nums;background:transparent;color:inherit;appearance:textfield;-moz-appearance:textfield}
.product-quantity-control input::-webkit-outer-spin-button,.product-quantity-control input::-webkit-inner-spin-button{-webkit-appearance:none;margin:0}
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
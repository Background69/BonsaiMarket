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
          <p v-if="description" class="product-detail-preview">{{ longDescription ? descriptionPreview+'…' : description }} <a href="#product-description" @click.prevent="information?.openDescription()">Đọc mô tả đầy đủ</a></p>
          <p v-else class="product-detail-preview product-detail-muted">Gian hàng chưa bổ sung mô tả cho sản phẩm này.</p>
          <dl v-if="knownStock&&!soldOut || product.store?.address" class="product-detail-attributes" aria-label="Thông tin sản phẩm">
            <div v-if="knownStock&&!soldOut"><dt><MarketIcon name="box"/>Số lượng hiện có</dt><dd>{{ product.stock }} sản phẩm</dd></div>
            <div v-if="product.store?.address"><dt><MarketIcon name="pin"/>Gian hàng tại</dt><dd>{{ product.store.address }}</dd></div>
          </dl>
          <dl v-if="careHighlights.length" class="product-care-highlights" aria-label="Thông tin hỗ trợ chọn sản phẩm"><div v-for="item in careHighlights" :key="item.label"><dt>{{ item.label }}</dt><dd>{{ item.value }}</dd></div></dl>
          <div class="product-detail-actions">
            <div v-if="knownStock && !soldOut" class="product-purchase-quantity"><label for="product-quantity">Số lượng mua</label><div><button type="button" aria-label="Giảm số lượng mua" :disabled="addingToCart || quantity<=1" @click="quantity=Math.max(1,quantity-1)">−</button><input id="product-quantity" ref="quantityInput" v-model.number="quantity" type="number" inputmode="numeric" min="1" :max="maximumQuantity" step="1" :disabled="addingToCart" :aria-invalid="!validQuantity" aria-describedby="product-quantity-help"/><button type="button" aria-label="Tăng số lượng mua" :disabled="addingToCart || quantity>=maximumQuantity" @click="quantity=Math.min(maximumQuantity,quantity+1)">+</button></div><p id="product-quantity-help" :class="{'product-cart-error':!validQuantity}">{{ validQuantity ? 'Giá và tồn kho được kiểm tra lại khi đặt hàng.' : 'Nhập số nguyên từ 1 đến '+maximumQuantity+'.' }}</p></div>
            <button v-if="knownStock && !soldOut && product.id" class="button button-outline product-add-cart" type="button" :disabled="addingToCart || !validQuantity" :aria-busy="addingToCart" @click="addToCart"><MarketIcon :name="cartMessage ? 'check' : 'cart'"/>{{ addingToCart ? 'Đang thêm vào giỏ…' : 'Thêm vào giỏ hàng' }}</button>
            <RouterLink v-if="knownStock && !soldOut && product.id && validQuantity && !addingToCart" :to="{path:'/checkout',query:{productId:product.id,quantity}}" class="button button-primary product-buy-now-btn"><MarketIcon name="cart"/><span>Đặt mua ngay</span></RouterLink>
            <button v-else class="button button-primary product-buy-now-btn" type="button" disabled>{{ soldOut ? 'Sản phẩm đã hết hàng' : !knownStock ? 'Chưa có thông tin tồn kho' : addingToCart ? 'Đang cập nhật giỏ hàng…' : 'Kiểm tra số lượng mua' }}</button>
            <p v-if="cartMessage" class="product-cart-feedback" role="status">{{ cartMessage }} <RouterLink to="/cart">Xem giỏ hàng</RouterLink></p>
            <p v-if="cartError" class="product-cart-error" role="alert">{{ cartError }}</p>
            <p v-if="soldOut" class="product-detail-sold-out-note">Sản phẩm hiện đã hết hàng. Bạn có thể khám phá những sản phẩm khác.</p>
            <a v-if="sameStoreProducts.length" class="product-detail-discover" href="#product-related" @click.prevent="focusSection('product-related-title')">
              <MarketIcon name="leaf"/><span>Xem sản phẩm cùng gian hàng</span><MarketIcon name="chevron"/>
            </a>
            <RouterLink v-else-if="soldOut || !storeLink" class="product-detail-discover" :to="storeLink ? storeLink+'/products' : '/products'">
              <MarketIcon name="leaf"/><span>{{ storeLink?'Xem sản phẩm cùng gian hàng':'Khám phá sản phẩm khác' }}</span><MarketIcon name="chevron"/>
            </RouterLink>
            <ReportForm v-if="product.id" :key="product.id" target-type="PRODUCT" :target-id="product.id"/>
          </div>
          <section class="product-detail-store" aria-label="Gian hàng bán sản phẩm">
            <div class="product-detail-store-identity">
              <CatalogImage :alt="product.store?.name || 'Gian hàng'" :src="storeLogo" avatar/>
              <div><h2>{{ product.store?.name || 'Chưa có thông tin gian hàng' }}</h2><p v-if="product.store?.description">{{ product.store.description }}</p></div>
            </div>
            <RouterLink v-if="storeLink" :to="storeLink" class="button button-outline">Xem gian hàng<MarketIcon name="arrow"/></RouterLink>
          </section>
          <MessageSellerButton v-if="storeLink" :store-id="product.store.id"/>
        </div>
      </section>
      <nav class="product-content-navigation" aria-label="Tìm hiểu trước khi mua"><a href="#product-description" @click.prevent="information?.openDescription()">Mô tả & thông số</a><a href="#product-reviews" @click.prevent="focusSection('reviews-title')">Đánh giá từ người mua</a><a href="#product-questions" @click.prevent="focusSection('questions-title')">Hỏi người bán</a></nav>
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
import MessageSellerButton from '../../../components/marketplace/MessageSellerButton.vue';
import {computed, ref, watch, onMounted, nextTick} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {useAuth} from '../../../composables/useAuth.js';
import {marketplaceRequest} from '../../../services/marketplace.js';
import {syncCartCount, MAX_CART_QUANTITY} from '../../../services/cart.js';
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
const currentRoute=useRoute(),router=useRouter(),{isAuthenticated}=useAuth();
async function focusReviewAnchor(){
  if(currentRoute.hash!=='#product-reviews')return;
  await nextTick();
  const heading=document.getElementById('reviews-title');
  if(heading){heading.setAttribute('tabindex','-1');heading.focus({preventScroll:true});heading.scrollIntoView({block:'start',behavior:'instant'});}
}
onMounted(focusReviewAnchor);
watch(()=>currentRoute.hash,focusReviewAnchor);
const addingToCart=ref(false),cartMessage=ref(''),cartError=ref('');
const quantity=ref(1),quantityInput=ref(null);
async function addToCart(){
  if(addingToCart.value)return;
  if(!validQuantity.value){quantityInput.value?.focus();return;}
  if(!isAuthenticated.value){await router.push({path:'/login',query:{redirect:currentRoute.fullPath}});return;}
  addingToCart.value=true;cartMessage.value='';cartError.value='';
  try{
    const count=quantity.value;
    const cart=await marketplaceRequest('/account/cart/items','POST',{productId:props.product.id,quantity:count});
    syncCartCount(cart);
    cartMessage.value='Đã thêm '+count+' sản phẩm vào giỏ hàng.';
  }catch(error){cartError.value=error.message;}
  finally{addingToCart.value=false;}
}
const imageLoaded=ref(false), imageFailed=ref(false), imageDialog=ref(null), zoomTrigger=ref(null);
const information=ref(null);
const imageUnavailable=computed(()=>!usableImage(props.product.image)||imageFailed.value);
const knownStock=computed(()=>Number.isInteger(props.product.stock)&&props.product.stock>=0);
const soldOut=computed(()=>knownStock.value&&props.product.stock===0);
const maximumQuantity=computed(()=>knownStock.value?Math.min(props.product.stock,MAX_CART_QUANTITY):0);
const validQuantity=computed(()=>Number.isSafeInteger(quantity.value)&&quantity.value>=1&&quantity.value<=maximumQuantity.value);
const careHighlights=computed(()=>{
  const s=props.product.specifications||{},items=[];
  if(typeof s.heightCm==='number')items.push({label:'Chiều cao cây',value:s.heightCm+' cm'});
  if(typeof s.potDiameterCm==='number')items.push({label:'Đường kính chậu',value:s.potDiameterCm+' cm'});
  if(s.potMaterial)items.push({label:'Chất liệu chậu',value:s.potMaterial});
  const light={INDIRECT:'Ánh sáng gián tiếp',PARTIAL_SHADE:'Bóng râm một phần',SUN:'Nắng trực tiếp'}[s.light];
  if(light)items.push({label:'Ánh sáng',value:light});
  if(s.watering && s.watering.length<=160)items.push({label:'Tưới nước',value:s.watering});
  if(s.condition)items.push({label:'Tình trạng sản phẩm',value:s.condition});
  return items;
});
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
watch(()=>props.product.id,()=>{quantity.value=1;cartMessage.value='';cartError.value='';});
function focusSection(id){
  const heading=document.getElementById(id);
  if(!heading)return;
  heading.setAttribute('tabindex','-1');
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
.product-care-highlights {display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:var(--bm-space-4);margin:var(--bm-space-6) 0;padding:var(--bm-space-4);background:var(--bm-soft);border-radius:var(--bm-radius);}
.product-care-highlights dt {font-size:var(--bm-type-label);color:var(--bm-muted);margin-bottom:var(--bm-space-1);}
.product-care-highlights dd {margin:0;line-height:1.6;white-space:pre-wrap;overflow-wrap:anywhere;}
.product-purchase-quantity {margin-bottom:var(--bm-space-4);}
.product-purchase-quantity>label {display:block;font-weight:600;margin-bottom:var(--bm-space-2);}
.product-purchase-quantity>div {display:grid;grid-template-columns:44px 80px 44px;width:fit-content;gap:var(--bm-space-2);}
.product-purchase-quantity button {border:1px solid var(--bm-control-border);border-radius:var(--bm-radius-control);background:var(--bm-surface);color:var(--bm-forest);font:inherit;font-size:20px;}
.product-purchase-quantity input {width:100%;text-align:center;}
.product-purchase-quantity p {margin:var(--bm-space-2) 0 0;font-size:var(--bm-type-label);line-height:1.6;}
.product-purchase-quantity p:not(.product-cart-error) {color:var(--bm-muted);}
.product-content-navigation {display:flex;flex-wrap:wrap;gap:var(--bm-space-3) var(--bm-space-6);padding-block:var(--bm-space-4);border-block:1px solid var(--bm-border);}
.product-content-navigation a {display:inline-flex;align-items:center;min-height:var(--bm-control-height);color:var(--bm-forest);font-weight:600;text-underline-offset:4px;}
@media(max-width:600px){.product-care-highlights {grid-template-columns:minmax(0,1fr);}}
.product-add-cart {width:100%;margin-bottom:12px;min-height:48px;}
.product-cart-feedback,.product-cart-error {line-height:1.6;margin:0 0 16px;}
.product-cart-feedback {color:var(--bm-forest);}
.product-cart-error {color:var(--bm-error);}
.product-cart-feedback a {text-decoration:underline;text-underline-offset:4px;}
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

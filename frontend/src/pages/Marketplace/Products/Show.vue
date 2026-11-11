<template>
  <MarketplaceLayout>
    <main class="bm-product-detail">
      <nav class="product-detail-breadcrumbs" aria-label="Đường dẫn điều hướng">
        <RouterLink to="/">Trang chủ</RouterLink><MarketIcon name="chevron"/>
        <template v-if="storeLink"><RouterLink :to="storeLink">{{ product.store.name }}</RouterLink><MarketIcon name="chevron"/><RouterLink :to="storeLink+'/products'">Sản phẩm</RouterLink><MarketIcon name="chevron"/></template>
        <template v-else><RouterLink to="/products">Sản phẩm</RouterLink><MarketIcon name="chevron"/></template>
        <span aria-current="page">{{ product.name }}</span>
      </nav>
      <section class="product-detail-hero" aria-labelledby="product-title">
        <ProductImageGallery v-if="product.id" :key="product.id" class="product-detail-media" detail :product-id="product.id" :name="product.name" :legacy-url="product.image"/>
        <div class="product-detail-info">
          <header>
            <p v-if="product.category" class="product-detail-category">{{ product.category }}</p>
            <h1 id="product-title">{{ product.name }}</h1>
            <div class="product-price-line"><p class="product-detail-price">{{ money(product.price) }}</p>
            <p class="product-detail-availability" :class="{'is-sold-out':soldOut,'is-unknown':!knownStock}">
              <MarketIcon :name="knownStock? soldOut?'box':'check':'info'"/>
              {{ knownStock ? soldOut?'Hết hàng':'Còn hàng' : 'Chưa có thông tin tồn kho' }}
            </p></div>
          </header>
          <p v-if="description" class="product-detail-preview">{{ longDescription ? descriptionPreview+'…' : description }} <a href="#product-description" @click.prevent="information?.openDescription()">Đọc mô tả đầy đủ</a></p>
          <p v-else class="product-detail-preview product-detail-muted">Gian hàng chưa bổ sung mô tả cho sản phẩm này.</p>
          <dl v-if="careHighlights.length" class="product-care-highlights" aria-label="Thông tin do người bán cung cấp"><div v-for="item in careHighlights.slice(0,3)" :key="item.label"><MarketIcon name="leaf"/><div><dt>{{ item.label }}</dt><dd>{{ item.value }}</dd></div></div></dl>
          <div class="product-detail-actions">
            <div v-if="knownStock && !soldOut" class="product-purchase-quantity"><label for="product-quantity">Số lượng mua</label><div><button type="button" aria-label="Giảm số lượng mua" :disabled="addingToCart || !validQuantity || quantity<=1" @click="quantity=Math.max(1,quantity-1)">−</button><input id="product-quantity" ref="quantityInput" v-model.number="quantity" type="number" inputmode="numeric" min="1" :max="maximumQuantity" step="1" :disabled="addingToCart" :aria-invalid="!validQuantity" aria-describedby="product-quantity-help"/><button type="button" aria-label="Tăng số lượng mua" :disabled="addingToCart || !validQuantity || quantity>=maximumQuantity" @click="quantity=Math.min(maximumQuantity,quantity+1)">+</button></div><p id="product-quantity-help" :class="{'product-cart-error':!validQuantity}">{{ validQuantity ? 'Giá và tồn kho được kiểm tra lại khi đặt hàng.' : 'Nhập số nguyên từ 1 đến '+maximumQuantity+'.' }}</p></div>
            <RouterLink v-if="knownStock && !soldOut && product.id && validQuantity && !addingToCart" :to="{path:'/checkout',query:{productId:product.id,quantity}}" class="button button-primary product-buy-now-btn"><MarketIcon name="cart"/><span>Mua ngay</span></RouterLink>
            <button v-else class="button button-primary product-buy-now-btn" type="button" disabled>{{ soldOut ? 'Sản phẩm đã hết hàng' : !knownStock ? 'Chưa có thông tin tồn kho' : addingToCart ? 'Đang cập nhật giỏ hàng…' : 'Kiểm tra số lượng mua' }}</button>
            <button v-if="knownStock && !soldOut && product.id" class="button button-outline product-add-cart" type="button" :disabled="addingToCart || !validQuantity || cartOutcomeUnknown" :aria-busy="addingToCart" @click="addToCart"><MarketIcon :name="cartMessage ? 'check' : 'cart'"/>{{ addingToCart ? 'Đang thêm vào giỏ…' : 'Thêm vào giỏ hàng' }}</button>
            <p v-if="cartMessage" class="product-cart-feedback" role="status">{{ cartMessage }} <RouterLink to="/cart">Xem giỏ hàng</RouterLink></p>
            <p v-if="cartError" class="product-cart-error" role="alert">{{ cartError }} <RouterLink v-if="cartOutcomeUnknown" to="/cart">Kiểm tra giỏ hàng</RouterLink></p>
            <p v-if="soldOut" class="product-detail-sold-out-note">Sản phẩm hiện đã hết hàng. Bạn có thể khám phá những sản phẩm khác.</p>
            <RouterLink class="product-detail-discover" :to="storeLink ? storeLink+'/products' : '/products'"><MarketIcon name="store"/><span>{{ storeLink ? 'Xem thêm sản phẩm của gian hàng' : 'Khám phá sản phẩm khác' }}</span><MarketIcon name="chevron"/></RouterLink>
          </div>
          <section class="product-detail-store" aria-label="Gian hàng bán sản phẩm">
            <div class="product-detail-store-identity">
              <CatalogImage :alt="product.store?.name || 'Gian hàng'" :src="storeLogo" avatar/>
              <div><h2>{{ product.store?.name || 'Chưa có thông tin gian hàng' }}</h2><p v-if="product.store?.description">{{ product.store.description }}</p></div>
            </div>
            <RouterLink v-if="storeLink" :to="storeLink" class="button button-outline">Xem gian hàng<MarketIcon name="arrow"/></RouterLink>
          </section>
          <div class="product-detail-secondary"><MessageSellerButton v-if="storeLink" :store-id="product.store.id"/><ReportForm v-if="product.id" :key="product.id" target-type="PRODUCT" :target-id="product.id"/></div>
        </div>
      </section>
      <nav class="product-content-navigation" aria-label="Thông tin sản phẩm"><a v-for="section in sections" :key="section.id" :href="'#'+section.id" :aria-current="activeSection===section.id?'location':undefined" @click.prevent="navigateSection(section)"><MarketIcon :name="section.icon"/>{{ section.label }}</a></nav>
      <ProductInformationTabs ref="information" :product="product"/>
      <ProductReviews v-if="product.id" :key="product.id" :product-id="product.id"/>
      <ProductQuestions v-if="product.id" :key="product.id" :product-id="product.id"/>
      <ProductSuggestions v-if="product.id" :key="product.id" :product-id="product.id" :category-id="product.categoryId" detail/>
      <section v-if="sameStoreProducts.length" id="product-related" class="product-detail-related" aria-labelledby="product-related-title">
        <div class="product-detail-section-heading">
          <div><h2 id="product-related-title" tabindex="-1">Sản phẩm khác từ cùng gian hàng</h2><p v-if="product.store?.name">Khám phá thêm từ {{ product.store.name }}.</p></div>
          <RouterLink :to="storeLink+'/products'" class="text-link">Xem tất cả<MarketIcon name="arrow"/></RouterLink>
        </div>
        <div class="product-detail-related-grid"><ProductCard v-for="p in sameStoreProducts" :key="p.id" :product="p" streamlined actual-only/></div>
      </section>
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
import ProductImageGallery from '../../../components/marketplace/ProductImageGallery.vue';
import ProductCard from '../../../components/ProductCard.vue';
import ReportForm from '../../../components/marketplace/ReportForm.vue';
import MarketIcon from '../../../components/marketplace/MarketIcon.vue';
import ProductInformationTabs from '../../../components/marketplace/ProductInformationTabs.vue';
import ProductReviews from '../../../components/marketplace/ProductReviews.vue';
import ProductQuestions from '../../../components/marketplace/ProductQuestions.vue';
import ProductSuggestions from '../../../components/marketplace/ProductSuggestions.vue';
import {usableImage} from '../../../services/catalog.js';
import {potPurchaseHighlights} from '../../../services/potSpecifications.js';

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
  if(addingToCart.value||cartOutcomeUnknown.value)return;
  if(!validQuantity.value){quantityInput.value?.focus();return;}
  if(!isAuthenticated.value){await router.push({path:'/login',query:{redirect:currentRoute.fullPath}});return;}
  addingToCart.value=true;cartMessage.value='';cartError.value='';
  try{
    const count=quantity.value;
    const cart=await marketplaceRequest('/account/cart/items','POST',{productId:props.product.id,quantity:count});
    syncCartCount(cart);
    cartMessage.value='Đã thêm '+count+' sản phẩm vào giỏ hàng.';
  }catch(error){const unknown=!error.status||error.status>=500;cartError.value=error.message+(unknown?' Kết quả thêm chưa rõ; mở giỏ hàng để kiểm tra trước khi thêm lần nữa.':'');if(unknown)cartOutcomeUnknown.value=true;}
  finally{addingToCart.value=false;}
}
const cartOutcomeUnknown=ref(false);
const information=ref(null),activeSection=ref('product-description');
const sections=[{id:'product-description',heading:'product-description-title',label:'Mô tả chi tiết',icon:'list'},{id:'product-specifications',heading:'product-specifications-title',label:'Thông số kỹ thuật',icon:'info'},{id:'product-reviews',heading:'reviews-title',label:'Đánh giá',icon:'star'},{id:'product-questions',heading:'questions-title',label:'Hỏi đáp',icon:'chat'}];
function navigateSection(section){activeSection.value=section.id;focusSection(section.heading);}
const knownStock=computed(()=>Number.isInteger(props.product.stock)&&props.product.stock>=0);
const soldOut=computed(()=>knownStock.value&&props.product.stock===0);
const maximumQuantity=computed(()=>knownStock.value?Math.min(props.product.stock,MAX_CART_QUANTITY):0);
const validQuantity=computed(()=>Number.isSafeInteger(quantity.value)&&quantity.value>=1&&quantity.value<=maximumQuantity.value);
const careHighlights=computed(()=>{
  if(props.product.potSpecs)return potPurchaseHighlights(props.product.potSpecs);
  const s=props.product.specifications||{},items=[];
  if(!props.product.potCategory&&typeof s.heightCm==='number')items.push({label:'Chiều cao cây',value:s.heightCm+' cm'});
  if(typeof s.potDiameterCm==='number')items.push({label:'Đường kính chậu',value:s.potDiameterCm+' cm'});
  if(s.potMaterial)items.push({label:'Chất liệu chậu',value:s.potMaterial});
  const light={INDIRECT:'Ánh sáng gián tiếp',PARTIAL_SHADE:'Bóng râm một phần',SUN:'Nắng trực tiếp'}[s.light];
  if(!props.product.potCategory&&light)items.push({label:'Ánh sáng',value:light});
  if(!props.product.potCategory&&s.watering && s.watering.length<=160)items.push({label:'Tưới nước',value:s.watering});
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
watch(()=>props.product.id,()=>{quantity.value=1;cartMessage.value='';cartError.value='';});
function focusSection(id){
  const heading=document.getElementById(id);
  if(!heading)return;
  heading.setAttribute('tabindex','-1');
  heading.focus({preventScroll:true});
  heading.scrollIntoView({block:'start',behavior:'instant'});
}
</script>

<style scoped>
.bm-product-detail{max-width:1360px;margin:auto;padding:28px 40px 48px;color:var(--bm-ink);font-family:var(--bm-font);font-size:15px;line-height:1.6}
.bm-product-detail :deep(*){box-sizing:border-box}.bm-product-detail :deep(h2){color:var(--bm-forest);font-size:21px;line-height:1.35;font-weight:700}.bm-product-detail :deep(h2[id]){scroll-margin-top:200px}.bm-product-detail :deep(button),.bm-product-detail :deep(a){-webkit-tap-highlight-color:transparent}.bm-product-detail :deep(:focus-visible){outline:3px solid var(--bm-forest);outline-offset:4px}.bm-product-detail :deep(.button){border-radius:10px;min-height:44px}.bm-product-detail :deep(.muted){color:var(--bm-muted)}
.product-detail-breadcrumbs{display:flex;flex-wrap:wrap;align-items:center;gap:10px;margin-bottom:32px;font-size:13px;color:var(--bm-muted)}.product-detail-breadcrumbs a{color:var(--bm-forest);text-decoration:none}.product-detail-breadcrumbs svg{width:14px;height:14px}
.product-detail-hero{display:grid;grid-template-columns:minmax(0,1.12fr) minmax(0,1fr);gap:56px;align-items:start}.product-detail-info{min-width:0}.product-detail-category{display:table;margin:0 0 6px;background:var(--bm-soft);color:var(--bm-forest);font-size:13px;font-weight:700;padding:2px 12px;border-radius:7px}.product-detail-info h1{margin:0 0 12px;font-size:36px;font-weight:750;line-height:1.2;letter-spacing:-.7px;overflow-wrap:anywhere}.product-price-line{display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap}.product-detail-price{font-size:32px;font-weight:750;line-height:1.3;color:var(--bm-forest);margin:0}.product-detail-availability{display:inline-flex;align-items:center;gap:6px;background:var(--bm-soft);border-radius:9px;padding:5px 12px;font-weight:650;font-size:13px;margin:0}.product-detail-availability svg{width:16px;height:16px}.product-detail-availability.is-sold-out,.product-detail-availability.is-unknown{color:var(--bm-muted);background:var(--bm-bg)}
.product-detail-preview{margin:18px 0;line-height:1.65}.product-detail-preview a{font-size:13px;color:var(--bm-forest);white-space:nowrap}.product-detail-muted{color:var(--bm-muted)}.product-care-highlights{display:flex;flex-wrap:wrap;gap:16px;margin:20px 0}.product-care-highlights>div{display:flex;align-items:start;gap:8px;flex:1;min-width:120px;font-size:12px}.product-care-highlights svg{color:var(--bm-forest);width:20px}.product-care-highlights dt{color:var(--bm-muted)}.product-care-highlights dd{margin:0;font-weight:600;overflow-wrap:anywhere}
.product-purchase-quantity{display:flex;align-items:center;flex-wrap:wrap;gap:12px 24px;margin:24px 0 16px}.product-purchase-quantity label{font-weight:650}.product-purchase-quantity>div{display:grid;grid-template-columns:40px 60px 40px;gap:0}.product-purchase-quantity button,.product-purchase-quantity input{height:38px;min-height:38px;border:1px solid var(--bm-control-border);border-radius:0;padding:0;background:white;color:var(--bm-forest);text-align:center;font:inherit;width:100%}.product-purchase-quantity button:first-child{border-radius:6px 0 0 6px}.product-purchase-quantity button:last-child{border-radius:0 6px 6px 0}.product-purchase-quantity input{border-inline:0;appearance:textfield}.product-purchase-quantity input::-webkit-inner-spin-button{appearance:none}.product-purchase-quantity p{flex-basis:100%;margin:0;color:var(--bm-muted);font-size:12px}.product-buy-now-btn,.product-add-cart{display:flex;width:100%;align-items:center;justify-content:center;gap:10px;margin-bottom:12px;min-height:50px!important;font-size:16px;font-weight:700;text-decoration:none}.product-buy-now-btn{background:var(--bm-forest);color:#fff}.product-add-cart{border:1px solid var(--bm-forest);color:var(--bm-forest);background:white}.product-detail-discover{display:flex;align-items:center;gap:12px;padding:12px 16px;border:1px solid #d6e1ce;background:#f5f8f0;border-radius:12px;color:var(--bm-forest);font-weight:650;text-decoration:none;margin:16px 0}.product-detail-discover svg:last-child{margin-left:auto;width:18px}
.product-detail-store{display:flex;align-items:center;gap:16px;padding:20px;border:1px solid var(--bm-border);border-radius:16px;box-shadow:0 2px 4px #103b2510}.product-detail-store-identity{display:flex;align-items:center;gap:14px;flex:1;min-width:0}.product-detail-store-identity :deep(.catalog-image){width:58px;height:58px;flex-shrink:0;border-radius:50%;background:var(--bm-soft)}.product-detail-store h2{margin:0 0 5px;font-size:17px;color:var(--bm-ink)}.product-detail-store p{margin:0;font-size:12px;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}.product-detail-store>.button{font-size:12px;white-space:nowrap;padding:7px 12px}.product-detail-store>.button svg{display:none}.product-detail-secondary{display:flex;justify-content:space-between;align-items:start;gap:12px;margin-top:14px;flex-wrap:wrap}.product-detail-secondary :deep(.report-form){font-size:12px}.product-cart-feedback{color:var(--bm-forest)}.product-cart-error{color:var(--bm-error)}
.product-content-navigation{display:flex;gap:8px;padding:5px;border:1px solid var(--bm-border);border-radius:16px;width:fit-content;margin:50px 0 20px;background:white;max-width:100%;overflow-x:auto}.product-content-navigation a{display:flex;gap:12px;align-items:center;justify-content:center;padding:10px 20px;min-height:44px;text-decoration:none;color:var(--bm-muted);font-weight:600;white-space:nowrap;border-radius:10px}.product-content-navigation a[aria-current]{background:var(--bm-forest);color:white}.product-content-navigation svg{width:20px}
.bm-product-detail :deep(.discussion),.bm-product-detail :deep(.bm-product-detail-card){width:100%;max-width:none;border:1px solid var(--bm-border);border-radius:18px;padding:24px;margin:20px 0;background:white;box-shadow:0 2px 5px #103b2505}.bm-product-detail :deep(.discussion>header h2){margin:0 0 10px}.bm-product-detail :deep(.discussion>header>.muted){font-size:12px;margin:0 0 12px}.bm-product-detail :deep(.discussion-entry){border:1px solid var(--bm-border);border-radius:16px;padding:20px;margin:0;min-width:0}.bm-product-detail :deep(.review-cards){display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:20px;margin-top:20px}.bm-product-detail :deep(.review-avatar){display:grid;place-items:center;float:left;margin-right:12px;width:44px;height:44px;border-radius:50%;background:var(--bm-soft);color:var(--bm-forest);font-weight:700}.bm-product-detail :deep(.review-rating .is-filled){color:#d6a215}.bm-product-detail :deep(.review-overview){grid-template-columns:minmax(0,1fr) minmax(0,1.5fr);gap:40px;padding:12px 0}.bm-product-detail :deep(.review-summary strong){font-size:36px}.bm-product-detail :deep(.discussion-meta){font-size:12px}.bm-product-detail :deep(.discussion-text){font-size:14px;line-height:1.65;overflow-wrap:anywhere}.bm-product-detail :deep(.discussion-entry time){font-size:12px;color:var(--bm-muted)}.bm-product-detail :deep(.qa-layout){display:grid;grid-template-columns:minmax(0,1.1fr) minmax(0,1fr);gap:32px;margin-top:16px;align-items:start}.bm-product-detail :deep(.qa-list .discussion-entry){border:0;border-radius:0;padding:12px 0}.bm-product-detail :deep(.seller-answer){border:0;border-radius:14px;background:#f1f6ec;padding:16px;margin-top:16px}.bm-product-detail :deep(.qa-form){padding:20px;border:1px solid var(--bm-border);border-radius:16px}.bm-product-detail :deep(.qa-form form){margin:0}.bm-product-detail :deep(.qa-form textarea){min-height:100px}
.product-detail-section-heading{display:flex;align-items:center;justify-content:space-between;gap:16px;margin:28px 0 18px}.product-detail-section-heading h2{margin:0}.product-detail-section-heading p{margin:4px 0;font-size:13px;color:var(--bm-muted)}.product-detail-section-heading .text-link{font-size:13px;color:var(--bm-forest);white-space:nowrap}.product-detail-related-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:24px}.bm-product-detail :deep(.suggestions-grid){gap:24px}.bm-product-detail :deep(.product-card){border-radius:14px;overflow:hidden;box-shadow:0 2px 4px #103b2508}.bm-product-detail :deep(.product-card h3){font-size:15px}.bm-product-detail :deep(.product-card .market-product-photo>.catalog-image){aspect-ratio:1.65!important}.bm-product-detail :deep(.product-card .market-product-price){font-size:19px}.bm-product-detail :deep(.product-suggestions .section-heading){display:flex;justify-content:space-between;gap:16px;align-items:center}.bm-product-detail :deep(.product-suggestions .text-link){font-size:13px;color:var(--bm-forest)}.bm-product-detail :deep(.product-suggestions .section-heading p){font-size:13px}
@media(min-width:1440px){.bm-product-detail{padding-inline:24px}}@media(max-width:1100px){.bm-product-detail{padding-inline:28px}.product-detail-hero{gap:32px}.product-detail-info h1{font-size:30px}.product-detail-store{flex-wrap:wrap}.product-content-navigation a{padding-inline:14px}}@media(max-width:767px){.bm-product-detail{padding:20px 16px 32px}.product-detail-breadcrumbs{margin-bottom:20px}.product-detail-hero{grid-template-columns:minmax(0,1fr);gap:24px}.product-detail-info h1{font-size:30px}.product-detail-price{font-size:28px}.product-detail-store{flex-wrap:nowrap}.product-content-navigation{margin-top:30px;width:100%;border-radius:12px}.product-content-navigation a{padding:8px 12px;font-size:13px;gap:6px}.bm-product-detail :deep(.discussion),.bm-product-detail :deep(.bm-product-detail-card){padding:18px;margin-block:16px}.bm-product-detail :deep(.review-cards),.bm-product-detail :deep(.qa-layout){grid-template-columns:minmax(0,1fr);gap:20px}.bm-product-detail :deep(.review-overview){grid-template-columns:minmax(0,1fr);gap:20px}.product-detail-related-grid{grid-template-columns:repeat(2,minmax(0,1fr));gap:12px}.bm-product-detail :deep(.suggestions-grid){gap:12px}.product-detail-section-heading{align-items:start}.product-detail-section-heading h2{font-size:18px}.product-detail-store{padding:16px;gap:10px}.product-detail-store-identity{gap:10px}.product-detail-store-identity :deep(.catalog-image){width:44px;height:44px}.product-detail-store h2{font-size:15px}.product-detail-store>.button{padding:6px 8px}.product-detail-secondary{font-size:13px}}
@media(prefers-reduced-motion:reduce){.bm-product-detail :deep(*){scroll-behavior:auto;transition:none;animation:none}}
</style>

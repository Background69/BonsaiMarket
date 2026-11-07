<template>
  <figure class="product-image-gallery" aria-label="Ảnh sản phẩm" @keydown.left.prevent="step(-1)" @keydown.right.prevent="step(1)">
    <button ref="mainButton" type="button" class="gallery-main" :disabled="!loaded" :aria-label="'Phóng to '+name+', ảnh '+(index+1)" aria-haspopup="dialog" @click="openLightbox" @touchstart.passive="touchStart" @touchend.passive="touchEnd" @touchcancel="touch=null">
      <CatalogImage :key="current?.imageUrl||'missing'" :src="usableImage(current?.imageUrl)" :alt="name+(current?' — ảnh '+(index+1):'')" ratio="1" fit="contain" priority @load="loaded=true" @error="loaded=false"/>
      <span v-if="loaded" class="gallery-zoom"><MarketIcon name="search"/>Phóng to ảnh</span>
    </button>
    <nav v-if="images.length>1" class="gallery-navigation" aria-label="Chuyển ảnh sản phẩm"><button type="button" class="button button-outline" @click="step(-1)">Ảnh trước</button><span role="status">{{ index+1 }} / {{ images.length }}</span><button type="button" class="button button-outline" @click="step(1)">Ảnh sau</button></nav>
    <div v-if="images.length>1" class="gallery-thumbnails" role="group" aria-label="Chọn góc ảnh sản phẩm">
      <button v-for="(image,position) in images" :key="image.id||image.imageUrl" type="button" :aria-label="'Xem ảnh '+(position+1)+(image.primary?', ảnh bìa':'')" :aria-pressed="index===position" @click="select(position)"><CatalogImage :src="usableImage(image.thumbnailUrl||image.imageUrl)" :alt="'Góc ảnh '+(position+1)+' của '+name" compact fit="cover"/></button>
    </div>
    <figcaption v-if="loading" class="gallery-caption" role="status">Đang tải danh sách ảnh…</figcaption><figcaption v-else class="gallery-caption">{{ images.length?'Ảnh sản phẩm từ gian hàng.':'Gian hàng chưa cung cấp ảnh có thể hiển thị.' }}</figcaption>
    <div v-if="error" class="gallery-load-error" role="status"><p>{{ error }} Ảnh bìa hiện có vẫn được giữ nếu khả dụng.</p><button type="button" class="button button-outline" :disabled="loading" @click="load">Tải lại gallery</button></div>
    <ProductImageLightbox ref="lightbox" :images="images" :name="name"/>
  </figure>
</template>
<script setup>
import {computed,onBeforeUnmount,ref,watch} from 'vue';import CatalogImage from './CatalogImage.vue';import MarketIcon from './MarketIcon.vue';import ProductImageLightbox from './ProductImageLightbox.vue';import {loadProductGallery} from '../../services/productGallery.js';import {usableImage} from '../../services/catalog.js';
const props=defineProps({productId:{type:Number,required:true},name:{type:String,required:true},legacyUrl:String});
const result=ref(null),index=ref(0),loading=ref(false),error=ref(''),loaded=ref(false),mainButton=ref(null),lightbox=ref(null);let generation=0,touch=null,suppressClickUntil=0;
const images=computed(()=>result.value?result.value.images:usableImage(props.legacyUrl)?[{id:null,imageUrl:props.legacyUrl,primary:true}]:[]),current=computed(()=>images.value[index.value]);
async function load(){const ticket=++generation,id=props.productId;loading.value=true;error.value='';try{const data=await loadProductGallery(id);if(ticket===generation){const selected=current.value?.id;result.value=data;const position=data.images.findIndex(image=>image.id===selected);index.value=position>=0?position:Math.max(0,data.images.findIndex(image=>image.primary));}}catch(cause){if(ticket===generation)error.value=cause.message||'Chưa tải được danh sách ảnh.';}finally{if(ticket===generation)loading.value=false;}}
function select(position){index.value=position;}
function step(delta){if(images.value.length>1)select((index.value+delta+images.value.length)%images.value.length);}
function touchStart(event){touch=event.touches.length===1&&(window.visualViewport?.scale??1)<=1?{x:event.touches[0].clientX,y:event.touches[0].clientY}:null;}
function touchEnd(event){if(!touch||event.touches.length||(window.visualViewport?.scale??1)>1){touch=null;return;}const point=event.changedTouches[0],dx=point.clientX-touch.x,dy=point.clientY-touch.y;touch=null;if(Math.abs(dx)>60&&Math.abs(dx)>Math.abs(dy)*1.5){suppressClickUntil=Date.now()+400;step(dx<0?1:-1);}}
function openLightbox(){if(loaded.value&&Date.now()>=suppressClickUntil)lightbox.value.open(index.value,mainButton.value);}
watch(()=>current.value?.imageUrl,()=>loaded.value=false);
watch(()=>props.productId,()=>{generation++;result.value=null;index.value=0;load();},{immediate:true});
onBeforeUnmount(()=>generation++);
</script>
<style scoped>
.product-image-gallery{min-width:0;margin:0;align-self:start}.gallery-main{display:block;position:relative;width:100%;min-width:0;border:1px solid var(--bm-border);border-radius:var(--bm-radius);padding:0;background:var(--bm-surface);overflow:hidden;cursor:zoom-in;touch-action:pan-y pinch-zoom}.gallery-main:disabled{cursor:default;opacity:1}.gallery-main>.catalog-image{width:100%}.gallery-zoom{position:absolute;right:12px;bottom:12px;display:flex;align-items:center;gap:8px;background:var(--bm-surface);color:var(--bm-forest);padding:10px 12px;border:1px solid var(--bm-border);border-radius:var(--bm-radius-control);font:600 var(--bm-type-label) var(--bm-font)}.gallery-zoom svg{width:18px;height:18px}.gallery-navigation{display:flex;justify-content:space-between;align-items:center;gap:8px;margin-block:12px}.gallery-navigation span{white-space:nowrap;font-variant-numeric:tabular-nums}.gallery-thumbnails{display:flex;gap:10px;max-width:100%;overflow-x:auto;padding:4px;overscroll-behavior-inline:contain}.gallery-thumbnails>button{width:72px;min-width:72px;min-height:72px;padding:3px;border:2px solid transparent;border-radius:var(--bm-radius-control);background:var(--bm-surface);cursor:pointer}.gallery-thumbnails>button[aria-pressed=true]{border-color:var(--bm-forest)}.gallery-caption{color:var(--bm-muted);font-size:var(--bm-type-label);line-height:1.6;margin-top:12px}.gallery-load-error{font-size:var(--bm-type-label);line-height:1.6}.product-image-gallery button:focus-visible{outline:var(--bm-focus-width) solid var(--bm-focus);outline-offset:var(--bm-focus-offset)}@media(max-width:600px){.gallery-thumbnails>button{width:64px;min-width:64px;min-height:64px}.gallery-navigation .button{padding-inline:12px}}@media(prefers-reduced-motion:reduce){.gallery-thumbnails{scroll-behavior:auto}}
</style>

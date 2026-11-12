<template>
  <section v-if="!compact" class="seller-gallery-manager" aria-label="Quản lý ảnh sản phẩm">
    <h2 id="product-image-heading" tabindex="-1">Hình ảnh sản phẩm</h2><p>{{ ready?gallery.images.length+' / 8 ảnh đã lưu':'Chưa tải được số ảnh hiện tại' }} · Chọn ảnh rõ sản phẩm, đặt một ảnh làm bìa.</p>
    <div v-if="ready&&gallery.images.length" class="bm-seller-gallery-strip"><button v-for="(image,index) in gallery.images" :key="image.id" type="button" :aria-label="'Xem ảnh '+(index+1)+' của '+name" @click="preview(index,$event.currentTarget)"><CatalogImage :src="usableImage(image.thumbnailUrl||image.imageUrl)" :alt="name+' — ảnh '+(index+1)" fit="contain"/><span v-if="image.primary">Ảnh bìa</span></button></div>
    <button type="button" class="button button-outline" @click="open($event.currentTarget)">{{ ready&&gallery.images.length?'Quản lý và tải thêm ảnh':'Thêm hình ảnh sản phẩm' }}</button>
  </section>
  <SellerImageUploadDialog ref="dialog" :name="name" :count="gallery.images.length" :remaining="remaining" :max-bytes="gallery.maxBytes" :busy="busy" :loading="loading" :ready="ready" :error="error" :message="message" :pending-count="pending.length+queue.filter(item=>item.status==='UPLOADING').length" :uploading="uploading" :overall="overall" @files="addFiles" @upload="uploadAll()" @reload="load(true)" @clear-uploaded="clearUploaded" @closed="$emit('closed')">
    <template #queue><ImageUploadQueue :items="queue" :busy="busy" @retry="uploadAll" @remove="removeQueue"/></template>
    <template #gallery><SellerGalleryGrid v-if="ready" ref="grid" :images="gallery.images" :name="name" :disabled="busy||!ready" @move="moveImage" @primary="primaryImage" @remove="removeImage" @preview="preview"/></template>
  </SellerImageUploadDialog>
  <ProductImageLightbox ref="lightbox" :images="gallery.images" :name="name"/>
</template>
<script setup>
import {onMounted,onBeforeUnmount,ref,watch} from 'vue';import {onBeforeRouteLeave,onBeforeRouteUpdate} from 'vue-router';import SellerImageUploadDialog from './SellerImageUploadDialog.vue';import ImageUploadQueue from './ImageUploadQueue.vue';import SellerGalleryGrid from './SellerGalleryGrid.vue';import ProductImageLightbox from './ProductImageLightbox.vue';import {useSellerGallery} from '../../composables/useSellerGallery.js';
import CatalogImage from './CatalogImage.vue';import {usableImage} from '../../services/catalog.js';
const props=defineProps({productId:{type:Number,required:true},name:{type:String,default:'Sản phẩm'},compact:Boolean,autoOpen:Boolean});const emit=defineEmits(['updated','closed','state']);
const {gallery,loading,busy,uploading,error,message,queue,ready,pending,remaining,overall,hasDraft,load,addFiles,removeQueue,clearUploaded,uploadAll,move,primary,remove,canDiscard}=useSellerGallery(props.productId,result=>emit('updated',result));
const dialog=ref(null),lightbox=ref(null),grid=ref(null);
watch([busy,hasDraft],()=>emit('state',{busy:busy.value,draft:hasDraft.value}),{immediate:true});
async function open(trigger){await dialog.value.open(trigger);if(!busy.value)await load(true);}
function preview(index,trigger){lightbox.value.open(index,trigger);}
async function moveImage(from,to){const id=gallery.value.images[from]?.id;if(await move(from,to))await grid.value?.focusImage(id);}
async function primaryImage(image){if(await primary(image))await grid.value?.focusImage(image.id);}
async function removeImage(image){const position=gallery.value.images.findIndex(item=>item.id===image.id);if(await remove(image)){const next=gallery.value.images[Math.min(position,gallery.value.images.length-1)];if(next)await grid.value?.focusImage(next.id);else dialog.value?.focusPicker();}}
function leave(to){return ['login','forbidden'].includes(to.name)||canDiscard();}
function unload(event){if(busy.value||hasDraft.value){event.preventDefault();event.returnValue='';}}
onBeforeRouteLeave(leave);onBeforeRouteUpdate(leave);
onMounted(()=>{window.addEventListener('beforeunload',unload);props.autoOpen?open():load(false);});onBeforeUnmount(()=>window.removeEventListener('beforeunload',unload));
defineExpose({open,canDiscard});
</script>
<style scoped>.seller-gallery-manager{margin-block:var(--bm-space-6);padding:var(--bm-space-5);border:1px solid var(--bm-border);border-radius:var(--bm-radius);min-width:0}.seller-gallery-manager h2{font-size:var(--bm-type-subheading);margin:0}.seller-gallery-manager p{line-height:1.6;color:var(--bm-muted)}.seller-gallery-manager button{min-height:44px}.bm-seller-gallery-strip{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:12px;margin-bottom:20px}.bm-seller-gallery-strip>button{padding:0;min-width:0;position:relative;border:1px solid var(--bm-border);border-radius:var(--bm-radius-control);background:var(--bm-soft);overflow:hidden;cursor:zoom-in}.bm-seller-gallery-strip span{position:absolute;bottom:4px;left:4px;background:var(--bm-surface);color:var(--bm-forest);font-size:13px;font-weight:600;padding:4px 8px;border-radius:var(--bm-radius-control)}.bm-seller-gallery-strip .catalog-image{transition:opacity var(--bm-motion-fast) var(--bm-motion-ease)}@media(max-width:600px){.bm-seller-gallery-strip{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(prefers-reduced-motion:reduce){.bm-seller-gallery-strip .catalog-image{transition:none}}</style>

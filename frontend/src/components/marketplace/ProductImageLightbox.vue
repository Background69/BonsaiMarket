<template>
  <dialog ref="dialog" class="image-lightbox bm-motion-dialog" :aria-labelledby="id+'-title'" @close="closed" @keydown.tab="trapDialogFocus" @keydown.left.stop.prevent="step(-1)" @keydown.right.stop.prevent="step(1)">
    <header><h2 :id="id+'-title'">{{ name }} <span v-if="images.length">· {{ index+1 }} / {{ images.length }}</span></h2><button type="button" class="button button-outline" autofocus @click="dialog.close()">Đóng ảnh</button></header>
    <div class="image-lightbox-stage" @touchstart.passive="touchStart" @touchend.passive="touchEnd" @touchcancel="touch=null">
      <CatalogImage v-if="opened&&current" :key="current.imageUrl" :src="usableImage(current.imageUrl)" :alt="name+' — ảnh '+(index+1)" ratio="auto" fit="contain" priority/>
    </div>
    <nav v-if="images.length>1" aria-label="Chuyển ảnh phóng to"><button type="button" class="button button-outline" @click="step(-1)">Ảnh trước</button><p role="status">Ảnh {{ index+1 }} / {{ images.length }}</p><button type="button" class="button button-outline" @click="step(1)">Ảnh sau</button></nav>
    <p class="image-lightbox-help">Dùng phím mũi tên hoặc vuốt ngang để đổi ảnh. Escape để đóng; có thể phóng to bằng thao tác của thiết bị.</p>
  </dialog>
</template>
<script setup>
import {computed,nextTick,onBeforeUnmount,ref,useId,watch} from 'vue';import CatalogImage from './CatalogImage.vue';import {usableImage} from '../../services/catalog.js';import {trapDialogFocus} from '../../utils/dialogFocus.js';
import {lockBodyScroll} from '../../utils/bodyScrollLock.js';
const props=defineProps({images:{type:Array,default:()=>[]},name:{type:String,default:'Sản phẩm'}});
const dialog=ref(null),index=ref(0),opened=ref(false),id=useId(),current=computed(()=>props.images[index.value]);let opener,unlock,touch=null;
async function open(at=0,trigger){if(!props.images.length)return;index.value=Math.max(0,Math.min(at,props.images.length-1));opener=trigger||document.activeElement;opened.value=true;await nextTick();if(!dialog.value.open){unlock=lockBodyScroll();dialog.value.showModal();}}
function closed(){opened.value=false;unlock?.();unlock=undefined;if(opener?.isConnected)opener.focus({preventScroll:true});}
function step(delta){if(props.images.length>1)index.value=(index.value+delta+props.images.length)%props.images.length;}
function touchStart(event){touch=event.touches.length===1&&(window.visualViewport?.scale??1)<=1?{x:event.touches[0].clientX,y:event.touches[0].clientY}:null;}
function touchEnd(event){if(!touch||event.touches.length||(window.visualViewport?.scale??1)>1){touch=null;return;}const point=event.changedTouches[0],dx=point.clientX-touch.x,dy=point.clientY-touch.y;touch=null;if(Math.abs(dx)>60&&Math.abs(dx)>Math.abs(dy)*1.5)step(dx<0?1:-1);}
watch(()=>props.images.length,length=>{if(index.value>=length)index.value=Math.max(0,length-1);if(!length&&dialog.value?.open)dialog.value.close();});
onBeforeUnmount(()=>unlock?.());
defineExpose({open});
</script>
<style scoped>
.image-lightbox{width:min(1100px,calc(100% - 24px));max-width:calc(100% - 24px);max-height:94dvh;padding:var(--bm-space-5);border:1px solid var(--bm-border);border-radius:var(--bm-radius);background:var(--bm-surface);color:var(--bm-ink);overflow:auto;box-sizing:border-box;overscroll-behavior:contain}.image-lightbox::backdrop{background:rgb(15 30 22 / .82)}.image-lightbox header{display:flex;align-items:start;justify-content:space-between;gap:var(--bm-space-4)}.image-lightbox h2{min-width:0;margin:0;font-size:var(--bm-type-subheading);overflow-wrap:anywhere}.image-lightbox header button{flex-shrink:0}.image-lightbox-stage{height:min(65dvh,720px);min-height:200px;touch-action:pan-y pinch-zoom;margin-block:var(--bm-space-4)}.image-lightbox-stage>.catalog-image{height:100%;width:100%}.image-lightbox-stage :deep(img){width:100%;height:100%;object-fit:contain}.image-lightbox nav{display:flex;align-items:center;justify-content:space-between;gap:var(--bm-space-3)}.image-lightbox-help{color:var(--bm-muted);font-size:var(--bm-type-label);line-height:1.6}.image-lightbox button{min-height:44px}.image-lightbox button:focus-visible{outline:var(--bm-focus-width) solid var(--bm-focus);outline-offset:var(--bm-focus-offset)}@media(max-width:600px){.image-lightbox{padding:var(--bm-space-3)}.image-lightbox header{flex-wrap:wrap}.image-lightbox-stage{height:55dvh}.image-lightbox h2{font-size:var(--bm-type-body)}}@media(prefers-reduced-motion:reduce){.image-lightbox{scroll-behavior:auto}}
</style>

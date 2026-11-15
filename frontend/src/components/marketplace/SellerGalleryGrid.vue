<template>
  <section class="seller-gallery-grid" aria-label="Ảnh đã lưu trong gallery">
    <h3>Ảnh đã lưu · {{ images.length }} / 8</h3><p class="muted">Kéo ảnh để sắp xếp hoặc dùng nút Lên/Xuống. Đặt ảnh bìa giữ nguyên thứ tự.</p>
    <p v-if="!images.length" class="muted">Chưa có ảnh. Ảnh đầu tiên tải thành công sẽ làm ảnh bìa.</p>
    <ol><li v-for="(image,index) in images" :key="image.id" :ref="element=>remember(image.id,element)" :draggable="!disabled" :class="{'gallery-is-dragging':dragged===image.id}" @dragstart="start($event,image.id)" @dragend="dragged=null" @dragover.prevent="dragOver" @drop.stop.prevent="drop($event,index)">
      <span v-if="image.primary" class="seller-gallery-cover">Ảnh bìa</span>
      <button type="button" class="seller-gallery-preview" :disabled="!loaded.has(image.id)" :aria-label="'Phóng to ảnh '+(index+1)+' của '+name" @click="$emit('preview',index,$event.currentTarget)"><CatalogImage :src="usableImage(image.thumbnailUrl||image.imageUrl)" :alt="name+' — ảnh '+(index+1)" fit="contain" @load="mark(image.id,true)" @error="mark(image.id,false)"/></button>
      <p>Ảnh {{ index+1 }}<span v-if="image.width&&image.height"> · {{ image.width }} × {{ image.height }} px</span></p>
      <div class="seller-gallery-actions"><button type="button" class="button button-outline" :disabled="disabled||index===0" :aria-label="'Đưa ảnh '+(index+1)+' lên trước'" @click="$emit('move',index,index-1)">Lên</button><button type="button" class="button button-outline" :disabled="disabled||index===images.length-1" :aria-label="'Đưa ảnh '+(index+1)+' xuống sau'" @click="$emit('move',index,index+1)">Xuống</button><button v-if="!image.primary" type="button" class="button button-outline" :disabled="disabled" @click="$emit('primary',image)">Đặt ảnh bìa</button><button type="button" class="button button-outline" :disabled="disabled" :aria-label="'Xóa ảnh '+(index+1)" @click="$emit('remove',image)">Xóa ảnh</button></div>
    </li></ol>
  </section>
</template>
<script setup>
import {nextTick,ref} from 'vue';import CatalogImage from './CatalogImage.vue';import {usableImage} from '../../services/catalog.js';
const props=defineProps({images:{type:Array,default:()=>[]},name:String,disabled:Boolean}),emit=defineEmits(['move','primary','remove','preview']);const dragged=ref(null),loaded=ref(new Set());
const elements=new Map();
function remember(id,element){if(element)elements.set(id,element);else elements.delete(id);}
async function focusImage(id){await nextTick();const element=elements.get(id);(element?.querySelector('.seller-gallery-actions button:not(:disabled)')||element?.querySelector('button:not(:disabled)'))?.focus({preventScroll:true});}
function mark(id,value){const next=new Set(loaded.value);value?next.add(id):next.delete(id);loaded.value=next;}
function start(event,id){if(props.disabled){event.preventDefault();return;}dragged.value=id;event.dataTransfer.effectAllowed='move';event.dataTransfer.setData('application/x-bonsai-gallery',String(id));}
function dragOver(event){if(dragged.value!=null)event.dataTransfer.dropEffect='move';}
function drop(event,index){if(props.disabled)return;const id=Number(event.dataTransfer.getData('application/x-bonsai-gallery')),from=props.images.findIndex(image=>image.id===id);dragged.value=null;if(from>=0&&from!==index)emit('move',from,index);}
defineExpose({focusImage});
</script>
<style scoped>
.seller-gallery-grid{margin-top:24px;border-top:1px solid var(--bm-border);padding-top:20px}.seller-gallery-grid ol{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:16px;padding:0;list-style:none}.seller-gallery-grid li{position:relative;min-width:0;padding:12px;border:1px solid var(--bm-border);border-radius:var(--bm-radius);background:var(--bm-surface)}.seller-gallery-grid li.gallery-is-dragging{opacity:.55;border-style:dashed}.seller-gallery-cover{display:inline-flex;padding:4px 8px;background:var(--bm-soft);color:var(--bm-forest);border-radius:var(--bm-radius-control);font-size:13px;font-weight:700;margin-bottom:8px}.seller-gallery-preview{display:block;width:100%;padding:0;border:0;background:transparent;border-radius:var(--bm-radius-control);cursor:zoom-in}.seller-gallery-preview:disabled{cursor:default}.seller-gallery-grid p{font-size:13px;line-height:1.6}.seller-gallery-actions{display:grid;grid-template-columns:1fr 1fr;gap:8px}.seller-gallery-actions button{min-height:44px;padding:8px;font-size:13px}.seller-gallery-actions button:nth-child(n+3){grid-column:1/-1}.seller-gallery-grid button:focus-visible{outline:3px solid var(--bm-focus);outline-offset:3px}@media(max-width:800px){.seller-gallery-grid ol{grid-template-columns:repeat(3,minmax(0,1fr))}}@media(max-width:600px){.seller-gallery-grid ol{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:380px){.seller-gallery-grid ol{grid-template-columns:minmax(0,1fr)}}
</style>

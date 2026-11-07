<template>
  <div class="seller-text">
    <template v-for="(block,index) in visible" :key="index">
      <component :is="block.level===2?'h2':'h3'" v-if="block.kind==='heading'"><SellerTextInline :text="block.text" :formatted="formatted"/></component>
      <component :is="block.kind==='ordered'?'ol':'ul'" v-else-if="block.items"><li v-for="(item,i) in block.items" :key="i"><SellerTextInline :text="item" :formatted="formatted"/></li></component>
      <p v-else><SellerTextInline :text="block.text" :formatted="formatted"/></p>
    </template>
    <button v-if="collapsible&&long" class="button button-outline seller-text-toggle" type="button" :aria-expanded="expanded" @click="toggle">{{ expanded?'Thu gọn':'Xem thêm' }}</button>
  </div>
</template>
<script setup>
import {computed,ref,watch} from 'vue';
import {sellerTextBlocks} from '../../services/sellerText.js';import SellerTextInline from './SellerTextInline.vue';
const props=defineProps({text:{type:String,default:''},format:{type:String,default:'PLAIN'},collapsible:Boolean});
const formatted=computed(()=>props.format==='MARKDOWN');
const expanded=ref(false);
watch(()=>props.text,()=>expanded.value=false);
// Cú pháp văn bản tối giản; interpolation escape nội dung người bán, không thực thi HTML.
const blocks=computed(()=>sellerTextBlocks(props.text,formatted.value));
const long=computed(()=>props.text.length>1200&&blocks.value.length>3);
const cutoff=computed(()=>{
  let length=0,index=0;
  for(const block of blocks.value){if(length>=650&&blocks.value[index-1]?.kind!=='heading')break;length+=(block.text||block.items?.join('')||'').length;index++;}
  // Không để tiêu đề mục tách khỏi đoạn nội dung.
  return Math.max(1,index);
});
const visible=computed(()=>props.collapsible&&long.value&&!expanded.value?blocks.value.slice(0,cutoff.value):blocks.value);
function toggle(event){expanded.value=!expanded.value;if(!expanded.value)event.currentTarget.closest('.seller-text').scrollIntoView({block:'start',behavior:'instant'});}
</script>
<style scoped>
.seller-text{overflow-wrap:anywhere;line-height:1.75}.seller-text p{white-space:pre-line;margin-block:0 1em}.seller-text :is(h2,h3){font-size:var(--bm-type-subheading);margin-block:1.5em .75em}.seller-text :is(ul,ol){padding-inline-start:1.5em;margin-block:0 1em}.seller-text li+li{margin-top:.4em}.seller-text :deep(a){color:var(--bm-forest);text-decoration:underline;text-underline-offset:3px}.seller-text :deep(a:focus-visible){outline:var(--bm-focus-width) solid var(--bm-focus);outline-offset:var(--bm-focus-offset)}
</style>

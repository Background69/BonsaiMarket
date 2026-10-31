<template>
  <div class="seller-text">
    <template v-for="(block,index) in visible" :key="index">
      <h3 v-if="block.kind==='heading'">{{ block.text }}</h3>
      <ul v-else-if="block.kind==='list'"><li v-for="(item,i) in block.items" :key="i">{{ item }}</li></ul>
      <p v-else>{{ block.text }}</p>
    </template>
    <button v-if="collapsible&&long" class="button button-outline seller-text-toggle" type="button" :aria-expanded="expanded" @click="toggle">{{ expanded?'Thu gọn':'Xem thêm' }}</button>
  </div>
</template>
<script setup>
import {computed,ref,watch} from 'vue';
const props=defineProps({text:{type:String,default:''},collapsible:Boolean});
const expanded=ref(false);
watch(()=>props.text,()=>expanded.value=false);
// Cú pháp văn bản tối giản; interpolation escape nội dung người bán, không thực thi HTML.
const blocks=computed(()=>{
  const result=[];
  for(const paragraph of props.text.replace(/\r\n?/g,'\n').split(/\n\s*\n/).filter(p=>p.trim())) {
    const lines=paragraph.split('\n');let pending=[];
    const flush=()=>{if(pending.length)result.push({kind:'paragraph',text:pending.join('\n')});pending=[];};
    for(const line of lines){
      const heading=line.match(/^\s*(?:#{1,3}\s+(.+)|\*\*(.+)\*\*)\s*$/);
      if(heading){flush();result.push({kind:'heading',text:heading[1]||heading[2]});}
      else if(/^\s*[-*]\s+/.test(line)){flush();const text=line.replace(/^\s*[-*]\s+/,'');const last=result.at(-1);if(last?.kind==='list')last.items.push(text);else result.push({kind:'list',items:[text]});}
      else pending.push(line);
    }flush();
  }return result;
});
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

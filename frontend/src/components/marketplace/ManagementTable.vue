<template>
  <div ref="region" class="management-table-region" :class="{'management-table-region--wide':columns.length>=5}" :style="{'--management-table-min-width':Math.max(720,columns.length*150)+'px'}" :aria-busy="loading" role="region" :aria-label="caption" :aria-describedby="scrollable?id+'-scroll-help':undefined" :tabindex="scrollable ? 0 : undefined">
    <p v-if="scrollable" :id="id+'-scroll-help'" class="sr-only">Bảng có thể cuộn ngang. Dùng phím mũi tên khi vùng bảng được chọn để xem các cột và thao tác.</p>
    <p v-if="loading" role="status">Đang tải danh sách…</p>
    <div v-else-if="error" class="management-error" role="alert">
      <p>{{ error }}</p><button type="button" class="button button-outline" @click="$emit('retry')">Thử lại</button>
    </div>
    <div v-else-if="!rows.length" class="management-table-empty" role="status"><MarketIcon name="list"/><h2>{{ emptyTitle }}</h2><p>{{ emptyDescription }}</p></div>
    <table v-else class="management-table" role="table">
      <caption class="sr-only">{{ caption }}</caption>
      <thead role="rowgroup"><tr role="row">
        <th v-for="column in columns" :id="id+'-'+column.key" :key="column.key" scope="col" role="columnheader" :class="{'management-number':column.numeric}">{{ column.label }}</th>
      </tr></thead>
      <tbody role="rowgroup"><tr v-for="row in rows" :key="row[rowKey]" class="management-row" role="row">
        <component :is="index===0?'th':'td'" v-for="(column,index) in columns" :key="column.key"
                   :id="index===0?id+'-row-'+row[rowKey]:undefined" :scope="index===0?'row':undefined"
                   :role="index===0?'rowheader':'cell'" :headers="index===0?id+'-'+column.key:id+'-'+column.key+' '+id+'-row-'+row[rowKey]"
                   :class="{'management-number':column.numeric, 'management-row-actions':column.key==='actions'}">
          <span v-if="index>0" class="management-cell-label" aria-hidden="true">{{ column.label }}</span>
          <div class="management-cell-value"><slot :name="column.key" :row="row">{{ row[column.key] ?? '—' }}</slot></div>
        </component>
      </tr></tbody>
    </table>
  </div>
</template>
<script setup>
import {useId,ref,onMounted,onBeforeUnmount,watch,nextTick} from 'vue';
import MarketIcon from './MarketIcon.vue';
const props=defineProps({
  columns: {type:Array,required:true}, rows: {type:Array,default:()=>[]}, rowKey: {type:String,default:'id'},
  caption: {type:String,required:true}, loading:Boolean, error:String,
  emptyTitle: {type:String,default:'Không có dữ liệu phù hợp.'},
  emptyDescription:{type:String,default:'Dữ liệu mới sẽ xuất hiện khi phát sinh. Bạn có thể thay đổi bộ lọc hoặc tải lại danh sách.'},
});
defineEmits(['retry']);
const id=useId();
const region=ref(null),scrollable=ref(false);let observer;
function measure(){const element=region.value;scrollable.value=!!element&&!props.loading&&!props.error&&element.scrollWidth>element.clientWidth+1;}
onMounted(()=>{if(typeof ResizeObserver!=='undefined'){observer=new ResizeObserver(measure);observer.observe(region.value);}measure();});
watch(()=>[props.rows,props.loading,props.error,props.columns.length],async()=>{await nextTick();measure();});
onBeforeUnmount(()=>observer?.disconnect());
</script>

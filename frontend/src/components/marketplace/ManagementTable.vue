<template>
  <div class="management-table-region" :class="{'management-table-region--wide':columns.length>6}" :aria-busy="loading" role="region" :aria-label="caption" :tabindex="columns.length>6 && !loading && !error && rows.length ? 0 : undefined">
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
import {useId} from 'vue';
import MarketIcon from './MarketIcon.vue';
defineProps({
  columns: {type:Array,required:true}, rows: {type:Array,default:()=>[]}, rowKey: {type:String,default:'id'},
  caption: {type:String,required:true}, loading:Boolean, error:String,
  emptyTitle: {type:String,default:'Không có dữ liệu phù hợp.'},
  emptyDescription:{type:String,default:'Dữ liệu mới sẽ xuất hiện khi phát sinh. Bạn có thể thay đổi bộ lọc hoặc tải lại danh sách.'},
});
defineEmits(['retry']);
const id=useId();
</script>

<template>
  <div class="management-table-region" :aria-busy="loading">
    <p v-if="loading" role="status">Đang tải danh sách…</p>
    <div v-else-if="error" class="management-error" role="alert">
      <p>{{ error }}</p><button type="button" class="button button-outline" @click="$emit('retry')">Thử lại</button>
    </div>
    <p v-else-if="!rows.length" class="management-table-empty" role="status">{{ emptyTitle }}</p>
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
defineProps({
  columns: {type:Array,required:true}, rows: {type:Array,default:()=>[]}, rowKey: {type:String,default:'id'},
  caption: {type:String,required:true}, loading:Boolean, error:String,
  emptyTitle: {type:String,default:'Không có dữ liệu phù hợp.'},
});
defineEmits(['retry']);
const id=useId();
</script>

<template>
  <div v-if="!loading" class="management-pagination">
    <p role="status">{{ totalElements.toLocaleString('vi-VN') }} {{ noun }}<span v-if="count"> · Hiển thị {{ page*size+1 }}–{{ page*size+count }}</span></p>
    <CatalogPagination :page="page" :total-pages="totalPages" :busy="busy" label="Phân trang danh sách"/>
  </div>
</template>
<script setup>
import CatalogPagination from './CatalogPagination.vue';
defineProps({page:{type:Number,default:0},size:{type:Number,default:20},totalPages:{type:Number,default:0},totalElements:{type:Number,default:0},count:{type:Number,default:0},noun:{type:String,default:'kết quả'},busy:Boolean,loading:Boolean});
</script>

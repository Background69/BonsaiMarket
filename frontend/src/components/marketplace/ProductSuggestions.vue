<template>
  <section v-if="loading || error || products.length" class="product-suggestions" :class="{'product-suggestions--botanical':botanical}" aria-labelledby="suggestions-title">
    <div class="section-heading"><div><h2 id="suggestions-title">{{ productId ? 'Sản phẩm cùng danh mục' : 'Gợi ý khám phá' }}</h2><p class="muted">{{ productId ? 'Tham khảo sản phẩm còn hàng từ những gian hàng khác, ưu tiên mức giá gần nhau.' : 'Sản phẩm còn hàng từ các gian hàng khác nhau.' }}</p></div></div>
    <ResourceState v-if="loading || error" resource="gợi ý sản phẩm" :loading="loading" :error="error" @retry="load"/>
    <div v-else class="suggestions-grid"><ProductCard v-for="p in products" :key="p.id" :product="p" :botanical="botanical"/></div>
  </section>
</template>
<script setup>
import {ref,watch,onBeforeUnmount} from 'vue';
import api from '../../services/api.js';import {adaptProduct} from '../../services/catalog.js';
import ProductCard from '../ProductCard.vue';import ResourceState from './ResourceState.vue';
const props=defineProps({productId:[Number,String],exclude:{type:Array,default:()=>[]},botanical:Boolean});
const products=ref([]),loading=ref(false),error=ref(false);let generation=0;
async function load(){const ticket=++generation;loading.value=true;error.value=false;products.value=[];try{const path=props.productId?'/products/'+encodeURIComponent(props.productId)+'/recommendations':'/products/recommendations'+(props.exclude.length?'?exclude='+props.exclude.join(','):'');const {data}=await api.get(path);if(ticket===generation)products.value=data.map(adaptProduct);}catch{if(ticket===generation)error.value=true;}finally{if(ticket===generation)loading.value=false;}}
watch(()=>[props.productId,props.exclude.join(',')],load,{immediate:true});onBeforeUnmount(()=>generation++);
</script>
<style scoped>
.product-suggestions{margin-top:40px}.product-suggestions h2{font-size:var(--bm-type-section);margin:0}.suggestions-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:24px;margin-top:20px}@media(max-width:1023px){.suggestions-grid{grid-template-columns:repeat(3,minmax(0,1fr));gap:16px}}@media(max-width:767px){.suggestions-grid{grid-template-columns:repeat(2,minmax(0,1fr));gap:12px}}
</style>

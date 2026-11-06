<template><SellerLayout><div class="management-page"><RouterLink :to="{path:'/seller/products',query:returnQuery}" class="text-link">Về sản phẩm và tồn kho</RouterLink><h1>Nội dung sản phẩm</h1><ProductContentEditor v-if="id" :key="id" :product-id="id"/><p v-else role="alert">Mã sản phẩm không hợp lệ.</p></div></SellerLayout></template>
<script setup>
import {computed} from 'vue';import {useRoute} from 'vue-router';import SellerLayout from '../SellerLayout.vue';import ProductContentEditor from '../../../../components/marketplace/ProductContentEditor.vue';
const route=useRoute(),id=computed(()=>{const n=Number(route.params.id);return Number.isSafeInteger(n)&&n>0?n:null;});
const returnQuery=computed(()=>Object.fromEntries(['page','inventorySearch','inventoryStock','inventoryStatus'].filter(key=>route.query[key]!==undefined).map(key=>[key,route.query[key]])));
</script>

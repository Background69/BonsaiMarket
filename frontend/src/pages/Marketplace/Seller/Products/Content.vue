<template><SellerLayout><div class="management-page"><RouterLink :to="{path:'/seller/products',query:route.query.page?{page:route.query.page}:{}}" class="text-link">Về sản phẩm và tồn kho</RouterLink><h1>Nội dung sản phẩm</h1><ProductContentEditor v-if="id" :key="id" :product-id="id"/><p v-else role="alert">Mã sản phẩm không hợp lệ.</p></div></SellerLayout></template>
<script setup>
import {computed} from 'vue';import {useRoute} from 'vue-router';import SellerLayout from '../SellerLayout.vue';import ProductContentEditor from '../../../../components/marketplace/ProductContentEditor.vue';
const route=useRoute(),id=computed(()=>{const n=Number(route.params.id);return Number.isSafeInteger(n)&&n>0?n:null;});
</script>

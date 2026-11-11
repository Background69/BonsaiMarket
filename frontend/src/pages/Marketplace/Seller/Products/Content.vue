<template><SellerLayout><div class="management-page"><RouterLink :to="{path:'/seller/products',query:returnQuery}" class="text-link">Về sản phẩm và tồn kho</RouterLink><h1>Nội dung sản phẩm</h1><template v-if="id"><RouterLink class="button button-outline" :to="'/seller/products/'+id+'/edit'">Sửa tên, giá và gửi duyệt</RouterLink><SellerGalleryManager :key="'gallery-'+id" :product-id="id" :name="'Sản phẩm #'+id"/><ProductContentEditor :key="id" :product-id="id"/></template><p v-else role="alert">Mã sản phẩm không hợp lệ.</p></div></SellerLayout></template>
<script setup>
import {computed} from 'vue';import {useRoute} from 'vue-router';import SellerLayout from '../SellerLayout.vue';import ProductContentEditor from '../../../../components/marketplace/ProductContentEditor.vue';
import SellerGalleryManager from '../../../../components/marketplace/SellerGalleryManager.vue';
const route=useRoute(),id=computed(()=>{const n=Number(route.params.id);return Number.isSafeInteger(n)&&n>0?n:null;});
const returnQuery=computed(()=>Object.fromEntries(['page','inventorySearch','inventoryStock','inventoryStatus','inventoryCategory','inventorySort'].filter(key=>route.query[key]!==undefined).map(key=>[key,route.query[key]])));
</script>

<template>
  <SellerLayout><div class="management-page">
    <header><h1>Hình ảnh sản phẩm</h1><p>Chọn sản phẩm để xem, thay hoặc xóa ảnh chính.</p></header>
    <div class="management-toolbar"><label>Tìm trong trang này<input v-model="search" type="search" placeholder="Tên sản phẩm…"/></label></div>
    <p class="muted">Tìm trong {{ data.content.length }} sản phẩm của trang {{ data.page+1 }}.</p>
    <ManagementTable caption="Sản phẩm và ảnh chính" row-key="productId" :columns="columns" :rows="visible" :loading="loading" :error="error" empty-title="Không có sản phẩm phù hợp trong trang này." @retry="load">
      <template #name="{row}"><div class="management-product-identity"><CatalogImage class="management-thumbnail" :src="usableImage(row.imageUrl)" :alt="row.name"/><div>{{ row.name }}<small>#{{ row.productId }}</small></div></div></template>
      <template #image="{row}"><span class="management-status">{{ row.imageUrl?'Đã có ảnh':'Chưa có ảnh' }}</span></template>
      <template #actions="{row}"><RouterLink class="button button-outline" :to="{path:route.path,query:{...route.query,productId:row.productId}}" :aria-current="selected?.productId===row.productId?'page':undefined" :aria-label="'Quản lý ảnh '+row.name">Quản lý ảnh</RouterLink></template>
    </ManagementTable>
    <ManagementPagination v-if="!error" :page="data.page" :size="data.size" :total-pages="data.totalPages" :total-elements="data.totalElements" :count="data.content.length" noun="sản phẩm" :loading="loading" :busy="loading"/>
    <ProductImageEditor v-if="selected" :key="selected.productId" :product-id="selected.productId" :name="selected.name" @updated="imageUpdated"/>
    <p v-else-if="route.query.productId&&!loading">Sản phẩm không nằm trong trang đang xem. Chọn sản phẩm từ danh sách.</p>
  </div></SellerLayout>
</template>
<script setup>
import {computed,ref,watch,nextTick} from 'vue';import {useRoute} from 'vue-router';import SellerLayout from './SellerLayout.vue';import ProductImageEditor from '../../../components/marketplace/ProductImageEditor.vue';import ManagementTable from '../../../components/marketplace/ManagementTable.vue';import ManagementPagination from '../../../components/marketplace/ManagementPagination.vue';import CatalogImage from '../../../components/marketplace/CatalogImage.vue';import {usableImage} from '../../../services/catalog.js';import {useServerList} from '../../../composables/useServerList.js';
const columns=[{key:'name',label:'Sản phẩm'},{key:'image',label:'Ảnh chính'},{key:'actions',label:'Thao tác'}],search=ref('');
const route=useRoute(),{data,loading,error,load}=useServerList('/seller/inventory',()=>({size:20}));const selected=computed(()=>data.value.content.find(p=>p.productId===Number(route.query.productId)));
const visible=computed(()=>data.value.content.filter(p=>p.name.toLocaleLowerCase('vi').includes(search.value.trim().toLocaleLowerCase('vi'))));
function imageUpdated(result){data.value.content=data.value.content.map(p=>p.productId===result.productId?{...p,imageUrl:result.imageUrl}:p);}
watch(()=>route.query.productId,async()=>{await nextTick();document.getElementById('product-image-heading')?.focus();});
</script>

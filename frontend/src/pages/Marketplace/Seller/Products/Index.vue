<template>
  <SellerLayout><div class="inventory-page">
    <header class="bm-seller-page-header"><div><h1>Sản phẩm và tồn kho</h1><p>Chăm chút sản phẩm, kiểm tra tồn khả dụng và theo dõi xét duyệt.</p></div><div class="bm-seller-editor-actions"><button class="button button-outline" :disabled="loading || savingCount>0 || editingIds.size>0" @click="load">Tải lại</button><RouterLink class="button button-primary" :to="{path:'/seller/products/new',query:route.query}">Tạo sản phẩm mới</RouterLink></div></header>
    <form class="inventory-tools" @submit.prevent="applyFilters">
      <label>Tìm toàn bộ sản phẩm<input v-model="search" type="search" maxlength="160" placeholder="Tên sản phẩm…" :disabled="editingIds.size>0 || loading"/></label>
      <label>Tình trạng tồn kho<select v-model="stockFilter" :disabled="editingIds.size>0 || loading"><option value="all">Tất cả</option><option value="available">Còn hàng</option><option value="empty">Hết hàng</option><option value="unknown">Chưa xác định</option></select></label>
      <label>Trạng thái sản phẩm<select v-model="statusFilter" :disabled="editingIds.size>0 || loading"><option value="all">Tất cả</option><option v-for="(label,key) in labels" :key="key" :value="key">{{ label }}</option></select></label>
      <label>Danh mục<select v-model="categoryFilter" :disabled="editingIds.size>0 || loading"><option value="">Tất cả</option><option v-for="category in categories" :key="category.id" :value="String(category.id)">{{ category.name }}</option></select></label>
      <label>Sắp xếp<select v-model="sortFilter" :disabled="editingIds.size>0 || loading"><option v-for="(label,key) in sortLabels" :key="key" :value="key">{{ label }}</option></select></label>
      <button class="button button-outline" :disabled="editingIds.size>0 || loading" type="submit">Áp dụng</button><button class="button button-outline" :disabled="editingIds.size>0 || loading" type="button" @click="clearFilters">Xóa bộ lọc</button>
    </form>
    <p v-if="categoryError" role="status">{{ categoryError }} <button class="button button-outline" @click="loadCategories">Tải lại danh mục</button></p>
    <p v-if="stockNotice" class="bm-seller-success" role="status">{{ stockNotice }}</p>
    <h2 id="inventory-results-title" class="inventory-results-title" tabindex="-1">Danh sách tồn kho</h2>
    <p v-if="!loading && !error" class="muted inventory-scope" role="status">{{ data.totalElements }} sản phẩm khớp trên toàn kho · Trang {{ data.page+1 }} · {{ appliedLabels }}.</p>
    <p v-if="loading" class="integration-loading" role="status">Đang tải tồn kho…</p><div v-else-if="error" class="catalog-state" role="alert"><h2>Chưa tải được tồn kho</h2><p>{{ error }}</p><button class="button button-primary" @click="load">Thử lại</button></div>
    <div v-else><p class="bm-seller-help">Tồn kho là số lượng còn bán. Nhãn “Sắp hết” dùng quy ước hiển thị từ 1–5 sản phẩm.</p><SellerInventoryTable v-if="visible.length" :products="visible" :query="route.query" :page="data.page" :busy="savingCount>0||editingIds.size>0" @gallery="openGallery" @updated="update" @saved="stockNotice='Đã lưu tồn kho · '+$event.name+' · '+$event.stock+' sản phẩm còn bán.'" @editing="setEditing" @saving="savingCount+=$event?1:-1"/><div v-else class="catalog-state"><h2>Không có sản phẩm khớp bộ lọc</h2><p>Thử đổi từ khóa hoặc bộ lọc, hoặc tạo bản nháp đầu tiên.</p><button class="button button-outline" @click="clearFilters">Xóa tìm kiếm và bộ lọc</button></div></div><ManagementPagination v-if="!error" :page="data.page" :size="data.size" :total-pages="data.totalPages" :total-elements="data.totalElements" :count="data.content.length" noun="sản phẩm" :loading="loading" :busy="loading || savingCount>0 || editingIds.size>0" focus-target="#inventory-results-title"/>
    <SellerGalleryManager v-if="galleryProduct" ref="galleryManager" :key="galleryProduct.productId" :product-id="galleryProduct.productId" :name="galleryProduct.name" compact auto-open @updated="galleryUpdated"/>
  </div></SellerLayout>
</template>
<script setup>
import {labelFor} from '../../../../services/presentation.js';

import {ref,watch,computed,onMounted,onBeforeUnmount} from 'vue';
import {useRoute,useRouter,onBeforeRouteLeave,onBeforeRouteUpdate} from 'vue-router';
import SellerLayout from '../SellerLayout.vue';
import ManagementPagination from '../../../../components/marketplace/ManagementPagination.vue';
import SellerInventoryTable from '../../../../components/seller/SellerInventoryTable.vue';
import SellerGalleryManager from '../../../../components/marketplace/SellerGalleryManager.vue';
import {marketplaceRequest} from '../../../../services/marketplace.js';
import {useAuth} from '../../../../composables/useAuth.js';
const {user}=useAuth();
const route=useRoute(),router=useRouter(),loading=ref(true),error=ref(''),data=ref({content:[],page:0,totalPages:0}),search=ref(''),stockFilter=ref('all'),statusFilter=ref('all'),savingCount=ref(0),editingIds=ref(new Set());
const labels={ACTIVE:'Đang bán',DRAFT:'Bản nháp',PENDING:'Chờ duyệt',REJECTED:'Bị từ chối',ARCHIVED:'Đã lưu trữ'};
const galleryProduct=ref(null),galleryManager=ref(null),stockNotice=ref('');
function openGallery(product,trigger){if(galleryProduct.value?.productId===product.productId){galleryManager.value?.open(trigger);return;}if(galleryManager.value&&!galleryManager.value.canDiscard())return;galleryProduct.value=product;}
function galleryUpdated(result){data.value.content=data.value.content.map(product=>product.productId===result.productId?{...product,imageUrl:result.imageUrl}:product);}
const categoryFilter=ref(''),sortFilter=ref('id_asc'),categories=ref([]),categoryError=ref('');
const sortLabels={id_asc:'Mã sản phẩm',latest:'Mới cập nhật',name_asc:'Tên A–Z',stock_asc:'Tồn kho tăng dần',stock_desc:'Tồn kho giảm dần'};
const visible=computed(()=>data.value.content);
const appliedLabels=computed(()=>[route.query.inventorySearch&&'Từ khóa: '+route.query.inventorySearch,{'available':'Còn hàng',empty:'Hết hàng',unknown:'Chưa rõ tồn kho'}[route.query.inventoryStock],labels[route.query.inventoryStatus],route.query.inventoryCategory&&'Danh mục #'+route.query.inventoryCategory,sortLabels[route.query.inventorySort||'id_asc']].filter(Boolean).join(' · '));
async function loadCategories(){categoryError.value='';try{categories.value=await marketplaceRequest('/categories');}catch(e){categoryError.value=e.message||'Chưa tải được danh mục.';}}
let requestId=0;
let reconcileAfterSave=false;
async function load(){if(savingCount.value||editingIds.value.size)return;const id=++requestId;loading.value=true;error.value='';try{const params={page:route.query.page||'0',size:'20',search:route.query.inventorySearch||'',stock:route.query.inventoryStock||'all',status:route.query.inventoryStatus||'all',sort:route.query.inventorySort||'id_asc'};if(route.query.inventoryCategory)params.categoryId=route.query.inventoryCategory;const result=await marketplaceRequest('/seller/inventory?'+new URLSearchParams(params));if(id===requestId){const lastPage=Math.max(0,result.totalPages-1);if(Number(route.query.page||0)>lastPage){await router.replace({path:route.path,query:{...route.query,page:String(lastPage)},hash:route.hash});return;}data.value=result;}}catch(e){if(id===requestId)error.value=e.message||'Không thể tải tồn kho. Vui lòng thử lại.';}finally{if(id===requestId)loading.value=false;}}
function applyFilters(){if(editingIds.value.size||savingCount.value)return;router.push({path:route.path,query:{...route.query,page:undefined,inventorySearch:search.value.trim()||undefined,inventoryStock:stockFilter.value==='all'?undefined:stockFilter.value,inventoryStatus:statusFilter.value==='all'?undefined:statusFilter.value,inventoryCategory:categoryFilter.value||undefined,inventorySort:sortFilter.value==='id_asc'?undefined:sortFilter.value}});}
function clearFilters(){search.value='';stockFilter.value='all';statusFilter.value='all';categoryFilter.value='';sortFilter.value='id_asc';applyFilters();}
function update(product){data.value.content=data.value.content.map(p=>p.productId===product.productId?product:p);reconcileAfterSave=true;}
watch([savingCount,()=>editingIds.value.size],()=>{if(reconcileAfterSave&&!savingCount.value&&!editingIds.value.size){reconcileAfterSave=false;load();}});
function setEditing(id,editing){const ids=new Set(editingIds.value);editing?ids.add(id):ids.delete(id);editingIds.value=ids;}
function leave(){if(!user.value)return true;if(savingCount.value)return false;if(editingIds.value.size&&!window.confirm('Rời trang và bỏ thay đổi tồn kho chưa lưu?'))return false;editingIds.value=new Set();return true;}
onBeforeRouteLeave(leave);onBeforeRouteUpdate(leave);
function unload(e){if(editingIds.value.size||savingCount.value){e.preventDefault();e.returnValue='';}}
onMounted(()=>{window.addEventListener('beforeunload',unload);loadCategories();});onBeforeUnmount(()=>{requestId++;window.removeEventListener('beforeunload',unload);});
watch([()=>route.query.inventorySearch,()=>route.query.inventoryStock,()=>route.query.inventoryStatus,()=>route.query.inventoryCategory,()=>route.query.inventorySort],([keyword,stock,status,category,sort])=>{search.value=String(keyword||'');stockFilter.value=['available','empty','unknown'].includes(stock)?stock:'all';statusFilter.value=Object.hasOwn(labels,status)?status:'all';categoryFilter.value=String(category||'');sortFilter.value=Object.hasOwn(sortLabels,sort)?sort:'id_asc';},{immediate:true});
watch(()=>JSON.stringify([route.query.page,route.query.inventorySearch,route.query.inventoryStock,route.query.inventoryStatus,route.query.inventoryCategory,route.query.inventorySort]),load,{immediate:true});
</script>

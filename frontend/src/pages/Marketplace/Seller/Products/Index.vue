<template>
  <SellerLayout><div class="inventory-page">
    <header class="section-heading"><div><h1>Sản phẩm và tồn kho</h1><p>Xem số lượng còn bán và cập nhật từng sản phẩm.</p></div><button class="button button-outline" :disabled="loading || savingCount>0 || editingIds.size>0" @click="load">Tải lại tồn kho</button></header>
    <form class="inventory-tools" @submit.prevent="applyFilters"><label>Tìm trong trang này<input v-model="search" type="search" maxlength="160" placeholder="Tên sản phẩm…" :disabled="editingIds.size>0" @change="applyFilters"/></label><label>Tình trạng tồn kho<select v-model="stockFilter" :disabled="editingIds.size>0" @change="applyFilters"><option value="all">Tất cả</option><option value="available">Còn hàng</option><option value="empty">Hết hàng</option></select></label><label>Trạng thái sản phẩm<select v-model="statusFilter" :disabled="editingIds.size>0" @change="applyFilters"><option value="all">Tất cả</option><option v-for="(label,key) in labels" :key="key" :value="key">{{ label }}</option></select></label><button class="button button-outline" :disabled="editingIds.size>0 || loading" type="submit">Áp dụng</button><button v-if="search || stockFilter!=='all' || statusFilter!=='all'" class="button button-outline" :disabled="editingIds.size>0" type="button" @click="clearFilters">Xóa bộ lọc</button></form>
    <h2 id="inventory-results-title" class="inventory-results-title" tabindex="-1">Danh sách tồn kho</h2>
    <p v-if="!loading && !error" class="muted inventory-scope" role="status">Hiển thị {{ visible.length }}/{{ data.content.length }} sản phẩm trong trang {{ data.page+1 }}. Bộ lọc chỉ áp dụng trong trang đang xem; chuyển trang để xem sản phẩm khác.</p>
    <p v-if="loading" class="integration-loading" role="status">Đang tải tồn kho…</p><div v-else-if="error" class="catalog-state" role="alert"><h2>Chưa tải được tồn kho</h2><p>{{ error }}</p><button class="button button-primary" @click="load">Thử lại</button></div>
    <div v-else class="inventory-list"><div class="inventory-column-head" aria-hidden="true"><span>Sản phẩm</span><span>Trạng thái</span><span>Tồn hiện tại</span><span>Cập nhật</span></div>
      <article v-for="product in visible" :key="product.productId" class="inventory-row" :aria-label="product.name"><div><h3><RouterLink v-if="product.status==='ACTIVE'" :to="'/products/'+product.productId">{{ product.name }}</RouterLink><span v-else>{{ product.name }}</span></h3><RouterLink class="text-link inventory-content-link" :to="{path:'/seller/products/'+product.productId+'/content',query:{...route.query,page:data.page}}">Sửa mô tả và thông số</RouterLink></div><p class="product-status"><span class="mobile-label">Trạng thái: </span>{{ labelFor(labels,product.status,'Trạng thái cần kiểm tra') }}</p><p class="stock-current"><span class="mobile-label">Tồn hiện tại: </span><strong>{{ product.stock }}</strong><span v-if="product.stock===0" class="catalog-sold-out"> · Hết hàng</span></p><InventoryEditor :product="product" :page="data.page" :disabled="loading || (editingIds.size>0&&!editingIds.has(product.productId))" @updated="update" @editing="setEditing(product.productId,$event)" @saving="savingCount+=$event?1:-1"/></article>
      <div v-if="!visible.length" class="catalog-state"><h2>{{ data.content.length?'Không có sản phẩm khớp trong trang này':'Chưa có sản phẩm để quản lý tồn kho' }}</h2><button v-if="data.content.length" class="button button-outline" @click="clearFilters">Xóa tìm kiếm và bộ lọc</button></div>
    </div><ManagementPagination v-if="!error" :page="data.page" :size="data.size" :total-pages="data.totalPages" :total-elements="data.totalElements" :count="data.content.length" noun="sản phẩm" :loading="loading" :busy="loading || savingCount>0 || editingIds.size>0" focus-target="#inventory-results-title"/>
  </div></SellerLayout>
</template>
<script setup>
import {labelFor} from '../../../../services/presentation.js';

import {ref,watch,computed,onMounted,onBeforeUnmount} from 'vue';
import {useRoute,useRouter,onBeforeRouteLeave,onBeforeRouteUpdate} from 'vue-router';
import SellerLayout from '../SellerLayout.vue';
import ManagementPagination from '../../../../components/marketplace/ManagementPagination.vue';
import InventoryEditor from '../../../../components/marketplace/InventoryEditor.vue';
import {marketplaceRequest} from '../../../../services/marketplace.js';
import {useAuth} from '../../../../composables/useAuth.js';
const {user}=useAuth();
const route=useRoute(),router=useRouter(),loading=ref(true),error=ref(''),data=ref({content:[],page:0,totalPages:0}),search=ref(''),stockFilter=ref('all'),statusFilter=ref('all'),savingCount=ref(0),editingIds=ref(new Set());
const labels={ACTIVE:'Đang bán',DRAFT:'Bản nháp',PENDING:'Chờ duyệt',REJECTED:'Bị từ chối',ARCHIVED:'Đã lưu trữ'};
const retained=ref(new Set());
const visible=computed(()=>data.value.content.filter(p=>retained.value.has(p.productId)||((!search.value||p.name.toLocaleLowerCase('vi').includes(search.value.toLocaleLowerCase('vi')))&&(stockFilter.value==='all'||(stockFilter.value==='empty'?p.stock===0:p.stock>0))&&(statusFilter.value==='all'||p.status===statusFilter.value))));
watch([search,stockFilter,statusFilter],()=>retained.value=new Set());
let requestId=0;
async function load(){if(savingCount.value||editingIds.value.size)return;const id=++requestId;loading.value=true;error.value='';try{const result=await marketplaceRequest('/seller/inventory?'+new URLSearchParams({page:route.query.page||'0',size:'20'}));if(id===requestId){const lastPage=Math.max(0,result.totalPages-1);if(Number(route.query.page||0)>lastPage){await router.replace({path:route.path,query:{...route.query,page:String(lastPage)},hash:route.hash});return;}data.value=result;}}catch(e){if(id===requestId)error.value=e.message||'Không thể tải tồn kho. Vui lòng thử lại.';}finally{if(id===requestId)loading.value=false;}}
function applyFilters(){if(editingIds.value.size||savingCount.value)return;router.replace({path:route.path,query:{...route.query,inventorySearch:search.value.trim()||undefined,inventoryStock:stockFilter.value==='all'?undefined:stockFilter.value,inventoryStatus:statusFilter.value==='all'?undefined:statusFilter.value}});}
function clearFilters(){search.value='';stockFilter.value='all';statusFilter.value='all';applyFilters();}
function update(product){retained.value=new Set([...retained.value,product.productId]);data.value.content=data.value.content.map(p=>p.productId===product.productId?product:p);}
function setEditing(id,editing){const ids=new Set(editingIds.value);editing?ids.add(id):ids.delete(id);editingIds.value=ids;}
function leave(){if(!user.value)return true;if(savingCount.value)return false;if(editingIds.value.size&&!window.confirm('Rời trang và bỏ thay đổi tồn kho chưa lưu?'))return false;editingIds.value=new Set();return true;}
onBeforeRouteLeave(leave);onBeforeRouteUpdate(leave);
function unload(e){if(editingIds.value.size||savingCount.value){e.preventDefault();e.returnValue='';}}
onMounted(()=>window.addEventListener('beforeunload',unload));onBeforeUnmount(()=>{requestId++;window.removeEventListener('beforeunload',unload);});
watch([()=>route.query.inventorySearch,()=>route.query.inventoryStock,()=>route.query.inventoryStatus],([keyword,stock,status])=>{search.value=String(keyword||'');stockFilter.value=['available','empty'].includes(stock)?stock:'all';statusFilter.value=Object.hasOwn(labels,status)?status:'all';},{immediate:true});
watch(()=>route.query.page,()=>{retained.value=new Set();load();},{immediate:true});
</script>

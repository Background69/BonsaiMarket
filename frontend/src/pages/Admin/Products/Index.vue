<template>
  <AdminLayout>
    <div class="admin-catalog">
      <header class="workspace-page-heading"><h1>Tra cứu sản phẩm</h1><p>Tìm thông tin sản phẩm công khai, gian hàng, giá và tồn kho.</p></header>
      <form class="admin-catalog-filters" @submit.prevent="apply"><label>Tìm sản phẩm<input v-model="search" aria-label="Tìm sản phẩm trong catalog" maxlength="160" type="search" placeholder="Tên sản phẩm…"/></label><label>Danh mục<select v-model="categoryFilter" aria-label="Lọc danh mục sản phẩm" @change="apply"><option value="">Tất cả danh mục</option><option v-for="c in categories" :key="c.id" :value="c.slug">{{ c.name }}</option></select></label><button class="button button-outline" type="submit">Tìm kiếm</button></form>
      <ResourceState v-if="isLoading || apiError" resource="catalog sản phẩm" :loading="isLoading" :error="apiError" @retry="$emit('retry')"/>
      <template v-else>
        <p class="admin-catalog-count" role="status">{{ products.total }} sản phẩm công khai</p>
        <table v-if="products.data.length" class="admin-catalog-table" role="table">
          <caption class="sr-only">Catalog công khai: sản phẩm, gian hàng, giá và tồn kho. Chỉ xem.</caption>
          <thead role="rowgroup"><tr role="row"><th role="columnheader" scope="col">Sản phẩm</th><th role="columnheader" scope="col">Gian hàng</th><th role="columnheader" scope="col">Giá</th><th role="columnheader" scope="col">Tồn kho</th></tr></thead>
          <tbody role="rowgroup"><tr v-for="p in products.data" :key="p.id" role="row">
            <th scope="row" role="rowheader"><RouterLink :to="'/products/'+p.id">{{ p.name }}</RouterLink><small>ID {{ p.id }} · {{ p.category || 'Chưa có danh mục' }}</small><span class="admin-public-status">{{ visibilityLabel(p.status) }}</span></th>
            <td role="cell"><span class="admin-cell-label" aria-hidden="true">Gian hàng</span><RouterLink v-if="p.store?.id" :to="'/stores/'+p.store.id">{{ p.store.name }}</RouterLink><span v-else>Chưa có thông tin</span></td>
            <td class="admin-number" role="cell"><span class="admin-cell-label" aria-hidden="true">Giá</span>{{ formatPrice(p.price) }}</td>
            <td class="admin-number" role="cell"><span class="admin-cell-label" aria-hidden="true">Tồn kho</span>{{ p.stock ?? 'Chưa có thông tin' }}<span v-if="p.stock===0" class="admin-stock-empty">Hết hàng</span></td>
          </tr></tbody>
        </table>
        <ResourceState v-else resource="sản phẩm" empty-title="Không có sản phẩm phù hợp" message="Thử tìm với từ khóa hoặc danh mục khác."/>
      </template>
      <CatalogPagination :page="products.page" :total-pages="products.totalPages" :busy="isLoading"/>
    </div>
  </AdminLayout>
</template>
<script setup>
import {ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import CatalogPagination from '../../../components/marketplace/CatalogPagination.vue';
import ResourceState from '../../../components/marketplace/ResourceState.vue';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';
defineProps({products:Object,categories:Array,isLoading:Boolean,apiError:Boolean});
defineEmits(['retry']);
const search=ref(''),categoryFilter=ref('');
const route=useRoute(),router=useRouter();
watch(()=>route.query,q=>{search.value=q.search||'';categoryFilter.value=q.category||'';},{immediate:true});
function apply(){const q={...route.query};delete q.page;delete q.categoryId;q.search=search.value;q.category=categoryFilter.value;router.push({path:route.path,query:q});}
const formatPrice=v=>Number.isFinite(v)?new Intl.NumberFormat('vi-VN',{style:'currency',currency:'VND'}).format(v):'Chưa có giá';
const visibilityLabel=s=>({active:'Đang hiển thị công khai',published:'Đang hiển thị công khai',draft:'Bản nháp',archived:'Đã lưu trữ',pending:'Chờ xử lý',pending_review:'Chờ xử lý',rejected:'Không hiển thị'}[s]||s||'Chưa có trạng thái');
</script>
<style scoped>
.admin-catalog-filters {display:flex;align-items:end;flex-wrap:wrap;gap:16px;margin-block:24px;}
.admin-catalog-filters label {display:flex;flex-direction:column;gap:8px;flex:1 1 220px;font-size:14px;font-weight:600;}
.admin-catalog-filters input,.admin-catalog-filters select {width:100%;min-height:44px;border:1px solid var(--bm-control-border);border-radius:var(--bm-radius-control);background:white;color:var(--bm-ink);font:inherit;font-size:16px;padding:10px 12px;}
.admin-catalog-count {font-size:14px;color:var(--bm-muted);margin:0 0 16px;}
.admin-catalog-table {width:100%;border-collapse:collapse;table-layout:fixed;font-size:16px;}
.admin-catalog-table th,.admin-catalog-table td {padding:20px 16px;vertical-align:top;text-align:left;border-bottom:1px solid var(--bm-border);overflow-wrap:anywhere;}
.admin-catalog-table thead th {font-size:14px;font-weight:600;color:var(--bm-muted);}
.admin-catalog-table th:first-child {width:40%;}
.admin-catalog-table th:nth-child(2) {width:26%;}
.admin-catalog-table tbody th {font-size:16px;line-height:1.5;font-weight:600;background:transparent;}
.admin-catalog-table a {color:var(--bm-forest);text-underline-offset:4px;}
.admin-catalog-table small {display:block;font-size:13px;line-height:1.5;font-weight:400;color:var(--bm-muted);margin-top:8px;}
.admin-public-status {display:block;font-size:13px;font-weight:400;color:var(--bm-forest);margin-top:4px;}
.admin-catalog-table .admin-number {font-variant-numeric:tabular-nums;text-align:right;}
.admin-stock-empty {display:block;font-size:14px;color:var(--bm-error);}
.admin-cell-label {display:none;}
@media(max-width:767px){
  .admin-catalog-table,.admin-catalog-table tbody {display:block;}
  .admin-catalog-table thead {position:absolute;width:1px;height:1px;overflow:hidden;clip-path:inset(50%);}
  .admin-catalog-table tbody tr {display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px;padding:24px 0;border-top:1px solid var(--bm-border);}
  .admin-catalog-table tbody th,.admin-catalog-table tbody td {display:block;width:auto;padding:0;border:0;}
  .admin-catalog-table tbody th,.admin-catalog-table tbody td:nth-child(2) {grid-column:1 / -1;}
  .admin-cell-label {display:block;font-size:14px;line-height:1.5;color:var(--bm-muted);margin-bottom:4px;}
  .admin-catalog-table .admin-number {text-align:left;}
}
</style>

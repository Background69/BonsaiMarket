<template>
  <MarketplaceLayout>
    <main class="market-container market-page marketplace-surface">
      <div class="breadcrumbs">
        <RouterLink to="/">Trang chủ</RouterLink>
        <span>/</span><span>Gian hàng</span></div>
      <header class="market-page-heading"><h1>Khám phá gian hàng</h1>
        <p>Mỗi gian hàng có sản phẩm và bản sắc riêng. Tìm người bán phù hợp với bạn.</p></header>
      <form class="store-search-form" @submit.prevent="commitSearch"><label for="store-search">Tìm tên, mô tả, danh mục hoặc khu vực</label>
        <div class="market-search"><input id="store-search" v-model="search" placeholder="Nhập tên hoặc mô tả gian hàng..."
                                          type="search"/>
          <MarketIcon name="search"/>
        </div><button class="button button-outline" type="submit">Tìm gian hàng</button>
      </form>
      <div class="store-category-filter"><label for="store-category">Danh mục đang bán</label>
      <select id="store-category" :value="route.query.categoryId || ''" @change="changeCategory($event.target.value)"><option value="">Tất cả danh mục</option><option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option></select>
      </div>
      <p v-if="!isLoading && !apiError" class="results-count" role="status">{{ stores.total }} gian hàng{{ route.query.search || route.query.categoryId ? ' phù hợp' : '' }}</p>
      <ResourceState v-if="isLoading || apiError" resource="gian hàng" :loading="isLoading" :error="apiError" @retry="$emit('retry')"/>
      <div v-else-if="stores.data.length" class="market-store-grid">
        <StoreCard v-for="store in stores.data" :key="store.id" :store="store"/>
      </div>
      <div v-else-if="!isLoading && !apiError" class="market-empty">
        <MarketIcon name="store"/>
        <h2>{{ hasFilters ? 'Không tìm thấy gian hàng phù hợp' : 'Chưa có gian hàng công khai' }}</h2>
        <p>{{
            hasFilters ? 'Thử từ khóa khác hoặc thay đổi danh mục đang chọn.' : 'Hãy quay lại sau để khám phá những gian hàng mới.'
          }}</p>
        <button v-if="hasFilters" class="button button-outline" @click="clearFilters">Xóa bộ lọc</button>
      </div>
      <CatalogPagination v-if="!apiError" :page="stores.page || 0" :total-pages="stores.totalPages || 0" :busy="isLoading" label="Phân trang gian hàng"/>
    </main>
  </MarketplaceLayout>
</template>
<script setup>
import {computed,ref, watch} from 'vue';
import CatalogPagination from '../../../components/marketplace/CatalogPagination.vue';
import {useRoute, useRouter} from 'vue-router';
import MarketplaceLayout from '../../../layouts/MarketplaceLayout.vue';
import StoreCard from '../../../components/marketplace/StoreCard.vue';
import MarketIcon from '../../../components/marketplace/MarketIcon.vue';
import ResourceState from '../../../components/marketplace/ResourceState.vue';

const props = defineProps({
  stores: {type: Object, default: () => ({data: [], total: 0})},
  isLoading: Boolean,
  apiError: Boolean,
  categories:{type:Array,default:()=>[]}
});
defineEmits(['retry']);
const route = useRoute(), router = useRouter(), search = ref('');
const hasFilters=computed(()=>Boolean(route.query.search||route.query.categoryId));
watch(()=>route.query.search, value=>search.value=String(value || ''), {immediate:true});
function commitSearch(){const query={...route.query};delete query.page;if(search.value.trim())query.search=search.value.trim();else delete query.search;router.push({path:'/stores',query});}
function clearFilters(){search.value='';router.push('/stores');}
function changeCategory(value){const query={...route.query};delete query.page;if(value)query.categoryId=value;else delete query.categoryId;router.push({path:'/stores',query});}
</script>
<style scoped>
.store-category-filter {display:grid;gap:8px;max-width:360px;margin-bottom:24px;}
.store-category-filter select {width:100%;min-height:44px;padding:10px 12px;font:inherit;color:var(--bm-ink);background:var(--bm-surface);border:1px solid var(--bm-control-border);border-radius:var(--bm-radius-control);}
.store-search-form {display:grid;grid-template-columns:minmax(0,1fr) auto;align-items:end;gap:12px;max-width:720px;margin-bottom:24px;}
.store-search-form>label {grid-column:1 / -1;margin:0;font-size:14px;}
.store-search-form .market-search {grid-column:1;grid-row:2;order:0;min-width:0;}
.store-search-form>button {grid-column:2;grid-row:2;}
.store-search-form input {width:100%;border:0;}
@media(max-width:480px){.store-search-form {grid-template-columns:1fr;}.store-search-form>button {grid-column:1;grid-row:3;justify-self:start;}}
</style>

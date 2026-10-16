<template>
  <MarketplaceLayout>
    <main class="market-container market-page marketplace-surface">
      <div class="breadcrumbs">
        <RouterLink to="/">Trang chủ</RouterLink>
        <span>/</span><span>Gian hàng</span></div>
      <header class="market-page-heading"><h1>Khám phá gian hàng</h1>
        <p>Mỗi gian hàng có sản phẩm và bản sắc riêng. Tìm người bán phù hợp với bạn.</p></header>
      <form class="store-search-form" @submit.prevent="commitSearch"><label for="store-search">Tìm theo tên hoặc mô tả gian hàng</label>
        <div class="market-search"><input id="store-search" v-model="search" placeholder="Nhập tên hoặc mô tả gian hàng..."
                                          type="search"/>
          <MarketIcon name="search"/>
        </div><button class="button button-outline" type="submit">Tìm gian hàng</button>
      </form>
      <p v-if="!isLoading && !apiError" class="results-count" role="status">{{ filtered.length }} gian hàng{{ search ? ' phù hợp' : '' }}</p>
      <ResourceState v-if="isLoading || apiError" resource="gian hàng" :loading="isLoading" :error="apiError" @retry="$emit('retry')"/>
      <div v-else-if="filtered.length" class="market-store-grid">
        <StoreCard v-for="store in filtered" :key="store.id" :store="store"/>
      </div>
      <div v-else-if="!isLoading && !apiError" class="market-empty">
        <MarketIcon name="store"/>
        <h2>{{ search ? 'Không tìm thấy gian hàng' : 'Chưa có gian hàng công khai' }}</h2>
        <p>{{
            search ? 'Thử tìm theo tên hoặc từ khóa khác.' : 'Hãy quay lại sau để khám phá những gian hàng mới.'
          }}</p>
        <button v-if="search" class="button button-outline" @click="clearSearch">Xóa từ khóa</button>
      </div>
    </main>
  </MarketplaceLayout>
</template>
<script setup>
import {computed, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import MarketplaceLayout from '../../../layouts/MarketplaceLayout.vue';
import StoreCard from '../../../components/marketplace/StoreCard.vue';
import MarketIcon from '../../../components/marketplace/MarketIcon.vue';
import ResourceState from '../../../components/marketplace/ResourceState.vue';

const props = defineProps({
  stores: {type: Object, default: () => ({data: [], total: 0})},
  isLoading: Boolean,
  apiError: Boolean
});
defineEmits(['retry']);
const route = useRoute(), router = useRouter(), search = ref('');
watch(()=>route.query.search, value=>search.value=String(value || ''), {immediate:true});
function commitSearch(){const query={...route.query};delete query.page;if(search.value.trim())query.search=search.value.trim();else delete query.search;router.push({path:'/stores',query});}
function clearSearch(){search.value='';commitSearch();}
const filtered = computed(() => props.stores.data.filter(s => (s.name + ' ' + (s.description || '')).toLocaleLowerCase('vi').includes(search.value.trim().toLocaleLowerCase('vi'))));
</script>
<style scoped>
.store-search-form {display:grid;grid-template-columns:minmax(0,1fr) auto;align-items:end;gap:12px;max-width:720px;margin-bottom:24px;}
.store-search-form>label {grid-column:1 / -1;margin:0;font-size:14px;}
.store-search-form .market-search {grid-column:1;grid-row:2;order:0;min-width:0;}
.store-search-form>button {grid-column:2;grid-row:2;}
.store-search-form input {width:100%;border:0;}
@media(max-width:480px){.store-search-form {grid-template-columns:1fr;}.store-search-form>button {grid-column:1;grid-row:3;justify-self:start;}}
</style>

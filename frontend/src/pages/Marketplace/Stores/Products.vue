<template>
  <MarketplaceLayout><main class="market-container market-page store-detail marketplace-surface storefront-page">
    <nav class="storefront-breadcrumbs" aria-label="Đường dẫn điều hướng"><RouterLink to="/">Trang chủ</RouterLink><MarketIcon name="chevron"/><template v-if="store.id"><RouterLink :to="'/stores/'+store.id">{{ store.name }}</RouterLink><MarketIcon name="chevron"/></template><span aria-current="page">Sản phẩm</span></nav>
    <StorefrontHeader v-if="store.id" :store="store" compact/>
    <h1 v-else>{{ isLoading?'Đang tải gian hàng…':'Gian hàng chưa khả dụng' }}</h1>
    <ReportForm v-if="store.id" :key="store.id" target-type="STORE" :target-id="store.id"/>
    <section id="store-products" :aria-busy="isLoading"><div class="section-heading"><h2>Sản phẩm của gian hàng</h2><span v-if="!isLoading && !apiError" role="status">{{ products.total }} kết quả</span></div>
      <form v-if="store.id" class="store-product-toolbar" @submit.prevent="apply"><label class="storefront-search">Tìm trong gian hàng<div class="catalog-search-row"><input v-model="search" maxlength="160" type="search" placeholder="Tên sản phẩm…"/><button class="button button-primary" type="submit">Tìm kiếm</button></div></label><label>Danh mục<select v-model="categoryId" @change="apply"><option value="">Tất cả danh mục</option><option v-for="c in categories" :key="c.id" :value="String(c.id)">{{ c.name }}</option></select></label><label>Sắp xếp<select v-model="sort" @change="apply"><option value="default">Mặc định</option><option value="latest">Mới đăng gần đây</option><option value="price_asc">Giá thấp → cao</option><option value="price_desc">Giá cao → thấp</option><option value="name_asc">Tên A → Z</option></select></label><label class="checkbox-label"><input v-model="inStock" type="checkbox" @change="apply"/>Chỉ sản phẩm còn hàng</label></form>
      <p v-if="route.query.categoryId&&!categories.some(c=>String(c.id)===String(route.query.categoryId))&&!isLoading" class="muted">Danh mục này không còn công khai. Bạn có thể xóa bộ lọc để xem các sản phẩm khác.</p>
      <div v-if="filtered" class="catalog-active-filters"><span v-if="route.query.search || route.query.q">Từ khóa: {{ route.query.search || route.query.q }}</span><span v-if="categoryId">Danh mục: {{ categories.find(c=>String(c.id)===categoryId)?.name||categoryId }}</span><span v-if="inStock">Còn hàng</span><button class="text-link" @click="clear">Xóa bộ lọc và từ khóa</button></div>
      <div v-if="!isLoading && !apiError && products.data.length" class="market-product-grid"><ProductCard v-for="product in products.data" :key="product.id" :product="product" streamlined/></div>
      <CatalogState v-else :loading="isLoading" :error="apiError" :message="errorMessage" :filtered="filtered" @retry="$emit('retry')" @clear="clear"/>
      <CatalogPagination v-if="!apiError && !isLoading && products.data.length" :page="products.page" :total-pages="products.totalPages"/>
    </section>
  </main></MarketplaceLayout>
</template>
<script setup>
import {ref,watch,computed} from 'vue';
import {useRoute,useRouter} from 'vue-router';
import MarketplaceLayout from '../../../layouts/MarketplaceLayout.vue';
import ProductCard from '../../../components/ProductCard.vue';
import CatalogImage from '../../../components/marketplace/CatalogImage.vue';
import CatalogPagination from '../../../components/marketplace/CatalogPagination.vue';
import CatalogState from '../../../components/marketplace/CatalogState.vue';
import ReportForm from '../../../components/marketplace/ReportForm.vue';
import StorefrontHeader from '../../../components/marketplace/StorefrontHeader.vue';import MarketIcon from '../../../components/marketplace/MarketIcon.vue';
defineProps({store:{type:Object,default:()=>({})},products:{type:Object,default:()=>({data:[]})},categories:{type:Array,default:()=>[]},isLoading:Boolean,apiError:Boolean,errorMessage:String});defineEmits(['retry']);
const route=useRoute(),router=useRouter(),search=ref(''),sort=ref('default'),inStock=ref(false),categoryId=ref('');
watch(()=>route.query,q=>{search.value=q.search||q.q||'';sort.value=q.sort||'default';inStock.value=['true','1'].includes(q.in_stock);categoryId.value=String(q.categoryId||'');},{immediate:true});
const filtered=computed(()=>!!(route.query.search||route.query.q||route.query.in_stock||route.query.category||route.query.categoryId||route.query.min_price||route.query.max_price));
function apply(){const query={...route.query};delete query.page;delete query.q;delete query.category;const term=search.value.trim().replace(/\s+/g,' ');if(term)query.search=term;else delete query.search;if(categoryId.value)query.categoryId=categoryId.value;else delete query.categoryId;if(sort.value==='default')delete query.sort;else query.sort=sort.value;if(inStock.value)query.in_stock='1';else delete query.in_stock;router.push({path:route.path,query});}
function clear(){router.push({path:route.path,query:{...(route.query.size?{size:route.query.size}:{}),...(route.query.sort?{sort:route.query.sort}:{})}});}
</script>

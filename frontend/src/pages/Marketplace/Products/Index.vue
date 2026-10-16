<template>
  <MarketplaceLayout :categories="categories">
    <main class="products-page market-container market-page marketplace-surface">
      <header class="products-page-heading"><h1>{{ filters.search ? 'Kết quả cho “'+filters.search+'”' : 'Khám phá sản phẩm' }}</h1><p>Cây cảnh, bonsai, chậu và phụ kiện từ các gian hàng.</p></header>
      <div class="products-layout">
        <button ref="filterTrigger" aria-controls="market-filters" aria-haspopup="dialog" :aria-expanded="filterOpen" class="products-filter-toggle" @click="openFilters"><MarketIcon name="filter"/>Bộ lọc<span v-if="activeFilterTags.length"> ({{ activeFilterTags.length }})</span></button>
        <dialog ref="filterDialog" aria-labelledby="filter-dialog-title" class="filter-dialog" @keydown.tab="trapFilterFocus" @close="filterOpen=false; syncQuery(currentRoute.query); filterTrigger?.focus()">
          <aside id="market-filters" class="products-sidebar"><div class="products-filter-panel">
            <div class="products-filter-header"><h2 id="filter-dialog-title">Bộ lọc</h2><button class="mobile-filter-close button button-outline" @click="filterDialog.close()">Đóng</button></div>
            <details class="catalog-filter-group" open><summary>Danh mục</summary>
              <div class="products-filter-options"><button v-for="cat in categories" :key="cat.id" class="products-filter-chip" :class="{'products-filter-chip-active':filters.category===cat.slug || String(filters.categoryId)===String(cat.id)}" :aria-pressed="filters.category===cat.slug || String(filters.categoryId)===String(cat.id)" @click="toggleCategory(cat.slug)">{{ cat.name }}</button></div>
              <p v-if="!categories.length" class="muted">{{ isLoading ? 'Đang tải danh mục…' : 'Danh mục chưa khả dụng.' }}</p>
            </details>
            <details class="catalog-filter-group" :open="present(filters.min_price)||present(filters.max_price)"><summary>Khoảng giá</summary>
              <div class="products-price-presets"><button v-for="p in pricePresets" :key="p.label" class="products-price-chip" :class="{'products-price-chip-active':samePrice(p)}" :aria-pressed="samePrice(p)" @click="setPricePreset(p)">{{ p.label }}</button></div>
              <form class="marketplace-price-form" novalidate @submit.prevent="applyCustomPrice"><div class="products-price-inputs">
                <label>Từ (₫)<input v-model="priceMinInput" aria-label="Giá tối thiểu" class="products-price-input" inputmode="decimal" type="number" min="0" step="0.01" :aria-invalid="!!priceError" :aria-describedby="priceError?'price-error':undefined"/></label>
                <label>Đến (₫)<input v-model="priceMaxInput" aria-label="Giá tối đa" class="products-price-input" inputmode="decimal" type="number" min="0" step="0.01" :aria-invalid="!!priceError" :aria-describedby="priceError?'price-error':undefined"/></label>
              </div><p v-if="priceError" id="price-error" class="marketplace-error" role="alert">{{ priceError }}</p><button class="products-filter-apply" type="submit">Áp dụng khoảng giá</button></form>
            </details>
            <details class="catalog-filter-group" :open="filters.in_stock!==null"><summary>Tình trạng hàng</summary><div role="radiogroup" aria-label="Tình trạng hàng"><label v-for="option in stockOptions" :key="String(option.value)" class="products-filter-row"><input v-model="filters.in_stock" type="radio" name="availability" :value="option.value" @change="applyFilters"/>{{ option.label }}</label></div></details>
            <details class="catalog-filter-group" :open="!!filters.store"><summary>Gian hàng</summary><label class="sr-only" for="products-store-filter">Gian hàng</label><select id="products-store-filter" v-model="filters.store" class="products-sort-select" @change="applyFilters"><option value="">Tất cả gian hàng</option><option v-for="store in stores" :key="store.id" :value="String(store.id)">{{ store.name }}</option></select></details>
            <button v-if="hasActiveFilters" class="products-filter-reset" @click="resetFilters">Xóa tất cả bộ lọc</button>
            <button class="mobile-filter-close button button-primary marketplace-filter-done" @click="applyFilters(true); filterDialog.close()">Xem kết quả</button>
          </div></aside>
        </dialog>
        <section class="products-main" aria-label="Kết quả sản phẩm" :aria-busy="isLoading">
          <div class="products-toolbar"><span v-if="!isLoading && !apiError" class="products-result-count" role="status">{{ products.total }} kết quả</span><span v-else>{{ apiError ? 'Kết quả chưa khả dụng' : 'Đang tìm sản phẩm…' }}</span>
            <label class="products-toolbar-right">Sắp xếp<select v-model="sort" aria-label="Sắp xếp sản phẩm" class="products-sort-select" @change="applyFilters"><option value="default">Mặc định</option><option value="latest">Mới đăng gần đây</option><option value="price_asc">Giá thấp → cao</option><option value="price_desc">Giá cao → thấp</option><option value="name_asc">Tên A → Z</option></select></label></div>
          <div v-if="activeFilterTags.length" class="catalog-active-filters" aria-label="Bộ lọc đang áp dụng"><span v-for="tag in activeFilterTags" :key="tag.label" class="products-active-tag">{{ tag.label }}<button :aria-label="'Xóa bộ lọc '+tag.label" class="products-active-close" @click="tag.remove"><MarketIcon name="close"/></button></span><button class="text-link" @click="resetFilters">Xóa tất cả</button></div>
          <div v-if="!isLoading && !apiError && products.data?.length" class="products-grid"><ProductCard v-for="product in products.data" :key="product.id" :product="product" streamlined/></div>
          <CatalogState v-else :loading="isLoading" :error="apiError" :message="errorMessage" :filtered="hasActiveFilters" @retry="$emit('retry')" @clear="resetFilters"/>
          <CatalogPagination v-if="!apiError && !isLoading && products.data?.length" :page="products.page" :total-pages="products.totalPages"/>
        </section>
      </div>
    </main>
  </MarketplaceLayout>
</template>
<script setup>
import {ref,computed,watch,onMounted,onBeforeUnmount} from 'vue';
import {useRoute,useRouter} from 'vue-router';
import MarketplaceLayout from '../../../layouts/MarketplaceLayout.vue';
import ProductCard from '../../../components/ProductCard.vue';
import MarketIcon from '../../../components/marketplace/MarketIcon.vue';
import CatalogPagination from '../../../components/marketplace/CatalogPagination.vue';
import CatalogState from '../../../components/marketplace/CatalogState.vue';
const props=defineProps({products:{type:Object,default:()=>({data:[],total:0})},categories:{type:Array,default:()=>[]},stores:{type:Array,default:()=>[]},isLoading:Boolean,apiError:Boolean,errorMessage:String});
defineEmits(['retry']);
const currentRoute=useRoute(),router=useRouter(),filterOpen=ref(false),filterDialog=ref(null),filterTrigger=ref(null);
const filters=ref({}),sort=ref('default'),priceMinInput=ref(''),priceMaxInput=ref(''),priceError=ref('');
function syncQuery(q){filters.value={search:q.search||q.q||'',category:q.category||'',categoryId:q.categoryId||'',store:q.store||'',min_price:q.min_price??null,max_price:q.max_price??null,in_stock:q.in_stock===undefined?null:['true','1'].includes(q.in_stock)?'1':'0'};sort.value=q.sort||'default';priceMinInput.value=q.min_price??'';priceMaxInput.value=q.max_price??'';priceError.value='';}
watch(()=>currentRoute.query,syncQuery,{immediate:true});
const pricePresets=[{label:'Dưới 100.000 ₫',min:null,max:100000},{label:'100.000 – 500.000 ₫',min:100000,max:500000},{label:'500.000 – 1.000.000 ₫',min:500000,max:1000000},{label:'Trên 1.000.000 ₫',min:1000000,max:null}];
const stockOptions=[{label:'Tất cả',value:null},{label:'Còn hàng',value:'1'},{label:'Hết hàng',value:'0'}];
const present=v=>v!==null&&v!==undefined&&v!=='';
const money=v=>new Intl.NumberFormat('vi-VN').format(Number(v))+' ₫';
const samePrice=p=>[p.min,p.max].every((v,i)=>present(v)?Number(v)===Number(i?filters.value.max_price:filters.value.min_price)&&present(i?filters.value.max_price:filters.value.min_price):!present(i?filters.value.max_price:filters.value.min_price));
const hasActiveFilters=computed(()=>Object.values(filters.value).some(present));
const activeFilterTags=computed(()=>{const f=filters.value,tags=[];const add=(label,keys)=>tags.push({label,remove:()=>{keys.forEach(k=>f[k]=null);applyFilters();}});
  if(f.search)add('Từ khóa: '+f.search,['search']);
  if(f.category||f.categoryId)add(props.categories.find(c=>c.slug===f.category||String(c.id)===String(f.categoryId))?.name||'Danh mục đã chọn',['category','categoryId']);
  if(f.store)add(props.stores.find(s=>String(s.id)===String(f.store))?.name||'Gian hàng #'+f.store,['store']);
  if(present(f.min_price)||present(f.max_price))add(present(f.min_price)&&present(f.max_price)?money(f.min_price)+' – '+money(f.max_price):present(f.max_price)?'Đến '+money(f.max_price):'Từ '+money(f.min_price),['min_price','max_price']);
  if(f.in_stock!==null)add(f.in_stock==='1'?'Còn hàng':'Hết hàng',['in_stock']);return tags;});
function applyFilters(force=false){if(filterOpen.value&&force!==true)return;const query={};for(const [k,v] of Object.entries(filters.value))if(present(v))query[k]=String(v);if(sort.value!=='default')query.sort=sort.value;if(currentRoute.query.size)query.size=currentRoute.query.size;router.push({path:'/products',query});}
function toggleCategory(slug){filters.value.category=filters.value.category===slug?null:slug;filters.value.categoryId=null;applyFilters();}
function setPricePreset(p){filters.value.min_price=p.min;filters.value.max_price=p.max;priceMinInput.value=p.min??'';priceMaxInput.value=p.max??'';priceError.value='';applyFilters();}
function applyCustomPrice(){const min=priceMinInput.value,max=priceMaxInput.value;if([min,max].some(v=>present(v)&&(!Number.isFinite(Number(v))||Number(v)<0||Number(v)>9999999999999.99))||(present(min)&&present(max)&&Number(min)>Number(max))){priceError.value='Nhập giá từ 0; giá tối thiểu không được lớn hơn giá tối đa.';return;}filters.value.min_price=present(min)?min:null;filters.value.max_price=present(max)?max:null;priceError.value='';applyFilters();}
function resetFilters(){const query={};if(sort.value!=='default')query.sort=sort.value;if(currentRoute.query.size)query.size=currentRoute.query.size;router.push({path:'/products',query});}
function openFilters(){filterOpen.value=true;filterDialog.value.showModal();}
function trapFilterFocus(event){const items=[...filterDialog.value.querySelectorAll('button,input,select,summary,a[href],[tabindex]')].filter(el=>!el.disabled&&el.tabIndex>=0&&el.getClientRects().length);const first=items[0],last=items.at(-1);if(!items.length)return;if(event.shiftKey&&document.activeElement===first){event.preventDefault();last.focus();}else if(!event.shiftKey&&document.activeElement===last){event.preventDefault();first.focus();}}
function resize(){if(innerWidth>=1024&&filterDialog.value?.open)filterDialog.value.close();}
onMounted(()=>window.addEventListener('resize',resize));onBeforeUnmount(()=>window.removeEventListener('resize',resize));
</script>

<template>
  <MarketplaceLayout :categories="categories">
    <main class="products-page market-container market-page marketplace-surface">
      <header class="products-page-heading"><h1>{{ filters.search ? 'Kết quả cho “'+filters.search+'”' : 'Khám phá sản phẩm' }}</h1><p>Cây cảnh, bonsai, chậu và phụ kiện từ các gian hàng.</p></header>
      <form class="catalog-search" role="search" aria-label="Tìm kiếm trong danh sách sản phẩm" @submit.prevent="searchProducts">
        <label for="catalog-search">Tìm sản phẩm</label><div class="catalog-search-row"><input id="catalog-search" ref="searchInput" v-model="searchTerm" type="search" maxlength="160" enterkeyhint="search" placeholder="Tên cây, chậu hoặc phụ kiện…" @search="!searchTerm && clearSearch()"/><button v-if="searchTerm" type="button" class="button button-outline" aria-label="Xóa từ khóa trong danh sách sản phẩm" @click="clearSearch"><MarketIcon name="close"/></button><button class="button button-primary" type="submit"><MarketIcon name="search"/>Tìm kiếm</button></div>
      </form>
      <div v-if="categoriesError || storesError" class="catalog-discovery-error" role="status"><p>{{ categoriesError && storesError ? 'Chưa tải được danh mục và gian hàng để lọc.' : categoriesError ? 'Chưa tải được danh mục để lọc.' : 'Chưa tải được gian hàng để lọc.' }} Bạn vẫn có thể tìm kiếm và xem sản phẩm.</p><button class="button button-outline" :disabled="isLoading" @click="$emit('retry')">Tải lại bộ lọc</button></div>
      <div class="products-layout">
        <button ref="filterTrigger" aria-controls="market-filters" aria-haspopup="dialog" :aria-expanded="filterOpen" class="products-filter-toggle" @click="openFilters"><MarketIcon name="filter"/>Bộ lọc<span v-if="activeFilterTags.length"> ({{ activeFilterTags.length }})</span></button>
        <dialog ref="filterDialog" aria-labelledby="filter-dialog-title" class="filter-dialog" @keydown.tab="trapDialogFocus" @close="closeFilters">
          <aside id="market-filters" class="products-sidebar"><div class="products-filter-panel">
            <div class="products-filter-header"><h2 id="filter-dialog-title">Bộ lọc</h2><button class="mobile-filter-close button button-outline" @click="filterDialog.close()">Đóng</button></div>
            <details class="catalog-filter-group" open><summary>Danh mục</summary>
              <div class="products-filter-options"><button v-for="cat in categories" :key="cat.id" class="products-filter-chip" :class="{'products-filter-chip-active':filters.category===cat.slug || String(filters.categoryId)===String(cat.id)}" :aria-pressed="filters.category===cat.slug || String(filters.categoryId)===String(cat.id)" @click="toggleCategory(cat)">{{ cat.name }}</button></div>
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
            <details v-if="showPotFilters" class="catalog-filter-group" open><summary>Thông số chậu</summary>
              <label>Chất liệu<input v-model="filters.pot_material" maxlength="160" placeholder="Ví dụ: Gốm" @change="applyFilters"/></label>
              <label>Kiểu dáng<select v-model="filters.pot_shape" @change="applyFilters"><option value="">Tất cả</option><option v-for="(label,key) in potShapes" :key="key" :value="key">{{ label }}</option></select></label>
              <label>Màu sắc<input v-model="filters.pot_color" maxlength="100" @change="applyFilters"/></label>
              <div class="products-price-inputs"><label>Đường kính từ (cm)<input v-model="filters.pot_min_diameter" type="number" min="0.1" max="500" step="0.1" @change="applyFilters"/></label><label>Đến (cm)<input v-model="filters.pot_max_diameter" type="number" min="0.1" max="500" step="0.1" @change="applyFilters"/></label></div>
              <label>Thoát nước<select v-model="filters.pot_drainage" @change="applyFilters"><option value="">Tất cả</option><option value="YES">Có</option><option value="NO">Không</option><option value="UNKNOWN">Chưa rõ</option></select></label>
              <p v-if="potError" role="alert" class="marketplace-error">{{ potError }}</p>
              <p class="muted">Chất liệu và màu khớp giá trị người bán đã khai báo.</p>
            </details>
            <button v-if="hasActiveFilters" class="products-filter-reset" @click="resetFilters">Xóa tất cả bộ lọc</button>
            <button class="mobile-filter-close button button-primary marketplace-filter-done" @click="confirmFilters">Áp dụng bộ lọc</button>
          </div></aside>
        </dialog>
        <section id="catalog-results" class="products-main" aria-label="Kết quả sản phẩm" tabindex="-1" :aria-busy="isLoading">
          <h2 id="catalog-results-title" class="sr-only" tabindex="-1">Kết quả sản phẩm</h2>
          <div class="products-toolbar"><span v-if="!isLoading && !apiError" class="products-result-count" role="status">{{ products.total }} kết quả</span><span v-else>{{ apiError ? 'Kết quả chưa khả dụng' : 'Đang tìm sản phẩm…' }}</span>
            <label class="products-toolbar-right">Sắp xếp<select v-model="sort" aria-label="Sắp xếp sản phẩm" class="products-sort-select" @change="applyFilters"><option value="sold_desc">Bán chạy nhất</option><option value="default">Mặc định</option><option value="latest">Mới đăng gần đây</option><option value="price_asc">Giá thấp → cao</option><option value="price_desc">Giá cao → thấp</option><option value="name_asc">Tên A → Z</option></select></label></div>
          <div v-if="activeFilterTags.length" class="catalog-active-filters" aria-label="Bộ lọc đang áp dụng"><span v-for="tag in activeFilterTags" :key="tag.label" class="products-active-tag">{{ tag.label }}<button :aria-label="'Xóa bộ lọc '+tag.label" class="products-active-close" @click="tag.remove"><MarketIcon name="close"/></button></span><button class="text-link" @click="resetFilters">Xóa tất cả</button></div>
          <div v-if="!isLoading && !apiError && products.data?.length" class="products-grid"><ProductCard v-for="product in products.data" :key="product.id" :product="product" streamlined/></div>
          <CatalogState v-else :loading="isLoading" :error="apiError" :message="errorMessage" :status="errorStatus" :filtered="hasActiveFilters" @retry="$emit('retry')" @clear="resetFilters"/>
          <CatalogPagination v-if="!apiError && !isLoading && products.data?.length" :page="products.page" :total-pages="products.totalPages" :busy="isLoading" focus-target="#catalog-results"/>
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
import {trapDialogFocus} from '../../../utils/dialogFocus.js';
const props=defineProps({products:{type:Object,default:()=>({data:[],total:0})},categories:{type:Array,default:()=>[]},stores:{type:Array,default:()=>[]},isLoading:Boolean,apiError:Boolean,errorMessage:String,errorStatus:Number,categoriesError:Boolean,storesError:Boolean});
defineEmits(['retry']);
const currentRoute=useRoute(),router=useRouter(),filterOpen=ref(false),filterDialog=ref(null),filterTrigger=ref(null);
let bodyOverflow;
watch(filterOpen,open=>{if(open){bodyOverflow=document.body.style.overflow;document.body.style.overflow='hidden';}else if(bodyOverflow!==undefined){document.body.style.overflow=bodyOverflow;bodyOverflow=undefined;}});
const filters=ref({}),sort=ref('default'),priceMinInput=ref(''),priceMaxInput=ref(''),priceError=ref('');
const searchTerm=ref(''),searchInput=ref(null);
const potKeys=['pot_material','pot_shape','pot_color','pot_min_diameter','pot_max_diameter','pot_drainage'];
const potShapes={ROUND:'Tròn',RECTANGULAR:'Chữ nhật',SQUARE:'Vuông',OVAL:'Bầu dục',OTHER:'Khác'},potError=ref('');
const showPotFilters=computed(()=>props.categories.some(c=>c.specificationType==='POT'&&(c.slug===filters.value.category||String(c.id)===String(filters.value.categoryId)))||potKeys.some(k=>filters.value[k]));
watch([()=>currentRoute.query.search,()=>currentRoute.query.q],([search,q])=>searchTerm.value=String(search||q||''),{immediate:true});
function searchProducts(){filters.value.search=searchTerm.value.trim().replace(/\s+/g,' ');applyFilters();}
function clearSearch(){searchTerm.value='';filters.value.search='';applyFilters();searchInput.value?.focus();}
function readQuery(q){return {search:q.search||q.q||'',category:q.category||'',categoryId:q.categoryId||'',store:q.store||'',min_price:q.min_price??null,max_price:q.max_price??null,in_stock:q.in_stock===undefined?null:['true','1'].includes(q.in_stock)?'1':['false','0'].includes(q.in_stock)?'0':null,...Object.fromEntries(potKeys.map(key=>[key,q[key]||'']))};}
function syncQuery(q){filters.value=readQuery(q);sort.value=q.sort||'default';priceMinInput.value=q.min_price??'';priceMaxInput.value=q.max_price??'';priceError.value='';potError.value='';}
watch(()=>currentRoute.query,syncQuery,{immediate:true});
const pricePresets=[{label:'Dưới 100.000 ₫',min:null,max:100000},{label:'100.000 – 500.000 ₫',min:100000,max:500000},{label:'500.000 – 1.000.000 ₫',min:500000,max:1000000},{label:'Trên 1.000.000 ₫',min:1000000,max:null}];
const stockOptions=[{label:'Tất cả',value:null},{label:'Còn hàng',value:'1'},{label:'Hết hàng',value:'0'}];
const present=v=>v!==null&&v!==undefined&&v!=='';
const money=v=>new Intl.NumberFormat('vi-VN').format(Number(v))+' ₫';
const samePrice=p=>[p.min,p.max].every((v,i)=>present(v)?Number(v)===Number(i?filters.value.max_price:filters.value.min_price)&&present(i?filters.value.max_price:filters.value.min_price):!present(i?filters.value.max_price:filters.value.min_price));
const hasActiveFilters=computed(()=>Object.values(filters.value).some(present));
const activeFilterTags=computed(()=>{const f=readQuery(currentRoute.query),tags=[];const add=(label,keys)=>tags.push({label,remove:()=>{filters.value={...f};keys.forEach(k=>filters.value[k]=null);applyFilters();}});
  if(f.search)add('Từ khóa: '+f.search,['search']);
  if(f.category||f.categoryId)add(props.categories.find(c=>c.slug===f.category||String(c.id)===String(f.categoryId))?.name||'Danh mục đã chọn',['category','categoryId']);
  if(f.store)add(props.stores.find(s=>String(s.id)===String(f.store))?.name||'Gian hàng #'+f.store,['store']);
  if(present(f.min_price)||present(f.max_price))add(present(f.min_price)&&present(f.max_price)?money(f.min_price)+' – '+money(f.max_price):present(f.max_price)?'Đến '+money(f.max_price):'Từ '+money(f.min_price),['min_price','max_price']);
  if(f.in_stock!==null)add(f.in_stock==='1'?'Còn hàng':'Hết hàng',['in_stock']);
  const potLabels={pot_material:'Chất liệu',pot_shape:'Kiểu dáng',pot_color:'Màu',pot_min_diameter:'Đường kính từ',pot_max_diameter:'Đường kính đến',pot_drainage:'Thoát nước'};
  for(const key of potKeys)if(f[key])add(potLabels[key]+': '+(key==='pot_shape'?potShapes[f[key]]||f[key]:key==='pot_drainage'?{YES:'Có',NO:'Không',UNKNOWN:'Chưa rõ'}[f[key]]||f[key]:f[key]),[key]);return tags;});
function validatePot(){const f=filters.value;potError.value='';const range=[f.pot_min_diameter,f.pot_max_diameter];if(range.some(v=>present(v)&&(!/^[0-9]{1,3}(\.[0-9])?$/.test(String(v))||Number(v)<0.1||Number(v)>500))||range.every(present)&&Number(range[0])>Number(range[1])){potError.value='Đường kính từ 0,1 đến 500 cm; giá trị tối thiểu không lớn hơn tối đa.';return false;}return true;}
function applyFilters(force=false){if(filterOpen.value&&force!==true)return;if(!validatePot())return;const query={};for(const [k,v] of Object.entries(filters.value))if(present(v))query[k]=String(v);if(sort.value!=='default')query.sort=sort.value;if(currentRoute.query.size)query.size=currentRoute.query.size;return router.push({path:'/products',query});}
function toggleCategory(category){const selected=filters.value.category===category.slug||String(filters.value.categoryId)===String(category.id);filters.value.category=selected?null:category.slug||null;filters.value.categoryId=selected||category.slug?null:category.id;applyFilters();}
function setPricePreset(p){filters.value.min_price=p.min;filters.value.max_price=p.max;priceMinInput.value=p.min??'';priceMaxInput.value=p.max??'';priceError.value='';applyFilters();}
function applyCustomPrice(){const min=priceMinInput.value,max=priceMaxInput.value;if([min,max].some(v=>present(v)&&(!/^[0-9]{1,13}(\.[0-9]{1,2})?$/.test(String(v))||!Number.isFinite(Number(v))))||(present(min)&&present(max)&&Number(min)>Number(max))){priceError.value='Nhập giá từ 0, tối đa 13 chữ số và 2 số thập phân; giá tối thiểu không được lớn hơn giá tối đa.';return false;}filters.value.min_price=present(min)?min:null;filters.value.max_price=present(max)?max:null;priceError.value='';applyFilters();return true;}
function resetFilters(){syncQuery({sort:sort.value});if(!filterOpen.value)searchTerm.value='';applyFilters();}
async function confirmFilters(){if(!applyCustomPrice()||!validatePot())return;await applyFilters(true);filterDialog.value?.close();}
function closeFilters(){filterOpen.value=false;syncQuery(currentRoute.query);if(filterTrigger.value?.getClientRects().length)filterTrigger.value.focus();}
function openFilters(){syncQuery(currentRoute.query);filterOpen.value=true;filterDialog.value.showModal();}
function resize(){if(innerWidth>=1024&&filterDialog.value?.open)filterDialog.value.close();}
onMounted(()=>window.addEventListener('resize',resize));onBeforeUnmount(()=>{window.removeEventListener('resize',resize);if(bodyOverflow!==undefined)document.body.style.overflow=bodyOverflow;});
</script>

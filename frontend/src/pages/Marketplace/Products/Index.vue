<template>
  <MarketplaceLayout :categories="categories">
    <main class="products-page market-container market-page">
      <header class="products-page-heading">
        <p class="products-eyebrow">Khám phá cây cảnh</p>
        <h1>Sản phẩm từ các gian hàng</h1>
        <p>Tìm cây theo tên, danh mục hoặc mức giá. Xem gian hàng đang bán khi sản phẩm có thông tin này.</p>
      </header>
      <div class="products-layout">
        <button ref="filterTrigger" class="products-filter-toggle" @click="filterDialog.showModal()" aria-haspopup="dialog" aria-controls="market-filters">
          <MarketIcon :name="filterOpen ? 'close' : 'filter'" width="18" height="18" />
          {{ filterOpen ? 'Đóng bộ lọc' : 'Bộ lọc' }}
        </button>
        <dialog @close="filterTrigger?.focus()" ref="filterDialog" class="filter-dialog" aria-labelledby="filter-dialog-title"><aside :class="{ 'products-sidebar-open': filterOpen }" class="products-sidebar" id="market-filters">
          <div class="products-filter-panel">
            <div class="products-filter-header">
              <h2 id="filter-dialog-title" class="products-filter-title">Bộ lọc</h2><button class="mobile-filter-close" aria-label="Đóng bộ lọc" @click="filterDialog.close()">Đóng</button>
              <button v-if="hasActiveFilters" class="products-filter-reset" @click="resetFilters">Xóa tất cả</button>
            </div>

            <div class="products-filter-group">
              <label for="products-local-search" class="products-filter-label">
                <MarketIcon name="search" width="18" height="18" />
                Tìm kiếm
              </label>
              <input id="products-local-search" v-model="filters.search" class="products-search-input" placeholder="Tên cây, phụ kiện..." type="text"
                     @keyup.enter="applyFilters"/>
            </div>

            <div class="products-filter-group">
              <h3 class="products-filter-label">
                <MarketIcon name="grid" width="18" height="18" />
                Danh mục
              </h3>
              <div class="products-filter-options">
                <button v-for="cat in categories" :key="cat.id"
                        :class="{ 'products-filter-chip-active': filters.category === cat.slug }"
                        class="products-filter-chip"
                        @click="toggleCategory(cat.slug)">
                  <span class="products-chip-dot"></span>
                  {{ cat.name }}
                </button>
              </div>
            </div>

            <div class="products-filter-group">
              <h3 class="products-filter-label">
                <MarketIcon name="wallet" width="18" height="18" />
                Khoảng giá
              </h3>
              <div class="products-price-presets">
                <button v-for="preset in pricePresets" :key="preset.label"
                        :class="{ 'products-price-chip-active': filters.min_price === preset.min && filters.max_price === preset.max }"
                        class="products-price-chip"
                        @click="setPricePreset(preset)">
                  {{ preset.label }}
                </button>
              </div>
              <div class="products-price-inputs">
                <input v-model.number="priceMinInput" class="products-price-input" placeholder="Từ" type="number" aria-label="Giá tối thiểu">
                <span class="products-price-sep">—</span>
                <input v-model.number="priceMaxInput" class="products-price-input" placeholder="Đến" type="number" aria-label="Giá tối đa">
              </div>
              <button class="products-filter-apply" @click="applyCustomPrice">Áp dụng</button>
            </div>

            <div class="products-filter-group">
              <h3 class="products-filter-label">
                <MarketIcon name="box" width="18" height="18" />
                Tình trạng
              </h3>
              <div class="products-filter-options">
                <label :class="{ 'products-filter-row-active': !filters.in_stock }" class="products-filter-row">
                  <input v-model="filters.in_stock" :value="null" class="products-radio" type="radio"
                         @change="applyFilters">
                  <span class="products-radio-label">Tất cả</span>
                </label>
                <label :class="{ 'products-filter-row-active': filters.in_stock }" class="products-filter-row">
                  <input v-model="filters.in_stock" class="products-radio" type="radio" value="1"
                         @change="applyFilters">
                  <span class="products-radio-label">Còn hàng</span>
                </label>
              </div>
            </div>
            <div v-if="stores.length" class="products-filter-group">
              <label for="products-store-filter" class="products-filter-label">Gian hàng</label>
              <select id="products-store-filter" v-model="filters.store" class="products-sort-select" @change="applyFilters">
                <option value="">Tất cả gian hàng</option>
                <option v-for="store in stores" :key="store.id" :value="String(store.id)">{{ store.name }}</option>
              </select>
            </div>
          </div>
        </aside></dialog>

        <div class="products-main">
          <div class="products-toolbar">
            <div class="products-toolbar-left">
              <span v-for="(tag, i) in activeFilterTags" :key="i" class="products-active-tag">
                {{ tag.label }}
                <button class="products-active-close" :aria-label="'Xóa bộ lọc '+tag.label" @click="tag.remove">
                  <MarketIcon name="close" width="14" height="14" />
                </button>
              </span>
              <span class="products-result-count">{{ products.total || 0 }} kết quả</span>
            </div>
            <div class="products-toolbar-right">
              <span class="products-sort-label">Sắp xếp:</span>
              <select v-model="sort" class="products-sort-select" aria-label="Sắp xếp sản phẩm" @change="applyFilters">
                <option value="latest">Theo catalog</option>
                <option value="price_asc">Giá thấp → cao</option>
                <option value="price_desc">Giá cao → thấp</option>
                <option value="name_asc">Tên A → Z</option>
              </select>
            </div>
          </div>

          <div v-if="products.data?.length" class="products-grid">
            <ProductCard v-for="product in products.data" :key="product.id" :product="product"/>
          </div>

          <div v-else-if="!isLoading && !apiError" class="products-empty">
            <div class="products-empty-icon">
              <MarketIcon name="search" width="48" height="48" />
            </div>
            <h3 class="products-empty-title">Không tìm thấy sản phẩm</h3>
            <p class="products-empty-desc">Thử thay đổi bộ lọc hoặc từ khóa tìm kiếm.</p>
            <button class="products-empty-btn" @click="resetFilters">Xóa bộ lọc</button>
          </div>

          <div v-if="products.last_page > 1" class="products-pagination">
            <Link v-if="products.prev_page_url" :href="products.prev_page_url"
                  class="pagination-btn" aria-label="Trang trước">
              <MarketIcon name="chevronLeft" />
            </Link>
            <template v-for="(link, i) in products.links" :key="i">
              <span v-if="!link.url && link.label === '...'" class="pagination-dots">...</span>
              <Link v-else-if="link.url && !isNaN(link.label)"
                    :class="{ 'pagination-active': link.active }"
                    :href="link.url"
                    class="pagination-page">
                {{ link.label }}
              </Link>
            </template>
            <Link v-if="products.next_page_url" :href="products.next_page_url"
                  class="pagination-btn" aria-label="Trang sau">
              <MarketIcon name="chevron" />
            </Link>
          </div>
        </div>
      </div>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import {ref, computed, watch} from 'vue';
import {useRoute} from 'vue-router';
import {Link, router, usePage} from '@inertiajs/vue3';
import {route} from 'ziggy-js';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';
import ProductCard from '@agriverse/Components/ProductCard.vue';
import MarketIcon from '../../../components/marketplace/MarketIcon.vue';
import {useToast} from 'primevue/usetoast';
import {useChat} from '@agriverse/Composables/useChat';
import webApi from '@agriverse/services/webApi';

const toast = useToast();
const page = usePage();
const currentRoute = useRoute();
const {openPanel} = useChat();
const isAuthenticated = computed(() => !!page.props.auth?.user);

const props = defineProps({
  products: {type: Object, default: () => ({data: [], total: 0, links: []})},
  categories: {type: Array, default: () => []},
  stores: {type: Array, default: () => []},
  filters: {type: Object, default: () => ({})},
  isLoading: Boolean,
  apiError: Boolean,
});

const filterOpen = ref(false);
const filterDialog = ref(null);
const filterTrigger = ref(null);
watch(()=>currentRoute.fullPath,()=>{if(filterDialog.value?.open)filterDialog.value.close()});
const sort = ref(currentRoute.query.sort || 'latest');
const filters = ref({
  search: currentRoute.query.search || '',
  category: currentRoute.query.category || null,
  store: currentRoute.query.store || '',
  min_price: currentRoute.query.min_price || null,
  max_price: currentRoute.query.max_price || null,
  in_stock: currentRoute.query.in_stock || null,
});
watch(() => currentRoute.query, query => {
  sort.value = query.sort || 'latest';
  filters.value = {
    search: query.search || '', category: query.category || null, store: query.store || '',
    min_price: query.min_price || null, max_price: query.max_price || null,
    in_stock: query.in_stock || null,
  };
});
const priceMinInput = ref(filters.value.min_price);
const priceMaxInput = ref(filters.value.max_price);

const pricePresets = [
  {label: 'Dưới 100k', min: null, max: 100000},
  {label: '100k - 500k', min: 100000, max: 500000},
  {label: '500k - 1tr', min: 500000, max: 1000000},
  {label: '1tr - 5tr', min: 1000000, max: 5000000},
  {label: 'Trên 5tr', min: 5000000, max: null},
];

const hasActiveFilters = computed(() => filters.value.search || filters.value.category || filters.value.store || filters.value.min_price || filters.value.max_price || filters.value.in_stock);

const activeFilterTags = computed(() => {
  const tags = [];
  if (filters.value.search) tags.push({label: `Tìm: ${filters.value.search}`, remove: () => { filters.value.search = ''; applyFilters(); }});
  if (filters.value.category) {
    const cat = props.categories.find(c => c.slug === filters.value.category);
    tags.push({
      label: cat?.name || filters.value.category,
      remove: () => {
        filters.value.category = null;
        applyFilters();
      }
    });
  }
  if (filters.value.store) tags.push({label: props.stores.find(s => String(s.id) === String(filters.value.store))?.name || 'Gian hàng', remove: () => { filters.value.store = ''; applyFilters(); }});
  if (filters.value.min_price || filters.value.max_price) {
    tags.push({
      label: `${filters.value.min_price ? formatPrice(filters.value.min_price) : 0}₫ - ${filters.value.max_price ? formatPrice(filters.value.max_price) : '∞'}₫`,
      remove: () => {
        filters.value.min_price = null;
        filters.value.max_price = null;
        applyFilters();
      }
    });
  }
  if (filters.value.in_stock) {
    tags.push({
      label: 'Còn hàng',
      remove: () => {
        filters.value.in_stock = null;
        applyFilters();
      }
    });
  }
  return tags;
});

function toggleCategory(slug) {
  filters.value.category = filters.value.category === slug ? null : slug;
  applyFilters();
}

function setPricePreset(preset) {
  if (filters.value.min_price === preset.min && filters.value.max_price === preset.max) {
    filters.value.min_price = null;
    filters.value.max_price = null;
  } else {
    filters.value.min_price = preset.min;
    filters.value.max_price = preset.max;
  }
  priceMinInput.value = filters.value.min_price;
  priceMaxInput.value = filters.value.max_price;
  applyFilters();
}

function formatPrice(price) {
  return new Intl.NumberFormat('vi-VN').format(price || 0);
}

function truncate(text, len) {
  if (!text) return '';
  return text.length > len ? text.substring(0, len) + '...' : text;
}

function applyFilters() {
  priceMinInput.value = filters.value.min_price;
  priceMaxInput.value = filters.value.max_price;
  const params = {};
  if (filters.value.search) params.search = filters.value.search;
  if (filters.value.category) params.category = filters.value.category;
  if (filters.value.store) params.store = filters.value.store;
  if (filters.value.min_price) params.min_price = filters.value.min_price;
  if (filters.value.max_price) params.max_price = filters.value.max_price;
  if (filters.value.in_stock) params.in_stock = filters.value.in_stock;
  if (sort.value && sort.value !== 'latest') params.sort = sort.value;
  router.get(route('agriverse.shop.products.index', params), {preserveState: true, preserveScroll: true});
}

function applyCustomPrice() {
  filters.value.min_price = priceMinInput.value || null;
  filters.value.max_price = priceMaxInput.value || null;
  applyFilters();
}

function resetFilters() {
  filters.value = {search: '', category: null, store: '', min_price: null, max_price: null, in_stock: null};
  priceMinInput.value = null;
  priceMaxInput.value = null;
  sort.value = 'latest';
  router.get(route('agriverse.shop.products.index'), {preserveState: true});
}

function quickAdd(productId) {
  window.dispatchEvent(new CustomEvent('bonsai:pending'));
}

function toggleWishlist(product) {
  window.dispatchEvent(new CustomEvent('bonsai:pending'));
}

async function chatWithSeller(product) {
  try {
    const {data} = await webApi.post('/agriverse/api/chat/start', {product_id: product.id})
    if (data.conversation) {
      openPanel(data.conversation.id, data.conversation.product)
    }
  } catch (e) {
    if (e.response?.status === 422) {
      toast.add({severity: 'warn', summary: e.response.data.error || 'Không thể nhắn tin', life: 3000})
    } else {
      toast.add({severity: 'error', summary: 'Vui lòng đăng nhập', life: 2000})
    }
  }
}
</script>

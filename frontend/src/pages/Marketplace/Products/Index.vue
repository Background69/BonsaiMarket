<template>
  <MarketplaceLayout>
    <main class="products-page">
      <div class="products-layout">
        <button class="products-filter-toggle" @click="filterOpen = !filterOpen">
          <span class="material-symbols-outlined" style="font-size: 18px;">{{ filterOpen ? 'close' : 'filter_list' }}</span>
          {{ filterOpen ? 'Đóng bộ lọc' : 'Bộ lọc' }}
        </button>
        <aside class="products-sidebar" :class="{ 'products-sidebar-open': filterOpen }">
          <div class="products-filter-panel">
            <div class="products-filter-header">
              <h2 class="products-filter-title">Bộ lọc</h2>
              <button v-if="hasActiveFilters" @click="resetFilters" class="products-filter-reset">Xoá tất cả</button>
            </div>

            <div class="products-filter-group">
              <h3 class="products-filter-label">
                <span class="material-symbols-outlined text-[18px]">search</span>
                Tìm kiếm
              </h3>
              <input v-model="filters.search" @keyup.enter="applyFilters" type="text" class="products-search-input" placeholder="Tên cây, phụ kiện..." />
            </div>

            <div class="products-filter-group">
              <h3 class="products-filter-label">
                <span class="material-symbols-outlined text-[18px]">category</span>
                Danh mục
              </h3>
              <div class="products-filter-options">
                <button v-for="cat in categories" :key="cat.id"
                  class="products-filter-chip"
                  :class="{ 'products-filter-chip-active': filters.category === cat.slug }"
                  @click="toggleCategory(cat.slug)">
                  <span class="products-chip-dot"></span>
                  {{ cat.name }}
                </button>
              </div>
            </div>

            <div class="products-filter-group">
              <h3 class="products-filter-label">
                <span class="material-symbols-outlined text-[18px]">payments</span>
                Khoảng giá
              </h3>
              <div class="products-price-presets">
                <button v-for="preset in pricePresets" :key="preset.label"
                  class="products-price-chip"
                  :class="{ 'products-price-chip-active': filters.min_price === preset.min && filters.max_price === preset.max }"
                  @click="setPricePreset(preset)">
                  {{ preset.label }}
                </button>
              </div>
              <div class="products-price-inputs">
                <input type="number" v-model.number="priceMinInput" placeholder="Từ" class="products-price-input">
                <span class="products-price-sep">—</span>
                <input type="number" v-model.number="priceMaxInput" placeholder="Đến" class="products-price-input">
              </div>
              <button @click="applyCustomPrice" class="products-filter-apply">Áp dụng</button>
            </div>

            <div class="products-filter-group">
              <h3 class="products-filter-label">
                <span class="material-symbols-outlined text-[18px]">inventory_2</span>
                Tình trạng
              </h3>
              <div class="products-filter-options">
                <label class="products-filter-row" :class="{ 'products-filter-row-active': !filters.in_stock }">
                  <input type="radio" v-model="filters.in_stock" :value="null" @change="applyFilters" class="products-radio">
                  <span class="products-radio-label">Tất cả</span>
                </label>
                <label class="products-filter-row" :class="{ 'products-filter-row-active': filters.in_stock }">
                  <input type="radio" v-model="filters.in_stock" value="1" @change="applyFilters" class="products-radio">
                  <span class="products-radio-label">Còn hàng</span>
                </label>
              </div>
            </div>
          </div>
        </aside>

        <div class="products-main">
          <div class="products-toolbar">
            <div class="products-toolbar-left">
              <span v-if="hasActiveFilters" class="products-active-tag" v-for="(tag, i) in activeFilterTags" :key="i">
                {{ tag.label }}
                <button @click="tag.remove" class="products-active-close">
                  <span class="material-symbols-outlined text-[14px]">close</span>
                </button>
              </span>
              <span class="products-result-count">{{ products.total || 0 }} kết quả</span>
            </div>
            <div class="products-toolbar-right">
              <span class="products-sort-label">Sắp xếp:</span>
              <select v-model="sort" @change="applyFilters" class="products-sort-select">
                <option value="latest">Mới nhất</option>
                <option value="price_asc">Giá thấp → cao</option>
                <option value="price_desc">Giá cao → thấp</option>
                <option value="name_asc">Tên A → Z</option>
              </select>
            </div>
          </div>

          <div v-if="products.data?.length" class="products-grid">
            <article v-for="product in products.data" :key="product.id" class="product-card">
              <Link :href="route('agriverse.shop.products.show', product.id)" class="product-card-link">
                <div class="product-card-img">
                  <img v-if="product.image" :src="product.image" :alt="product.name" class="product-card-real-img" />
                  <div v-else class="product-card-placeholder">
                    <span class="product-card-letter">{{ product.name.charAt(0).toUpperCase() }}</span>
                  </div>
                  <div class="product-card-badges">
                    <span v-if="product.has_approved_passport === false" class="product-badge-passport">Chưa có hộ chiếu</span>
                    <span v-if="product.compare_price && product.compare_price > product.price" class="product-badge-discount">
                      -{{ Math.round((1 - product.price / product.compare_price) * 100) }}%
                    </span>
                    <span v-if="product.model_3d_path" class="product-badge-3d" title="Có mô hình 3D">
                      <span class="material-symbols-outlined text-[14px]">view_in_ar</span>
                    </span>
                  </div>
                  <button @click.prevent="toggleWishlist(product)"
                    class="absolute top-3 right-3 w-8 h-8 rounded-xl flex items-center justify-center z-10 transition-all duration-200 hover:scale-110"
                    style="background: rgba(255,255,255,0.7); backdrop-filter: blur(8px);"
                    :title="product.wishlisted ? 'Bỏ yêu thích' : 'Thêm yêu thích'">
                    <span class="material-symbols-outlined text-sm"
                      :class="product.wishlisted ? 'text-[var(--ag-danger)]' : 'text-[var(--ag-text-muted)]'"
                      :style="`font-variation-settings: 'FILL' ${product.wishlisted ? 1 : 0}`">
                      favorite
                    </span>
                  </button>
                  <div v-if="product.stock < 1" class="product-badge-soldout">Hết hàng</div>
                </div>
                <div class="product-card-body">
                  <div class="product-card-top">
                    <h3 class="product-card-name">{{ product.name }}</h3>
                    <span class="product-card-price">{{ formatPrice(product.price) }}₫</span>
                  </div>
                  <p v-if="product.description" class="product-card-desc">{{ truncate(product.description, 80) }}</p>
                  <div class="product-card-footer">
                    <div class="product-card-meta flex flex-col gap-1">
                      <span v-if="product.store" class="product-card-store">{{ product.store.name }}</span>
                      <span v-if="product.seller" class="text-xs text-stone-500 flex items-center gap-1">
                        <span class="material-symbols-outlined text-[14px]">person</span>
                        {{ product.seller.name }}
                      </span>
                    </div>
                    <div class="flex gap-2">
                      <button v-if="product.seller && isAuthenticated" @click.prevent="chatWithSeller(product)" class="hover:bg-stone-100 p-2 rounded-xl transition-colors text-[var(--ag-primary-500)] flex items-center justify-center" title="Nhắn tin với người bán">
                        <span class="material-symbols-outlined text-lg">chat</span>
                      </button>
                      <button v-if="product.stock > 0" @click.prevent="quickAdd(product.id)" class="product-card-add">
                        <span class="material-symbols-outlined">add_shopping_cart</span>
                      </button>
                    </div>
                  </div>
                </div>
              </Link>
            </article>
          </div>

          <div v-else class="products-empty">
            <div class="products-empty-icon">
              <span class="material-symbols-outlined text-5xl">search_insights</span>
            </div>
            <h3 class="products-empty-title">Không tìm thấy sản phẩm</h3>
            <p class="products-empty-desc">Thử thay đổi bộ lọc hoặc từ khóa tìm kiếm.</p>
            <button @click="resetFilters" class="products-empty-btn">Xóa bộ lọc</button>
          </div>

          <div v-if="products.last_page > 1" class="products-pagination">
            <Link v-if="products.prev_page_url" :href="products.prev_page_url"
              class="pagination-btn">
              <span class="material-symbols-outlined">chevron_left</span>
            </Link>
            <template v-for="(link, i) in products.links" :key="i">
              <span v-if="!link.url && link.label === '...'" class="pagination-dots">...</span>
              <Link v-else-if="link.url && !isNaN(link.label)"
                :href="link.url"
                class="pagination-page"
                :class="{ 'pagination-active': link.active }">
                {{ link.label }}
              </Link>
            </template>
            <Link v-if="products.next_page_url" :href="products.next_page_url"
              class="pagination-btn">
              <span class="material-symbols-outlined">chevron_right</span>
            </Link>
          </div>
        </div>
      </div>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { useRoute } from 'vue-router';
import { Link, router, usePage } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';
import { useToast } from 'primevue/usetoast';
import { useChat } from '@agriverse/Composables/useChat';
import webApi from '@agriverse/services/webApi';

const toast = useToast();
const page = usePage();
const currentRoute = useRoute();
const { openPanel } = useChat();
const isAuthenticated = computed(() => !!page.props.auth?.user);

const props = defineProps({
  products: { type: Object, default: () => ({ data: [], total: 0, links: [] }) },
  categories: { type: Array, default: () => [] },
  filters: { type: Object, default: () => ({}) },
});

const filterOpen = ref(false);
const sort = ref(currentRoute.query.sort || 'latest');
const filters = ref({
  search: currentRoute.query.search || '',
  category: currentRoute.query.category || null,
  min_price: currentRoute.query.min_price || null,
  max_price: currentRoute.query.max_price || null,
  in_stock: currentRoute.query.in_stock || null,
});
watch(() => currentRoute.query, query => {
  sort.value = query.sort || 'latest';
  filters.value = {
    search: query.search || '', category: query.category || null,
    min_price: query.min_price || null, max_price: query.max_price || null,
    in_stock: query.in_stock || null,
  };
});
const priceMinInput = ref(filters.value.min_price);
const priceMaxInput = ref(filters.value.max_price);

const pricePresets = [
  { label: 'Dưới 100k', min: null, max: 100000 },
  { label: '100k - 500k', min: 100000, max: 500000 },
  { label: '500k - 1tr', min: 500000, max: 1000000 },
  { label: '1tr - 5tr', min: 1000000, max: 5000000 },
  { label: 'Trên 5tr', min: 5000000, max: null },
];

const hasActiveFilters = computed(() => filters.value.category || filters.value.min_price || filters.value.max_price || filters.value.in_stock);

const activeFilterTags = computed(() => {
  const tags = [];
  if (filters.value.category) {
    const cat = props.categories.find(c => c.slug === filters.value.category);
    tags.push({
      label: cat?.name || filters.value.category,
      remove: () => { filters.value.category = null; applyFilters(); }
    });
  }
  if (filters.value.min_price || filters.value.max_price) {
    tags.push({
      label: `${filters.value.min_price ? formatPrice(filters.value.min_price) : 0}₫ - ${filters.value.max_price ? formatPrice(filters.value.max_price) : '∞'}₫`,
      remove: () => { filters.value.min_price = null; filters.value.max_price = null; applyFilters(); }
    });
  }
  if (filters.value.in_stock) {
    tags.push({
      label: 'Còn hàng',
      remove: () => { filters.value.in_stock = null; applyFilters(); }
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
  if (filters.value.min_price) params.min_price = filters.value.min_price;
  if (filters.value.max_price) params.max_price = filters.value.max_price;
  if (filters.value.in_stock) params.in_stock = filters.value.in_stock;
  if (sort.value && sort.value !== 'latest') params.sort = sort.value;
  router.get(route('agriverse.shop.products.index', params), { preserveState: true, preserveScroll: true });
}

function applyCustomPrice() {
  filters.value.min_price = priceMinInput.value || null;
  filters.value.max_price = priceMaxInput.value || null;
  applyFilters();
}

function resetFilters() {
  filters.value = { search: '', category: null, min_price: null, max_price: null, in_stock: null };
  priceMinInput.value = null;
  priceMaxInput.value = null;
  sort.value = 'latest';
  router.get(route('agriverse.shop.products.index'), { preserveState: true });
}

function quickAdd(productId) {
  router.post(route('agriverse.api.cart.add'), { product_id: productId, quantity: 1 }, {
    preserveState: true,
    preserveScroll: true,
    onSuccess: () => toast.add({ severity: 'success', summary: 'Đã thêm vào giỏ hàng', life: 2000 }),
    onError: () => toast.add({ severity: 'error', summary: 'Vui lòng đăng nhập', life: 2000 }),
  });
}

function toggleWishlist(product) {
  product.wishlisted = !product.wishlisted;
  router.post(route('agriverse.api.wishlist.toggle', product.id), {}, {
    preserveState: true,
    preserveScroll: true,
    onError: () => {
      product.wishlisted = !product.wishlisted;
      toast.add({ severity: 'error', summary: 'Vui lòng đăng nhập', life: 2000 });
    },
  });
}

async function chatWithSeller(product) {
  try {
    const { data } = await webApi.post('/agriverse/api/chat/start', { product_id: product.id })
    if (data.conversation) {
      openPanel(data.conversation.id, data.conversation.product)
    }
  } catch (e) {
    if (e.response?.status === 422) {
      toast.add({ severity: 'warn', summary: e.response.data.error || 'Không thể nhắn tin', life: 3000 })
    } else {
      toast.add({ severity: 'error', summary: 'Vui lòng đăng nhập', life: 2000 })
    }
  }
}
</script>

<style scoped>
.products-page {
  padding-top: 104px;
  padding-bottom: 80px;
  max-width: 1280px;
  margin: 0 auto;
  padding-left: 64px;
  padding-right: 64px;
}
@media (max-width: 768px) {
  .products-page { padding: 88px 20px 60px; }
}

.products-layout {
  display: flex;
  flex-direction: column;
  gap: 32px;
}
@media (min-width: 1024px) {
  .products-layout { flex-direction: row; gap: 40px; }
}

.products-sidebar {
  width: 100%;
  flex-shrink: 0;
}
@media (min-width: 1024px) {
  .products-sidebar { width: 260px; }
}
@media (max-width: 1023px) {
  .products-sidebar { display: none; }
  .products-sidebar-open { display: block; }
}
.products-filter-toggle {
  display: none;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border: 1px solid var(--ag-border);
  border-radius: 10px;
  background: white;
  color: var(--ag-text-primary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: var(--ag-font-body);
  transition: all 0.2s;
}
.products-filter-toggle:hover {
  border-color: var(--ag-primary-500);
  color: var(--ag-primary-500);
}
@media (max-width: 1023px) {
  .products-filter-toggle { display: inline-flex; }
}
.products-filter-panel {
  background: white;
  padding: 20px;
  border-radius: 16px;
  border: 1px solid rgba(116, 121, 108, 0.08);
  position: sticky;
  top: 96px;
}
.products-filter-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(116, 121, 108, 0.06);
}
.products-filter-title {
  font-family: var(--ag-font-body);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--ag-text-primary);
}
.products-filter-reset {
  font-size: 12px;
  color: var(--ag-primary-500);
  background: none;
  border: none;
  cursor: pointer;
  font-weight: 600;
  font-family: var(--ag-font-body);
}
.products-filter-reset:hover { opacity: 0.7; }

.products-filter-group {
  margin-bottom: 24px;
}
.products-filter-group:last-child { margin-bottom: 0; }
.products-filter-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--ag-text-primary);
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: var(--ag-font-body);
}

.products-search-input {
  width: 100%;
  height: 36px;
  padding: 0 12px;
  border: 1px solid var(--ag-border);
  border-radius: 8px;
  font-size: 13px;
  color: var(--ag-text-primary);
  background: var(--ag-bg);
  outline: none;
  transition: all 0.2s;
  font-family: var(--ag-font-body);
  box-sizing: border-box;
}
.products-search-input:focus {
  border-color: var(--ag-primary-500);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ag-primary-500) 10%, transparent);
  background: white;
}

.products-filter-options {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.products-filter-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 7px 10px;
  border: none;
  background: transparent;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: var(--ag-text-secondary);
  text-align: left;
  transition: all 0.2s;
  font-family: var(--ag-font-body);
}
.products-filter-chip:hover {
  background: color-mix(in srgb, var(--ag-primary-500) 6%, transparent);
  color: var(--ag-primary-500);
}
.products-filter-chip-active {
  background: color-mix(in srgb, var(--ag-primary-500) 8%, transparent);
  color: var(--ag-primary-500);
  font-weight: 600;
}
.products-chip-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--ag-neutral-300);
  flex-shrink: 0;
  transition: background 0.2s;
}
.products-filter-chip-active .products-chip-dot {
  background: var(--ag-primary-500);
}
.products-filter-chip:hover .products-chip-dot {
  background: var(--ag-primary-500);
}

.products-price-presets {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 8px;
}
.products-price-chip {
  padding: 4px 10px;
  border-radius: 6px;
  border: 1px solid var(--ag-border);
  background: white;
  font-size: 11px;
  color: var(--ag-text-secondary);
  cursor: pointer;
  transition: all 0.2s;
  font-family: var(--ag-font-body);
  font-weight: 500;
}
.products-price-chip:hover {
  border-color: var(--ag-primary-500);
  color: var(--ag-primary-500);
}
.products-price-chip-active {
  background: color-mix(in srgb, var(--ag-primary-500) 8%, transparent);
  border-color: var(--ag-primary-500);
  color: var(--ag-primary-500);
  font-weight: 600;
}

.products-price-inputs {
  display: flex;
  align-items: center;
  gap: 6px;
}
.products-price-input {
  width: 100%;
  height: 34px;
  padding: 0 10px;
  border: 1px solid var(--ag-border);
  border-radius: 6px;
  font-size: 12px;
  color: var(--ag-text-primary);
  background: var(--ag-bg);
  outline: none;
  transition: border-color 0.2s;
  font-family: var(--ag-font-body);
  box-sizing: border-box;
}
.products-price-input:focus {
  border-color: var(--ag-primary-500);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ag-primary-500) 10%, transparent);
}
.products-price-sep {
  color: var(--ag-neutral-300);
  font-size: 12px;
  flex-shrink: 0;
}
.products-filter-apply {
  width: 100%;
  margin-top: 8px;
  height: 32px;
  border-radius: 6px;
  border: none;
  background: var(--ag-primary-500);
  color: white;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-family: var(--ag-font-body);
}
.products-filter-apply:hover {
  background: var(--ag-primary-600);
}

.products-filter-row {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 8px;
  transition: all 0.2s;
}
.products-filter-row:hover {
  background: color-mix(in srgb, var(--ag-primary-500) 6%, transparent);
}
.products-filter-row-active {
  background: color-mix(in srgb, var(--ag-primary-500) 8%, transparent);
}
.products-radio {
  width: 15px;
  height: 15px;
  accent-color: var(--ag-primary-500);
  margin: 0;
  flex-shrink: 0;
}
.products-radio-label {
  font-size: 13px;
  color: var(--ag-text-secondary);
  transition: color 0.2s;
  font-family: var(--ag-font-body);
}
.products-filter-row-active .products-radio-label {
  color: var(--ag-primary-500);
  font-weight: 600;
}
.products-filter-row:hover .products-radio-label {
  color: var(--ag-primary-500);
}

.products-main {
  flex: 1;
  min-width: 0;
}

.products-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(116, 121, 108, 0.06);
}
.products-toolbar-left {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}
.products-active-tag {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 4px 10px;
  background: color-mix(in srgb, var(--ag-primary-500) 10%, transparent);
  color: var(--ag-primary-500);
  font-size: 11px;
  font-weight: 600;
  border-radius: 9999px;
  font-family: var(--ag-font-body);
}
.products-active-close {
  background: none;
  border: none;
  cursor: pointer;
  color: inherit;
  display: flex;
  padding: 0;
}
.products-result-count {
  font-size: 12px;
  color: var(--ag-text-secondary);
  font-family: var(--ag-font-body);
}
.products-toolbar-right {
  display: flex;
  align-items: center;
  gap: 6px;
}
.products-sort-label {
  font-size: 12px;
  color: var(--ag-text-secondary);
  white-space: nowrap;
  font-family: var(--ag-font-body);
}
.products-sort-select {
  height: 36px;
  padding: 0 30px 0 10px;
  border: 1px solid var(--ag-border);
  border-radius: 8px;
  font-size: 12px;
  color: var(--ag-text-primary);
  background: white;
  outline: none;
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%2374796c' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
  min-width: 120px;
  font-family: var(--ag-font-body);
}
@media (max-width: 640px) {
  .products-sort-select { min-width: 100px; font-size: 11px; }
  .products-sort-label { display: none; }
  .products-toolbar-left { width: 100%; }
}
.products-sort-select:focus {
  border-color: var(--ag-primary-500);
}

.products-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
@media (min-width: 640px) {
  .products-grid { grid-template-columns: repeat(2, 1fr); gap: 20px; }
}
@media (min-width: 1024px) {
  .products-grid { grid-template-columns: repeat(3, 1fr); gap: 24px; }
}

.products-grid .product-card:nth-child(even) {
  margin-top: 24px;
}
@media (min-width: 640px) {
  .products-grid .product-card:nth-child(even) {
    margin-top: 36px;
  }
}
@media (min-width: 1024px) {
  .products-grid .product-card:nth-child(even) {
    margin-top: 0;
  }
  .products-grid .product-card:nth-child(3n+2) {
    margin-top: 28px;
  }
}

.product-card {
  background: white;
  border-radius: 14px;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(116, 121, 108, 0.08);
}
.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px -8px rgba(44, 44, 44, 0.06);
  border-color: color-mix(in srgb, var(--ag-primary-500) 20%, transparent);
}
.product-card-link {
  text-decoration: none;
  display: flex;
  flex-direction: column;
  height: 100%;
}
.product-card-img {
  position: relative;
  aspect-ratio: 4 / 5;
  overflow: hidden;
  background: var(--ag-bg);
}
.product-card-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}
.product-card:hover .product-card-placeholder {
  transform: scale(1.06);
}
.product-card-real-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}
.product-card:hover .product-card-real-img {
  transform: scale(1.06);
}
.product-card-letter {
  font-family: var(--ag-font-display);
  font-size: 48px;
  font-weight: 500;
  color: var(--ag-neutral-300);
}
.product-card-badges {
  position: absolute;
  top: 10px;
  left: 10px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.product-badge-passport {
  padding: 2px 8px;
  background: rgba(217, 119, 6, 0.16);
  border: 1px solid rgba(217, 119, 6, 0.35);
  color: #b45309;
  border-radius: 9999px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.03em;
  backdrop-filter: blur(4px);
  font-family: var(--ag-font-body);
}
.product-badge-discount {
  padding: 2px 8px;
  background: rgba(139, 79, 39, 0.12);
  color: var(--ag-secondary-500);
  border-radius: 9999px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.03em;
  backdrop-filter: blur(4px);
  font-family: var(--ag-font-body);
}
.product-badge-3d {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: rgba(255,255,255,0.85);
  border: 1px solid rgba(116, 121, 108, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ag-primary-500);
  backdrop-filter: blur(4px);
}
.product-badge-soldout {
  position: absolute;
  bottom: 10px;
  left: 10px;
  padding: 2px 10px;
  background: rgba(0,0,0,0.55);
  color: white;
  border-radius: 9999px;
  font-size: 10px;
  font-weight: 600;
  backdrop-filter: blur(4px);
  font-family: var(--ag-font-body);
}
.product-card-body {
  padding: 14px 16px 16px;
  display: flex;
  flex-direction: column;
  flex: 1;
}
@media (max-width: 640px) {
  .product-card-body { padding: 10px 12px 12px; }
  .product-card-name { font-size: 13px; }
  .product-card-price { font-size: 12px; }
  .product-card-desc { font-size: 11px; }
}
.product-card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 4px;
}
.product-card-name {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.3;
  color: var(--ag-text-primary);
  transition: color 0.2s;
  font-family: var(--ag-font-body);
}
.product-card:hover .product-card-name {
  color: var(--ag-primary-500);
}
.product-card-price {
  font-size: 14px;
  font-weight: 700;
  color: var(--ag-primary-500);
  white-space: nowrap;
  flex-shrink: 0;
  font-family: var(--ag-font-body);
}
.product-card-desc {
  font-size: 12px;
  line-height: 16px;
  color: var(--ag-text-secondary);
  margin-bottom: auto;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-family: var(--ag-font-body);
}
.product-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid rgba(116, 121, 108, 0.06);
}
.product-card-store {
  font-size: 11px;
  color: var(--ag-text-secondary);
  font-family: var(--ag-font-body);
}
.product-card-add {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: none;
  background: color-mix(in srgb, var(--ag-primary-500) 8%, transparent);
  color: var(--ag-primary-500);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s;
  flex-shrink: 0;
}
.product-card-add:hover {
  background: var(--ag-primary-500);
  color: white;
  transform: scale(1.05);
}
.product-card-add:active {
  transform: scale(0.92);
}

.products-empty {
  text-align: center;
  padding: 80px 24px;
  background: white;
  border-radius: 16px;
  border: 1px solid rgba(116, 121, 108, 0.06);
}
.products-empty-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--ag-neutral-100);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 14px;
  color: var(--ag-neutral-400);
}
.products-empty-title {
  font-family: var(--ag-font-display);
  font-size: 20px;
  font-weight: 500;
  color: var(--ag-text-primary);
  margin-bottom: 6px;
}
.products-empty-desc {
  font-size: 13px;
  color: var(--ag-text-secondary);
  margin-bottom: 20px;
  font-family: var(--ag-font-body);
}
.products-empty-btn {
  padding: 8px 24px;
  border-radius: 8px;
  border: none;
  background: var(--ag-primary-500);
  color: white;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-family: var(--ag-font-body);
}
.products-empty-btn:hover {
  background: var(--ag-primary-600);
}

.products-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 4px;
  margin-top: 40px;
}
.pagination-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid rgba(116, 121, 108, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ag-text-secondary);
  text-decoration: none;
  transition: all 0.2s;
  font-size: 16px;
}
.pagination-btn:hover {
  background: var(--ag-surface-container);
  border-color: var(--ag-primary-500);
  color: var(--ag-primary-500);
}
.pagination-page {
  min-width: 34px;
  height: 34px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: var(--ag-text-secondary);
  text-decoration: none;
  border: 1px solid transparent;
  transition: all 0.2s;
  font-family: var(--ag-font-body);
}
.pagination-page:hover {
  background: var(--ag-surface-container);
  border-color: rgba(116, 121, 108, 0.15);
}
.pagination-active {
  background: var(--ag-primary-500);
  color: white;
  border-color: var(--ag-primary-500);
}
.pagination-active:hover {
  background: var(--ag-primary-600);
}
.pagination-dots {
  font-size: 12px;
  color: var(--ag-text-secondary);
  padding: 0 2px;
  font-family: var(--ag-font-body);
}
</style>

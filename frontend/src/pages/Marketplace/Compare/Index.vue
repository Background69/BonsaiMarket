<template>
  <MarketplaceLayout>
    <div style="padding-top: 120px; max-width: var(--ag-container-max, 1280px); margin: 0 auto; padding-left: var(--ag-margin-desktop, 64px); padding-right: var(--ag-margin-desktop, 64px); padding-bottom: 80px;">
      <header style="margin-bottom: 64px;">
        <span style="font-family: var(--ag-font-body); font-size: 12px; font-weight: 600; letter-spacing: 0.1em; text-transform: uppercase; color: var(--ag-primary-500); margin-bottom: 16px; display: block;">So sánh sản phẩm</span>
        <h1 style="font-family: var(--ag-font-display); font-size: clamp(2.25rem, 1.7rem + 1.4vw, 4rem); font-weight: 500; line-height: 1.1; letter-spacing: -0.02em; color: var(--ag-text-primary); margin-bottom: 12px;">Đối chiếu thông tin</h1>
        <p style="font-family: var(--ag-font-body); font-size: 18px; line-height: 28px; color: var(--ag-text-secondary);">So sánh chi tiết giữa các sản phẩm để chọn lựa phù hợp nhất.</p>
      </header>

      <div v-if="!products.length" class="compare-empty">
        <div class="compare-empty-icon">
          <span class="material-symbols-outlined" style="font-size: 40px;">compare_arrows</span>
        </div>
        <h2 class="compare-empty-title">Chưa có sản phẩm để so sánh</h2>
        <p class="compare-empty-desc">Thêm sản phẩm vào danh sách so sánh bằng cách nhấn nút <strong>So sánh</strong> trên sản phẩm.</p>
        <Link :href="route('agriverse.shop.products.index')" class="compare-empty-btn">Khám phá sản phẩm</Link>
      </div>

      <div v-else class="compare-grid">
        <div class="compare-cards">
          <div v-for="p in products" :key="p.id" class="compare-card">
            <button @click="removeProduct(p.id)" class="compare-card-close" title="Xoá khỏi so sánh">
              <span class="material-symbols-outlined">close</span>
            </button>
            <div class="compare-card-visual">
              <img v-if="p.image" :src="p.image" :alt="p.name" class="compare-card-img" />
              <span v-else class="compare-card-char">{{ p.name?.charAt(0)?.toUpperCase() || 'P' }}</span>
              <span v-if="p.compare_price" class="compare-card-badge">-{{ discountPercent(p) }}%</span>
              <span v-if="p.has_approved_passport === false" class="compare-card-passport" title="Cây chưa được cấp hộ chiếu NFT">Chưa có hộ chiếu</span>
            </div>
            <h3 class="compare-card-name">{{ p.name }}</h3>
            <div class="compare-card-price">
              <span class="compare-card-current">{{ formatPrice(p.price) }}₫</span>
              <span v-if="p.compare_price" class="compare-card-old">{{ formatPrice(p.compare_price) }}₫</span>
            </div>
            <span class="compare-card-stock" :class="p.stock > 0 ? 'in-stock' : 'out-of-stock'">
              {{ p.stock > 0 ? 'Còn hàng' : 'Hết hàng' }}
            </span>
            <button @click="addToCart(p)" class="compare-card-cart">Thêm vào giỏ</button>
          </div>
        </div>

        <div class="compare-specs">
          <!-- Thông tin chung -->
          <div class="compare-spec-divider">Thông tin chung</div>
          <div v-if="hasStoreName" class="compare-spec-row" :style="specGridStyle">
            <div class="compare-spec-label">Gian hàng</div>
            <div v-for="p in products" :key="p.id" class="compare-spec-value">{{ p.store_name || '—' }}</div>
          </div>
          <div v-if="hasCategory" class="compare-spec-row" :style="specGridStyle">
            <div class="compare-spec-label">Danh mục</div>
            <div v-for="p in products" :key="p.id" class="compare-spec-value">{{ p.category || '—' }}</div>
          </div>
          <div class="compare-spec-row" :style="specGridStyle">
            <div class="compare-spec-label">Trạng thái</div>
            <div v-for="p in products" :key="p.id" class="compare-spec-value">
              <span :class="p.status === 'published' ? 'tag-active' : 'tag-inactive'">
                {{ statusLabel(p.status) }}
              </span>
            </div>
          </div>
          <div class="compare-spec-row" :style="specGridStyle">
            <div class="compare-spec-label">Nổi bật</div>
            <div v-for="p in products" :key="p.id" class="compare-spec-value">
              <span :class="p.is_featured ? 'tag-active' : 'tag-inactive'">
                {{ p.is_featured ? 'Có' : 'Không' }}
              </span>
            </div>
          </div>
          <div v-if="hasTags" class="compare-spec-row" :style="specGridStyle">
            <div class="compare-spec-label">Thẻ</div>
            <div v-for="p in products" :key="p.id" class="compare-spec-value">
              <div class="tag-group">
                <span v-if="p.tags?.length" v-for="tag in p.tags" :key="tag" class="tag">{{ tag }}</span>
                <span v-else>—</span>
              </div>
            </div>
          </div>

          <!-- Giá & Kho -->
          <div class="compare-spec-divider">Giá & Kho</div>
          <div class="compare-spec-row" :style="specGridStyle">
            <div class="compare-spec-label">Giá bán</div>
            <div v-for="p in products" :key="p.id" class="compare-spec-value"><strong>{{ formatPrice(p.price) }}₫</strong></div>
          </div>
          <div class="compare-spec-row" :style="specGridStyle">
            <div class="compare-spec-label">Giá gốc</div>
            <div v-for="p in products" :key="p.id" class="compare-spec-value">
              <span v-if="p.compare_price">{{ formatPrice(p.compare_price) }}₫</span>
              <span v-else class="muted">—</span>
            </div>
          </div>
          <div class="compare-spec-row" :style="specGridStyle">
            <div class="compare-spec-label">Giảm giá</div>
            <div v-for="p in products" :key="p.id" class="compare-spec-value">
              <span v-if="p.compare_price" class="text-danger">{{ discountPercent(p) }}%</span>
              <span v-else class="muted">—</span>
            </div>
          </div>
          <div class="compare-spec-row" :style="specGridStyle">
            <div class="compare-spec-label">Tồn kho</div>
            <div v-for="p in products" :key="p.id" class="compare-spec-value">{{ p.stock }}</div>
          </div>
          <div v-if="hasVariants" class="compare-spec-row" :style="specGridStyle">
            <div class="compare-spec-label">Có biến thể</div>
            <div v-for="p in products" :key="p.id" class="compare-spec-value">{{ p.has_variants ? 'Có' : 'Không' }}</div>
          </div>
          <div v-if="hasSoldCount" class="compare-spec-row" :style="specGridStyle">
            <div class="compare-spec-label">Đã bán</div>
            <div v-for="p in products" :key="p.id" class="compare-spec-value">{{ p.sold_count || 0 }}</div>
          </div>

          <!-- Mô tả & Đánh giá -->
          <div v-if="hasDescription || hasRating" class="compare-spec-divider">Mô tả & Đánh giá</div>
          <div v-if="hasDescription" class="compare-spec-row" :style="specGridStyle">
            <div class="compare-spec-label">Mô tả</div>
            <div v-for="p in products" :key="p.id" class="compare-spec-value compare-value-desc">{{ truncate(p.description, 150) || '—' }}</div>
          </div>
          <div v-if="hasRating" class="compare-spec-row" :style="specGridStyle">
            <div class="compare-spec-label">Đánh giá</div>
            <div v-for="p in products" :key="p.id" class="compare-spec-value">
              <span v-if="p.avg_rating">⭐ {{ p.avg_rating }} ({{ p.reviews_count }} đánh giá)</span>
              <span v-else class="muted">Chưa có đánh giá</span>
            </div>
          </div>

          <!-- Liên kết -->
          <div v-if="hasManufacturer || hasProductType || hasModel3d" class="compare-spec-divider">Liên kết</div>
          <div v-if="hasManufacturer" class="compare-spec-row" :style="specGridStyle">
            <div class="compare-spec-label">Nhà sản xuất</div>
            <div v-for="p in products" :key="p.id" class="compare-spec-value">{{ p.manufacturer_name || '—' }}</div>
          </div>
          <div v-if="hasProductType" class="compare-spec-row" :style="specGridStyle">
            <div class="compare-spec-label">Loại sản phẩm</div>
            <div v-for="p in products" :key="p.id" class="compare-spec-value">{{ p.product_type_name || '—' }}</div>
          </div>
          <div v-if="hasModel3d" class="compare-spec-row" :style="specGridStyle">
            <div class="compare-spec-label">Mô hình 3D</div>
            <div v-for="p in products" :key="p.id" class="compare-spec-value">
              <span v-if="p.model_3d_url" class="tag-active">Có</span>
              <span v-else class="muted">Không</span>
            </div>
          </div>

          <!-- Thông số kỹ thuật -->
          <template v-if="specs.length">
            <div class="compare-spec-divider">Thông số kỹ thuật</div>
            <div v-for="spec in specs" :key="spec" class="compare-spec-row" :style="specGridStyle">
              <div class="compare-spec-label">{{ specLabel(spec) }}</div>
              <div v-for="p in products" :key="p.id" class="compare-spec-value">{{ specValue(p, spec) }}</div>
            </div>
          </template>

          <!-- Thông tin bổ sung -->
          <template v-if="hasMetadata">
            <div class="compare-spec-divider">Thông tin bổ sung</div>
            <div v-for="(_, key) in allMetaKeys" :key="key" class="compare-spec-row" :style="specGridStyle">
              <div class="compare-spec-label">{{ metaLabel(key) }}</div>
              <div v-for="p in products" :key="p.id" class="compare-spec-value">{{ metaValue(p, key) }}</div>
            </div>
          </template>
        </div>
      </div>
    </div>
  </MarketplaceLayout>
</template>

<script setup>
import { computed, onMounted } from 'vue';
import { Link, router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';
import { formatPrice } from '@agriverse/utils';
import { useToast } from 'primevue/usetoast';
import { useCompare } from '@agriverse/Composables/useCompare';

const toast = useToast();
const { compareIds, remove } = useCompare();

const props = defineProps({
  products: { type: Array, default: () => [] },
  specs: { type: Array, default: () => [] },
});

onMounted(() => {
  if (!props.products.length && compareIds.value.length >= 2) {
    router.get(route('agriverse.shop.compare.index', { ids: compareIds.value.join(',') }), {}, {
      preserveState: false,
    });
  }
});

const specGridStyle = computed(() => ({
  gridTemplateColumns: `140px repeat(${props.products.length}, 1fr)`,
}));

/* --- Visibility computed --- */
const hasStoreName = computed(() => props.products.some(p => p.store_name));
const hasCategory = computed(() => props.products.some(p => p.category));
const hasTags = computed(() => props.products.some(p => p.tags?.length));
const hasVariants = computed(() => props.products.some(p => p.has_variants));
const hasSoldCount = computed(() => props.products.some(p => p.sold_count));
const hasDescription = computed(() => props.products.some(p => p.description));
const hasRating = computed(() => props.products.some(p => p.avg_rating));
const hasManufacturer = computed(() => props.products.some(p => p.manufacturer_name));
const hasProductType = computed(() => props.products.some(p => p.product_type_name));
const hasModel3d = computed(() => props.products.some(p => p.model_3d_url));
const hasMetadata = computed(() => props.products.some(p => p.metadata && Object.keys(p.metadata).length));
const allMetaKeys = computed(() => {
  const keys = new Set();
  props.products.forEach(p => {
    if (p.metadata) Object.keys(p.metadata).forEach(k => keys.add(k));
  });
  return Array.from(keys);
});

/* --- Helpers --- */
function statusLabel(status) {
  const map = { published: 'Đã xuất bản', draft: 'Nháp', archived: 'Lưu trữ' };
  return map[status] || status;
}

function discountPercent(product) {
  if (!product.compare_price) return 0;
  return Math.round((1 - product.price / product.compare_price) * 100);
}

function specLabel(key) {
  const labels = {
    height: 'Chiều cao', width: 'Chiều rộng', weight: 'Cân nặng',
    color: 'Màu sắc', material: 'Chất liệu', origin: 'Nguồn gốc',
    light: 'Ánh sáng', water: 'Tưới nước', temperature: 'Nhiệt độ',
    humidity: 'Độ ẩm', fertilizer: 'Phân bón', lifespan: 'Tuổi thọ',
    difficulty: 'Độ khó', pot_size: 'Kích thước chậu',
  };
  return labels[key] || key;
}

function specValue(product, key) {
  if (!product.technical_specs) return '—';
  const val = product.technical_specs[key];
  if (val === null || val === undefined) return '—';
  return String(val);
}

function metaLabel(key) {
  const labels = {
    brand: 'Thương hiệu', origin: 'Xuất xứ', material: 'Chất liệu',
    warranty: 'Bảo hành', size: 'Kích thước', color: 'Màu sắc',
    age: 'Tuổi', height: 'Chiều cao', pot_included: 'Bao gồm chậu',
  };
  return labels[key] || key;
}

function metaValue(product, key) {
  if (!product.metadata) return '—';
  const val = product.metadata[key];
  if (val === null || val === undefined) return '—';
  return String(val);
}

function truncate(text, len) {
  if (!text) return '';
  return text.length > len ? text.substring(0, len) + '...' : text;
}

function removeProduct(id) {
  remove(id);
  router.get(route('agriverse.shop.compare.index', { ids: getRemainingIds(id) }), {
    preserveState: true,
    preserveScroll: true,
  });
}

function addToCart(product) {
  router.post(route('agriverse.api.cart.add'), { product_id: product.id, quantity: 1 }, {
    preserveState: true,
    preserveScroll: true,
    onSuccess: () => toast.add({ severity: 'success', summary: 'Đã thêm vào giỏ hàng', life: 2000 }),
    onError: () => toast.add({ severity: 'error', summary: 'Thêm thất bại', life: 2000 }),
  });
}

function getRemainingIds(removedId) {
  const ids = props.products.filter(p => p.id !== removedId).map(p => p.id);
  return ids.join(',');
}
</script>

<style scoped>
.compare-empty {
  text-align: center;
  padding: 80px 24px;
}
.compare-empty-icon {
  width: 80px; height: 80px;
  border-radius: 50%;
  background: rgba(72, 103, 48, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
  color: var(--ag-primary-500);
}
.compare-empty-title {
  font-family: var(--ag-font-display);
  font-size: 28px;
  font-weight: 500;
  color: var(--ag-text-primary);
  margin-bottom: 8px;
}
.compare-empty-desc {
  font-family: var(--ag-font-body);
  font-size: 16px;
  line-height: 24px;
  color: var(--ag-text-secondary);
  max-width: 400px;
  margin: 0 auto 32px;
}
.compare-empty-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 14px 32px;
  background: var(--ag-primary-500);
  color: white;
  border-radius: 9999px;
  font-family: var(--ag-font-body);
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.3s;
}
.compare-empty-btn:hover { background: var(--ag-primary-600); transform: translateY(-1px); }

.compare-grid {
  display: flex;
  flex-direction: column;
  gap: 48px;
}

/* Product cards row */
.compare-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}
.compare-card {
  background: white;
  border: 1px solid var(--ag-border);
  border-radius: 16px;
  padding: 32px 24px 24px;
  text-align: center;
  position: relative;
  transition: box-shadow 0.3s;
}
.compare-card:hover {
  box-shadow: 0 8px 30px rgba(0,0,0,0.06);
}
.compare-card-close {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 32px; height: 32px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: var(--ag-text-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.compare-card-close:hover {
  background: color-mix(in srgb, var(--ag-danger) 10%, transparent);
  color: var(--ag-danger);
}
.compare-card-close .material-symbols-outlined { font-size: 18px; }
.compare-card-visual {
  position: relative;
  width: 100%;
  max-width: 160px;
  aspect-ratio: 4/5;
  margin: 0 auto 20px;
  background: var(--ag-surface-container);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.compare-card-char {
  font-family: var(--ag-font-display);
  font-size: 36px;
  color: rgba(116, 121, 108, 0.15);
}
.compare-card-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.compare-card-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 4px 10px;
  background: var(--ag-danger);
  color: white;
  border-radius: 6px;
  font-family: var(--ag-font-body);
  font-size: 11px;
  font-weight: 700;
}
.compare-card-passport {
  position: absolute;
  top: 8px;
  right: 8px;
  padding: 4px 10px;
  background: rgba(217, 119, 6, 0.92);
  color: white;
  border-radius: 6px;
  font-family: var(--ag-font-body);
  font-size: 11px;
  font-weight: 700;
}
.compare-card-name {
  font-family: var(--ag-font-display);
  font-size: 20px;
  font-weight: 500;
  color: var(--ag-text-primary);
  margin-bottom: 12px;
  line-height: 1.3;
}
.compare-card-price {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 12px;
}
.compare-card-current {
  font-family: var(--ag-font-body);
  font-size: 22px;
  font-weight: 700;
  color: var(--ag-danger);
}
.compare-card-old {
  font-family: var(--ag-font-body);
  font-size: 14px;
  color: var(--ag-text-muted);
  text-decoration: line-through;
}
.compare-card-stock {
  display: inline-block;
  font-family: var(--ag-font-body);
  font-size: 12px;
  font-weight: 700;
  padding: 4px 14px;
  border-radius: 8px;
  margin-bottom: 16px;
}
.compare-card-stock.in-stock {
  background: rgba(72, 103, 48, 0.08);
  color: var(--ag-primary-600);
}
.compare-card-stock.out-of-stock {
  background: rgba(220, 53, 69, 0.08);
  color: var(--ag-danger);
}
.compare-card-cart {
  width: 100%;
  padding: 12px;
  background: var(--ag-primary-500);
  color: white;
  border: none;
  border-radius: 10px;
  font-family: var(--ag-font-body);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}
.compare-card-cart:hover { background: var(--ag-primary-600); }

/* Specs grid */
.compare-specs {
  background: white;
  border: 1px solid var(--ag-border);
  border-radius: 16px;
  overflow: hidden;
}
.compare-spec-divider {
  font-family: var(--ag-font-body);
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--ag-text-muted);
  padding: 24px 20px 8px;
  background: var(--ag-surface-container);
  border-bottom: 1px solid var(--ag-border);
}
.compare-spec-row {
  display: grid;
  grid-template-columns: 140px 1fr 1fr;
  border-bottom: 1px solid var(--ag-border);
}
.compare-spec-row:last-child { border-bottom: none; }
.compare-spec-label {
  font-family: var(--ag-font-body);
  font-size: 13px;
  font-weight: 600;
  color: var(--ag-text-secondary);
  padding: 14px 20px;
  background: var(--ag-surface-container);
  display: flex;
  align-items: center;
}
.compare-spec-value {
  font-family: var(--ag-font-body);
  font-size: 14px;
  color: var(--ag-text-primary);
  padding: 14px 20px;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
}
.compare-value-desc {
  text-align: left;
  justify-content: flex-start;
  line-height: 1.5;
}

.muted { color: var(--ag-text-muted); }
.text-danger { color: var(--ag-danger); font-weight: 700; }

.tag-active {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 6px;
  background: rgba(72, 103, 48, 0.08);
  color: var(--ag-primary-600);
  font-family: var(--ag-font-body);
  font-size: 12px;
  font-weight: 600;
}
.tag-inactive {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 6px;
  background: var(--ag-surface-container);
  color: var(--ag-text-muted);
  font-family: var(--ag-font-body);
  font-size: 12px;
  font-weight: 600;
}
.tag-group {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  justify-content: center;
}
.tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  background: rgba(72, 103, 48, 0.06);
  color: var(--ag-text-secondary);
  font-family: var(--ag-font-body);
  font-size: 11px;
  font-weight: 600;
}

@media (max-width: 768px) {
  .compare-cards { gap: 16px; }
  .compare-card { padding: 24px 16px 20px; }
  .compare-card-visual { max-width: 120px; }
  .compare-spec-row { grid-template-columns: 100px 1fr 1fr; }
  .compare-spec-label { padding: 12px 14px; font-size: 12px; }
  .compare-spec-value { padding: 12px 14px; font-size: 13px; }
}
</style>

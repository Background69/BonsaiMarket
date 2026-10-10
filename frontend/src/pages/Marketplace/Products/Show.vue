<template>
  <MarketplaceLayout>
    <main class="market-container market-page">
      <div class="breadcrumbs">
        <Link href="/">Trang chủ</Link>
        <span>/</span>
        <Link href="/products">Sản phẩm</Link>
        <span>/</span><span>{{ product.name }}</span></div>
      <section class="product-detail-grid">
        <div class="product-detail-media">
          <CatalogImage :alt="product.name" :src="product.image || productVisuals[product.id]" priority/>
          <p class="image-note">Ảnh sản phẩm do gian hàng cung cấp.</p></div>
        <div class="product-detail-summary"><p class="detail-category">{{ product.category }}</p>
          <h1>{{ product.name }}</h1>
          <p class="detail-price">{{ formatPrice(product.price) }} ₫</p><span class="status-badge">{{
              product.stock > 0 ? 'Còn ' + product.stock + ' sản phẩm' : 'Tạm hết hàng'
            }}</span>
          <p class="detail-excerpt">{{ cleanDescription?.slice(0, 220) }}</p>
          <div class="purchase-preview"><label for="detail-quantity">Số lượng</label>
            <div class="quantity-preview">
              <button :disabled="quantity<=1 || !product.stock" aria-label="Giảm số lượng" @click="decrementQty">−
              </button>
              <input id="detail-quantity" :value="quantity" readonly type="number"/>
              <button :disabled="quantity>=product.stock || !product.stock" aria-label="Tăng số lượng"
                      @click="incrementQty">+
              </button>
            </div>
            <div class="button-row">
              <button class="button button-primary" disabled @click="addToCart">Thêm vào giỏ</button>
              <button class="button button-outline" disabled @click="buyNow">Mua ngay</button>
            </div>
            <p class="development-note">Giỏ hàng, đặt hàng và thanh toán đang được phát triển. Hiện có thể xem sản phẩm
              và gian hàng.</p></div>
          <SellerIdentityCard :store="product.store || {}"/>
        </div>
      </section>
      <section class="market-panel detail-description"><h2>Mô tả sản phẩm</h2>
        <p class="description-text">{{ cleanDescription || 'Gian hàng chưa bổ sung mô tả.' }}</p>
        <div v-if="identitySpecs.length || careSpecs.length" class="plant-specs">
          <dl v-for="(spec,i) in [...identitySpecs,...careSpecs]" :key="i">
            <dt>{{ spec.label }}</dt>
            <dd>{{ spec.value }}</dd>
          </dl>
        </div>
      </section>
      <section v-if="reviews.length" class="market-section"><h2>Đánh giá sản phẩm</h2>
        <article v-for="review in reviews" :key="review.id" class="market-panel">
          <strong>{{ review.user?.name || 'Ẩn danh' }}</strong>
          <p>{{ review.rating }}/5</p>
          <p>{{ review.comment }}</p></article>
      </section>
      <section v-if="relatedProducts.length" class="market-section">
        <div class="section-heading"><h2>Sản phẩm khác từ cùng gian hàng</h2>
          <Link :href="'/stores/'+product.store.id" class="text-link">Xem gian hàng</Link>
        </div>
        <div class="market-product-grid">
          <ProductCard v-for="p in relatedProducts" :key="p.id" :product="p"/>
        </div>
      </section>
    </main>
  </MarketplaceLayout>
</template>
<script setup>
import CatalogImage from '../../../components/marketplace/CatalogImage.vue';
import SellerIdentityCard from '../../../components/marketplace/SellerIdentityCard.vue';
import {ref, computed, onMounted, nextTick, onBeforeUnmount} from 'vue';
import {Link, router, usePage} from '@inertiajs/vue3';
import {route} from 'ziggy-js';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';
import ProductCard from '@agriverse/Components/ProductCard.vue';
import HomeImage from '../../../components/home/HomeImage.vue';
import {homeVisuals, productVisuals} from '../../../config/homeVisuals.js';
import {parsePlantDescription} from '@agriverse/utils';
import {useToast} from 'primevue/usetoast';
import {useChat} from '@agriverse/Composables/useChat';
import {useCompare} from '@agriverse/Composables/useCompare';
// Advanced 3D/AR views remain in the repository and will be activated later.
const ARViewer = null;
const Model3DViewer = null;

const toast = useToast();
const page = usePage();
const {openPanel} = useChat();
const {isComparing, toggle: toggleCompareId} = useCompare();
const isAuthenticated = computed(() => !!page.props.auth?.user);

const props = defineProps({
  product: {type: Object, default: () => ({})},
  relatedProducts: {type: Array, default: () => []},
});

const carouselRef = ref(null);
const carouselAtStart = ref(true);
const carouselAtEnd = ref(false);

function scrollCarousel(dir) {
  if (!carouselRef.value) return;
  const scrollAmount = carouselRef.value.querySelector('.carousel-item')?.offsetWidth + 24 || 320;
  carouselRef.value.scrollBy({left: dir * scrollAmount, behavior: 'smooth'});
}

function onCarouselScroll() {
  if (!carouselRef.value) return;
  const el = carouselRef.value;
  carouselAtStart.value = el.scrollLeft <= 4;
  carouselAtEnd.value = el.scrollLeft + el.clientWidth >= el.scrollWidth - 4;
}

const quantity = ref(1);
const arModalOpen = ref(false);
const arModelUrl = computed(() => props.product?.model_3d_url || '');
const reviews = computed(() => props.product?.reviews || []);
const averageRating = computed(() => {
  const r = reviews.value;
  if (!r.length) return 0;
  return (r.reduce((s, r) => s + (r.rating || 0), 0) / r.length).toFixed(1);
});

const discountPercent = computed(() => {
  if (!props.product?.compare_price) return 0;
  return Math.round((1 - props.product.price / props.product.compare_price) * 100);
});

const specIconMap = {
  'Tên khoa học': 'biotech',
  'Họ thực vật': 'category',
  'Nguồn gốc': 'public',
  'Ánh sáng': 'light_mode',
  'Tưới nước': 'water_drop',
  'Phân bón': 'nutrition',
  'Nhiệt độ tối thiểu': 'thermostat',
  'Độ pH đất': 'science',
  'Vị trí': 'home',
};
const identityKeys = ['Tên khoa học', 'Họ thực vật', 'Nguồn gốc'];

function parseSpecs() {
  const desc = props.product?.description || '';
  const parsed = parsePlantDescription(desc);

  if (parsed.identity.length || parsed.care.length) {
    return parsed;
  }

  const specs = props.product?.technical_specs;
  if (specs && typeof specs === 'object') {
    for (const [key, val] of Object.entries(specs)) {
      if (!val) continue;
      const icon = specIconMap[key] || 'info';
      const item = {label: key, value: val, icon};
      if (identityKeys.includes(key)) {
        parsed.identity.push(item);
      } else {
        parsed.care.push(item);
      }
    }
    const meta = props.product?.metadata;
    if (meta?.origin) parsed.identity.push({label: 'Nguồn gốc', value: meta.origin, icon: 'public'});
    if (meta?.fertilizing_guide) parsed.care.push({
      label: 'Phân bón',
      value: meta.fertilizing_guide,
      icon: 'nutrition'
    });
    if (meta?.meaning_fengshui) parsed.fengshui = meta.meaning_fengshui;
  }
  return parsed;
}

const identitySpecs = computed(() => parseSpecs().identity);
const careSpecs = computed(() => parseSpecs().care);
const fengshui = computed(() => parseSpecs().fengshui);
const cleanDescription = computed(() => {
  const parsed = parseSpecs();
  return parsed.cleanDesc || props.product?.description || '';
});

const passportLogs = computed(() => props.product?.passport_logs || []);
const passportCareLogs = computed(() => props.product?.passport?.logs || []);

const showStickyBar = ref(false);
let scrollHandler = null;

const passportQrCanvas = ref(null);
const passportQrUrl = computed(() => props.product?.passport?.lookup_url || '');

function renderPassportQr() {
  if (!passportQrCanvas.value || !passportQrUrl.value) return;
  if (typeof window.QRCode === 'undefined') {
    const s = document.createElement('script');
    s.src = 'https://cdn.jsdelivr.net/npm/qrcode/build/qrcode.min.js';
    s.onload = () => renderPassportQr();
    s.onerror = () => {
    };
    document.head.appendChild(s);
    return;
  }
  window.QRCode.toCanvas(
      passportQrCanvas.value,
      passportQrUrl.value,
      {width: 132, height: 132, margin: 1, color: {dark: '#1f2937', light: '#ffffff'}},
      (err) => {
        if (err) console.error('QR render failed', err);
      }
  );
}

onMounted(() => {
  scrollHandler = () => {
    showStickyBar.value = window.scrollY > 800;
  };
  window.addEventListener('scroll', scrollHandler, {passive: true});
  nextTick(() => {
    onCarouselScroll();
    renderPassportQr();
  });
});

onBeforeUnmount(() => {
  if (scrollHandler) window.removeEventListener('scroll', scrollHandler);
});

function formatPrice(price) {
  return new Intl.NumberFormat('vi-VN').format(price || 0);
}

function incrementQty() {
  quantity.value = Math.min(quantity.value + 1, props.product.stock || 99);
}

function decrementQty() {
  quantity.value = Math.max(1, quantity.value - 1);
}

async function addToCart() {
  window.dispatchEvent(new CustomEvent('bonsai:pending'));
}

const currentUserId = computed(() => page.props.auth?.user?.id ?? null);
const canOffer = computed(() => isAuthenticated.value && props.product?.seller?.id != null && props.product.seller.id !== currentUserId.value);

function goOffer() {
  router.get('/agriverse/de-xuat-gia', {product: props.product.id}, {preserveState: true});
}

async function buyNow() {
  window.dispatchEvent(new CustomEvent('bonsai:pending'));
}

function passportLabel(action) {
  const labels = {
    product_submitted: 'Sản phẩm được tạo',
    product_approved: 'Sản phẩm được duyệt',
    ordered: 'Đơn hàng được đặt',
    order_confirmed: 'Đơn hàng đã được xác nhận',
    order_shipped: 'Đơn hàng đã được giao cho vận chuyển',
    order_delivered: 'Đơn hàng đã được giao thành công',
    order_completed: 'Người mua đã xác nhận nhận hàng',
    order_cancelled: 'Đơn hàng đã bị hủy',
    seed_planted: 'Ươm hạt thành công',
    styled_bonsai: 'Uốn nắn tạo dáng lần 1',
    repotted: 'Sang chậu đất nung',
    certified_healthy: 'Chứng nhận sức khỏe định kỳ',
  };
  return labels[action] || action;
}

function careLogLabel(type) {
  const labels = {
    tuoi: 'Tưới nước',
    'bon-phan': 'Bón phân',
    'cat-tia': 'Cắt tỉa',
    'thay-dat': 'Thay đất',
    'phun-thuoc': 'Phun thuốc',
    other: 'Khác',
  };
  return labels[type] || type;
}

function careLogIcon(type) {
  const icons = {
    tuoi: 'water_drop',
    'bon-phan': 'grass',
    'cat-tia': 'content_cut',
    'thay-dat': 'potted_plant',
    'phun-thuoc': 'medication',
    other: 'notes',
  };
  return icons[type] || 'circle';
}

function passportIcon(action) {
  const icons = {
    product_submitted: 'upload_file',
    product_approved: 'verified',
    ordered: 'shopping_cart',
    order_confirmed: 'check_circle',
    order_shipped: 'local_shipping',
    order_delivered: 'inventory_2',
    order_completed: 'handshake',
    order_cancelled: 'cancel',
    seed_planted: 'eco',
    styled_bonsai: 'content_cut',
    repotted: 'potted_plant',
    certified_healthy: 'health_and_safety',
  };
  return icons[action] || 'circle';
}

async function toggleWishlist() {
  window.dispatchEvent(new CustomEvent('bonsai:pending'));
}

function toggleCompare() {
  window.dispatchEvent(new CustomEvent('bonsai:pending'));
}

const quickReplies = [
  'Cây này còn không?',
  'Giá có giảm được không?',
  'Giao hàng tận nơi không?',
  'Có kèm hướng dẫn chăm sóc không?',
  'Cây đã được kiểm dịch chưa?',
];

async function chatWithSeller(message = '') {
  window.dispatchEvent(new CustomEvent('bonsai:pending'));
}

function openARViewer() {
  window.dispatchEvent(new CustomEvent('bonsai:pending'));
}

function closeARViewer() {
  arModalOpen.value = false;
}
</script>

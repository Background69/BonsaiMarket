<template>
  <header ref="headerRoot" class="market-header public-header" @keydown.esc="closeWithEscape">
    <div class="hfs-utility">
      <div class="hfs-container hfs-utility-inner">
        <span class="hfs-announcement"><MarketIcon name="sprout"/>Bonsai & cây cảnh từ những gian hàng độc lập</span>
        <nav aria-label="Liên kết nhanh" class="hfs-utility-links">
          <RouterLink to="/products">
            <MarketIcon name="leaf"/>
            Khám phá cây cảnh
          </RouterLink>
          <RouterLink to="/stores">
            <MarketIcon name="store"/>
            Gian hàng độc lập
          </RouterLink>
          <button v-if="showAI" type="button" @click="openAI">
            <MarketIcon name="chat"/>
            Trợ lý cây cảnh
          </button>
        </nav>
      </div>
    </div>
    <div class="hfs-container hfs-main">
      <RouterLink aria-label="BonsaiMarket — Trang chủ" class="hfs-brand" to="/">
        <BonsaiMark/>
        <span>BonsaiMarket<small>Nhiều gian hàng · Một đam mê</small></span></RouterLink>
      <form id="marketplace-search-form" class="hfs-search" role="search" @submit.prevent="search">
        <MarketIcon name="search"/>
        <label class="sr-only" for="marketplace-search">Tìm kiếm sản phẩm trên BonsaiMarket</label><input
          id="marketplace-search" v-model="term" placeholder="Tìm kiếm bonsai, cây cảnh, chậu, phụ kiện..."
          type="search"/>
        <button aria-label="Tìm kiếm sản phẩm" type="submit">
          <MarketIcon name="search"/>
        </button>
      </form>
      <div class="hfs-actions">
        <details v-if="authenticated" ref="accountMenu" class="hfs-account">
          <summary aria-label="Mở menu tài khoản">
            <MarketIcon name="user"/>
            <span :title="user?.name">{{ user?.name }}<small>Tài khoản của tôi</small></span></summary>
          <nav aria-label="Tài khoản">
            <RouterLink to="/profile">Tài khoản của tôi</RouterLink>
            <RouterLink to="/orders">Đơn mua</RouterLink>
            <RouterLink v-if="seller" to="/seller">Kênh người bán</RouterLink>
            <RouterLink v-if="user?.role==='admin'" to="/admin">Quản trị sàn</RouterLink>
            <button type="button" @click="$emit('logout')">Đăng xuất</button>
          </nav>
        </details>
        <div v-else class="hfs-guest">
          <RouterLink aria-label="Đăng nhập" class="hfs-user-icon" to="/login">
            <MarketIcon name="user"/>
          </RouterLink>
          <div>
            <RouterLink to="/login">Đăng nhập</RouterLink>
            <RouterLink to="/register">Tạo tài khoản</RouterLink>
          </div>
        </div>
        <span aria-hidden="true" class="hfs-action-divider"></span>
        <RouterLink aria-label="Giỏ hàng — chức năng đang phát triển" class="hfs-cart" title="Giỏ hàng đang phát triển"
                    to="/cart">
          <MarketIcon name="cart"/>
          <span>Giỏ hàng</span><b v-if="Number.isInteger(cartCount) && cartCount>0"
                                  :aria-label="cartCount+' sản phẩm trong giỏ'">{{ cartCount }}</b></RouterLink>
        <button ref="mobileTrigger" :aria-expanded="mobileOpen" :aria-label="mobileOpen?'Đóng menu điều hướng':'Mở menu điều hướng'" aria-controls="hfs-mobile-nav"
                class="hfs-menu-toggle" type="button"
                @click="toggleMobile">
          <MarketIcon :name="mobileOpen?'close':'menu'"/>
        </button>
      </div>
    </div>
    <nav aria-label="Điều hướng marketplace" class="hfs-container hfs-nav">
      <RouterLink to="/products">
        <MarketIcon name="grid"/>
        Sản phẩm
      </RouterLink>
      <div ref="categoryRoot" class="hfs-category-nav">
        <RouterLink to="/categories">
          <MarketIcon name="list"/>
          Danh mục
        </RouterLink>
        <button ref="categoryTrigger" :aria-expanded="categoryOpen" aria-controls="hfs-category-panel" aria-label="Mở danh mục sản phẩm"
                type="button" @click="toggleCategories" @keydown.down.prevent="openCategories">
          <MarketIcon name="chevron"/>
        </button>
        <div v-if="categoryOpen" id="hfs-category-panel" ref="categoryPanel" class="hfs-category-panel">
          <RouterLink v-for="category in categories" :key="category.id"
                      :to="{path:'/products',query:{category:category.slug}}">{{ category.name }}
          </RouterLink>
          <RouterLink class="hfs-all-categories" to="/categories">Xem tất cả danh mục
            <MarketIcon name="arrow"/>
          </RouterLink>
        </div>
      </div>
      <RouterLink to="/stores">
        <MarketIcon name="store"/>
        Gian hàng
      </RouterLink>
      <RouterLink :title="seller?'Quản lý gian hàng':'Kênh người bán — đăng ký gian hàng đang phát triển'" :to="seller?'/seller':'/login?intent=seller'"
                  class="hfs-seller">
        <MarketIcon name="store"/>
        Dành cho người bán
      </RouterLink>
    </nav>
    <nav v-if="mobileOpen" id="hfs-mobile-nav" ref="mobilePanel" aria-label="Điều hướng trên điện thoại"
         class="hfs-mobile-nav hfs-container">
      <RouterLink to="/products">
        <MarketIcon name="grid"/>
        Sản phẩm
        <MarketIcon name="chevron"/>
      </RouterLink>
      <details>
        <summary>
          <MarketIcon name="list"/>
          Danh mục
          <MarketIcon name="chevron"/>
        </summary>
        <RouterLink v-for="category in categories" :key="category.id"
                    :to="{path:'/products',query:{category:category.slug}}">{{ category.name }}
        </RouterLink>
        <RouterLink to="/categories">Xem tất cả danh mục</RouterLink>
      </details>
      <RouterLink to="/stores">
        <MarketIcon name="store"/>
        Gian hàng
        <MarketIcon name="chevron"/>
      </RouterLink>
      <RouterLink v-if="!authenticated" to="/login">
        <MarketIcon name="user"/>
        Đăng nhập
      </RouterLink>
      <RouterLink v-if="!authenticated" to="/register">
        <MarketIcon name="user"/>
        Tạo tài khoản
      </RouterLink>
      <RouterLink v-else to="/profile">
        <MarketIcon name="user"/>
        Tài khoản của tôi
      </RouterLink>
      <RouterLink :to="seller?'/seller':'/login?intent=seller'" class="hfs-seller">
        <MarketIcon name="store"/>
        Dành cho người bán
      </RouterLink>
    </nav>
  </header>
</template>
<script setup>
import {ref, watch, computed, nextTick, onMounted, onBeforeUnmount} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import MarketIcon from '../marketplace/MarketIcon.vue';
import BonsaiMark from './BonsaiMark.vue';

defineProps({
  authenticated: Boolean,
  seller: Boolean,
  user: Object,
  userNav: Array,
  cartCount: {type: Number, default: 0},
  categories: {type: Array, default: () => []}
});
defineEmits(['login', 'register', 'logout']);
const router = useRouter(), current = useRoute(), term = ref(String(current.query.search || ''));
const headerRoot = ref(null), categoryRoot = ref(null), categoryPanel = ref(null), categoryTrigger = ref(null),
    accountMenu = ref(null), mobileTrigger = ref(null), mobilePanel = ref(null);
const categoryOpen = ref(false), mobileOpen = ref(false);
const showAI = computed(() => !/^\/(login|register|admin|seller)(\/|$)/.test(current.path));
watch(() => current.query.search, v => term.value = String(v || ''));
watch(() => current.fullPath, () => {
  categoryOpen.value = false;
  mobileOpen.value = false;
  if (accountMenu.value) accountMenu.value.open = false;
});

function search() {
  router.push({path: '/products', query: term.value.trim() ? {search: term.value.trim()} : {}})
}

function openAI() {
  window.dispatchEvent(new CustomEvent('agriverse-open-ai-expert'))
}

function toggleCategories() {
  categoryOpen.value = !categoryOpen.value;
}

async function openCategories() {
  categoryOpen.value = true;
  await nextTick();
  categoryPanel.value?.querySelector('a')?.focus();
}

async function toggleMobile() {
  mobileOpen.value = !mobileOpen.value;
  if (mobileOpen.value) {
    await nextTick();
    mobilePanel.value?.querySelector('a')?.focus();
  }
}

function closeWithEscape(event) {
  if (categoryOpen.value) {
    categoryOpen.value = false;
    categoryTrigger.value?.focus();
    event.preventDefault();
  } else if (accountMenu.value?.open) {
    accountMenu.value.open = false;
    accountMenu.value.querySelector('summary')?.focus();
    event.preventDefault();
  } else if (mobileOpen.value) {
    mobileOpen.value = false;
    mobileTrigger.value?.focus();
    event.preventDefault();
  }
}

function onOutside(event) {
  if (categoryRoot.value && !categoryRoot.value.contains(event.target)) categoryOpen.value = false;
  if (accountMenu.value && !accountMenu.value.contains(event.target)) accountMenu.value.open = false;
  if (headerRoot.value && !headerRoot.value.contains(event.target)) mobileOpen.value = false;
}

function onFocusOutside(event) {
  if (categoryRoot.value && !categoryRoot.value.contains(event.target)) categoryOpen.value = false;
}

function onResize() {
  if (window.innerWidth >= 768) mobileOpen.value = false;
}

onMounted(() => {
  document.addEventListener('click', onOutside);
  document.addEventListener('focusin', onFocusOutside);
  window.addEventListener('resize', onResize);
});
onBeforeUnmount(() => {
  document.removeEventListener('click', onOutside);
  document.removeEventListener('focusin', onFocusOutside);
  window.removeEventListener('resize', onResize);
});
</script>

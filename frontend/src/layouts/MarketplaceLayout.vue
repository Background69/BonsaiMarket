<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--ag-bg);">
    <Toast position="top-right"/>

    <!-- ========== HEADER: Glassmorphism Sticky Nav ========== -->
    <header ref="headerRef"
            :class="{ 'header-scrolled': scrolled }"
            class="header">
      <div class="header-container">
        <!-- Mobile Menu Toggle -->
        <button :aria-expanded="mobileOpen" aria-label="Mở menu" class="md:hidden w-10 h-10 flex items-center justify-center rounded-lg header-icon-btn header-menu-toggle"
                @click="mobileOpen = !mobileOpen">
          <span class="material-symbols-outlined text-xl">{{ mobileOpen ? 'close' : 'menu' }}</span>
        </button>

        <!-- Left Group: Logo + Nav -->
        <div class="header-brand-group flex items-center gap-12">
          <!-- Brand: Roboto -->
          <Link :href="route('agriverse.shop.home')" class="flex items-center gap-2.5 shrink-0 group">
            <div
                class="w-8 h-8 rounded-lg header-logo-mark flex items-center justify-center group-hover:scale-105 transition-transform duration-300">
              <span class="material-symbols-outlined text-white" style="font-size: 18px;">eco</span>
            </div>
            <span class="brand-text" style="font-size: 24px;">BonsaiMarket</span>
          </Link>
        </div>

        <form class="header-search" role="search" @submit.prevent="submitSearch">
          <label class="sr-only" for="marketplace-search">Tìm sản phẩm cây cảnh</label>
          <input id="marketplace-search" v-model="searchTerm" type="search" placeholder="Tìm cây cảnh, sản phẩm..." autocomplete="off"/>
          <button type="submit" aria-label="Tìm sản phẩm" title="Tìm sản phẩm">
            <span class="material-symbols-outlined">search</span>
          </button>
        </form>

        <!-- Right Group: Icons + Auth -->
        <div class="flex items-center gap-6 justify-end header-actions">
          <!-- Icons -->
          <div class="flex items-center gap-0.5 md:gap-0.5">
            <Link :class="{ 'wishlist-active': route().current('agriverse.shop.wishlist.*') }"
                  :href="route('agriverse.shop.wishlist.index')"
                  class="w-9 h-9 md:w-10 md:h-10 flex items-center justify-center rounded-lg header-icon-btn"
                  title="Yêu thích" aria-label="Yêu thích">
              <span class="material-symbols-outlined" style="font-size: 20px;">favorite</span>
            </Link>

            <Link :class="{ 'cart-active': route().current('agriverse.shop.cart.*') }"
                  :href="route('agriverse.shop.cart.index')"
                  class="w-9 h-9 md:w-10 md:h-10 flex items-center justify-center rounded-lg header-icon-btn relative"
                  title="Giỏ hàng" aria-label="Giỏ hàng">
              <span class="material-symbols-outlined" style="font-size: 20px;">shopping_bag</span>
              <span v-if="cartCount > 0"
                    class="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] rounded-full cart-badge flex items-center justify-center px-[4px] text-[10px] font-bold animate-scale-in">
                {{ cartCount > 99 ? '99+' : cartCount }}
              </span>
            </Link>
          </div>

          <!-- Auth -->
          <div class="flex items-center gap-1 pl-3 header-divider nav-text header-auth">
            <template v-if="isAuthenticated">
              <div ref="userMenuRef" class="relative">
                <button type="button" class="w-9 h-9 md:w-10 md:h-10 flex items-center justify-center rounded-full header-user-btn" title="Tài khoản" aria-label="Mở menu tài khoản" :aria-expanded="userMenuOpen" @click="userMenuOpen = !userMenuOpen">
                  <span v-if="user?.avatar_url" class="w-full h-full rounded-full overflow-hidden"><img :alt="user.name" :src="user.avatar_url" class="w-full h-full object-cover"/></span>
                  <span v-else class="header-user-avatar">{{ user?.name?.charAt(0)?.toUpperCase() || 'U' }}</span>
                </button>
                <div v-if="userMenuOpen" class="absolute right-0 top-full mt-2 w-60 rounded-xl shadow-lg border user-dropdown z-50 overflow-hidden">
                  <div class="px-4 py-3 user-dropdown-header"><p class="text-sm font-semibold">{{ user?.name || 'Người dùng' }}</p><p class="text-xs">{{ user?.email || '' }}</p></div>
                  <Link v-if="isSeller" :href="route('agriverse.shop.seller.dashboard')" class="user-dropdown-item block px-4 py-2.5 text-sm" @click="userMenuOpen = false">Kênh người bán</Link>
                  <Link v-for="item in userNav" :key="item.route" :href="route(item.route)" class="user-dropdown-item block px-4 py-2.5 text-sm" @click="userMenuOpen = false">{{ item.label }}</Link>
                  <button class="user-dropdown-logout block px-4 py-2.5 text-sm w-full text-left" type="button" @click="handleLogoutFromMenu">Đăng xuất</button>
                </div>
              </div>
            </template>
            <template v-else>
              <button class="px-3 py-2 rounded-lg text-sm header-secondary-link transition-all duration-200" @click="goToLogin">Đăng nhập</button>
              <button class="px-4 py-2 rounded-lg header-register-btn text-sm font-semibold transition-all duration-200 header-register" @click="goToRegister">Đăng ký</button>
            </template>
          </div>
        </div>
      </div>

      <div class="header-nav-row">
        <div class="header-nav-container">
          <nav aria-label="Điều hướng chính" class="nav-text desktop-nav">
            <Link v-for="item in primaryNav" :key="item.route" :class="route().current(item.pattern)
              ? 'nav-link-active'
              : ''"
                  :href="route(item.route)"
                  class="px-3 py-2 rounded-lg nav-link transition-all duration-200">
              {{ item.label }}
            </Link>
            <!-- More dropdown -->
            <div ref="moreMenuRef" class="more-wrap"
                 @mouseenter="openMore()"
                 @mouseleave="closeMoreDelayed()">
              <button type="button" :aria-expanded="moreOpen" aria-label="Mở thêm mục điều hướng" @click="moreOpen = !moreOpen"
                  :class="moreActive ? 'nav-link-active' : ''"
                  class="px-3 py-2 rounded-lg nav-link nav-more-btn transition-all duration-200 flex items-center gap-1">
                Thêm
                <span :class="{ 'rotate-180': moreOpen }"
                      class="material-symbols-outlined text-sm transition-transform duration-200">expand_more</span>
              </button>
              <Transition name="more-fade">
                <div v-if="moreOpen" class="more-dropdown" @mouseenter="cancelCloseMore()"
                     @mouseleave="closeMoreDelayed()">
                  <Link v-for="item in moreNav" :key="item.route" :class="{ 'more-dropdown-item-active': route().current(item.pattern) }"
                        :href="route(item.route)"
                        class="more-dropdown-item"
                        @click="moreOpen = false">
                    <span class="material-symbols-outlined">{{ item.icon }}</span>
                    {{ item.label }}
                  </Link>
                </div>
              </Transition>
            </div>
          </nav>
        </div>
      </div>
    </header>

    <div v-if="mobileOpen" class="mobile-drawer-wrap">
      <button aria-label="Đóng menu" class="mobile-drawer-backdrop" @click="mobileOpen = false"></button>
      <nav aria-label="Điều hướng di động" class="mobile-drawer">
        <Link v-for="item in mobileNav" :key="item.route" :href="route(item.route)"
              class="mobile-drawer-link" @click="mobileOpen = false">
          <span class="material-symbols-outlined">{{ item.icon }}</span>{{ item.label }}
        </Link>
        <Link class="mobile-drawer-link" href="/login" @click="mobileOpen = false">Đăng nhập</Link>
        <Link class="mobile-drawer-link" href="/register" @click="mobileOpen = false">Đăng ký</Link>
      </nav>
    </div>

    <!-- ========== MAIN CONTENT ========== -->
    <main class="flex-1">
      <slot/>
    </main>

    <!-- ========== FOOTER ========== -->
    <footer v-if="!hideFooter" class="footer">
      <div class="footer-container">
        <div class="grid grid-cols-12 gap-8 md:gap-12">
          <!-- Brand Column -->
          <div class="col-span-12 md:col-span-4">
            <div class="flex items-center gap-2.5 mb-4">
              <div class="w-8 h-8 rounded-lg header-logo-mark flex items-center justify-center">
                <span class="material-symbols-outlined text-white" style="font-size: 18px;">spa</span>
              </div>
              <span class="brand-text" style="font-size: 20px;">BonsaiMarket</span>
            </div>
            <p class="footer-description">
              Cộng đồng bonsai &amp; cây cảnh Việt Nam &mdash; nơi hội tụ của những người yêu cây, chia sẻ kiến thức và
              kết nối vườn ươm uy tín trên toàn quốc.
            </p>
            <div class="flex items-center gap-3 mt-5">
              <Link :href="route('agriverse.shop.forum.index')" class="footer-social-link" title="Diễn đàn cộng đồng">
                <span class="material-symbols-outlined" style="font-size: 18px;">groups</span>
              </Link>
              <Link :href="route('agriverse.shop.journal.index')" class="footer-social-link" title="Bài viết kiến thức">
                <span class="material-symbols-outlined" style="font-size: 18px;">article</span>
              </Link>
              <Link :href="route('agriverse.shop.support.index')" class="footer-social-link" title="Liên hệ hỗ trợ">
                <span class="material-symbols-outlined" style="font-size: 18px;">support_agent</span>
              </Link>
            </div>
          </div>

          <!-- Khám phá -->
          <div class="col-span-6 md:col-span-2">
            <div class="footer-heading">Khám phá</div>
            <div class="space-y-3">
              <Link :href="route('agriverse.shop.products.index', { category: 'bonsai-co-thu' })"
                    class="block text-sm footer-link">Cây cảnh
              </Link>
              <Link :href="route('agriverse.shop.products.index', { category: 'phu-kien-dung-cu' })"
                    class="block text-sm footer-link">Phụ kiện
              </Link>
              <Link :href="route('agriverse.shop.categories.index')" class="block text-sm footer-link">Danh mục</Link>
              <Link :href="route('agriverse.shop.products.index')" class="block text-sm footer-link">Ưu đãi đặc biệt
              </Link>
            </div>
          </div>

          <!-- Kiến thức -->
          <div class="col-span-6 md:col-span-2">
            <div class="footer-heading">Kiến thức</div>
            <div class="space-y-3">
              <Link :href="route('agriverse.shop.garden.index')" class="block text-sm footer-link">Khu vườn của tôi
              </Link>
              <Link :href="route('agriverse.shop.quiz.index')" class="block text-sm footer-link">Tìm mẫu cây cảnh</Link>
              <Link :href="route('agriverse.shop.diagnostic.index')" class="block text-sm footer-link">Chẩn đoán cây
              </Link>
            </div>
          </div>

          <!-- Newsletter -->
          <div class="col-span-12 md:col-span-4">
            <div class="footer-heading">Bản tin xanh</div>
            <p class="footer-description mb-4">
              Nhận bí quyết chăm sóc cây cảnh, mẹo từ nghệ nhân và cập nhật bộ sưu tập mới mỗi tuần.
            </p>
            <div class="flex items-center border-b border-outline/30 pb-2"
                 style="border-bottom-color: rgba(116, 121, 108, 0.3);">
              <label for="footer-newsletter-email" class="header-search-label">Email nhận bản tin</label>
              <input id="footer-newsletter-email" class="flex-1 bg-transparent border-none text-sm outline-none shadow-none focus:ring-0 placeholder-muted p-0 h-8" placeholder="Nhập email của bạn"
                     type="email">
              <button aria-label="Đăng ký bản tin" class="text-primary hover:opacity-80 transition-opacity flex items-center justify-center h-8"
                      style="color: var(--ag-primary-500);"
                      type="button" @click="openChatPanel">
                <span class="material-symbols-outlined" style="font-size: 24px;">arrow_forward</span>
              </button>
            </div>
          </div>
        </div>

        <!-- Copyright -->
        <div class="footer-copyright">
          &copy; {{ new Date().getFullYear() }} BonsaiMarket. Sàn cây cảnh Việt Nam.
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import {ref, computed, watch, onMounted, onBeforeUnmount} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {Link, usePage} from '@inertiajs/vue3';
import {route} from 'ziggy-js';
import {useAuth} from '@agriverse/Composables/useAuth';
import {useChat} from '@agriverse/Composables/useChat';
import Toast from 'primevue/toast';

const props = defineProps({
  hideFooter: {type: Boolean, default: false},
});

const {user, isAuthenticated, logout, syncFromPageProps} = useAuth();
const {state: chatState, togglePanel: toggleChatPanel} = useChat();

const mobileOpen = ref(false);
const scrolled = ref(false);
const headerRef = ref(null);
const userMenuOpen = ref(false);
const userMenuRef = ref(null);
const page = usePage();
const vueRouter = useRouter();
const currentRoute = useRoute();
const searchTerm = ref(String(currentRoute.query.search || ''));
watch(() => currentRoute.query.search, value => { searchTerm.value = String(value || ''); });

function submitSearch() {
  vueRouter.push({path: '/products', query: searchTerm.value.trim() ? {search: searchTerm.value.trim()} : {}});
  mobileOpen.value = false;
}

function onScroll() {
  scrolled.value = window.scrollY > 20;
}

function onClickOutside(e) {
  if (userMenuOpen.value && userMenuRef.value && !userMenuRef.value.contains(e.target)) {
    userMenuOpen.value = false;
  }
}

function handleLogoutFromMenu() {
  userMenuOpen.value = false;
  handleLogout();
}

function openChatPanel() {
  window.dispatchEvent(new CustomEvent('bonsai:pending'));
}

onMounted(() => {
  window.addEventListener('scroll', onScroll, {passive: true});
  document.addEventListener('click', onClickOutside);
  syncFromPageProps();
});
onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll);
  document.removeEventListener('click', onClickOutside);
});

// Khi đăng nhập/đổi user → tự kết nối (hoặc nối lại) WebSocket chat
watch(isAuthenticated, () => {
});

const cartCount = computed(() => page.props.cartCount ?? 0);

const isSeller = computed(() => page.props.auth?.user?.role === 'seller')

const canBecomeSeller = computed(() => {
  return !isSeller.value && page.props.auth?.user?.role !== 'admin'
})

const moreOpen = ref(false);
const moreMenuRef = ref(null);
let moreCloseTimer = null;

const primaryNav = [
  {label: 'Sản phẩm', route: 'agriverse.shop.products.index', pattern: 'agriverse.shop.products.*'},
  {label: 'Danh mục', route: 'agriverse.shop.categories.index', pattern: 'agriverse.shop.categories.*'},
  {label: 'Gian hàng', route: 'agriverse.shop.stores.index', pattern: 'agriverse.shop.stores.*'},
  {label: 'Kênh người bán', route: 'agriverse.shop.seller.dashboard', pattern: 'agriverse.shop.seller.*'},
];

const moreNav = [
  {label: 'Chẩn đoán cây (đang phát triển)', icon: 'ecg_heart', route: 'agriverse.shop.diagnostic.index', pattern: 'agriverse.shop.diagnostic.*'},
  {label: 'Bài viết (đang phát triển)', icon: 'article', route: 'agriverse.shop.journal.index', pattern: 'agriverse.shop.journal.*'},
  {label: 'Diễn đàn (đang phát triển)', icon: 'forum', route: 'agriverse.shop.forum.index', pattern: 'agriverse.shop.forum.*'},
  {
    label: 'Hỗ trợ',
    icon: 'contact_support',
    route: 'agriverse.shop.support.index',
    pattern: 'agriverse.shop.support.*'
  },
  {label: 'Tìm mẫu', icon: 'quiz', route: 'agriverse.shop.quiz.index', pattern: 'agriverse.shop.quiz.*'},
  {
    label: 'PT Bền vững',
    icon: 'energy_savings_leaf',
    route: 'agriverse.shop.sustainability.index',
    pattern: 'agriverse.shop.sustainability.*'
  },
];

const moreActive = computed(() => moreNav.some(item => route().current(item.pattern)));

function openMore() {
  if (moreCloseTimer) {
    clearTimeout(moreCloseTimer);
    moreCloseTimer = null;
  }
  moreOpen.value = true;
}

function closeMoreDelayed() {
  moreCloseTimer = setTimeout(() => {
    moreOpen.value = false;
  }, 300);
}

function cancelCloseMore() {
  if (moreCloseTimer) {
    clearTimeout(moreCloseTimer);
    moreCloseTimer = null;
  }
}

const userNav = [
  {label: 'Hồ sơ', icon: 'person', route: 'agriverse.shop.profile.index'},
  {label: 'Đơn hàng', icon: 'receipt', route: 'agriverse.shop.orders.index'},
  {label: 'Flash Sale', icon: 'flash_on', route: 'agriverse.shop.market.flash-deal'},
  {label: 'Đề xuất giá', icon: 'handshake', route: 'agriverse.shop.market.offer'},
  {label: 'Thông báo', icon: 'notifications', route: 'agriverse.shop.notifications.index'},
  {label: 'Theo dõi vận chuyển', icon: 'local_shipping', route: 'agriverse.shop.tracking.index'},
  {label: 'Affiliate', icon: 'loyalty', route: 'agriverse.shop.affiliate.index'},
  {label: 'Cài đặt', icon: 'settings', route: 'agriverse.shop.account.settings'},
];

const mobileNav = [
  {label: 'Trang chủ', icon: 'home', route: 'agriverse.shop.home', pattern: 'agriverse.shop.home'},
  {
    label: 'Sản phẩm',
    icon: 'inventory_2',
    route: 'agriverse.shop.products.index',
    pattern: 'agriverse.shop.products.*'
  },
  {label: 'Danh mục', icon: 'category', route: 'agriverse.shop.categories.index', pattern: 'agriverse.shop.categories.*'},
  {label: 'Gian hàng', icon: 'storefront', route: 'agriverse.shop.stores.index', pattern: 'agriverse.shop.stores.*'},
  {label: 'Kênh người bán', icon: 'store', route: 'agriverse.shop.seller.dashboard', pattern: 'agriverse.shop.seller.*'},
  {label: 'Bài viết', icon: 'article', route: 'agriverse.shop.journal.index', pattern: 'agriverse.shop.journal.*'},
  {label: 'Diễn đàn', icon: 'forum', route: 'agriverse.shop.forum.index', pattern: 'agriverse.shop.forum.*'},
  {label: 'Khu vườn', icon: 'forest', route: 'agriverse.shop.garden.index', pattern: 'agriverse.shop.garden.*'},
  {label: 'Đơn hàng', icon: 'receipt', route: 'agriverse.shop.orders.index', pattern: 'agriverse.shop.orders.*'},
  {
    label: 'Thông báo',
    icon: 'notifications',
    route: 'agriverse.shop.notifications.index',
    pattern: 'agriverse.shop.notifications.*'
  },
  {
    label: 'Theo dõi vận chuyển',
    icon: 'local_shipping',
    route: 'agriverse.shop.tracking.index',
    pattern: 'agriverse.shop.tracking.*'
  },
  {label: 'Yêu thích', icon: 'favorite', route: 'agriverse.shop.wishlist.index', pattern: 'agriverse.shop.wishlist.*'},
  {label: 'Giỏ hàng', icon: 'shopping_bag', route: 'agriverse.shop.cart.index', pattern: 'agriverse.shop.cart.*'},
  {label: 'Tìm mẫu', icon: 'quiz', route: 'agriverse.shop.quiz.index', pattern: 'agriverse.shop.quiz.*'},
  {
    label: 'PT Bền vững',
    icon: 'energy_savings_leaf',
    route: 'agriverse.shop.sustainability.index',
    pattern: 'agriverse.shop.sustainability.*'
  },
  {
    label: 'Hỗ trợ',
    icon: 'contact_support',
    route: 'agriverse.shop.support.index',
    pattern: 'agriverse.shop.support.*'
  },
  {label: 'Affiliate', icon: 'loyalty', route: 'agriverse.shop.affiliate.index', pattern: 'agriverse.shop.affiliate.*'},
  {
    label: 'Cài đặt',
    icon: 'settings',
    route: 'agriverse.shop.account.settings',
    pattern: 'agriverse.shop.account.settings'
  },
];

function goToLogin() {
  window.location.href = '/login';
}

function goToRegister() {
  window.location.href = '/register';
}

function openChatFromMobile() {
  mobileOpen.value = false;
  openChatPanel();
}

function goToLoginMobile() {
  mobileOpen.value = false;
  window.location.href = '/login';
}

function goToRegisterMobile() {
  mobileOpen.value = false;
  window.location.href = '/register';
}

async function handleLogout() {
  await logout();
}

function handleLogoutMobile() {
  mobileOpen.value = false;
  handleLogout();
}

</script>

<style scoped>
/* ============================================================
   BOTANICAL HERITAGE DESIGN SYSTEM — MarketplaceLayout
   ============================================================
   Typography: Roboto
   Palette: Sage Green (#486730) + Terracotta (#8b4f27) + Sand (#f4f1ea)
   Container: 1280px, 8px base grid, 24px gutter
   ============================================================ */

/* ===== Brand Typography ===== */
.brand-text {
  font-family: var(--ag-font-display);
  font-weight: 500;
  color: var(--ag-primary-500);
  letter-spacing: -0.02em;
}

.nav-text {
  font-family: var(--ag-font-body);
  font-size: 14px;
  font-weight: 500;
}

/* ===== Header ===== */
.header {
  position: sticky;
  top: 0;
  z-index: 50;
  background: rgba(252, 249, 248, 0.7);
  backdrop-filter: blur(12px) saturate(1.4);
  -webkit-backdrop-filter: blur(12px) saturate(1.4);
  border-bottom: 1px solid rgba(234, 231, 231, 0.5);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.header-scrolled {
  background: rgba(252, 249, 248, 0.8);
  backdrop-filter: blur(20px) saturate(1.5);
  -webkit-backdrop-filter: blur(20px) saturate(1.5);
  border-bottom: 1px solid rgba(234, 231, 231, 0.8);
  box-shadow: 0 1px 3px rgba(72, 103, 48, 0.04);
}

.header-container {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 64px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

@media (max-width: 768px) {
  .header-container {
    padding: 0 16px;
    height: 56px;
    gap: 8px;
  }
}

.header-logo-mark {
  background: var(--ag-primary-500);
}

.header-icon-btn {
  color: var(--ag-text-secondary);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.header-icon-btn:hover {
  color: var(--ag-primary-500);
  background: color-mix(in srgb, var(--ag-primary-500) 6%, transparent);
}

.header-icon-btn:active {
  transform: scale(0.93);
}

.wishlist-active {
  color: var(--ag-danger) !important;
}

.wishlist-active:hover {
  background: color-mix(in srgb, var(--ag-danger) 6%, transparent) !important;
}

.cart-active {
  color: var(--ag-primary-500) !important;
}

.cart-badge {
  background: var(--ag-secondary-500);
  color: white;
}

.nav-link {
  display: inline-flex;
  align-items: center;
  color: var(--ag-text-secondary);
}

.nav-link:hover {
  color: var(--ag-primary-500);
  background: color-mix(in srgb, var(--ag-primary-500) 6%, transparent);
}

.nav-link-active {
  color: var(--ag-primary-600);
  background: color-mix(in srgb, var(--ag-primary-500) 8%, transparent);
  font-weight: 600;
}

.nav-more-btn {
  font-size: 13px;
  gap: 2px;
}

.nav-more-btn .material-symbols-outlined {
  font-size: 16px;
}

/* More dropdown */
.more-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%);
  min-width: 200px;
  background: white;
  border: 1px solid var(--ag-border);
  border-radius: 12px;
  box-shadow: 0 12px 32px -8px rgba(0, 0, 0, 0.1);
  padding: 6px;
  z-index: 60;
}

.more-dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: var(--ag-text-secondary);
  text-decoration: none;
  transition: all 0.15s;
}

.more-dropdown-item:hover {
  background: var(--ag-neutral-100);
  color: var(--ag-text-primary);
}

.more-dropdown-item-active {
  background: color-mix(in srgb, var(--ag-primary-500) 8%, transparent);
  color: var(--ag-primary-600);
}

.more-dropdown-item .material-symbols-outlined {
  font-size: 18px;
  color: var(--ag-text-muted);
}

.more-dropdown-item-active .material-symbols-outlined {
  color: var(--ag-primary-500);
}

.more-fade-enter-active,
.more-fade-leave-active {
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.more-fade-enter-from,
.more-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

.header-secondary-link {
  color: var(--ag-text-secondary);
}

.header-secondary-link:hover {
  color: var(--ag-primary-500);
  background: color-mix(in srgb, var(--ag-primary-500) 6%, transparent);
}

.header-logout-btn {
  color: var(--ag-text-muted);
}

.header-logout-btn:hover {
  color: var(--ag-danger);
  background: color-mix(in srgb, var(--ag-danger) 6%, transparent);
}

.header-register-btn {
  background: var(--ag-primary-500);
  color: white;
}

.header-register-btn:hover {
  background: var(--ag-primary-600);
  box-shadow: 0 4px 12px -2px color-mix(in srgb, var(--ag-primary-500) 30%, transparent);
}

.header-divider {
  border-left: 1px solid var(--ag-border);
}

@media (max-width: 1100px) {
  .header .desktop-nav {
    display: none;
  }

  .header .header-menu-toggle {
    display: flex;
    flex-shrink: 0;
  }

  .header-container {
    padding: 0 20px;
    gap: 12px;
  }
}

@media (max-width: 480px) {
  .header-container {
    padding: 0 12px;
    gap: 6px;
  }

  .header-actions {
    gap: 6px;
  }

  .header-auth {
    padding-left: 5px;
  }

  .header-register {
    display: none;
  }
}

.mobile-drawer-wrap {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: flex;
}

.mobile-drawer-backdrop {
  position: absolute;
  inset: 0;
  border: 0;
  background: rgba(27, 28, 28, .35);
}

.mobile-drawer {
  position: relative;
  width: min(320px, 86vw);
  height: 100%;
  overflow-y: auto;
  padding: 20px 12px;
  background: var(--ag-bg-card);
  box-shadow: 8px 0 32px rgba(0, 0, 0, .12);
}

.mobile-drawer-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 9px;
  color: var(--ag-text-primary);
  font-size: 14px;
}

.mobile-drawer-link:hover {
  background: var(--ag-primary-50);
  color: var(--ag-primary-600);
}

.mobile-drawer-link .material-symbols-outlined {
  width: 24px;
  color: var(--ag-primary-500);
}

/* ===== User Avatar Button ===== */
.header-user-btn {
  background: color-mix(in srgb, var(--ag-primary-500) 8%, transparent);
  color: var(--ag-primary-500);
  border: 2px solid transparent;
}

.header-user-btn:hover {
  background: color-mix(in srgb, var(--ag-primary-500) 12%, transparent);
  border-color: color-mix(in srgb, var(--ag-primary-500) 20%, transparent);
}

.header-user-avatar {
  background: var(--ag-primary-500);
  color: white;
}

/* ===== User Dropdown ===== */
.user-dropdown {
  background: white;
  border-color: var(--ag-border);
  box-shadow: 0 12px 40px -8px rgba(27, 28, 28, 0.12);
}

.user-dropdown-header {
  background: var(--ag-neutral-50);
}

.user-dropdown-item {
  color: var(--ag-text-secondary);
  font-family: var(--ag-font-body);
}

.user-dropdown-item:hover {
  color: var(--ag-text-primary);
  background: color-mix(in srgb, var(--ag-primary-500) 4%, transparent);
}

.user-dropdown-logout {
  color: var(--ag-danger);
  font-family: var(--ag-font-body);
}

.user-dropdown-logout:hover {
  background: color-mix(in srgb, var(--ag-danger) 5%, transparent);
}

/* Dropdown Transition */
.dropdown-enter-active {
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.dropdown-leave-active {
  transition: all 0.15s ease;
}

.dropdown-enter-from {
  opacity: 0;
  transform: translateY(-8px) scale(0.96);
}

.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px) scale(0.97);
}

/* ===== Search ===== */
.search-input {
  border: 1px solid var(--ag-border);
  background: var(--ag-neutral-100);
  color: var(--ag-text-primary);
  font-family: var(--ag-font-body);
}

.search-input::placeholder {
  color: var(--ag-text-muted);
}

.search-input:hover {
  border-color: var(--ag-neutral-300);
}

.search-input:focus {
  border-color: var(--ag-primary-500);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ag-primary-500) 10%, transparent);
  background: white;
}

.search-btn {
  color: var(--ag-text-muted);
}

.search-btn:hover {
  color: var(--ag-primary-500);
}


/* ===== Mobile Menu ===== */
.mobile-menu-enter-active {
  transition: opacity 0.3s ease;
}

.mobile-menu-leave-active {
  transition: opacity 0.2s ease;
}

.mobile-menu-enter-from, .mobile-menu-leave-to {
  opacity: 0;
}

.mobile-menu-enter-active > div:last-child {
  transition: transform 0.35s cubic-bezier(0.32, 0.72, 0, 1);
}

.mobile-menu-leave-active > div:last-child {
  transition: transform 0.25s ease;
}

.mobile-menu-enter-from > div:last-child {
  transform: translateX(-100%);
}

.mobile-menu-leave-to > div:last-child {
  transform: translateX(-100%);
}

.mobile-overlay {
  background: color-mix(in srgb, var(--ag-neutral-900) 20%, transparent);
  backdrop-filter: blur(4px);
}

.mobile-drawer {
  background: white;
}

.mobile-nav-item {
  color: var(--ag-text-secondary);
  font-family: var(--ag-font-body);
}

.mobile-nav-item:hover {
  color: var(--ag-text-primary);
  background: color-mix(in srgb, var(--ag-primary-500) 4%, transparent);
}

.mobile-nav-item-active {
  color: var(--ag-primary-600);
  background: color-mix(in srgb, var(--ag-primary-500) 8%, transparent);
  font-weight: 600;
}

.mobile-nav-icon {
  color: var(--ag-text-muted);
}

.mobile-nav-icon-active {
  color: var(--ag-primary-500);
}

.mobile-logout-btn {
  color: var(--ag-danger);
  font-family: var(--ag-font-body);
}

.mobile-logout-btn:hover {
  background: color-mix(in srgb, var(--ag-danger) 5%, transparent);
}

/* ===== Footer: Botanical Heritage Sand/Bone ===== */
.footer {
  background: var(--ag-surface-container, #f0eded);
  border-top: 1px solid var(--ag-outline-10, rgba(116, 121, 108, 0.1));
  margin-top: 80px;
}

.footer-container {
  max-width: 1280px;
  margin: 0 auto;
  padding: 80px 64px 40px;
}

@media (max-width: 768px) {
  .footer-container {
    padding: 64px 24px 40px;
  }
}

@media (max-width: 480px) {
  .footer-container > .grid {
    column-gap: 12px;
  }
}

.footer-heading {
  font-family: var(--ag-font-body);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--ag-neutral-700);
  margin-bottom: 16px;
}

.footer-description {
  font-family: var(--ag-font-body);
  font-size: 14px;
  line-height: 22px;
  color: var(--ag-text-secondary);
  max-width: 320px;
}

.footer-link {
  color: var(--ag-text-secondary);
  font-family: var(--ag-font-body);
  transition: color 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.footer-link:hover {
  color: var(--ag-primary-500);
}

.footer-social-link {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: color-mix(in srgb, var(--ag-primary-500) 10%, transparent);
  color: var(--ag-primary-500);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.footer-social-link:hover {
  background: var(--ag-primary-500);
  color: white;
}

.footer-newsletter-input {
  border: 1px solid var(--ag-accent-300, #ddd6ca);
  background: white;
  font-family: var(--ag-font-body);
  color: var(--ag-text-primary);
}

.footer-newsletter-input::placeholder {
  color: var(--ag-text-muted);
}

.footer-newsletter-input:focus {
  border-color: var(--ag-primary-500);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ag-primary-500) 10%, transparent);
}

.footer-newsletter-btn {
  background: var(--ag-primary-500);
  color: white;
  border: none;
  cursor: pointer;
  font-family: var(--ag-font-body);
  font-weight: 600;
}

.footer-newsletter-btn:hover {
  background: var(--ag-primary-600);
}

.footer-copyright {
  margin-top: 48px;
  padding-top: 32px;
  border-top: 1px solid var(--ag-accent-300, #ddd6ca);
  text-align: center;
  font-family: var(--ag-font-body);
  font-size: 12px;
  color: var(--ag-accent-500, #a19f99);
}

/* ===== Animations ===== */
@keyframes scale-in {
  from {
    transform: scale(0);
  }
  to {
    transform: scale(1);
  }
}

.animate-scale-in {
  animation: scale-in 0.3s cubic-bezier(0.32, 0.72, 0, 1);
}

/* Marketplace discovery header, using the existing color tokens. */
.header-container { gap: 24px; }
.header-brand-group { flex: 0 0 auto; min-width: 0; }
.header-search {
  display: flex;
  align-items: center;
  flex: 1 1 420px;
  min-width: 0;
  max-width: 620px;
  height: 44px;
  border: 1px solid var(--ag-border);
  border-radius: 12px;
  background: var(--ag-bg-card);
}
.header-search:focus-within { border-color: var(--ag-primary-500); box-shadow: 0 0 0 3px rgba(72,103,48,0.08); }
.header-search input { flex: 1; min-width: 0; height: 100%; padding: 0 16px; border: 0; background: transparent; color: var(--ag-text-primary); font: inherit; outline: 0; }
.header-search input::placeholder { color: var(--ag-text-secondary); }
.header-search label { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; border: 0; }
.header-search-label { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; border: 0; }
.header-search button { width: 44px; height: 42px; display: grid; place-items: center; flex-shrink: 0; color: var(--ag-primary-500); cursor: pointer; }
.header-actions { flex: 0 0 auto; }
.header-nav-row { border-top: 1px solid var(--ag-border); }
.header-nav-container { max-width: 1280px; margin: auto; padding: 0 64px; }
.header-nav-container nav { display: flex; align-items: center; gap: 8px; min-height: 46px; }
.header-nav-container .more-wrap { position: relative; }
.header :deep(a:focus-visible), .header button:focus-visible, .mobile-drawer :deep(a:focus-visible) { outline: 2px solid var(--ag-primary-500); outline-offset: 2px; }

@media (max-width: 1100px) {
  .header-nav-row { display: none; }
  .header-container { flex-wrap: wrap; height: auto; min-height: 56px; padding: 8px 20px 10px; gap: 8px; }
  .header-search { order: 4; flex: 0 0 100%; max-width: none; height: 42px; }
  .header-actions { margin-left: auto; }
}
@media (max-width: 600px) {
  .header-container { padding: 8px 12px 10px; }
  .brand-text { font-size: 20px !important; }
  .header-logo-mark { flex-shrink: 0; }
  .header-auth { display: none; }
  .header-actions { gap: 0; }
}
</style>

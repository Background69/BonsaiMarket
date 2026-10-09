<template><div class="marketplace-app"><a class="skip-link" href="#market-main">Bỏ qua điều hướng</a><PublicHeader :categories="navigationCategories" :authenticated="isAuthenticated" :seller="isSeller" :user="user" :user-nav="userNav" :cart-count="cartCount" @login="goToLogin" @register="goToRegister" @logout="handleLogout"/><div id="market-main" tabindex="-1" class="market-content"><slot/></div><PublicFooter v-if="!hideFooter" :categories="navigationCategories"/><Toast/><AIExpertChat v-if="showAiChat"/></div></template>
<script setup>

import {ref, computed, watch, onMounted, onBeforeUnmount} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {Link, usePage} from '@inertiajs/vue3';
import {route} from 'ziggy-js';
import {useAuth} from '@agriverse/Composables/useAuth';
import {useChat} from '@agriverse/Composables/useChat';
import Toast from 'primevue/toast';
import AIExpertChat from '../components/AIExpertChat.vue';
import PublicHeader from '../components/home/PublicHeader.vue';
import PublicFooter from '../components/home/PublicFooter.vue';
import {apiGet} from '../services/api.js';


const props = defineProps({
  hideFooter: {type: Boolean, default: false},
  categories: {type: Array, default: () => []},
});

const loadedNavigationCategories = ref([]);
const navigationCategories = computed(() => props.categories.length ? props.categories : loadedNavigationCategories.value);
onMounted(async () => {
  if (props.categories.length) return;
  try {
    const categories = await apiGet('/categories');
    if (Array.isArray(categories)) loadedNavigationCategories.value = categories;
  } catch { /* The catalog link stays available when categories cannot be loaded. */ }
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
const referenceStorefront = computed(() => currentRoute.path === '/' || /^\/(products|categories|stores)(\/|$)/.test(currentRoute.path));
const showAiChat = computed(() => !/^\/(admin|seller|login|register)(\/|$)/.test(currentRoute.path));
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

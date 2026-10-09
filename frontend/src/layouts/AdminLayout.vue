<template><WorkspaceShell role="admin"><slot/></WorkspaceShell></template>
<script setup>
import WorkspaceShell from '../components/marketplace/WorkspaceShell.vue';
import {ref, computed, onMounted, onBeforeUnmount} from 'vue';
import {Link, router, usePage} from '@inertiajs/vue3';
import {route} from 'ziggy-js';

const page = usePage();
const adminUserMenuOpen = ref(false);
const adminUserMenuRef = ref(null);
const sidebarOpen = ref(false);

function onClickOutside(e) {
  if (adminUserMenuOpen.value && adminUserMenuRef.value && !adminUserMenuRef.value.contains(e.target)) {
    adminUserMenuOpen.value = false;
  }
}

function handleAdminLogout() {
  adminUserMenuOpen.value = false;
  router.post('/logout');
}

onMounted(() => document.addEventListener('click', onClickOutside));
onBeforeUnmount(() => document.removeEventListener('click', onClickOutside));

const navItems = [
  {label: 'Tổng quan', icon: 'dashboard', route: 'admin.agriverse.dashboard', pattern: 'admin.agriverse.dashboard'},
  {
    label: 'Sản phẩm',
    icon: 'inventory_2',
    route: 'admin.agriverse.products.index',
    pattern: 'admin.agriverse.products.*'
  },
  {label: 'Đơn hàng', icon: 'receipt_long', route: 'admin.agriverse.orders.index', pattern: 'admin.agriverse.orders.*'},
  {label: 'Cửa hàng', icon: 'storefront', route: 'admin.agriverse.stores.index', pattern: 'admin.agriverse.stores.*'},
  {
    label: 'Danh mục',
    icon: 'category',
    route: 'admin.agriverse.categories.index',
    pattern: 'admin.agriverse.categories.*'
  },
  {label: 'Người dùng', icon: 'people', route: 'admin.agriverse.users.index', pattern: 'admin.agriverse.users.*'},
  {
    label: 'Banner',
    icon: 'view_carousel',
    route: 'admin.agriverse.banners.index',
    pattern: 'admin.agriverse.banners.*'
  },
  {label: 'Diễn đàn', icon: 'forum', route: 'admin.agriverse.forum.index', pattern: 'admin.agriverse.forum.*'},
  {
    label: 'DM diễn đàn',
    icon: 'label',
    route: 'admin.agriverse.forum-categories.index',
    pattern: 'admin.agriverse.forum-categories.*'
  },
  {label: 'Tệp tin', icon: 'folder', route: 'admin.agriverse.files.index', pattern: 'admin.agriverse.files.*'},
  {
    label: 'Gói đăng ký',
    icon: 'subscriptions',
    route: 'admin.agriverse.plans.index',
    pattern: 'admin.agriverse.plans.*'
  },
  {
    label: 'Hợp đồng',
    icon: 'contract',
    route: 'admin.agriverse.contracts.index',
    pattern: 'admin.agriverse.contracts.*'
  },
  {label: 'Mã giảm giá', icon: 'redeem', route: 'admin.agriverse.coupons.index', pattern: 'admin.agriverse.coupons.*'},
  {label: 'Scan 3D', icon: 'view_in_ar', route: 'admin.agriverse.scans.index', pattern: 'admin.agriverse.scans.*'},
  {
    label: 'Hộ chiếu NFT',
    icon: 'workspace_premium',
    route: 'admin.agriverse.bonsais.index',
    pattern: 'admin.agriverse.bonsais.*'
  },
  {
    label: 'Giao dịch',
    icon: 'payments',
    route: 'admin.agriverse.transactions.index',
    pattern: 'admin.agriverse.transactions.*'
  },
  {
    label: 'Hoàn tiền',
    icon: 'currency_exchange',
    route: 'admin.agriverse.refunds.index',
    pattern: 'admin.agriverse.refunds.*'
  },
  {label: 'Báo cáo', icon: 'bar_chart', route: 'admin.agriverse.reports.index', pattern: 'admin.agriverse.reports.*'},
  {
    label: 'Nhóm chat',
    icon: 'forum',
    route: 'admin.agriverse.chat-groups.index',
    pattern: 'admin.agriverse.chat-groups.*'
  },
];

const breadcrumbs = computed(() => {
  const crumbs = [];
  const url = window.location.pathname;
  if (url.includes('products')) crumbs.push({label: 'Sản phẩm', route: route('admin.agriverse.products.index')});
  else if (url.includes('orders')) crumbs.push({label: 'Đơn hàng', route: route('admin.agriverse.orders.index')});
  else if (url.includes('stores')) crumbs.push({label: 'Cửa hàng', route: route('admin.agriverse.stores.index')});
  else if (url.includes('categories')) crumbs.push({
    label: 'Danh mục',
    route: route('admin.agriverse.categories.index')
  });
  else if (url.includes('users')) crumbs.push({label: 'Người dùng', route: route('admin.agriverse.users.index')});
  else if (url.includes('banners')) crumbs.push({label: 'Banner', route: route('admin.agriverse.banners.index')});
  else if (url.includes('forum')) crumbs.push({label: 'Diễn đàn', route: route('admin.agriverse.forum.index')});
  else if (url.includes('files')) crumbs.push({label: 'Tệp tin', route: route('admin.agriverse.files.index')});
  else if (url.includes('plans')) crumbs.push({label: 'Gói đăng ký', route: route('admin.agriverse.plans.index')});
  else if (url.includes('contracts')) crumbs.push({label: 'Hợp đồng', route: route('admin.agriverse.contracts.index')});
  else if (url.includes('coupons')) crumbs.push({label: 'Mã giảm giá', route: route('admin.agriverse.coupons.index')});
  else if (url.includes('scans')) crumbs.push({label: 'Scan 3D', route: route('admin.agriverse.scans.index')});
  else if (url.includes('bonsais')) crumbs.push({label: 'Hộ chiếu NFT', route: route('admin.agriverse.bonsais.index')});
  else if (url.includes('transactions')) crumbs.push({
    label: 'Giao dịch',
    route: route('admin.agriverse.transactions.index')
  });
  else if (url.includes('reports')) crumbs.push({label: 'Báo cáo', route: route('admin.agriverse.reports.index')});
  else if (url.includes('refunds')) crumbs.push({label: 'Hoàn tiền', route: route('admin.agriverse.refunds.index')});
  else if (url.includes('chat-groups')) crumbs.push({
    label: 'Nhóm chat',
    route: route('admin.agriverse.chat-groups.index')
  });
  else if (url.includes('forum-categories')) crumbs.push({
    label: 'DM diễn đàn',
    route: route('admin.agriverse.forum-categories.index')
  });
  return crumbs;
});
</script>

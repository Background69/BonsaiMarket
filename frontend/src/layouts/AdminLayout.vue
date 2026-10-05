<template>
  <div class="min-h-screen flex" style="background: var(--ag-bg);">
    <!-- Mobile overlay -->
    <div v-if="sidebarOpen" class="md:hidden fixed inset-0 bg-black/30 z-40" @click="sidebarOpen = false" />

    <!-- ===== Sidebar ===== -->
    <aside class="admin-sidebar" :class="{ open: sidebarOpen }">
      <div class="sidebar-header">
        <Link :href="route('admin.agriverse.dashboard')" class="flex items-center gap-2.5">
          <div class="w-7 h-7 rounded-md flex items-center justify-center" style="background: var(--ag-primary-500);">
            <span class="material-symbols-outlined text-white" style="font-size: 16px;">eco</span>
          </div>
          <span class="sidebar-brand">AgriVerse</span>
        </Link>
        <span class="sidebar-badge">Admin</span>
      </div>
      <nav class="flex-1 py-3 overflow-y-auto">
        <Link v-for="item in navItems" :key="item.route"
          :href="route(item.route)"
          class="sidebar-link"
          :class="{ 'sidebar-link-active': route().current(item.pattern) }"
          @click="sidebarOpen = false">
          <span class="material-symbols-outlined text-base sidebar-link-icon">{{ item.icon }}</span>
          {{ item.label }}
        </Link>
      </nav>
      <div class="sidebar-footer">
        <Link :href="route('agriverse.shop.home')" class="sidebar-back-link">
          <span class="material-symbols-outlined text-sm">arrow_back</span>
          Về marketplace
        </Link>
      </div>
    </aside>

    <!-- ===== Main Area ===== -->
    <div class="flex-1 flex flex-col min-w-0">
      <header class="admin-header">
        <div class="flex items-center gap-2">
          <button @click="sidebarOpen = !sidebarOpen"
            class="md:hidden w-9 h-9 flex items-center justify-center rounded-lg hover:bg-[var(--ag-neutral-100)] transition-colors">
            <span class="material-symbols-outlined text-xl" style="color: var(--ag-text-secondary);">{{ sidebarOpen ? 'close' : 'menu' }}</span>
          </button>
          <div class="admin-breadcrumbs">
            <Link :href="route('admin.agriverse.dashboard')" class="breadcrumb-link">Admin</Link>
            <template v-for="(crumb, i) in breadcrumbs" :key="i">
              <span class="breadcrumb-separator">/</span>
              <span v-if="i === breadcrumbs.length - 1" class="breadcrumb-current">{{ crumb.label }}</span>
              <Link v-else :href="crumb.route" class="breadcrumb-link">{{ crumb.label }}</Link>
            </template>
          </div>
        </div>
        <div class="relative" ref="adminUserMenuRef">
          <button @click="adminUserMenuOpen = !adminUserMenuOpen"
            class="admin-user-btn flex items-center gap-2.5 transition-all duration-200">
            <div class="w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold" style="background: var(--ag-primary-500); color: white;">
              {{ page.props.auth?.user?.name?.charAt(0)?.toUpperCase() || 'A' }}
            </div>
            <span class="admin-user-name">{{ page.props.auth?.user?.name || 'Admin' }}</span>
            <span class="material-symbols-outlined text-base transition-transform duration-200" style="color: var(--ag-text-muted);"
              :class="{ 'rotate-180': adminUserMenuOpen }">expand_more</span>
          </button>

          <Transition name="dropdown">
            <div v-if="adminUserMenuOpen"
              class="absolute right-0 top-full mt-2 w-56 rounded-xl shadow-lg border z-50 overflow-hidden"
              style="background: white; border-color: var(--ag-border);">
              <div class="px-4 py-3" style="background: var(--ag-neutral-50);">
                <p class="text-sm font-semibold truncate" style="color: var(--ag-on-surface);">{{ page.props.auth?.user?.name || 'Admin' }}</p>
                <p class="text-xs truncate mt-0.5" style="color: var(--ag-on-surface-variant);">{{ page.props.auth?.user?.email || '' }}</p>
              </div>
              <div class="h-px" style="background-color: var(--ag-border);" />
              <div class="py-1">
                <button @click="handleAdminLogout"
                  class="flex items-center gap-3 px-4 py-2.5 text-sm transition-all duration-200 w-full text-left admin-dropdown-logout">
                  <span class="material-symbols-outlined text-lg">logout</span>
                  Đăng xuất
                </button>
              </div>
            </div>
          </Transition>
        </div>
      </header>
      <main class="flex-1 p-5">
        <slot />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { Link, router, usePage } from '@inertiajs/vue3';
import { route } from 'ziggy-js';

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
  { label: 'Dashboard', icon: 'dashboard', route: 'admin.agriverse.dashboard', pattern: 'admin.agriverse.dashboard' },
  { label: 'Sản phẩm', icon: 'inventory_2', route: 'admin.agriverse.products.index', pattern: 'admin.agriverse.products.*' },
  { label: 'Đơn hàng', icon: 'receipt_long', route: 'admin.agriverse.orders.index', pattern: 'admin.agriverse.orders.*' },
  { label: 'Cửa hàng', icon: 'storefront', route: 'admin.agriverse.stores.index', pattern: 'admin.agriverse.stores.*' },
  { label: 'Danh mục', icon: 'category', route: 'admin.agriverse.categories.index', pattern: 'admin.agriverse.categories.*' },
  { label: 'Người dùng', icon: 'people', route: 'admin.agriverse.users.index', pattern: 'admin.agriverse.users.*' },
  { label: 'Banner', icon: 'view_carousel', route: 'admin.agriverse.banners.index', pattern: 'admin.agriverse.banners.*' },
  { label: 'Diễn đàn', icon: 'forum', route: 'admin.agriverse.forum.index', pattern: 'admin.agriverse.forum.*' },
  { label: 'DM diễn đàn', icon: 'label', route: 'admin.agriverse.forum-categories.index', pattern: 'admin.agriverse.forum-categories.*' },
  { label: 'Tệp tin', icon: 'folder', route: 'admin.agriverse.files.index', pattern: 'admin.agriverse.files.*' },
  { label: 'Gói đăng ký', icon: 'subscriptions', route: 'admin.agriverse.plans.index', pattern: 'admin.agriverse.plans.*' },
  { label: 'Hợp đồng', icon: 'contract', route: 'admin.agriverse.contracts.index', pattern: 'admin.agriverse.contracts.*' },
  { label: 'Mã giảm giá', icon: 'redeem', route: 'admin.agriverse.coupons.index', pattern: 'admin.agriverse.coupons.*' },
  { label: 'Scan 3D', icon: 'view_in_ar', route: 'admin.agriverse.scans.index', pattern: 'admin.agriverse.scans.*' },
  { label: 'Hộ chiếu NFT', icon: 'workspace_premium', route: 'admin.agriverse.bonsais.index', pattern: 'admin.agriverse.bonsais.*' },
  { label: 'Giao dịch', icon: 'payments', route: 'admin.agriverse.transactions.index', pattern: 'admin.agriverse.transactions.*' },
  { label: 'Hoàn tiền', icon: 'currency_exchange', route: 'admin.agriverse.refunds.index', pattern: 'admin.agriverse.refunds.*' },
  { label: 'Báo cáo', icon: 'bar_chart', route: 'admin.agriverse.reports.index', pattern: 'admin.agriverse.reports.*' },
  { label: 'Nhóm chat', icon: 'forum', route: 'admin.agriverse.chat-groups.index', pattern: 'admin.agriverse.chat-groups.*' },
];

const breadcrumbs = computed(() => {
  const crumbs = [];
  const url = window.location.pathname;
  if (url.includes('products')) crumbs.push({ label: 'Sản phẩm', route: route('admin.agriverse.products.index') });
  else if (url.includes('orders')) crumbs.push({ label: 'Đơn hàng', route: route('admin.agriverse.orders.index') });
  else if (url.includes('stores')) crumbs.push({ label: 'Cửa hàng', route: route('admin.agriverse.stores.index') });
  else if (url.includes('categories')) crumbs.push({ label: 'Danh mục', route: route('admin.agriverse.categories.index') });
  else if (url.includes('users')) crumbs.push({ label: 'Người dùng', route: route('admin.agriverse.users.index') });
  else if (url.includes('banners')) crumbs.push({ label: 'Banner', route: route('admin.agriverse.banners.index') });
  else if (url.includes('forum')) crumbs.push({ label: 'Diễn đàn', route: route('admin.agriverse.forum.index') });
  else if (url.includes('files')) crumbs.push({ label: 'Tệp tin', route: route('admin.agriverse.files.index') });
  else if (url.includes('plans')) crumbs.push({ label: 'Gói đăng ký', route: route('admin.agriverse.plans.index') });
  else if (url.includes('contracts')) crumbs.push({ label: 'Hợp đồng', route: route('admin.agriverse.contracts.index') });
  else if (url.includes('coupons')) crumbs.push({ label: 'Mã giảm giá', route: route('admin.agriverse.coupons.index') });
  else if (url.includes('scans')) crumbs.push({ label: 'Scan 3D', route: route('admin.agriverse.scans.index') });
  else if (url.includes('bonsais')) crumbs.push({ label: 'Hộ chiếu NFT', route: route('admin.agriverse.bonsais.index') });
  else if (url.includes('transactions')) crumbs.push({ label: 'Giao dịch', route: route('admin.agriverse.transactions.index') });
  else if (url.includes('reports')) crumbs.push({ label: 'Báo cáo', route: route('admin.agriverse.reports.index') });
  else if (url.includes('refunds')) crumbs.push({ label: 'Hoàn tiền', route: route('admin.agriverse.refunds.index') });
  else if (url.includes('chat-groups')) crumbs.push({ label: 'Nhóm chat', route: route('admin.agriverse.chat-groups.index') });
  else if (url.includes('forum-categories')) crumbs.push({ label: 'DM diễn đàn', route: route('admin.agriverse.forum-categories.index') });
  return crumbs;
});
</script>

<style scoped>
/* ============================================================
   BOTANICAL HERITAGE DESIGN SYSTEM — AdminLayout
   ============================================================ */

/* ===== Sidebar ===== */
.admin-sidebar {
  width: 220px;
  background: var(--ag-bg-card);
  border-right: 1px solid var(--ag-border);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}
@media (max-width: 768px) {
  .admin-sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 50;
    transform: translateX(-100%);
    transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    width: 260px;
    box-shadow: 4px 0 20px rgba(0,0,0,0.1);
  }
  .admin-sidebar.open {
    transform: translateX(0);
  }
}
.sidebar-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid var(--ag-border);
}
.sidebar-brand {
  font-family: var(--ag-font-display);
  font-size: 16px;
  font-weight: 500;
  color: var(--ag-primary-600);
  letter-spacing: -0.02em;
}
.sidebar-badge {
  font-family: var(--ag-font-body);
  font-size: 10px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 4px;
  color: var(--ag-text-muted);
  background: var(--ag-neutral-100);
}
.sidebar-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  margin: 1px 8px;
  font-family: var(--ag-font-body);
  font-size: 13px;
  font-weight: 500;
  color: var(--ag-text-secondary);
  text-decoration: none;
  border-radius: 8px;
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
}
.sidebar-link:hover {
  background: var(--ag-neutral-100);
  color: var(--ag-text-primary);
}
.sidebar-link-active {
  background: color-mix(in srgb, var(--ag-primary-500) 8%, transparent);
  color: var(--ag-primary-600);
  font-weight: 600;
}
.sidebar-link-active .sidebar-link-icon {
  color: var(--ag-primary-500);
}
.sidebar-link-icon {
  color: var(--ag-text-muted);
}
.sidebar-footer {
  padding: 12px;
  border-top: 1px solid var(--ag-border);
}
.sidebar-back-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  font-family: var(--ag-font-body);
  font-size: 12px;
  color: var(--ag-text-secondary);
  text-decoration: none;
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
}
.sidebar-back-link:hover {
  background: var(--ag-neutral-100);
  color: var(--ag-text-primary);
}

/* ===== Admin Header ===== */
.admin-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  position: sticky;
  top: 0;
  z-index: 40;
  background: var(--ag-bg-card);
  border-bottom: 1px solid var(--ag-border);
}
.admin-breadcrumbs {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--ag-font-body);
  font-size: 13px;
}
.breadcrumb-link {
  color: var(--ag-text-muted);
  text-decoration: none;
  transition: color 0.15s;
}
.breadcrumb-link:hover {
  color: var(--ag-primary-500);
}
.breadcrumb-separator {
  color: var(--ag-neutral-300);
}
.breadcrumb-current {
  color: var(--ag-text-primary);
  font-weight: 500;
}
.admin-user-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  font-family: var(--ag-font-body);
}
.admin-user-btn:hover {
  background: var(--ag-neutral-100);
}
.admin-user-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--ag-text-secondary);
}
.admin-dropdown-item {
  color: var(--ag-text-secondary);
  font-family: var(--ag-font-body);
}
.admin-dropdown-item:hover {
  color: var(--ag-text-primary);
  background: color-mix(in srgb, var(--ag-primary-500) 4%, transparent);
}
.admin-dropdown-logout {
  color: var(--ag-danger);
  font-family: var(--ag-font-body);
}
.admin-dropdown-logout:hover {
  background: color-mix(in srgb, var(--ag-danger) 5%, transparent);
}

/* Dropdown Transition */
.dropdown-enter-active { transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); }
.dropdown-leave-active { transition: all 0.15s ease; }
.dropdown-enter-from { opacity: 0; transform: translateY(-8px) scale(0.96); }
.dropdown-leave-to { opacity: 0; transform: translateY(-4px) scale(0.97); }

.rotate-180 { transform: rotate(180deg); }
</style>

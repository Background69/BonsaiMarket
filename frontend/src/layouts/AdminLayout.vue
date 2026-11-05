<template>
  <div class="admin-console" :class="{'admin-collapsed':collapsed}">
    <a class="skip-link" href="#workspace-main">Bỏ qua điều hướng</a>
    <aside class="admin-sidebar" aria-label="Quản trị sàn">
      <RouterLink class="admin-brand" to="/admin">
        <MarketIcon name="sprout"/>
        <span>BonsaiMarket</span></RouterLink>
      <nav class="admin-nav" aria-label="Điều hướng quản trị">
        <div v-for="group in groups" :key="group.title" class="admin-nav-group"><p>{{ group.title }}</p>
          <RouterLink v-for="item in group.items" :key="item.to" :to="item.to"
                      :aria-current="active(item)?'page':undefined" :aria-label="item.label" :title="item.label">
            <MarketIcon :name="item.icon"/>
            <span>{{ item.label }}</span></RouterLink>
        </div>
      </nav>
      <div class="admin-sidebar-foot">
        <RouterLink to="/">
          <MarketIcon name="arrow"/>
          <span>Về trang mua sắm</span></RouterLink>
      </div>
    </aside>
    <div class="admin-body">
      <header class="admin-topbar">
        <button type="button" class="admin-icon-button admin-collapse-toggle"
                :aria-label="collapsed?'Mở rộng điều hướng':'Thu gọn điều hướng'" :aria-expanded="!collapsed"
                @click="collapsed=!collapsed">
          <MarketIcon name="menu"/>
        </button>
        <button ref="trigger" type="button" class="admin-icon-button admin-mobile-toggle"
                aria-label="Mở điều hướng quản trị" aria-controls="admin-drawer" aria-haspopup="dialog"
                :aria-expanded="drawerOpen" @click="openDrawer">
          <MarketIcon name="menu"/>
        </button>
        <nav class="admin-breadcrumbs" aria-label="Vị trí">
          <RouterLink to="/admin">Quản trị sàn</RouterLink>
          <span aria-hidden="true">/</span><strong>{{ title }}</strong></nav>
        <AdminNotificationBell/>
        <slot name="topbar"/>
        <details ref="account" class="admin-account" @keydown.esc.prevent="closeAccount">
          <summary aria-label="Tài khoản quản trị"><span class="admin-avatar">{{ initial }}</span><span
              class="admin-account-summary"><strong>{{
              user?.name || 'Quản trị viên'
            }}</strong><small>Quản trị sàn</small></span>
            <MarketIcon name="chevron"/>
          </summary>
          <div class="admin-account-menu">
            <RouterLink :to="profilePath" @click="account.open=false">Hồ sơ quản trị viên</RouterLink>
            <RouterLink to="/admin/ai-assistant" @click="account.open=false">Trợ lý AI</RouterLink>
            <button type="button" :disabled="loggingOut" @click="signOut">
              {{ loggingOut ? 'Đang đăng xuất…' : 'Đăng xuất' }}
            </button>
          </div>
        </details>
      </header>
      <main id="workspace-main" class="admin-content" tabindex="-1"><p v-if="authError" class="management-error"
                                                                       role="alert">{{ authError }}</p>
        <slot/>
      </main>
      <footer class="admin-footer">BonsaiMarket · Điều hành marketplace cây cảnh Việt Nam</footer>
    </div>
    <dialog id="admin-drawer" ref="drawer" class="admin-drawer" aria-labelledby="admin-drawer-title" @click="backdrop"
            @close="drawerClosed">
      <div class="admin-drawer-header"><strong id="admin-drawer-title">BonsaiMarket · Quản trị</strong>
        <button type="button" class="admin-icon-button" aria-label="Đóng điều hướng" @click="drawer.close()">
          <MarketIcon name="close"/>
        </button>
      </div>
      <nav class="admin-nav" aria-label="Điều hướng quản trị trên điện thoại">
        <div v-for="group in groups" :key="group.title" class="admin-nav-group"><p>{{ group.title }}</p>
          <RouterLink v-for="item in group.items" :key="item.to" :to="item.to"
                      :aria-current="active(item)?'page':undefined" @click="drawer.close()">
            <MarketIcon :name="item.icon"/>
            <span>{{ item.label }}</span></RouterLink>
        </div>
        <RouterLink to="/" @click="drawer.close()">Về trang mua sắm</RouterLink>
      </nav>
    </dialog>
  </div>
</template>
<script setup>
import {computed, onBeforeUnmount, onMounted, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {useAuth} from '../composables/useAuth.js';
import {workspaceNavigation} from '../navigation/marketplace.js';
import MarketIcon from '../components/marketplace/MarketIcon.vue';
import AdminNotificationBell from '../components/admin/AdminNotificationBell.vue';
import '../css/admin.css';

const route = useRoute(), router = useRouter(), {user, logout} = useAuth(), collapsed = ref(false), drawer = ref(null),
    trigger = ref(null), drawerOpen = ref(false), account = ref(null), loggingOut = ref(false), authError = ref('');
const labels = ['Tổng quan', 'Quản lý sàn', 'Xử lý & kiểm duyệt', 'Công cụ & hệ thống'];
const menuOrder = ['/admin', '/admin/statistics', '/admin/users', '/admin/stores', '/admin/store-applications', '/admin/products', '/admin/categories', '/admin/orders', '/admin/transactions', '/admin/refunds', '/admin/disputes', '/admin/reports', '/admin/community', '/messages', '/admin/ai-assistant', '/admin/audit', '/admin/notifications', '/admin/profile'];
const groupOf = item => item.group ?? (['/admin', '/admin/statistics'].includes(item.to) ? 0 : ['/admin/users', '/admin/categories', '/admin/products', '/admin/orders', '/admin/store-applications', '/admin/stores'].includes(item.to) ? 1 : ['/admin/refunds', '/admin/reports', '/admin/community', '/messages', '/admin/disputes'].includes(item.to) ? 2 : 3);
const groups = computed(() => labels.map((title, index) => ({
  title,
  items: workspaceNavigation.admin.filter(i => groupOf(i) === index).sort((a, b) => menuOrder.indexOf(a.to) - menuOrder.indexOf(b.to))
})).filter(g => g.items.length));
const active = item => route.path === item.to || (item.to !== '/admin' && route.path.startsWith(item.to + '/'));
const title = computed(() => workspaceNavigation.admin.find(active)?.label || 'Quản trị sàn');
const initial = computed(() => (user.value?.name || 'Q').trim().slice(0, 1).toUpperCase());
const profilePath = computed(() => router.hasRoute('admin-profile') ? '/admin/profile' : '/profile');

function openDrawer() {
  drawer.value.showModal();
  drawerOpen.value = true;
}

function drawerClosed() {
  drawerOpen.value = false;
  if (trigger.value?.getClientRects().length) trigger.value.focus();
}

function backdrop(e) {
  if (e.target !== drawer.value) return;
  const r = drawer.value.getBoundingClientRect();
  if (e.clientX < r.left || e.clientX > r.right || e.clientY < r.top || e.clientY > r.bottom) drawer.value.close();
}

function closeAccount() {
  if (account.value?.open) {
    account.value.open = false;
    account.value.querySelector('summary').focus();
  }
}

function outside(e) {
  if (account.value?.open && !account.value.contains(e.target)) account.value.open = false;
}

let desktop;

function resize() {
  if (desktop.matches && drawer.value?.open) drawer.value.close();
}

onMounted(() => {
  desktop = matchMedia('(min-width:768px)');
  desktop.addEventListener('change', resize);
  document.addEventListener('click', outside);
});
onBeforeUnmount(() => {
  desktop?.removeEventListener('change', resize);
  document.removeEventListener('click', outside);
});
watch(() => route.fullPath, () => {
  drawer.value?.close();
  if (account.value) account.value.open = false;
});

async function signOut() {
  if (loggingOut.value) return;
  loggingOut.value = true;
  authError.value = '';
  try {
    await logout();
  } catch (e) {
    authError.value = e.message;
  } finally {
    loggingOut.value = false;
  }
}
</script>

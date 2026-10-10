<template>
  <div :class="'workspace-'+role" class="workspace-shell"><a class="skip-link" href="#workspace-main">Bỏ qua điều
    hướng</a>
    <aside class="workspace-sidebar">
      <RouterLink class="market-brand" to="/"><span class="brand-mark"><MarketIcon name="leaf"/></span><span>BonsaiMarket</span>
      </RouterLink>
      <p class="workspace-role-label">{{ role === 'admin' ? 'QUẢN TRỊ MARKETPLACE' : 'KÊNH NGƯỜI BÁN' }}</p>
      <nav aria-label="Điều hướng workspace">
        <RouterLink v-for="item in items" :key="item.to" :aria-current="current.fullPath===item.to?'page':undefined"
                    :class="{'workspace-link-active':current.path===item.to}"
                    :to="item.to">
          <MarketIcon :name="item.icon"/>
          {{ item.label }}
        </RouterLink>
      </nav>
      <div class="workspace-next"><p>CHỨC NĂNG SẮP TỚI</p><span v-for="label in future" :key="label">{{ label }} <small>Chưa triển khai</small></span>
      </div>
      <RouterLink class="workspace-back" to="/">Về marketplace
        <MarketIcon name="arrow"/>
      </RouterLink>
    </aside>
    <div class="workspace-body">
      <header class="workspace-topbar">
        <button ref="mobileTrigger" aria-controls="workspace-navigation" aria-haspopup="dialog"
                aria-label="Mở điều hướng workspace" class="workspace-mobile-toggle" @click="dialog.showModal()">
          <MarketIcon name="menu"/>
        </button>
        <div class="workspace-breadcrumbs">
          <RouterLink :to="base">{{ role === 'admin' ? 'Quản trị sàn' : 'Kênh người bán' }}</RouterLink>
          <span>/</span><strong>{{ pageTitle }}</strong></div>
        <span class="workspace-role-badge">{{ role === 'admin' ? 'Admin' : 'Seller' }} · Bản phát triển</span></header>
      <main id="workspace-main" class="workspace-main" tabindex="-1">
        <slot/>
      </main>
      <footer class="workspace-footer">BonsaiMarket ·
        {{ role === 'admin' ? 'Điều hành hoạt động của sàn' : 'Quản lý gian hàng độc lập' }}
      </footer>
    </div>
    <dialog id="workspace-navigation" ref="dialog" aria-labelledby="workspace-navigation-title"
            class="workspace-mobile-dialog" @click="closeOnBackdrop" @close="mobileTrigger?.focus()">
      <div class="workspace-dialog-header"><strong
          id="workspace-navigation-title">{{ role === 'admin' ? 'Quản trị sàn' : 'Kênh người bán' }}</strong>
        <button aria-label="Đóng điều hướng" @click="dialog.close()">
          <MarketIcon name="close"/>
        </button>
      </div>
      <nav>
        <RouterLink v-for="item in items" :key="item.to" :to="item.to" @click="dialog.close()">
          <MarketIcon :name="item.icon"/>
          {{ item.label }}
        </RouterLink>
        <RouterLink to="/" @click="dialog.close()">Về marketplace</RouterLink>
      </nav>
      <p class="muted">Các nghiệp vụ tài khoản, đơn hàng và quản trị đang được hoàn thiện.</p></dialog>
  </div>
</template>
<script setup>
import {computed, ref} from 'vue';
import {useRoute} from 'vue-router';
import MarketIcon from './MarketIcon.vue';

const props = defineProps({role: {type: String, default: 'seller'}}), current = useRoute(), dialog = ref(null),
    mobileTrigger = ref(null);
const base = computed(() => props.role === 'admin' ? '/admin' : '/seller');
const items = computed(() => props.role === 'admin' ? [{
  to: '/admin',
  label: 'Tổng quan sàn',
  icon: 'grid'
}, {to: '/admin/users', label: 'Người dùng', icon: 'users'}, {
  to: '/admin/products',
  label: 'Sản phẩm',
  icon: 'leaf'
}, {to: '/admin/orders', label: 'Đơn hàng', icon: 'bag'}] : [{
  to: '/seller',
  label: 'Tổng quan gian hàng',
  icon: 'grid'
}, {to: '/seller/products', label: 'Sản phẩm · bản xem trước', icon: 'leaf'}, {
  to: '/seller/orders',
  label: 'Đơn hàng',
  icon: 'bag'
}, {to: '/seller#revenue', label: 'Doanh thu', icon: 'chart'}]);
const future = computed(() => props.role === 'admin' ? ['Quản lý gian hàng', 'Xử lý báo cáo', 'Hoàn tiền'] : ['Đăng sản phẩm', 'Cài đặt gian hàng', 'Tin nhắn']);
const pageTitle = computed(() => items.value.find(i => i.to === current.path)?.label || 'Tổng quan');

function closeOnBackdrop(e) {
  if (e.target === dialog.value) {
    const r = dialog.value.getBoundingClientRect();
    if (e.clientX < r.left || e.clientX > r.right || e.clientY < r.top || e.clientY > r.bottom) dialog.value.close()
  }
}
</script>

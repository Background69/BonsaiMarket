<template>
  <MarketplaceLayout>
    <div class="max-w-7xl mx-auto px-6 py-8 flex gap-8 seller-shell">
      <!-- Sidebar -->
      <aside class="w-56 shrink-0 hidden lg:block">
        <div class="rounded-2xl border p-4 sticky top-24" style="background: white; border-color: var(--ag-border);">
          <div class="mb-4 pb-4 border-b" style="border-color: var(--ag-border);">
            <p class="text-sm font-semibold" style="color: var(--ag-on-surface);">{{
                store.name || 'Cửa hàng của tôi'
              }}</p>
            <p class="text-xs mt-0.5" style="color: var(--ag-on-surface-variant);">Quản lý cửa hàng</p>
          </div>
          <nav class="space-y-1">
            <Link v-for="item in navItems" :key="item.route" :class="route().current(item.pattern) ? 'sidebar-link-active' : 'sidebar-link'"
                  :href="route(item.route)"
                  class="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm transition-all duration-200">
              <span class="material-symbols-outlined text-lg">{{ item.icon }}</span>
              {{ item.label }}
            </Link>
          </nav>
        </div>
      </aside>

      <!-- Mobile Nav -->
      <div class="lg:hidden w-full mb-4">
        <div class="flex gap-2 overflow-x-auto pb-2 seller-mobile-nav">
          <Link v-for="item in navItems" :key="item.route" :class="route().current(item.pattern) ? 'bg-[var(--ag-primary-500)] text-white border-transparent' : 'bg-white text-[var(--ag-text-secondary)] border-[var(--ag-border)]'"
                :href="route(item.route)"
                class="px-4 py-2 rounded-xl text-sm font-medium whitespace-nowrap border transition-colors">
            <span class="material-symbols-outlined text-base align-middle mr-1.5">{{ item.icon }}</span>
            {{ item.label }}
          </Link>
        </div>
      </div>

      <!-- Main Content -->
      <div class="flex-1 min-w-0">
        <slot/>
      </div>
    </div>
  </MarketplaceLayout>
</template>

<script setup>
import {Link} from '@inertiajs/vue3'
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue'
import {usePage} from '@inertiajs/vue3'

const page = usePage()
const store = page.props.store || {}

const navItems = [
  {
    label: 'Tổng quan',
    icon: 'dashboard',
    route: 'agriverse.shop.seller.dashboard',
    pattern: 'agriverse.shop.seller.dashboard'
  },
  {
    label: 'Sản phẩm',
    icon: 'inventory_2',
    route: 'agriverse.shop.seller.products.index',
    pattern: 'agriverse.shop.seller.products.*'
  },
  {
    label: 'Hộ chiếu cây',
    icon: 'qr_code_2',
    route: 'agriverse.shop.seller.bonsai-passports.index',
    pattern: 'agriverse.shop.seller.bonsai-passports.*'
  },
  {
    label: 'Đơn hàng',
    icon: 'receipt_long',
    route: 'agriverse.shop.seller.orders.index',
    pattern: 'agriverse.shop.seller.orders.*'
  },
  {
    label: 'Đánh giá',
    icon: 'star',
    route: 'agriverse.shop.seller.reviews.index',
    pattern: 'agriverse.shop.seller.reviews.*'
  },
  {
    label: 'Cửa hàng',
    icon: 'store',
    route: 'agriverse.shop.seller.store.edit',
    pattern: 'agriverse.shop.seller.store.*'
  },
]
</script>

<style scoped>
.seller-shell {
  flex-direction: column;
}

@media (min-width: 1024px) {
  .seller-shell {
    flex-direction: row;
  }
}

.seller-mobile-nav a {
  flex-shrink: 0;
  white-space: nowrap;
  color: var(--ag-text-secondary);
  background: white;
  border: 1px solid var(--ag-border);
}

.seller-mobile-nav a.router-link-active {
  background: var(--ag-primary-500);
  color: white;
  border-color: var(--ag-primary-500);
}

.sidebar-link {
  color: var(--ag-text-secondary);
}

.sidebar-link:hover {
  background: color-mix(in srgb, var(--ag-primary-500) 6%, transparent);
  color: var(--ag-primary-500);
}

.sidebar-link-active {
  background: color-mix(in srgb, var(--ag-primary-500) 10%, transparent);
  color: var(--ag-primary-600);
  font-weight: 600;
}
</style>

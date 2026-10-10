<template>
  <WorkspaceShell role="seller">
    <slot/>
  </WorkspaceShell>
</template>
<script setup>
import WorkspaceShell from '../../../components/marketplace/WorkspaceShell.vue';
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
    label: 'Hộ chiếu cây (đang phát triển)',
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
    label: 'Đánh giá (đang phát triển)',
    icon: 'star',
    route: 'agriverse.shop.seller.reviews.index',
    pattern: 'agriverse.shop.seller.reviews.*'
  },
  {
    label: 'Gian hàng (đang phát triển)',
    icon: 'store',
    route: 'agriverse.shop.seller.store.edit',
    pattern: 'agriverse.shop.seller.store.*'
  },
]
</script>

<template>
  <MarketplaceLayout><main class="market-container market-page marketplace-surface">
    <header><h1>Đơn mua của bạn</h1><p>Theo dõi trạng thái, xem sản phẩm và hủy đơn trước khi giao.</p></header>
    <OrderList endpoint="/account/orders" detail-base="/orders" admin/>
  </main></MarketplaceLayout>
</template>
<script setup>
import MarketplaceLayout from '../../../layouts/MarketplaceLayout.vue';
import OrderList from '../../../components/marketplace/OrderList.vue';
</script>

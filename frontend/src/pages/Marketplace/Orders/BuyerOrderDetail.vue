<template>
  <MarketplaceLayout><main class="market-container market-page marketplace-surface">
    <OrderDetailView endpoint="/account/orders" list-path="/orders" buyer/>
  </main></MarketplaceLayout>
</template>
<script setup>
import MarketplaceLayout from '../../../layouts/MarketplaceLayout.vue';
import OrderDetailView from '../../../components/marketplace/OrderDetailView.vue';
</script>

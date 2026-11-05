<template>
  <MarketplaceLayout>
    <main class="market-container market-page">
      <header class="market-page-heading"><h1>Thông báo của bạn</h1>
        <p>Theo dõi hồ sơ gian hàng, báo cáo và hỗ trợ đơn hàng.</p></header>
      <NotificationCenter/>
    </main>
  </MarketplaceLayout>
</template>
<script setup>
import MarketplaceLayout from '../../layouts/MarketplaceLayout.vue';
import NotificationCenter from '../../components/marketplace/NotificationCenter.vue';
</script>

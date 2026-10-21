<template>
  <SellerLayout>
    <header class="management-page"><h1>Đơn hàng của gian hàng</h1><p>Xác nhận, giao và theo dõi các đơn khách đã đặt tại gian hàng của bạn.</p></header>
    <OrderList detail-base="/seller/orders" endpoint="/seller/orders"/>
  </SellerLayout>
</template>
<script setup>
import SellerLayout from '../SellerLayout.vue';
import OrderList from '../../../../components/marketplace/OrderList.vue';
</script>

<template>
  <SellerLayout>
    <OrderDetailView endpoint="/seller/orders" list-path="/seller/orders"/>
  </SellerLayout>
</template>
<script setup>
import SellerLayout from '../SellerLayout.vue';
import OrderDetailView from '../../../../components/marketplace/OrderDetailView.vue';
</script>

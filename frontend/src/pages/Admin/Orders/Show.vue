<template>
  <AdminLayout>
    <OrderDetailView endpoint="/admin/orders" list-path="/admin/orders"/>
  </AdminLayout>
</template>
<script setup>
import AdminLayout from '../../../layouts/AdminLayout.vue';
import OrderDetailView from '../../../components/marketplace/OrderDetailView.vue';
</script>

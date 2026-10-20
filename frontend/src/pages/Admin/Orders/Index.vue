<template>
  <AdminLayout>
    <header class="management-page"><h1>Đơn hàng</h1><p>Theo dõi và xử lý đơn hàng của toàn sàn.</p></header>
    <OrderList admin detail-base="/admin/orders" endpoint="/admin/orders"/>
  </AdminLayout>
</template>
<script setup>
import AdminLayout from '../../../layouts/AdminLayout.vue';
import OrderList from '../../../components/marketplace/OrderList.vue';
</script>

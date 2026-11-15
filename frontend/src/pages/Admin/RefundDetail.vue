<template>
  <AdminLayout>
    <AdminPageHeader title="Chi tiết hồ sơ hoàn tiền"
                     description="Lịch sử yêu cầu, quyết định nội bộ và tiến độ đối soát.">
      <RouterLink class="button button-outline" to="/admin/refunds">Danh sách hồ sơ</RouterLink>
      <RouterLink class="button button-outline" :to="'/admin/orders/'+route.params.id">Xem đơn liên quan</RouterLink>
    </AdminPageHeader>
    <OrderRefundPanel endpoint="/admin/orders" :order-id="route.params.id"/>
  </AdminLayout>
</template>
<script setup>
import {useRoute} from 'vue-router';
import AdminLayout from '../../layouts/AdminLayout.vue';
import AdminPageHeader from '../../components/admin/AdminPageHeader.vue';
import OrderRefundPanel from '../../components/marketplace/OrderRefundPanel.vue';

const route = useRoute();
</script>

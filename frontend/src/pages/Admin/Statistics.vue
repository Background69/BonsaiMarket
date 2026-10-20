<template>
  <AdminLayout>
    <div class="management-page">
      <header><h1>Thống kê</h1><p>Doanh thu theo khoảng ngày và tỷ trọng doanh thu theo danh mục sản phẩm.</p></header>
      <StatisticsPanel endpoint="/admin/statistics"/>
    </div>
  </AdminLayout>
</template>
<script setup>
import AdminLayout from '../../layouts/AdminLayout.vue';
import StatisticsPanel from '../../components/marketplace/StatisticsPanel.vue';
</script>

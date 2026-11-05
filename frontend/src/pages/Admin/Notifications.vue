<template>
  <AdminLayout>
    <AdminPageHeader title="Trung tâm thông báo"
                     description="Công việc và cập nhật dành riêng cho tài khoản quản trị hiện tại."/>
    <NotificationCenter admin/>
  </AdminLayout>
</template>
<script setup>
import AdminLayout from '../../layouts/AdminLayout.vue';
import AdminPageHeader from '../../components/admin/AdminPageHeader.vue';
import NotificationCenter from '../../components/marketplace/NotificationCenter.vue';
</script>

<template>
  <div v-if="data" class="admin-stat-grid">
    <AdminStatCard label="Tổng tài khoản" :value="data.users" icon="users"/>
    <AdminStatCard label="Đang hoạt động" :value="data.activeUsers" icon="check"/>
    <AdminStatCard label="Tài khoản bị khóa" :value="data.users-data.activeUsers" icon="user"/>
  </div>
  <p v-if="error" role="alert">{{ error }}
    <button class="button button-outline" @click="load">Thử lại</button>
  </p>
</template>
<script setup>
import AdminStatCard from './AdminStatCard.vue';
import {useAdminResource} from '../../composables/useAdminResource.js';

const {data, error, load} = useAdminResource(() => '/admin/overview');
defineExpose({load});
</script>

<template>
  <div v-if="data" class="admin-stat-grid">
    <AdminStatCard label="Tổng đơn trong kỳ" :value="total" icon="bag"/>
    <AdminStatCard label="Chờ xác nhận" :value="data.statusCounts.PENDING" icon="clock"/>
    <AdminStatCard label="Đã giao / hoàn tất" :value="data.statusCounts.DELIVERED+data.statusCounts.COMPLETED"
                   icon="check"/>
  </div>
  <p v-if="error" class="management-error" role="alert">{{ error }}
    <button class="button button-outline" @click="load">Tải lại thống kê</button>
  </p>
  <nav class="admin-tabs" aria-label="Lọc trạng thái đơn hàng">
    <RouterLink v-for="status in statuses" :key="status"
                :to="{path:route.path,query:{...route.query,status,page:undefined}}"
                :aria-current="(route.query.status||'')===status?'page':undefined">
      {{ status ? statusLabel(status) : 'Tất cả đơn' }}
    </RouterLink>
  </nav>
</template>
<script setup>
import {computed} from 'vue';
import {useRoute} from 'vue-router';
import AdminStatCard from './AdminStatCard.vue';
import {useAdminResource} from '../../composables/useAdminResource.js';
import {statusLabel} from '../../utils.js';

const route = useRoute(), {
  data,
  error,
  load
} = useAdminResource(() => '/admin/statistics?' + new URLSearchParams({
  from: route.query.from || '',
  to: route.query.to || ''
}));
const total = computed(() => Object.values(data.value?.statusCounts || {}).reduce((a, b) => a + b, 0)),
    statuses = ['', 'PENDING', 'CONFIRMED', 'SHIPPING', 'DELIVERED', 'COMPLETED', 'CANCELLED'];
</script>

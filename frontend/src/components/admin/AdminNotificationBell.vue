<template>
  <RouterLink class="admin-notification-bell admin-icon-button" to="/admin/notifications"
              :aria-label="error?'Thông báo: chưa tải được số chưa đọc':data?'Thông báo: '+data.unread+' chưa đọc':'Thông báo: đang kiểm tra'"
              :title="error||'Trung tâm thông báo'">
    <MarketIcon name="bell"/>
    <span v-if="data?.unread" class="admin-unread-count">{{ data.unread > 99 ? '99+' : data.unread }}</span><span
      v-if="error" aria-hidden="true">!</span></RouterLink>
</template>
<script setup>
import {onBeforeUnmount, onMounted} from 'vue';
import MarketIcon from '../marketplace/MarketIcon.vue';
import {useAdminResource} from '../../composables/useAdminResource.js';

const {data, error, load} = useAdminResource(() => '/admin/notifications/count');
let timer;

function refresh() {
  if (!document.hidden) load();
}

onMounted(() => {
  timer = setInterval(refresh, 30000);
  window.addEventListener('bonsai:notifications-updated', refresh);
  document.addEventListener('visibilitychange', refresh);
});
onBeforeUnmount(() => {
  clearInterval(timer);
  window.removeEventListener('bonsai:notifications-updated', refresh);
  document.removeEventListener('visibilitychange', refresh);
});
</script>

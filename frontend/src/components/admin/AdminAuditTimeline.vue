<template>
  <ol v-if="entries.length" class="admin-timeline">
    <li v-for="entry in entries" :key="entry.id"><strong>{{ entry.action }}</strong>
      <time :datetime="entry.createdAt">{{ dateTime(entry.createdAt) }} · {{ entry.actor }}</time>
      <p v-if="entry.before||entry.after">{{ entry.before || 'Chưa ghi nhận' }} → {{
          entry.after || 'Chưa ghi nhận'
        }}</p>
      <p v-if="entry.reason">{{ entry.reason }}</p></li>
  </ol>
  <AdminEmptyState v-else title="Chưa có lịch sử quản trị"
                   description="Các quyết định phát sinh từ thời điểm ghi nhận sẽ hiển thị tại đây." icon="clock"/>
</template>
<script setup>
import AdminEmptyState from './AdminEmptyState.vue';
import {dateTime} from '../../services/adminPresentation.js';

defineProps({entries: {type: Array, default: () => []}});
</script>

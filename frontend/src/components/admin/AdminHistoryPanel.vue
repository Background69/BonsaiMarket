<template>
  <section class="market-panel"><h2>{{ title }}</h2>
    <p v-if="loading" role="status">Đang tải lịch sử…</p>
    <div v-else-if="error" class="management-error" role="alert"><p>{{ error }}</p>
      <button class="button button-outline" @click="load">Thử lại</button>
    </div>
    <AdminAuditTimeline v-else :entries="data||[]"/>
  </section>
</template>
<script setup>
import AdminAuditTimeline from './AdminAuditTimeline.vue';
import {useAdminResource} from '../../composables/useAdminResource.js';

const props = defineProps({
  endpoint: {type: String, required: true},
  title: {type: String, default: 'Lịch sử quản trị'}
}), {data, loading, error, load} = useAdminResource(() => props.endpoint);
</script>

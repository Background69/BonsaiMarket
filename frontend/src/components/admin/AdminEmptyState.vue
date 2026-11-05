<template>
  <div class="admin-empty" role="status">
    <MarketIcon :name="icon"/>
    <h2>{{ title }}</h2>
    <p>{{ description }}</p>
    <slot/>
  </div>
</template>
<script setup>
import MarketIcon from '../marketplace/MarketIcon.vue';

defineProps({
  title: {type: String, default: 'Chưa có dữ liệu'},
  description: {type: String, default: 'Dữ liệu mới sẽ xuất hiện tại đây khi phát sinh.'},
  icon: {type: String, default: 'list'}
});
</script>
<style scoped>
.admin-empty {
  padding: var(--bm-space-12) var(--bm-space-6);
  display: grid;
  justify-items: center;
  text-align: center;
  gap: var(--bm-space-3)
}

.admin-empty > svg {
  width: calc(var(--bm-space-8) + var(--bm-space-1));
  height: calc(var(--bm-space-8) + var(--bm-space-1));
  color: var(--bm-forest)
}

.admin-empty :is(h2,p) {
  margin: 0;
  max-width: 60ch
}

.admin-empty h2 {
  font-size: var(--bm-type-subheading)
}

.admin-empty p {
  color: var(--bm-muted)
}
</style>

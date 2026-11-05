<template>
  <article class="admin-stat">
    <div class="admin-stat-top"><span>{{ label }}</span>
      <MarketIcon :name="icon"/>
    </div>
    <strong>{{ value }}</strong>
    <p v-if="note">{{ note }}</p></article>
</template>
<script setup>
import MarketIcon from '../marketplace/MarketIcon.vue';

defineProps({
  label: {type: String, required: true},
  value: {type: [String, Number], required: true},
  icon: {type: String, default: 'chart'},
  note: String
});
</script>

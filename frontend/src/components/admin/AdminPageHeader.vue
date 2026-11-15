<template>
  <header class="admin-page-header">
    <div><p v-if="eyebrow" class="admin-eyebrow">{{ eyebrow }}</p>
      <h1>{{ title }}</h1>
      <p v-if="description">{{ description }}</p></div>
    <div v-if="$slots.default" class="admin-header-actions">
      <slot/>
    </div>
  </header>
</template>
<script setup>
defineProps({title: {type: String, required: true}, description: String, eyebrow: String});
</script>

<template>
  <div class="home-section-heading" :class="{'home-section-heading--center': center}">
    <div><p class="bm-eyebrow">{{ eyebrow }}</p><h2>{{ title }} <span v-if="accent">{{ accent }}</span></h2></div>
    <slot/>
  </div>
</template>
<script setup>
defineProps({eyebrow: String, title: String, accent: String, center: Boolean});
</script>

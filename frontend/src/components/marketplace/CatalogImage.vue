<template>
  <div :class="{'catalog-image--avatar':avatar,'catalog-image--loaded':loaded,'catalog-image--fallback':useFallback}"
       :style="{aspectRatio:ratio}"
       class="catalog-image">
    <img v-if="activeSrc && !failed" :key="activeSrc" :alt="alt" :fetchpriority="priority?'high':'auto'" :loading="priority?'eager':'lazy'"
         :src="activeSrc" :style="{objectFit:fit}" decoding="async" @error="onError"
         @load="loaded=true"/>
    <div v-if="!loaded" :aria-label="alt ? alt+' — '+label : label" class="catalog-image-placeholder" role="img">
      <span v-if="avatar" aria-hidden="true" class="store-initial">{{
          alt?.trim().charAt(0).toUpperCase() || 'G'
        }}</span>
      <template v-else>
        <svg aria-hidden="true" class="catalog-placeholder-plant" fill="none" viewBox="0 0 80 80">
          <path d="M39 58V27m0 16L23 32m16 6 15-17" stroke="currentColor" stroke-linecap="round" stroke-width="2"/>
          <path d="M39 34C24 37 15 29 15 19c14-2 24 3 24 15Zm2-6C40 13 49 7 63 7c0 13-8 21-22 21Z" fill="currentColor"
                opacity=".28"/>
          <path d="M24 56h32l-5 18H29l-5-18Z" fill="currentColor" opacity=".25"/>
          <path d="M22 56h36m-26 5 2 8m13-8-2 8" stroke="currentColor" stroke-linecap="round" stroke-width="2"/>
        </svg>
        <span>{{ label }}</span></template>
    </div>
  </div>
</template>
<script setup>
import {computed, ref, watch} from 'vue';

const props = defineProps({
  src: String,
  fallback: String,
  alt: String,
  ratio: {type: String, default: '1'},
  avatar: Boolean,
  priority: Boolean,
  label: {type: String, default: 'Chưa có ảnh'},
  fit: {type: String, default: 'contain'}
});
const failed = ref(false), loaded = ref(false), useFallback = ref(false);
const safeUrl = url => typeof url === 'string' && /^(https?:\/\/|\/[^/])/.test(url) ? url : '';
const activeSrc = computed(() => useFallback.value ? safeUrl(props.fallback) : safeUrl(props.src) || safeUrl(props.fallback));
watch(() => [props.src, props.fallback], () => {
  failed.value = false;
  loaded.value = false;
  useFallback.value = false
});

function onError() {
  loaded.value = false;
  if (!useFallback.value && safeUrl(props.fallback) && activeSrc.value !== safeUrl(props.fallback)) useFallback.value = true; else failed.value = true;
}
</script>

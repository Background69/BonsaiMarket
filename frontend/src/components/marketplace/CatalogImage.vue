<template>
  <div :class="{'catalog-image--avatar':avatar,'catalog-image--loaded':loaded,'catalog-image--fallback':useFallback}"
       :style="{aspectRatio:ratio}" :data-image-state="loaded?'loaded':failed?'missing':activeSrc?'loading':'missing'"
       class="catalog-image">
    <img v-if="activeSrc && !failed" :key="activeSrc" :alt="alt" :fetchpriority="priority?'high':'auto'" :loading="priority?'eager':'lazy'"
         :src="activeSrc" :style="{objectFit:fit}" decoding="async" @error="onError"
         @load="loaded=true; emit('load')"/>
    <div v-if="!loaded" :aria-label="alt ? alt+' — '+stateLabel : stateLabel" class="catalog-image-placeholder" role="img">
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
        <span>{{ stateLabel }}</span></template>
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
const emit = defineEmits(['load','error']);
const safeUrl = url => typeof url === 'string' && /^(https?:\/\/|\/[^/])/.test(url) ? url : '';
const activeSrc = computed(() => useFallback.value ? safeUrl(props.fallback) : safeUrl(props.src) || safeUrl(props.fallback));
const stateLabel = computed(() => activeSrc.value && !failed.value ? 'Đang tải ảnh' : props.label);
watch(() => [props.src, props.fallback], () => {
  failed.value = false;
  loaded.value = false;
  useFallback.value = false
});

function onError() {
  emit('error');
  loaded.value = false;
  if (!useFallback.value && safeUrl(props.fallback) && activeSrc.value !== safeUrl(props.fallback)) useFallback.value = true; else failed.value = true;
}
</script>
<style scoped>
.catalog-image {position:relative;width:100%;overflow:hidden;isolation:isolate;background:var(--bm-soft);}
.catalog-image img {position:absolute;inset:0;width:100%;height:100%;z-index:1;}
.catalog-image-placeholder {position:absolute;inset:0;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:8px;color:var(--bm-muted);background:var(--bm-soft);}
.catalog-image-placeholder>span {font-size:var(--bm-type-meta);line-height:1.5;color:var(--bm-muted);}
.catalog-placeholder-plant {width:40px;height:40px;}
.catalog-image--avatar {border-radius:50%;}
.catalog-image--avatar img {object-fit:cover;}
.catalog-image--avatar .store-initial {font-size:18px;font-weight:600;color:var(--bm-forest);}
</style>

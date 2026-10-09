<template>
  <div class="home-image" :class="[`home-image--${variant}`, { 'home-image--loaded': loaded }]" :style="{aspectRatio: ratio}">
    <div class="home-image-art" aria-hidden="true">
      <svg viewBox="0 0 400 440" fill="none" class="botanical-art">
        <circle cx="205" cy="215" r="155" class="art-halo"/>
        <path d="M197 336C200 275 206 172 204 86M200 279L144 224M204 241L260 180M204 192L155 144M203 157L249 108" class="art-stem"/>
        <path d="M201 310C126 315 85 271 87 226C143 218 186 251 201 310Z" class="art-leaf"/>
        <path d="M203 275C276 278 322 230 312 186C253 192 218 224 203 275Z" class="art-leaf-light"/>
        <path d="M203 227C133 232 102 179 112 144C165 145 192 180 203 227Z" class="art-leaf-light"/>
        <path d="M205 194C262 193 294 156 282 112C237 115 216 155 205 194Z" class="art-leaf"/>
        <path d="M204 150C160 139 143 101 152 71C190 81 206 112 204 150Z" class="art-leaf"/>
        <path d="M204 120C240 100 251 57 230 35C207 55 196 91 204 120Z" class="art-leaf-light"/>
        <path d="M151 332H254L241 404H165L151 332Z" class="art-pot"/>
        <path d="M148 331H257V344H148Z" class="art-pot-rim"/>
        <path d="M173 350L179 390M191 350L193 390M210 350L209 390M231 350L226 390" class="art-pot-line"/>
        <ellipse cx="203" cy="415" rx="75" ry="8" class="art-shadow"/>
      </svg>
      <span v-if="label" class="home-image-label">{{ label }}</span>
    </div>
    <img v-if="activeSrc && !failed" :key="activeSrc" :src="activeSrc" :alt="alt" :loading="priority ? 'eager' : 'lazy'"
         :fetchpriority="priority ? 'high' : 'auto'" :style="{objectFit: fit}" decoding="async" @load="loaded = true" @error="onError"/>
    <span v-if="!loaded" class="sr-only">{{ alt }} — khung minh họa chờ ảnh</span>
  </div>
</template>

<script setup>
import {computed, ref, watch} from 'vue';
const props = defineProps({src: String, fallback: String, alt: {type: String, default: ''}, label: String,
  ratio: {type: String, default: '1 / 1'}, fit: {type: String, default: 'cover'},
  variant: {type: String, default: 'plant'}, priority: Boolean});
const failed = ref(false);
const loaded = ref(false);
const useFallback = ref(false);
const activeSrc = computed(() => useFallback.value ? props.fallback : (props.src || props.fallback));
watch(() => [props.src, props.fallback], () => {failed.value = false; loaded.value = false; useFallback.value = false;});
function onError() {
  loaded.value = false;
  if (props.src && props.fallback && props.src !== props.fallback && !useFallback.value) useFallback.value = true;
  else failed.value = true;
}
</script>

<style scoped>
.home-image { position: relative; width: 100%; overflow: hidden; background: var(--bm-image-bg, #edf1e9); isolation: isolate; }
.home-image-art { position: absolute; inset: 0; display: grid; place-items: center; }
.botanical-art { width: 72%; height: 85%; max-height: 560px; }
.art-halo { fill: var(--bm-art-halo, #e0e9da); }
.art-stem { stroke: #597453; stroke-width: 4; stroke-linecap: round; }
.art-leaf { fill: var(--bm-art-leaf, #628064); }
.art-leaf-light { fill: var(--bm-art-light, #9cb28d); }
.art-pot { fill: #d8cbb3; }
.art-pot-rim { fill: #cabda6; }
.art-pot-line { stroke: #f2ecdf; stroke-width: 3; }
.art-shadow { fill: #344e3520; }
.home-image-label { position: absolute; bottom: 18px; font-size: 10px; font-weight: 500; letter-spacing: .16em; text-transform: uppercase; color: #50604d; }
.home-image img { position: absolute; inset: 0; width: 100%; height: 100%; }
.home-image--loaded .home-image-art { visibility: hidden; }
.home-image--lifestyle { --bm-image-bg: #eeeae1; --bm-art-halo: #e1ddcf; }
.home-image--lifestyle .botanical-art { transform: rotate(-9deg) scale(1.1); }
.home-image--gold { --bm-image-bg: #f8d73f; --bm-art-halo: #eac743; --bm-art-leaf: #315b40; --bm-art-light: #6f9252; }
.home-image--sage { --bm-image-bg: #dae5d5; --bm-art-halo: #c4d5be; }
.home-image--forest { --bm-image-bg: #174f36; --bm-art-halo: #286044; --bm-art-leaf: #85a478; --bm-art-light: #b2c49c; }
.home-image--forest .home-image-label { color: #edf2e8; }
</style>

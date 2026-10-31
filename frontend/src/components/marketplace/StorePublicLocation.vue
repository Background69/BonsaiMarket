<template>
  <section v-if="location || error" class="store-public-location" aria-labelledby="store-location-title">
    <h2 id="store-location-title">{{ location?.mode === 'AREA' ? 'Khu vực hoạt động' : 'Vị trí cửa hàng' }}</h2><p v-if="error" role="alert">Chưa tải được vị trí. <button class="text-link" type="button" @click="load">Thử lại</button></p>
    <template v-if="location"><p>{{ [location.publicAddress, location.ward, location.region].filter(Boolean).join(', ') }}</p><p v-if="location.mode === 'AREA'">Người bán chỉ công khai khu vực hoạt động. Đây không phải địa chỉ để đến mua trực tiếp.</p><template v-else><button class="button button-outline" type="button" :aria-expanded="showMap" @click="showMap = !showMap">{{ showMap ? 'Đóng bản đồ cửa hàng' : 'Xem bản đồ cửa hàng' }}</button><StoreLocationMap v-if="showMap" :locations="[location]"/></template></template>
  </section>
</template>
<script setup>
import {defineAsyncComponent, onBeforeUnmount, ref, watch} from 'vue';import {apiGet} from '../../services/api.js';
const StoreLocationMap = defineAsyncComponent(() => import('./StoreLocationMap.vue'));
const props = defineProps({storeId: {type: Number, required: true}});const location = ref(null), error = ref(false), showMap = ref(false); let sequence = 0;
async function load() {const ticket = ++sequence; location.value = null; error.value = false; showMap.value = false; try {const result = await apiGet('/stores/locations?storeId=' + props.storeId); if (ticket === sequence) location.value = result.content[0] || null;} catch {if (ticket === sequence) error.value = true;}}
watch(() => props.storeId, load, {immediate: true});onBeforeUnmount(() => sequence++);
</script>
<style scoped>
.store-public-location {border-top:1px solid var(--bm-border);padding-top:28px;margin-block:28px;max-width:850px;}.store-public-location h2 {font-size:var(--bm-type-section);margin:0 0 12px;}.store-public-location p {line-height:1.65;max-width:70ch;overflow-wrap:anywhere;}.store-public-location button {margin-bottom:16px;}
</style>

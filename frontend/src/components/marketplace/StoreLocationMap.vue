<template>
  <div class="store-location-map">
    <p v-if="!configured" role="status">Bản đồ chưa khả dụng. Bạn vẫn có thể xem vị trí và mở gian hàng từ danh sách.</p>
    <template v-else>
      <p v-if="loading" role="status">Đang tải bản đồ…</p><p v-if="error" role="alert">{{ error }}</p>
      <div ref="canvas" class="map-canvas" role="region" :aria-label="editable ? 'Chọn vị trí công khai trên bản đồ' : 'Bản đồ vị trí gian hàng công khai'"></div>
      <p class="map-help">{{ editable ? 'Bấm bản đồ để chọn vị trí. Bạn cũng có thể nhập vĩ độ, kinh độ bằng bàn phím.' : 'Chỉ ghim địa chỉ được người bán đồng ý công khai. Khu vực hoạt động nằm trong danh sách, không đặt pin gần đúng.' }}</p>
    </template>
  </div>
</template>
<script setup>
import {computed, onMounted, onBeforeUnmount, ref, watch} from 'vue';
const props = defineProps({locations: {type: Array, default: () => []}, editable: Boolean, latitude: [Number, String], longitude: [Number, String]});
const emit = defineEmits(['pick']);
const canvas = ref(null), loading = ref(true), error = ref('');
// Public OSM is best-effort interactive viewing only. An explicit empty URL disables maps.
// Environment overrides remain available for a licensed deployment provider.
const tileUrl = import.meta.env.VITE_MAP_TILE_URL ?? 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';
const attribution = import.meta.env.VITE_MAP_ATTRIBUTION ?? 'OpenStreetMap';
const configured = computed(() => {
  try {const u = new URL(tileUrl.replaceAll('{z}', '1').replaceAll('{x}', '1').replaceAll('{y}', '1').replaceAll('{s}', 'a')); return u.protocol === 'https:' && !u.username && !u.password && attribution.trim() && ['{z}', '{x}', '{y}'].every(p => tileUrl.includes(p));} catch {return false;}
});
let map, group, L, disposed = false;
function point(lat, lng) {return lat !== '' && lat != null && lng !== '' && lng != null && Number.isFinite(+lat) && Number.isFinite(+lng) && Math.abs(+lat) <= 90 && Math.abs(+lng) <= 180 && !(+lat === 0 && +lng === 0);}
function render() {
  if (!map) return;
  group.clearLayers(); const points = [];
  function marker(lat, lng, name) {points.push([+lat, +lng]); return L.marker([+lat, +lng], {title: name, alt: name, keyboard: true, icon: L.divIcon({className: 'public-store-pin', html: '', iconSize: [20, 20], iconAnchor: [10, 10]})}).addTo(group);}
  for (const s of props.locations.slice(0, 50)) {
    if (s.mode !== 'EXACT' || !point(s.latitude, s.longitude)) continue;
    const content = document.createElement('div'), title = document.createElement('strong'), detail = document.createElement('p'), link = document.createElement('a');
    title.textContent = s.name; detail.textContent = [s.publicAddress, s.ward, s.region].join(', ');
    link.textContent = 'Xem gian hàng'; link.setAttribute('href', '/stores/' + encodeURIComponent(s.storeId)); content.append(title, detail);
    if (s.description) {const description = document.createElement('p'); description.textContent = s.description.slice(0, 180); content.append(description);}
    content.append(link); marker(s.latitude, s.longitude, s.name).bindPopup(content);
  }
  if (props.editable && point(props.latitude, props.longitude)) marker(props.latitude, props.longitude, 'Vị trí công khai đang chọn');
  if (points.length) map.fitBounds(L.latLngBounds(points), {padding: [36, 36], maxZoom: 15, animate: false});
}
onMounted(async () => {
  if (!configured.value) return;
  try {
    const [leaflet] = await Promise.all([import('leaflet'), import('leaflet/dist/leaflet.css')]); if (disposed) return; L = leaflet.default;
    map = L.map(canvas.value, {scrollWheelZoom: false}).setView([16, 106], 5);
    const positionControls = () => map.zoomControl.setPosition(matchMedia('(max-width:600px)').matches ? 'bottomright' : 'topleft');
    positionControls(); map.on('resize', positionControls);
    const text = document.createElement('span'); text.textContent = attribution;
    group = L.layerGroup().addTo(map); render();
    const tiles = L.tileLayer(tileUrl, {maxZoom: 18, keepBuffer: 0, updateWhenIdle: true, referrerPolicy: 'strict-origin-when-cross-origin', attribution: text.outerHTML + ' · <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">© OpenStreetMap contributors</a>'}).addTo(map);
    tiles.on('tileerror', () => {error.value = 'Chưa tải được nền bản đồ. Kiểm tra kết nối hoặc xem vị trí trong danh sách.';});
    if (props.editable) map.on('click', e => emit('pick', {latitude: +e.latlng.lat.toFixed(7), longitude: +e.latlng.lng.toFixed(7)}));
  } catch {error.value = 'Bản đồ chưa tải được. Bạn vẫn có thể sử dụng danh sách hoặc nhập tọa độ.';} finally {loading.value = false;}
});
watch(() => [props.locations, props.latitude, props.longitude], render, {deep: true});
onBeforeUnmount(() => {disposed = true; map?.remove();});
</script>
<style scoped>
.store-location-map {min-width:0;isolation:isolate;}
.map-canvas {height:360px;width:100%;z-index:0;border:1px solid var(--bm-border);border-radius:var(--bm-radius);background:var(--bm-bg);}
.map-help,.store-location-map>p {font-size:var(--bm-type-label);line-height:1.65;color:var(--bm-muted);max-width:75ch;margin-block:12px;}
.map-canvas :deep(.public-store-pin) {background:var(--bm-forest);border:3px solid var(--bm-surface);border-radius:50%;box-shadow:0 2px 6px rgb(11 61 43 / .3);}
.map-canvas :deep(.leaflet-popup-content) {font-family:inherit;font-size:14px;line-height:1.6;max-width:230px;overflow-wrap:anywhere;}
.map-canvas :deep(.leaflet-popup-content a) {color:var(--bm-forest);font-weight:700;}
.map-canvas :deep(:focus-visible) {outline:3px solid var(--bm-forest);outline-offset:3px;}
@media(max-width:600px) {
  .map-canvas {height:300px;}
  .map-canvas :deep(.leaflet-control-zoom a) {width:44px;height:44px;line-height:44px;}
  .map-canvas :deep(.leaflet-popup-close-button) {width:44px;height:44px;line-height:44px;font-size:24px;}
  .map-canvas :deep(.leaflet-popup-content) {margin-right:44px;}
}
@media(prefers-reduced-motion:reduce) {.map-canvas :deep(*) {transition:none !important;animation:none !important;}}
</style>

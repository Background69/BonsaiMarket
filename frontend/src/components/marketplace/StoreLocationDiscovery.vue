<template>
  <section class="location-discovery" aria-labelledby="location-discovery-title">
    <h2 id="location-discovery-title">Gian hàng có vị trí công khai</h2><p>Chỉ hiển thị gian hàng có hồ sơ người bán đã được duyệt và đã đồng ý công bố vị trí.</p>
    <form class="location-region-filter" @submit.prevent="page = 0; load()"><label for="map-region">Lọc tỉnh/thành</label><div><input id="map-region" v-model="region" maxlength="120" placeholder="Tên tỉnh/thành đã công bố"/><button class="button button-outline" type="submit">Lọc vị trí</button></div></form>
    <p v-if="loading" role="status">Đang tải vị trí gian hàng…</p><div v-else-if="error" role="alert"><p>{{ error }}</p><button class="button button-outline" type="button" @click="load">Tải lại vị trí</button></div>
    <template v-else>
      <div v-if="rows.length" class="location-split"><StoreLocationMap :locations="rows"/><ul class="location-results" aria-label="Danh sách vị trí công khai"><li v-for="s in rows" :key="s.storeId"><h3><RouterLink :to="'/stores/' + s.storeId">{{ s.name }}</RouterLink></h3><p class="location-kind">{{ s.mode === 'EXACT' ? 'Địa chỉ cửa hàng' : 'Khu vực hoạt động' }}</p><p>{{ [s.publicAddress, s.ward, s.region].filter(Boolean).join(', ') }}</p><p v-if="s.mode === 'AREA'" class="location-kind">Không ghim địa chỉ để đến mua trực tiếp.</p></li></ul></div>
      <div v-else class="market-empty"><h3>Chưa có vị trí công khai phù hợp</h3><p>Bạn vẫn có thể tìm sản phẩm và mở gian hàng trong chế độ danh sách.</p></div>
      <nav v-if="page || hasNext" class="location-pagination" aria-label="Phân trang vị trí"><button class="button button-outline" :disabled="page === 0" type="button" @click="changePage(-1)">Trang trước</button><span>Trang {{ page + 1 }}</span><button class="button button-outline" :disabled="!hasNext" type="button" @click="changePage(1)">Trang sau</button></nav>
    </template>
  </section>
</template>
<script setup>
import {defineAsyncComponent, onBeforeUnmount, ref, watch} from 'vue';import {apiGet} from '../../services/api.js';
const StoreLocationMap = defineAsyncComponent(() => import('./StoreLocationMap.vue'));
const props = defineProps({search: String, categoryId: [String, Number]});
const rows = ref([]), region = ref(''), page = ref(0), hasNext = ref(false), loading = ref(true), error = ref('');let sequence = 0;
async function load() {
  const ticket = ++sequence; loading.value = true; error.value = ''; rows.value = [];const params = new URLSearchParams({page: String(page.value)});
  if (props.search) params.set('search', props.search); if (props.categoryId) params.set('categoryId', props.categoryId); if (region.value.trim()) params.set('region', region.value.trim());
  try {const result = await apiGet('/stores/locations?' + params); if (ticket === sequence) {rows.value = result.content; hasNext.value = result.hasNext;}}catch (e) {if (ticket === sequence) error.value = e.message;}finally {if (ticket === sequence) loading.value = false;}
}
function changePage(delta) {page.value += delta; load();}
watch(() => [props.search, props.categoryId], () => {page.value = 0; load();}, {immediate: true});onBeforeUnmount(() => sequence++);
</script>
<style scoped>
.location-discovery {margin-block:28px;padding-bottom:64px;}.location-discovery h2 {font-size:var(--bm-type-section);margin-bottom:12px;}.location-discovery>p {max-width:70ch;color:var(--bm-muted);line-height:1.65;}
.location-region-filter {display:grid;gap:8px;margin-block:24px;max-width:600px;}.location-region-filter>div {display:flex;gap:12px;}.location-region-filter input {min-width:0;flex:1;min-height:44px;border:1px solid var(--bm-control-border);border-radius:var(--bm-radius-control);padding:10px 12px;font:inherit;background:var(--bm-surface);color:var(--bm-ink);}
.location-split {display:grid;grid-template-columns:minmax(0,1.7fr) minmax(240px,1fr);gap:28px;align-items:start;}.location-results {list-style:none;margin:0;padding:0;max-height:430px;overflow:auto;}.location-results li {padding:16px 0;border-bottom:1px solid var(--bm-border);overflow-wrap:anywhere;}.location-results li:first-child {padding-top:0;}.location-results h3 {font-size:var(--bm-type-body);margin:0 0 8px;}.location-results p {font-size:var(--bm-type-label);line-height:1.6;margin:6px 0;}.location-kind {color:var(--bm-muted);}
.location-results a {color:var(--bm-forest);font-weight:700;text-decoration:underline;text-underline-offset:3px;}.location-results a:hover {color:var(--bm-dark);}
.location-pagination {display:flex;align-items:center;flex-wrap:wrap;gap:16px;margin-top:24px;}.location-discovery :focus-visible {outline:3px solid var(--bm-forest);outline-offset:3px;}
@media(max-width:768px) {.location-split {grid-template-columns:1fr;}.location-results {max-height:none;}}@media(max-width:480px) {.location-region-filter>div {flex-direction:column;}}
</style>

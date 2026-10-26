<template>
  <section class="location-editor" aria-labelledby="location-editor-title">
    <h2 id="location-editor-title">Vị trí công khai</h2><p class="location-explanation">Bạn quyết định vị trí khách được xem. Phần này không sử dụng địa chỉ tài khoản hoặc địa chỉ kho lấy hàng.</p>
    <p v-if="loading" role="status">Đang tải vị trí…</p><p v-if="error" role="alert">{{ error }}</p><p v-if="success" role="status">{{ success }}</p>
    <button v-if="!loaded && !loading" class="button button-outline" type="button" @click="load">Tải lại vị trí</button>
    <template v-if="loaded">
      <p v-if="!eligible" class="location-explanation">Cần chủ sở hữu xác thực email, hồ sơ Seller đã duyệt và gian hàng hoạt động để công khai vị trí. Gian hàng cũ thiếu hồ sơ duyệt cần quản trị viên đối chiếu.</p>
      <form class="location-form" :aria-busy="busy" @submit.prevent="save">
        <fieldset :disabled="busy || loading || (!eligible && !saved)">
          <label for="location-mode">Thông tin khách được xem</label><select id="location-mode" v-model="draft.mode" @change="consent = false"><option value="AREA">Khu vực hoạt động — không ghim địa chỉ</option><option value="EXACT">Địa chỉ cửa hàng — vị trí chính xác</option></select>
          <div class="location-fields"><div><label for="location-region">Tỉnh/thành công khai</label><input id="location-region" v-model="draft.region" required maxlength="120" autocomplete="off" @input="consent = false"/></div><div><label for="location-ward">Phường/xã công khai</label><input id="location-ward" v-model="draft.ward" required maxlength="120" autocomplete="off" @input="consent = false"/></div></div>
          <template v-if="draft.mode === 'EXACT'">
            <label for="location-address">Địa chỉ cửa hàng được phép đến</label><input id="location-address" v-model="draft.publicAddress" required maxlength="500" autocomplete="off" @input="consent = false"/>
            <div class="location-fields"><div><label for="location-latitude">Vĩ độ (WGS84)</label><input id="location-latitude" v-model="draft.latitude" type="number" step="0.0000001" min="-90" max="90" required @input="consent = false"/></div><div><label for="location-longitude">Kinh độ (WGS84)</label><input id="location-longitude" v-model="draft.longitude" type="number" step="0.0000001" min="-180" max="180" required @input="consent = false"/></div></div>
            <button class="button button-outline" type="button" :aria-expanded="showMap" @click="showMap = !showMap">{{ showMap ? 'Đóng bản đồ chọn vị trí' : 'Mở bản đồ chọn vị trí' }}</button><StoreLocationMap v-if="showMap" editable :latitude="draft.latitude" :longitude="draft.longitude" @pick="pick"/>
          </template>
          <div class="location-preview"><h3>Khách sẽ thấy</h3><p>{{ draft.mode === 'AREA' ? 'Khu vực hoạt động' : 'Địa chỉ cửa hàng' }}: {{ [draft.mode === 'EXACT' ? draft.publicAddress : '', draft.ward, draft.region].filter(Boolean).join(', ') || 'Chưa nhập vị trí' }}</p><p v-if="draft.mode === 'AREA'">Không hiển thị pin và không phải địa chỉ để đến mua trực tiếp.</p></div>
          <label class="location-check"><input v-model="draft.published" type="checkbox" :disabled="!eligible && !draft.published" @change="consent = false"/>Hiển thị vị trí trên gian hàng và bản đồ khám phá</label>
          <label v-if="draft.published" class="location-check"><input v-model="consent" type="checkbox" required/>Tôi đồng ý công khai vùng/địa chỉ và tọa độ đã chọn. Đây là thông tin tôi có quyền công bố.</label>
          <button class="button button-primary" type="submit" :disabled="busy || (draft.published && (!eligible || !consent))">{{ busy ? 'Đang lưu vị trí…' : 'Lưu vị trí' }}</button>
        </fieldset>
      </form>
    </template>
  </section>
</template>
<script setup>
import {defineAsyncComponent, onMounted, ref, reactive} from 'vue';
import {marketplaceRequest as request} from '../../services/marketplace.js';
const StoreLocationMap = defineAsyncComponent(() => import('./StoreLocationMap.vue'));
const draft = reactive({mode: 'AREA', published: false, region: '', ward: '', publicAddress: '', latitude: '', longitude: '', revision: 0});
const loading = ref(true), busy = ref(false), loaded = ref(false), eligible = ref(false), saved = ref(false), consent = ref(false), error = ref(''), success = ref(''), showMap = ref(false);
function apply(data) {eligible.value = data.eligible; saved.value = !!data.location; if (data.location) Object.assign(draft, data.location); consent.value = false; loaded.value = true;}
async function load() {loading.value = true; error.value = ''; try {apply(await request('/seller/store/location'));} catch (e) {error.value = e.message;} finally {loading.value = false;}}
function pick(p) {Object.assign(draft, p); consent.value = false;}
async function save() {
  if (busy.value) return; busy.value = true; error.value = ''; success.value = '';
  const body = {...draft, consent: consent.value, latitude: draft.mode === 'EXACT' ? Number(draft.latitude) : null, longitude: draft.mode === 'EXACT' ? Number(draft.longitude) : null, publicAddress: draft.mode === 'EXACT' ? draft.publicAddress : ''};
  try {const result = await request('/seller/store/location', 'PUT', body, false); const readback = await request('/seller/store/location'); if (!readback.location || result.revision !== readback.location.revision) throw new Error('Dữ liệu đã thay đổi khi đối chiếu. Tải lại để kiểm tra vị trí đã lưu.'); apply(readback); success.value = draft.published ? 'Đã lưu và công khai vị trí bạn cho phép.' : 'Đã lưu vị trí ở chế độ ẩn.';} catch (e) {error.value = e.message;} finally {busy.value = false;}
}
onMounted(load);
</script>
<style scoped>
.location-editor {border-top:1px solid var(--bm-border);margin-top:36px;padding-top:28px;max-width:850px;padding-bottom:48px;}
.location-editor h2 {font-size:var(--bm-type-section);margin:0 0 12px;}
.location-explanation {max-width:70ch;line-height:1.7;color:var(--bm-muted);}
.location-form fieldset {display:grid;gap:12px;border:0;margin:24px 0 0;padding:0;min-width:0;}
.location-form label {font-size:var(--bm-type-label);font-weight:600;}
.location-form input:not([type=checkbox]),.location-form select {width:100%;min-height:44px;border:1px solid var(--bm-control-border);border-radius:var(--bm-radius-control);background:var(--bm-surface);color:var(--bm-ink);padding:10px 12px;font:inherit;}
.location-fields {display:grid;grid-template-columns:1fr 1fr;gap:16px;}.location-fields>div {min-width:0;display:grid;gap:8px;}
.location-check {display:flex;align-items:flex-start;gap:12px;line-height:1.7;padding-block:8px;}.location-check input {flex-shrink:0;width:20px;height:20px;margin-top:3px;accent-color:var(--bm-forest);}
.location-preview {margin-block:12px;line-height:1.65;overflow-wrap:anywhere;}.location-preview h3 {font-size:var(--bm-type-body);margin:0 0 8px;}.location-preview p {margin:4px 0;}
.location-form button {justify-self:start;}.location-form :focus-visible {outline:3px solid var(--bm-forest);outline-offset:3px;}
@media(max-width:600px) {.location-fields {grid-template-columns:1fr;}.location-form button {width:100%;}}
</style>

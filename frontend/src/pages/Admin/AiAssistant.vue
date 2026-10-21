<script setup>
import {ref, computed, onMounted, onBeforeUnmount, nextTick} from 'vue';
import AdminLayout from '../../layouts/AdminLayout.vue';
import MarketIcon from '../../components/marketplace/MarketIcon.vue';
import {adminAiRequest, safeAdminAiPath} from '../../services/adminAi.js';
import '../../css/admin-ai.css';

const capabilities = ref(null), loading = ref(true), loadError = ref('');
const draft = ref(''), from = ref(''), to = ref(''), pending = ref(false), error = ref(''), failed = ref(null);
const entries = ref([]), composer = ref(null), loadingController = new AbortController();
const announcement = ref('');
let requestController, generation = 0, alive = true;
const configured = computed(() => capabilities.value?.providerConfigured === true);
const valid = computed(() => draft.value.trim().length > 0 && draft.value.length <= 2000 && (!from.value && !to.value || from.value && to.value && from.value <= to.value));
const number = value => new Intl.NumberFormat('vi-VN').format(value);
const measured = value => new Intl.DateTimeFormat('vi-VN', {dateStyle: 'medium', timeStyle: 'medium', timeZone: 'Asia/Ho_Chi_Minh'}).format(new Date(value));
async function load() {
    if (!alive) return;
    loading.value = true; loadError.value = '';
    try {const result = await adminAiRequest('/capabilities', undefined, loadingController.signal); if (alive) capabilities.value = result;}
    catch (cause) {if (alive) loadError.value = cause.message;}
    finally {if (alive) loading.value = false;}
}
function choose(prompt) {draft.value = prompt; error.value = ''; composer.value?.focus();}
async function submit(retryBody) {
    if (pending.value || !configured.value || loading.value || loadError.value) return;
    if (!retryBody && !valid.value) {error.value = 'Nhập câu hỏi tối đa 2.000 ký tự và chọn đủ hai ngày hợp lệ nếu cần.'; composer.value?.focus(); return;}
    const body = retryBody || {message: draft.value.trim(), ...(from.value || to.value ? {from: from.value, to: to.value} : {})};
    const current = ++generation;
    requestController = new AbortController(); pending.value = true; error.value = ''; failed.value = null;
    if (!retryBody) entries.value.push({message: body.message});
    if (entries.value.length > 20) entries.value.splice(0, entries.value.length - 20);
    await nextTick(); composer.value?.focus();
    try {
        const result = await adminAiRequest('/chat', body, requestController.signal);
        if (alive && current === generation) {
            entries.value.push({result});
            announcement.value = `Đã phân tích xong. Có ${result.metrics.length} chỉ số để đối chiếu.`;
            if (draft.value.trim() === body.message) draft.value = '';
        }
    } catch (cause) {
        if (alive && current === generation) {error.value = cause.message; failed.value = body;}
    } finally {if (alive && current === generation) pending.value = false;}
}
function onKey(event) {
    if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {event.preventDefault(); submit();}
}
function clear() {if (pending.value) return; entries.value = []; error.value = ''; failed.value = null; composer.value?.focus();}
function forgetSession() {entries.value = []; failed.value = null; error.value = ''; generation++; requestController?.abort(); pending.value = false;}
onMounted(() => {load(); window.addEventListener('bonsai:session-expired', forgetSession); window.addEventListener('bonsai:logged-out', forgetSession);});
onBeforeUnmount(() => {alive = false; generation++; loadingController.abort(); requestController?.abort(); window.removeEventListener('bonsai:session-expired', forgetSession); window.removeEventListener('bonsai:logged-out', forgetSession);});
</script>

<template>
  <AdminLayout>
    <div class="admin-ai-page">
      <header class="workspace-page-heading admin-ai-heading">
        <div><h1>Trợ lý AI quản trị</h1><p>Phân tích dữ liệu BonsaiMarket và xác định những việc cần kiểm tra tiếp theo.</p></div>
        <button v-if="entries.length" class="button button-outline" :disabled="pending" @click="clear">Xóa hội thoại</button>
      </header>
      <p class="admin-ai-readonly"><MarketIcon name="info"/><span><strong>Chỉ đọc dữ liệu.</strong> Trợ lý không thực hiện quyết định quản trị. Hội thoại chỉ giữ trong trang đang mở; mỗi câu hỏi được xử lý độc lập.</span></p>

      <div v-if="loading" class="admin-ai-state" role="status" aria-busy="true">Đang kiểm tra khả năng hỗ trợ…</div>
      <div v-else-if="loadError" class="admin-ai-error" role="alert"><p>{{ loadError }}</p><button class="button button-outline" @click="load">Tải lại khả năng hỗ trợ</button></div>
      <template v-else-if="capabilities">
        <p class="admin-ai-provider"><span :class="{'admin-ai-provider-ready': configured}">Gemini · {{ configured ? 'Đã cấu hình' : 'Chưa khả dụng' }}</span> {{ capabilities.message }}</p>
        <section v-if="!entries.length" class="admin-ai-welcome" aria-labelledby="admin-ai-welcome-title">
          <h2 id="admin-ai-welcome-title">Bạn muốn kiểm tra điều gì?</h2>
          <p>Chọn một câu hỏi hoặc nêu rõ vấn đề cần phân tích. Số liệu, định nghĩa và nguồn được trả cùng kết quả.</p>
          <ul class="admin-ai-prompts">
            <li v-for="item in capabilities.capabilities" :key="item.id"><button type="button" :disabled="!configured" @click="choose(item.suggestedPrompt)"><span>{{ item.label }}</span><MarketIcon name="arrow"/></button></li>
          </ul>
          <details class="admin-ai-limits"><summary>Phạm vi chưa hỗ trợ</summary><ul><li v-for="item in capabilities.unsupported" :key="item">{{ item }}</li></ul></details>
        </section>
        <section v-else class="admin-ai-conversation" aria-label="Nội dung hỏi và phân tích">
          <template v-for="(entry, index) in entries" :key="index">
            <article v-if="entry.message" class="admin-ai-question"><h2>Câu hỏi của bạn</h2><p>{{ entry.message }}</p></article>
            <article v-else class="admin-ai-result" :aria-labelledby="'result-'+index">
              <h2 :id="'result-'+index">Kết quả phân tích</h2>
              <p>{{ entry.result.answer }}</p>
              <p class="admin-ai-freshness">Đo lúc {{ measured(entry.result.measuredAt) }} · {{ entry.result.timezone }}</p>
              <p v-if="entry.result.dateRange" class="admin-ai-date-range">{{ entry.result.dateRange }}</p>
              <div class="admin-ai-table-scroll" role="region" :aria-label="'Số liệu kết quả '+(index+1)" tabindex="0">
                <table class="admin-ai-metrics"><caption>Số liệu tại thời điểm đo</caption><thead><tr><th scope="col">Chỉ số</th><th scope="col">Giá trị</th><th scope="col">Định nghĩa và phạm vi</th></tr></thead><tbody><tr v-for="metric in entry.result.metrics" :key="metric.id"><th scope="row">{{ metric.name }}</th><td>{{ number(metric.value) }}</td><td>{{ metric.definition }}<small>Nguồn: {{ entry.result.sources.find(source => source.id === metric.sourceId)?.label || 'Xem định nghĩa chỉ số' }}</small></td></tr></tbody></table>
              </div>
              <section v-if="entry.result.findings.length" class="admin-ai-findings" aria-label="Nhận xét và kiểm tra tiếp theo"><h3>Những điểm cần kiểm tra</h3><ul><li v-for="finding in entry.result.findings" :key="finding.id"><p>{{ finding.text }}</p><p><strong>Kiểm tra tiếp:</strong> {{ finding.nextCheck }}</p></li></ul></section>
              <div v-if="entry.result.rows.length" class="admin-ai-table-scroll" role="region" aria-label="Bản ghi tham chiếu" tabindex="0">
                <table class="admin-ai-records"><caption>Một số bản ghi để đối chiếu · tối đa 10</caption><thead><tr><th scope="col">Bản ghi</th><th scope="col">Sản phẩm công khai</th><th scope="col">Thông tin cần lưu ý</th></tr></thead><tbody><tr v-for="row in entry.result.rows" :key="row.id"><th scope="row"><RouterLink v-if="safeAdminAiPath(row.path)" :to="safeAdminAiPath(row.path)">{{ row.name }}</RouterLink><span v-else>{{ row.name }}</span><small>#{{ row.id }}</small></th><td>{{ row.count === null ? 'Không áp dụng' : number(row.count) }}</td><td>{{ row.warnings.join(' · ') || 'Không có cảnh báo trong phạm vi đo' }}</td></tr></tbody></table>
              </div>
              <ul class="admin-ai-warnings"><li v-for="warning in entry.result.warnings" :key="warning">{{ warning }}</li></ul>
              <nav class="admin-ai-sources" aria-label="Mở dữ liệu nguồn"><strong>Đối chiếu nguồn</strong><template v-for="source in entry.result.sources" :key="source.id"><RouterLink v-if="safeAdminAiPath(source.path)" :to="safeAdminAiPath(source.path)">{{ source.label }}</RouterLink><span v-else>{{ source.label }}</span></template></nav>
              <details class="admin-ai-trace"><summary>Thông tin đối chiếu yêu cầu</summary><p>Mã yêu cầu: {{ entry.result.requestId }}<br>Phân tích: {{ entry.result.capability }}</p></details>
            </article>
          </template>
        </section>
        <p class="sr-only" role="status" aria-atomic="true">{{ announcement }}</p>
        <p v-if="pending" class="admin-ai-pending" role="status"><MarketIcon name="ai"/>Đang đọc dữ liệu và phân tích. Vui lòng chờ…</p>
        <div v-if="error" class="admin-ai-error" role="alert"><p>{{ error }}</p><button v-if="failed" type="button" class="button button-outline" :disabled="pending" @click="submit(failed)">Gửi lại câu hỏi</button></div>
        <form class="admin-ai-composer" :aria-busy="pending" @submit.prevent="submit()">
          <label for="admin-ai-message">Câu hỏi quản trị</label>
          <textarea id="admin-ai-message" ref="composer" v-model="draft" rows="3" maxlength="2000" :disabled="!configured" :aria-invalid="!!error" aria-describedby="admin-ai-input-help" placeholder="Ví dụ: Hiện có bao nhiêu sản phẩm công khai đang hết hàng?" @keydown="onKey"/>
          <details class="admin-ai-date-filter"><summary>Khoảng ngày tạo sản phẩm / gian hàng (tùy chọn)</summary><div><label for="admin-ai-from">Từ ngày<input id="admin-ai-from" v-model="from" type="date" :disabled="pending || !configured"></label><label for="admin-ai-to">Đến ngày<input id="admin-ai-to" v-model="to" type="date" :disabled="pending || !configured"></label><button type="button" class="admin-ai-clear-dates" :disabled="pending || !configured" @click="from = ''; to = ''">Bỏ lọc ngày</button></div></details>
          <div class="admin-ai-composer-bottom"><p id="admin-ai-input-help">{{ draft.length }}/2.000 · Enter để gửi, Shift+Enter để xuống dòng.<br>Không nhập thông tin cá nhân hoặc bí mật vào câu hỏi.</p><button type="submit" class="button button-primary" :disabled="!configured || pending || !valid"><MarketIcon name="send"/>{{ pending ? 'Đang phân tích…' : 'Phân tích' }}</button></div>
        </form>
      </template>
    </div>
  </AdminLayout>
</template>

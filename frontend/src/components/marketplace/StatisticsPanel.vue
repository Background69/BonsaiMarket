<template>
  <section aria-label="Thống kê doanh thu" :aria-busy="loading" class="statistics-panel">
    <form class="chart-date-range" @submit.prevent="apply">
      <label>Từ ngày<input v-model="from" :disabled="loading" :max="to || undefined" type="date"/></label>
      <label>Đến ngày<input v-model="to" :disabled="loading" :min="from || undefined" type="date"/></label>
      <button :disabled="loading || invalid" class="button button-primary">Xem thống kê</button>
      <button :disabled="loading" class="button button-outline" type="button" @click="reset">30 ngày gần nhất</button>
    </form>
    <p v-if="invalid" class="management-error" role="alert">Ngày kết thúc phải bằng hoặc sau ngày bắt đầu.</p>
    <div v-if="error" class="management-error" role="alert">
      <p>{{ error }}</p>
      <button class="button button-outline" type="button" @click="load()">Thử lại</button>
    </div>
    <p v-if="loading" role="status">Đang tải thống kê…</p>
    <template v-if="data">
      <dl class="statistics-summary">
        <div><dt>Doanh thu</dt><dd>{{ money(data.totalRevenue) }}</dd></div>
        <div><dt>Đơn tính doanh thu</dt><dd>{{ data.revenueOrders }}</dd></div>
        <div><dt>Đơn chờ xác nhận</dt><dd>{{ data.pendingOrders }}</dd></div>
      </dl>
      <p class="muted statistics-note">Doanh thu là giá trị hàng hóa (chưa gồm phí vận chuyển) của đơn đã giao hoặc hoàn thành, tính theo ngày đặt hàng (giờ Việt Nam).</p>
      <div class="dashboard-chart-grid">
        <article class="market-panel">
          <div class="section-heading"><h2>Doanh thu theo ngày</h2><span class="muted">VNĐ</span></div>
          <RevenueBarChart :days="data.days"/>
        </article>
        <article class="market-panel">
          <div class="section-heading"><h2>Doanh thu theo danh mục</h2></div>
          <CategoryPieChart :categories="data.categories"/>
        </article>
      </div>
      <article class="market-panel statistics-status">
        <div class="section-heading"><h2>Đơn hàng theo trạng thái</h2></div>
        <table class="pie-legend">
          <thead><tr><th>Trạng thái</th><th>Số đơn đặt trong khoảng ngày</th></tr></thead>
          <tbody>
          <tr v-for="(count, key) in data.statusCounts" :key="key"><td>{{ statusLabel(String(key).toLowerCase()) }}</td><td>{{ count }}</td></tr>
          </tbody>
        </table>
      </article>
    </template>
  </section>
</template>
<script setup>
import {computed, onBeforeUnmount, onMounted, ref} from 'vue';
import RevenueBarChart from './RevenueBarChart.vue';
import CategoryPieChart from './CategoryPieChart.vue';
import {marketplaceRequest} from '../../services/marketplace.js';
import {formatPrice, statusLabel} from '../../utils.js';

const props = defineProps({endpoint: {type: String, required: true}});
const from = ref(''), to = ref(''), data = ref(null), loading = ref(false), error = ref('');
const invalid = computed(() => !!from.value && !!to.value && from.value > to.value);
const money = v => formatPrice(v) + ' ₫';
let sequence = 0;

async function load(params = {}) {
  const id = ++sequence;
  loading.value = true;
  error.value = '';
  try {
    const query = new URLSearchParams(params).toString();
    const result = await marketplaceRequest(props.endpoint + (query ? '?' + query : ''));
    if (id !== sequence) return;
    data.value = result;
    from.value = result.from;
    to.value = result.to;
  } catch (e) {
    if (id === sequence) error.value = e.message || 'Không thể tải thống kê. Vui lòng thử lại.';
  } finally {
    if (id === sequence) loading.value = false;
  }
}

function apply() {
  if (invalid.value) return;
  const params = {};
  if (from.value) params.from = from.value;
  if (to.value) params.to = to.value;
  load(params);
}

function reset() {
  from.value = '';
  to.value = '';
  load();
}

onMounted(() => load());
onBeforeUnmount(() => sequence++);
</script>
<style scoped>
.statistics-panel {display: grid; gap: 20px;}
.statistics-summary {display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; margin: 0;}
.statistics-summary > div {padding: 16px; border: 1px solid var(--bm-border); border-radius: var(--bm-radius); background: var(--bm-surface);}
.statistics-summary dt {font-size: 13px; color: var(--bm-muted); margin: 0 0 6px;}
.statistics-summary dd {margin: 0; font-size: 22px; font-weight: 600; color: var(--bm-forest); overflow-wrap: anywhere;}
.statistics-note {margin: 0; font-size: 13px;}
</style>

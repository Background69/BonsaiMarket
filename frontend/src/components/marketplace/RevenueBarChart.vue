<template>
  <div>
    <div v-if="!hasRevenue" class="chart-empty" role="status">Không có doanh thu trong khoảng ngày đã chọn.</div>
    <template v-else>
      <div class="chart-scroll">
        <svg :viewBox="'0 0 '+width+' 240'" :style="{minWidth: width+'px'}" :aria-label="'Biểu đồ cột doanh thu theo ngày. Tổng '+format(total)+' đồng.'"
             class="revenue-chart" role="img">
          <line stroke="#a1b0a2" x1="64" x2="64" y1="18" y2="198"/>
          <line :x2="width-16" stroke="#a1b0a2" x1="64" y1="198" y2="198"/>
          <template v-for="tick in [0,.5,1]" :key="tick">
            <text :y="198-tick*170+4" text-anchor="end" x="58">{{ compact(max * tick) }}</text>
            <line :x2="width-16" :y1="198-tick*170" :y2="198-tick*170" stroke="#e8ede4" x1="65"/>
          </template>
          <g v-for="(point,i) in points" :key="point.date" :aria-label="point.label+': '+format(point.revenue)+' đồng'" tabindex="0">
            <title>{{ point.label }}: {{ format(point.revenue) }} ₫ · {{ point.orders }} đơn</title>
            <rect :height="point.height" :width="barWidth" :x="left(i)" :y="198-point.height" fill="#347354" rx="2"/>
            <text v-if="i % every === 0" :x="left(i)+barWidth/2" text-anchor="middle" y="217">{{ point.label }}</text>
          </g>
          <text x="8" y="12">VNĐ</text>
        </svg>
      </div>
      <details class="chart-data-table">
        <summary>Xem dữ liệu doanh thu theo ngày</summary>
        <table class="pie-legend">
          <thead><tr><th>Ngày</th><th>Số đơn</th><th>Doanh thu (VNĐ)</th></tr></thead>
          <tbody>
          <tr v-for="point in points" :key="point.date"><td>{{ point.label }}</td><td>{{ point.orders }}</td><td>{{ format(point.revenue) }}</td></tr>
          </tbody>
        </table>
      </details>
    </template>
  </div>
</template>
<script setup>
import {computed} from 'vue';

const props = defineProps({days: {type: Array, default: () => []}});
const format = v => new Intl.NumberFormat('vi-VN').format(v);
const compact = v => new Intl.NumberFormat('vi-VN', {notation: 'compact', maximumFractionDigits: 1}).format(v);
const values = computed(() => props.days.map(d => Math.max(0, Number(d.revenue) || 0)));
const max = computed(() => Math.max(1, ...values.value));
const total = computed(() => values.value.reduce((sum, v) => sum + v, 0));
const hasRevenue = computed(() => total.value > 0);
// Wider spacing for short ranges; compact bars for long ranges so the chart stays readable.
const step = computed(() => props.days.length <= 14 ? 54 : props.days.length <= 45 ? 30 : props.days.length <= 120 ? 16 : 9);
const barWidth = computed(() => Math.max(4, Math.round(step.value * 0.6)));
const every = computed(() => Math.max(1, Math.ceil(48 / step.value)));
const width = computed(() => Math.max(360, props.days.length * step.value + 90));
const left = i => 76 + i * step.value;
const points = computed(() => props.days.map((d, i) => {
  const [year, month, day] = String(d.date).split('-');
  return {
    date: d.date, label: day + '/' + month + (props.days.length > 120 ? '/' + String(year).slice(2) : ''),
    revenue: values.value[i], orders: Number(d.orders) || 0, height: values.value[i] / max.value * 170,
  };
}));
</script>

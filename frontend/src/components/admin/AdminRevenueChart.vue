<template>
  <div ref="host" class="admin-revenue-chart">
    <AdminEmptyState v-if="!total" title="Chưa có giá trị đơn đủ điều kiện"
                     description="Biểu đồ xuất hiện khi có đơn đã giao hoặc hoàn tất trong kỳ đã chọn." icon="chart"/>
    <template v-else>
      <svg :viewBox="'0 0 '+width+' 260'" role="img" :aria-label="'Giá trị hàng hóa theo ngày: '+money(total)">
        <g v-for="tick in [0,.5,1]" :key="tick">
          <line x1="54" :x2="width-12" :y1="214-tick*180" :y2="214-tick*180" class="admin-chart-grid"/>
          <text x="48" :y="218-tick*180" text-anchor="end">{{ compact(max * tick) }}</text>
        </g>
        <g v-for="(day,i) in days" :key="day.date" tabindex="0" :aria-label="day.date+': '+money(day.revenue)"><title>{{
          day.date }} · {{ money(day.revenue) }} · {{ day.orders }} đơn</title>
          <rect :x="left(i)" :y="214-height(day.revenue)" :width="bar" :height="height(day.revenue)" rx="2"/>
          <text v-if="i%every===0||i===days.length-1&&i%every>every/2" :x="left(i)+bar/2" y="239" text-anchor="middle">
            {{ day.date.slice(8) + '/' + day.date.slice(5, 7) }}
          </text>
        </g>
      </svg>
      <details class="admin-chart-table">
        <summary>Xem dữ liệu theo ngày</summary>
        <table>
          <caption class="sr-only">Giá trị đơn hàng theo ngày</caption>
          <thead>
          <tr>
            <th scope="col">Ngày</th>
            <th scope="col">Số đơn</th>
            <th scope="col">Giá trị hàng hóa</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="day in days" :key="day.date">
            <th scope="row">{{ day.date }}</th>
            <td>{{ day.orders }}</td>
            <td>{{ money(day.revenue) }}</td>
          </tr>
          </tbody>
        </table>
      </details>
    </template>
  </div>
</template>
<script setup>
import {computed, onBeforeUnmount, onMounted, ref} from 'vue';
import AdminEmptyState from './AdminEmptyState.vue';
import {money} from '../../services/adminPresentation.js';

const props = defineProps({days: {type: Array, default: () => []}}), host = ref(null), width = ref(600);
let observer;
onMounted(() => {
  observer = new ResizeObserver(entries => {
    width.value = Math.max(250, Math.floor(entries[0].contentRect.width));
  });
  observer.observe(host.value);
});
onBeforeUnmount(() => observer?.disconnect());
const total = computed(() => props.days.reduce((s, d) => s + Number(d.revenue), 0)),
    max = computed(() => Math.max(1, ...props.days.map(d => Number(d.revenue)))),
    step = computed(() => (width.value - 72) / Math.max(1, props.days.length)),
    bar = computed(() => Math.max(.5, step.value * .65)),
    every = computed(() => Math.max(1, Math.ceil(props.days.length / Math.max(2, Math.floor(width.value / 90)))));
const left = i => 58 + i * step.value, height = v => Math.max(0, Number(v)) / max.value * 180,
    compact = v => new Intl.NumberFormat('vi-VN', {notation: 'compact', maximumFractionDigits: 1}).format(v);
</script>

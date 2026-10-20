<template>
  <div>
    <div v-if="!total" class="chart-empty" role="status">Chưa có doanh thu theo danh mục trong khoảng ngày đã chọn.</div>
    <template v-else>
      <div :aria-label="'Doanh thu theo danh mục. Tổng '+format(total)+' đồng. Chi tiết trong bảng bên dưới.'" :style="{background: gradient}"
           class="pie-chart" role="img">
        <div><strong>{{ quantity }}</strong><span>Sản phẩm bán ra</span></div>
      </div>
      <table class="pie-legend">
        <thead><tr><th>Danh mục</th><th>Doanh thu (VNĐ)</th><th>Tỷ lệ</th></tr></thead>
        <tbody>
        <tr v-for="(item,i) in slices" :key="item.name+i">
          <td><i :style="{background: colors[i % colors.length]}" aria-hidden="true"/>{{ item.name }} <small class="muted">({{ item.quantity }} sp)</small></td>
          <td>{{ format(item.revenue) }}</td>
          <td>{{ (item.revenue / total * 100).toFixed(1) }}%</td>
        </tr>
        </tbody>
      </table>
    </template>
  </div>
</template>
<script setup>
import {computed} from 'vue';

const props = defineProps({categories: {type: Array, default: () => []}});
const colors = ['#104e35', '#347354', '#8a9e5b', '#c89c35', '#678c99', '#a76554', '#6b5b95', '#3f7f93', '#8c8c8c'];
const format = v => new Intl.NumberFormat('vi-VN').format(v);
const slices = computed(() => props.categories
  .map(c => ({name: c.name, revenue: Math.max(0, Number(c.revenue) || 0), quantity: Number(c.quantity) || 0}))
  .filter(c => c.revenue > 0));
const total = computed(() => slices.value.reduce((sum, c) => sum + c.revenue, 0));
const quantity = computed(() => slices.value.reduce((sum, c) => sum + c.quantity, 0));
const gradient = computed(() => {
  let start = 0;
  return 'conic-gradient(' + slices.value.map((c, i) => {
    const end = start + c.revenue / total.value * 100;
    const stop = colors[i % colors.length] + ' ' + start + '% ' + end + '%';
    start = end;
    return stop;
  }).join(',') + ')';
});
</script>

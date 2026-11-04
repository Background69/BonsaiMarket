<template>
  <section class="order-history" aria-labelledby="order-history-title">
    <h2 id="order-history-title">Lịch sử trạng thái đơn</h2>
    <p v-if="!entries.length" class="muted">Chưa có lịch sử thay đổi được ghi nhận cho đơn này.</p>
    <ol v-else>
      <li v-for="entry in entries" :key="entry.id">
        <p><span v-if="entry.fromStatus">{{ label(entry.fromStatus) }} <span aria-hidden="true">→</span><span class="sr-only"> chuyển sang </span> </span><strong>{{ label(entry.toStatus) }}</strong></p>
        <p class="muted">{{ entry.actorLabel }}<span v-if="entry.actorId"> · Tài khoản #{{ entry.actorId }}</span> · <time :datetime="entry.createdAt">{{ date(entry.createdAt) }}</time></p>
        <p v-if="!entry.fromStatus" class="muted">Sự kiện cũ chưa ghi nhận trạng thái trước đó.</p>
        <p v-if="entry.reason">Lý do hủy: {{ entry.reason }}</p>
      </li>
    </ol>
  </section>
</template>
<script setup>
import {statusLabel} from '../../utils.js';
defineProps({entries:{type:Array,default:()=>[]}});
const label=value=>statusLabel(value.toLowerCase());
const date=value=>new Intl.DateTimeFormat('vi-VN',{dateStyle:'medium',timeStyle:'short',timeZone:'Asia/Ho_Chi_Minh'}).format(new Date(value));
</script>
<style scoped>
.order-history{padding-block:24px;border-top:1px solid var(--bm-border);overflow-wrap:anywhere}.order-history h2{font-size:var(--bm-type-section);margin:0 0 16px}.order-history ol{list-style:none;margin:0;padding:0 0 0 20px;border-left:2px solid var(--bm-border)}.order-history li+li{margin-top:24px}.order-history p{margin:4px 0;line-height:1.6}.order-history .muted{font-size:var(--bm-type-label)}
.sr-only{position:absolute;width:1px;height:1px;padding:0;overflow:hidden;clip-path:inset(50%);white-space:nowrap;border:0}
</style>

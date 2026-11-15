<script setup>
import MarketIcon from './MarketIcon.vue';
defineProps({subtotal:Number,count:Number});
const money=value=>typeof value==='number'&&Number.isFinite(value)?new Intl.NumberFormat('vi-VN',{style:'currency',currency:'VND'}).format(value):'Chưa xác định';
</script>
<template>
  <section class="checkout-estimate" aria-labelledby="checkout-summary-title">
    <h2 id="checkout-summary-title"><MarketIcon name="list"/>Tóm tắt đơn hàng</h2>
    <dl><div><dt>Tạm tính ({{ count }} sản phẩm)</dt><dd>{{ money(subtotal) }}</dd></div><div><dt>Phí vận chuyển</dt><dd class="muted">Chưa tính</dd></div></dl>
    <div class="estimate-total"><span>Giá trị sản phẩm tạm tính</span><strong>{{ money(subtotal) }}</strong></div>
    <p class="estimate-info"><MarketIcon name="info"/><span>Tổng thanh toán sẽ được xác định sau khi nhận báo giá vận chuyển. Phí và điều kiện COD được kiểm tra ở bước tiếp theo.</span></p>
  </section>
</template>
<style scoped>
.checkout-estimate{padding:16px 18px;border:1px solid var(--bm-border);border-radius:16px;background:var(--bm-surface);box-shadow:0 4px 20px rgb(11 61 43 / 4%);min-width:0}.checkout-estimate h2{display:flex;align-items:center;gap:12px;margin:0 0 8px;padding-bottom:10px;border-bottom:1px solid var(--bm-border);font-size:18px;font-weight:700}.checkout-estimate h2 svg{color:var(--bm-forest)}dl{margin:0}dl>div{display:flex;justify-content:space-between;gap:16px;margin:8px 4px;font-size:14px}dd{margin:0;text-align:right;font-variant-numeric:tabular-nums;white-space:nowrap}.muted{color:var(--bm-muted)}.estimate-total{display:flex;align-items:center;justify-content:space-between;gap:16px;background:var(--bm-soft);border-radius:8px;padding:12px;color:var(--bm-forest);font-weight:700;font-size:16px}.estimate-total strong{white-space:nowrap;font-variant-numeric:tabular-nums;font-size:20px}.estimate-info{display:flex;gap:10px;margin:10px 0 0;padding:10px 12px;background:var(--bm-soft);border-radius:8px;color:var(--bm-dark);font-size:13px;line-height:1.5}.estimate-info svg{width:18px;height:18px;margin-top:1px}@media(max-width:450px){.estimate-total{align-items:start;flex-direction:column;gap:6px}}
</style>

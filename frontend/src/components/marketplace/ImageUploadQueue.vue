<template>
  <section v-if="items.length" class="upload-queue" aria-label="Hàng đợi tải ảnh">
    <h3>Hàng đợi tải lên · {{ items.length }} tệp</h3><p class="muted">Tệp đang chọn tách biệt với ảnh đã lưu ở bên dưới.</p>
    <ul><li v-for="item in items" :key="item.key">
      <div class="upload-queue-preview"><img v-if="(item.preview||item.uploadedUrl)&&!failed.has(item.key)" :src="item.preview||item.uploadedUrl" :alt="'Xem trước '+item.name" width="160" height="120" @error="failed=new Set([...failed,item.key])"/><span v-else role="img" :aria-label="'Không có bản xem trước cho '+item.name"><svg aria-hidden="true" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M3 3h18v18H3ZM3 17l5-5 4 4 3-3 6 6M8 7h.01"/></svg>Không có bản xem trước</span></div>
      <div class="upload-queue-copy"><strong>{{ item.name }}</strong><p>{{ size(item.size) }} · {{ labels[item.status] }}</p><p v-if="item.error" :class="{'upload-queue-error':item.status==='FAILED'}">{{ item.error }}</p>
        <template v-if="item.status==='UPLOADING'"><progress :value="item.progress==null?undefined:Math.floor(item.progress*100)" max="100" :aria-label="'Tiến trình tải '+item.name"/><span>{{ item.progress===null?'Đang truyền tệp…':item.progress>=1?'Đã truyền tệp, đang chờ máy chủ lưu…':Math.floor(item.progress*100)+'%' }}</span></template>
      </div>
      <div class="upload-queue-actions"><button v-if="item.status==='FAILED'&&!item.invalid" type="button" class="button button-outline" :disabled="busy" :aria-label="'Thử lại '+item.name" @click="$emit('retry',item.key)">Thử lại</button><button type="button" class="button button-outline" :disabled="busy" :aria-label="(item.status==='UPLOADED'?'Dọn thông báo ':'Bỏ tệp ')+item.name" @click="$emit('remove',item.key)">{{ item.status==='UPLOADED'?'Dọn thông báo':'Bỏ tệp đã chọn' }}</button></div><small v-if="item.status==='UPLOADED'">Ảnh vẫn được giữ trên máy chủ khi dọn thông báo.</small>
    </li></ul>
  </section>
</template>
<script setup>
import {ref} from 'vue';
const failed=ref(new Set());
defineProps({items:{type:Array,default:()=>[]},busy:Boolean});defineEmits(['retry','remove']);
const labels={READY:'Sẵn sàng',UPLOADING:'Đang tải lên',UPLOADED:'Đã tải lên',FAILED:'Chưa tải được',CANCELLED:'Đã hủy yêu cầu cũ'};
const size=value=>value>=1048576?(value/1048576).toLocaleString('vi-VN',{maximumFractionDigits:2})+' MB':Math.ceil(value/1024)+' KB';
</script>
<style scoped>
.upload-queue ul{padding:0;list-style:none;display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:var(--bm-space-3)}.upload-queue li{display:flex;flex-direction:column;gap:12px;min-width:0;padding:12px;border:1px solid var(--bm-border);border-radius:var(--bm-radius);background:var(--bm-surface)}.upload-queue-preview{width:100%;aspect-ratio:4/3;background:var(--bm-soft);border-radius:var(--bm-radius-control);overflow:hidden}.upload-queue img{width:100%;height:100%;object-fit:contain}.upload-queue-preview>span{height:100%;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:8px;color:var(--bm-muted);font-size:13px;padding:12px;text-align:center}.upload-queue-copy{min-width:0;overflow-wrap:anywhere;font-size:14px;flex:1}.upload-queue-copy strong{display:block;overflow-wrap:anywhere}.upload-queue p{margin:6px 0;font-size:13px;line-height:1.6}.upload-queue small{font-size:13px;color:var(--bm-muted)}.upload-queue progress{display:block;width:100%;height:12px;accent-color:var(--bm-forest)}.upload-queue-error{color:var(--bm-error)}.upload-queue-actions{display:flex;flex-wrap:wrap;gap:8px;width:100%}.upload-queue-actions button{min-height:44px;flex:1;padding:8px;font-size:13px}@media(max-width:800px){.upload-queue ul{grid-template-columns:repeat(3,minmax(0,1fr))}}@media(max-width:600px){.upload-queue ul{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:380px){.upload-queue ul{grid-template-columns:minmax(0,1fr)}}
</style>

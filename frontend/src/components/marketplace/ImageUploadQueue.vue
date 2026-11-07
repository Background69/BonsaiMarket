<template>
  <section v-if="items.length" class="upload-queue" aria-label="Hàng đợi tải ảnh">
    <h3>Ảnh đã chọn</h3>
    <ul><li v-for="item in items" :key="item.key">
      <img v-if="item.preview||item.uploadedUrl" :src="item.preview||item.uploadedUrl" :alt="'Xem trước '+item.name" width="80" height="80"/>
      <div class="upload-queue-copy"><strong>{{ item.name }}</strong><p>{{ size(item.size) }} · {{ labels[item.status] }}</p><p v-if="item.error" :class="{'upload-queue-error':item.status==='FAILED'}">{{ item.error }}</p>
        <template v-if="item.status==='UPLOADING'"><progress :value="item.progress==null?undefined:Math.floor(item.progress*100)" max="100" :aria-label="'Tiến trình tải '+item.name"/><span>{{ item.progress===null?'Đang truyền tệp…':item.progress>=1?'Đã truyền tệp, đang chờ máy chủ lưu…':Math.floor(item.progress*100)+'%' }}</span></template>
      </div>
      <div class="upload-queue-actions"><button v-if="item.status==='FAILED'&&!item.invalid" type="button" class="button button-outline" :disabled="busy" :aria-label="'Thử lại '+item.name" @click="$emit('retry',item.key)">Thử lại</button><button type="button" class="button button-outline" :disabled="busy" :aria-label="'Bỏ '+item.name+' khỏi hàng đợi'" @click="$emit('remove',item.key)">Bỏ khỏi hàng đợi</button></div>
    </li></ul>
  </section>
</template>
<script setup>
defineProps({items:{type:Array,default:()=>[]},busy:Boolean});defineEmits(['retry','remove']);
const labels={READY:'Sẵn sàng',UPLOADING:'Đang tải lên',UPLOADED:'Đã tải lên',FAILED:'Chưa tải được',CANCELLED:'Đã hủy yêu cầu cũ'};
const size=value=>value>=1048576?(value/1048576).toLocaleString('vi-VN',{maximumFractionDigits:2})+' MB':Math.ceil(value/1024)+' KB';
</script>
<style scoped>
.upload-queue ul{padding:0;list-style:none;display:grid;gap:var(--bm-space-3)}.upload-queue li{display:flex;align-items:start;gap:var(--bm-space-3);min-width:0;padding:var(--bm-space-4);border:1px solid var(--bm-border);border-radius:var(--bm-radius-control)}.upload-queue img{width:80px;height:80px;object-fit:contain;flex-shrink:0;background:var(--bm-soft);border-radius:var(--bm-radius-control)}.upload-queue-copy{min-width:0;flex:1;overflow-wrap:anywhere}.upload-queue p{margin:6px 0;font-size:var(--bm-type-label);line-height:1.6}.upload-queue progress{display:block;max-width:100%;height:16px}.upload-queue-error{color:var(--bm-error)}.upload-queue-actions{display:flex;flex-wrap:wrap;gap:8px}.upload-queue button{min-height:44px}@media(max-width:600px){.upload-queue li{flex-wrap:wrap}.upload-queue-actions{flex-basis:100%}.upload-queue-actions .button{flex:1}.upload-queue img{width:64px;height:64px}}
</style>

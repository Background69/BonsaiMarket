<template>
  <div class="resource-state" :role="error?'alert':'status'" :aria-busy="loading || undefined">
    <p v-if="loading">Đang tải {{ resource }}…</p>
    <template v-else-if="error"><h2>{{ notFound ? 'Không tìm thấy '+resource : 'Chưa tải được '+resource }}</h2><p>{{ errorMessage || 'Kết nối bị gián đoạn. Bạn có thể thử tải lại.' }}</p><RouterLink v-if="notFound" class="button button-outline" :to="recoveryTo">{{ recoveryLabel }}</RouterLink><button v-else class="button button-outline" @click="$emit('retry')">Thử lại</button></template>
    <template v-else><h2>{{ emptyTitle || 'Chưa có '+resource+' công khai' }}</h2><p>{{ message || 'Bạn có thể quay lại sau.' }}</p><slot/></template>
  </div>
</template>
<script setup>
defineProps({resource:{type:String,required:true},loading:Boolean,error:Boolean,emptyTitle:String,message:String,errorMessage:String,notFound:Boolean,recoveryTo:{type:String,default:'/products'},recoveryLabel:{type:String,default:'Khám phá sản phẩm'}});
defineEmits(['retry']);
</script>
<style scoped>
.resource-state {padding-block:24px;max-width:65ch;}
.resource-state h2 {margin:0 0 8px;font-size:20px;line-height:1.4;}
.resource-state p {margin:0 0 16px;color:var(--bm-muted);font-size:16px;line-height:1.6;}
</style>

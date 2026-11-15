<script setup>defineProps({resource:{type:Object,required:true},label:String});defineEmits(['retry']);</script>
<template>
  <div v-if="resource.loading" class="bm-buyer-account-resource" role="status" aria-busy="true"><span class="bm-buyer-account-sr">Đang tải {{ label }}…</span><span v-for="n in 3" :key="n" class="bm-buyer-account-skeleton" aria-hidden="true"/></div>
  <div v-else-if="resource.error" class="bm-buyer-account-error" role="alert"><p>{{ resource.error }}</p><button class="button button-outline" type="button" @click="$emit('retry')">Thử tải lại {{ label }}</button></div>
  <slot v-else-if="resource.data!==null"/>
  <p v-else role="status">Chưa tải được {{ label }}.</p>
</template>

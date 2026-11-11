<script setup>
import MarketIcon from '../marketplace/MarketIcon.vue';
defineProps({name:String});
const paths={home:'m3 10 9-8 9 8v11h-6v-7H9v7H3Z',mail:'M3 4h18v16H3ZM3 5l9 8 9-8',shield:'m12 2 9 4v6c0 6-9 10-9 10S3 18 3 12V6Zm-5 9 3 3 6-6',lock:'M5 10h14v12H5ZM8 10V6a4 4 0 0 1 8 0v4M12 15v3',edit:'m16 3 5 5L8 21H3v-5ZM14 5l5 5',support:'M3 14v-3a9 9 0 0 1 18 0v3M3 12h4v8H3ZM17 12h4v8h-4ZM17 20c0 2-3 2-5 2'};
</script>
<template><svg v-if="paths[name]" aria-hidden="true" focusable="false" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path :d="paths[name]"/></svg><MarketIcon v-else :name="name"/></template>

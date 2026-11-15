<script setup>
import MarketIcon from './MarketIcon.vue';
defineProps({name:String});
const paths={bank:'m2 8 10-6 10 6ZM4 10v9m5-9v9m6-9v9m5-9v9M2 22h20M2 19h20',eye:'M2 12s4-7 10-7 10 7 10 7-4 7-10 7-10-7-10-7ZM15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0',withdraw:'M6 18 18 6M6 6h12v12',transfer:'M3 7h17m-5-5 5 5-5 5M21 17H4m5-5-5 5 5 5',refresh:'M20 4v6h-6M4 20v-6h6M4 10a8 8 0 0 1 13-6l3 6M4 14l3 6a8 8 0 0 0 13-6',shield:'m12 2 9 4v6c0 6-9 10-9 10S3 18 3 12V6Zm-5 9 3 3 6-6',lock:'M5 10h14v12H5ZM8 10V6a4 4 0 0 1 8 0v4M12 15v3'};
</script>
<template><svg v-if="paths[name]" aria-hidden="true" focusable="false" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path :d="paths[name]"/></svg><MarketIcon v-else :name="name"/></template>

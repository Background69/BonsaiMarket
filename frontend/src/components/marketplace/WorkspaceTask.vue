<template>
  <section class="workspace-task"><div><h2>{{ title }}</h2><p>{{ description }}</p></div><RouterLink :to="to" class="button button-primary">{{ action }} <MarketIcon name="arrow"/></RouterLink></section>
</template>
<script setup>
import MarketIcon from './MarketIcon.vue';
defineProps({title:String,description:String,to:{type:String,required:true},action:{type:String,required:true}});
</script>
<style scoped>
.workspace-task {display:flex;justify-content:space-between;align-items:center;gap:24px;flex-wrap:wrap;padding-block:8px 24px;margin-bottom:24px;}
.workspace-task h2 {font-size:var(--bm-type-section);line-height:1.3;margin:0 0 12px;}
.workspace-task p {font-size:var(--bm-type-body);line-height:1.6;margin:0;max-width:60ch;color:var(--bm-muted);}
</style>

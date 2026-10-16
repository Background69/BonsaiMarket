<template><section class="workspace-unavailable"><header class="workspace-page-heading"><h1>{{ title }}</h1><p>{{ message }}</p></header><RouterLink :to="to" class="button button-outline">{{ action }}</RouterLink></section></template>
<script setup>
defineProps({title:String,message:String,to:{type:String,required:true},action:{type:String,default:'Về tổng quan'}});
</script>
<style scoped>
.workspace-unavailable {max-width:70ch;}
.workspace-unavailable .workspace-page-heading {margin-bottom:24px;}
</style>

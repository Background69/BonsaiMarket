<template>
  <div v-if="error" :id="prefix+'-errors'" class="management-error" role="alert" tabindex="-1"><p>{{ error }}</p><ul v-if="Object.keys(fields).length"><li v-for="(message,key) in fields" :key="key"><a :href="'#'+prefix+'-'+key">{{ message }}</a></li></ul></div>
  <p v-if="success" class="management-success" role="status">{{ success }}</p>
</template>
<script setup>
defineProps({error:String,success:String,fields:{type:Object,default:()=>({})},prefix:{type:String,default:'form'}});
</script>

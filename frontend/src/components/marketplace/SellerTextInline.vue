<template><template v-for="(token,index) in tokens" :key="index"><strong v-if="token.kind==='bold'">{{ token.text }}</strong><em v-else-if="token.kind==='italic'">{{ token.text }}</em><a v-else-if="token.kind==='link'" :href="token.href" target="_blank" rel="noopener noreferrer nofollow">{{ token.text }}<span class="sr-only"> (mở trong thẻ mới)</span></a><template v-else>{{ token.text }}</template></template></template>
<script setup>
import {computed} from 'vue';import {inlineSellerText} from '../../services/sellerText.js';
const props=defineProps({text:{type:String,default:''},formatted:Boolean});
const tokens=computed(()=>inlineSellerText(props.text,props.formatted));
</script>

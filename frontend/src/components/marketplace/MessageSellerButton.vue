<template>
  <div class="message-seller-entry"><button type="button" class="button button-outline" :disabled="busy" @click="open">{{ busy?'Đang mở…':'Nhắn tin riêng cho gian hàng' }}</button><p v-if="error" role="alert">{{ error }}</p>
    <dialog ref="dialog" class="private-chat-dialog bm-motion-dialog" aria-labelledby="private-chat-title" tabindex="-1" @keydown.tab="trapDialogFocus" @close="closed"><header><h2 id="private-chat-title">Tin nhắn riêng tư</h2><button type="button" class="button button-outline" autofocus @click="dialog.close()">Đóng</button></header><PrivateConversation v-if="conversationId" :conversation-id="conversationId" :active="active"/><RouterLink v-if="conversationId" :to="'/messages/'+conversationId">Mở trong hộp thư</RouterLink></dialog>
  </div>
</template>
<script setup>
import {ref,onBeforeUnmount,watch} from 'vue';
import {useRoute,useRouter} from 'vue-router';
import {useAuth} from '../../composables/useAuth.js';
import {marketplaceRequest as request} from '../../services/marketplace.js';
import PrivateConversation from './PrivateConversation.vue';
import {trapDialogFocus} from '../../utils/dialogFocus.js';
const props=defineProps({storeId:{type:[Number,String],required:true}}),{user}=useAuth(),route=useRoute(),router=useRouter(),dialog=ref(null),conversationId=ref(null),active=ref(false),busy=ref(false),error=ref('');let generation=0,opener;
async function open(){if(busy.value)return;if(!user.value){router.push({path:'/login',query:{redirect:route.fullPath}});return;}const ticket=generation;busy.value=true;error.value='';opener=document.activeElement;try{const c=await request('/account/conversations','POST',{storeId:Number(props.storeId)},false);if(ticket!==generation)return;conversationId.value=c.id;active.value=true;dialog.value.showModal();}catch(e){if(ticket===generation)error.value=e.message;}finally{if(ticket===generation)busy.value=false;}}
function closed(){active.value=false;if(opener?.isConnected)opener.focus();}
watch([()=>props.storeId,()=>user.value?.id],()=>{generation++;dialog.value?.close();conversationId.value=null;busy.value=false;error.value='';});onBeforeUnmount(()=>generation++);
</script>
<style scoped>
.message-seller-entry{margin-top:12px}.private-chat-dialog{width:min(700px,calc(100% - 32px));max-height:calc(100dvh - 32px);overflow:auto;padding:24px;border:1px solid var(--bm-border);border-radius:var(--bm-radius);color:var(--bm-ink);background:var(--bm-surface);box-shadow:var(--bm-shadow-dialog)}.private-chat-dialog::backdrop{background:var(--bm-scrim)}.private-chat-dialog>header{display:flex;gap:16px;justify-content:space-between;align-items:center;margin-bottom:16px}.private-chat-dialog>header h2{font-size:var(--bm-type-subheading);margin:0}.private-chat-dialog>a{display:inline-block;padding-block:12px;min-height:44px}.private-chat-dialog :focus-visible{outline:3px solid var(--bm-forest);outline-offset:3px}
</style>

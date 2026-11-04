<template>
  <section class="private-thread" :aria-busy="loading" aria-label="Nội dung hội thoại riêng tư">
    <p v-if="!networkOnline" role="status">Bạn đang ngoại tuyến. Bản nháp được giữ; kết nối lại để tải và gửi tin nhắn.</p>
    <p v-if="loading" role="status">Đang tải lịch sử tin nhắn…</p>
    <div v-if="error" role="alert"><p>{{ error }}</p><button type="button" class="button button-outline" :disabled="loading" @click="refresh">Thử kết nối lại</button></div>
    <template v-if="conversation">
      <header class="private-thread-header"><h2>{{ conversation.kind==='STORE'?conversation.storeName:conversation.peerName }}</h2><p>{{ conversation.kind==='STORE'?'Trao đổi với '+conversation.peerName:'Hỗ trợ riêng tư · '+roleLabel(conversation.peerRole) }}</p><p class="muted">Chỉ người tham gia được đọc. Tin mới được kiểm tra mỗi 5 giây khi mở.</p></header>
      <div ref="scroller" class="private-message-scroll" tabindex="0" aria-label="Lịch sử tin nhắn" @scroll="trackScroll">
        <button v-if="hasOlder" type="button" class="button button-outline" :disabled="olderBusy" @click="older">{{ olderBusy?'Đang tải…':'Xem tin nhắn trước' }}</button>
        <p v-if="!messages.length && !loading">Chưa có tin nhắn. Hãy bắt đầu trao đổi.</p>
        <ol class="private-messages"><li v-for="message in messages" :key="message.sequence" :class="{mine:message.mine}"><strong>{{ message.mine?'Bạn':message.senderName }}</strong><p>{{ message.body }}</p><time :datetime="message.createdAt+'Z'">{{ date(message.createdAt) }}</time></li></ol>
      </div>
      <button v-if="newBelow" type="button" class="button button-outline" @click="toBottom">Có tin nhắn mới · xuống cuối</button>
      <form class="private-composer" :aria-busy="sending" @submit.prevent="send">
        <label :for="'private-draft-'+conversationId">Tin nhắn riêng tư</label><textarea :id="'private-draft-'+conversationId" v-model="draft" rows="3" required maxlength="4000" :disabled="sending" aria-describedby="private-message-help" @keydown="enter"/>
        <p id="private-message-help" class="muted">Enter để gửi · Shift+Enter xuống dòng · tối đa 4.000 ký tự.</p><p v-if="sendError" role="alert">{{ sendError }}</p>
        <button class="button button-primary" :disabled="sending || !draft.trim()">{{ sending?'Đang gửi…':'Gửi tin nhắn' }}</button>
      </form>
      <p class="sr-only" role="status" aria-live="polite">{{ announcement }}</p>
    </template>
  </section>
</template>
<script setup>
import {nextTick,onBeforeUnmount,ref,watch} from 'vue';
import {marketplaceRequest as request} from '../../services/marketplace.js';
import {useAuth} from '../../composables/useAuth.js';
const props=defineProps({conversationId:{type:[Number,String],required:true},active:{type:Boolean,default:true}}),{user}=useAuth();
const conversation=ref(null),messages=ref([]),loading=ref(false),error=ref(''),sendError=ref(''),draft=ref(''),sending=ref(false),hasOlder=ref(false),olderBusy=ref(false),scroller=ref(null),newBelow=ref(false),announcement=ref('');
const networkOnline=ref(navigator.onLine);
const emit=defineEmits(['read']);
let lastReadSent=0;
let epoch=0,timer,pollBusy=false,nearBottom=true,pending=null,terminal=false;const drafts=new Map();
const path=()=>'/account/conversations/'+encodeURIComponent(props.conversationId),date=v=>new Date(v+'Z').toLocaleString('vi-VN'),roleLabel=v=>({ADMIN:'Quản trị viên',SELLER:'Người bán',CUSTOMER:'Người mua'}[v]||'Thành viên');
function trackScroll(){const el=scroller.value;nearBottom=!el||el.scrollHeight-el.scrollTop-el.clientHeight<80;if(nearBottom)newBelow.value=false;}
async function toBottom(){await nextTick();if(scroller.value)scroller.value.scrollTop=scroller.value.scrollHeight;nearBottom=true;newBelow.value=false;}
function merge(rows){const map=new Map(messages.value.map(m=>[m.sequence,m]));for(const m of rows)map.set(m.sequence,m);messages.value=[...map.values()].sort((a,b)=>a.sequence-b.sequence);}
async function read(ticket){if(ticket!==epoch||!props.active||document.hidden||!messages.value.length)return;const max=messages.value.at(-1).sequence;if(max<=lastReadSent)return;await request(path()+'/read','POST',{sequence:max},false);if(ticket===epoch){lastReadSent=max;emit('read');}}
function schedule(){clearTimeout(timer);if(props.active&&!terminal)timer=setTimeout(poll,error.value?15000:5000);}
function failed(e){error.value=e.message;terminal=[400,401,403,404].includes(e.status);}
async function poll(){if(pollBusy||!props.active||terminal)return;if(document.hidden||!navigator.onLine){schedule();return;}const ticket=epoch;pollBusy=true;try{const after=messages.value.at(-1)?.sequence||0;const result=await request(path()+'/messages?after='+after);if(ticket!==epoch)return;const fresh=result.content.filter(m=>m.sequence>after);merge(fresh);if(fresh.length){announcement.value='Có '+fresh.length+' tin nhắn mới.';if(nearBottom)await toBottom();else newBelow.value=true;}error.value='';await read(ticket);}catch(e){if(ticket===epoch)failed(e);}finally{if(ticket===epoch){pollBusy=false;schedule();}}}
async function refresh(){if(loading.value)return;const ticket=epoch;terminal=false;loading.value=true;error.value='';try{const [info,data]=await Promise.all([request(path()),request(path()+'/messages')]);if(ticket!==epoch)return;conversation.value=info;messages.value=data.content;hasOlder.value=data.hasMore;await toBottom();await read(ticket);}catch(e){if(ticket===epoch)failed(e);}finally{if(ticket===epoch){loading.value=false;schedule();}}}
async function older(){if(olderBusy.value)return;const ticket=epoch,el=scroller.value,height=el?.scrollHeight||0;olderBusy.value=true;try{const result=await request(path()+'/messages?before='+messages.value[0].sequence);if(ticket!==epoch)return;merge(result.content);hasOlder.value=result.hasMore;await nextTick();if(el)el.scrollTop+=el.scrollHeight-height;}catch(e){if(ticket===epoch)error.value=e.message;}finally{if(ticket===epoch)olderBusy.value=false;}}
function enter(event){if(event.key==='Enter'&&!event.shiftKey&&!event.isComposing){event.preventDefault();if(draft.value.trim())send();}}
async function send(){
  if(sending.value||!draft.value.trim())return;
  const ticket=epoch,text=draft.value,restoreInput=document.activeElement?.id==='private-draft-'+props.conversationId;
  const payload=pending?.body===text?pending:{body:text,clientKey:crypto.randomUUID()};
  pending=payload;sending.value=true;sendError.value='';
  try{
    await request(path()+'/messages','POST',payload,false);
    if(ticket!==epoch)return;
    draft.value='';pending=null;drafts.delete(String(props.conversationId));
    await poll();await toBottom();announcement.value='Đã gửi tin nhắn.';
  }catch(e){if(ticket===epoch)sendError.value=e.message;}
  finally{
    if(ticket===epoch){
      sending.value=false;await nextTick();
      if(restoreInput&&document.activeElement===document.body)document.getElementById('private-draft-'+props.conversationId)?.focus({preventScroll:true});
    }
  }
}
watch([()=>props.conversationId,()=>user.value?.id],(value,previous)=>{if(previous&&value[1]===previous[1])drafts.set(String(previous[0]),{body:draft.value,pending});else drafts.clear();epoch++;lastReadSent=0;clearTimeout(timer);conversation.value=null;messages.value=[];error.value='';sendError.value='';loading.value=false;pollBusy=false;sending.value=false;olderBusy.value=false;const saved=drafts.get(String(props.conversationId));draft.value=saved?.body||'';pending=saved?.pending||null;newBelow.value=false;announcement.value='';if(props.conversationId&&user.value)refresh();},{immediate:true});
watch(()=>props.active,active=>{clearTimeout(timer);if(active)poll();});
function visible(){if(props.active&&!document.hidden)poll();}
function connectivity(){networkOnline.value=navigator.onLine;if(networkOnline.value)visible();}
document.addEventListener('visibilitychange',visible);window.addEventListener('online',connectivity);window.addEventListener('offline',connectivity);
onBeforeUnmount(()=>{epoch++;clearTimeout(timer);drafts.clear();document.removeEventListener('visibilitychange',visible);window.removeEventListener('online',connectivity);window.removeEventListener('offline',connectivity);});
</script>
<style scoped>
.private-thread{min-width:0;overflow-wrap:anywhere}.private-thread-header h2{font-size:var(--bm-type-subheading);margin:0 0 8px}.private-thread-header p{margin:4px 0}.private-thread .muted{color:var(--bm-muted);font-size:var(--bm-type-label);line-height:1.6}.private-message-scroll{max-height:50dvh;min-height:160px;overflow-y:auto;overscroll-behavior:contain;padding:12px 4px}.private-messages{display:flex;flex-direction:column;gap:16px;list-style:none;margin:0;padding:0}.private-messages li{max-width:90%;align-self:flex-start;background:var(--bm-soft);padding:12px 16px;border:1px solid var(--bm-border);border-radius:var(--bm-radius)}.private-messages li.mine{align-self:flex-end;background:var(--bm-surface)}.private-messages p{white-space:pre-wrap;margin:8px 0;line-height:1.6}.private-messages time{font-size:var(--bm-type-meta);color:var(--bm-muted)}.private-composer{display:grid;gap:8px;border-top:1px solid var(--bm-border);padding-top:16px}.private-composer label{font-weight:600}.private-composer textarea{width:100%;padding:12px;border:1px solid var(--bm-control-border);border-radius:var(--bm-radius-control);font:inherit;background:var(--bm-surface);color:var(--bm-ink);resize:vertical}.private-thread :focus-visible{outline:3px solid var(--bm-forest);outline-offset:3px}.sr-only{position:absolute;width:1px;height:1px;overflow:hidden;clip-path:inset(50%);white-space:nowrap}
</style>

<script setup>
import {computed,nextTick,onBeforeUnmount,ref,watch} from 'vue';
import {marketplaceRequest as request} from '../../services/marketplace.js';
import {useAuth} from '../../composables/useAuth.js';
import MarketIcon from './MarketIcon.vue';
import ConversationStoreReport from './ConversationStoreReport.vue';
import CustomerReportDialog from './CustomerReportDialog.vue';
import {privateMessageDrafts as drafts,privateMessageScrolls as scrolls,guardPrivateDraftActor} from '../../services/privateMessageDrafts.js';
const props=defineProps({conversationId:{type:[Number,String],required:true},active:{type:Boolean,default:true},inbox:Boolean}),{user}=useAuth();
const conversation=ref(null),messages=ref([]),loading=ref(false),error=ref(''),sendError=ref(''),draft=ref(''),sending=ref(false),hasOlder=ref(false),olderBusy=ref(false),scroller=ref(null),newBelow=ref(false),announcement=ref(''),delivery=ref(null),networkOnline=ref(navigator.onLine);
const emit=defineEmits(['read']);
let lastReadSent=0,epoch=0,timer,pollBusy=false,nearBottom=true,pending=null,terminal=false,failures=0,disposed=false;
const title=computed(()=>user.value?.role==='seller'?conversation.value?.peerName:conversation.value?.storeName||conversation.value?.peerName);
const path=()=>'/account/conversations/'+encodeURIComponent(props.conversationId);
const day=v=>new Date(v+'Z').toLocaleDateString('vi-VN'),time=v=>new Date(v+'Z').toLocaleTimeString('vi-VN',{hour:'2-digit',minute:'2-digit'}),money=v=>new Intl.NumberFormat('vi-VN',{style:'currency',currency:'VND'}).format(v);
function trackScroll(){const el=scroller.value;nearBottom=!el||el.scrollHeight-el.scrollTop-el.clientHeight<60;if(nearBottom){newBelow.value=false;read(epoch).catch(()=>{});}}
async function toBottom(){await nextTick();if(scroller.value)scroller.value.scrollTop=scroller.value.scrollHeight;nearBottom=true;newBelow.value=false;}
function merge(rows){const map=new Map(messages.value.map(m=>[m.sequence,m]));for(const m of rows)map.set(m.sequence,m);messages.value=[...map.values()].sort((a,b)=>a.sequence-b.sequence);if(pending&&rows.some(m=>m.clientKey===pending.clientKey)){if(draft.value===pending.body)draft.value='';pending=null;delivery.value=null;sendError.value='';}}
async function read(ticket){if(ticket!==epoch||!props.active||document.hidden||!nearBottom||!scroller.value?.getClientRects().length||!messages.value.length)return;const max=messages.value.at(-1).sequence;if(max<=lastReadSent)return;await request(path()+'/read','POST',{sequence:max},false);if(ticket===epoch){lastReadSent=max;emit('read');}}
function schedule(){clearTimeout(timer);if(props.active&&!terminal&&!disposed)timer=setTimeout(poll,Math.min(120000,10000*2**failures));}
function failed(e){error.value=e.message;failures=Math.min(4,failures+1);terminal=[400,401,403,404].includes(e.status);}
async function poll(){if(pollBusy||!props.active||terminal)return;if(document.hidden||!navigator.onLine){schedule();return;}const ticket=epoch;pollBusy=true;try{const after=messages.value.at(-1)?.sequence||0,result=await request(path()+'/messages?after='+after);if(ticket!==epoch)return;const fresh=result.content.filter(m=>m.sequence>after);merge(fresh);if(fresh.length){announcement.value='Có '+fresh.length+' tin nhắn mới.';if(nearBottom)await toBottom();else newBelow.value=true;}error.value='';failures=0;await read(ticket);}catch(e){if(ticket===epoch)failed(e);}finally{if(ticket===epoch){pollBusy=false;schedule();}}}
async function refresh(){if(loading.value||!props.active)return;const ticket=epoch;terminal=false;loading.value=true;error.value='';try{const [info,data]=await Promise.all([request(path()),request(path()+'/messages')]);if(ticket!==epoch)return;conversation.value=info;messages.value=data.content;hasOlder.value=data.hasMore;merge(data.content);await toBottom();if(scrolls.has(String(props.conversationId))){scroller.value.scrollTop=scrolls.get(String(props.conversationId));trackScroll();}await read(ticket);}catch(e){if(ticket===epoch)failed(e);}finally{if(ticket===epoch){loading.value=false;schedule();}}}
async function older(){if(olderBusy.value)return;const ticket=epoch,el=scroller.value,height=el?.scrollHeight||0;olderBusy.value=true;try{const result=await request(path()+'/messages?before='+messages.value[0].sequence);if(ticket!==epoch)return;merge(result.content);hasOlder.value=result.hasMore;await nextTick();if(el)el.scrollTop+=el.scrollHeight-height;}catch(e){if(ticket===epoch)error.value=e.message;}finally{if(ticket===epoch)olderBusy.value=false;}}
function enter(event){if(event.key==='Enter'&&!event.shiftKey&&!event.isComposing){event.preventDefault();if(draft.value.trim())send();}}
async function send(){
 if(sending.value||(!pending&&!draft.value.trim()))return;
 const ticket=epoch,endpoint=path();pending??={body:draft.value,clientKey:crypto.randomUUID()};const payload=pending;
 sending.value=true;sendError.value='';delivery.value=delivery.value==='failed'?'retrying':'sending';
 try{
  // Resolve an uncertain delivery before any retry. The original key is retained even if absent.
  const result=await request(endpoint+'/delivery/'+payload.clientKey);if(ticket!==epoch)return;
  const saved=result.message||await request(endpoint+'/messages','POST',payload,false);if(ticket!==epoch)return;
  merge([saved]);pending=null;draft.value='';delivery.value=null;drafts.delete(String(props.conversationId));await toBottom();await read(ticket);announcement.value='Đã gửi tin nhắn.';
 }catch(e){if(ticket===epoch){sendError.value=e.message;delivery.value='failed';}}
 finally{if(ticket===epoch)sending.value=false;}
}
watch([()=>props.conversationId,()=>user.value?.id],(value,previous)=>{
 guardPrivateDraftActor(value[1]);
 if(previous&&value[1]===previous[1]){drafts.set(String(previous[0]),{body:draft.value,pending,delivery:delivery.value});scrolls.set(String(previous[0]),scroller.value?.scrollTop||0);}
 epoch++;lastReadSent=0;clearTimeout(timer);conversation.value=null;messages.value=[];error.value='';sendError.value='';loading.value=false;pollBusy=false;sending.value=false;olderBusy.value=false;
 const saved=drafts.get(String(props.conversationId));draft.value=saved?.body||'';pending=saved?.pending||null;delivery.value=pending?'failed':null;newBelow.value=false;announcement.value='';nearBottom=true;
 if(props.conversationId&&user.value)nextTick(refresh);
},{immediate:true});
watch(()=>props.active,active=>{clearTimeout(timer);if(active)conversation.value?poll():refresh();});
function visible(){if(props.active&&!document.hidden)poll();}
function connectivity(){networkOnline.value=navigator.onLine;if(networkOnline.value)visible();}
document.addEventListener('visibilitychange',visible);window.addEventListener('online',connectivity);window.addEventListener('offline',connectivity);
onBeforeUnmount(()=>{if(user.value){guardPrivateDraftActor(user.value.id);drafts.set(String(props.conversationId),{body:draft.value,pending,delivery:delivery.value});scrolls.set(String(props.conversationId),scroller.value?.scrollTop||0);}disposed=true;epoch++;clearTimeout(timer);document.removeEventListener('visibilitychange',visible);window.removeEventListener('online',connectivity);window.removeEventListener('offline',connectivity);});
</script>
<template><section class="bm-messages-thread" :aria-busy="loading" aria-label="Nội dung hội thoại riêng tư">
 <RouterLink v-if="inbox" class="bm-messages-back" to="/messages"><MarketIcon name="chevronLeft"/> Danh sách hội thoại</RouterLink>
 <p v-if="!networkOnline" class="bm-messages-notice" role="status">Bạn đang ngoại tuyến. Bản nháp được giữ lại.</p>
 <p v-if="loading" class="bm-messages-notice" role="status">Đang tải lịch sử tin nhắn…</p>
 <div v-if="error" class="bm-messages-notice" role="alert">{{ error }} <button type="button" @click="refresh">Thử lại</button></div>
 <template v-if="conversation">
 <header class="bm-messages-thread-heading"><span class="bm-messages-avatar"><img v-if="conversation.avatarUrl" :src="conversation.avatarUrl" alt="" @error="e=>e.target.hidden=true"/><MarketIcon v-else name="store"/></span><div><h2>{{ title }}</h2><p>Chỉ người tham gia được đọc · Cập nhật mỗi 10 giây</p></div><div class="bm-messages-thread-actions"><button type="button" :disabled="loading" @click="refresh"><MarketIcon name="clock"/> Làm mới</button><ConversationStoreReport v-if="conversation.canReportStore" :store-id="conversation.storeId"/><CustomerReportDialog v-else-if="conversation.reportOrderId" :order-id="conversation.reportOrderId"/></div></header>
 <div v-if="conversation.product" class="bm-messages-product"><img v-if="conversation.product.imageUrl" :src="conversation.product.imageUrl" alt="" @error="e=>e.target.hidden=true"/><MarketIcon v-else name="box"/><div><small>Sản phẩm đang trao đổi</small><strong>{{ conversation.product.name }}</strong><b>{{ money(conversation.product.price) }}</b></div><RouterLink :to="'/products/'+conversation.product.id">Xem sản phẩm <MarketIcon name="arrow"/></RouterLink></div>
 <div ref="scroller" class="bm-messages-scroll" tabindex="0" aria-label="Lịch sử tin nhắn" @scroll="trackScroll">
 <button v-if="hasOlder" type="button" :disabled="olderBusy" @click="older">{{ olderBusy?'Đang tải…':'Xem tin nhắn trước' }}</button>
 <p v-if="!messages.length&&!loading" class="bm-messages-notice">Chưa có tin nhắn. Hãy bắt đầu trao đổi.</p>
 <ol class="bm-messages-bubbles"><template v-for="(message,index) in messages" :key="message.sequence">
 <li v-if="!index||day(message.createdAt)!==day(messages[index-1].createdAt)" class="bm-messages-date"><span>{{ day(message.createdAt) }}</span></li>
 <li :class="['bm-messages-bubble',{mine:message.mine}]"><span class="bm-messages-sr">{{ message.mine?'Bạn':message.senderName }}: </span><p>{{ message.body }}</p><small><time :datetime="message.createdAt+'Z'">{{ time(message.createdAt) }}</time><span v-if="message.mine"> · Đã gửi</span></small></li>
 </template><li v-if="pending" class="bm-messages-bubble mine" :class="{'bm-messages-failed':delivery==='failed'}"><p>{{ pending.body }}</p><small role="status">{{ delivery==='failed'?'Gửi chưa được xác nhận':delivery==='retrying'?'Đang xác nhận lại…':'Đang gửi…' }}</small><button v-if="delivery==='failed'" type="button" :disabled="sending" @click="send">Xác nhận & thử lại</button></li></ol>
 </div>
 <button v-if="newBelow" type="button" @click="toBottom">Có tin nhắn mới · xuống cuối</button>
 <form class="bm-messages-composer" :aria-busy="sending" @submit.prevent="send"><p v-if="sendError" role="alert">{{ sendError }}</p><label class="bm-messages-sr" :for="'private-draft-'+conversationId">Tin nhắn riêng tư</label><textarea :id="'private-draft-'+conversationId" v-model="draft" rows="2" required maxlength="4000" :readonly="!!pending" :placeholder="'Nhập tin nhắn cho '+title+'…'" aria-describedby="bm-message-help" @keydown="enter"/><button type="submit" :disabled="sending||!draft.trim()"><MarketIcon name="send"/>{{ sending?'Đang gửi…':pending?'Thử lại':'Gửi tin nhắn' }}</button><small id="bm-message-help">Enter để gửi · Shift+Enter xuống dòng · {{ draft.length }}/4.000 ký tự</small></form>
 <p class="bm-messages-sr" role="status" aria-live="polite">{{ announcement }}</p>
 </template>
</section></template>
<style scoped>
.bm-messages-thread{min-width:0;overflow-wrap:anywhere;display:flex;flex-direction:column;border:1px solid var(--bm-border);border-radius:16px;background:var(--bm-surface);box-shadow:0 4px 14px rgb(11 61 43 / 6%);overflow:hidden;color:var(--bm-ink)}
.bm-messages-thread-heading{display:flex;gap:12px;align-items:center;padding:14px 18px;border-bottom:1px solid var(--bm-border);flex-wrap:wrap}.bm-messages-thread-heading h2{font:700 18px var(--bm-font);margin:0 0 5px}.bm-messages-thread-heading p{margin:0;font-size:12px;color:var(--bm-muted)}.bm-messages-thread-actions{margin-left:auto;display:flex;align-items:center;gap:8px;flex-wrap:wrap}
.bm-messages-thread button{font:inherit;cursor:pointer;min-height:40px;padding:8px 12px;color:var(--bm-forest);background:var(--bm-surface);border:1px solid var(--bm-border);border-radius:8px}.bm-messages-thread button:disabled{opacity:.6;cursor:default}.bm-messages-thread-actions>button{border:0;font-size:13px;display:flex;align-items:center;gap:8px}
.bm-messages-avatar{width:44px;height:44px;flex-shrink:0;border-radius:50%;background:var(--bm-soft);color:var(--bm-forest);display:grid;place-items:center;overflow:hidden}.bm-messages-avatar img{width:100%;height:100%;object-fit:cover}
.bm-messages-product{display:flex;align-items:center;gap:14px;margin:12px 16px;background:var(--bm-soft);border-radius:12px;padding:10px 14px}.bm-messages-product>img{width:66px;height:62px;object-fit:cover;border-radius:8px}.bm-messages-product>div{display:grid;gap:4px;min-width:0}.bm-messages-product small{font-size:12px;color:var(--bm-muted)}.bm-messages-product strong{font-size:14px}.bm-messages-product b{color:var(--bm-forest)}.bm-messages-product>a{margin-left:auto;color:var(--bm-forest);font-weight:700;font-size:13px;display:flex;gap:8px;align-items:center;flex-shrink:0;border:1px solid var(--bm-control-border);border-radius:8px;padding:12px;text-decoration:none}
.bm-messages-scroll{height:365px;min-height:220px;overflow-y:auto;overscroll-behavior:contain;padding:8px 18px;scrollbar-gutter:stable;flex:1}.bm-messages-bubbles{display:flex;flex-direction:column;gap:14px;list-style:none;margin:0;padding:0}.bm-messages-bubble{max-width:84%;align-self:flex-start;padding:10px 14px;background:var(--bm-surface);border:1px solid var(--bm-border);border-radius:14px}.bm-messages-bubble.mine{align-self:flex-end;background:var(--bm-soft);border-color:transparent}.bm-messages-bubble p{white-space:pre-wrap;line-height:1.5;font-size:14px;margin:0 0 6px}.bm-messages-bubble small{font-size:11px;color:var(--bm-muted);display:block;text-align:right}.bm-messages-bubble.bm-messages-failed{background:var(--bm-error-surface);border-color:var(--bm-error)}.bm-messages-date{align-self:stretch;text-align:center;font-size:11px;color:var(--bm-muted);margin:5px 0}.bm-messages-date span{padding:5px 14px;border-radius:20px;background:var(--bm-bg)}
.bm-messages-composer{display:grid;grid-template-columns:minmax(0,1fr) auto;align-items:center;gap:8px 12px;margin:14px 16px 12px}.bm-messages-composer textarea{width:100%;font:14px/1.5 var(--bm-font);padding:10px 14px;border:1px solid var(--bm-control-border);border-radius:10px;resize:vertical;color:var(--bm-ink);background:var(--bm-surface);max-height:160px;min-height:50px}.bm-messages-composer>button{background:var(--bm-forest);color:var(--bm-surface);font-size:14px;font-weight:700;display:flex;align-items:center;gap:10px}.bm-messages-composer>small{grid-column:1/-1;font-size:11px;color:var(--bm-muted)}.bm-messages-composer>p{grid-column:1/-1;color:var(--bm-error);font-size:13px}
.bm-messages-notice{padding:12px;line-height:1.6}.bm-messages-sr{position:absolute;width:1px;height:1px;overflow:hidden;clip-path:inset(50%)}.bm-messages-back{display:none}.bm-messages-thread :focus-visible{outline:3px solid var(--bm-forest);outline-offset:3px}
@media(hover:hover) and (pointer:fine){.bm-messages-composer>button{transition:transform var(--bm-motion-fast) var(--bm-motion-ease)}.bm-messages-composer>button:active{transform:scale(.98)}}
@media(max-width:767px){.bm-messages-back{display:flex;gap:8px;align-items:center;padding:12px;color:var(--bm-forest)}.bm-messages-thread-heading{padding:12px}.bm-messages-thread-heading h2{font-size:16px}.bm-messages-thread-actions{margin-left:0}.bm-messages-product{margin:10px;padding:10px;flex-wrap:wrap}.bm-messages-product>a{padding:8px}.bm-messages-scroll{height:45dvh;flex:auto;padding:12px}.bm-messages-composer{margin:10px;gap:8px;padding-bottom:env(safe-area-inset-bottom)}.bm-messages-composer textarea{font-size:16px}.bm-messages-composer>button{padding:10px;font-size:12px}.bm-messages-bubble{max-width:92%}}
@media(prefers-reduced-motion:reduce){.bm-messages-thread button{transition:none}}
</style>

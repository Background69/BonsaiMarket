<script setup>
import {computed,onBeforeUnmount,ref,watch} from 'vue';
import {useRoute,useRouter} from 'vue-router';
import {useAuth} from '../../composables/useAuth.js';
import {marketplaceRequest as request} from '../../services/marketplace.js';
import MarketplaceLayout from '../../layouts/MarketplaceLayout.vue';
import AdminLayout from '../../layouts/AdminLayout.vue';
import SellerLayout from './Seller/SellerLayout.vue';
import PrivateConversation from '../../components/marketplace/PrivateConversation.vue';
import MarketIcon from '../../components/marketplace/MarketIcon.vue';
import '../../css/messages.css';
const route=useRoute(),router=useRouter(),{user}=useAuth();
const data=ref({content:[],totalPages:0,totalElements:0}),page=ref(0),unread=ref(null),search=ref(''),unreadOnly=ref(false),error=ref(''),loading=ref(false);
let generation=0,revision=0,timer,debounce,failures=0,disposed=false;
const layout=computed(()=>user.value?.role==='admin'?AdminLayout:user.value?.role==='seller'?SellerLayout:MarketplaceLayout);
const title=item=>user.value?.role==='seller'?item.peerName:item.storeName||item.peerName;
const time=v=>v?new Date(v+'Z').toLocaleTimeString('vi-VN',{hour:'2-digit',minute:'2-digit'}):'';
function schedule(){clearTimeout(timer);if(!disposed&&user.value)timer=setTimeout(()=>load(true),Math.min(120000,15000*2**failures));}
async function load(quiet=false){
 if(quiet&&(document.hidden||!navigator.onLine)){schedule();return;}
 const ticket=++revision,actor=generation;loading.value=!quiet;error.value='';
 try{const [list,count]=await Promise.all([request('/account/conversations?'+new URLSearchParams({page:page.value,search:search.value,unreadOnly:unreadOnly.value})),request('/account/conversations/unread')]);
 if(actor!==generation||ticket!==revision)return;data.value=list;unread.value=count.count;failures=0;
 if(page.value>0&&page.value>=list.totalPages){page.value=Math.max(0,list.totalPages-1);return load();}
 }catch(e){if(actor===generation&&ticket===revision){error.value=e.message;failures=Math.min(3,failures+1);}}
 finally{if(actor===generation&&ticket===revision){loading.value=false;schedule();}}
}
watch([search,unreadOnly],()=>{clearTimeout(debounce);revision++;page.value=0;debounce=setTimeout(()=>load(),300);});
watch(()=>user.value?.id,async()=>{generation++;revision++;clearTimeout(timer);data.value={content:[],totalPages:0,totalElements:0};unread.value=null;page.value=0;const ticket=generation;
 try{if(user.value&&route.query.supportUser){const c=await request('/account/conversations','POST',{supportUserId:Number(route.query.supportUser)},false);if(ticket!==generation)return;await router.replace('/messages/'+c.id);}if(user.value)await load();}catch(e){if(ticket===generation)error.value=e.message;}
},{immediate:true});
function visible(){if(!document.hidden)load(true);}
document.addEventListener('visibilitychange',visible);
onBeforeUnmount(()=>{disposed=true;generation++;revision++;clearTimeout(timer);clearTimeout(debounce);document.removeEventListener('visibilitychange',visible);});
</script>
<template><component :is="layout"><main class="bm-messages-page market-container">
 <header class="bm-messages-hero"><span class="bm-messages-hero-icon"><MarketIcon name="chat"/></span><div><h1>Tin nhắn riêng tư</h1><p>Trao đổi trực tiếp với người tham gia. Nội dung không hiển thị trong hỏi đáp công khai.</p></div></header>
 <div class="bm-messages-grid" :class="{'bm-messages-open':!!route.params.id}">
 <aside class="bm-messages-inbox" aria-label="Danh sách hội thoại"><div class="bm-messages-list-heading"><h2>Danh sách hội thoại</h2><span><MarketIcon name="clock"/> Cập nhật tự động</span></div>
 <label class="bm-messages-search"><MarketIcon name="search"/><span class="bm-messages-sr">Tìm hội thoại</span><input v-model="search" type="search" placeholder="Tìm hội thoại..." maxlength="160"/></label>
 <div class="bm-messages-filters" aria-label="Lọc hội thoại"><button type="button" :aria-pressed="!unreadOnly" @click="unreadOnly=false">Tất cả<span v-if="!search&&!unreadOnly"> ({{ data.totalElements }})</span></button><button type="button" :aria-pressed="unreadOnly" @click="unreadOnly=true">Chưa đọc<span v-if="unread!==null"> · {{ unread }} tin</span></button></div>
 <p v-if="loading" class="bm-messages-state" role="status">Đang tải hội thoại…</p>
 <div v-if="error" class="bm-messages-state" role="alert">{{ error }} <button type="button" @click="load()">Thử lại</button></div>
 <p v-else-if="!loading&&!data.content.length" class="bm-messages-state">{{ search||unreadOnly?'Không có hội thoại phù hợp.':'Chưa có hội thoại. Mở trang sản phẩm để nhắn cho gian hàng.' }}</p>
 <ul class="bm-messages-list"><li v-for="item in data.content" :key="item.id"><RouterLink :to="'/messages/'+item.id" :aria-current="String(item.id)===String(route.params.id)?'page':undefined">
 <span class="bm-messages-avatar"><img v-if="item.avatarUrl" :src="item.avatarUrl" alt="" @error="event=>event.target.hidden=true"/><MarketIcon v-else :name="item.kind==='STORE'?'store':'user'"/></span>
 <span class="bm-messages-preview"><strong>{{ title(item) }}</strong><small>{{ item.kind==='SUPPORT'?'Hỗ trợ riêng tư':user?.role==='seller'?'Khách hàng':'Người bán Bonsai' }}</small><span>{{ item.lastMessage||'Chưa có tin nhắn' }}</span></span>
 <span class="bm-messages-meta"><time>{{ time(item.updatedAt) }}</time><b v-if="item.unreadCount" :aria-label="item.unreadCount+' tin chưa đọc'">{{ item.unreadCount }}</b></span>
 </RouterLink></li></ul>
 <nav v-if="data.totalPages>1" class="bm-messages-pagination" aria-label="Phân trang hội thoại"><button :disabled="page===0||loading" @click="page--;load()">Trước</button><span>{{ page+1 }} / {{ data.totalPages }}</span><button :disabled="page+1>=data.totalPages||loading" @click="page++;load()">Sau</button></nav>
 <button class="bm-messages-refresh" type="button" :disabled="loading" @click="load()"><MarketIcon name="clock"/> Làm mới hộp thư</button>
 <div class="bm-messages-tip"><MarketIcon name="chat"/><div><strong>Bạn có thắc mắc về sản phẩm?</strong><p>Nhắn tin cho người bán để trao đổi thông tin trước khi mua.</p></div></div>
 </aside>
 <PrivateConversation v-show="route.params.id" :conversation-id="route.params.id||0" :active="!!route.params.id" inbox @read="load(true)"/>
 <section v-if="!route.params.id" class="bm-messages-placeholder"><MarketIcon name="chat"/><h2>Cuộc trò chuyện của bạn</h2><p>Chọn một hội thoại để xem lịch sử và gửi tin nhắn.</p><p>Chỉ người tham gia được đọc nội dung riêng tư.</p></section>
 </div>
</main></component></template>
<template>
  <component :is="layout"><div class="market-container market-page private-inbox"><header class="market-page-heading"><h1>Tin nhắn riêng tư</h1><p>{{ user?.role==='admin'?'Chỉ hiển thị hội thoại hỗ trợ bạn tham gia. Không có quyền đọc mọi cuộc trò chuyện.':'Trao đổi trực tiếp với người tham gia. Nội dung không hiển thị trong hỏi đáp công khai.' }}</p></header>
    <p v-if="error" role="alert">{{ error }} <button class="button button-outline" :disabled="loading" @click="load">Thử lại</button></p>
    <div class="private-inbox-grid"><aside aria-label="Danh sách hội thoại"><h2>Hộp thư <span v-if="unread">· {{ unread }} tin chưa đọc</span></h2><p v-if="loading" role="status">Đang tải hội thoại…</p><p v-else-if="!error && !data.content.length">Chưa có hội thoại. Người mua có thể nhắn từ trang sản phẩm; Admin bắt đầu hỗ trợ từ hồ sơ người dùng.</p><ul><li v-for="item in data.content" :key="item.id"><RouterLink :to="'/messages/'+item.id" :aria-current="String(item.id)===String(route.params.id)?'page':undefined"><strong>{{ item.storeName||item.peerName }}</strong><span>{{ item.kind==='SUPPORT'?'Hỗ trợ · ':'' }}{{ item.peerName }}</span><span v-if="item.unreadCount">{{ item.unreadCount }} tin chưa đọc</span></RouterLink></li></ul><nav v-if="data.totalPages>1" class="management-actions" aria-label="Phân trang hội thoại"><button class="button button-outline" :disabled="page===0||loading" @click="page--;load()">Trước</button><span>{{ page+1 }} / {{ data.totalPages }}</span><button class="button button-outline" :disabled="page+1>=data.totalPages||loading" @click="page++;load()">Sau</button></nav><button class="button button-outline" :disabled="loading" @click="load">Cập nhật hộp thư</button></aside>
      <PrivateConversation v-if="route.params.id" :conversation-id="route.params.id" @read="load"/><p v-else class="private-inbox-hint">Chọn một hội thoại để xem lịch sử và gửi tin nhắn.</p></div>
  </div></component>
</template>
<script setup>
import {computed,onBeforeUnmount,ref,watch} from 'vue';
import {useRoute,useRouter} from 'vue-router';
import {useAuth} from '../../composables/useAuth.js';
import {marketplaceRequest as request} from '../../services/marketplace.js';
import MarketplaceLayout from '../../layouts/MarketplaceLayout.vue';
import AdminLayout from '../../layouts/AdminLayout.vue';
import SellerLayout from './Seller/SellerLayout.vue';
import PrivateConversation from '../../components/marketplace/PrivateConversation.vue';
const route=useRoute(),router=useRouter(),{user}=useAuth(),data=ref({content:[],totalPages:0}),page=ref(0),unread=ref(0),error=ref(''),loading=ref(false);let generation=0,timer;
const layout=computed(()=>user.value?.role==='admin'?AdminLayout:user.value?.role==='seller'?SellerLayout:MarketplaceLayout);
async function load(){if(loading.value)return;const ticket=generation;loading.value=true;error.value='';try{const [list,count]=await Promise.all([request('/account/conversations?page='+page.value),request('/account/conversations/unread')]);if(ticket!==generation)return;data.value=list;unread.value=count.count;}catch(e){if(ticket===generation)error.value=e.message;}finally{if(ticket===generation)loading.value=false;}}
async function init(){generation++;clearInterval(timer);data.value={content:[],totalPages:0};unread.value=0;error.value='';loading.value=false;const ticket=generation;try{if(route.query.supportUser){const conversation=await request('/account/conversations','POST',{supportUserId:Number(route.query.supportUser)},false);if(ticket!==generation)return;await router.replace('/messages/'+conversation.id);}await load();if(ticket===generation)timer=setInterval(()=>{if(!document.hidden&&navigator.onLine)load();},15000);}catch(e){if(ticket===generation)error.value=e.message;}}
watch(()=>user.value?.id,init,{immediate:true});onBeforeUnmount(()=>{generation++;clearInterval(timer);});
</script>
<style scoped>
.private-inbox{min-width:0;overflow-wrap:anywhere}.private-inbox>header{margin-bottom:24px}.private-inbox-grid{display:grid;grid-template-columns:minmax(240px,1fr) minmax(0,2fr);gap:24px}.private-inbox aside{min-width:0;border-right:1px solid var(--bm-border);padding-right:24px}.private-inbox h2{font-size:var(--bm-type-subheading)}.private-inbox ul{list-style:none;margin:16px 0;padding:0}.private-inbox li{border-bottom:1px solid var(--bm-border)}.private-inbox li a{display:grid;gap:4px;padding:16px 12px;border-radius:var(--bm-radius);color:var(--bm-ink)}.private-inbox li a[aria-current]{background:var(--bm-soft)}.private-inbox li span{color:var(--bm-muted);font-size:var(--bm-type-label)}.private-inbox-hint{padding:24px;color:var(--bm-muted)}@media(max-width:767px){.private-inbox-grid{grid-template-columns:minmax(0,1fr)}.private-inbox aside{border-right:0;border-bottom:1px solid var(--bm-border);padding:0 0 24px}}
</style>

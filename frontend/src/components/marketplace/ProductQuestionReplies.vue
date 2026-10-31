<template>
  <details class="question-replies" @toggle="open">
    <summary>Xem và trả lời câu hỏi</summary>
    <div v-if="expanded">
      <p v-if="loading" role="status">Đang tải trả lời…</p>
      <div v-else-if="error" role="alert"><p>{{ error }}</p><button class="button button-outline" @click="load">Thử lại</button></div>
      <template v-else>
        <p v-if="!data.content.length" class="discussion-notice">Chưa có trả lời từ thành viên.</p>
        <ol class="question-reply-list"><li v-for="reply in data.content" :key="reply.id">
          <header><strong>{{ reply.authorName }}</strong><span v-if="reply.authorLabel==='SELLER'" class="discussion-meta">Chủ gian hàng</span><span v-else-if="reply.authorLabel==='ADMIN'" class="discussion-meta">Quản trị viên</span><time :datetime="reply.createdAt">{{ date(reply.createdAt) }}</time></header><p class="discussion-text">{{ reply.body }}</p>
        </li></ol>
        <nav v-if="data.totalPages>1" class="discussion-pagination" aria-label="Phân trang trả lời"><button class="button button-outline" :disabled="page===0||busy" @click="page--;load()">Trước</button><span>Trang {{ page+1 }} / {{ data.totalPages }}</span><button class="button button-outline" :disabled="page+1>=data.totalPages||busy" @click="page++;load()">Sau</button></nav>
      </template>
      <p v-if="message" role="status">{{ message }}</p><p v-if="submitError" role="alert">{{ submitError }}</p>
      <form v-if="isAuthenticated" :aria-busy="busy" @submit.prevent="submit"><label :for="'member-reply-'+questionId">Trả lời của bạn</label><textarea :id="'member-reply-'+questionId" v-model="body" required maxlength="4000" rows="3" :disabled="busy"/><p class="discussion-notice">Tối đa 4.000 ký tự. Nội dung hiển thị công khai.</p><button class="button button-outline" :disabled="busy||!body.trim()">{{ busy?'Đang gửi…':'Gửi trả lời' }}</button></form>
      <p v-else><RouterLink :to="{path:'/login',query:{redirect:route.fullPath}}">Đăng nhập để trả lời</RouterLink></p>
    </div>
  </details>
</template>
<script setup>
import {ref,watch,onBeforeUnmount} from 'vue';
import {useRoute} from 'vue-router';
import {useAuth} from '../../composables/useAuth.js';
import {marketplaceRequest as request} from '../../services/marketplace.js';
const props=defineProps({productId:{type:[Number,String],required:true},questionId:{type:[Number,String],required:true}}),route=useRoute(),{user,isAuthenticated}=useAuth();
const expanded=ref(false),data=ref({content:[],totalPages:0}),page=ref(0),loading=ref(false),error=ref(''),body=ref(''),busy=ref(false),message=ref(''),submitError=ref('');let context=0,sequence=0;
const date=v=>v?new Intl.DateTimeFormat('vi-VN',{dateStyle:'medium',timeZone:'Asia/Ho_Chi_Minh'}).format(new Date(v+'Z')):'';
async function load(){const scope=context,ticket=++sequence;loading.value=true;error.value='';try{const result=await request('/products/'+props.productId+'/questions/'+props.questionId+'/replies?page='+page.value);if(scope===context&&ticket===sequence)data.value=result;}catch(e){if(scope===context&&ticket===sequence)error.value=e.message;}finally{if(scope===context&&ticket===sequence)loading.value=false;}}
function open(e){expanded.value=e.target.open;if(expanded.value)load();}
async function submit(){if(busy.value)return;const scope=context,path='/account/products/'+props.productId+'/questions/'+props.questionId+'/replies',text=body.value;busy.value=true;submitError.value='';message.value='';try{await request(path,'POST',{body:text},false);if(scope!==context)return;body.value='';message.value='Đã gửi trả lời.';const total=data.value.totalElements||0;page.value=Math.floor(total/10);await load();}catch(e){if(scope===context)submitError.value=e.message;}finally{if(scope===context)busy.value=false;}}
watch([()=>props.productId,()=>props.questionId,()=>user.value?.id],()=>{context++;body.value='';busy.value=false;submitError.value='';message.value='';page.value=0;if(expanded.value)load();});onBeforeUnmount(()=>context++);
</script>
<style scoped>
.question-replies{margin-top:20px}.question-replies summary{color:var(--bm-forest);font-weight:600;cursor:pointer;padding-block:12px;min-height:44px}.question-reply-list{list-style:none;padding:0 0 0 16px;border-left:1px solid var(--bm-border);margin:16px 0}.question-reply-list li{padding-block:16px;border-bottom:1px solid var(--bm-border)}.question-reply-list li:last-child{border:0}.question-reply-list header{display:flex;flex-wrap:wrap;align-items:baseline;gap:8px 12px}.question-replies .discussion-text{margin-bottom:0}.question-replies .discussion-notice{margin:0}@media(max-width:600px){.question-reply-list{padding-left:12px}}
</style>

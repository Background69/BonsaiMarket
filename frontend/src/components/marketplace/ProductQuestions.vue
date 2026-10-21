<template>
  <section class="discussion product-questions" aria-labelledby="questions-title">
    <header><h2 id="questions-title">Hỏi đáp về sản phẩm</h2><p class="muted">Hỏi người bán về sản phẩm. Câu trả lời chính thức đến từ chủ gian hàng.</p></header>
    <ResourceState v-if="loading || error" resource="câu hỏi" :loading="loading" :error="Boolean(error)" @retry="load"/>
    <template v-else>
      <p v-if="!data.content.length" class="discussion-empty">Chưa có câu hỏi. Bạn có thể hỏi thêm về kích thước, chậu hoặc tình trạng sản phẩm.</p>
      <article v-for="q in data.content" :key="q.id" class="discussion-entry">
        <header><strong>{{ q.authorName }}</strong><time :datetime="q.createdAt">{{ date(q.createdAt) }}</time></header><p class="discussion-text">{{ q.body }}</p>
        <div v-if="q.answerBody" class="seller-answer"><strong>Trả lời từ gian hàng · {{ q.answerAuthorName }}</strong><p class="discussion-text">{{ q.answerBody }}</p><time :datetime="q.answeredAt">{{ date(q.answeredAt) }}</time></div>
        <form v-if="q.canAnswer" @submit.prevent="answer(q)"><label :for="'reply-'+q.id">{{ q.answerBody ? 'Cập nhật câu trả lời' : 'Trả lời với tư cách chủ gian hàng' }}</label><textarea :id="'reply-'+q.id" v-model="replies[q.id]" required maxlength="4000" rows="3"/><button class="button button-outline" :disabled="busy">Gửi trả lời</button></form>
        <button v-if="canRemove(q)" class="text-link" type="button" :disabled="busy" @click="remove(q)">{{ isAdmin ? 'Gỡ câu hỏi' : 'Xóa câu hỏi của tôi' }}</button>
      </article>
      <nav v-if="data.totalPages>1" aria-label="Phân trang câu hỏi" class="discussion-pagination"><button class="button button-outline" :disabled="page===0 || busy" @click="page--;load()">Trước</button><span>Trang {{ page+1 }} / {{ data.totalPages }}</span><button class="button button-outline" :disabled="page+1>=data.totalPages || busy" @click="page++;load()">Sau</button></nav>
    </template>
    <p v-if="message" role="status">{{ message }}</p><p v-if="submitError" role="alert">{{ submitError }}</p>
    <form v-if="isAuthenticated" @submit.prevent="ask"><label for="product-question-body">Câu hỏi của bạn</label><textarea id="product-question-body" v-model="body" required maxlength="2000" rows="4"/><p class="muted">Tối đa 2.000 ký tự. Không chia sẻ số điện thoại hoặc thông tin riêng tư.</p><button class="button button-primary" :disabled="busy || !body.trim()">{{ busy ? 'Đang gửi…' : 'Gửi câu hỏi' }}</button></form>
    <p v-else><RouterLink :to="{path:'/login',query:{redirect:route.fullPath}}">Đăng nhập để đặt câu hỏi</RouterLink></p>
  </section>
</template>
<script setup>
import {ref,reactive,watch,onBeforeUnmount} from 'vue';import {useRoute} from 'vue-router';
import {useAuth} from '../../composables/useAuth.js';import {marketplaceRequest as request} from '../../services/marketplace.js';import ResourceState from './ResourceState.vue';import '../../css/discussions.css';
const props=defineProps({productId:{type:[Number,String],required:true}}),route=useRoute(),{user,isAuthenticated,isAdmin}=useAuth();
const data=ref({content:[],totalPages:0}),page=ref(0),loading=ref(false),error=ref(''),busy=ref(false),body=ref(''),replies=reactive({}),message=ref(''),submitError=ref('');let generation=0;
const date=value=>value?new Intl.DateTimeFormat('vi-VN',{dateStyle:'medium',timeZone:'Asia/Ho_Chi_Minh'}).format(new Date(value+'Z')):'';
const canRemove=q=>isAuthenticated.value&&(isAdmin.value||user.value?.id===q.authorId);
async function load(){const ticket=++generation;loading.value=true;error.value='';try{const result=await request('/products/'+props.productId+'/questions?page='+page.value);if(ticket===generation){data.value=result;result.content.forEach(q=>replies[q.id]=q.answerBody||'');}}catch(e){if(ticket===generation)error.value=e.message;}finally{if(ticket===generation)loading.value=false;}}
async function mutate(action,success){if(busy.value)return;busy.value=true;submitError.value='';message.value='';const ticket=generation;try{await action();if(ticket!==generation)return;message.value=success;await load();}catch(e){if(ticket===generation)submitError.value=e.message;}finally{busy.value=false;}}
function ask(){const text=body.value;return mutate(async()=>{await request('/account/products/'+props.productId+'/questions','POST',{body:text});body.value='';page.value=0;},'Đã gửi câu hỏi.');}
function answer(q){return mutate(()=>request('/seller/product-questions/'+q.id+'/answer','PUT',{body:replies[q.id],revision:q.revision}),'Đã lưu câu trả lời.');}
function remove(q){return mutate(()=>request('/account/product-questions/'+q.id+'?revision='+q.revision,'DELETE'),'Đã xóa câu hỏi.');}
watch(()=>[props.productId,user.value?.id],()=>{page.value=0;body.value='';submitError.value='';message.value='';load();},{immediate:true});onBeforeUnmount(()=>generation++);
</script>

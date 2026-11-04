<template>
  <details class="question-replies" @toggle="open">
    <summary>Xem và trả lời câu hỏi</summary>
    <div v-if="expanded">
      <button type="button" class="button button-outline" :disabled="loading || busy" @click="load">Cập nhật phản hồi</button>
      <p v-if="loading" role="status">Đang tải trả lời…</p>
      <div v-else-if="error" role="alert"><p>{{ error }}</p><button class="button button-outline" @click="load">Thử lại</button></div>
      <template v-else>
        <p v-if="!data.content.length" class="discussion-notice">Chưa có trả lời từ thành viên.</p>
        <ol class="question-reply-list"><li v-for="reply in data.content" :key="reply.id">
          <header><strong>{{ reply.authorName }}</strong><span v-if="reply.authorLabel==='SELLER'" class="discussion-meta">Chủ gian hàng</span><span v-else-if="reply.authorLabel==='ADMIN'" class="discussion-meta">Quản trị viên</span><time :datetime="reply.createdAt">{{ date(reply.createdAt) }}</time></header><p class="discussion-text">{{ reply.body }}</p>
          <form v-if="editing?.id===reply.id" @submit.prevent="saveEdit"><label :for="'edit-reply-'+reply.id">Sửa phản hồi của bạn</label><textarea :id="'edit-reply-'+reply.id" v-model="editBody" required rows="3" maxlength="4000" :disabled="busy"/><p v-if="submitError" role="alert">{{ submitError }}</p><div class="management-actions"><button class="button button-primary" :disabled="busy">Lưu phản hồi</button><button type="button" class="button button-outline" :disabled="busy" @click="editing=null">Bỏ chỉnh sửa</button></div></form>
          <div v-else class="management-actions"><button v-if="reply.canEdit" type="button" class="button button-outline" :disabled="busy" @click="startEdit(reply)">Sửa</button><button v-if="reply.canRemove" type="button" class="button button-outline" :disabled="busy" @click="prepareRemove(reply)">{{ reply.canEdit?'Xóa phản hồi':'Gỡ phản hồi vi phạm' }}</button></div>
        </li></ol>
        <nav v-if="data.totalPages>1" class="discussion-pagination" aria-label="Phân trang trả lời"><button class="button button-outline" :disabled="page===0||busy" @click="page--;load()">Trước</button><span>Trang {{ page+1 }} / {{ data.totalPages }}</span><button class="button button-outline" :disabled="page+1>=data.totalPages||busy" @click="page++;load()">Sau</button></nav>
      </template>
      <p v-if="message" role="status">{{ message }}</p><p v-if="submitError" role="alert">{{ submitError }}</p>
      <form v-if="isAuthenticated && !editing" :aria-busy="busy" @submit.prevent="submit"><label :for="'member-reply-'+questionId">Trả lời của bạn</label><textarea :id="'member-reply-'+questionId" v-model="body" required maxlength="4000" rows="3" :disabled="busy"/><p class="discussion-notice">Tối đa 4.000 ký tự. Nội dung hiển thị công khai.</p><button class="button button-outline" :disabled="busy||!body.trim()">{{ busy?'Đang gửi…':'Gửi trả lời' }}</button></form>
      <p v-else><RouterLink :to="{path:'/login',query:{redirect:route.fullPath}}">Đăng nhập để trả lời</RouterLink></p>
    </div>
    <dialog ref="removeDialog" aria-labelledby="remove-reply-title" tabindex="-1" @keydown.tab="trapDialogFocus" @cancel="busy && $event.preventDefault()" @close="opener?.focus()"><h2 id="remove-reply-title">Gỡ phản hồi công khai?</h2><p>Phản hồi sẽ không còn hiển thị. Lịch sử moderation vẫn được lưu.</p><p v-if="submitError" role="alert">{{ submitError }}</p><div class="management-actions"><button class="button button-outline" autofocus :disabled="busy" @click="removeDialog.close()">Giữ phản hồi</button><button class="button button-primary" :disabled="busy" @click="remove">Xác nhận gỡ</button></div></dialog>
  </details>
</template>
<script setup>
import {ref,watch,onBeforeUnmount,nextTick} from 'vue';
import {useRoute} from 'vue-router';
import {useAuth} from '../../composables/useAuth.js';
import {marketplaceRequest as request} from '../../services/marketplace.js';
import {trapDialogFocus} from '../../utils/dialogFocus.js';
const props=defineProps({productId:{type:[Number,String],required:true},questionId:{type:[Number,String],required:true}}),route=useRoute(),{user,isAuthenticated}=useAuth();
const expanded=ref(false),data=ref({content:[],totalPages:0}),page=ref(0),loading=ref(false),error=ref(''),body=ref(''),busy=ref(false),message=ref(''),submitError=ref('');let context=0,sequence=0;
const editing=ref(null),editBody=ref(''),removing=ref(null),removeDialog=ref(null);let opener;
const replyPath=id=>'/account/products/'+props.productId+'/questions/'+props.questionId+'/replies/'+id;
async function startEdit(reply){editing.value={...reply};editBody.value=reply.body;submitError.value='';await nextTick();document.getElementById('edit-reply-'+reply.id)?.focus();}
async function saveEdit(){if(busy.value)return;const scope=context;busy.value=true;submitError.value='';try{await request(replyPath(editing.value.id),'PUT',{body:editBody.value,revision:editing.value.revision},false);if(scope!==context)return;editing.value=null;message.value='Đã sửa phản hồi.';await load();}catch(e){if(scope===context)submitError.value=e.message;}finally{if(scope===context)busy.value=false;}}
function prepareRemove(reply){removing.value=reply;submitError.value='';opener=document.activeElement;removeDialog.value.showModal();}
async function remove(){if(busy.value)return;const scope=context;busy.value=true;submitError.value='';try{await request(replyPath(removing.value.id)+'?revision='+removing.value.revision,'DELETE',undefined,false);if(scope!==context)return;removeDialog.value.close();message.value='Đã gỡ phản hồi.';page.value=0;await load();}catch(e){if(scope===context)submitError.value=e.message;}finally{if(scope===context)busy.value=false;}}
const date=v=>v?new Intl.DateTimeFormat('vi-VN',{dateStyle:'medium',timeZone:'Asia/Ho_Chi_Minh'}).format(new Date(v+'Z')):'';
async function load(){const scope=context,ticket=++sequence;loading.value=true;error.value='';try{const result=await request('/products/'+props.productId+'/questions/'+props.questionId+'/replies?page='+page.value);if(scope===context&&ticket===sequence)data.value=result;}catch(e){if(scope===context&&ticket===sequence)error.value=e.message;}finally{if(scope===context&&ticket===sequence)loading.value=false;}}
function open(e){expanded.value=e.target.open;if(expanded.value)load();}
async function submit(){if(busy.value)return;const scope=context,path='/account/products/'+props.productId+'/questions/'+props.questionId+'/replies',text=body.value;busy.value=true;submitError.value='';message.value='';try{await request(path,'POST',{body:text},false);if(scope!==context)return;body.value='';message.value='Đã gửi trả lời.';const total=data.value.totalElements||0;page.value=Math.floor(total/10);await load();}catch(e){if(scope===context)submitError.value=e.message;}finally{if(scope===context)busy.value=false;}}
watch([()=>props.productId,()=>props.questionId,()=>user.value?.id],()=>{context++;removeDialog.value?.close();editing.value=null;editBody.value='';data.value={content:[],totalPages:0};body.value='';busy.value=false;submitError.value='';message.value='';page.value=0;if(expanded.value)load();});onBeforeUnmount(()=>context++);
</script>
<style scoped>
.question-replies{margin-top:20px}.question-replies summary{color:var(--bm-forest);font-weight:600;cursor:pointer;padding-block:12px;min-height:44px}.question-reply-list{list-style:none;padding:0 0 0 16px;border-left:1px solid var(--bm-border);margin:16px 0}.question-reply-list li{padding-block:16px;border-bottom:1px solid var(--bm-border)}.question-reply-list li:last-child{border:0}.question-reply-list header{display:flex;flex-wrap:wrap;align-items:baseline;gap:8px 12px}.question-replies .discussion-text{margin-bottom:0}.question-replies .discussion-notice{margin:0}@media(max-width:600px){.question-reply-list{padding-left:12px}}
.question-replies dialog{width:min(520px,calc(100% - 32px));padding:24px;max-height:calc(100dvh - 32px);overflow:auto;border:1px solid var(--bm-border);border-radius:var(--bm-radius);background:var(--bm-surface);color:var(--bm-ink)}.question-replies dialog::backdrop{background:var(--bm-scrim)}
</style>

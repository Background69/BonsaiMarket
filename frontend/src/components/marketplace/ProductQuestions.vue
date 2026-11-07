<template>
  <section id="product-questions" class="discussion product-questions" aria-labelledby="questions-title">
    <header><h2 id="questions-title" tabindex="-1">Hỏi đáp về sản phẩm</h2><p class="muted">Hỏi người bán về kích thước, chậu hoặc tình trạng sản phẩm. Phản hồi thành viên được tách khỏi trả lời chính thức của gian hàng.</p></header>
    <button class="button button-outline" type="button" :disabled="loading || busy" @click="load">{{ loading ? 'Đang tải…' : 'Cập nhật câu hỏi' }}</button>
    <ResourceState v-if="loading || error" resource="câu hỏi" :loading="loading" :error="Boolean(error)" :error-message="error" @retry="load"/>
    <template v-else>
      <p v-if="!data.content.length" class="discussion-empty">Chưa có câu hỏi. Bạn có thể hỏi thêm về kích thước, chậu hoặc tình trạng sản phẩm.</p>
      <article v-for="q in data.content" :id="'product-question-'+q.id" :key="q.id" class="discussion-entry" tabindex="-1" :aria-label="'Câu hỏi của '+q.authorName">
        <header><strong>{{ q.authorName }}</strong><time :datetime="q.createdAt">{{ date(q.createdAt) }}</time><span class="discussion-meta">{{ q.answerBody ? 'Đã có trả lời chính thức' : 'Chưa có trả lời chính thức' }}</span></header>
        <form v-if="edits[q.id]" :aria-busy="busy" @submit.prevent="saveQuestion(q)">
          <label :for="'question-edit-'+q.id">Sửa câu hỏi của bạn</label>
          <textarea :id="'question-edit-'+q.id" v-model="edits[q.id].body" required maxlength="2000" rows="4" :disabled="busy" :readonly="!q.canEdit"/>
          <p v-if="!q.canEdit" class="discussion-notice">Câu hỏi đã có phản hồi nên không thể sửa. Nội dung đang soạn được giữ để bạn sao chép.</p>
          <p v-else-if="edits[q.id].revision!==q.revision" class="discussion-notice">Câu hỏi đã thay đổi. Giữ lại nội dung cần thiết, hủy bản sửa rồi mở lại phiên bản mới.</p>
          <div class="question-form-actions"><button class="button button-primary" :disabled="busy || !q.canEdit || edits[q.id].revision!==q.revision || !edits[q.id].body.trim()">{{ busy ? 'Đang lưu…' : 'Lưu câu hỏi' }}</button><button class="button button-outline" type="button" :disabled="busy" @click="cancelEdit(q)">Hủy chỉnh sửa</button></div>
        </form>
        <p v-else class="discussion-text">{{ q.body }}</p>
        <div v-if="q.answerBody" class="seller-answer"><strong>Trả lời chính thức từ gian hàng · {{ q.answerAuthorName }}</strong><p class="discussion-text">{{ q.answerBody }}</p><time :datetime="q.answeredAt">{{ date(q.answeredAt) }}</time></div>
        <form v-if="q.canAnswer" :aria-busy="busy" @submit.prevent="answer(q)">
          <label :for="'reply-'+q.id">{{ q.answerBody ? 'Cập nhật câu trả lời' : 'Trả lời với tư cách chủ gian hàng' }}</label>
          <textarea :id="'reply-'+q.id" v-model="drafts[q.id].body" required maxlength="4000" rows="3" :disabled="busy" :aria-describedby="'reply-help-'+q.id"/>
          <p :id="'reply-help-'+q.id" class="discussion-notice">{{ drafts[q.id].body.length }}/4.000 ký tự. Bản nháp chưa gửi sẽ được giữ khi cập nhật danh sách.</p>
          <p v-if="drafts[q.id].conflict" class="question-draft-conflict" role="status">Câu hỏi hoặc câu trả lời đã thay đổi trên máy chủ. Bản nháp của bạn được giữ nguyên; hãy đối chiếu trước khi gửi.</p>
          <div class="question-form-actions"><button class="button button-outline" :disabled="busy || drafts[q.id].conflict || !drafts[q.id].body.trim()">{{ busy ? 'Đang gửi…' : 'Gửi trả lời' }}</button><button v-if="drafts[q.id].conflict" class="button button-outline" type="button" :disabled="busy" @click="resetAnswerDraft(drafts,q)">Bỏ bản nháp, dùng nội dung máy chủ</button></div>
        </form>
        <ProductQuestionReplies :product-id="productId" :question-id="q.id"/>
        <button v-if="q.canEdit && !edits[q.id]" class="text-link" type="button" :disabled="busy" @click="startEdit(q)">Sửa câu hỏi của tôi</button>
        <button v-if="canRemove(q)" class="text-link" type="button" :disabled="busy" @click="remove(q)">{{ isAdmin ? 'Gỡ câu hỏi' : 'Xóa câu hỏi của tôi' }}</button>
      </article>
      <nav v-if="data.totalPages>1" aria-label="Phân trang câu hỏi" class="discussion-pagination"><button class="button button-outline" :disabled="page===0 || busy || loading" @click="changePage(page-1)">Trước</button><span role="status">Trang {{ page+1 }} / {{ data.totalPages }}</span><button class="button button-outline" :disabled="page+1>=data.totalPages || busy || loading" @click="changePage(page+1)">Sau</button></nav>
    </template>
    <p v-if="message" role="status">{{ message }}</p><p v-if="submitError" ref="submitFeedback" role="alert" tabindex="-1">{{ submitError }}</p>
    <form v-if="isAuthenticated" :aria-busy="busy" @submit.prevent="ask"><label for="product-question-body">Câu hỏi của bạn</label><textarea id="product-question-body" v-model="body" required maxlength="2000" rows="4" :disabled="busy" aria-describedby="product-question-help"/><p id="product-question-help" class="discussion-notice">{{ body.length }}/2.000 ký tự. Không chia sẻ thông tin riêng tư. Bạn chỉ được sửa trước khi có bất kỳ phản hồi nào.</p><button class="button button-primary" :disabled="busy || !body.trim()">{{ busy ? 'Đang gửi…' : 'Gửi câu hỏi' }}</button></form>
    <p v-else><RouterLink :to="{path:'/login',query:{redirect:route.fullPath}}">Đăng nhập để đặt câu hỏi</RouterLink></p>
  </section>
</template>
<script setup>
import {ref,reactive,watch,onBeforeUnmount,nextTick} from 'vue';import {useRoute,useRouter} from 'vue-router';
import {useAuth} from '../../composables/useAuth.js';import {marketplaceRequest as request} from '../../services/marketplace.js';import ResourceState from './ResourceState.vue';import '../../css/discussions.css';
import ProductQuestionReplies from './ProductQuestionReplies.vue';
import {reconcileAnswerDrafts,resetAnswerDraft} from '../../services/questionDrafts.js';
const props=defineProps({productId:{type:[Number,String],required:true}}),route=useRoute(),router=useRouter(),{user,isAuthenticated,isAdmin}=useAuth();
const data=ref({content:[],totalPages:0}),page=ref(0),loading=ref(false),error=ref(''),busy=ref(false),body=ref(''),drafts=reactive({}),edits=reactive({}),message=ref(''),submitError=ref(''),submitFeedback=ref(null);let generation=0,context=0;
const date=value=>value?new Intl.DateTimeFormat('vi-VN',{dateStyle:'medium',timeZone:'Asia/Ho_Chi_Minh'}).format(new Date(value+'Z')):'';
const canRemove=q=>isAuthenticated.value&&(isAdmin.value||user.value?.id===q.authorId);
const queryPage=()=>{const value=Number(route.query.questionsPage||0);return Number.isInteger(value)&&value>=0&&value<=10000?value:0;};
async function focusQuestion(id){await nextTick();const article=document.getElementById('product-question-'+id);if(article){article.focus({preventScroll:true});article.scrollIntoView({block:'center'});}}
async function load(focusPage=false){
  const ticket=++generation;loading.value=true;error.value='';
  const focus=Number(route.query.question),suffix=Number.isSafeInteger(focus)&&focus>0?'&questionId='+focus:'';
  try{
    const result=await request('/products/'+props.productId+'/questions?page='+page.value+suffix);
    if(ticket!==generation)return;
    if(!suffix&&page.value>0&&page.value>=result.totalPages){await changePage(Math.max(0,result.totalPages-1),true);return;}
    reconcileAnswerDrafts(drafts,result.content);data.value=result;page.value=result.page;
  }catch(e){if(ticket===generation)error.value=e.message;}
  finally{if(ticket===generation){loading.value=false;if(!error.value&&suffix)await focusQuestion(focus);else if(!error.value&&focusPage===true){await nextTick();if(ticket===generation)document.getElementById('questions-title')?.focus({preventScroll:true});}}}
}
async function mutate(action,success,after=()=>{}){
  if(busy.value)return;busy.value=true;submitError.value='';message.value='';const ticket=context;
  try{const result=await action();if(ticket!==context)return;await after(result);message.value=success;await load();}
  catch(e){if(ticket===context){submitError.value=e.message;if(e.status===409)await load();await nextTick();submitFeedback.value?.focus();}}
  finally{if(ticket===context)busy.value=false;}
}
function ask(){const text=body.value,id=props.productId;return mutate(()=>request('/account/products/'+id+'/questions','POST',{body:text},false),'Đã gửi câu hỏi.',async saved=>{body.value='';await router.replace({path:route.path,query:{...route.query,question:saved.id,questionsPage:undefined},hash:'#product-question-'+saved.id});});}
function answer(q){const draft=drafts[q.id],payload={body:draft.body,revision:draft.revision};return mutate(()=>request('/seller/product-questions/'+q.id+'/answer','PUT',payload,false),'Đã lưu câu trả lời.',saved=>resetAnswerDraft(drafts,saved));}
function remove(q){if(!window.confirm('Xác nhận xóa câu hỏi này?'))return;return mutate(()=>request('/account/product-questions/'+q.id+'?revision='+q.revision,'DELETE'),'Đã xóa câu hỏi.',async()=>{delete drafts[q.id];delete edits[q.id];if(String(route.query.question)===String(q.id))await changePage(page.value,true);});}
async function startEdit(q){edits[q.id]={body:q.body,revision:q.revision};await nextTick();document.getElementById('question-edit-'+q.id)?.focus();}
async function cancelEdit(q){delete edits[q.id];await focusQuestion(q.id);}
function saveQuestion(q){const payload={...edits[q.id]};return mutate(()=>request('/account/product-questions/'+q.id,'PUT',payload,false),'Đã cập nhật câu hỏi.',()=>{delete edits[q.id];});}
function changePage(value,replace=false){if(!Number.isSafeInteger(value)||value<0||value>10000||busy.value&&!replace)return;return router[replace?'replace':'push']({path:route.path,query:{...route.query,question:undefined,questionsPage:value||undefined},hash:'#product-questions'});}
watch([()=>props.productId,()=>user.value?.id],()=>{context++;busy.value=false;page.value=queryPage();body.value='';submitError.value='';message.value='';for(const map of [drafts,edits])Object.keys(map).forEach(key=>delete map[key]);load();},{immediate:true});
watch([()=>route.query.question,()=>route.query.questionsPage],()=>{page.value=queryPage();load(true);});
onBeforeUnmount(()=>{generation++;context++;});
</script>
<style scoped>
.product-questions>button{margin-block:8px 16px}.product-questions .discussion-entry{scroll-margin-top:160px}.product-questions .discussion-entry:focus{outline:2px solid var(--bm-forest);outline-offset:4px}.question-form-actions{display:flex;flex-wrap:wrap;gap:12px}.question-draft-conflict{color:var(--bm-warning);font-size:var(--bm-type-label);line-height:1.6}.product-questions .text-link+.text-link{margin-left:20px}
</style>

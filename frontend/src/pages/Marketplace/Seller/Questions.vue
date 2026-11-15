<template>
  <SellerLayout>
    <header class="management-page"><h1 id="seller-questions-title" tabindex="-1">Hỏi đáp sản phẩm</h1><p>Câu hỏi công khai về các sản phẩm đang bán của gian hàng bạn. Ưu tiên câu chưa có trả lời chính thức.</p></header>
    <div class="management-page seller-questions">
      <form class="management-toolbar" @submit.prevent="apply">
        <label for="seller-question-filter">Trả lời chính thức</label>
        <select id="seller-question-filter" v-model="answer" :disabled="loading">
          <option value="UNANSWERED">Chưa trả lời</option><option value="ANSWERED">Đã trả lời</option><option value="ALL">Tất cả</option>
        </select>
        <button class="button button-primary" :disabled="loading">Lọc câu hỏi</button>
        <button class="button button-outline" type="button" :disabled="loading" @click="load">Cập nhật danh sách</button>
      </form>
      <p v-if="!loading && !error" class="muted" role="status">{{ data.totalElements }} câu hỏi · {{ appliedAnswerLabel }}. Mở câu hỏi để trả lời với tư cách gian hàng.</p>
      <ManagementTable :columns="columns" :rows="data.content" :loading="loading" :error="error" caption="Câu hỏi sản phẩm thuộc gian hàng của bạn"
                       empty-title="Chưa có câu hỏi phù hợp. Câu hỏi mới sẽ xuất hiện tại đây khi sản phẩm còn được công khai." @retry="load">
        <template #product="{row}"><RouterLink :to="destination(row)">{{ row.productName }}</RouterLink></template>
        <template #question="{row}"><p class="seller-question-body">{{ row.body }}</p><small>{{ row.authorName }}</small></template>
        <template #status="{row}"><span class="management-status">{{ row.answered ? 'Đã trả lời chính thức' : 'Chưa có trả lời chính thức' }}</span></template>
        <template #createdAt="{row}"><time :datetime="row.createdAt+'Z'">{{ date(row.createdAt) }}</time></template>
        <template #actions="{row}"><RouterLink class="button button-outline" :to="destination(row)">{{ row.answered ? 'Xem câu hỏi' : 'Mở để trả lời' }}</RouterLink></template>
      </ManagementTable>
      <ManagementPagination v-if="!error" :loading="loading" :busy="loading" :count="data.content.length" :page="data.page" :size="10"
                            :total-elements="data.totalElements" :total-pages="data.totalPages" noun="câu hỏi" focus-target="#seller-questions-title"/>
    </div>
  </SellerLayout>
</template>
<script setup>
import {computed,ref,watch} from 'vue';
import {useRoute,useRouter} from 'vue-router';
import SellerLayout from './SellerLayout.vue';
import ManagementTable from '../../../components/marketplace/ManagementTable.vue';
import ManagementPagination from '../../../components/marketplace/ManagementPagination.vue';
import {useServerList} from '../../../composables/useServerList.js';
const route=useRoute(),router=useRouter(),answer=ref('UNANSWERED');
const {data,loading,error,load}=useServerList('/seller/product-questions',q=>({answer:q.answer||'UNANSWERED'}));
const appliedAnswerLabel=computed(()=>({UNANSWERED:'Chưa trả lời',ANSWERED:'Đã trả lời',ALL:'Tất cả trạng thái'}[route.query.answer||'UNANSWERED']||'Trạng thái cần kiểm tra'));
const columns=[{key:'product',label:'Sản phẩm'},{key:'question',label:'Câu hỏi'},{key:'status',label:'Trạng thái'},{key:'createdAt',label:'Ngày hỏi'},{key:'actions',label:'Thao tác'}];
const destination=row=>({path:'/products/'+row.productId,query:{question:row.id},hash:'#product-question-'+row.id});
const date=value=>new Intl.DateTimeFormat('vi-VN',{dateStyle:'medium',timeZone:'Asia/Ho_Chi_Minh'}).format(new Date(value+'Z'));
watch(()=>route.query.answer,value=>{answer.value=value||'UNANSWERED';},{immediate:true});
function apply(){router.push({path:route.path,query:{...route.query,answer:answer.value,page:0}});}
</script>
<style scoped>
.seller-question-body{margin:0 0 8px;white-space:pre-wrap;line-height:1.6;overflow-wrap:anywhere}.seller-questions small{color:var(--bm-muted)}.seller-questions select{min-height:44px;max-width:100%;font:inherit;padding:8px;border:1px solid var(--bm-control-border);border-radius:var(--bm-radius-control);background:var(--bm-surface);color:var(--bm-ink)}
</style>

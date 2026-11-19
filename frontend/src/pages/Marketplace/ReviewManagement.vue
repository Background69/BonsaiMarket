<template>
  <component :is="admin ? AdminLayout : SellerLayout">
    <header class="management-page"><h1 id="review-management-title" tabindex="-1">Đánh giá sản phẩm</h1><p>Giữ nguyên nội dung và số sao của người mua. Cảm xúc là gợi ý đọc toàn bộ đánh giá, không phải kết luận về người bán hay từng khía cạnh.</p></header>
    <div class="management-page review-management">
      <form class="management-toolbar" @submit.prevent="apply">
        <label for="review-sentiment">Cảm xúc đánh giá</label>
        <select id="review-sentiment" v-model="sentiment" :disabled="loading">
          <option v-for="(label,value) in filters" :key="value" :value="value">{{ label }}</option>
        </select>
        <button class="button button-primary" :disabled="loading">Lọc đánh giá</button>
        <button type="button" class="button button-outline" :disabled="loading" @click="load">Cập nhật danh sách</button>
      </form>
      <p v-if="!loading && !error" role="status">{{ data.totalElements }} đánh giá · {{ filters[route.query.sentiment || 'ALL'] || 'Bộ lọc cần kiểm tra' }}</p>
      <ManagementTable :columns="columns" :rows="data.content" :loading="loading" :error="error"
                       caption="Đánh giá sản phẩm trong phạm vi quản lý" empty-title="Chưa có đánh giá phù hợp." @retry="load">
        <template #product="{row}"><RouterLink :to="'/products/'+row.productId+'#product-reviews'">{{ row.productName }}</RouterLink><small v-if="admin">{{ row.storeName }}</small></template>
        <template #review="{row}"><p class="review-management-comment">{{ row.comment }}</p><small>{{ row.rating }}/5 sao · {{ date(row.createdAt) }}</small></template>
        <template #sentiment="{row}">
          <span class="management-status">{{ row.sentimentStatus === 'ANALYZED' ? filters[row.sentimentLabel] : states[row.sentimentStatus] || 'Không xác định' }}</span>
          <small v-if="row.demoOnly">Bản demo · chưa đánh giá độc lập</small>
          <details v-if="row.modelVersion"><summary>Thông tin phân tích</summary><p class="review-management-version">Mô hình: {{ row.modelVersion }}<br>Phiên bản nội dung: {{ row.contentVersion }}</p></details>
        </template>
      </ManagementTable>
      <ManagementPagination v-if="!error" :loading="loading" :busy="loading" :count="data.content.length" :page="data.page" :size="10"
                            :total-elements="data.totalElements" :total-pages="data.totalPages" noun="đánh giá" focus-target="#review-management-title"/>
    </div>
  </component>
</template>
<script setup>
import {ref,watch} from 'vue';
import {useRoute,useRouter} from 'vue-router';
import AdminLayout from '../../layouts/AdminLayout.vue';
import SellerLayout from './Seller/SellerLayout.vue';
import ManagementTable from '../../components/marketplace/ManagementTable.vue';
import ManagementPagination from '../../components/marketplace/ManagementPagination.vue';
import {useServerList} from '../../composables/useServerList.js';
const route=useRoute(),router=useRouter(),admin=route.path.startsWith('/admin/'),sentiment=ref('ALL');
const filters={ALL:'Tất cả',positive:'Tích cực',neutral:'Trung tính',negative:'Tiêu cực',mixed:'Vừa khen vừa chê',UNANALYZED:'Chưa phân tích',PENDING:'Đang chờ',PROCESSING:'Đang phân tích',FAILED:'Lỗi / không xác định'};
const states={UNANALYZED:'Chưa phân tích',PENDING:'Đang chờ',PROCESSING:'Đang phân tích',FAILED:'Lỗi / không xác định'};
const columns=[{key:'product',label:'Sản phẩm'},{key:'review',label:'Đánh giá của người mua'},{key:'sentiment',label:'Cảm xúc tổng thể'}];
const {data,loading,error,load}=useServerList(admin?'/admin/reviews':'/seller/reviews',q=>({sentiment:q.sentiment||'ALL'}));
watch(()=>route.query.sentiment,value=>{sentiment.value=value||'ALL';},{immediate:true});
function apply(){router.push({path:route.path,query:{...route.query,sentiment:sentiment.value,page:0}});}
const date=value=>value?new Intl.DateTimeFormat('vi-VN',{dateStyle:'medium',timeZone:'Asia/Ho_Chi_Minh'}).format(new Date(value+'Z')):'Chưa có ngày';
</script>
<style scoped>
.review-management select{min-height:44px;max-width:100%;padding:8px;font:inherit;color:var(--bm-ink);background:var(--bm-surface);border:1px solid var(--bm-control-border);border-radius:var(--bm-radius-control)}
.review-management-comment{white-space:pre-wrap;overflow-wrap:anywhere;line-height:1.6;margin:0 0 8px;max-width:65ch}
.review-management small{display:block;margin-top:8px;color:var(--bm-muted)}
.review-management details{margin-top:12px}.review-management summary{cursor:pointer;min-height:24px}
.review-management-version{overflow-wrap:anywhere;max-width:40ch;font-size:var(--bm-type-label);line-height:1.6}
</style>

<template>
  <AdminLayout><div class="management-page">
    <header><h1>{{ isApplication?'Xét duyệt gian hàng':'Báo cáo vi phạm' }}</h1><p>{{ isApplication?'Kiểm tra hồ sơ và ghi nhận quyết định xét duyệt.':'Tiếp nhận báo cáo, kiểm tra nội dung và ghi nhận kết quả xử lý.' }}</p></header>
    <form class="management-toolbar" @submit.prevent><label>Trạng thái<select :value="status" :disabled="busy" @change="filter($event.target.value)"><option v-for="s in statuses" :key="s" :value="s">{{ labels[s] }}</option></select></label><button type="button" class="button button-outline" :disabled="loading||busy" @click="refresh">Tải lại danh sách</button></form>
    <FormFeedback prefix="case" :error="error" :fields="fields" :success="success"/>
    <ManagementTable :caption="isApplication?'Hồ sơ đăng ký gian hàng':'Báo cáo vi phạm'" :columns="columns" :rows="data.content" :loading="loading" :error="listError" :empty-title="isApplication?'Không có hồ sơ ở trạng thái này.':'Không có báo cáo ở trạng thái này.'" @retry="load">
      <template #target="{row}">{{ isApplication?row.name:(row.targetType==='PRODUCT'?'Sản phẩm #':'Gian hàng #')+row.targetId }}<small>#{{ row.id }}</small></template>
      <template #context="{row}">{{ isApplication?row.applicantName:reasons[row.reason] || row.reason }}</template>
      <template #submitted="{row}"><time :datetime="row.submittedAt || row.createdAt">{{ formatTime(row.submittedAt || row.createdAt) }}</time></template>
      <template #status="{row}"><span class="management-status">{{ labels[row.status] }}</span></template>
      <template #actions="{row}"><button class="button button-outline" :disabled="busy" :aria-label="'Xem chi tiết #'+row.id" @click="select(row,$event)">Xem chi tiết</button></template>
    </ManagementTable>
    <ManagementPagination v-if="!listError" :page="data.page" :size="data.size" :total-pages="data.totalPages" :total-elements="data.totalElements" :count="data.content.length" :noun="isApplication?'hồ sơ':'báo cáo'" :loading="loading" :busy="loading||busy"/>
    <section v-if="selected" class="management-secondary" aria-labelledby="case-heading">
      <h2 id="case-heading" tabindex="-1">{{ isApplication?'Hồ sơ':'Báo cáo' }} #{{ selected.id }}</h2>
      <dl class="management-detail-list">
        <template v-if="isApplication"><dt>Người đăng ký</dt><dd>{{ selected.applicantName }}</dd><dt>Gian hàng</dt><dd>{{ selected.name }}</dd><dt>Giới thiệu</dt><dd>{{ selected.description || 'Chưa bổ sung' }}</dd><dt>Liên hệ công khai</dt><dd>{{ selected.phone || 'Chưa bổ sung' }}</dd><dt>Khu vực công khai</dt><dd>{{ selected.address || 'Chưa bổ sung' }}</dd></template>
        <template v-else><dt>Đối tượng</dt><dd><RouterLink :to="(selected.targetType==='PRODUCT'?'/products/':'/stores/')+selected.targetId">{{ selected.targetType==='PRODUCT'?'Sản phẩm':'Gian hàng' }} #{{ selected.targetId }}</RouterLink></dd><dt>Lý do</dt><dd>{{ reasons[selected.reason] }}</dd><dt>Mô tả</dt><dd>{{ selected.description || 'Không bổ sung' }}</dd></template>
        <dt>Trạng thái</dt><dd>{{ labels[selected.status] }}</dd><dt>Gửi lúc</dt><dd><time :datetime="selected.submittedAt || selected.createdAt">{{ selected.submittedAt || selected.createdAt }}</time></dd><dt v-if="selected.processedAt">Xử lý lúc</dt><dd v-if="selected.processedAt"><time :datetime="selected.processedAt">{{ selected.processedAt }}</time></dd><dt>Kết quả / lý do</dt><dd>{{ (isApplication?selected.reason:selected.resolution)||'Chưa có' }}</dd>
      </dl>
      <section v-if="!isApplication" aria-labelledby="report-history-heading" class="management-history">
        <h3 id="report-history-heading">Lịch sử xử lý</h3>
        <p v-if="historyLoading" role="status">Đang tải lịch sử…</p>
        <div v-else-if="historyError" role="alert"><p>{{ historyError }}</p><button class="button button-outline" :disabled="historyLoading" @click="loadHistory">Tải lại lịch sử</button></div>
        <ol v-else-if="history.length"><li v-for="entry in history" :key="entry.id"><strong>{{ labels[entry.action.replace('REPORT_','')] || entry.action }}</strong><time :datetime="entry.createdAt">{{ formatTime(entry.createdAt) }}</time><p>{{ entry.reason }}</p></li></ol>
        <p v-else class="muted">Chưa có quyết định xử lý.</p>
      </section>
      <form v-if="actions.length" class="management-form" :aria-busy="busy" @submit.prevent="confirmed?decide():confirmed=true">
        <label class="management-field">Quyết định<select v-model="draft.status" required :disabled="busy||conflict" @change="confirmed=false"><option value="" disabled>Chọn thao tác</option><option v-for="a in actions" :key="a" :value="a">{{ labels[a] }}</option></select></label>
        <ManagedField id="case-reason" v-model="draft.reason" :label="isApplication?'Lý do (bắt buộc khi từ chối)':'Ghi chú xử lý'" :max="1000" multiline :required="!isApplication||draft.status==='REJECTED'" :disabled="busy" :error="fields.reason"/>
        <p v-if="confirmed" class="management-confirm">Xác nhận chuyển {{ isApplication?'hồ sơ':'báo cáo' }} #{{ selected.id }} sang “{{ labels[draft.status] }}”?</p>
        <div class="management-actions"><button class="button button-primary" :disabled="busy||conflict">{{ busy?'Đang xử lý…':confirmed?'Xác nhận quyết định':'Kiểm tra quyết định' }}</button><button type="button" class="button button-outline" :disabled="busy" @click="close">Đóng</button></div>
        <p v-if="conflict">Dữ liệu đã thay đổi. Đóng chi tiết và tải lại danh sách để kiểm tra quyết định mới.</p>
      </form><button v-else class="button button-outline" @click="close">Đóng chi tiết</button>
    </section>
  </div></AdminLayout>
</template>
<script setup>
import {computed,ref,nextTick,watch,onBeforeUnmount} from 'vue';import {useRoute,useRouter} from 'vue-router';import AdminLayout from '../../layouts/AdminLayout.vue';import ManagedField from '../../components/marketplace/ManagedField.vue';import FormFeedback from '../../components/marketplace/FormFeedback.vue';import ManagementTable from '../../components/marketplace/ManagementTable.vue';import ManagementPagination from '../../components/marketplace/ManagementPagination.vue';import {useManagedForm} from '../../composables/useManagedForm.js';import {useServerList} from '../../composables/useServerList.js';import {marketplaceRequest as request} from '../../services/marketplace.js';
const props=defineProps({kind:{type:String,required:true}}),isApplication=props.kind==='applications',endpoint=isApplication?'/admin/store-applications':'/admin/reports',route=useRoute(),router=useRouter(),selected=ref(null),confirmed=ref(false),conflict=ref(false);
let opener,historyRevision=0;
const history=ref([]),historyLoading=ref(false),historyError=ref('');
async function loadHistory(){if(isApplication||!selected.value)return;const id=selected.value.id,revision=++historyRevision;historyLoading.value=true;historyError.value='';try{const result=await request('/admin/reports/'+id+'/history');if(revision===historyRevision&&selected.value?.id===id)history.value=result;}catch(e){if(revision===historyRevision)historyError.value=e.message;}finally{if(revision===historyRevision)historyLoading.value=false;}}
onBeforeUnmount(()=>historyRevision++);
const columns=[{key:'target',label:isApplication?'Gian hàng':'Đối tượng'},{key:'context',label:isApplication?'Người đăng ký':'Lý do'},{key:'submitted',label:'Ngày gửi'},{key:'status',label:'Trạng thái'},{key:'actions',label:'Thao tác'}];
const formatTime=value=>value?new Intl.DateTimeFormat('vi-VN',{dateStyle:'short',timeStyle:'short'}).format(new Date(value+'Z')):'—';
const labels={PENDING:'Chờ duyệt',APPROVED:'Đã duyệt',REJECTED:'Từ chối',OPEN:'Mới gửi',IN_REVIEW:'Đang xem xét',RESOLVED:'Đã xử lý',DISMISSED:'Không đủ cơ sở'},reasons={MISLEADING:'Thông tin sai lệch',PROHIBITED:'Nội dung không phù hợp',FRAUD:'Dấu hiệu lừa đảo',OTHER:'Lý do khác'};
const statuses=isApplication?['PENDING','APPROVED','REJECTED']:['OPEN','IN_REVIEW','RESOLVED','DISMISSED'],status=computed(()=>route.query.status||statuses[0]);
const {draft,busy,dirty,error,success,fields,reset,submit}=useManagedForm({status:'',reason:'',revision:0},'case');const {data,loading,error:listError,load}=useServerList(endpoint,q=>({status:q.status||statuses[0]}));
const actions=computed(()=>!selected.value?[]:isApplication?(selected.value.status==='PENDING'?['APPROVED','REJECTED']:[]):selected.value.status==='OPEN'?['IN_REVIEW']:selected.value.status==='IN_REVIEW'?['RESOLVED','DISMISSED']:[]);
async function close(){if(dirty.value&&!window.confirm('Bỏ quyết định chưa gửi?'))return;reset({status:'',reason:'',revision:0});historyRevision++;history.value=[];selected.value=null;confirmed.value=false;conflict.value=false;await nextTick();opener?.isConnected&&opener.focus();}
async function select(row,event){if(dirty.value&&!window.confirm('Bỏ quyết định chưa gửi?'))return;opener=event?.currentTarget;selected.value=row;reset({status:'',reason:'',revision:row.revision});confirmed.value=false;conflict.value=false;success.value='';await nextTick();document.getElementById('case-heading')?.focus();history.value=[];loadHistory();}
async function refresh(){if(dirty.value&&!window.confirm('Bỏ quyết định chưa gửi và tải lại?'))return;reset({status:'',reason:'',revision:0});selected.value=null;await load();}
function filter(value){router.push({path:route.path,query:{status:value}});}
watch(()=>[route.query.status,route.query.page],()=>{reset({status:'',reason:'',revision:0});selected.value=null;confirmed.value=false;conflict.value=false;});
function decide(){submit(async()=>{try{const updated=await request(endpoint+'/'+selected.value.id,'PATCH',{...draft});reset({status:'',reason:'',revision:updated.revision});selected.value=updated;confirmed.value=false;success.value='Đã ghi nhận quyết định.';await load();await loadHistory();}catch(e){conflict.value=e.status===409;throw e;}});}
</script>

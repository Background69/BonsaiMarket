<template>
  <MarketplaceLayout><main class="market-container market-page management-page"><header><h1>Mở gian hàng BonsaiMarket</h1><p>Gửi thông tin gian hàng để quản trị viên xét duyệt. Bạn vẫn có thể khám phá sản phẩm trong lúc chờ.</p></header>
    <p v-if="loading" role="status">Đang tải trạng thái đăng ký…</p><FormFeedback prefix="application" :error="error" :fields="fields" :success="success"/>
    <button v-if="error&&!loadFailed" class="button button-outline" :disabled="busy" @click="refresh">Tải lại trạng thái mới</button>
    <button v-if="loadFailed" class="button button-outline" @click="load">Tải lại trạng thái</button>
    <template v-if="!loading&&!loadFailed">
      <section v-if="application?.status==='APPROVED'"><h2>Gian hàng đã được duyệt</h2><p>{{ application.name }}</p><div class="management-actions"><RouterLink class="button button-primary" to="/seller">Vào kênh người bán</RouterLink><RouterLink v-if="application.storeId" class="text-link" :to="'/stores/'+application.storeId">Xem gian hàng</RouterLink></div></section>
      <section v-else-if="application?.status==='PENDING'"><h2>Đang chờ xét duyệt</h2><p>Hồ sơ đã được ghi nhận. Bạn không cần gửi lại.</p><dl class="management-detail-list"><dt>Tên gian hàng</dt><dd>{{ application.name }}</dd><dt>Giới thiệu</dt><dd>{{ application.description || 'Chưa bổ sung' }}</dd><dt>Liên hệ công khai</dt><dd>{{ application.phone || 'Chưa bổ sung' }}</dd><dt>Khu vực công khai</dt><dd>{{ application.address || 'Chưa bổ sung' }}</dd></dl><button class="button button-outline" :disabled="busy" @click="load">Kiểm tra trạng thái</button></section>
      <template v-else><div v-if="application?.status==='REJECTED'" class="management-secondary"><h2>Hồ sơ chưa được chấp thuận</h2><p>Lý do: {{ application.reason }}</p><p>Chỉnh sửa thông tin bên dưới và gửi lại để được xét duyệt.</p></div>
        <form class="management-form" :aria-busy="busy" @submit.prevent="save"><StoreFields prefix="application" :draft="draft" :fields="fields" :busy="busy"/><p class="muted">Tên, giới thiệu, số điện thoại và khu vực bạn nhập sẽ trở thành thông tin công khai khi được duyệt.</p><button class="button button-primary" :disabled="busy">{{ busy?'Đang gửi…':application?'Gửi lại hồ sơ':'Gửi đăng ký gian hàng' }}</button></form>
      </template>
    </template>
  </main></MarketplaceLayout>
</template>
<script setup>
import {ref,onMounted} from 'vue';import MarketplaceLayout from '../../layouts/MarketplaceLayout.vue';
import StoreFields from '../../components/marketplace/StoreFields.vue';import FormFeedback from '../../components/marketplace/FormFeedback.vue';
import {useManagedForm} from '../../composables/useManagedForm.js';import {restoreSession} from '../../composables/useAuth.js';import {marketplaceRequest as request} from '../../services/marketplace.js';
const application=ref(null),loading=ref(true),loadFailed=ref(false);
const {draft,busy,dirty,error,success,fields,reset,fail,submit}=useManagedForm({name:'',description:'',phone:'',address:'',revision:0},'application');
function values(a){return {name:a?.name||'',description:a?.description||'',phone:a?.phone||'',address:a?.address||'',revision:a?.revision||0};}
async function load(){loading.value=true;loadFailed.value=false;try{application.value=await request('/account/seller-application');reset(values(application.value));if(application.value?.status==='APPROVED')await restoreSession(true);}catch(e){loadFailed.value=true;await fail(e);}finally{loading.value=false;}}
function refresh(){if(!dirty.value||window.confirm('Bỏ bản nháp và tải trạng thái mới?'))load();}
function save(){submit(async()=>{application.value=await request('/account/seller-application','POST',{...draft});reset(values(application.value));success.value='Đã gửi hồ sơ đăng ký.';});}
onMounted(load);
</script>

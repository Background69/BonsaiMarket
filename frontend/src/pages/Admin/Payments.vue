<template>
  <AdminLayout>
    <AdminPageHeader title="Giao dịch & thanh toán"
                     description="Hồ sơ thanh toán đơn hàng và các lần giao dịch đã được backend ghi nhận.">
      <RouterLink class="button button-primary" to="/admin/refunds">Xem hồ sơ hoàn tiền</RouterLink>
    </AdminPageHeader>
    <div v-if="summary" class="admin-stat-grid">
      <AdminStatCard label="Hồ sơ thanh toán" :value="summary.total" icon="wallet"
                     note="Toàn sàn · hồ sơ và lần giao dịch"/>
      <AdminStatCard label="Hồ sơ COD" :value="summary.cod" icon="truck" note="Không suy ra đã thu tiền"/>
      <AdminStatCard label="Lần giao dịch VNPAY" :value="summary.online" icon="chart"
                     note="Theo dữ liệu giao dịch đã lưu"/>
    </div>
    <p v-if="summaryError" class="management-error" role="alert">{{ summaryError }}
      <button class="button button-outline" @click="loadSummary">Tải lại thống kê</button>
    </p>
    <p class="admin-note">Trạng thái thanh toán, thu COD và đối soát là các dữ liệu riêng. Phê duyệt hoàn tiền không xác
      nhận tiền đã được chuyển.</p>
    <form class="management-toolbar" @submit.prevent="apply"><label>Tìm hồ sơ<input v-model="search" type="search"
                                                                                    maxlength="100"
                                                                                    placeholder="Mã đơn hoặc người mua"/></label><label>Phương
      thức<select v-model="method">
        <option value="">Tất cả</option>
        <option value="COD">Thanh toán khi nhận hàng</option>
        <option value="VNPAY">VNPAY</option>
      </select></label>
      <button class="button button-primary" :disabled="loading">Lọc hồ sơ</button>
    </form>
    <ManagementTable caption="Hồ sơ thanh toán và lần giao dịch" :columns="columns" :rows="data.content"
                     :loading="loading" :error="error"
                     empty-title="Chưa có hồ sơ thanh toán phù hợp. Dữ liệu xuất hiện khi có đơn hoặc giao dịch hợp lệ."
                     @retry="load">
      <template #code="{row}">
        <RouterLink :to="'/admin/orders/'+row.orderId">{{ row.code }}</RouterLink>
        <small>{{ row.store }}</small></template>
      <template #amount="{row}">{{ money(row.amount) }}</template>
      <template #method="{row}">{{ paymentMethodLabel(row.method) }}</template>
      <template #status="{row}"><span class="admin-badge">{{ paymentLabel(row.status) }}</span><small
          v-if="row.collection">Thu tiền: {{ collectionLabel(row.collection) }}</small><small v-if="row.settlement">Đối
        soát: {{ settlementLabel(row.settlement) }}</small></template>
      <template #createdAt="{row}">{{ dateTime(row.createdAt) }}</template>
      <template #actions="{row}">
        <RouterLink class="button button-outline" :to="'/admin/orders/'+row.orderId">Chi tiết đơn</RouterLink>
      </template>
    </ManagementTable>
    <ManagementPagination v-if="!error" v-bind="data" :count="data.content.length" :busy="loading"
                          noun="hồ sơ thanh toán"/>
  </AdminLayout>
</template>
<script setup>
import {ref, watch} from 'vue';
import AdminStatCard from '../../components/admin/AdminStatCard.vue';
import {useAdminResource} from '../../composables/useAdminResource.js';

const {
  data: summary,
  error: summaryError,
  load: loadSummary
} = useAdminResource(() => '/admin/payment-records/summary');
import {useRoute, useRouter} from 'vue-router';
import AdminLayout from '../../layouts/AdminLayout.vue';
import AdminPageHeader from '../../components/admin/AdminPageHeader.vue';
import ManagementTable from '../../components/marketplace/ManagementTable.vue';
import ManagementPagination from '../../components/marketplace/ManagementPagination.vue';
import {useServerList} from '../../composables/useServerList.js';
import {money, dateTime} from '../../services/adminPresentation.js';
import {paymentMethodLabel} from '../../services/presentation.js';

const route = useRoute(), router = useRouter(), search = ref(''), method = ref(''), {
  data,
  loading,
  error,
  load
} = useServerList('/admin/payment-records', q => ({search: q.search || '', method: q.method || ''}));
const columns = [{key: 'code', label: 'Đơn hàng'}, {key: 'buyer', label: 'Người mua'}, {
  key: 'amount',
  label: 'Giá trị',
  numeric: true
}, {key: 'method', label: 'Phương thức'}, {key: 'status', label: 'Trạng thái'}, {
  key: 'createdAt',
  label: 'Ngày ghi nhận'
}, {key: 'actions', label: 'Thao tác'}];
const paymentLabel = s => ({
  PENDING: 'Chờ thanh toán',
  SUCCESS: 'Giao dịch được ghi nhận thành công',
  FAILED: 'Giao dịch thất bại',
  AWAITING_COLLECTION: 'Chờ thu tiền',
  REVIEW_REQUIRED: 'Cần đối soát',
  CANCELLED: 'Đã hủy',
  EXPIRED: 'Đã hết hạn'
}[s] || 'Cần kiểm tra trạng thái');
const collectionLabel = s => ({
  REQUESTED: 'Đã yêu cầu thu',
  NOT_REQUESTED: 'Chưa yêu cầu',
  UNKNOWN: 'Chưa rõ kết quả',
  REPORTED: 'Có báo cáo cần đối chiếu'
}[s] || 'Cần đối chiếu');
const settlementLabel = s => ({
  UNKNOWN: 'Chưa xác minh',
  REVIEW_REQUIRED: 'Cần đối soát',
  REPORTED: 'Có báo cáo cần đối chiếu'
}[s] || 'Cần kiểm tra');
watch(() => route.query, q => {
  search.value = q.search || '';
  method.value = q.method || '';
}, {immediate: true});

function apply() {
  router.push({path: route.path, query: {search: search.value, method: method.value}});
}
</script>

<template>
  <AdminLayout>
    <AdminPageHeader title="Hồ sơ hoàn tiền" description="Xét duyệt, theo dõi và đối chiếu kết quả xử lý nội bộ.">
      <RouterLink class="button button-outline" to="/admin/transactions">Hồ sơ thanh toán</RouterLink>
    </AdminPageHeader>
    <p class="admin-environment-note">Phê duyệt của Admin không đồng nghĩa đơn vị thanh toán đã chuyển tiền. Không có
      thao tác xác nhận hoàn tiền thành công khi chưa có contract bằng chứng.</p>
    <ManagementTable caption="Danh sách hồ sơ hoàn tiền" :columns="columns" :rows="data.content" :loading="loading"
                     :error="error"
                     empty-title="Chưa có hồ sơ hoàn tiền. Hồ sơ mới xuất hiện sau khi yêu cầu hợp lệ được ghi nhận."
                     @retry="load">
      <template #orderId="{row}">
        <RouterLink :to="'/admin/orders/'+row.orderId">Đơn #{{ row.orderId }}</RouterLink>
      </template>
      <template #amount="{row}">{{ money(row.amount) }}</template>
      <template #method="{row}">{{ paymentMethodLabel(row.method) }}</template>
      <template #status="{row}"><span class="admin-badge">{{ refundStatuses[row.status] || 'Cần đối chiếu' }}</span>
      </template>
      <template #updatedAt="{row}">{{ dateTime(row.updatedAt) }}</template>
      <template #actions="{row}">
        <RouterLink class="button button-outline" :to="'/admin/refunds/'+row.orderId">Xem hồ sơ</RouterLink>
      </template>
    </ManagementTable>
    <ManagementPagination v-if="!error" v-bind="data" :count="data.content.length" :busy="loading"
                          noun="hồ sơ hoàn tiền"/>
  </AdminLayout>
</template>
<script setup>
import AdminLayout from '../../layouts/AdminLayout.vue';
import AdminPageHeader from '../../components/admin/AdminPageHeader.vue';
import ManagementTable from '../../components/marketplace/ManagementTable.vue';
import ManagementPagination from '../../components/marketplace/ManagementPagination.vue';
import {useServerList} from '../../composables/useServerList.js';
import {refundStatuses, money, dateTime} from '../../services/adminPresentation.js';
import {paymentMethodLabel} from '../../services/presentation.js';

const {data, loading, error, load} = useServerList('/admin/refunds');
const columns = [{key: 'orderId', label: 'Đơn hàng'}, {
  key: 'amount',
  label: 'Giá trị đề nghị',
  numeric: true
}, {key: 'method', label: 'Phương thức'}, {key: 'status', label: 'Tiến độ'}, {
  key: 'updatedAt',
  label: 'Cập nhật'
}, {key: 'actions', label: 'Thao tác'}];
</script>

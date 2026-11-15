<template>
  <AdminLayout>
    <AdminPageHeader title="Tranh chấp & hồ sơ hỗ trợ"
                     description="Xem phản hồi giữa người mua, người bán và quản trị viên."/>
    <p class="admin-note">Kết luận hồ sơ không thay đổi trạng thái thanh toán hoặc xác nhận hoàn tiền.</p>
    <form class="management-toolbar" @submit.prevent="apply"><label>Trạng thái<select v-model="status">
      <option value="">Tất cả</option>
      <option v-for="(label,key) in caseStatuses" :key="key" :value="key">{{ label }}</option>
    </select></label>
      <button class="button button-primary" :disabled="loading">Lọc hồ sơ</button>
    </form>
    <ManagementTable caption="Hồ sơ tranh chấp liên quan đơn hàng" :columns="columns" :rows="data.content"
                     :loading="loading" :error="error"
                     empty-title="Chưa có hồ sơ tranh chấp phù hợp. Hồ sơ mới sẽ xuất hiện khi một bên liên quan gửi yêu cầu."
                     @retry="load">
      <template #code="{row}">
        <RouterLink :to="'/admin/disputes/'+row.id">{{ row.code }}</RouterLink>
      </template>
      <template #status="{row}"><span class="admin-badge">{{ caseStatuses[row.status] }}</span></template>
      <template #updatedAt="{row}">{{ dateTime(row.updatedAt) }}</template>
      <template #actions="{row}">
        <RouterLink class="button button-outline" :to="'/admin/disputes/'+row.id">Mở hồ sơ</RouterLink>
      </template>
    </ManagementTable>
    <ManagementPagination v-if="!error" v-bind="data" :count="data.content.length" :busy="loading" noun="hồ sơ"/>
  </AdminLayout>
</template>
<script setup>
import {ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import AdminLayout from '../../layouts/AdminLayout.vue';
import AdminPageHeader from '../../components/admin/AdminPageHeader.vue';
import ManagementTable from '../../components/marketplace/ManagementTable.vue';
import ManagementPagination from '../../components/marketplace/ManagementPagination.vue';
import {useServerList} from '../../composables/useServerList.js';
import {caseStatuses, dateTime} from '../../services/adminPresentation.js';

const route = useRoute(), router = useRouter(), status = ref(''), {
  data,
  loading,
  error,
  load
} = useServerList('/admin/disputes', q => ({status: q.status || ''}));
const columns = [{key: 'code', label: 'Đơn hàng'}, {key: 'buyer', label: 'Người mua'}, {
  key: 'seller',
  label: 'Người bán'
}, {key: 'status', label: 'Trạng thái'}, {key: 'updatedAt', label: 'Cập nhật'}, {key: 'actions', label: 'Thao tác'}];
watch(() => route.query.status, s => status.value = s || '', {immediate: true});

function apply() {
  router.push({path: route.path, query: {status: status.value}});
}
</script>

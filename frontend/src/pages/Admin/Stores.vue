<template>
  <AdminLayout>
    <AdminPageHeader title="Gian hàng" description="Theo dõi hoạt động và vòng đời gian hàng toàn sàn.">
      <RouterLink class="button button-primary" to="/admin/store-applications">Xét duyệt hồ sơ</RouterLink>
    </AdminPageHeader>
    <form class="management-toolbar" @submit.prevent="apply"><label>Tìm gian hàng<input v-model="search" type="search"
                                                                                        maxlength="100"
                                                                                        placeholder="Tên gian hàng hoặc người bán"/></label><label>Trạng
      thái<select v-model="status">
        <option value="">Tất cả</option>
        <option v-for="(label,key) in storeStatuses" :key="key" :value="key">{{ label }}</option>
      </select></label>
      <button class="button button-primary" :disabled="loading">Lọc</button>
      <button type="button" class="button button-outline" @click="router.push({path:route.path})">Xóa bộ lọc</button>
    </form>
    <ManagementTable caption="Gian hàng toàn sàn" :columns="columns" :rows="data.content" :loading="loading"
                     :error="error" empty-title="Không có gian hàng phù hợp. Thử thay đổi bộ lọc." @retry="load">
      <template #name="{row}">
        <RouterLink :to="'/admin/stores/'+row.id">{{ row.name }}</RouterLink>
      </template>
      <template #status="{row}"><span class="admin-badge" :data-tone="row.status==='INACTIVE'?'warning':undefined">{{
          storeStatuses[row.status]
        }}</span></template>
      <template #createdAt="{row}">{{ dateTime(row.createdAt) }}</template>
      <template #actions="{row}">
        <RouterLink class="button button-outline" :to="'/admin/stores/'+row.id">Chi tiết</RouterLink>
      </template>
    </ManagementTable>
    <ManagementPagination v-if="!error" v-bind="data" :count="data.content.length" :busy="loading" noun="gian hàng"/>
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
import {storeStatuses, dateTime} from '../../services/adminPresentation.js';

const route = useRoute(), router = useRouter(), search = ref(''), status = ref(''), {
  data,
  loading,
  error,
  load
} = useServerList('/admin/stores', q => ({search: q.search || '', status: q.status || ''}));
const columns = [{key: 'name', label: 'Gian hàng'}, {key: 'ownerName', label: 'Người bán'}, {
  key: 'status',
  label: 'Trạng thái'
}, {key: 'products', label: 'Sản phẩm', numeric: true}, {
  key: 'orders',
  label: 'Đơn hàng',
  numeric: true
}, {key: 'createdAt', label: 'Ngày tạo'}, {key: 'actions', label: 'Thao tác'}];
watch(() => route.query, q => {
  search.value = q.search || '';
  status.value = q.status || '';
}, {immediate: true});

function apply() {
  router.push({path: route.path, query: {search: search.value, status: status.value}});
}
</script>

<template>
  <AdminLayout>
    <AdminPageHeader title="Sản phẩm" description="Tra cứu và kiểm duyệt sản phẩm của các gian hàng trên sàn."/>
    <form class="management-toolbar" @submit.prevent="apply"><label>Tìm sản phẩm<input v-model="term" type="search"
                                                                                       maxlength="100"
                                                                                       placeholder="Tên sản phẩm hoặc gian hàng"/></label><label>Danh
      mục<select v-model="category">
        <option value="">Tất cả danh mục</option>
        <option v-for="item in categories||[]" :key="item.id" :value="String(item.id)">{{ item.name }}</option>
      </select></label><label>Trạng thái<select v-model="status">
      <option value="">Tất cả</option>
      <option v-for="(label,key) in productStatuses" :key="key" :value="key">{{ label }}</option>
    </select></label>
      <button class="button button-primary" :disabled="loading">Lọc sản phẩm</button>
      <button type="button" class="button button-outline" @click="router.push({path:route.path})">Xóa bộ lọc</button>
    </form>
    <p v-if="categoryError" class="management-error" role="alert">{{ categoryError }}
      <button class="button button-outline" @click="loadCategories">Tải lại danh mục</button>
    </p>
    <ManagementTable caption="Sản phẩm toàn sàn" :columns="columns" :rows="data.content" :loading="loading"
                     :error="error" empty-title="Không tìm thấy sản phẩm phù hợp. Thử đổi từ khóa hoặc trạng thái."
                     @retry="load">
      <template #name="{row}">
        <div class="management-product-identity">
          <CatalogImage class="management-thumbnail" :src="row.imageUrl" :alt="row.name"/>
          <div>
            <RouterLink :to="'/admin/products/'+row.id">{{ row.name }}</RouterLink>
            <small>{{ row.category }}</small></div>
        </div>
      </template>
      <template #storeName="{row}">
        <RouterLink :to="'/admin/stores/'+row.storeId">{{ row.storeName }}</RouterLink>
        <small>{{ row.ownerName }}</small></template>
      <template #price="{row}">{{ money(row.price) }}</template>
      <template #status="{row}"><span class="admin-badge">{{ productStatuses[row.status] || 'Cần kiểm tra' }}</span>
      </template>
      <template #actions="{row}">
        <RouterLink class="button button-outline" :to="'/admin/products/'+row.id">Kiểm duyệt</RouterLink>
      </template>
    </ManagementTable>
    <ManagementPagination v-if="!error" v-bind="data" :count="data.content.length" :busy="loading" noun="sản phẩm"/>
  </AdminLayout>
</template>
<script setup>
import {ref, watch} from 'vue';
import {useAdminResource} from '../../composables/useAdminResource.js';
import {useRoute, useRouter} from 'vue-router';
import AdminLayout from '../../layouts/AdminLayout.vue';
import AdminPageHeader from '../../components/admin/AdminPageHeader.vue';
import ManagementTable from '../../components/marketplace/ManagementTable.vue';
import ManagementPagination from '../../components/marketplace/ManagementPagination.vue';
import CatalogImage from '../../components/marketplace/CatalogImage.vue';
import {useServerList} from '../../composables/useServerList.js';
import {productStatuses, money} from '../../services/adminPresentation.js';

const route = useRoute(), router = useRouter(), term = ref(''), status = ref(''), category = ref('');
const {data: categories, error: categoryError, load: loadCategories} = useAdminResource(() => '/categories');
const {data, loading, error, load} = useServerList('/admin/products', q => ({
  search: q.search || '',
  status: q.status || '', ...(q.category ? {category: q.category} : {})
}));
const columns = [{key: 'name', label: 'Sản phẩm / danh mục'}, {key: 'storeName', label: 'Gian hàng'}, {
  key: 'price',
  label: 'Giá',
  numeric: true
}, {key: 'stock', label: 'Tồn kho', numeric: true}, {key: 'status', label: 'Hiển thị'}, {
  key: 'actions',
  label: 'Thao tác'
}];
watch(() => route.query, q => {
  term.value = q.search || '';
  status.value = q.status || '';
  category.value = q.category || '';
}, {immediate: true});

function apply() {
  router.push({
    path: route.path,
    query: {search: term.value, status: status.value, ...(category.value ? {category: category.value} : {})}
  });
}
</script>

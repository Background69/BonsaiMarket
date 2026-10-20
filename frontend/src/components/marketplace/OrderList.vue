<template>
  <div class="management-page">
    <form class="management-toolbar" @submit.prevent="apply">
      <label>Tìm đơn hàng<input v-model="term" :disabled="loading" maxlength="100" placeholder="Mã đơn hoặc tên khách hàng" type="search"/></label>
      <label>Trạng thái
        <select v-model="status" :disabled="loading">
          <option value="">Tất cả</option>
          <option v-for="s in statuses" :key="s" :value="s">{{ statusLabel(s) }}</option>
        </select>
      </label>
      <label>Từ ngày<input v-model="from" :disabled="loading" :max="to || undefined" type="date"/></label>
      <label>Đến ngày<input v-model="to" :disabled="loading" :min="from || undefined" type="date"/></label>
      <button :disabled="loading || invalid" class="button button-primary">Lọc</button>
      <button :disabled="loading" class="button button-outline" type="button" @click="clear">Xóa bộ lọc</button>
    </form>
    <p v-if="invalid" class="management-error" role="alert">Ngày kết thúc phải bằng hoặc sau ngày bắt đầu.</p>
    <ManagementTable :columns="columns" :error="listError" :loading="loading" :rows="data.content" caption="Đơn hàng: mã đơn, khách hàng, giá trị, trạng thái và thao tác"
                     empty-title="Không có đơn hàng phù hợp." @retry="load">
      <template #code="{row}"><RouterLink :to="detailBase + '/' + row.id">{{ row.code }}</RouterLink></template>
      <template #buyer="{row}">{{ row.buyerName }}<small v-if="row.buyerEmail">{{ row.buyerEmail }}</small></template>
      <template #store="{row}">{{ row.storeName }}</template>
      <template #items="{row}">{{ row.itemCount }} dòng · {{ row.quantity }} sản phẩm</template>
      <template #total="{row}">{{ money(row.total) }}</template>
      <template #status="{row}"><span class="management-status">{{ statusLabel(row.status.toLowerCase()) }}</span></template>
      <template #createdAt="{row}">{{ dateTime(row.createdAt) }}</template>
      <template #actions="{row}">
        <div class="management-actions"><RouterLink :aria-label="'Xem chi tiết đơn ' + row.code" :to="detailBase + '/' + row.id" class="button button-outline">Chi tiết</RouterLink></div>
      </template>
    </ManagementTable>
    <ManagementPagination v-if="!listError" :busy="loading" :count="data.content.length" :loading="loading" :page="data.page" :size="data.size"
                          :total-elements="data.totalElements" :total-pages="data.totalPages" noun="đơn hàng"/>
  </div>
</template>
<script setup>
import {computed, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import ManagementTable from './ManagementTable.vue';
import ManagementPagination from './ManagementPagination.vue';
import {useServerList} from '../../composables/useServerList.js';
import {formatPrice, statusLabel} from '../../utils.js';

const props = defineProps({endpoint: {type: String, required: true}, detailBase: {type: String, required: true}, admin: Boolean});
const route = useRoute(), router = useRouter();
const statuses = ['PENDING', 'CONFIRMED', 'SHIPPING', 'DELIVERED', 'COMPLETED', 'CANCELLED'];
const term = ref(''), status = ref(''), from = ref(''), to = ref('');
const invalid = computed(() => !!from.value && !!to.value && from.value > to.value);
const {data, loading, error: listError, load} = useServerList(props.endpoint, q => ({
  search: q.search || '', status: q.status || '', from: q.from || '', to: q.to || '',
}));
const columns = computed(() => [
  {key: 'code', label: 'Mã đơn'}, {key: 'buyer', label: 'Khách hàng'},
  ...(props.admin ? [{key: 'store', label: 'Gian hàng'}] : []),
  {key: 'items', label: 'Sản phẩm'}, {key: 'total', label: 'Tổng tiền', numeric: true}, {key: 'status', label: 'Trạng thái'},
  {key: 'createdAt', label: 'Ngày đặt'}, {key: 'actions', label: 'Thao tác'},
]);
watch(() => route.query, q => {
  term.value = q.search || '';
  status.value = q.status || '';
  from.value = q.from || '';
  to.value = q.to || '';
}, {immediate: true});

const money = v => formatPrice(v) + ' ₫';
const dateTime = v => v ? new Date(v).toLocaleString('vi-VN') : '—';

function push(query) {
  router.push({path: route.path, query});
}

function apply() {
  if (invalid.value) return;
  const query = {};
  if (term.value.trim()) query.search = term.value.trim();
  if (status.value) query.status = status.value;
  if (from.value) query.from = from.value;
  if (to.value) query.to = to.value;
  push(query);
}

function clear() {
  push({});
}
</script>

<template>
  <AdminLayout>
    <AdminPageHeader title="Nhật ký quản trị"
                     description="Theo dõi người thực hiện, quyết định và diễn biến nghiệp vụ trên sàn."/>
    <form class="management-toolbar" @submit.prevent="apply"><label>Người thực hiện<input v-model="filters.search"
                                                                                          type="search" maxlength="100"
                                                                                          placeholder="Tìm theo tên"/></label><label>Đối
      tượng<select v-model="filters.type">
        <option value="">Tất cả</option>
        <option v-for="(label,key) in types" :key="key" :value="key">{{ label }}</option>
      </select></label><label>Hoạt động<select v-model="filters.action">
      <option value="">Tất cả</option>
      <option v-for="(label,key) in actions" :key="key" :value="key">{{ label }}</option>
    </select></label><label>Từ ngày<input v-model="filters.from" type="date"
                                          :max="filters.to||undefined"/></label><label>Đến ngày<input
        v-model="filters.to" type="date" :min="filters.from||undefined"/></label>
      <button class="button button-primary" :disabled="loading||!!(filters.from&&filters.to&&filters.from>filters.to)">
        Lọc nhật ký
      </button>
      <button type="button" class="button button-outline" @click="router.push({path:route.path})">Xóa bộ lọc</button>
    </form>
    <p class="admin-note">Lịch sử cũ có thể chưa ghi nhận trạng thái trước/sau. Nội dung kiểm tra toàn vẹn được giữ
      trong hệ thống, không hiển thị trên giao diện nghiệp vụ.</p>
    <ManagementTable caption="Nhật ký quản trị toàn sàn" :columns="columns" :rows="data.content" :loading="loading"
                     :error="error" empty-title="Chưa có hoạt động phù hợp. Thử thay đổi khoảng ngày hoặc bộ lọc."
                     @retry="load">
      <template #action="{row}"><strong>{{ row.action }}</strong><small>{{
          row.reason || 'Không có ghi chú bổ sung'
        }}</small></template>
      <template #entity="{row}">
        <RouterLink :to="row.link">{{ row.entity }} #{{ row.entityId }}</RouterLink>
      </template>
      <template #state="{row}"><span>{{ row.before || 'Chưa ghi nhận' }}</span><span aria-hidden="true"> → </span><span>{{
          row.after || 'Chưa ghi nhận'
        }}</span></template>
      <template #createdAt="{row}">{{ dateTime(row.createdAt) }}</template>
      <template #actions="{row}">
        <button class="button button-outline" :aria-label="'Xem hoạt động '+row.action" @click="selected=row">Chi tiết
        </button>
      </template>
    </ManagementTable>
    <ManagementPagination v-if="!error" v-bind="data" :count="data.content.length" :busy="loading" noun="hoạt động"/>
    <section v-if="selected" class="market-panel" aria-label="Chi tiết hoạt động">
      <div class="admin-section-heading"><h2>Chi tiết hoạt động</h2>
        <button class="button button-outline" @click="selected=null">Đóng chi tiết</button>
      </div>
      <AdminAuditTimeline :entries="[selected]"/>
      <RouterLink :to="selected.link">Mở đối tượng liên quan</RouterLink>
    </section>
  </AdminLayout>
</template>
<script setup>
import {reactive, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import AdminLayout from '../../layouts/AdminLayout.vue';
import AdminPageHeader from '../../components/admin/AdminPageHeader.vue';
import AdminAuditTimeline from '../../components/admin/AdminAuditTimeline.vue';
import ManagementTable from '../../components/marketplace/ManagementTable.vue';
import ManagementPagination from '../../components/marketplace/ManagementPagination.vue';
import {useServerList} from '../../composables/useServerList.js';
import {dateTime} from '../../services/adminPresentation.js';

const route = useRoute(), router = useRouter(), selected = ref(null),
    filters = reactive({search: '', type: '', action: '', from: '', to: ''});
const {
  data,
  loading,
  error,
  load
} = useServerList('/admin/audit', q => Object.fromEntries(Object.keys(filters).map(k => [k, q[k] || ''])));
watch(() => route.query, q => {
  for (const k of Object.keys(filters)) filters[k] = q[k] || '';
  selected.value = null;
}, {immediate: true});

function apply() {
  router.push({path: route.path, query: {...filters}});
}

const types = {
  USER: 'Tài khoản',
  STORE: 'Gian hàng',
  APPLICATION: 'Hồ sơ gian hàng',
  PRODUCT: 'Sản phẩm',
  CATEGORY: 'Danh mục',
  ORDER: 'Đơn hàng',
  REFUND: 'Hoàn tiền',
  REPORT: 'Báo cáo',
  SUPPORT_CASE: 'Tranh chấp',
  COMMUNITY_POST: 'Bài cộng đồng',
  COMMUNITY_COMMENT: 'Bình luận cộng đồng'
}, actions = {
  USER_DEACTIVATE: 'Khóa tài khoản',
  USER_ACTIVATE: 'Mở khóa tài khoản',
  APPLICATION_APPROVED: 'Duyệt gian hàng',
  APPLICATION_REJECTED: 'Từ chối hồ sơ',
  STORE_SUSPEND: 'Đình chỉ gian hàng',
  STORE_RESTORE: 'Khôi phục gian hàng',
  PRODUCT_HIDE: 'Ẩn sản phẩm',
  PRODUCT_RESTORE: 'Khôi phục sản phẩm',
  PRODUCT_REMOVE: 'Gỡ sản phẩm',
  COMMUNITY_HIDE: 'Ẩn nội dung cộng đồng',
  REPORT_RESOLVED: 'Xử lý báo cáo',
  REFUND_APPROVED: 'Duyệt hoàn tiền nội bộ'
};
const columns = [{key: 'action', label: 'Hoạt động & lý do'}, {key: 'actor', label: 'Người thực hiện'}, {
  key: 'entity',
  label: 'Đối tượng'
}, {key: 'state', label: 'Trước → Sau'}, {key: 'createdAt', label: 'Thời điểm'}, {key: 'actions', label: 'Thao tác'}];
</script>

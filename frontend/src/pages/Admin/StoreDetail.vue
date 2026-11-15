<template>
  <AdminLayout>
    <AdminPageHeader :title="data?.store.name||'Chi tiết gian hàng'"
                     description="Thông tin gian hàng, hồ sơ và quyết định quản trị.">
      <RouterLink class="button button-outline" to="/admin/stores">Danh sách gian hàng</RouterLink>
    </AdminPageHeader>
    <p v-if="loading" role="status">Đang tải gian hàng…</p>
    <div v-else-if="error" class="management-error" role="alert"><p>{{ error }}</p>
      <button class="button button-outline" @click="load">Thử lại</button>
    </div>
    <template v-else-if="data"><p v-if="success" role="status">{{ success }}</p>
      <div class="admin-two-column">
        <section class="market-panel"><h2>Thông tin gian hàng</h2>
          <dl class="admin-facts">
            <dt>Người bán</dt>
            <dd>
              <RouterLink :to="'/admin/users/'+data.store.ownerId">{{ data.store.ownerName }}</RouterLink>
            </dd>
            <dt>Trạng thái</dt>
            <dd><span class="admin-badge">{{ storeStatuses[data.store.status] }}</span></dd>
            <dt>Ngày tạo</dt>
            <dd>{{ dateTime(data.store.createdAt) }}</dd>
            <dt>Sản phẩm</dt>
            <dd>{{ data.store.products }}</dd>
            <dt>Đơn hàng</dt>
            <dd>{{ data.store.orders }}</dd>
            <dt>Giới thiệu</dt>
            <dd>{{ data.store.description || 'Chưa có giới thiệu.' }}</dd>
          </dl>
        </section>
        <aside class="market-panel"><h2>Hồ sơ & vòng đời</h2>
          <p v-if="data.applicationId">
            <RouterLink :to="'/admin/store-applications?record='+data.applicationId+'&status=APPROVED'">Xem hồ sơ đã xét
              duyệt
            </RouterLink>
          </p>
          <p v-else class="admin-note">Chưa có hồ sơ xét duyệt được liên kết. Không suy ra gian hàng đã được xác
            minh.</p>
          <p class="admin-note">Đình chỉ chặn giao dịch mới. Không hủy đơn cũ hoặc tự hoàn tiền.</p>
          <button type="button" class="button button-outline" @click="choose">{{ nextLabel }}</button>
        </aside>
      </div>
      <section class="market-panel"><h2>Sản phẩm gần đây</h2>
        <ManagementTable caption="Sản phẩm của gian hàng" :columns="columns" :rows="data.products"
                         empty-title="Gian hàng chưa có sản phẩm.">
          <template #name="{row}">
            <RouterLink :to="'/admin/products/'+row.id">{{ row.name }}</RouterLink>
          </template>
          <template #price="{row}">{{ money(row.price) }}</template>
          <template #status="{row}">{{ productStatuses[row.status] }}</template>
        </ManagementTable>
        <RouterLink :to="'/admin/products?search='+encodeURIComponent(data.store.name)">Tra cứu sản phẩm của gian hàng
        </RouterLink>
      </section>
      <section class="market-panel"><h2>Đơn hàng gần đây</h2>
        <ManagementTable caption="Mười đơn gần đây của gian hàng" :columns="orderColumns" :rows="data.orders"
                         empty-title="Gian hàng chưa có đơn hàng.">
          <template #code="{row}">
            <RouterLink :to="'/admin/orders/'+row.id">{{ row.code }}</RouterLink>
          </template>
          <template #status="{row}">{{ statusLabel(row.status) }}</template>
          <template #total="{row}">{{ money(row.total) }}</template>
          <template #createdAt="{row}">{{ dateTime(row.createdAt) }}</template>
        </ManagementTable>
      </section>
      <section class="market-panel"><h2>Lịch sử quản trị</h2>
        <AdminAuditTimeline :entries="data.history"/>
      </section>
    </template>
    <AdminConfirmDialog ref="confirm" :title="nextLabel"
                        description="Thao tác thay đổi khả năng giao dịch mới của gian hàng và được lưu vào nhật ký."
                        :busy="busy" :error="actionError" @confirm="decide"/>
  </AdminLayout>
</template>
<script setup>
import {computed, ref} from 'vue';
import {statusLabel} from '../../utils.js';

const orderColumns = [{key: 'code', label: 'Mã đơn'}, {key: 'status', label: 'Trạng thái'}, {
  key: 'total',
  label: 'Tổng tiền',
  numeric: true
}, {key: 'createdAt', label: 'Ngày đặt'}];
import {useRoute} from 'vue-router';
import AdminLayout from '../../layouts/AdminLayout.vue';
import AdminPageHeader from '../../components/admin/AdminPageHeader.vue';
import AdminConfirmDialog from '../../components/admin/AdminConfirmDialog.vue';
import AdminAuditTimeline from '../../components/admin/AdminAuditTimeline.vue';
import ManagementTable from '../../components/marketplace/ManagementTable.vue';
import {useAdminResource} from '../../composables/useAdminResource.js';
import {marketplaceRequest as request} from '../../services/marketplace.js';
import {storeStatuses, productStatuses, money, dateTime} from '../../services/adminPresentation.js';

const route = useRoute(), {data, loading, error, load} = useAdminResource(() => '/admin/stores/' + route.params.id),
    confirm = ref(null), busy = ref(false), actionError = ref(''), success = ref('');
const nextLabel = computed(() => data.value?.store.status === 'ACTIVE' ? 'Đình chỉ gian hàng' : 'Khôi phục gian hàng'),
    columns = [{key: 'name', label: 'Sản phẩm'}, {key: 'price', label: 'Giá', numeric: true}, {
      key: 'stock',
      label: 'Tồn kho',
      numeric: true
    }, {key: 'status', label: 'Hiển thị'}];

function choose(e) {
  actionError.value = '';
  confirm.value.open(e.currentTarget);
}

async function decide(reason) {
  if (busy.value) return;
  busy.value = true;
  actionError.value = '';
  try {
    data.value = await request('/admin/stores/' + route.params.id + '/status', 'POST', {
      action: data.value.store.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE',
      revision: data.value.store.revision,
      reason
    });
    confirm.value.close();
    success.value = 'Đã ghi nhận trạng thái gian hàng.';
  } catch (e) {
    actionError.value = e.message;
  } finally {
    busy.value = false;
  }
}
</script>

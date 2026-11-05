<template>
  <AdminLayout>
    <AdminPageHeader :title="data?.product.name||'Kiểm duyệt sản phẩm'"
                     description="Xem thông tin và quyết định phạm vi hiển thị trên sàn.">
      <RouterLink class="button button-outline" to="/admin/products">Danh sách sản phẩm</RouterLink>
    </AdminPageHeader>
    <p v-if="loading" role="status">Đang tải sản phẩm…</p>
    <div v-else-if="error" class="management-error" role="alert"><p>{{ error }}</p>
      <button class="button button-outline" @click="load">Thử lại</button>
    </div>
    <template v-else-if="data"><p v-if="success" role="status">{{ success }}</p>
      <div class="admin-two-column">
        <div class="admin-stack">
          <section class="market-panel"><h2>Thông tin cơ bản</h2>
            <dl class="admin-facts">
              <dt>Tên sản phẩm</dt>
              <dd>{{ data.product.name }}</dd>
              <dt>Danh mục</dt>
              <dd>{{ data.product.category }}</dd>
              <dt>Giá niêm yết</dt>
              <dd>{{ money(data.product.price) }}</dd>
              <dt>Tồn kho</dt>
              <dd>{{ data.product.stock }}</dd>
              <dt>Hiển thị</dt>
              <dd><span class="admin-badge">{{ productStatuses[data.product.status] }}</span></dd>
            </dl>
            <h3>Mô tả sản phẩm</h3>
            <p class="admin-product-description">{{ data.description || 'Chưa có mô tả.' }}</p></section>
          <section class="market-panel"><h2>Lịch sử kiểm duyệt</h2>
            <AdminAuditTimeline :entries="data.history"/>
          </section>
        </div>
        <aside class="admin-stack">
          <section class="market-panel"><h2>Hình ảnh sản phẩm</h2>
            <CatalogImage class="admin-product-image" :src="data.product.imageUrl" :alt="data.product.name"/>
          </section>
          <section class="market-panel"><h2>Gian hàng & người bán</h2>
            <RouterLink :to="'/admin/stores/'+data.product.storeId">{{ data.product.storeName }}</RouterLink>
            <p>{{ data.product.ownerName }}</p>
            <RouterLink :to="'/admin/reports?targetType=PRODUCT&targetId='+data.product.id">Xem báo cáo liên quan
            </RouterLink>
          </section>
          <section class="market-panel"><h2>Hành động kiểm duyệt</h2>
            <p class="admin-note">Ẩn hoặc gỡ chặn giao dịch mới. Lịch sử đơn đã phát sinh được giữ nguyên.</p>
            <div class="admin-stack">
              <button v-for="action in data.actions" :key="action" type="button" class="button button-outline"
                      @click="choose(action,$event)">{{ actions[action] }}
              </button>
              <p v-if="!data.actions.length">Không có thao tác kiểm duyệt phù hợp.</p></div>
          </section>
        </aside>
      </div>
    </template>
    <AdminConfirmDialog ref="confirm" :title="actions[selected]||'Xác nhận kiểm duyệt'"
                        description="Quyết định được ghi vào nhật ký quản trị. Giá và tồn kho không thay đổi."
                        :busy="busy" :error="actionError" @confirm="moderate"/>
  </AdminLayout>
</template>
<script setup>
import {ref} from 'vue';
import {useRoute} from 'vue-router';
import AdminLayout from '../../layouts/AdminLayout.vue';
import AdminPageHeader from '../../components/admin/AdminPageHeader.vue';
import AdminConfirmDialog from '../../components/admin/AdminConfirmDialog.vue';
import AdminAuditTimeline from '../../components/admin/AdminAuditTimeline.vue';
import CatalogImage from '../../components/marketplace/CatalogImage.vue';
import {useAdminResource} from '../../composables/useAdminResource.js';
import {marketplaceRequest as request} from '../../services/marketplace.js';
import {productStatuses, money} from '../../services/adminPresentation.js';

const route = useRoute(), {data, loading, error, load} = useAdminResource(() => '/admin/products/' + route.params.id),
    confirm = ref(null), selected = ref(''), busy = ref(false), actionError = ref(''), success = ref('');
const actions = {HIDE: 'Ẩn sản phẩm', RESTORE: 'Khôi phục hiển thị', REMOVE: 'Gỡ và lưu trữ sản phẩm'};

function choose(action, event) {
  selected.value = action;
  actionError.value = '';
  confirm.value.open(event.currentTarget);
}

async function moderate(reason) {
  if (busy.value) return;
  busy.value = true;
  actionError.value = '';
  try {
    data.value = await request('/admin/products/' + route.params.id + '/moderation', 'POST', {
      action: selected.value,
      revision: data.value.product.revision,
      reason
    });
    confirm.value.close();
    success.value = 'Đã ghi nhận quyết định kiểm duyệt.';
  } catch (e) {
    actionError.value = e.message;
  } finally {
    busy.value = false;
  }
}
</script>

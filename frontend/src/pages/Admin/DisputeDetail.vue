<template>
  <AdminLayout>
    <AdminPageHeader :title="data?'Hồ sơ tranh chấp · '+data.support.code:'Chi tiết hồ sơ tranh chấp'"
                     description="Các bên cung cấp thông tin và chứng cứ bằng văn bản trong cùng hồ sơ.">
      <RouterLink class="button button-outline" to="/admin/disputes">Danh sách hồ sơ</RouterLink>
    </AdminPageHeader>
    <p v-if="loading" role="status">Đang tải hồ sơ…</p>
    <div v-else-if="error" class="management-error" role="alert"><p>{{ error }}</p>
      <button class="button button-outline" @click="load">Thử lại</button>
    </div>
    <template v-else-if="data">
      <div class="admin-two-column">
        <section class="market-panel"><h2>Diễn biến hồ sơ</h2>
          <ol class="admin-timeline">
            <li v-for="event in data.events" :key="event.id"><strong>{{ event.actor }} · {{
                kinds[event.kind]
              }}</strong>
              <time :datetime="event.createdAt">{{ dateTime(event.createdAt) }}</time>
              <p class="admin-product-description">{{ event.body }}</p><span
                  class="admin-badge">{{ caseStatuses[event.status] }}</span></li>
          </ol>
          <form v-if="data.canRespond" class="management-form" @submit.prevent="respond"><label for="support-response">Phản
            hồi của quản trị viên</label><textarea id="support-response" v-model="body" maxlength="4000" required
                                                   rows="5" :disabled="busy"/>
            <p v-if="responseError" class="management-error" role="alert">{{ responseError }}</p>
            <button class="button button-primary" :disabled="busy||!body.trim()">{{
                busy ? 'Đang gửi…' : 'Gửi phản hồi'
              }}
            </button>
            <p class="admin-note">Khi kết quả gửi chưa rõ, giữ nội dung và mã gửi để kiểm tra trước khi thử lại.</p>
          </form>
        </section>
        <aside class="admin-stack">
          <section class="market-panel"><h2>Các bên liên quan</h2>
            <dl class="admin-facts">
              <dt>Người mua</dt>
              <dd>{{ data.support.buyer }}</dd>
              <dt>Người bán</dt>
              <dd>{{ data.support.seller }}</dd>
              <dt>Trạng thái</dt>
              <dd>{{ caseStatuses[data.support.status] }}</dd>
              <dt>Đơn hàng</dt>
              <dd>
                <RouterLink :to="'/admin/orders/'+data.support.orderId">{{ data.support.code }}</RouterLink>
              </dd>
            </dl>
          </section>
          <section class="market-panel"><h2>Xử lý hồ sơ</h2>
            <p class="admin-note">Quyết định chỉ áp dụng hồ sơ hỗ trợ. Mọi quyết định tài chính đi qua quy trình hoàn
              tiền riêng.</p>
            <div class="admin-stack">
              <button v-for="action in data.actions" :key="action" class="button button-outline"
                      @click="choose(action,$event)">{{ caseStatuses[action] }}
              </button>
              <p v-if="!data.actions.length">Hồ sơ đã kết thúc.</p></div>
          </section>
        </aside>
      </div>
    </template>
    <AdminConfirmDialog ref="confirm" :title="'Chuyển hồ sơ: '+(caseStatuses[selected]||'')"
                        description="Nhập nhận xét và cơ sở xử lý. Các bên liên quan có thể xem nội dung này."
                        :busy="busy" :error="actionError" @confirm="decide"/>
  </AdminLayout>
</template>
<script setup>
import {ref} from 'vue';
import {useRoute} from 'vue-router';
import AdminLayout from '../../layouts/AdminLayout.vue';
import AdminPageHeader from '../../components/admin/AdminPageHeader.vue';
import AdminConfirmDialog from '../../components/admin/AdminConfirmDialog.vue';
import {useAdminResource} from '../../composables/useAdminResource.js';
import {marketplaceRequest as request} from '../../services/marketplace.js';
import {caseStatuses, dateTime} from '../../services/adminPresentation.js';

const route = useRoute(), {data, loading, error, load} = useAdminResource(() => '/admin/disputes/' + route.params.id),
    body = ref(''), busy = ref(false), responseError = ref(''), actionError = ref(''), confirm = ref(null),
    selected = ref('');
let requestKey = crypto.randomUUID();
const kinds = {OPENED: 'Mở hồ sơ', RESPONSE: 'Phản hồi', DECISION: 'Quyết định xử lý'};

function choose(action, e) {
  selected.value = action;
  actionError.value = '';
  confirm.value.open(e.currentTarget);
}

async function respond() {
  if (busy.value) return;
  busy.value = true;
  responseError.value = '';
  try {
    data.value = await request('/admin/disputes/' + route.params.id + '/responses', 'POST', {
      revision: data.value.support.revision,
      body: body.value,
      requestKey
    });
    body.value = '';
    requestKey = crypto.randomUUID();
  } catch (e) {
    responseError.value = e.message;
  } finally {
    busy.value = false;
  }
}

async function decide(reason) {
  if (busy.value) return;
  busy.value = true;
  actionError.value = '';
  try {
    data.value = await request('/admin/disputes/' + route.params.id + '/decision', 'POST', {
      revision: data.value.support.revision,
      status: selected.value,
      reason
    });
    confirm.value.close();
  } catch (e) {
    actionError.value = e.message;
  } finally {
    busy.value = false;
  }
}
</script>

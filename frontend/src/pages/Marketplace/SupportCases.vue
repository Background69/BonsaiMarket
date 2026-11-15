<template>
  <MarketplaceLayout>
    <main class="market-container market-page support-page">
      <header><h1>Hồ sơ hỗ trợ đơn hàng</h1>
        <p>Trao đổi thông tin và chứng cứ bằng văn bản với bên liên quan và quản trị viên.</p></header>
      <p class="muted">Kết luận hồ sơ không đồng nghĩa đã hoàn tiền. Hoàn tiền vẫn được xem xét trong quy trình riêng
        của đơn hàng.</p>
      <p v-if="loading" role="status">Đang tải hồ sơ…</p>
      <div v-else-if="error" role="alert"><p>{{ error }}</p>
        <button class="button button-outline" @click="load">Thử lại</button>
      </div>
      <template v-else-if="data">
        <template v-if="route.params.id">
          <RouterLink to="/support-cases">← Danh sách hồ sơ của tôi</RouterLink>
          <section class="market-panel"><h2>{{ data.support.code }}</h2><span
              class="management-status">{{ caseStatuses[data.support.status] }}</span>
            <p>Người mua: {{ data.support.buyer }} · Người bán: {{ data.support.seller }}</p>
            <ol class="support-timeline">
              <li v-for="event in data.events" :key="event.id"><strong>{{ event.actor }}</strong>
                <time :datetime="event.createdAt">{{ dateTime(event.createdAt) }}</time>
                <p>{{ event.body }}</p></li>
            </ol>
            <form v-if="data.canRespond" class="management-form" @submit.prevent="respond"><label for="case-response">Nội
              dung phản hồi / chứng cứ</label><textarea id="case-response" v-model="body" required maxlength="4000"
                                                        rows="5" :disabled="busy"/>
              <button class="button button-primary" :disabled="busy||!body.trim()">
                {{ busy ? 'Đang gửi…' : 'Gửi phản hồi' }}
              </button>
            </form>
          </section>
        </template>
        <template v-else>
          <section v-if="orderId" class="market-panel"><h2>Mở hồ sơ cho đơn hàng</h2>
            <p>Đơn #{{ orderId }} · Máy chủ kiểm tra bạn là người mua hoặc chủ gian hàng của đơn này.</p>
            <form class="management-form" @submit.prevent="open"><label for="case-open-reason">Vấn đề và thông tin cần
              hỗ trợ</label><textarea id="case-open-reason" v-model="reason" required maxlength="1000" rows="5"
                                      :disabled="busy"/>
              <button class="button button-primary" :disabled="busy||!reason.trim()">
                {{ busy ? 'Đang gửi…' : 'Gửi yêu cầu hỗ trợ' }}
              </button>
            </form>
          </section>
          <h2>Hồ sơ của tôi</h2>
          <ul v-if="data.content.length" class="support-list">
            <li v-for="item in data.content" :key="item.id">
              <RouterLink :to="'/support-cases/'+item.id">{{ item.code }} · {{ caseStatuses[item.status] }}</RouterLink>
              <p>{{ item.reason }}</p></li>
          </ul>
          <section v-else class="market-empty"><h2>Chưa có hồ sơ hỗ trợ</h2>
            <p>Mở chi tiết đơn hàng liên quan để gửi yêu cầu khi cần.</p>
            <RouterLink class="button button-outline" to="/orders">Xem đơn hàng của tôi</RouterLink>
          </section>
          <ManagementPagination v-if="data.content.length" v-bind="data" :count="data.content.length" noun="hồ sơ"/>
        </template>
      </template>
      <p v-if="actionError" class="management-error" role="alert">{{ actionError }}</p></main>
  </MarketplaceLayout>
</template>
<script setup>
import {computed, ref} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import MarketplaceLayout from '../../layouts/MarketplaceLayout.vue';
import ManagementPagination from '../../components/marketplace/ManagementPagination.vue';
import {useAdminResource} from '../../composables/useAdminResource.js';
import {marketplaceRequest as request} from '../../services/marketplace.js';
import {caseStatuses, dateTime} from '../../services/adminPresentation.js';

const route = useRoute(), router = useRouter(),
    orderId = computed(() => /^\d+$/.test(String(route.query.order || '')) ? route.query.order : null), {
      data,
      loading,
      error,
      load
    } = useAdminResource(() => route.params.id ? '/account/disputes/' + route.params.id : '/account/disputes?page=' + (route.query.page || 0)),
    reason = ref(''), body = ref(''), busy = ref(false), actionError = ref('');
let key = crypto.randomUUID();

async function open() {
  if (busy.value) return;
  busy.value = true;
  actionError.value = '';
  try {
    const result = await request('/account/disputes', 'POST', {orderId: Number(orderId.value), reason: reason.value});
    await router.push('/support-cases/' + result.support.id);
  } catch (e) {
    actionError.value = e.message;
  } finally {
    busy.value = false;
  }
}

async function respond() {
  if (busy.value) return;
  busy.value = true;
  actionError.value = '';
  try {
    data.value = await request('/account/disputes/' + route.params.id + '/responses', 'POST', {
      revision: data.value.support.revision,
      body: body.value,
      requestKey: key
    });
    body.value = '';
    key = crypto.randomUUID();
  } catch (e) {
    actionError.value = e.message;
  } finally {
    busy.value = false;
  }
}
</script>
<style scoped>
.support-page {
  max-width: 1100px;
}

.support-page header {
  margin-bottom: 24px;
}

.support-page section {
  margin-block: 24px;
}

.support-timeline, .support-list {
  list-style: none;
  padding: 0;
  display: grid;
  gap: 20px;
}

.support-timeline li, .support-list li {
  padding: 16px;
  border-bottom: 1px solid var(--bm-border);
}

.support-timeline time {
  display: block;
  color: var(--bm-muted);
  font-size: 13px;
}

.support-timeline p, .support-list p {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.support-page textarea {
  width: 100%;
  font: inherit;
  padding: 12px;
  border: 1px solid var(--bm-control-border);
  border-radius: var(--bm-radius);
}
</style>

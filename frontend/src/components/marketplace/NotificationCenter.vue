<template>
  <section class="notification-center">
    <nav class="notification-filters" aria-label="Lọc thông báo">
      <RouterLink v-for="item in filters" :key="item.label" :to="{path:route.path,query:{unread:item.value}}"
                  :aria-current="(route.query.unread||'')===item.value?'page':undefined">{{ item.label }}
      </RouterLink>
    </nav>
    <p v-if="actionError" class="management-error" role="alert">{{ actionError }}</p>
    <p v-if="loading" role="status">Đang tải thông báo…</p>
    <div v-else-if="error" class="management-error" role="alert"><p>{{ error }}</p>
      <button class="button button-outline" @click="load">Thử lại</button>
    </div>
    <template v-else>
      <AdminEmptyState v-if="!data.content.length" title="Chưa có thông báo phù hợp"
                       description="Thông báo mới xuất hiện khi có hồ sơ hoặc phản hồi dành cho bạn." icon="bell"/>
      <ul v-else class="notification-list">
        <li v-for="item in data.content" :key="item.id" :class="{'is-unread':!item.readAt}">
          <MarketIcon name="bell"/>
          <div>
            <RouterLink :to="item.link" @click.prevent="open(item)"><strong>{{ item.title }}</strong></RouterLink>
            <p>{{ item.body }}</p>
            <time :datetime="item.createdAt">{{ dateTime(item.createdAt) }}</time>
            <span class="notification-status">{{ item.readAt ? 'Đã đọc' : 'Chưa đọc' }}</span></div>
          <button v-if="!item.readAt" type="button" class="button button-outline" :disabled="!!busy"
                  @click="mark(item)">{{ busy === item.id ? 'Đang lưu…' : 'Đánh dấu đã đọc' }}
          </button>
        </li>
      </ul>
      <ManagementPagination v-bind="data" :count="data.content.length" :busy="loading||!!busy" noun="thông báo"/>
    </template>
  </section>
</template>
<script setup>
import {ref} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import AdminEmptyState from '../admin/AdminEmptyState.vue';
import MarketIcon from './MarketIcon.vue';
import ManagementPagination from './ManagementPagination.vue';
import {useServerList} from '../../composables/useServerList.js';
import {marketplaceRequest as request} from '../../services/marketplace.js';
import {dateTime} from '../../services/adminPresentation.js';

const props = defineProps({admin: Boolean}), route = useRoute(), router = useRouter(),
    endpoint = props.admin ? '/admin/notifications' : '/account/notifications', {
      data,
      loading,
      error,
      load
    } = useServerList(endpoint, q => ({unread: q.unread || ''})), busy = ref(''), actionError = ref(''),
    filters = [{label: 'Tất cả', value: ''}, {label: 'Chưa đọc', value: 'true'}, {label: 'Đã đọc', value: 'false'}];

async function mark(item) {
  if (busy.value) return false;
  busy.value = item.id;
  actionError.value = '';
  try {
    await request(endpoint + '/' + item.id + '/read', 'POST');
    await load();
    window.dispatchEvent(new Event('bonsai:notifications-updated'));
    return true;
  } catch (e) {
    actionError.value = e.message;
    return false;
  } finally {
    busy.value = '';
  }
}

async function open(item) {
  if (busy.value) return;
  if (!item.readAt && !await mark(item)) return;
  await router.push(item.link);
}
</script>
<style scoped>
.notification-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 20px 0
}

.notification-filters a {
  display: inline-flex;
  align-items: center;
  min-height: 44px;
  padding: 10px 16px;
  border: 1px solid var(--bm-border);
  border-radius: var(--bm-radius-control);
  color: var(--bm-forest);
  text-decoration: none
}

.notification-filters a[aria-current=page] {
  background: var(--bm-soft);
  border-color: var(--bm-forest);
  font-weight: 600
}

.notification-status {
  display: inline-flex;
  padding: 4px 8px;
  border-radius: var(--bm-radius-control);
  background: var(--bm-soft);
  color: var(--bm-forest);
  font-size: 12px
}

.notification-list {
  list-style: none;
  margin: 24px 0;
  padding: 0;
  display: grid;
  gap: 12px
}

.notification-list li {
  display: flex;
  align-items: start;
  gap: 16px;
  padding: 20px;
  border: 1px solid var(--bm-border);
  border-radius: var(--bm-radius);
  background: var(--bm-surface)
}

.notification-list li.is-unread {
  background: var(--bm-soft)
}

.notification-list li > div {
  flex: 1;
  min-width: 0;
  overflow-wrap: anywhere
}

.notification-list p {
  margin: 8px 0;
  max-width: 70ch
}

.notification-list time {
  display: inline-block;
  font-size: 13px;
  color: var(--bm-muted);
  margin-right: 12px
}

.notification-list svg {
  color: var(--bm-forest)
}

.notification-list .button {
  flex: none
}

.notification-list a {
  color: var(--bm-forest);
  text-underline-offset: 4px
}

@media (max-width: 600px) {
  .notification-list li {
    flex-wrap: wrap;
    padding: 16px
  }

  .notification-list li > .button {
    margin-left: 38px;
    width: calc(100% - 38px)
  }
}
</style>

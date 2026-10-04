<template>
  <AdminLayout>
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-base font-bold text-stone-800">Diễn đàn / Bài viết</h1>
    </div>
    <div class="bg-white rounded-xl border border-stone-200 overflow-hidden">
      <div class="p-3 border-b border-stone-100 flex gap-2 flex-wrap">
        <input v-model="search" @input="filter" placeholder="Tìm kiếm..." class="h-8 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500 w-52">
        <select v-model="statusFilter" @change="filter" class="h-8 px-2 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500">
          <option value="">Tất cả trạng thái</option>
          <option value="pending">Chờ duyệt</option>
          <option value="approved">Đã duyệt</option>
          <option value="rejected">Từ chối</option>
        </select>
      </div>
      <table class="w-full text-xs">
        <thead><tr class="bg-stone-50 text-stone-500 text-left"><th class="p-3 font-medium">ID</th><th class="p-3 font-medium">Tiêu đề</th><th class="p-3 font-medium">Danh mục</th><th class="p-3 font-medium">Người đăng</th><th class="p-3 font-medium">Thống kê</th><th class="p-3 font-medium">Trạng thái</th><th class="p-3 font-medium">Ngày</th><th class="p-3 font-medium"></th></tr></thead>
        <tbody>
          <tr v-for="p in posts.data" :key="p.id" class="border-t border-stone-100 hover:bg-stone-50 transition-colors">
            <td class="p-3 text-stone-500 font-mono whitespace-nowrap">{{ p.id }}</td>
            <td class="p-3 font-medium text-stone-800 max-w-[200px] truncate">
              <span v-if="p.is_pinned" class="material-symbols-outlined text-amber-500 text-sm align-middle mr-0.5" title="Đã ghim">push_pin</span>
              {{ p.title || '—' }}
            </td>
            <td class="p-3"><span class="text-[10px] px-1.5 py-0.5 rounded-full font-semibold bg-sky-50 text-sky-600 whitespace-nowrap">{{ p.category || '—' }}</span></td>
            <td class="p-3 text-stone-500">{{ p.user?.name || 'Khách' }}</td>
            <td class="p-3 text-stone-400 whitespace-nowrap">
              <span class="mr-2" title="Bình luận"><span class="material-symbols-outlined text-xs align-middle">chat_bubble</span> {{ p.comments_count ?? 0 }}</span>
              <span title="Lượt thích"><span class="material-symbols-outlined text-xs align-middle">favorite</span> {{ p.likes_count ?? 0 }}</span>
            </td>
            <td class="p-3"><span class="text-[10px] px-1.5 py-0.5 rounded-full font-semibold" :class="statusClass(p.status)">{{ statusLabel(p.status) }}</span></td>
            <td class="p-3 text-stone-500 whitespace-nowrap">{{ formatDate(p.created_at) }}</td>
            <td class="p-3 text-right whitespace-nowrap">
              <Link :href="route('admin.agriverse.forum.show', p.id)" class="text-sky-600 hover:text-sky-800 mr-2">Xem</Link>
              <button @click="approve(p)" class="text-emerald-600 hover:text-emerald-800 mr-2">{{ p.status === 'approved' ? 'Bỏ duyệt' : 'Duyệt' }}</button>
              <button v-if="p.status !== 'rejected'" @click="openReject(p)" class="text-red-500 hover:text-red-700 mr-2">Từ chối</button>
              <button @click="togglePin(p)" class="text-amber-600 hover:text-amber-800 mr-2">{{ p.is_pinned ? 'Bỏ ghim' : 'Ghim' }}</button>
              <button @click="destroy(p.id)" class="text-red-500 hover:text-red-700">Xóa</button>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Reject modal -->
      <div v-if="rejectTarget" class="fixed inset-0 z-50 flex items-center justify-center bg-black/30 backdrop-blur-sm" @click.self="rejectTarget = null">
        <div class="bg-white rounded-2xl p-6 max-w-md w-full mx-4 shadow-2xl">
          <div class="flex items-center gap-3 mb-4">
            <div class="w-10 h-10 rounded-full bg-red-50 text-red-500 flex items-center justify-center">
              <span class="material-symbols-outlined text-lg">block</span>
            </div>
            <div>
              <h3 class="text-sm font-bold text-stone-800">Từ chối bài viết</h3>
              <p class="text-xs text-stone-500">"{{ rejectTarget?.title }}"</p>
            </div>
          </div>
          <textarea v-model="rejectReason" placeholder="Nhập lý do từ chối..." class="w-full h-24 px-4 py-3 rounded-xl border border-stone-200 text-sm outline-none resize-none focus:border-red-400 focus:ring-4 focus:ring-red-50 mb-4" />
          <div class="flex gap-3">
            <button @click="rejectTarget = null" class="flex-1 h-10 rounded-xl border border-stone-200 text-stone-600 text-xs font-semibold hover:bg-stone-50 transition-all">Hủy</button>
            <button @click="submitReject" :disabled="!rejectReason.trim()" class="flex-1 h-10 rounded-xl bg-red-500 text-white text-xs font-semibold hover:bg-red-600 transition-all disabled:opacity-50">Xác nhận từ chối</button>
          </div>
        </div>
      </div>

      <div class="p-3 border-t border-stone-100 flex items-center justify-between text-xs text-stone-500">
        <span>Trang {{ posts.current_page }}/{{ posts.last_page }}</span>
        <div class="flex gap-1"><Link v-for="link in posts.links" :key="link.label" :href="link.url || '#'" v-html="link.label" class="px-2 py-1 rounded border border-stone-200 hover:bg-emerald-50" :class="{ 'bg-emerald-600 text-white border-emerald-600': link.active }" /></div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref } from 'vue';
import { Link, router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';
import { statusLabel, statusClass } from '@agriverse/utils';

const props = defineProps({ posts: Object });
const search = ref('');
const statusFilter = ref('');
const rejectTarget = ref(null);
const rejectReason = ref('');

function formatDate(d) { return d ? new Date(d).toLocaleDateString('vi-VN') : '—'; }

function filter() {
  router.get(route('admin.agriverse.forum.index'), { search: search.value, status: statusFilter.value }, { preserveState: true });
}

function approve(p) {
  router.post(route('admin.agriverse.forum.approve', p.id));
}

function openReject(p) {
  rejectTarget.value = p;
  rejectReason.value = '';
}

function submitReject() {
  if (!rejectReason.value.trim()) return;
  router.post(route('admin.agriverse.forum.reject', rejectTarget.value.id), { reason: rejectReason.value }, {
    preserveState: true,
    onSuccess: () => { rejectTarget.value = null; rejectReason.value = ''; },
  });
}

function togglePin(p) {
  router.post(route('admin.agriverse.forum.pin', p.id));
}

function destroy(id) {
  if (confirm('Xóa bài viết này?')) router.delete(route('admin.agriverse.forum.destroy', id));
}
</script>

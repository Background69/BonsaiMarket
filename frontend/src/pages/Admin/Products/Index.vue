<template>
  <AdminLayout>
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-base font-bold text-stone-800">Sản phẩm</h1>
      <Link :href="route('admin.agriverse.products.create')" class="h-8 px-3 rounded-lg bg-emerald-600 text-white text-xs font-bold leading-8 hover:bg-emerald-700 transition-all">+ Thêm</Link>
    </div>
    <div class="bg-white rounded-xl border border-stone-200 overflow-hidden">
      <div class="p-3 border-b border-stone-100 flex flex-wrap items-center gap-2">
        <input v-model="search" @input="filter" placeholder="Tìm kiếm..." class="h-8 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500 w-52">
        <select v-model="categoryFilter" @change="filter" class="h-8 px-2 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500">
          <option value="">Tất cả danh mục</option>
          <option v-for="c in categories" :key="c.id" :value="c.name">{{ c.name }}</option>
        </select>
      </div>
      <div class="px-3 py-2 border-b border-stone-100 flex gap-1 text-xs">
        <button v-for="tab in filterTabs" :key="tab.value" @click="statusFilter = tab.value; filter()"
          class="px-3 py-1.5 rounded-lg font-medium transition-all"
          :class="statusFilter === tab.value ? 'bg-emerald-600 text-white' : 'text-stone-600 hover:bg-stone-100'">
          {{ tab.label }}
        </button>
      </div>
      <table class="w-full text-xs">
        <thead><tr class="bg-stone-50 text-stone-500 text-left"><th class="p-3 font-medium">Tên</th><th class="p-3 font-medium">Danh mục</th><th class="p-3 font-medium">Giá</th><th class="p-3 font-medium">Trạng thái</th><th class="p-3 font-medium"></th></tr></thead>
        <tbody>
          <tr v-for="p in products.data" :key="p.id" class="border-t border-stone-100 hover:bg-stone-50 transition-colors">
            <td class="p-3 font-medium text-stone-800">{{ p.name }}</td>
            <td class="p-3 text-stone-500">{{ p.category || '—' }}</td>
            <td class="p-3 text-stone-700">{{ formatPrice(p.price) }}₫</td>
            <td class="p-3"><span class="text-[10px] px-1.5 py-0.5 rounded-full font-semibold" :style="statusBadgeStyle(p.status)">{{ statusLabel(p.status) }}</span></td>
            <td class="p-3 text-right">
              <Link :href="route('admin.agriverse.products.show', p.id)" class="text-blue-600 hover:text-blue-800 mr-2">Xem</Link>
              <Link :href="route('admin.agriverse.products.edit', p.id)" class="text-emerald-600 hover:text-emerald-800 mr-2">Sửa</Link>
              <button v-if="p.status === 'pending_review'" @click="approve(p.id)" class="text-emerald-600 hover:text-emerald-800 mr-2 font-semibold">Duyệt</button>
              <button v-if="p.status === 'pending_review'" @click="openRejectModal(p)" class="text-red-500 hover:text-red-700 mr-2">Từ chối</button>
              <button @click="destroy(p.id)" class="text-red-500 hover:text-red-700">Xóa</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div class="p-3 border-t border-stone-100 flex items-center justify-between text-xs text-stone-500">
        <span>Trang {{ products.current_page }}/{{ products.last_page }}</span>
        <div class="flex gap-1">
          <Link v-for="link in products.links" :key="link.label" :href="link.url || '#'" v-html="link.label" class="px-2 py-1 rounded border border-stone-200 hover:bg-emerald-50" :class="{ 'bg-emerald-600 text-white border-emerald-600': link.active }" />
        </div>
      </div>
    </div>

    <!-- Reject Modal -->
    <Teleport to="body">
      <div v-if="showRejectModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/30">
        <div class="bg-white rounded-xl p-4 w-full max-w-md mx-4 shadow-xl">
          <h3 class="text-sm font-bold text-stone-800 mb-3">Từ chối sản phẩm</h3>
          <p class="text-xs text-stone-500 mb-2">Lý do từ chối sản phẩm "{{ rejectTarget?.name }}"</p>
          <textarea v-model="rejectReason" rows="3" class="w-full px-3 py-2 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500 mb-3" placeholder="Nhập lý do từ chối..."></textarea>
          <div class="flex gap-2 justify-end">
            <button @click="showRejectModal = false" class="h-8 px-3 rounded-lg border border-stone-300 text-stone-600 text-xs font-medium">Hủy</button>
            <button @click="confirmReject" class="h-8 px-3 rounded-lg bg-red-500 text-white text-xs font-medium hover:bg-red-600" :disabled="!rejectReason.trim()">Xác nhận từ chối</button>
          </div>
        </div>
      </div>
    </Teleport>
  </AdminLayout>
</template>

<script setup>
import { ref } from 'vue';
import { Link, router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';

const props = defineProps({ products: Object, categories: Array });
const search = ref('');
const statusFilter = ref('');
const categoryFilter = ref('');
const showRejectModal = ref(false);
const rejectTarget = ref(null);
const rejectReason = ref('');

const filterTabs = [
  { label: 'Tất cả', value: '' },
  { label: 'Chờ duyệt', value: 'pending_review' },
  { label: 'Đã duyệt', value: 'published' },
  { label: 'Từ chối', value: 'rejected' },
];

function formatPrice(v) { return new Intl.NumberFormat('vi-VN').format(v); }
function statusLabel(s) { return { pending_review: 'Chờ duyệt', active: 'Đã duyệt', rejected: 'Từ chối', published: 'Đã duyệt', draft: 'Nháp', archived: 'Lưu trữ' }[s] || s; }

function statusBadgeStyle(s) {
  const map = {
    pending_review: 'background: #fef3c7; color: #f59e0b;',
    active: 'background: #dcfce7; color: #22c55e;',
    published: 'background: #dcfce7; color: #22c55e;',
    rejected: 'background: #fef2f2; color: #ef4444;',
    published: 'background: #d1fae5; color: #059669;',
    draft: 'background: #fef3c7; color: #d97706;',
    archived: 'background: #f5f5f4; color: #78716c;',
  };
  return map[s] || 'background: #f5f5f4; color: #78716c;';
}

function filter() {
  router.get(route('admin.agriverse.products.index'), { search: search.value, status: statusFilter.value, category: categoryFilter.value }, { preserveState: true });
}

function destroy(id) {
  if (confirm('Xóa sản phẩm này?')) router.delete(route('admin.agriverse.products.destroy', id));
}

function approve(id) {
  if (confirm('Duyệt sản phẩm này?')) router.post(route('admin.agriverse.products.approve', id));
}

function openRejectModal(product) {
  rejectTarget.value = product;
  rejectReason.value = '';
  showRejectModal.value = true;
}

function confirmReject() {
  if (!rejectTarget.value || !rejectReason.value.trim()) return;
  router.post(route('admin.agriverse.products.reject', rejectTarget.value.id), { reject_reason: rejectReason.value }, {
    onFinish: () => { showRejectModal.value = false; rejectTarget.value = null; rejectReason.value = ''; },
  });
}
</script>

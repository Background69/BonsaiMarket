<template>
  <AdminLayout>
    <div class="flex items-center justify-between mb-4">
      <div>
        <h1 class="text-base font-bold text-stone-800">Hộ chiếu NFT Bonsai</h1>
        <p class="text-xs text-stone-500 mt-0.5">Quản lý cây, cấp phát hộ chiếu Soulbound Token (SBT) trên Polygon
          Amoy.</p>
      </div>
      <Link :href="route('admin.agriverse.bonsais.create')"
            class="h-9 px-4 rounded-lg bg-emerald-600 text-white text-xs font-bold hover:bg-emerald-700 transition-all flex items-center gap-1.5">
        <span class="material-symbols-outlined text-base">add</span> Thêm cây Bonsai
      </Link>
    </div>

    <div v-if="flash.success"
         class="mb-3 px-4 py-3 bg-emerald-50 border border-emerald-200 text-emerald-700 rounded-lg text-xs font-semibold">
      {{ flash.success }}
    </div>
    <div v-if="flash.error"
         class="mb-3 px-4 py-3 bg-red-50 border border-red-200 text-red-700 rounded-lg text-xs font-semibold">
      {{ flash.error }}
    </div>

    <div class="bg-white rounded-xl border border-stone-200 overflow-hidden">
      <div class="p-3 border-b border-stone-100 flex flex-wrap items-center gap-2">
        <select v-model="statusFilter" class="h-8 px-2 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"
                @change="filter">
          <option value="">Tất cả trạng thái</option>
          <option v-for="s in statuses" :key="s" :value="s">{{ statusLabel(s) }}</option>
        </select>
        <input v-model="search" class="h-8 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500" placeholder="Tên / mã / loài / ví..."
               @keyup.enter="filter">
        <button class="h-8 px-3 rounded-lg border border-stone-300 text-stone-600 text-xs font-semibold hover:bg-stone-50"
                @click="filter">
          Lọc
        </button>
      </div>

      <table class="w-full text-xs">
        <thead>
        <tr class="bg-stone-50 text-stone-500 text-left">
          <th class="p-3 font-medium">Mã</th>
          <th class="p-3 font-medium">Cây</th>
          <th class="p-3 font-medium">Loài</th>
          <th class="p-3 font-medium">Chủ sở hữu</th>
          <th class="p-3 font-medium">Token</th>
          <th class="p-3 font-medium">Trạng thái</th>
          <th class="p-3 font-medium"></th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="b in bonsais.data" :key="b.id" class="border-t border-stone-100 hover:bg-stone-50 transition-colors">
          <td class="p-3 font-mono text-stone-600">{{ b.code }}</td>
          <td class="p-3 font-semibold text-stone-800">{{ b.name }}</td>
          <td class="p-3 text-stone-600">{{ b.species || '—' }}</td>
          <td class="p-3">
            <div class="text-stone-700 font-medium">{{ b.owner_name || '—' }}</div>
            <div class="font-mono text-[10px] text-stone-400">{{ b.owner_wallet || '—' }}</div>
          </td>
          <td class="p-3">
            <span v-if="b.is_minted"
                  class="px-2 py-0.5 rounded-lg bg-emerald-50 text-emerald-700 font-bold">#{{ b.token_id }}</span>
            <span v-else class="text-stone-400">Chưa mint</span>
          </td>
          <td class="p-3"><span :class="statusClass(b.status)"
                                class="text-[10px] px-1.5 py-0.5 rounded-full font-semibold">{{ statusLabel(b.status) }}</span></td>
          <td class="p-3 text-right">
            <Link :href="route('admin.agriverse.bonsais.show', b.id)" class="text-emerald-600 hover:text-emerald-800">
              Chi tiết
            </Link>
          </td>
        </tr>
        <tr v-if="!bonsais.data.length">
          <td class="p-8 text-center text-stone-400" colspan="7">Chưa có cây nào.
            <Link :href="route('admin.agriverse.bonsais.create')" class="text-emerald-600 font-semibold">Thêm cây đầu
              tiên
            </Link>
          </td>
        </tr>
        </tbody>
      </table>
      <div class="p-3 border-t border-stone-100 flex items-center justify-between text-xs text-stone-500">
        <span>Trang {{ bonsais.current_page }}/{{ bonsais.last_page }}</span>
        <div class="flex gap-1">
          <Link v-for="link in bonsais.links" :key="link.label" :class="{ 'bg-emerald-600 text-white': link.active }" :href="link.url || '#'"
                class="px-2 py-1 rounded border border-stone-200 hover:bg-emerald-50"
                v-html="link.label"/>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import {ref, computed} from 'vue';
import {Link, router, usePage} from '@inertiajs/vue3';
import {route} from 'ziggy-js';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';

const props = defineProps({bonsais: Object, status: String, search: String, statuses: Array});

const page = usePage();
const flash = computed(() => page.props.flash || {});
const statusFilter = ref(props.status || '');
const search = ref(props.search || '');

function statusLabel(s) {
  return {draft: 'Draft', pending: 'Chờ duyệt', approved: 'Đã duyệt', rejected: 'Từ chối'}[s] || s;
}

function statusClass(s) {
  return {
    draft: 'bg-gray-100 text-gray-600',
    pending: 'bg-amber-50 text-amber-600',
    approved: 'bg-emerald-50 text-emerald-600',
    rejected: 'bg-red-50 text-red-600'
  }[s] || '';
}

function filter() {
  router.get(route('admin.agriverse.bonsais.index'), {
    status: statusFilter.value,
    search: search.value
  }, {preserveState: true, replace: true});
}
</script>

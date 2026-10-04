<template>
  <AdminLayout>
    <div class="py-6 px-5">
      <div class="flex items-center justify-between mb-4">
        <h1 class="text-lg font-bold" style="color: var(--ag-text-primary);">Người bán</h1>
      </div>

      <div class="bg-white rounded-2xl border border-[var(--ag-border)] overflow-hidden">
        <!-- Filters -->
        <div class="p-4 border-b border-[var(--ag-border)] flex gap-3">
          <select v-model="statusFilter" @change="filter"
            class="h-9 px-3 rounded-xl border border-[var(--ag-border)] text-xs outline-none focus:border-[var(--ag-primary-500)] bg-white">
            <option value="">Tất cả trạng thái</option>
            <option value="pending">Chờ duyệt</option>
            <option value="approved">Đã duyệt</option>
            <option value="rejected">Từ chối</option>
          </select>
        </div>

        <!-- Table -->
        <table class="w-full text-sm">
          <thead>
            <tr class="bg-[var(--ag-bg)] text-[var(--ag-text-muted)] text-left">
              <th class="p-4 font-medium text-xs">ID</th>
              <th class="p-4 font-medium text-xs">Người dùng</th>
              <th class="p-4 font-medium text-xs">Email</th>
              <th class="p-4 font-medium text-xs">Số điện thoại</th>
              <th class="p-4 font-medium text-xs">Trạng thái</th>
              <th class="p-4 font-medium text-xs">Ngày gửi</th>
              <th class="p-4 font-medium text-xs"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="v in verifications.data" :key="v.id"
              class="border-t border-[var(--ag-border)] hover:bg-[var(--ag-neutral-50)] transition-colors">
              <td class="p-4 text-[var(--ag-text-muted)]">{{ v.id }}</td>
              <td class="p-4">
                <div class="flex items-center gap-3">
                  <div class="w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold" style="background: var(--ag-primary-500); color: white;">
                    {{ v.user?.name?.charAt(0)?.toUpperCase() || '?' }}
                  </div>
                  <span class="font-semibold text-[var(--ag-text-primary)]">{{ v.user?.name || '—' }}</span>
                </div>
              </td>
              <td class="p-4 text-[var(--ag-text-secondary)]">{{ v.email || v.user?.email || '—' }}</td>
              <td class="p-4 text-[var(--ag-text-secondary)]">{{ v.phone || '—' }}</td>
              <td class="p-4">
                <span class="text-[11px] px-2.5 py-1 rounded-full font-semibold" :class="statusClass(v.status)">
                  {{ statusLabel(v.status) }}
                </span>
              </td>
              <td class="p-4 text-[var(--ag-text-muted)] text-xs">{{ v.created_at ? new Date(v.created_at).toLocaleDateString('vi-VN') : '—' }}</td>
              <td class="p-4 text-right">
                <Link :href="route('admin.agriverse.sellers.show', v.id)"
                  class="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all"
                  style="color: var(--ag-primary-500); background: color-mix(in srgb, var(--ag-primary-500) 8%, transparent);"
                  hover-style="background: color-mix(in srgb, var(--ag-primary-500) 14%, transparent);">
                  <span class="material-symbols-outlined text-sm">visibility</span>
                  Xem
                </Link>
              </td>
            </tr>
            <tr v-if="!verifications.data?.length">
              <td colspan="7" class="p-16 text-center text-[var(--ag-text-secondary)]">
                Chưa có yêu cầu đăng ký người bán nào.
              </td>
            </tr>
          </tbody>
        </table>

        <!-- Pagination -->
        <div v-if="verifications.last_page > 1" class="p-4 border-t border-[var(--ag-border)] flex items-center justify-between text-xs text-[var(--ag-text-muted)]">
          <span>Trang {{ verifications.current_page }} / {{ verifications.last_page }}</span>
          <div class="flex gap-1">
            <Link v-for="link in verifications.links" :key="link.label"
              :href="link.url || '#'"
              class="px-2.5 py-1.5 rounded-lg border border-[var(--ag-border)] hover:bg-[var(--ag-primary-50)] text-xs"
              :class="{ 'bg-[var(--ag-primary-500)] text-white border-[var(--ag-primary-500)]': link.active }"
              v-html="link.label" />
          </div>
        </div>
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

const props = defineProps({ verifications: Object, filter: Object });
const statusFilter = ref(props.filter?.status || '');

function filter() {
  router.get(route('admin.agriverse.sellers.index'), { status: statusFilter.value }, { preserveState: true });
}
</script>

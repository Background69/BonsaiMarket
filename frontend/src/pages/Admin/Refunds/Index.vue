<template>
  <AdminLayout>
    <div class="py-8 px-5">
      <h1 class="text-xl font-bold text-[var(--ag-text-primary)] mb-6">Yêu cầu hoàn tiền</h1>
      <div v-if="refunds.data?.length" class="space-y-3">
        <div v-for="r in refunds.data" :key="r.id" class="bg-white rounded-2xl border border-[var(--ag-border)] p-4 hover:border-[var(--ag-primary-500)]/25 transition-all">
          <Link :href="route('admin.agriverse.refunds.show', r.id)" class="flex items-center gap-4">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2">
                <span class="text-sm font-bold text-[var(--ag-text-primary)]">{{ r.order?.product?.name }}</span>
                <span class="text-xs px-2 py-0.5 rounded-full font-semibold" :class="statusClass(r.status)">
                  {{ statusLabel(r.status) }}
                </span>
              </div>
              <div class="text-sm text-[var(--ag-text-muted)] mt-1">
                {{ r.user?.name }} — {{ formatPrice(r.amount) }}₫ — {{ r.reason }}
              </div>
            </div>
            <span class="material-symbols-outlined text-[var(--ag-neutral-300)]">chevron_right</span>
          </Link>
        </div>
      </div>
      <div v-else class="bg-white rounded-2xl border border-[var(--ag-border)] p-16 text-center text-[var(--ag-text-secondary)]">
        Chưa có yêu cầu hoàn tiền nào.
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { Link } from '@inertiajs/vue3';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';
import { formatPrice, statusLabel, statusClass } from '@agriverse/utils';
defineProps({ refunds: Object });
</script>

<template>
  <AdminLayout>
    <div class="max-w-3xl mx-auto py-8 px-5 space-y-5">
      <!-- User Info Card -->
      <div class="bg-white rounded-2xl border border-[var(--ag-border)] p-6">
        <div class="flex items-center justify-between mb-5">
          <h1 class="text-lg font-bold" style="color: var(--ag-text-primary);">Chi tiết yêu cầu</h1>
          <span class="text-xs px-3 py-1 rounded-full font-semibold" :class="statusClass(verification.status)">
            {{ statusLabel(verification.status) }}
          </span>
        </div>

        <div class="flex items-center gap-4 mb-6 pb-5 border-b border-[var(--ag-border)]">
          <div class="w-14 h-14 rounded-full flex items-center justify-center text-lg font-bold" style="background: var(--ag-primary-500); color: white;">
            {{ verification.user?.name?.charAt(0)?.toUpperCase() || '?' }}
          </div>
          <div>
            <div class="text-base font-bold" style="color: var(--ag-text-primary);">{{ verification.user?.name || '—' }}</div>
            <div class="text-sm" style="color: var(--ag-text-secondary);">{{ verification.user?.email || '—' }}</div>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-4 text-sm">
          <div class="p-3 rounded-xl" style="background: var(--ag-bg);">
            <div class="text-xs" style="color: var(--ag-text-muted);">Số CMND/CCCD</div>
            <div class="font-semibold mt-0.5" style="color: var(--ag-text-primary);">{{ verification.id_card_number || '—' }}</div>
          </div>
          <div class="p-3 rounded-xl" style="background: var(--ag-bg);">
            <div class="text-xs" style="color: var(--ag-text-muted);">Số điện thoại</div>
            <div class="font-semibold mt-0.5" style="color: var(--ag-text-primary);">{{ verification.phone || '—' }}</div>
          </div>
          <div class="p-3 rounded-xl" style="background: var(--ag-bg);">
            <div class="text-xs" style="color: var(--ag-text-muted);">Email</div>
            <div class="font-semibold mt-0.5" style="color: var(--ag-text-primary);">{{ verification.email || '—' }}</div>
          </div>
          <div class="p-3 rounded-xl" style="background: var(--ag-bg);">
            <div class="text-xs" style="color: var(--ag-text-muted);">Ngày gửi</div>
            <div class="font-semibold mt-0.5" style="color: var(--ag-text-primary);">{{ verification.created_at ? new Date(verification.created_at).toLocaleDateString('vi-VN') : '—' }}</div>
          </div>
        </div>
      </div>

      <!-- Images Card -->
      <div class="bg-white rounded-2xl border border-[var(--ag-border)] p-6">
        <h2 class="text-sm font-bold mb-4" style="color: var(--ag-text-primary);">Hình ảnh giấy tờ</h2>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <div class="text-xs font-semibold mb-2" style="color: var(--ag-text-muted);">Ảnh chân dung</div>
            <div v-if="verification.portrait_image" class="rounded-xl overflow-hidden border border-[var(--ag-border)]">
              <img :src="'/storage/' + verification.portrait_image" class="w-full h-48 object-cover" />
            </div>
            <div v-else class="rounded-xl border border-dashed border-[var(--ag-border)] h-48 flex items-center justify-center text-xs" style="color: var(--ag-text-muted);">
              Chưa tải lên
            </div>
          </div>
          <div>
            <div class="text-xs font-semibold mb-2" style="color: var(--ag-text-muted);">CMND/CCCD mặt trước</div>
            <div v-if="verification.id_card_image_front" class="rounded-xl overflow-hidden border border-[var(--ag-border)]">
              <img :src="'/storage/' + verification.id_card_image_front" class="w-full h-48 object-cover" />
            </div>
            <div v-else class="rounded-xl border border-dashed border-[var(--ag-border)] h-48 flex items-center justify-center text-xs" style="color: var(--ag-text-muted);">
              Chưa tải lên
            </div>
          </div>
          <div>
            <div class="text-xs font-semibold mb-2" style="color: var(--ag-text-muted);">CMND/CCCD mặt sau</div>
            <div v-if="verification.id_card_image_back" class="rounded-xl overflow-hidden border border-[var(--ag-border)]">
              <img :src="'/storage/' + verification.id_card_image_back" class="w-full h-48 object-cover" />
            </div>
            <div v-else class="rounded-xl border border-dashed border-[var(--ag-border)] h-48 flex items-center justify-center text-xs" style="color: var(--ag-text-muted);">
              Chưa tải lên
            </div>
          </div>
        </div>
      </div>

      <!-- Reject reason (if rejected) -->
      <div v-if="verification.status === 'rejected' && verification.reject_reason" class="bg-white rounded-2xl border border-red-200 p-6" style="background: color-mix(in srgb, #dc2626 4%, transparent);">
        <div class="flex items-start gap-3">
          <span class="material-symbols-outlined text-xl" style="color: #dc2626;">info</span>
          <div>
            <h3 class="text-sm font-bold" style="color: var(--ag-text-primary);">Lý do từ chối</h3>
            <p class="text-sm mt-1" style="color: var(--ag-text-secondary);">{{ verification.reject_reason }}</p>
          </div>
        </div>
      </div>

      <!-- Approve/Reject Actions -->
      <div v-if="verification.status === 'pending'" class="bg-white rounded-2xl border border-[var(--ag-border)] p-6">
        <h3 class="text-sm font-bold mb-4" style="color: var(--ag-text-primary);">Xử lý yêu cầu</h3>
        <textarea v-model="note" placeholder="Ghi chú (bắt buộc nếu từ chối)..."
          class="w-full h-24 px-4 py-3 rounded-xl border-2 border-[var(--ag-border)] text-sm outline-none resize-none focus:border-[var(--ag-primary-500)]/40 focus:ring-4 focus:ring-[var(--ag-primary-500)]/8" />
        <div class="flex gap-3 mt-4">
          <button @click="approve" class="h-11 px-6 rounded-2xl text-white text-sm font-semibold transition-all" style="background: var(--ag-primary-500);" hover-style="background: var(--ag-primary-600);">
            Duyệt người bán
          </button>
          <button @click="reject" class="h-11 px-6 rounded-2xl text-white text-sm font-semibold transition-all" style="background: var(--ag-danger);" hover-style="background: #A0122E;">
            Từ chối
          </button>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref } from 'vue';
import { router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';
import { statusLabel, statusClass } from '@agriverse/utils';

const props = defineProps({ verification: Object });
const note = ref('');

function approve() {
  router.post(route('admin.agriverse.sellers.approve', props.verification.id), {}, { preserveState: true });
}

function reject() {
  if (!note.value) { alert('Vui lòng nhập ghi chú khi từ chối.'); return; }
  router.post(route('admin.agriverse.sellers.reject', props.verification.id), { reject_reason: note.value }, { preserveState: true });
}
</script>

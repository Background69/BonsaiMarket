<template>
  <AdminLayout>
    <div class="flex items-center justify-between mb-4">
      <div class="flex items-center gap-3">
        <Link :href="route('admin.agriverse.chat-groups.index')" class="text-stone-400 hover:text-stone-600">
          <span class="material-symbols-outlined" style="font-size: 20px;">arrow_back</span>
        </Link>
        <h1 class="text-base font-bold text-stone-800">{{ group.name }}</h1>
        <span class="text-[11px] px-2 py-0.5 rounded-full bg-stone-100 text-stone-500 font-medium">{{ group.member_count }} thành viên</span>
      </div>
      <button @click="destroy" class="h-8 px-3 rounded-lg text-xs font-semibold text-red-500 border border-red-200 hover:bg-red-50">Xóa nhóm</button>
    </div>

    <!-- Pending Members -->
    <div v-if="pendingMembers.length" class="bg-amber-50 border border-amber-200 rounded-xl p-4 mb-6">
      <h3 class="text-xs font-bold text-amber-700 mb-3 flex items-center gap-2">
        <span class="material-symbols-outlined" style="font-size: 16px;">pending_actions</span>
        Yêu cầu tham gia ({{ pendingMembers.length }})
      </h3>
      <div class="flex flex-wrap gap-2">
        <div v-for="m in pendingMembers" :key="m.id"
          class="flex items-center gap-2 bg-white rounded-lg px-3 py-2 border border-amber-100">
          <div class="w-7 h-7 rounded-full flex items-center justify-center text-[11px] font-bold"
            style="background: color-mix(in srgb, var(--ag-primary-500) 10%, transparent); color: var(--ag-primary-500);">
            {{ m.name?.charAt(0)?.toUpperCase() }}
          </div>
          <span class="text-xs font-medium text-stone-700">{{ m.name }}</span>
          <button @click="approveMember(m.id)"
            class="ml-2 px-2.5 py-1 rounded-lg text-[10px] font-bold text-white"
            style="background: var(--ag-primary-500);">
            Duyệt
          </button>
          <button @click="rejectMember(m.id)"
            class="px-2.5 py-1 rounded-lg text-[10px] font-bold text-red-500 border border-red-200 hover:bg-red-50">
            Từ chối
          </button>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- Messages -->
      <div class="lg:col-span-2 bg-white rounded-xl border border-stone-200 overflow-hidden flex flex-col" style="min-height: 500px;">
        <div class="px-4 py-3 border-b border-stone-100 flex items-center justify-between">
          <h3 class="text-xs font-bold text-stone-600">Tin nhắn</h3>
          <span class="text-[10px] text-stone-400">{{ messages.length }} tin nhắn</span>
        </div>
        <div class="flex-1 overflow-y-auto p-4 space-y-3" style="max-height: 500px;">
          <div v-for="m in messages" :key="m.id" class="flex" :class="m.sender_id === userId ? 'justify-end' : 'justify-start'">
            <div class="max-w-[80%] rounded-2xl px-4 py-2.5"
              :class="m.sender_id === userId
                ? 'rounded-br-md'
                : 'rounded-bl-md'"
              :style="m.sender_id === userId
                ? 'background: color-mix(in srgb, var(--ag-primary-500) 12%, transparent);'
                : 'background: var(--ag-neutral-100);'">
              <p v-if="m.sender_id !== userId" class="text-[10px] font-semibold mb-0.5" style="color: var(--ag-primary-500);">{{ m.sender_name }}</p>
              <p class="text-sm text-stone-800">{{ m.message }}</p>
              <p class="text-[10px] text-stone-400 mt-1 text-right">{{ formatTime(m.created_at) }}</p>
            </div>
          </div>
          <div v-if="!messages.length" class="text-center py-12 text-stone-400 text-xs">Chưa có tin nhắn nào</div>
        </div>
      </div>

      <!-- Members -->
      <div class="bg-white rounded-xl border border-stone-200 overflow-hidden">
        <div class="px-4 py-3 border-b border-stone-100">
          <h3 class="text-xs font-bold text-stone-600">Thành viên</h3>
        </div>
        <div class="p-3 space-y-1">
          <div v-for="m in members" :key="m.id"
            class="flex items-center gap-2 px-2 py-1.5 rounded-lg hover:bg-stone-50">
            <div class="w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-bold"
              style="background: color-mix(in srgb, var(--ag-primary-500) 10%, transparent); color: var(--ag-primary-500);">
              {{ m.name?.charAt(0)?.toUpperCase() }}
            </div>
            <span class="text-xs font-medium text-stone-700">{{ m.name }}</span>
          </div>
          <div v-if="!members.length" class="text-center py-6 text-stone-400 text-xs">Chưa có thành viên</div>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { Link, router, usePage } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';

const props = defineProps({
  group: Object,
  members: Array,
  pendingMembers: Array,
  messages: Array,
});

const userId = usePage().props.auth?.user?.id;

function formatTime(d) {
  if (!d) return '';
  const date = new Date(d);
  return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
}

function approveMember(userId) {
  router.post(route('admin.agriverse.chat-groups.approve-member', [props.group.id, userId]));
}

function rejectMember(userId) {
  router.post(route('admin.agriverse.chat-groups.reject-member', [props.group.id, userId]));
}

function destroy() {
  if (confirm('Xóa nhóm chat này?')) {
    router.delete(route('admin.agriverse.chat-groups.destroy', props.group.id));
  }
}
</script>

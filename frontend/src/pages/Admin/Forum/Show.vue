<template>
  <AdminLayout>
    <div class="max-w-3xl mx-auto py-8 px-5 space-y-5">
      <Link :href="route('admin.agriverse.forum.index')" class="inline-flex items-center gap-1 text-xs text-emerald-600 hover:text-emerald-800">
        <span class="material-symbols-outlined text-sm">arrow_back</span> Quay lại
      </Link>

      <div class="bg-white rounded-2xl border border-stone-200 p-6">
        <div class="flex items-start justify-between mb-4">
          <h1 class="text-lg font-bold text-stone-800">{{ post.title || '—' }}</h1>
          <span class="text-xs px-3 py-1 rounded-full font-semibold" :class="statusClass(post.status)">{{ statusLabel(post.status) }}</span>
        </div>

        <div class="flex items-center gap-3 mb-5 pb-4 border-b border-stone-100">
          <div class="w-9 h-9 rounded-full flex items-center justify-center text-xs font-bold text-white" style="background: var(--ag-primary-500);">
            {{ post.user?.name?.charAt(0)?.toUpperCase() || '?' }}
          </div>
          <div>
            <div class="text-sm font-semibold text-stone-800">{{ post.user?.name || 'Khách' }}</div>
            <div class="text-xs text-stone-400">{{ formatDate(post.created_at) }}</div>
          </div>
        </div>

        <!-- Category & Stats -->
        <div class="flex flex-wrap items-center gap-3 mb-4 text-xs">
          <span v-if="post.category" class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-sky-50 text-sky-600 font-semibold">
            <span class="material-symbols-outlined text-xs">folder</span> {{ post.category }}
          </span>
          <span class="text-stone-400"><span class="material-symbols-outlined text-xs align-middle">chat_bubble</span> {{ post.comments_count ?? 0 }} bình luận</span>
          <span class="text-stone-400"><span class="material-symbols-outlined text-xs align-middle">favorite</span> {{ post.likes_count ?? 0 }} lượt thích</span>
          <span v-if="post.is_pinned" class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-amber-50 text-amber-600 font-semibold">
            <span class="material-symbols-outlined text-xs">push_pin</span> Đã ghim
          </span>
        </div>

        <div class="text-sm text-stone-700 leading-relaxed whitespace-pre-wrap">{{ post.content || '—' }}</div>
      </div>

      <div v-if="post.reject_reason" class="bg-red-50 border border-red-200 rounded-2xl p-4">
        <div class="flex items-start gap-2">
          <span class="material-symbols-outlined text-lg text-red-500">info</span>
          <div>
            <h3 class="text-sm font-bold text-stone-800">Lý do từ chối</h3>
            <p class="text-sm text-stone-600 mt-1">{{ post.reject_reason }}</p>
          </div>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { Link } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';
import { statusLabel, statusClass } from '@agriverse/utils';

const props = defineProps({ post: Object });

function formatDate(d) { return d ? new Date(d).toLocaleDateString('vi-VN') : '—'; }
</script>

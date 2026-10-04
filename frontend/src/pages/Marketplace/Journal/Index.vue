<template>
  <MarketplaceLayout>
    <section class="max-w-[1280px] mx-auto px-5 py-8">
      <!-- Breadcrumb -->
      <nav class="flex items-center gap-2 text-sm text-[var(--ag-text-muted)] mb-6">
        <Link :href="route('agriverse.shop.home')" class="hover:text-[var(--ag-primary-500)] transition-colors">Trang chủ</Link>
        <span class="material-symbols-outlined text-sm">chevron_right</span>
        <span class="text-[var(--ag-text-primary)] font-medium">Bài viết</span>
      </nav>

      <!-- Header -->
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 class="text-2xl sm:text-3xl font-[500] text-[var(--ag-text-primary)] tracking-tight" style="font-family:var(--ag-font-display)">Bài viết & Kiến thức</h1>
          <p class="text-sm text-[var(--ag-text-secondary)] mt-1">Khám phá kiến thức chuyên sâu về cây cảnh và bonsai</p>
        </div>
        <div class="relative w-full sm:w-72">
          <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-sm text-[var(--ag-text-muted)]">search</span>
          <input
            v-model="searchQuery"
            type="text"
            placeholder="Tìm bài viết..."
            class="w-full h-10 pl-9 pr-4 rounded-xl border border-[var(--ag-border)] bg-white text-sm text-[var(--ag-text-primary)] outline-none transition-all focus:border-[var(--ag-primary-500)] focus:ring-2 focus:ring-[var(--ag-primary-500)]/10"
            @input="debouncedSearch"
          />
        </div>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="flex justify-center py-20">
        <div class="w-8 h-8 border-2 border-[var(--ag-primary-300)]/30 border-t-[var(--ag-primary-500)] rounded-full animate-spin"></div>
      </div>

      <!-- Results -->
      <template v-if="!loading">
        <div v-if="articles.length" class="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          <Link
            v-for="article in articles"
            :key="article.id"
            :href="route('agriverse.shop.journal.show', article.slug || article.id)"
            class="group bg-white rounded-2xl border border-[var(--ag-border)] overflow-hidden transition-all duration-300 hover:shadow-lg hover:border-[var(--ag-primary-500)]/20 hover:-translate-y-0.5"
          >
            <div class="aspect-[16/9] bg-[var(--ag-bg)] overflow-hidden">
              <img
                v-if="article.hero_image_url"
                :src="article.hero_image_url"
                :alt="article.title"
                class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
              />
              <div v-else class="w-full h-full flex items-center justify-center">
                <span class="material-symbols-outlined text-4xl text-[var(--ag-text-muted)]">article</span>
              </div>
            </div>
            <div class="p-5">
              <div class="flex items-center gap-2 mb-3">
                <span v-if="article.tag" class="text-[10px] px-2.5 py-1 rounded-full font-semibold bg-[var(--ag-primary-500)]/10 text-[var(--ag-primary-500)]">{{ article.tag }}</span>
                <span v-if="article.is_peer_reviewed" class="text-[10px] px-2.5 py-1 rounded-full font-semibold bg-[var(--ag-accent-500)]/10 text-[var(--ag-accent-500)]">Đã bình duyệt</span>
              </div>
              <h3 class="font-[500] text-[var(--ag-text-primary)] line-clamp-2 group-hover:text-[var(--ag-primary-500)] transition-colors leading-snug" style="font-family:var(--ag-font-body)">{{ article.title }}</h3>
              <p v-if="article.abstract" class="text-sm text-[var(--ag-text-secondary)] mt-2 line-clamp-2">{{ article.abstract }}</p>
              <div class="flex items-center gap-4 mt-4 text-xs text-[var(--ag-text-muted)]">
                <span v-if="article.author_name">
                  <span class="material-symbols-outlined text-[10px] align-text-bottom">person</span>
                  {{ article.author_name }}
                </span>
                <span v-if="article.published_at">
                  <span class="material-symbols-outlined text-[10px] align-text-bottom">calendar_today</span>
                  {{ formatDate(article.published_at) }}
                </span>
                <span v-if="article.read_time_minutes">
                  <span class="material-symbols-outlined text-[10px] align-text-bottom">schedule</span>
                  {{ article.read_time_minutes }} phút
                </span>
              </div>
            </div>
          </Link>
        </div>

        <div v-else class="text-center py-20">
          <span class="material-symbols-outlined text-5xl text-[var(--ag-text-muted)] mb-4">menu_book</span>
          <p class="text-[var(--ag-text-secondary)]">Chưa có bài viết nào.</p>
        </div>
      </template>

      <!-- Pagination -->
      <div v-if="pagination.last_page > 1" class="flex justify-center mt-10 gap-2">
        <button
          :disabled="pagination.current_page <= 1"
          class="h-9 px-4 rounded-xl border border-[var(--ag-border)] text-sm font-medium text-[var(--ag-text-secondary)] disabled:opacity-40 hover:border-[var(--ag-primary-500)] hover:text-[var(--ag-primary-500)] transition-all"
          @click="goToPage(pagination.current_page - 1)"
        >
          Trước
        </button>
        <span class="h-9 px-3 flex items-center text-sm text-[var(--ag-text-secondary)]">
          {{ pagination.current_page }} / {{ pagination.last_page }}
        </span>
        <button
          :disabled="pagination.current_page >= pagination.last_page"
          class="h-9 px-4 rounded-xl border border-[var(--ag-border)] text-sm font-medium text-[var(--ag-text-secondary)] disabled:opacity-40 hover:border-[var(--ag-primary-500)] hover:text-[var(--ag-primary-500)] transition-all"
          @click="goToPage(pagination.current_page + 1)"
        >
          Sau
        </button>
      </div>
    </section>
  </MarketplaceLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { Link, router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';

const props = defineProps({
  articles: { type: Array, default: () => [] },
  pagination: {
    type: Object,
    default: () => ({ current_page: 1, last_page: 1, total: 0 }),
  },
  search: { type: String, default: '' },
});

const loading = ref(false);
const searchQuery = ref(props.search || '');
let debounceTimer = null;

function formatDate(date) {
  if (!date) return '';
  return new Date(date).toLocaleDateString('vi-VN', {
    year: 'numeric', month: 'long', day: 'numeric',
  });
}

function debouncedSearch() {
  clearTimeout(debounceTimer);
  debounceTimer = setTimeout(() => {
    router.get(
      route('agriverse.shop.journal.index'),
      { search: searchQuery.value || undefined },
      { preserveState: true, preserveScroll: true }
    );
  }, 400);
}

function goToPage(page) {
  router.get(
    route('agriverse.shop.journal.index'),
    { search: searchQuery.value || undefined, page },
    { preserveState: true, preserveScroll: true }
  );
}
</script>

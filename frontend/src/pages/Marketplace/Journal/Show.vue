<template>
  <MarketplaceLayout>
    <!-- Reading progress bar -->
    <div class="fixed top-0 left-0 h-0.5 bg-[var(--ag-primary-500)] z-50 transition-all duration-150" :style="{ width: readingProgress + '%' }"></div>

    <article class="max-w-[840px] mx-auto px-5 py-8">
      <nav class="flex items-center gap-2 text-sm text-[var(--ag-text-muted)] mb-8">
        <Link :href="route('agriverse.shop.home')" class="hover:text-[var(--ag-primary-500)] transition-colors">Trang chủ</Link>
        <span class="material-symbols-outlined text-sm">chevron_right</span>
        <Link :href="route('agriverse.shop.journal.index')" class="hover:text-[var(--ag-primary-500)] transition-colors">Bài viết</Link>
        <span class="material-symbols-outlined text-sm">chevron_right</span>
        <span class="text-[var(--ag-text-primary)] font-medium truncate">{{ article?.title || 'Bài viết' }}</span>
      </nav>

      <div v-if="!article" class="text-center py-20">
        <span class="material-symbols-outlined text-5xl text-[var(--ag-text-muted)] mb-4">menu_book</span>
        <p class="text-[var(--ag-text-secondary)] mb-4">Không tìm thấy bài viết.</p>
        <Link :href="route('agriverse.shop.journal.index')" class="text-sm text-[var(--ag-primary-500)] hover:underline">Quay lại danh sách</Link>
      </div>

      <template v-if="article">
        <!-- Hero -->
        <div class="relative mb-10 rounded-2xl overflow-hidden bg-[var(--ag-bg)] shadow-sm" :class="{ 'aspect-[16/7]': article.hero_image_url }">
          <img v-if="article.hero_image_url" :src="article.hero_image_url" :alt="article.title" class="w-full h-full object-cover" />
          <div v-if="article.hero_image_url" class="absolute inset-0 bg-gradient-to-t from-black/40 via-transparent to-transparent"></div>
          <div class="absolute bottom-0 left-0 right-0 p-6 sm:p-8">
            <div class="flex items-center gap-2 mb-3">
              <span v-if="article.tag" class="text-[11px] px-3 py-1 rounded-full font-semibold bg-white/20 backdrop-blur-md text-white border border-white/20">{{ article.tag }}</span>
              <span v-if="article.is_peer_reviewed" class="text-[11px] px-3 py-1 rounded-full font-semibold bg-[var(--ag-accent-500)]/80 text-white">Đã bình duyệt</span>
            </div>
          </div>
        </div>

        <!-- Header -->
        <header class="mb-8">
          <div v-if="!article.hero_image_url" class="flex items-center gap-3 mb-5">
            <span v-if="article.tag" class="text-xs px-3 py-1 rounded-full font-semibold bg-[var(--ag-primary-500)]/10 text-[var(--ag-primary-500)]">{{ article.tag }}</span>
            <span v-if="article.is_peer_reviewed" class="text-xs px-2 py-0.5 rounded-full bg-[var(--ag-accent-500)]/10 text-[var(--ag-accent-500)] font-semibold">Đã bình duyệt</span>
          </div>

          <h1 class="text-3xl sm:text-4xl lg:text-5xl font-[600] text-[var(--ag-text-primary)] tracking-tight leading-[1.1] mb-6" style="font-family:var(--ag-font-display)">{{ article.title }}</h1>

          <!-- Author card -->
          <div class="flex flex-wrap items-center justify-between gap-4 pb-6 border-b border-[var(--ag-border)]">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-full bg-gradient-to-br from-[var(--ag-primary-400)] to-[var(--ag-primary-600)] flex items-center justify-center text-sm font-semibold text-white shadow-sm">
                {{ authorInitial }}
              </div>
              <div>
                <p class="text-sm font-medium text-[var(--ag-text-primary)]">{{ article.author_name }}</p>
                <p v-if="article.author_role" class="text-xs text-[var(--ag-text-muted)]">{{ article.author_role }}</p>
              </div>
            </div>
            <div class="flex items-center gap-4 text-xs text-[var(--ag-text-muted)]">
              <span v-if="article.published_at" class="flex items-center gap-1">
                <span class="material-symbols-outlined text-sm">calendar_today</span>
                {{ formatDate(article.published_at) }}
              </span>
              <span v-if="article.read_time_minutes" class="flex items-center gap-1">
                <span class="material-symbols-outlined text-sm">schedule</span>
                {{ article.read_time_minutes }} phút
              </span>
            </div>
          </div>
        </header>

        <!-- Abstract -->
        <div v-if="article.abstract" class="relative mb-10 pl-6 before:content-[''] before:absolute before:left-0 before:top-0 before:bottom-0 before:w-1 before:bg-gradient-to-b before:from-[var(--ag-primary-500)] before:to-[var(--ag-primary-300)] before:rounded-full">
          <p class="text-lg text-[var(--ag-text-secondary)] leading-relaxed italic font-medium">{{ article.abstract }}</p>
        </div>

        <!-- Article body -->
        <div v-if="article.content" class="article-content text-[var(--ag-text-primary)] leading-relaxed mb-12" v-html="article.content"></div>

        <!-- Blockquote -->
        <div v-if="article.blockquote_text" class="relative bg-gradient-to-br from-[var(--ag-primary-500)]/[0.05] to-[var(--ag-primary-500)]/[0.02] rounded-2xl p-8 mb-12 border border-[var(--ag-primary-500)]/10">
          <span class="material-symbols-outlined text-3xl text-[var(--ag-primary-500)]/20 absolute top-4 left-4">format_quote</span>
          <p class="text-xl text-[var(--ag-text-primary)] leading-relaxed italic font-medium relative z-10">"{{ article.blockquote_text }}"</p>
          <p v-if="article.blockquote_author" class="text-sm text-[var(--ag-text-muted)] mt-3">— {{ article.blockquote_author }}</p>
        </div>

        <!-- Tags / metadata footer -->
        <div class="flex flex-wrap items-center gap-3 pt-6 border-t border-[var(--ag-border)] mb-12">
          <span class="text-xs text-[var(--ag-text-muted)]">Chia sẻ:</span>
          <button @click="shareFacebook" class="flex items-center gap-1.5 text-xs px-3 py-1.5 rounded-full bg-[#1877F2]/10 text-[#1877F2] hover:bg-[#1877F2]/20 transition-colors">
            <span class="material-symbols-outlined text-sm">share</span>
            Facebook
          </button>
          <button @click="shareTwitter" class="flex items-center gap-1.5 text-xs px-3 py-1.5 rounded-full bg-black/5 text-black hover:bg-black/10 transition-colors">
            <span class="material-symbols-outlined text-sm">x</span>
            Twitter
          </button>
          <button @click="copyLink" class="flex items-center gap-1.5 text-xs px-3 py-1.5 rounded-full bg-[var(--ag-primary-500)]/10 text-[var(--ag-primary-500)] hover:bg-[var(--ag-primary-500)]/20 transition-colors">
            <span class="material-symbols-outlined text-sm">link</span>
            Sao chép link
          </button>
        </div>

        <!-- Related articles suggestion -->
        <div v-if="relatedArticles.length" class="mb-12">
          <h3 class="text-lg font-[600] text-[var(--ag-text-primary)] mb-5" style="font-family:var(--ag-font-display)">Bài viết liên quan</h3>
          <div class="grid sm:grid-cols-2 gap-4">
            <Link
              v-for="rel in relatedArticles"
              :key="rel.id"
              :href="route('agriverse.shop.journal.show', rel.slug)"
              class="group flex gap-4 p-4 rounded-xl bg-[var(--ag-bg)] border border-[var(--ag-border)] hover:border-[var(--ag-primary-500)]/20 hover:shadow-sm transition-all"
            >
              <div v-if="rel.hero_image_url" class="w-20 h-20 rounded-lg overflow-hidden bg-[var(--ag-bg)] flex-shrink-0">
                <img :src="rel.hero_image_url" :alt="rel.title" class="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105" />
              </div>
              <div>
                <p class="text-sm font-medium text-[var(--ag-text-primary)] group-hover:text-[var(--ag-primary-500)] transition-colors line-clamp-2">{{ rel.title }}</p>
                <p class="text-xs text-[var(--ag-text-muted)] mt-1">{{ rel.read_time_minutes }} phút đọc</p>
              </div>
            </Link>
          </div>
        </div>
      </template>
    </article>

    <!-- Back to top -->
    <button
      v-if="showBackToTop"
      @click="scrollToTop"
      class="fixed bottom-6 right-6 w-10 h-10 rounded-full bg-[var(--ag-primary-500)] text-white shadow-lg flex items-center justify-center transition-all duration-300 hover:bg-[var(--ag-primary-600)] hover:shadow-xl hover:-translate-y-0.5 z-40"
    >
      <span class="material-symbols-outlined text-sm">arrow_upward</span>
    </button>
  </MarketplaceLayout>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { Link, router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';

const props = defineProps({
  article: { type: Object, default: null },
  recentArticles: { type: Array, default: () => [] },
});

const readingProgress = ref(0);
const showBackToTop = ref(false);

const authorInitial = computed(() => {
  if (!props.article?.author_name) return '?';
  const parts = props.article.author_name.split(' ');
  return parts[parts.length - 1]?.charAt(0)?.toUpperCase() || '?';
});

const relatedArticles = computed(() => {
  if (!props.article) return [];
  if (props.recentArticles?.length) {
    return props.recentArticles.filter(a => a.id !== props.article.id).slice(0, 4);
  }
  return [];
});

function formatDate(date) {
  if (!date) return '';
  return new Date(date).toLocaleDateString('vi-VN', {
    year: 'numeric', month: 'long', day: 'numeric',
  });
}

function shareFacebook() {
  const url = encodeURIComponent(window.location.href);
  window.open(`https://www.facebook.com/sharer/sharer.php?u=${url}`, '_blank', 'width=600,height=400');
}

function shareTwitter() {
  const text = encodeURIComponent(props.article?.title || '');
  const url = encodeURIComponent(window.location.href);
  window.open(`https://twitter.com/intent/tweet?text=${text}&url=${url}`, '_blank', 'width=600,height=400');
}

function copyLink() {
  navigator.clipboard.writeText(window.location.href);
}

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function handleScroll() {
  const scrollTop = window.scrollY;
  const docHeight = document.documentElement.scrollHeight - window.innerHeight;
  readingProgress.value = docHeight > 0 ? Math.min((scrollTop / docHeight) * 100, 100) : 0;
  showBackToTop.value = scrollTop > 400;
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true });
});

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll);
});
</script>

<style scoped>
.article-content h2 {
  font-size: 1.5rem;
  font-weight: 600;
  margin-top: 2.5rem;
  margin-bottom: 1rem;
  color: var(--ag-text-primary);
  font-family: var(--ag-font-display);
  padding-bottom: 0.5rem;
  border-bottom: 1px solid var(--ag-border);
}

.article-content h3 {
  font-size: 1.2rem;
  font-weight: 600;
  margin-top: 2rem;
  margin-bottom: 0.75rem;
  color: var(--ag-text-primary);
  font-family: var(--ag-font-display);
}

.article-content h4 {
  font-size: 1.1rem;
  font-weight: 500;
  margin-top: 1.5rem;
  margin-bottom: 0.5rem;
  color: var(--ag-text-primary);
}

.article-content p {
  margin-bottom: 1.25rem;
  line-height: 1.85;
  color: var(--ag-text-secondary);
  font-size: 1.05rem;
}

.article-content p:first-of-type::first-letter {
  font-size: 3.5em;
  font-weight: 700;
  float: left;
  line-height: 0.85;
  margin-right: 0.5rem;
  margin-top: 0.15rem;
  color: var(--ag-primary-500);
  font-family: var(--ag-font-display);
}

.article-content ul,
.article-content ol {
  margin-bottom: 1.5rem;
  padding-left: 1.5rem;
  color: var(--ag-text-secondary);
  line-height: 1.85;
}

.article-content ul {
  list-style-type: disc;
}

.article-content ol {
  list-style-type: decimal;
}

.article-content li {
  margin-bottom: 0.5rem;
}

.article-content li strong {
  color: var(--ag-text-primary);
}

.article-content strong {
  font-weight: 600;
  color: var(--ag-text-primary);
}

.article-content a {
  color: var(--ag-primary-500);
  text-decoration: underline;
  text-underline-offset: 2px;
  transition: opacity 0.2s;
}

.article-content a:hover {
  opacity: 0.8;
}

.article-content blockquote {
  border-left: 3px solid var(--ag-primary-500);
  padding-left: 1.25rem;
  margin: 1.5rem 0;
  font-style: italic;
  color: var(--ag-text-secondary);
  background: linear-gradient(to right, var(--ag-primary-500)/[0.04], transparent);
  padding: 1.25rem;
  border-radius: 0.75rem;
}

.article-content img {
  border-radius: 0.75rem;
  margin: 1.5rem auto;
  max-width: 100%;
  height: auto;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.article-content hr {
  border: none;
  border-top: 1px solid var(--ag-border);
  margin: 2rem 0;
}

@media (max-width: 640px) {
  .article-content p:first-of-type::first-letter {
    font-size: 2.5em;
  }
}
</style>

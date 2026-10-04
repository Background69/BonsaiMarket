<template>
  <MarketplaceLayout>
    <main class="forum-page">
      <header class="forum-hero">
        <h1 class="forum-hero-title">Diễn đàn cộng đồng</h1>
        <p class="forum-hero-desc">
          Nơi chia sẻ kiến thức, kinh nghiệm và kết nối những người yêu cây cảnh trên toàn quốc.
        </p>
        <div class="forum-hero-actions">
          <div class="forum-hero-search">
            <span class="material-symbols-outlined forum-search-icon">search</span>
            <input v-model="searchQuery" @keyup.enter="applyFilters" type="text" class="forum-search-input" placeholder="Tìm kiếm bài viết..." />
          </div>
          <Link :href="route('agriverse.shop.forum.create')" class="forum-create-btn" v-if="$page.props.auth?.user">
            <span class="material-symbols-outlined">edit</span>
            Tạo bài viết
          </Link>
        </div>
      </header>

      <section class="forum-categories">
        <button
          class="forum-cat-chip"
          :class="{ 'forum-cat-chip--active': !filters.category }"
          @click="setCategory(null)">
          <span class="material-symbols-outlined">grid_view</span>
          Tất cả
        </button>
        <button v-for="cat in categories" :key="cat.id"
          class="forum-cat-chip"
          :class="{ 'forum-cat-chip--active': filters.category === cat.slug }"
          @click="setCategory(cat.slug)">
          {{ cat.name }}
        </button>
      </section>

      <!-- Pending Posts Section -->
      <section v-if="pendingPosts && pendingPosts.data && pendingPosts.data.length > 0" class="forum-pending-section">
        <div class="forum-pending-header">
          <h2 class="forum-pending-title">Bài viết của bạn đang chờ duyệt</h2>
          <p class="forum-pending-desc">{{ pendingPosts.data.length }} bài viết</p>
        </div>
        <div class="forum-grid">
          <article v-for="p in pendingPosts.data" :key="p.id" class="forum-card pending-card">
            <Link :href="route('agriverse.shop.forum.show', p.id)" class="forum-card-link">
              <div class="forum-card-img">
                <img v-if="p.images?.length" :src="p.images[0]" alt="" class="forum-card-thumb" loading="lazy" />
                <span v-else class="forum-card-letter">{{ (p.title || '?').charAt(0).toUpperCase() }}</span>
              </div>
              <div class="forum-card-body">
                <div class="forum-card-tags">
                  <span class="forum-status-badge forum-status-pending">Chờ duyệt</span>
                  <span v-if="p.category" class="forum-card-cat">{{ p.category.name }}</span>
                </div>
                <h3 class="forum-card-title">{{ p.title }}</h3>
                <p class="forum-card-desc">{{ p.content }}</p>
                <div class="forum-card-meta">
                  <div class="forum-card-author">
                    <div class="forum-avatar">
                      <span>{{ p.user?.name?.charAt(0)?.toUpperCase() || '?' }}</span>
                    </div>
                    <div>
                      <span class="forum-card-author-name">{{ p.user?.name }}</span>
                      <span class="forum-card-date">{{ p.created_at }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </Link>
          </article>
        </div>
      </section>

      <section class="forum-posts">
        <div class="forum-posts-header">
          <h2 class="forum-section-title">Bài viết mới nhất</h2>
          <div class="forum-sort-wrap">
            <span class="forum-sort-label">Sắp xếp:</span>
            <select v-model="sortOrder" @change="applyFilters" class="forum-sort-select">
              <option value="latest">Mới nhất</option>
              <option value="popular">Phổ biến</option>
            </select>
          </div>
        </div>

        <!-- Post Grid -->
        <div v-if="posts.data?.length" class="forum-grid">
          <article v-for="p in posts.data" :key="p.id" class="forum-card">
            <Link :href="route('agriverse.shop.forum.show', p.id)" class="forum-card-link">
              <div class="forum-card-img">
                <img v-if="p.images?.length" :src="p.images[0]" alt="" class="forum-card-thumb" loading="lazy" />
                <span v-else class="forum-card-letter">{{ (p.title || '?').charAt(0).toUpperCase() }}</span>
                <div v-if="p.is_pinned" class="forum-card-pin">
                  <span class="material-symbols-outlined">push_pin</span>
                </div>
              </div>
              <div class="forum-card-body">
                <div class="forum-card-tags">
                  <span v-if="p.category" class="forum-card-cat">{{ p.category.name }}</span>
                  <span v-if="p.status" class="forum-status-badge" :class="p.status === 'approved' ? 'forum-status-approved' : 'forum-status-pending'">{{ p.status === 'approved' ? 'Đã duyệt' : 'Chờ duyệt' }}</span>
                </div>
                <h3 class="forum-card-title">{{ p.title }}</h3>
                <p class="forum-card-desc">{{ p.content }}</p>
                <div class="forum-card-meta">
                  <div class="forum-card-author">
                    <div class="forum-avatar">
                      <span>{{ p.user?.name?.charAt(0)?.toUpperCase() || '?' }}</span>
                    </div>
                    <div>
                      <span class="forum-card-author-name">{{ p.user?.name }}</span>
                      <span class="forum-card-date">{{ p.created_at }}</span>
                    </div>
                  </div>
                  <div class="forum-card-stats">
                    <span class="forum-stat">
                      <span class="material-symbols-outlined">chat_bubble</span>
                      {{ p.comments_count }}
                    </span>
                    <span class="forum-stat">
                      <span class="material-symbols-outlined">favorite</span>
                      {{ p.likes_count }}
                    </span>
                  </div>
                </div>
              </div>
            </Link>
          </article>
        </div>

        <div v-else class="forum-empty">
          <span class="material-symbols-outlined forum-empty-icon">forum</span>
          <h3 class="forum-empty-title">Chưa có bài viết nào</h3>
          <p class="forum-empty-desc">Hãy là người đầu tiên chia sẻ bài viết trong diễn đàn</p>
          <Link :href="route('agriverse.shop.forum.create')" class="forum-empty-btn" v-if="$page.props.auth?.user">
            Tạo bài viết đầu tiên
          </Link>
        </div>
      </section>

      <!-- Pagination -->
      <div v-if="posts.last_page > 1" class="forum-pagination">
        <Link v-for="link in posts.links" :key="link.label" :href="link.url || '#'"
          class="forum-page-btn" :class="{ 'forum-page-btn--active': link.active }"
          v-html="link.label" />
      </div>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import { ref } from 'vue';
import { Link, router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';

const props = defineProps({
  categories: Array,
  posts: Object,
  filters: Object,
  pendingPosts: Object,
});

const searchQuery = ref(props.filters?.search || '');
const sortOrder = ref(props.filters?.sort || 'latest');

function setCategory(slug) {
  router.get(route('agriverse.shop.forum.index'), {
    category: slug,
    search: searchQuery.value || null,
    sort: sortOrder.value,
  }, { preserveState: true });
}

function applyFilters() {
  router.get(route('agriverse.shop.forum.index'), {
    category: props.filters?.category || null,
    search: searchQuery.value || null,
    sort: sortOrder.value,
  }, { preserveState: true });
}
</script>

<style scoped>
.forum-page {
  padding-top: 128px;
  padding-bottom: 80px;
}

.forum-hero {
  max-width: 1280px;
  margin: 0 auto 48px;
  padding: 0 64px;
  text-align: center;
}
@media (max-width: 768px) {
  .forum-hero { padding: 0 20px; }
}
.forum-hero-title {
  font-family: var(--ag-font-display);
  font-size: 48px;
  font-weight: 500;
  line-height: 56px;
  letter-spacing: -0.02em;
  color: var(--ag-text-primary);
  margin-bottom: 16px;
}
.forum-hero-desc {
  font-family: var(--ag-font-body);
  font-size: 18px;
  line-height: 28px;
  color: var(--ag-text-secondary);
  max-width: 700px;
  margin: 0 auto 32px;
}
.forum-hero-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}
.forum-hero-search {
  position: relative;
  flex: 1;
  max-width: 400px;
  min-width: 240px;
}
.forum-search-icon {
  position: absolute;
  left: 20px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 20px;
  color: var(--ag-text-muted);
  pointer-events: none;
}
.forum-search-input {
  width: 100%;
  padding: 14px 20px 14px 52px;
  background: white;
  border: 1px solid rgba(116, 121, 108, 0.12);
  border-radius: 9999px;
  font-size: 15px;
  color: var(--ag-text-primary);
  outline: none;
  transition: all 0.2s;
  font-family: var(--ag-font-body);
  box-sizing: border-box;
}
.forum-search-input:focus {
  border-color: var(--ag-primary-500);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ag-primary-500) 10%, transparent);
}
.forum-create-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 14px 28px;
  border-radius: 10px;
  background: var(--ag-primary-500);
  color: white;
  font-family: var(--ag-font-body);
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.2s;
  white-space: nowrap;
}
.forum-create-btn:hover {
  background: var(--ag-primary-600);
  transform: translateY(-1px);
}

.forum-categories {
  max-width: 1280px;
  margin: 0 auto 48px;
  padding: 0 64px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}
@media (max-width: 768px) {
  .forum-categories { padding: 0 20px; }
}
.forum-cat-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  border-radius: 9999px;
  border: 1px solid rgba(116, 121, 108, 0.12);
  background: white;
  font-family: var(--ag-font-body);
  font-size: 13px;
  font-weight: 500;
  color: var(--ag-text-secondary);
  cursor: pointer;
  transition: all 0.25s;
}
.forum-cat-chip:hover {
  border-color: var(--ag-primary-500);
  color: var(--ag-primary-500);
}
.forum-cat-chip .material-symbols-outlined { font-size: 18px; }
.forum-cat-chip--active {
  background: var(--ag-primary-500);
  color: white;
  border-color: var(--ag-primary-500);
}

.forum-pending-section {
  max-width: 1280px;
  margin: 0 auto 64px;
  padding: 0 64px;
}
@media (max-width: 768px) {
  .forum-pending-section { padding: 0 20px; }
}
.forum-pending-header {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(116, 121, 108, 0.12);
}
.forum-pending-title {
  font-family: var(--ag-font-display);
  font-size: 20px;
  font-weight: 500;
  color: var(--ag-text-primary);
}
.forum-pending-desc {
  font-family: var(--ag-font-body);
  font-size: 13px;
  color: var(--ag-text-secondary);
}
.forum-status-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 9999px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  font-family: var(--ag-font-body);
}
.forum-status-pending {
  background: rgba(255, 193, 7, 0.12);
  color: #d97706;
}
.forum-status-approved {
  background: rgba(16, 185, 129, 0.12);
  color: #10b981;
}
.pending-card {
  border: 1px solid rgba(255, 193, 7, 0.3);
}
.pending-card:hover {
  box-shadow: 0 12px 32px -8px rgba(44,44,44,0.12);
}

.forum-posts {
  max-width: 1280px;
  margin: 0 auto 80px;
  padding: 0 64px;
}
@media (max-width: 768px) {
  .forum-posts { padding: 0 20px; }
}
.forum-posts-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  gap: 16px;
  flex-wrap: wrap;
}
.forum-section-title {
  font-family: var(--ag-font-display);
  font-size: 28px;
  font-weight: 500;
  color: var(--ag-text-primary);
}
.forum-sort-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}
.forum-sort-label {
  font-family: var(--ag-font-body);
  font-size: 13px;
  color: var(--ag-text-muted);
}
.forum-sort-select {
  height: 36px;
  padding: 0 28px 0 10px;
  border: 1px solid rgba(116, 121, 108, 0.12);
  border-radius: 8px;
  font-size: 13px;
  color: var(--ag-text-primary);
  background: white;
  outline: none;
  cursor: pointer;
  font-family: var(--ag-font-body);
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%2374796c' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
}

.forum-grid {
  display: grid;
  gap: 24px;
}
@media (min-width: 640px) {
  .forum-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (min-width: 1024px) {
  .forum-grid { grid-template-columns: repeat(3, 1fr); }
}

.forum-card {
  border-radius: 16px;
  overflow: hidden;
  background: white;
  border: 1px solid rgba(116, 121, 108, 0.06);
  transition: all 0.3s;
}
.forum-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px -8px rgba(44,44,44,0.06);
}
.forum-card-link { text-decoration: none; display: flex; flex-direction: column; height: 100%; }
.forum-card-img {
  height: 140px;
  background: linear-gradient(135deg, var(--ag-primary-100), var(--ag-surface-container-low));
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}
.forum-card-letter {
  font-family: var(--ag-font-display);
  font-size: 52px;
  font-weight: 500;
  color: var(--ag-primary-200);
}
.forum-card-pin {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(255,255,255,0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #d97706;
}
.forum-card-pin .material-symbols-outlined { font-size: 18px; }
.forum-card-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.forum-card-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  flex: 1;
}
.forum-card-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}
.forum-card-cat {
  padding: 2px 10px;
  border-radius: 9999px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  background: color-mix(in srgb, var(--ag-primary-500) 8%, transparent);
  color: var(--ag-primary-500);
  font-family: var(--ag-font-body);
}
.forum-card-title {
  font-family: var(--ag-font-body);
  font-size: 16px;
  font-weight: 700;
  color: var(--ag-text-primary);
  margin-bottom: 6px;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.forum-card-desc {
  font-family: var(--ag-font-body);
  font-size: 13px;
  line-height: 18px;
  color: var(--ag-text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 12px;
  flex: 1;
}
.forum-card-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 10px;
  border-top: 1px solid rgba(116, 121, 108, 0.06);
  gap: 8px;
}
.forum-card-author {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.forum-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: color-mix(in srgb, var(--ag-primary-500) 10%, transparent);
  color: var(--ag-primary-500);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}
.forum-card-author-name {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--ag-text-secondary);
  font-family: var(--ag-font-body);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100px;
}
.forum-card-date {
  display: block;
  font-size: 11px;
  color: var(--ag-text-muted);
  font-family: var(--ag-font-body);
}
.forum-card-stats {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}
.forum-stat {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  color: var(--ag-text-muted);
}
.forum-stat .material-symbols-outlined { font-size: 16px; }

.forum-empty {
  text-align: center;
  padding: 80px 24px;
  background: white;
  border-radius: 16px;
  border: 1px solid rgba(116, 121, 108, 0.06);
}
.forum-empty-icon {
  font-size: 64px;
  color: var(--ag-primary-200);
  margin-bottom: 16px;
}
.forum-empty-title {
  font-family: var(--ag-font-display);
  font-size: 24px;
  font-weight: 500;
  color: var(--ag-text-primary);
  margin-bottom: 8px;
}
.forum-empty-desc {
  font-family: var(--ag-font-body);
  font-size: 15px;
  color: var(--ag-text-secondary);
  margin-bottom: 24px;
}
.forum-empty-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 12px 28px;
  border-radius: 10px;
  background: var(--ag-primary-500);
  color: white;
  font-family: var(--ag-font-body);
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.2s;
}
.forum-empty-btn:hover { background: var(--ag-primary-600); }

.forum-pagination {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 64px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 4px;
}
@media (max-width: 768px) {
  .forum-pagination { padding: 0 20px; }
}
.forum-page-btn {
  min-width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--ag-text-secondary);
  text-decoration: none;
  border: 1px solid transparent;
  transition: all 0.2s;
  font-family: var(--ag-font-body);
}
.forum-page-btn:hover {
  background: var(--ag-surface-container);
  border-color: color-mix(in srgb, var(--ag-border) 50%, transparent);
}
.forum-page-btn--active {
  background: var(--ag-primary-500);
  color: white;
  border-color: var(--ag-primary-500);
}
</style>

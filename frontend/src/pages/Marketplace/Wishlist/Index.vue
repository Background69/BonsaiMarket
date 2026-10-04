<template>
  <MarketplaceLayout>
    <main style="padding-top: 120px; max-width: var(--ag-container-max, 1280px); margin: 0 auto; padding-left: var(--ag-margin-desktop, 64px); padding-right: var(--ag-margin-desktop, 64px); padding-bottom: 80px;">
      <header style="margin-bottom: 64px; display: flex; flex-direction: column; gap: 24px;">
        <div style="max-width: 600px;">
          <span style="font-family: var(--ag-font-body); font-size: 12px; font-weight: 600; letter-spacing: 0.1em; text-transform: uppercase; color: var(--ag-primary-500); margin-bottom: 16px; display: block;">Bộ sưu tập cá nhân</span>
          <h1 style="font-family: var(--ag-font-display); font-size: clamp(2.25rem, 1.7rem + 1.4vw, 4rem); font-weight: 500; line-height: 1.1; letter-spacing: -0.02em; color: var(--ag-text-primary); margin-bottom: 12px;">Danh sách yêu thích</h1>
          <p style="font-family: var(--ag-font-body); font-size: 18px; line-height: 28px; color: var(--ag-text-secondary);">Tuyển chọn các loài thực vật bạn mong muốn nhất.</p>
        </div>
      </header>

      <div v-if="items.length" class="wishlist-grid">
        <div v-for="(item, index) in items" :key="item.id" class="wishlist-item-card">
          <div class="wishlist-card-visual">
            <button @click="toggleHeart(item)" class="wishlist-heart-btn">
              <span class="material-symbols-outlined" :class="item.liked ? 'text-[var(--ag-danger)]' : 'text-[var(--ag-text-muted)]'" :style="`font-variation-settings: 'FILL' ${item.liked ? 1 : 0}`">favorite</span>
            </button>
            <img v-if="item.product?.image" :src="item.product.image" :alt="item.product?.name" class="wishlist-card-img" />
            <span v-else class="wishlist-card-char">{{ item.product?.name?.charAt(0)?.toUpperCase() || 'P' }}</span>
          </div>
          <div class="wishlist-card-info">
            <div class="wishlist-card-top">
              <h3 class="wishlist-card-name">{{ item.product?.name }}</h3>
              <span class="wishlist-card-price">{{ formatPrice(item.product?.price) }}₫</span>
            </div>
            <p v-if="item.product?.description" class="wishlist-card-desc">{{ truncate(item.product.description, 60) }}</p>
            <div class="wishlist-card-actions">
              <button @click="addToCart(item)" class="wishlist-add-btn">
                <span class="material-symbols-outlined" style="font-size: 18px;">add_shopping_cart</span>
                Thêm vào giỏ
              </button>
              <button @click="removeItem(item)" class="wishlist-remove-btn">
                <span class="material-symbols-outlined" style="font-size: 18px;">delete</span>
              </button>
            </div>
          </div>
        </div>

        <div class="wishlist-add-more">
          <div class="wishlist-add-icon">
            <span class="material-symbols-outlined" style="font-size: 32px;">add</span>
          </div>
          <h4 class="wishlist-add-title">Mở rộng danh sách</h4>
          <p class="wishlist-add-desc">Khám phá bộ sưu tập mới nhất và tìm cây yêu thích tiếp theo.</p>
          <Link :href="route('agriverse.shop.products.index')" class="wishlist-browse-link">Khám phá bộ sưu tập</Link>
        </div>
      </div>

      <div v-else class="wishlist-empty">
        <div class="wishlist-empty-icon">
          <span class="material-symbols-outlined" style="font-size: 40px; font-variation-settings: 'FILL' 1;">favorite</span>
        </div>
        <h2 class="wishlist-empty-title">Chưa có sản phẩm yêu thích</h2>
        <p class="wishlist-empty-desc">Duyệt qua bộ sưu tập và bắt đầu xây dựng bộ sưu tập của bạn.</p>
        <Link :href="route('agriverse.shop.products.index')" class="wishlist-empty-btn">Khám phá ngay</Link>
      </div>

      <section v-if="recommendations.length" style="margin-top: 120px;">
        <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 48px;">
          <h2 style="font-family: var(--ag-font-display); font-size: 36px; font-weight: 500; color: var(--ag-text-primary);">Gợi ý cho bạn</h2>
          <Link :href="route('agriverse.shop.products.index')" style="display: flex; align-items: center; gap: 4px; font-family: var(--ag-font-body); font-size: 12px; font-weight: 600; letter-spacing: 0.05em; color: var(--ag-primary-500); text-decoration: none; transition: all 0.3s;">
            Khám phá thêm
            <span class="material-symbols-outlined" style="font-size: 18px; transition: transform 0.3s;">arrow_forward</span>
          </Link>
        </div>
        <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 24px;">
          <div v-for="rec in recommendations" :key="rec.id" style="cursor: pointer; transition: transform 0.3s;">
            <Link :href="route('agriverse.shop.products.show', rec.id)" style="text-decoration: none; display: block;">
               <div style="aspect-ratio: 4/5; overflow: hidden; border-radius: 12px; margin-bottom: 16px; background: var(--ag-surface-container); display: flex; align-items: center; justify-content: center;">
                 <img v-if="rec.image" :src="rec.image" :alt="rec.name" style="width: 100%; height: 100%; object-fit: cover; transition: transform 0.5s cubic-bezier(0.16,1,0.3,1);" />
                 <span v-else style="font-family: var(--ag-font-display); font-size: 36px; color: rgba(116, 121, 108, 0.15);">{{ rec.name?.charAt(0)?.toUpperCase() || 'P' }}</span>
               </div>
              <h4 style="font-family: var(--ag-font-display); font-size: 18px; font-weight: 500; color: var(--ag-text-primary); margin-bottom: 4px;">{{ rec.name }}</h4>
              <span style="font-family: var(--ag-font-body); font-size: 14px; color: var(--ag-text-secondary);">{{ formatPrice(rec.price) }}₫</span>
            </Link>
          </div>
        </div>
      </section>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import { ref } from 'vue';
import { Link, router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';
import { formatPrice } from '@agriverse/utils';
import { useToast } from 'primevue/usetoast';

const toast = useToast();
const props = defineProps({
  wishlistItems: { type: Object, default: () => ({ data: [] }) },
  recommendations: { type: Array, default: () => [] },
});

const items = ref((props.wishlistItems?.data || []).map(item => ({ ...item, liked: true })));

function truncate(text, len) {
  if (!text) return '';
  return text.length > len ? text.substring(0, len) + '...' : text;
}

function toggleHeart(item) {
  const wasLiked = item.liked;
  item.liked = !item.liked;
  router.post(route('agriverse.api.wishlist.toggle', item.product_id || item.product?.id), {}, {
    preserveState: true,
    preserveScroll: true,
    onSuccess: ({ data }) => {
      if (data && typeof data.wishlisted === 'boolean') {
        item.liked = data.wishlisted;
      }
    },
    onError: (e) => {
      item.liked = wasLiked;
      const msg = e.response?.data?.error || 'Không thể thay đổi yêu thích';
      toast.add({ severity: 'error', summary: msg, life: 3000 });
    },
  });
}

function addToCart(item) {
  router.post(route('agriverse.api.cart.add'), { product_id: item.product_id || item.product?.id, quantity: 1 }, {
    preserveState: true,
    preserveScroll: true,
    onSuccess: () => toast.add({ severity: 'success', summary: 'Đã thêm vào giỏ hàng', life: 2000 }),
    onError: () => toast.add({ severity: 'error', summary: 'Thêm thất bại', life: 2000 }),
  });
}

function removeItem(item) {
  router.delete(route('agriverse.api.wishlist.remove', item.id), {
    preserveState: true,
    preserveScroll: true,
    onSuccess: () => {
      items.value = items.value.filter(i => i.id !== item.id);
      toast.add({ severity: 'success', summary: 'Đã xóa khỏi danh sách yêu thích', life: 2000 });
    },
    onError: () => toast.add({ severity: 'error', summary: 'Xóa thất bại', life: 2000 }),
  });
}
</script>

<style scoped>
.wishlist-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}
@media (min-width: 768px) {
  .wishlist-grid { grid-template-columns: repeat(12, 1fr); }
}

.wishlist-item-card { grid-column: 1 / -1; }
@media (min-width: 768px) { .wishlist-item-card { grid-column: span 4; } }
.wishlist-card-visual {
  position: relative;
  aspect-ratio: 16 / 9;
  background: var(--ag-surface-container);
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  transition: transform 0.4s cubic-bezier(0.2, 1, 0.3, 1), box-shadow 0.4s ease;
}
.wishlist-item-card:hover .wishlist-card-visual {
  transform: translateY(-4px);
  box-shadow: 0 10px 30px rgba(72, 103, 48, 0.08);
}
.wishlist-heart-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 10;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255,255,255,0.85);
  backdrop-filter: blur(4px);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}
.wishlist-heart-btn:hover { transform: scale(1.1); }
.wishlist-heart-btn .material-symbols-outlined { font-size: 20px; }
.wishlist-card-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}
.wishlist-item-card:hover .wishlist-card-img {
  transform: scale(1.06);
}
.wishlist-card-char {
  font-family: var(--ag-font-display);
  font-size: 36px;
  color: color-mix(in srgb, var(--ag-text-secondary) 20%, transparent);
  transition: transform 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}
.wishlist-item-card:hover .wishlist-card-char {
  transform: scale(1.06);
}
.wishlist-card-info {
  padding: 0 4px;
  display: flex;
  flex-direction: column;
  flex: 1;
}
.wishlist-card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}
.wishlist-card-name {
  font-family: var(--ag-font-display);
  font-size: 20px;
  font-weight: 500;
  color: var(--ag-text-primary);
  flex: 1;
}
.wishlist-card-price {
  font-family: var(--ag-font-display);
  font-size: 20px;
  font-weight: 500;
  color: var(--ag-text-primary);
  white-space: nowrap;
}
.wishlist-card-desc {
  font-family: var(--ag-font-body);
  font-size: 14px;
  line-height: 20px;
  color: var(--ag-text-secondary);
  margin-bottom: 12px;
}
.wishlist-card-actions {
  display: flex;
  gap: 8px;
  margin-top: auto;
}
.wishlist-add-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px;
  background: var(--ag-primary-500);
  border: none;
  color: white;
  border-radius: 10px;
  font-family: var(--ag-font-body);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}
.wishlist-add-btn:hover { background: var(--ag-primary-600); }
.wishlist-remove-btn {
  width: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--ag-surface-container-highest);
  border: none;
  color: var(--ag-text-muted);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s;
}
.wishlist-remove-btn:hover { background: color-mix(in srgb, var(--ag-danger) 10%, transparent); color: var(--ag-danger); }

.wishlist-add-more {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  border: 2px dashed rgba(116, 121, 108, 0.15);
  border-radius: 12px;
  padding: 48px;
  min-height: 240px;
  cursor: pointer;
  transition: all 0.3s;
}
@media (min-width: 768px) { .wishlist-add-more { grid-column: span 4; } }
.wishlist-add-more:hover { background: var(--ag-surface-container); border-color: color-mix(in srgb, var(--ag-primary-500) 30%, transparent); }
.wishlist-add-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: rgba(72, 103, 48, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ag-primary-500);
  margin-bottom: 16px;
}
.wishlist-add-title {
  font-family: var(--ag-font-display);
  font-size: 22px;
  font-weight: 500;
  color: var(--ag-text-primary);
  margin-bottom: 8px;
}
.wishlist-add-desc {
  font-family: var(--ag-font-body);
  font-size: 14px;
  line-height: 20px;
  color: var(--ag-text-secondary);
  max-width: 240px;
}
.wishlist-browse-link {
  display: inline-block;
  margin-top: 16px;
  font-family: var(--ag-font-body);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.05em;
  color: var(--ag-primary-500);
  text-decoration: underline;
  text-underline-offset: 4px;
  text-decoration-thickness: 2px;
  transition: color 0.2s;
}

.wishlist-empty {
  text-align: center;
  padding: 80px 24px;
}
.wishlist-empty-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: rgba(72, 103, 48, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
  color: var(--ag-primary-500);
}
.wishlist-empty-title {
  font-family: var(--ag-font-display);
  font-size: 28px;
  font-weight: 500;
  color: var(--ag-text-primary);
  margin-bottom: 8px;
}
.wishlist-empty-desc {
  font-family: var(--ag-font-body);
  font-size: 16px;
  line-height: 24px;
  color: var(--ag-text-secondary);
  max-width: 400px;
  margin: 0 auto 32px;
}
.wishlist-empty-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 14px 32px;
  background: var(--ag-primary-500);
  color: white;
  border-radius: 9999px;
  font-family: var(--ag-font-body);
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.3s;
}
.wishlist-empty-btn:hover { background: var(--ag-primary-600); transform: translateY(-1px); }

@media (max-width: 768px) {
  main {
    padding-left: var(--ag-margin-mobile, 20px) !important;
    padding-right: var(--ag-margin-mobile, 20px) !important;
    padding-top: 100px !important;
    padding-bottom: 48px !important;
  }
}
</style>

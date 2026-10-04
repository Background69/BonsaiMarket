<template>
  <AdminLayout>
    <div class="max-w-4xl">
      <Link :href="route('admin.agriverse.products.index')" class="inline-flex items-center gap-1 text-xs mb-4 text-emerald-600 hover:text-emerald-800">
        <span class="material-symbols-outlined text-sm">arrow_back</span> Quay lại danh sách
      </Link>

      <div class="flex items-start justify-between mb-4">
        <div>
          <h1 class="text-lg font-bold text-stone-800">{{ product.name }}</h1>
          <p class="text-xs text-stone-500 mt-1">ID: #{{ product.id }} &middot; {{ product.uuid }}</p>
        </div>
        <div class="flex items-center gap-2">
          <span class="text-[10px] px-2 py-1 rounded-full font-semibold" :style="statusBadgeStyle(product.status)">
            {{ statusLabel(product.status) }}
          </span>
          <Link :href="route('admin.agriverse.products.edit', product.id)" class="h-8 px-3 rounded-lg bg-emerald-600 text-white text-xs font-bold leading-8 hover:bg-emerald-700">Sửa</Link>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <!-- Main Info -->
        <div class="lg:col-span-2 space-y-4">
          <!-- Image -->
          <div class="bg-white rounded-xl border border-stone-200 p-4">
            <h3 class="text-xs font-bold text-stone-600 mb-3">Ảnh sản phẩm</h3>
            <div v-if="product.image" class="rounded-lg overflow-hidden bg-stone-50">
              <img :src="product.image" :alt="product.name" class="w-full max-h-64 object-contain">
            </div>
            <div v-else class="h-32 rounded-lg bg-stone-50 flex items-center justify-center text-stone-400 text-xs">
              Chưa có ảnh
            </div>
          </div>

          <!-- Description -->
          <div class="bg-white rounded-xl border border-stone-200 p-4">
            <h3 class="text-xs font-bold text-stone-600 mb-3">Mô tả</h3>
            <p class="text-sm text-stone-700 whitespace-pre-wrap">{{ product.description || 'Chưa có mô tả.' }}</p>
          </div>

          <!-- 3D Model -->
          <div v-if="product.model_3d_url" class="bg-white rounded-xl border border-stone-200 p-4">
            <h3 class="text-xs font-bold text-stone-600 mb-3">Mô hình 3D</h3>
            <a :href="product.model_3d_url" target="_blank" class="inline-flex items-center gap-2 text-xs text-emerald-600 hover:text-emerald-800">
              <span class="material-symbols-outlined text-sm">view_in_ar</span>
              Tải mô hình GLB
            </a>
            <p class="text-[11px] text-stone-400 mt-2">{{ product.model_3d_path }}</p>
          </div>

          <!-- Reviews -->
          <div class="bg-white rounded-xl border border-stone-200 p-4">
            <h3 class="text-xs font-bold text-stone-600 mb-3">Đánh giá ({{ product.reviews?.length || 0 }})</h3>
            <div v-if="product.reviews?.length" class="space-y-3">
              <div v-for="review in product.reviews" :key="review.id" class="p-3 rounded-lg bg-stone-50">
                <div class="flex items-center justify-between mb-1">
                  <span class="text-xs font-semibold text-stone-700">{{ review.user?.name || 'Ẩn danh' }}</span>
                  <span class="text-[10px] text-stone-400">{{ review.created_at }}</span>
                </div>
                <div class="flex items-center gap-1 mb-1">
                  <span v-for="i in 5" :key="i" class="text-xs" :class="i <= review.rating ? 'text-yellow-500' : 'text-stone-300'">★</span>
                </div>
                <p class="text-xs text-stone-600">{{ review.comment }}</p>
              </div>
            </div>
            <p v-else class="text-xs text-stone-400">Chưa có đánh giá.</p>
          </div>
        </div>

        <!-- Sidebar -->
        <div class="space-y-4">
          <!-- Pricing -->
          <div class="bg-white rounded-xl border border-stone-200 p-4">
            <h3 class="text-xs font-bold text-stone-600 mb-3">Giá & Tồn kho</h3>
            <div class="space-y-3">
              <div class="flex justify-between text-sm">
                <span class="text-stone-500">Giá bán</span>
                <span class="font-bold text-emerald-600">{{ formatPrice(product.price) }}₫</span>
              </div>
              <div v-if="product.compare_price" class="flex justify-between text-sm">
                <span class="text-stone-500">Giá so sánh</span>
                <span class="text-stone-400 line-through">{{ formatPrice(product.compare_price) }}₫</span>
              </div>
              <div class="flex justify-between text-sm">
                <span class="text-stone-500">Tồn kho</span>
                <span class="font-bold" :class="product.stock > 0 ? 'text-stone-800' : 'text-red-500'">{{ product.stock }}</span>
              </div>
            </div>
          </div>

          <!-- Meta -->
          <div class="bg-white rounded-xl border border-stone-200 p-4">
            <h3 class="text-xs font-bold text-stone-600 mb-3">Thông tin khác</h3>
            <div class="space-y-2">
              <div class="flex justify-between text-sm">
                <span class="text-stone-500">Danh mục</span>
                <span class="text-stone-800">{{ product.category || '—' }}</span>
              </div>
              <div class="flex justify-between text-sm">
                <span class="text-stone-500">Nổi bật</span>
                <span class="text-stone-800">{{ product.is_featured ? 'Có' : 'Không' }}</span>
              </div>
              <div class="flex justify-between text-sm">
                <span class="text-stone-500">Tags</span>
                <span class="text-stone-800 text-right max-w-[180px]">{{ Array.isArray(product.tags) ? product.tags.join(', ') : (product.tags || '—') }}</span>
              </div>
              <div v-if="product.store" class="flex justify-between text-sm">
                <span class="text-stone-500">Cửa hàng</span>
                <span class="text-stone-800">{{ product.store.name }}</span>
              </div>
              <div v-if="product.user" class="flex justify-between text-sm">
                <span class="text-stone-500">Người đăng</span>
                <span class="text-stone-800">{{ product.user.name }}</span>
              </div>
              <div class="flex justify-between text-sm">
                <span class="text-stone-500">Ngày tạo</span>
                <span class="text-stone-800">{{ product.created_at }}</span>
              </div>
            </div>
          </div>

          <!-- Reject reason -->
          <div v-if="product.reject_reason" class="bg-red-50 rounded-xl border border-red-200 p-4">
            <h3 class="text-xs font-bold text-red-600 mb-2">Lý do từ chối</h3>
            <p class="text-sm text-red-700">{{ product.reject_reason }}</p>
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

const props = defineProps({ product: Object });

function formatPrice(v) { return new Intl.NumberFormat('vi-VN').format(v || 0); }
function statusLabel(s) { return { pending_review: 'Chờ duyệt', published: 'Đã duyệt', rejected: 'Từ chối', draft: 'Nháp', archived: 'Lưu trữ' }[s] || s; }
function statusBadgeStyle(s) {
  const map = {
    pending_review: 'background: #fef3c7; color: #f59e0b;',
    published: 'background: #dcfce7; color: #22c55e;',
    rejected: 'background: #fef2f2; color: #ef4444;',
    draft: 'background: #fef3c7; color: #d97706;',
    archived: 'background: #f5f5f4; color: #78716c;',
  };
  return map[s] || 'background: #f5f5f4; color: #78716c;';
}
</script>

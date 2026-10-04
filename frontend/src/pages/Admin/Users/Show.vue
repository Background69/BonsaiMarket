<template>
  <AdminLayout>
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-base font-bold text-stone-800">Chi tiết người dùng</h1>
      <Link :href="route('admin.agriverse.users.index')" class="h-8 px-3 rounded-lg border border-stone-300 text-stone-600 text-xs font-bold leading-8 hover:bg-stone-50 transition-all">← Quay lại</Link>
    </div>

    <div class="grid grid-cols-12 gap-4">
      <div class="col-span-12 md:col-span-4">
        <div class="bg-white rounded-xl border border-stone-200 p-4">
          <div class="flex flex-col items-center mb-4">
            <div class="w-16 h-16 rounded-full flex items-center justify-center text-xl font-bold mb-2" style="background: var(--ag-primary-500); color: white;">{{ user.name?.charAt(0)?.toUpperCase() }}</div>
            <h2 class="text-sm font-bold text-stone-800">{{ user.name }}</h2>
            <span class="text-[10px] px-1.5 py-0.5 rounded-full font-semibold mt-1" :class="user.is_active ? 'bg-emerald-50 text-emerald-600' : 'bg-red-50 text-red-600'">{{ user.is_active ? 'Hoạt động' : 'Vô hiệu' }}</span>
          </div>
          <div class="space-y-2 text-xs">
            <div class="flex justify-between"><span class="text-stone-500">Email:</span><span class="text-stone-800">{{ user.email }}</span></div>
            <div class="flex justify-between"><span class="text-stone-500">Điện thoại:</span><span class="text-stone-800">{{ user.phone || '—' }}</span></div>
            <div class="flex justify-between"><span class="text-stone-500">Vai trò:</span><span class="text-stone-800 font-semibold" :class="roleClass(user.role)">{{ roleLabel(user.role) }}</span></div>
            <div class="flex justify-between"><span class="text-stone-500">Ngày ĐK:</span><span class="text-stone-800">{{ formatDate(user.created_at) }}</span></div>
          </div>
        </div>
        <div v-if="addresses.length" class="bg-white rounded-xl border border-stone-200 p-4 mt-4">
          <h3 class="text-xs font-bold text-stone-700 mb-3">Địa chỉ</h3>
          <div v-for="a in addresses" :key="a.id" class="py-2 border-t border-stone-100 first:border-t-0">
            <p class="text-xs text-stone-800">{{ a.recipient_name }} — {{ a.phone }}</p>
            <p class="text-[10px] text-stone-500 mt-0.5">{{ [a.address_detail, a.ward, a.district, a.province].filter(Boolean).join(', ') }}</p>
          </div>
        </div>
      </div>

      <div class="col-span-12 md:col-span-8 space-y-4">
        <div class="bg-white rounded-xl border border-stone-200 p-4">
          <h3 class="text-xs font-bold text-stone-700 mb-3">Lịch sử đơn hàng</h3>
          <table class="w-full text-xs">
            <thead><tr class="text-stone-500 text-left"><th class="pb-2 font-medium">Mã ĐH</th><th class="pb-2 font-medium">Sản phẩm</th><th class="pb-2 font-medium">Tổng</th><th class="pb-2 font-medium">Trạng thái</th><th class="pb-2 font-medium">Ngày</th></tr></thead>
            <tbody>
              <tr v-for="o in orders" :key="o.id" class="border-t border-stone-100">
                <td class="py-2 font-mono text-stone-500">{{ o.uuid?.slice(0, 8) || '#'+o.id }}</td>
                <td class="py-2 text-stone-800">{{ o.product?.name || '—' }}</td>
                <td class="py-2 text-stone-700">{{ formatPrice(o.total_amount) }}₫</td>
                <td class="py-2"><span class="text-[10px] px-1.5 py-0.5 rounded-full font-semibold" :class="statusClass(o.status)">{{ statusLabel(o.status) }}</span></td>
                <td class="py-2 text-stone-500">{{ formatDate(o.created_at) }}</td>
              </tr>
              <tr v-if="!orders.length"><td colspan="5" class="py-4 text-center text-stone-400">Chưa có đơn hàng</td></tr>
            </tbody>
          </table>
        </div>

        <div class="bg-white rounded-xl border border-stone-200 p-4">
          <h3 class="text-xs font-bold text-stone-700 mb-3">Đánh giá</h3>
          <table class="w-full text-xs">
            <thead><tr class="text-stone-500 text-left"><th class="pb-2 font-medium">Sản phẩm</th><th class="pb-2 font-medium">Đánh giá</th><th class="pb-2 font-medium">Nội dung</th><th class="pb-2 font-medium">Ngày</th></tr></thead>
            <tbody>
              <tr v-for="r in reviews" :key="r.id" class="border-t border-stone-100">
                <td class="py-2 text-stone-800">{{ r.product?.name || '—' }}</td>
                <td class="py-2"><span class="text-amber-500">{{ '★'.repeat(r.rating) }}{{ '☆'.repeat(5 - r.rating) }}</span></td>
                <td class="py-2 text-stone-500 max-w-xs truncate">{{ r.comment || '—' }}</td>
                <td class="py-2 text-stone-500">{{ formatDate(r.created_at) }}</td>
              </tr>
              <tr v-if="!reviews.length"><td colspan="4" class="py-4 text-center text-stone-400">Chưa có đánh giá</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { Link } from '@inertiajs/vue3';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';
import { formatPrice, statusLabel, statusClass } from '@agriverse/utils';

const props = defineProps({ user: Object, orders: Array, reviews: Array, addresses: Array });

function formatDate(d) { return d ? new Date(d).toLocaleDateString('vi-VN') : '—'; }
function roleLabel(r) { return { admin: 'Admin', seller: 'Người bán', employee: 'Nhân viên', buyer: 'Người mua' }[r] || r; }
function roleClass(r) { return { admin: 'text-purple-600', seller: 'text-blue-600', employee: 'text-amber-600', buyer: 'text-emerald-600' }[r] || ''; }
</script>

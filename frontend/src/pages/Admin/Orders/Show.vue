<template>
  <AdminLayout>
    <div class="mb-4">
      <div class="flex items-center justify-between">
        <div><h1 class="text-base font-bold text-stone-800">Đơn hàng #{{ order.uuid || order.id }}</h1><span class="text-[10px] px-2 py-0.5 rounded-full font-semibold" :class="statusClass(order.status)">{{ statusLabel(order.status) }}</span></div>
      </div>
    </div>
    <div class="grid grid-cols-12 gap-4">
      <div class="col-span-12 md:col-span-8 space-y-3">
        <div class="bg-white rounded-xl border border-stone-200 p-4">
          <div class="flex items-start gap-3">
            <div class="w-12 h-12 rounded-lg bg-stone-50 flex items-center justify-center text-lg font-bold text-stone-300">{{ order.product?.name?.charAt(0) }}</div>
            <div>
              <div class="text-sm font-semibold">{{ order.product?.name }}</div>
              <div class="text-xs text-stone-500 mt-0.5">SL: {{ order.quantity }} | Đơn giá: {{ formatPrice(order.unit_price) }}₫</div>
              <div class="text-xs text-stone-500">Cửa hàng: {{ order.store?.name }} | Người bán: {{ order.seller?.name }}</div>
            </div>
          </div>
        </div>
        <div class="bg-white rounded-xl border border-stone-200 p-4">
          <h3 class="text-xs font-bold text-stone-700 mb-2">Thông tin giao hàng</h3>
          <p class="text-xs text-stone-500">{{ order.shipping_address }}</p>
          <p v-if="order.notes" class="text-xs text-stone-500 mt-1">Ghi chú: {{ order.notes }}</p>
        </div>
        <div class="bg-white rounded-xl border border-stone-200 p-4">
          <h3 class="text-xs font-bold text-stone-700 mb-2">Cập nhật trạng thái</h3>
          <form @submit.prevent="updateStatus" class="flex gap-2">
            <select v-model="newStatus" class="flex-1 h-8 px-2 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500">
              <option value="pending">Chờ xác nhận</option>
              <option value="confirmed">Đã xác nhận</option>
              <option value="shipping">Đang giao</option>
              <option value="delivered">Đã giao</option>
              <option value="completed">Hoàn thành</option>
              <option value="cancelled">Đã hủy</option>
            </select>
            <input v-model="note" placeholder="Ghi chú..." class="flex-1 h-8 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500">
            <button type="submit" class="h-8 px-3 rounded-lg bg-emerald-600 text-white text-xs font-bold hover:bg-emerald-700 transition-all">Cập nhật</button>
          </form>
        </div>
        <div v-if="statuses.length" class="bg-white rounded-xl border border-stone-200 p-4">
          <h3 class="text-xs font-bold text-stone-700 mb-2">Lịch sử trạng thái</h3>
          <div class="space-y-2">
            <div v-for="(s, i) in statuses" :key="s.id" class="flex gap-2">
              <div class="w-2 h-2 rounded-full mt-1" :class="i === 0 ? 'bg-emerald-500' : 'bg-stone-300'"></div>
              <div><div class="text-xs font-semibold text-stone-700">{{ statusLabel(s.status) }}</div><div class="text-[10px] text-stone-400">{{ s.created_at }} <span v-if="s.note">— {{ s.note }}</span></div></div>
            </div>
          </div>
        </div>
      </div>
      <div class="col-span-12 md:col-span-4 space-y-3">
        <div class="bg-white rounded-xl border border-stone-200 p-4">
          <h3 class="text-xs font-bold text-stone-700 mb-2">Chi tiết thanh toán</h3>
          <div class="space-y-1 text-xs">
            <div class="flex justify-between text-stone-600"><span>Tạm tính</span><span>{{ formatPrice(order.total_price) }}₫</span></div>
            <div v-if="order.discount_amount" class="flex justify-between text-emerald-600"><span>Giảm giá</span><span>-{{ formatPrice(order.discount_amount) }}₫</span></div>
            <div class="flex justify-between text-stone-600"><span>Phí GD</span><span>{{ formatPrice(order.commission_fee) }}₫</span></div>
            <div class="h-px bg-stone-200 my-1"></div>
            <div class="flex justify-between text-sm font-bold"><span>Thành tiền</span><span class="text-red-500">{{ formatPrice(order.total_amount) }}₫</span></div>
          </div>
        </div>
        <div v-if="order.transaction" class="bg-white rounded-xl border border-stone-200 p-4">
          <h3 class="text-xs font-bold text-stone-700 mb-2">Giao dịch</h3>
          <div class="text-xs text-stone-500 space-y-1">
            <div>Mã GD: {{ order.transaction.transaction_id || '—' }}</div>
            <div>Phương thức: {{ order.transaction.payment_method || '—' }}</div>
            <div>Trạng thái: {{ order.transaction.payment_status }}</div>
          </div>
        </div>
        <div v-if="order.contract" class="bg-white rounded-xl border border-stone-200 p-4">
          <h3 class="text-xs font-bold text-stone-700 mb-2">Hợp đồng</h3>
          <Link :href="route('admin.agriverse.contracts.show', order.contract.id)" class="text-xs text-emerald-600 hover:text-emerald-800">Xem hợp đồng →</Link>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, computed } from 'vue';
import { Link, router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';
import { formatPrice, statusLabel, statusClass } from '@agriverse/utils';

const props = defineProps({ order: Object });
const newStatus = ref(props.order?.status || 'pending');
const note = ref('');
const statuses = computed(() => props.order?.statuses || []);

function updateStatus() {
  router.post(route('admin.agriverse.orders.update-status', props.order.id), { status: newStatus.value, note: note.value });
}
</script>

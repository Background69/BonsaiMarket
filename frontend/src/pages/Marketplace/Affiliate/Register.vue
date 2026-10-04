<template>
  <MarketplaceLayout>
    <main class="max-w-[640px] mx-auto px-5 sm:px-16 py-20 min-h-screen">
      <div class="mb-8">
        <h1 class="text-2xl sm:text-3xl font-semibold mb-2" style="color: var(--ag-text-primary); font-family: var(--ag-font-display);">Đăng ký Affiliate</h1>
        <p class="text-sm" style="color: var(--ag-text-secondary);">Tham gia chương trình affiliate để kiếm hoa hồng từ việc giới thiệu sản phẩm.</p>
      </div>

      <div v-if="errors.length" class="rounded-2xl border p-4 mb-6" style="border-color: rgba(220,38,38,0.2); background: rgba(220,38,38,0.06);">
        <div v-for="(err, i) in errors" :key="i" class="text-sm flex items-start gap-2" style="color: var(--ag-danger);">
          <span class="material-symbols-outlined text-base shrink-0 mt-0.5">error</span>
          <span>{{ err }}</span>
        </div>
      </div>

      <form @submit.prevent="submit" class="rounded-2xl border p-6 space-y-5" style="border-color: var(--ag-border); background: var(--ag-bg-card);">
        <div>
          <label class="block text-xs font-semibold uppercase tracking-wider mb-2" style="color: var(--ag-text-secondary);">Phương thức nhận hoa hồng</label>
          <select v-model="form.payout_method" @change="clearError" class="w-full h-11 px-3 rounded-xl text-sm outline-none transition-all" style="border: 1px solid var(--ag-border); background: var(--ag-bg); color: var(--ag-text-primary);">
            <option value="banking">Chuyển khoản ngân hàng</option>
            <option value="momo">Ví MoMo</option>
          </select>
        </div>

        <template v-if="form.payout_method === 'banking'">
          <div>
            <label class="block text-xs font-semibold uppercase tracking-wider mb-2" style="color: var(--ag-text-secondary);">Ngân hàng</label>
            <input v-model="form.bank_name" class="w-full h-11 px-3 rounded-xl text-sm outline-none transition-all" style="border: 1px solid var(--ag-border); background: var(--ag-bg); color: var(--ag-text-primary);" placeholder="VD: Vietcombank" @input="clearError" />
          </div>
          <div>
            <label class="block text-xs font-semibold uppercase tracking-wider mb-2" style="color: var(--ag-text-secondary);">Số tài khoản</label>
            <input v-model="form.account_number" class="w-full h-11 px-3 rounded-xl text-sm outline-none transition-all" style="border: 1px solid var(--ag-border); background: var(--ag-bg); color: var(--ag-text-primary);" placeholder="VD: 1234567890" @input="clearError" />
          </div>
          <div>
            <label class="block text-xs font-semibold uppercase tracking-wider mb-2" style="color: var(--ag-text-secondary);">Chủ tài khoản</label>
            <input v-model="form.account_name" class="w-full h-11 px-3 rounded-xl text-sm outline-none transition-all" style="border: 1px solid var(--ag-border); background: var(--ag-bg); color: var(--ag-text-primary);" placeholder="VD: NGUYEN VAN A" @input="clearError" />
          </div>
        </template>

        <button type="submit" :disabled="submitting"
          class="w-full h-12 rounded-2xl text-sm font-semibold text-white transition-all active:scale-[0.98] disabled:opacity-50 flex items-center justify-center gap-2"
          style="background: var(--ag-primary-500);">
          <span v-if="submitting" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
          Đăng ký ngay
        </button>
      </form>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { router } from '@inertiajs/vue3';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';

const submitting = ref(false);
const errors = ref([]);

const form = reactive({
  payout_method: 'banking',
  bank_name: '',
  account_number: '',
  account_name: '',
});

function clearError() {
  errors.value = [];
}

function validate() {
  const errs = [];
  if (form.payout_method === 'banking') {
    if (!form.bank_name.trim()) errs.push('Vui lòng nhập tên ngân hàng');
    if (!form.account_number.trim()) errs.push('Vui lòng nhập số tài khoản');
    if (!form.account_name.trim()) errs.push('Vui lòng nhập tên chủ tài khoản');
  }
  return errs;
}

function submit() {
  errors.value = validate();
  if (errors.value.length) return;
  submitting.value = true;
  router.post(route('agriverse.shop.affiliate.register'), form, {
    onError: (e) => {
      submitting.value = false;
      const errs = [];
      if (typeof e === 'object') {
        Object.values(e).forEach(v => {
          if (Array.isArray(v)) errs.push(...v);
          else if (typeof v === 'string') errs.push(v);
        });
      }
      errors.value = errs.length ? errs : ['Đã xảy ra lỗi. Vui lòng thử lại.'];
    },
    onFinish: () => { submitting.value = false; },
  });
}
</script>

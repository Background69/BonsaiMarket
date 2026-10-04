<template>
  <MarketplaceLayout>
    <section class="max-w-[640px] mx-auto px-5 py-8">
      <nav class="flex items-center gap-2 text-sm text-[var(--ag-text-muted)] mb-6">
        <Link :href="route('agriverse.shop.home')" class="hover:text-[var(--ag-primary-500)] transition-colors">Trang chủ</Link>
        <span class="material-symbols-outlined text-sm">chevron_right</span>
        <Link :href="route('agriverse.shop.addresses.index')" class="hover:text-[var(--ag-primary-500)] transition-colors">Địa chỉ</Link>
        <span class="material-symbols-outlined text-sm">chevron_right</span>
        <span class="text-[var(--ag-text-primary)] font-medium">{{ isEdit ? 'Sửa địa chỉ' : 'Thêm địa chỉ' }}</span>
      </nav>

      <h1 class="text-xl font-bold text-[var(--ag-text-primary)] tracking-tight mb-6">{{ isEdit ? 'Sửa địa chỉ' : 'Thêm địa chỉ mới' }}</h1>

      <form @submit.prevent="submit" class="bg-white rounded-2xl border border-[var(--ag-border)] p-6 space-y-5">
        <div class="grid grid-cols-2 gap-4">
          <div class="col-span-2 md:col-span-1">
            <label class="text-xs font-semibold text-[var(--ag-text-secondary)] mb-1.5 block">Họ tên người nhận *</label>
            <input v-model="form.recipient_name" required
              class="w-full h-11 px-4 rounded-xl border-2 border-[var(--ag-border)] text-sm outline-none focus:border-[var(--ag-primary-500)]/40 focus:ring-4 focus:ring-[var(--ag-primary-500)]/8 transition-all" />
          </div>
          <div class="col-span-2 md:col-span-1">
            <label class="text-xs font-semibold text-[var(--ag-text-secondary)] mb-1.5 block">Số điện thoại *</label>
            <input v-model="form.phone" required type="tel"
              class="w-full h-11 px-4 rounded-xl border-2 border-[var(--ag-border)] text-sm outline-none focus:border-[var(--ag-primary-500)]/40 focus:ring-4 focus:ring-[var(--ag-primary-500)]/8 transition-all" />
          </div>
        </div>

        <div>
          <label class="text-xs font-semibold text-[var(--ag-text-secondary)] mb-1.5 block">Nhãn *</label>
          <div class="flex gap-2">
            <button v-for="label in labels" :key="label" type="button" @click="form.label = label"
              class="h-10 px-4 rounded-xl border-2 text-sm font-semibold transition-all"
              :class="form.label === label ? 'border-[var(--ag-primary-500)] bg-[var(--ag-primary-500)]/8 text-[var(--ag-primary-500)]' : 'border-[var(--ag-border)] text-[var(--ag-text-secondary)] hover:border-[var(--ag-text-muted)]'">
              {{ label }}
            </button>
          </div>
        </div>

        <!-- Province / District / Ward -->
        <div class="grid grid-cols-3 gap-3">
          <div class="relative">
            <label class="text-xs font-semibold text-[var(--ag-text-secondary)] mb-1.5 block">Tỉnh/Thành *</label>
            <select v-model="selectedProvince" @change="onProvinceChange"
              class="w-full h-11 px-4 rounded-xl border-2 border-[var(--ag-border)] text-sm outline-none focus:border-[var(--ag-primary-500)]/40 focus:ring-4 focus:ring-[var(--ag-primary-500)]/8 transition-all appearance-none bg-white">
              <option value="">Chọn tỉnh</option>
              <option v-for="p in allProvinces" :key="p.province_id" :value="p.province_id">{{ p.province_name }}</option>
            </select>
          </div>
          <div>
            <label class="text-xs font-semibold text-[var(--ag-text-secondary)] mb-1.5 block">Quận/Huyện *</label>
            <select v-model="selectedDistrict" @change="onDistrictChange" :disabled="!selectedProvince"
              class="w-full h-11 px-4 rounded-xl border-2 border-[var(--ag-border)] text-sm outline-none focus:border-[var(--ag-primary-500)]/40 focus:ring-4 focus:ring-[var(--ag-primary-500)]/8 transition-all appearance-none bg-white disabled:opacity-40">
              <option value="">Chọn huyện</option>
              <option v-for="d in districts" :key="d.district_id" :value="d.district_id">{{ d.district_name }}</option>
            </select>
          </div>
          <div>
            <label class="text-xs font-semibold text-[var(--ag-text-secondary)] mb-1.5 block">Phường/Xã *</label>
            <select v-model="selectedWard" :disabled="!selectedDistrict"
              class="w-full h-11 px-4 rounded-xl border-2 border-[var(--ag-border)] text-sm outline-none focus:border-[var(--ag-primary-500)]/40 focus:ring-4 focus:ring-[var(--ag-primary-500)]/8 transition-all appearance-none bg-white disabled:opacity-40">
              <option value="">Chọn xã</option>
              <option v-for="w in wards" :key="w.ward_code" :value="w.ward_code">{{ w.ward_name }}</option>
            </select>
          </div>
        </div>

        <div>
          <label class="text-xs font-semibold text-[var(--ag-text-secondary)] mb-1.5 block">Địa chỉ cụ thể *</label>
          <input v-model="form.address_detail" required placeholder="Số nhà, tên đường..."
            class="w-full h-11 px-4 rounded-xl border-2 border-[var(--ag-border)] text-sm outline-none focus:border-[var(--ag-primary-500)]/40 focus:ring-4 focus:ring-[var(--ag-primary-500)]/8 transition-all" />
        </div>

        <label class="flex items-center gap-2 cursor-pointer select-none">
          <input type="checkbox" v-model="form.is_default" class="w-4 h-4 rounded border-[var(--ag-border)] text-[var(--ag-primary-500)] focus:ring-[var(--ag-primary-500)]/30" />
          <span class="text-sm text-[var(--ag-text-secondary)]">Đặt làm địa chỉ mặc định</span>
        </label>

        <div class="flex gap-3 pt-2">
          <button type="submit" :disabled="loading"
            class="flex-1 h-12 rounded-2xl bg-[var(--ag-primary-500)] text-white text-sm font-semibold hover:bg-[var(--ag-primary-600)] transition-all active:scale-[0.97] disabled:opacity-50 flex items-center justify-center gap-2">
            <span v-if="loading" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            {{ isEdit ? 'Cập nhật' : 'Thêm mới' }}
          </button>
          <Link :href="route('agriverse.shop.addresses.index')"
            class="flex-1 h-12 rounded-2xl border-2 border-[var(--ag-border)] text-[var(--ag-text-secondary)] text-sm font-semibold flex items-center justify-center hover:bg-[var(--ag-bg)] transition-all">
            Hủy
          </Link>
        </div>
      </form>
    </section>
  </MarketplaceLayout>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue';
import { Link, router } from '@inertiajs/vue3';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';
import webApi from '@agriverse/services/webApi';

const props = defineProps({ address: Object, provinces: { type: Array, default: () => [] } });
const isEdit = computed(() => !!props.address);

const labels = ['Nhà', 'Văn phòng', 'Người thân', 'Kho'];
const loading = ref(false);

const allProvinces = ref([]);
const districts = ref([]);
const wards = ref([]);
const selectedProvince = ref('');
const selectedDistrict = ref('');
const selectedWard = ref('');

const form = reactive({
  label: 'Nhà',
  recipient_name: '',
  phone: '',
  province: '',
  province_id: null,
  district: '',
  district_id: null,
  ghn_district_id: null,
  ward: '',
  ward_code: '',
  ghn_ward_code: '',
  address_detail: '',
  is_default: false,
});

// Load provinces on mount
onMounted(async () => {
  if (isEdit.value) {
    form.label = props.address.label;
    form.recipient_name = props.address.recipient_name;
    form.phone = props.address.phone;
    form.province = props.address.province;
    form.district = props.address.district;
    form.ward = props.address.ward;
    form.address_detail = props.address.address_detail;
    form.is_default = props.address.is_default;
    form.province_id = props.address.province_id;
    form.district_id = props.address.district_id;
    form.ghn_district_id = props.address.ghn_district_id;
    form.ward_code = props.address.ward_code;
    form.ghn_ward_code = props.address.ghn_ward_code;
  }
  allProvinces.value = props.provinces;
  // Restore selections for edit
  if (isEdit.value && form.province_id) {
    selectedProvince.value = String(form.province_id);
    await loadDistricts();
    if (form.district_id) {
      selectedDistrict.value = String(form.district_id);
      await loadWards();
      if (form.ward_code) {
        selectedWard.value = form.ward_code;
      }
    }
  }
});

async function loadDistricts() {
  if (!selectedProvince.value) { districts.value = []; return; }
  try {
    const { data } = await webApi.post('/agriverse/api/ghtk/districts', { province_id: selectedProvince.value });
    districts.value = data.data || [];
  } catch { districts.value = []; }
}

async function loadWards() {
  if (!selectedDistrict.value) { wards.value = []; return; }
  try {
    const { data } = await webApi.post('/agriverse/api/ghtk/wards', { district_id: selectedDistrict.value });
    wards.value = data.data || [];
  } catch { wards.value = []; }
}

async function onProvinceChange() {
  selectedDistrict.value = '';
  selectedWard.value = '';
  districts.value = [];
  wards.value = [];
  const p = allProvinces.value.find(x => Number(x.province_id) === Number(selectedProvince.value));
  form.province = p?.province_name || '';
  form.province_id = p?.province_id || null;
  if (selectedProvince.value) await loadDistricts();
}

async function onDistrictChange() {
  selectedWard.value = '';
  wards.value = [];
  const d = districts.value.find(x => Number(x.district_id) === Number(selectedDistrict.value));
  form.district = d?.district_name || '';
  form.district_id = d?.district_id || null;
  form.ghn_district_id = d?.district_id || null;
  if (selectedDistrict.value) await loadWards();
}

watch(selectedWard, (code) => {
  const w = wards.value.find(x => x.ward_code === code);
  form.ward = w?.ward_name || '';
  form.ward_code = w?.ward_code || null;
  form.ghn_ward_code = w?.ward_code || null;
});

function submit() {
  if (!form.recipient_name || !form.phone || !form.province || !form.district || !form.ward || !form.address_detail) return;
  loading.value = true;
  const url = isEdit.value
    ? route('agriverse.shop.addresses.update', props.address.id)
    : route('agriverse.shop.addresses.store');
  router[isEdit.value ? 'put' : 'post'](url, form, {
    onError: () => { loading.value = false; },
    onSuccess: () => { loading.value = false; },
  });
}
</script>

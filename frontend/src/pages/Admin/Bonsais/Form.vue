<template>
  <AdminLayout>
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-base font-bold text-stone-800">{{ bonsai ? 'Chỉnh sửa cây Bonsai' : 'Thêm cây Bonsai mới' }}</h1>
    </div>

    <div v-if="Object.keys(errors).length"
         class="mb-3 px-4 py-3 bg-red-50 border border-red-200 text-red-700 rounded-lg text-xs font-semibold space-y-1">
      <div v-for="(list, k) in errors" :key="k">{{ list.join(', ') }}</div>
    </div>

    <div class="bg-white rounded-xl border border-stone-200 p-4 mb-4 max-w-3xl">
      <label class="block text-xs font-medium text-stone-600 mb-1">Chọn nhanh cây trên hệ thống (chưa có hộ chiếu) — tự
        điền thông tin</label>
      <select v-model="selectedProduct" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"
              @change="fillFromProduct">
        <option value="">— Không chọn (nhập tay) —</option>
        <option v-for="p in availableProducts" :key="p.id" :value="p.id">{{
            p.name
          }}{{ p.has_passport ? ' (đang sửa)' : '' }}
        </option>
      </select>
      <p class="text-[11px] text-stone-400 mt-1.5">Chỉ hiển thị cây đang bán và chưa từng được cấp hộ chiếu NFT.</p>
    </div>

    <form class="bg-white rounded-xl border border-stone-200 p-4 max-w-3xl" @submit.prevent="submit">
      <div class="grid md:grid-cols-2 gap-4">
        <div><label class="block text-xs font-medium text-stone-600 mb-1">Tên cây <span
            class="text-red-500">*</span></label><input v-model="form.name"
                                                        class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"
                                                        required></div>
        <div><label class="block text-xs font-medium text-stone-600 mb-1">Mã cây (để trống = tự sinh)</label><input
            v-model="form.code" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs font-mono outline-none focus:border-emerald-500"
            placeholder="BSN-...">
        </div>
        <div><label class="block text-xs font-medium text-stone-600 mb-1">Loài / Giống</label><input
            v-model="form.species"
            class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500">
        </div>
        <div><label class="block text-xs font-medium text-stone-600 mb-1">Biến thể kỹ thuật</label><input
            v-model="form.variety"
            class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500">
        </div>
        <div><label class="block text-xs font-medium text-stone-600 mb-1">Tuổi (năm)</label><input
            v-model.number="form.age_years" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500" min="0"
            type="number">
        </div>
        <div><label class="block text-xs font-medium text-stone-600 mb-1">Chiều cao (cm)</label><input
            v-model.number="form.height_cm" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500" min="0" step="0.01"
            type="number">
        </div>
        <div><label class="block text-xs font-medium text-stone-600 mb-1">Đường kính thân (cm)</label><input
            v-model.number="form.trunk_cm" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500" min="0" step="0.01"
            type="number">
        </div>
        <div><label class="block text-xs font-medium text-stone-600 mb-1">Nguồn gốc</label><input v-model="form.origin"
                                                                                                  class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500">
        </div>
        <div><label class="block text-xs font-medium text-stone-600 mb-1">URL ảnh</label><input v-model="form.image_url"
                                                                                                class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500">
        </div>
        <div><label class="block text-xs font-medium text-stone-600 mb-1">Tên chủ sở hữu</label><input
            v-model="form.owner_name"
            class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500">
        </div>
        <div><label class="block text-xs font-medium text-stone-600 mb-1">Ví chủ sở hữu (0x...) — nhận NFT</label><input
            v-model="form.owner_wallet" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs font-mono outline-none focus:border-emerald-500"
            placeholder="0x...">
        </div>
      </div>
      <div class="mt-4"><label class="block text-xs font-medium text-stone-600 mb-1">Mô tả</label><textarea
          v-model="form.description" class="w-full px-3 py-2 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"
          rows="4"></textarea>
      </div>

      <div class="flex gap-2 mt-4 pt-3 border-t border-stone-100">
        <button :disabled="form.processing"
                class="h-9 px-4 rounded-lg bg-emerald-600 text-white text-xs font-bold hover:bg-emerald-700 transition-all"
                type="submit">{{ bonsai ? 'Lưu thay đổi' : 'Tạo cây' }}
        </button>
        <Link :href="route('admin.agriverse.bonsais.index')"
              class="h-9 px-4 rounded-lg border border-stone-300 text-stone-600 text-xs font-bold leading-9 hover:bg-stone-50 transition-all">
          Hủy
        </Link>
      </div>
    </form>
  </AdminLayout>
</template>

<script setup>
import {ref, computed} from 'vue';
import {Link, useForm} from '@inertiajs/vue3';
import {route} from 'ziggy-js';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';

const props = defineProps({bonsai: Object, eventTypes: Object, availableProducts: {type: Array, default: () => []}});

const selectedProduct = ref(props.bonsai?.product_id ? String(props.bonsai.product_id) : '');

const form = useForm({
  product_id: props.bonsai?.product_id ?? null,
  name: props.bonsai?.name || '',
  code: props.bonsai?.code || '',
  species: props.bonsai?.species || '',
  variety: props.bonsai?.variety || '',
  age_years: props.bonsai?.age_years ?? '',
  height_cm: props.bonsai?.height_cm ?? '',
  trunk_cm: props.bonsai?.trunk_cm ?? '',
  origin: props.bonsai?.origin || '',
  image_url: props.bonsai?.image_url || '',
  description: props.bonsai?.description || '',
  owner_name: props.bonsai?.owner_name || '',
  owner_wallet: props.bonsai?.owner_wallet || '',
});

const errors = computed(() => form.errors || {});

function fillFromProduct() {
  const p = props.availableProducts.find(x => String(x.id) === String(selectedProduct.value));
  if (!p) {
    form.product_id = null;
    return;
  }
  form.product_id = p.id;
  form.name = p.name || form.name;
  form.species = p.species || form.species;
  form.variety = p.family || form.variety;
  form.origin = p.origin || form.origin;
  form.description = p.description || form.description;
  form.image_url = p.image || form.image_url;
}

function submit() {
  const routeName = props.bonsai
      ? route('admin.agriverse.bonsais.update', props.bonsai.id)
      : route('admin.agriverse.bonsais.store');
  form[props.bonsai ? 'put' : 'post'](routeName);
}
</script>

<template>
  <AdminLayout>
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-base font-bold text-stone-800">{{ product ? 'Sửa sản phẩm' : 'Thêm sản phẩm' }}</h1>
    </div>
    <form @submit.prevent="submit" class="bg-white rounded-xl border border-stone-200 p-4 max-w-2xl">
      <div class="space-y-3">
        <div><label class="block text-xs font-medium text-stone-600 mb-1">Tên sản phẩm</label><input v-model="form.name" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500" required></div>
        <div><label class="block text-xs font-medium text-stone-600 mb-1">Mô tả</label><textarea v-model="form.description" rows="4" class="w-full px-3 py-2 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"></textarea></div>
        <div class="grid grid-cols-2 gap-3">
          <div><label class="block text-xs font-medium text-stone-600 mb-1">Giá</label><input v-model.number="form.price" type="number" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500" required></div>
          <div><label class="block text-xs font-medium text-stone-600 mb-1">Giá so sánh</label><input v-model.number="form.compare_price" type="number" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"></div>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div><label class="block text-xs font-medium text-stone-600 mb-1">Danh mục</label><select v-model="form.category" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"><option value="">Chọn</option><option v-for="c in categories" :key="c.id" :value="c.name">{{ c.name }}</option></select></div>
          <div><label class="block text-xs font-medium text-stone-600 mb-1">Trạng thái</label><select v-model="form.status" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"><option value="draft">Nháp</option><option value="published">Đã đăng</option><option value="archived">Lưu trữ</option></select></div>
        </div>
        <div><label class="block text-xs font-medium text-stone-600 mb-1">Tồn kho</label><input v-model.number="form.stock" type="number" class="w-full h-9 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500" required></div>
      </div>

      <!-- 3D Model Upload (edit mode only) -->
      <div v-if="product" class="mt-4 pt-3 border-t border-stone-100">
        <h3 class="text-xs font-bold text-stone-700 mb-2">Mô hình 3D</h3>
        <div v-if="product.model_3d_path" class="flex items-center gap-3 mb-3">
          <span class="text-xs text-emerald-600 flex items-center gap-1">
            <span class="material-symbols-outlined text-sm">check_circle</span>
            Đã có mô hình 3D
          </span>
          <button @click="delete3dModel" type="button" class="text-xs text-red-500 hover:text-red-700">Xóa</button>
        </div>
        <div class="flex items-center gap-3">
          <input ref="fileInput" type="file" accept=".glb,.gltf,.zip" @change="upload3dModel" class="text-xs text-stone-500 file:mr-3 file:h-8 file:px-3 file:rounded-lg file:border-0 file:bg-emerald-50 file:text-emerald-700 file:text-xs file:font-semibold hover:file:bg-emerald-100">
          <span v-if="uploading" class="text-xs text-stone-500">Đang tải lên...</span>
        </div>
        <div v-if="$page.props.errors.model" class="mt-2 text-xs text-red-500">{{ $page.props.errors.model }}</div>
      </div>

      <div class="flex gap-2 mt-4 pt-3 border-t border-stone-100">
        <button type="submit" class="h-9 px-4 rounded-lg bg-emerald-600 text-white text-xs font-bold hover:bg-emerald-700 transition-all" :disabled="saving">{{ saving ? 'Đang lưu...' : 'Lưu' }}</button>
        <Link :href="route('admin.agriverse.products.index')" class="h-9 px-4 rounded-lg border border-stone-300 text-stone-600 text-xs font-bold leading-9 hover:bg-stone-50 transition-all">Hủy</Link>
      </div>
    </form>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { Link, router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';

const props = defineProps({ product: Object, categories: Array });
const saving = ref(false);
const uploading = ref(false);
const fileInput = ref(null);

const form = reactive({
  name: props.product?.name || '',
  description: props.product?.description || '',
  price: props.product?.price || 0,
  compare_price: props.product?.compare_price || null,
  category: props.product?.category || '',
  status: props.product?.status || 'draft',
  stock: props.product?.stock || 0,
});

function submit() {
  saving.value = true;
  const routeName = props.product ? route('admin.agriverse.products.update', props.product.id) : route('admin.agriverse.products.store');
  const method = props.product ? 'put' : 'post';
  router[method](routeName, form, { onFinish: () => { saving.value = false; } });
}

function upload3dModel(event) {
  const file = event.target.files[0];
  if (!file) return;
  uploading.value = true;
  const formData = new FormData();
  formData.append('model', file);
  router.post(route('admin.agriverse.products.upload-3d-model', props.product.id), formData, {
    onError: (errors) => {
      uploading.value = false;
      alert(errors.model || 'File quá lớn hoặc không hợp lệ. Vui lòng kiểm tra lại cấu hình PHP (post_max_size, upload_max_filesize) hoặc chọn file nhỏ hơn 8MB.');
      if (fileInput.value) fileInput.value.value = '';
    },
    onSuccess: () => {
      uploading.value = false;
      if (fileInput.value) fileInput.value.value = '';
    },
  });
}

function delete3dModel() {
  if (confirm('Xóa mô hình 3D này?')) {
    router.delete(route('admin.agriverse.products.delete-3d-model', props.product.id));
  }
}
</script>

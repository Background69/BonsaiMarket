<template>
  <SellerLayout>
    <div class="max-w-3xl">
      <Link :href="route('agriverse.shop.seller.products.index')" class="inline-flex items-center gap-1 text-sm mb-6" style="color: var(--ag-primary-500);">
        <span class="material-symbols-outlined text-base">arrow_back</span> Quay lại
      </Link>
      <h1 class="text-2xl font-semibold mb-8" style="color: var(--ag-on-surface); font-family: var(--ag-font-display);">
        {{ product ? 'Sửa sản phẩm' : 'Thêm sản phẩm mới' }}
      </h1>

      <form @submit.prevent="submit" class="space-y-6">
        <!-- Tên sản phẩm -->
        <div>
          <label class="form-label">Tên sản phẩm <span class="text-red-500">*</span></label>
          <input v-model="form.name" class="form-input" placeholder="VD: Bonsai San Jose Juniper">
          <p v-if="errors.name" class="form-error">{{ errors.name }}</p>
        </div>

        <!-- Mô tả -->
        <div>
          <label class="form-label">Mô tả sản phẩm</label>
          <textarea v-model="form.description" rows="4" class="form-textarea" placeholder="Mô tả chi tiết về sản phẩm..."></textarea>
        </div>

        <!-- Thông tin cây trồng -->
        <div class="border-t pt-6" style="border-color: var(--ag-border);">
          <h2 class="text-sm font-semibold uppercase tracking-wider mb-4" style="color: var(--ag-text-secondary);">Thông tin cây trồng</h2>

          <div class="grid grid-cols-3 gap-4 mb-4">
            <div>
              <label class="form-label">Tên khoa học</label>
              <input v-model="form.species_latin" class="form-input" placeholder="VD: Ixora coccinea">
            </div>
            <div>
              <label class="form-label">Họ thực vật</label>
              <input v-model="form.family" class="form-input" placeholder="VD: Rubiaceae">
            </div>
            <div>
              <label class="form-label">Nguồn gốc</label>
              <input v-model="form.origin" class="form-input" placeholder="VD: Việt Nam">
            </div>
          </div>

          <h3 class="text-xs font-semibold uppercase tracking-wider mb-3" style="color: var(--ag-text-muted);">Hướng dẫn chăm sóc</h3>
          <div class="grid grid-cols-3 gap-4 mb-4">
            <div>
              <label class="form-label">Ánh sáng</label>
              <input v-model="form.light_requirements" class="form-input" placeholder="VD: Ưa sáng">
            </div>
            <div>
              <label class="form-label">Tưới nước</label>
              <input v-model="form.watering_needs" class="form-input" placeholder="VD: Nhỏ: hàng ngày, lớn: 2-3 ngày/lần">
            </div>
            <div>
              <label class="form-label">Phân bón</label>
              <input v-model="form.fertilizing_guide" class="form-input" placeholder="VD: Bón thêm NPK khi có nụ">
            </div>
            <div>
              <label class="form-label">Nhiệt độ tối thiểu (°C)</label>
              <input v-model="form.min_temp" class="form-input" placeholder="VD: 15">
            </div>
            <div>
              <label class="form-label">Độ pH đất</label>
              <input v-model="form.soil_ph" class="form-input" placeholder="VD: 6.0-7.0">
            </div>
            <div>
              <label class="form-label">Vị trí</label>
              <input v-model="form.indoor_outdoor" class="form-input" placeholder="VD: Ngoài trời, sân vườn">
            </div>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="form-label">Mô tả / Đặc điểm nổi bật</label>
              <textarea v-model="form.key_features" rows="3" class="form-textarea" placeholder="VD: Hoa đỏ đẹp. Sinh trưởng mạnh. Làm hàng rào."></textarea>
            </div>
            <div>
              <label class="form-label">Ý nghĩa phong thủy</label>
              <textarea v-model="form.meaning_fengshui" rows="3" class="form-textarea" placeholder="VD: Vương giả, phú quý, giàu sang"></textarea>
            </div>
          </div>
        </div>

        <!-- Giá -->
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="form-label">Giá bán (VND) <span class="text-red-500">*</span></label>
            <input v-model.number="form.price" type="number" min="0" class="form-input" placeholder="0">
            <p v-if="errors.price" class="form-error">{{ errors.price }}</p>
          </div>
          <div>
            <label class="form-label">Giá so sánh (VND)</label>
            <input v-model.number="form.compare_price" type="number" min="0" class="form-input" placeholder="0">
          </div>
        </div>

        <!-- Danh mục & Tồn kho -->
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="form-label">Danh mục <span class="text-red-500">*</span></label>
            <select v-model="form.category" class="form-input">
              <option value="">Chọn danh mục</option>
              <option v-for="cat in categories" :key="cat.id" :value="cat.name">{{ cat.name }}</option>
            </select>
            <p v-if="errors.category" class="form-error">{{ errors.category }}</p>
          </div>
          <div>
            <label class="form-label">Tồn kho <span class="text-red-500">*</span></label>
            <input v-model.number="form.stock" type="number" min="0" class="form-input" placeholder="0">
            <p v-if="errors.stock" class="form-error">{{ errors.stock }}</p>
          </div>
        </div>

        <!-- Loại sản phẩm & Nhà sản xuất -->
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="form-label">Loại sản phẩm</label>
            <select v-model="form.product_type_id" class="form-input">
              <option value="">Chọn loại</option>
              <option v-for="pt in productTypes" :key="pt.id" :value="pt.id">{{ pt.name }}</option>
            </select>
          </div>
          <div>
            <label class="form-label">Nhà sản xuất</label>
            <select v-model="form.manufacturer_id" class="form-input">
              <option value="">Chọn nhà sản xuất</option>
              <option v-for="m in manufacturers" :key="m.id" :value="m.id">{{ m.name }}</option>
            </select>
          </div>
        </div>

        <!-- Tags -->
        <div>
          <label class="form-label">Tags</label>
          <input v-model="form.tags" class="form-input" placeholder="VD: cây phong thủy, cây để bàn (phân tách bằng dấu phẩy)">
        </div>

        <!-- Ảnh sản phẩm -->
        <div>
          <label class="form-label">Ảnh sản phẩm</label>
          <div v-if="form.image" class="mb-3 relative inline-block">
            <img :src="form.image" class="h-32 rounded-lg object-cover border" style="border-color: var(--ag-border);">
            <button type="button" @click="form.image = ''" class="absolute -top-2 -right-2 w-6 h-6 rounded-full bg-red-500 text-white text-xs flex items-center justify-center">✕</button>
          </div>
          <div v-else>
            <input v-model="form.image" class="form-input" placeholder="Nhập URL ảnh hoặc dán link ảnh">
          </div>
        </div>

        <!-- Upload Model 3D -->
        <div>
          <label class="form-label">Mô hình 3D (GLB)</label>
          <div v-if="form.model_3d_path" class="mb-3 flex items-center gap-3 p-3 rounded-lg" style="background: var(--ag-surface-container-low); border: 1px solid var(--ag-border);">
            <span class="material-symbols-outlined" style="color: var(--ag-primary-500);">view_in_ar</span>
            <span class="text-sm flex-1" style="color: var(--ag-text-primary);">{{ form.model_3d_path.split('/').pop() }}</span>
            <button type="button" @click="form.model_3d_path = ''" class="text-xs px-2 py-1 rounded" style="color: var(--ag-danger);">Xóa</button>
          </div>
          <div v-else>
            <label class="form-upload">
              <input type="file" accept=".glb,.gltf" @change="onFile3D" class="hidden">
              <span class="material-symbols-outlined" style="font-size: 24px; color: var(--ag-text-muted);">upload_file</span>
              <span class="text-sm" style="color: var(--ag-text-muted);">Chọn file .GLB hoặc .GLTF</span>
            </label>
          </div>
        </div>

        <!-- Trạng thái & Nổi bật -->
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="form-label">Trạng thái</label>
            <select v-model="form.status" class="form-input">
              <option value="draft">Nháp</option>
              <option value="pending_review">Chờ duyệt</option>
            </select>
          </div>
          <div class="flex items-end pb-1">
            <label class="flex items-center gap-2 cursor-pointer select-none">
              <input type="checkbox" v-model="form.is_featured" class="w-4 h-4 rounded" style="accent-color: var(--ag-primary-500);">
              <span class="text-sm font-medium" style="color: var(--ag-text-secondary);">Sản phẩm nổi bật</span>
            </label>
          </div>
        </div>

        <!-- Submit -->
        <div class="flex items-center gap-3 pt-4 border-t" style="border-color: var(--ag-border);">
          <button type="submit" :disabled="saving" class="px-6 py-2.5 rounded-xl text-sm font-semibold text-white disabled:opacity-50" style="background: var(--ag-primary-500);">
            <span v-if="saving" class="inline-block w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin mr-2"></span>
            {{ saving ? 'Đang lưu...' : (product ? 'Cập nhật' : 'Đăng sản phẩm') }}
          </button>
          <Link :href="route('agriverse.shop.seller.products.index')" class="px-5 py-2.5 rounded-xl text-sm font-medium" style="color: var(--ag-text-secondary);">Hủy</Link>
        </div>
      </form>
    </div>
  </SellerLayout>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { Link, router } from '@inertiajs/vue3'
import SellerLayout from '../SellerLayout.vue'

const props = defineProps({
  product: { type: Object, default: null },
  categories: { type: Array, default: () => [] },
  manufacturers: { type: Array, default: () => [] },
  productTypes: { type: Array, default: () => [] },
})

const saving = ref(false)
const errors = ref({})

const form = reactive({
  name: props.product?.name || '',
  description: props.product?.description || '',
  species_latin: props.product?.technical_specs?.['Tên khoa học'] || props.product?.metadata?.species_latin || '',
  family: props.product?.technical_specs?.['Họ thực vật'] || props.product?.metadata?.family || '',
  origin: props.product?.metadata?.origin || '',
  light_requirements: props.product?.technical_specs?.['Ánh sáng'] || props.product?.metadata?.light_requirements || '',
  watering_needs: props.product?.technical_specs?.['Tưới nước'] || props.product?.metadata?.watering_needs || '',
  fertilizing_guide: props.product?.metadata?.fertilizing_guide || '',
  min_temp: props.product?.technical_specs?.['Nhiệt độ tối thiểu']?.replace('°C', '') || '',
  soil_ph: props.product?.technical_specs?.['Độ pH đất'] || '',
  indoor_outdoor: props.product?.technical_specs?.['Vị trí'] || '',
  key_features: '',
  meaning_fengshui: props.product?.metadata?.meaning_fengshui || '',
  price: props.product?.price || 0,
  compare_price: props.product?.compare_price || 0,
  category: props.product?.categories?.[0]?.name || props.product?.category || '',
  stock: props.product?.stock || 0,
  tags: Array.isArray(props.product?.tags) ? props.product.tags.join(', ') : (props.product?.tags || ''),
  image: props.product?.image || '',
  model_3d_path: props.product?.model_3d_path || '',
  product_type_id: props.product?.product_type_id || '',
  manufacturer_id: props.product?.manufacturer_id || '',
  is_featured: props.product?.is_featured || false,
  status: props.product?.status || 'pending_review',
})

function buildDescription() {
  const parts = []
  const f = form
  if (f.species_latin) parts.push(`🌿 *Tên khoa học:* ${f.species_latin}`)
  if (f.family) parts.push(`🏷️ *Họ:* ${f.family}`)
  if (f.origin) parts.push(`🌍 *Nguồn gốc:* ${f.origin}`)
  parts.push('')
  parts.push('---')
  parts.push('**📋 Hướng dẫn chăm sóc:**')
  parts.push('')
  if (f.light_requirements) parts.push(`☀️ *Ánh sáng:* ${f.light_requirements}`)
  if (f.watering_needs) parts.push(`💧 *Tưới nước:* ${f.watering_needs}`)
  if (f.fertilizing_guide) parts.push(`🧪 *Phân bón:* ${f.fertilizing_guide}`)
  if (f.min_temp) parts.push(`🌡️ *Nhiệt độ tối thiểu:* ${f.min_temp}°C`)
  if (f.soil_ph) parts.push(`🧫 *Độ pH đất:* ${f.soil_ph}`)
  if (f.indoor_outdoor) parts.push(`🏠 *Vị trí:* ${f.indoor_outdoor}`)
  if (f.key_features) {
    parts.push('')
    parts.push('---')
    parts.push(`**📝 Mô tả:** ${f.key_features}`)
  }
  if (f.meaning_fengshui) {
    parts.push('')
    parts.push(`💰 *Ý nghĩa phong thủy:* ${f.meaning_fengshui}`)
  }
  return parts.join('\n')
}

function onFile3D(e) {
  const file = e.target.files[0]
  if (!file) return
  form.model_3d_path = file.name
  form._model_3d_file = file
}

function submit() {
  saving.value = true
  errors.value = {}

  // Compile description from structured fields
  form.description = buildDescription()

  // Build technical_specs and metadata
  const specs = {}
  const meta = {}
  if (form.species_latin) specs['Tên khoa học'] = form.species_latin
  if (form.family) specs['Họ thực vật'] = form.family
  if (form.light_requirements) specs['Ánh sáng'] = form.light_requirements
  if (form.watering_needs) specs['Tưới nước'] = form.watering_needs
  if (form.min_temp) specs['Nhiệt độ tối thiểu'] = form.min_temp + '°C'
  if (form.soil_ph) specs['Độ pH đất'] = form.soil_ph
  if (form.indoor_outdoor) specs['Vị trí'] = form.indoor_outdoor
  if (form.origin) meta['origin'] = form.origin
  if (form.fertilizing_guide) meta['fertilizing_guide'] = form.fertilizing_guide
  if (form.meaning_fengshui) meta['meaning_fengshui'] = form.meaning_fengshui
  if (form.key_features) meta['key_features'] = form.key_features

  const payload = {
    ...form,
    technical_specs: Object.keys(specs).length ? specs : null,
    metadata: Object.keys(meta).length ? meta : null,
  }

  // Remove helper fields from payload
  delete payload.species_latin
  delete payload.family
  delete payload.origin
  delete payload.light_requirements
  delete payload.watering_needs
  delete payload.fertilizing_guide
  delete payload.min_temp
  delete payload.soil_ph
  delete payload.indoor_outdoor
  delete payload.key_features
  delete payload.meaning_fengshui

  const routeName = props.product
    ? 'agriverse.shop.seller.products.update'
    : 'agriverse.shop.seller.products.store'
  const method = props.product ? 'put' : 'post'

  if (form._model_3d_file) {
    const fd = new FormData()
    Object.keys(payload).forEach(k => {
      if (k === '_model_3d_file') fd.append('model_3d_file', form[k])
      else fd.append(k, typeof payload[k] === 'object' ? JSON.stringify(payload[k]) : (payload[k] ?? ''))
    })
    fd.append('_method', method)

    router.post(route(routeName, props.product?.id), fd, {
      preserveScroll: true,
      forceFormData: true,
      onError: (err) => { errors.value = err; saving.value = false },
      onSuccess: () => { saving.value = false },
    })
  } else {
    delete payload._model_3d_file
    router[method](route(routeName, props.product?.id), payload, {
      preserveScroll: true,
      onError: (err) => { errors.value = err; saving.value = false },
      onSuccess: () => { saving.value = false },
    })
  }
}
</script>

<style scoped>
.form-label {
  display: block;
  font-family: var(--ag-font-body);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--ag-text-secondary);
  margin-bottom: 6px;
}
.form-input {
  width: 100%;
  height: 44px;
  padding: 0 14px;
  border: 1px solid var(--ag-border);
  border-radius: 10px;
  font-family: var(--ag-font-body);
  font-size: 14px;
  color: var(--ag-text-primary);
  background: white;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}
.form-input:focus {
  border-color: var(--ag-primary-500);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ag-primary-500) 10%, transparent);
}
.form-textarea {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--ag-border);
  border-radius: 10px;
  font-family: var(--ag-font-body);
  font-size: 14px;
  color: var(--ag-text-primary);
  background: white;
  outline: none;
  transition: all 0.2s;
  resize: vertical;
  min-height: 80px;
}
.form-textarea:focus {
  border-color: var(--ag-primary-500);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ag-primary-500) 10%, transparent);
}
.form-error {
  font-size: 12px;
  color: var(--ag-error, #dc2626);
  margin-top: 4px;
}
.form-upload {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px;
  border: 2px dashed var(--ag-border);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}
.form-upload:hover {
  border-color: var(--ag-primary-500);
  background: color-mix(in srgb, var(--ag-primary-500) 4%, transparent);
}
</style>

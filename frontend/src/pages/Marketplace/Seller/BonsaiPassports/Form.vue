<template>
  <SellerLayout>
    <div class="max-w-3xl">
      <Link :href="route('agriverse.shop.seller.bonsai-passports.index')"
            class="inline-flex items-center gap-1 text-sm mb-6" style="color: var(--ag-primary-500);">
        <span class="material-symbols-outlined text-base">arrow_back</span> Quay lại
      </Link>

      <h1 class="text-2xl font-semibold mb-2" style="color: var(--ag-on-surface); font-family: var(--ag-font-display);">
        {{ bonsai ? 'Sửa yêu cầu hộ chiếu' : 'Làm hộ chiếu cây' }}
      </h1>
      <p class="text-sm mb-6" style="color: var(--ag-on-surface-variant);">
        Khai báo thông tin cây để xin cấp hộ chiếu NFT (Soulbound). Sau khi gửi, yêu cầu sẽ được chuyển cho admin duyệt.
      </p>

      <div v-if="bonsai?.status === 'rejected'" class="mb-4 px-4 py-3 rounded-xl text-sm font-semibold"
           style="background: #fef2f2; color: #b91c1c; border: 1px solid #fecaca;">
        Yêu cầu trước đó bị từ chối: {{ bonsai.reject_reason || 'Không có lý do.' }} — sửa lại thông tin rồi gửi lại để
        admin duyệt.
      </div>

      <div v-if="Object.keys(form.errors).length" class="mb-4 px-4 py-3 rounded-xl text-sm font-semibold"
           style="background: #fef2f2; color: #b91c1c; border: 1px solid #fecaca;">
        <div v-for="(list, k) in form.errors" :key="k">{{ list.join(', ') }}</div>
      </div>

      <form class="space-y-6" @submit.prevent="submit">
        <!-- Chọn nhanh cây -->
        <div class="rounded-2xl border p-5" style="background: white; border-color: var(--ag-border);">
          <label class="form-label">Chọn cây (tự điền thông tin)</label>
          <select v-model="selectedProduct" class="form-input" @change="fillFromProduct">
            <option value="">— Chọn cây để tự điền —</option>
            <option v-for="p in availableProducts" :key="p.id" :value="p.id">{{ p.name }}</option>
          </select>
          <p class="text-xs mt-1.5" style="color: var(--ag-text-muted);">Chỉ hiển thị cây của bạn chưa có hộ chiếu NFT.
            Bạn vẫn có thể nhập tay.</p>
        </div>

        <!-- Thông tin cây -->
        <div class="rounded-2xl border p-5" style="background: white; border-color: var(--ag-border);">
          <h2 class="text-sm font-semibold uppercase tracking-wider mb-4" style="color: var(--ag-text-secondary);">Thông
            tin cây</h2>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="form-label">Tên cây <span class="text-red-500">*</span></label>
              <input v-model="form.name" class="form-input" placeholder="VD: Sanh Cổ Thụ" required>
            </div>
            <div>
              <label class="form-label">Mã cây (để trống = tự sinh)</label>
              <input v-model="form.code" class="form-input font-mono" placeholder="BSN-...">
            </div>
            <div>
              <label class="form-label">Loài / Giống</label>
              <input v-model="form.species" class="form-input" placeholder="VD: Ficus microcarpa">
            </div>
            <div>
              <label class="form-label">Biến thể kỹ thuật</label>
              <input v-model="form.variety" class="form-input">
            </div>
            <div>
              <label class="form-label">Tuổi (năm)</label>
              <input v-model.number="form.age_years" class="form-input" min="0" type="number">
            </div>
            <div>
              <label class="form-label">Chiều cao (cm)</label>
              <input v-model.number="form.height_cm" class="form-input" min="0" step="0.01" type="number">
            </div>
            <div>
              <label class="form-label">Đường kính thân (cm)</label>
              <input v-model.number="form.trunk_cm" class="form-input" min="0" step="0.01" type="number">
            </div>
            <div>
              <label class="form-label">Nguồn gốc</label>
              <input v-model="form.origin" class="form-input" placeholder="VD: Việt Nam">
            </div>
            <div class="col-span-2">
              <label class="form-label">URL ảnh</label>
              <input v-model="form.image_url" class="form-input" placeholder="https://...">
              <img v-if="form.image_url" :src="form.image_url" class="mt-3 h-32 rounded-lg object-cover border"
                   style="border-color: var(--ag-border);">
            </div>
            <div class="col-span-2">
              <label class="form-label">Mô tả</label>
              <textarea v-model="form.description" class="form-textarea" placeholder="Đặc điểm, tình trạng cây..."
                        rows="4"></textarea>
            </div>
          </div>
        </div>

        <!-- Chủ sở hữu -->
        <div class="rounded-2xl border p-5" style="background: white; border-color: var(--ag-border);">
          <h2 class="text-sm font-semibold uppercase tracking-wider mb-4" style="color: var(--ag-text-secondary);">Chủ
            sở hữu NFT</h2>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="form-label">Tên chủ sở hữu</label>
              <input v-model="form.owner_name" :placeholder="currentUserName || 'Tên của bạn'" class="form-input">
            </div>
            <div>
              <label class="form-label">Ví nhận hộ chiếu (0x...) <span class="text-red-500">*</span></label>
              <input v-model="form.owner_wallet" class="form-input font-mono" placeholder="0x..." required>
              <p class="text-xs mt-1.5" style="color: var(--ag-text-muted);">Hộ chiếu NFT sẽ được cấp về ví này sau khi
                admin duyệt. Kiểm tra kỹ địa chỉ.</p>
            </div>
          </div>
        </div>

        <!-- Submit -->
        <div class="flex items-center gap-3 pt-2">
          <button :disabled="form.processing"
                  class="px-6 py-2.5 rounded-xl text-sm font-semibold text-white disabled:opacity-50 transition-all active:scale-[0.97]"
                  style="background: var(--ag-primary-500);"
                  type="submit">
            <span v-if="form.processing"
                  class="inline-block w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin mr-2"></span>
            {{ bonsai ? 'Lưu & gửi lại duyệt' : 'Gửi yêu cầu cấp hộ chiếu' }}
          </button>
          <Link :href="route('agriverse.shop.seller.bonsai-passports.index')"
                class="px-5 py-2.5 rounded-xl text-sm font-medium" style="color: var(--ag-text-secondary);">Hủy
          </Link>
        </div>
      </form>
    </div>
  </SellerLayout>
</template>

<script setup>
import {ref, computed} from 'vue'
import {Link, useForm, usePage} from '@inertiajs/vue3'
import {route} from 'ziggy-js'
import SellerLayout from '../SellerLayout.vue'

const props = defineProps({
  bonsai: {type: Object, default: null},
  availableProducts: {type: Array, default: () => []},
  preselectedProductId: {type: Number, default: null},
})

const page = usePage()
const currentUserName = computed(() => page.props.auth?.user?.name || '')

const selectedProduct = ref(props.bonsai?.product_id ? String(props.bonsai.product_id) : (props.preselectedProductId ? String(props.preselectedProductId) : ''))

const form = useForm({
  product_id: props.bonsai?.product_id ?? (props.preselectedProductId ?? null),
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
})

function fillFromProduct() {
  const p = props.availableProducts.find(x => String(x.id) === String(selectedProduct.value))
  if (!p) {
    form.product_id = null
    return
  }
  form.product_id = p.id
  form.name = p.name || form.name
  form.species = p.species || form.species
  form.variety = p.family || form.variety
  form.origin = p.origin || form.origin
  form.description = p.description || form.description
  form.image_url = p.image || form.image_url
}

function submit() {
  const routeName = props.bonsai
      ? route('agriverse.shop.seller.bonsai-passports.update', props.bonsai.id)
      : route('agriverse.shop.seller.bonsai-passports.store')
  form[props.bonsai ? 'put' : 'post'](routeName)
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
</style>

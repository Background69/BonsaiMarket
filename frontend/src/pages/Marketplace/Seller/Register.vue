<template>
  <MarketplaceLayout>
    <main class="max-w-2xl mx-auto py-12 px-6">
      <h1 class="text-3xl font-semibold mb-2" style="color: var(--ag-on-surface); font-family: var(--ag-font-display);">Đăng ký người bán</h1>
      <p class="text-sm mb-10" style="color: var(--ag-on-surface-variant);">Trở thành người bán trên AgriVerse</p>

      <!-- Status: Already submitted -->
      <div v-if="verification && verification.status !== 'none'" class="rounded-2xl p-8 text-center border" :class="statusBgClass">
        <span class="material-symbols-outlined text-5xl mb-4" :style="{ color: statusIconColor }">{{ statusIcon }}</span>
        <h2 class="text-xl font-semibold mb-2" style="color: var(--ag-on-surface);">{{ statusTitle }}</h2>
        <p class="text-sm" style="color: var(--ag-on-surface-variant);">{{ statusDesc }}</p>
        <p v-if="verification.reject_reason" class="mt-4 p-4 rounded-xl bg-red-50 text-red-700 text-sm">{{ verification.reject_reason }}</p>
        <button v-if="verification.status === 'rejected'" @click="resetAndRetry" class="mt-6 px-6 py-2.5 rounded-xl text-sm font-semibold text-white" style="background: var(--ag-primary-500);">
          Đăng ký lại
        </button>
        <Link v-else :href="route('agriverse.shop.home')" class="mt-6 inline-block px-6 py-2.5 rounded-xl text-sm font-semibold text-white" style="background: var(--ag-primary-500);">
          Về trang chủ
        </Link>
      </div>

      <!-- Registration Form -->
      <div v-else class="space-y-8">
        <!-- Step indicator -->
        <div class="flex items-center gap-2 text-sm">
          <span v-for="(s, i) in steps" :key="i" class="flex items-center gap-2">
            <span class="w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold" :class="i <= currentStep ? 'step-active' : 'step-inactive'">{{ i + 1 }}</span>
            <span :class="i <= currentStep ? 'text-foreground' : 'text-muted'">{{ s }}</span>
            <span v-if="i < steps.length - 1" class="w-8 h-px mx-1" style="background: var(--ag-border);"></span>
          </span>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-8">
          <!-- Step 1: Personal Info -->
          <div v-show="currentStep === 0" class="space-y-6">
            <h2 class="text-lg font-semibold">Thông tin cá nhân</h2>

            <div>
              <label class="text-sm font-medium mb-1.5 block">Họ tên</label>
              <input v-model="form.name" type="text" readonly class="w-full h-11 px-4 rounded-xl border text-sm bg-gray-50 cursor-not-allowed" style="border-color: var(--ag-border); color: var(--ag-on-surface);">
            </div>

            <div>
              <label class="text-sm font-medium mb-1.5 block">Email <span class="text-red-500">*</span></label>
              <div class="flex gap-3">
                <input v-model="form.email" type="email" :readonly="!!user.email" class="flex-1 h-11 px-4 rounded-xl border text-sm outline-none focus:border-[var(--ag-primary-500)]" :class="{ 'bg-gray-50 cursor-not-allowed': !!user.email }" style="border-color: var(--ag-border); color: var(--ag-on-surface);"
                  placeholder="your@email.com">
                <button v-if="!emailVerified" type="button" @click="sendCode" :disabled="sendingCode" class="px-4 h-11 rounded-xl text-sm font-semibold text-white shrink-0 disabled:opacity-50" style="background: var(--ag-primary-500);">
                  {{ sendingCode ? 'Đang gửi...' : 'Gửi mã' }}
                </button>
                <span v-else class="flex items-center gap-1.5 text-sm font-medium text-green-600">
                  <span class="material-symbols-outlined text-lg">verified</span> Đã xác thực
                </span>
              </div>
            </div>

            <div v-if="codeSent && !emailVerified">
              <label class="text-sm font-medium mb-1.5 block">Nhập mã xác thực</label>
              <div class="flex gap-3">
                <input v-model="verifyCode" type="text" maxlength="6" class="flex-1 h-11 px-4 rounded-xl border text-sm text-center tracking-[8px] font-bold text-lg outline-none focus:border-[var(--ag-primary-500)]" style="border-color: var(--ag-border);" placeholder="000000">
                <button type="button" @click="confirmCode" :disabled="verifyingCode" class="px-4 h-11 rounded-xl text-sm font-semibold text-white shrink-0 disabled:opacity-50" style="background: var(--ag-primary-500);">
                  {{ verifyingCode ? 'Đang xác thực...' : 'Xác thực' }}
                </button>
              </div>
              <p class="text-xs mt-1.5" style="color: var(--ag-on-surface-variant);">Mã có hiệu lực 10 phút. Kiểm tra hộp thư đến hoặc spam.</p>
            </div>

            <div>
              <label class="text-sm font-medium mb-1.5 block">Số điện thoại</label>
              <input v-model="form.phone" type="text" :readonly="!!user.phone" class="w-full h-11 px-4 rounded-xl border text-sm outline-none focus:border-[var(--ag-primary-500)]" :class="{ 'bg-gray-50 cursor-not-allowed': !!user.phone }" style="border-color: var(--ag-border); color: var(--ag-on-surface);" placeholder="Số điện thoại">
            </div>

            <div>
              <label class="text-sm font-medium mb-1.5 block">CCCD / CMND</label>
              <input v-model="form.id_card_number" type="text" maxlength="20" class="w-full h-11 px-4 rounded-xl border text-sm outline-none focus:border-[var(--ag-primary-500)]" style="border-color: var(--ag-border); color: var(--ag-on-surface);" placeholder="Số căn cước công dân">
            </div>
          </div>

          <!-- Step 2: ID Card Photos -->
          <div v-show="currentStep === 1" class="space-y-6">
            <h2 class="text-lg font-semibold">Ảnh căn cước</h2>

            <div>
              <label class="text-sm font-medium mb-1.5 block">Mặt trước CCCD</label>
              <div class="border-2 border-dashed rounded-xl p-8 text-center cursor-pointer hover:bg-gray-50 transition-colors" :class="{ 'border-green-400 bg-green-50': idFrontPreview }" style="border-color: var(--ag-border);" @click="$refs.idFront.click()">
                <input ref="idFront" type="file" accept="image/*" class="hidden" @change="e => handleFile(e, 'id_front')">
                <img v-if="idFrontPreview" :src="idFrontPreview" class="max-h-48 mx-auto rounded-lg">
                <div v-else>
                  <span class="material-symbols-outlined text-4xl" style="color: var(--ag-text-muted);">credit_card</span>
                  <p class="text-sm mt-2" style="color: var(--ag-on-surface-variant);">Nhấn để chọn ảnh mặt trước</p>
                </div>
              </div>
            </div>

            <div>
              <label class="text-sm font-medium mb-1.5 block">Mặt sau CCCD</label>
              <div class="border-2 border-dashed rounded-xl p-8 text-center cursor-pointer hover:bg-gray-50 transition-colors" :class="{ 'border-green-400 bg-green-50': idBackPreview }" style="border-color: var(--ag-border);" @click="$refs.idBack.click()">
                <input ref="idBack" type="file" accept="image/*" class="hidden" @change="e => handleFile(e, 'id_back')">
                <img v-if="idBackPreview" :src="idBackPreview" class="max-h-48 mx-auto rounded-lg">
                <div v-else>
                  <span class="material-symbols-outlined text-4xl" style="color: var(--ag-text-muted);">credit_card</span>
                  <p class="text-sm mt-2" style="color: var(--ag-on-surface-variant);">Nhấn để chọn ảnh mặt sau</p>
                </div>
              </div>
            </div>
          </div>

          <!-- Navigation -->
          <div class="flex items-center justify-between pt-4 border-t" style="border-color: var(--ag-border);">
            <button v-if="currentStep > 0" type="button" @click="currentStep--" class="px-5 py-2.5 rounded-xl text-sm font-medium transition-colors hover:bg-gray-100" style="color: var(--ag-on-surface);">
              Quay lại
            </button>
            <div v-else></div>
            <div class="flex gap-3">
              <button v-if="currentStep < steps.length - 1" type="button" :disabled="!canProceed" @click="currentStep++" class="px-6 py-2.5 rounded-xl text-sm font-semibold text-white disabled:opacity-40" style="background: var(--ag-primary-500);">
                Tiếp theo
              </button>
              <button v-else type="submit" :disabled="submitting || !canSubmit" class="px-6 py-2.5 rounded-xl text-sm font-semibold text-white disabled:opacity-40" style="background: var(--ag-primary-500);">
                {{ submitting ? 'Đang gửi...' : 'Gửi yêu cầu' }}
              </button>
            </div>
          </div>
        </form>
      </div>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Link, router } from '@inertiajs/vue3'
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue'

const props = defineProps({
  verification: { type: Object, default: null },
  user: { type: Object, default: () => ({}) },
})

const steps = ['Thông tin', 'Ảnh CCCD', 'Hoàn tất']
const currentStep = ref(0)
const sendingCode = ref(false)
const verifyingCode = ref(false)
const codeSent = ref(false)
const emailVerified = ref(!!props.verification?.email_verified_at)
const verifyCode = ref('')
const submitting = ref(false)

const form = ref({
  name: props.user?.name || '',
  email: props.user?.email || '',
  phone: props.user?.phone || '',
  id_card_number: props.verification?.id_card_number || '',
  id_card_image_front: null,
  id_card_image_back: null,
})

const idFrontPreview = ref(null)
const idBackPreview = ref(null)

function handleFile(e, field) {
  const file = e.target.files[0]
  if (!file) return
  if (field === 'id_front') {
    form.value.id_card_image_front = file
    idFrontPreview.value = URL.createObjectURL(file)
  } else {
    form.value.id_card_image_back = file
    idBackPreview.value = URL.createObjectURL(file)
  }
}

const canProceed = computed(() => {
  if (currentStep.value === 0) {
    return form.value.email && emailVerified.value && form.value.id_card_number
  }
  return true
})

const canSubmit = computed(() => {
  return form.value.id_card_image_front && form.value.id_card_image_back
})

function sendCode() {
  if (!form.value.email) return
  sendingCode.value = true
  router.post(route('agriverse.shop.seller.register.send-code'), { email: form.value.email }, {
    preserveState: true,
    preserveScroll: true,
    onSuccess: () => {
      codeSent.value = true
      sendingCode.value = false
    },
    onError: () => { sendingCode.value = false },
  })
}

function confirmCode() {
  if (!verifyCode.value || verifyCode.value.length !== 6) return
  verifyingCode.value = true
  router.post(route('agriverse.shop.seller.register.verify-code'), { code: verifyCode.value }, {
    preserveState: true,
    preserveScroll: true,
    onSuccess: () => {
      emailVerified.value = true
      verifyingCode.value = false
    },
    onError: () => { verifyingCode.value = false },
  })
}

function handleSubmit() {
  if (!canSubmit.value) return
  submitting.value = true
  const data = new FormData()
  data.append('id_card_number', form.value.id_card_number)
  data.append('phone', form.value.phone)
  if (form.value.id_card_image_front) data.append('id_card_image_front', form.value.id_card_image_front)
  if (form.value.id_card_image_back) data.append('id_card_image_back', form.value.id_card_image_back)

  router.post(route('agriverse.shop.seller.register.submit'), data, {
    preserveState: false,
    onError: () => { submitting.value = false },
  })
}

function resetAndRetry() {
  window.location.reload()
}

// Status display
const statusBgClass = computed(() => {
  if (props.verification?.status === 'approved') return 'bg-green-50 border-green-200'
  if (props.verification?.status === 'rejected') return 'bg-red-50 border-red-200'
  return 'bg-yellow-50 border-yellow-200'
})

const statusIconColor = computed(() => {
  if (props.verification?.status === 'approved') return '#16a34a'
  if (props.verification?.status === 'rejected') return '#dc2626'
  return '#ca8a04'
})

const statusIcon = computed(() => {
  if (props.verification?.status === 'approved') return 'check_circle'
  if (props.verification?.status === 'rejected') return 'cancel'
  return 'hourglass_top'
})

const statusTitle = computed(() => {
  if (props.verification?.status === 'approved') return 'Chúc mừng! Bạn đã là người bán'
  if (props.verification?.status === 'rejected') return 'Yêu cầu đã bị từ chối'
  return 'Đang chờ duyệt'
})

const statusDesc = computed(() => {
  if (props.verification?.status === 'approved') return 'Tài khoản của bạn đã được kích hoạt quyền bán hàng. Bạn có thể bắt đầu đăng sản phẩm ngay.'
  if (props.verification?.status === 'rejected') return 'Yêu cầu đăng ký người bán của bạn đã bị từ chối. Vui lòng kiểm tra lý do bên dưới và đăng ký lại.'
  return 'Yêu cầu của bạn đang được quản trị viên xem xét. Bạn sẽ nhận được thông báo khi có kết quả.'
})
</script>

<style scoped>
.step-active {
  background: var(--ag-primary-500);
  color: white;
}
.step-inactive {
  background: var(--ag-border);
  color: var(--ag-text-muted);
}
</style>

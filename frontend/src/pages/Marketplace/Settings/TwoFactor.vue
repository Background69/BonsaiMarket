<template>
  <MarketplaceLayout>
    <main class="max-w-[1280px] mx-auto px-5 sm:px-16 py-20 min-h-screen">
      <h1 class="text-2xl sm:text-3xl font-semibold mb-2" style="color: var(--ag-text-primary); font-family: var(--ag-font-display);">Xác thực hai yếu tố</h1>
      <p class="text-sm mb-10" style="color: var(--ag-text-secondary);">Tăng cường bảo mật tài khoản bằng mã xác thực từ ứng dụng Authenticator.</p>

      <div v-if="!setupMode" class="max-w-xl">
        <div class="rounded-2xl border p-6" style="border-color: var(--ag-border); background: var(--ag-bg-card);">
          <div class="flex items-center gap-4 mb-6">
            <div class="w-12 h-12 rounded-xl flex items-center justify-center" :style="{ background: isEnabled ? 'rgba(72,103,48,0.1)' : 'rgba(217,119,6,0.1)' }">
              <span class="material-symbols-outlined text-2xl" :style="{ color: isEnabled ? 'var(--ag-primary-500)' : 'var(--ag-warning)' }">security</span>
            </div>
            <div>
              <h2 class="text-base font-semibold" style="color: var(--ag-text-primary);">{{ isEnabled ? 'Đã bật 2FA' : 'Chưa bật 2FA' }}</h2>
              <p class="text-xs mt-0.5" style="color: var(--ag-text-secondary);">
                {{ isEnabled ? 'Bảo vệ tài khoản bằng mã xác thực' : 'Tài khoản của bạn chưa được bảo vệ thêm' }}
              </p>
            </div>
            <span v-if="isEnabled" class="ml-auto w-3 h-3 rounded-full" style="background: var(--ag-primary-500);"></span>
          </div>

          <div v-if="isEnabled" class="space-y-4">
            <div class="grid grid-cols-2 gap-3 text-sm">
              <div class="p-3 rounded-xl" style="background: var(--ag-bg);">
                <div class="text-xs" style="color: var(--ag-text-muted);">Ngày bật</div>
                <div class="font-semibold mt-0.5" style="color: var(--ag-text-primary);">{{ enabledAt ? formatDate(enabledAt) : '—' }}</div>
              </div>
              <div class="p-3 rounded-xl" style="background: var(--ag-bg);">
                <div class="text-xs" style="color: var(--ag-text-muted);">Mã dự phòng</div>
                <div class="font-semibold mt-0.5" style="color: var(--ag-text-primary);">{{ recoveryCodesCount }} mã còn lại</div>
              </div>
            </div>

            <div v-if="showRecoveryCodes && recoveryCodesList.length" class="p-4 rounded-xl" style="background: rgba(217,119,6,0.06); border: 1px solid rgba(217,119,6,0.12);">
              <div class="flex items-center gap-2 mb-3">
                <span class="material-symbols-outlined text-sm" style="color: var(--ag-warning);">info</span>
                <span class="text-xs font-semibold" style="color: var(--ag-warning);">Mã dự phòng (lưu lại an toàn)</span>
              </div>
              <div class="grid grid-cols-2 gap-2">
                <div v-for="(code, i) in recoveryCodesList" :key="i" class="font-mono text-xs px-2 py-1 rounded" style="background: var(--ag-bg-card); color: var(--ag-text-primary);">
                  {{ code }}
                </div>
              </div>
            </div>

            <div class="flex flex-wrap gap-3 pt-2">
              <button @click="showRegenInput = !showRegenInput" :disabled="loading" class="px-4 py-2 rounded-xl text-sm font-semibold transition-all" style="border: 1px solid var(--ag-border); color: var(--ag-text-secondary); background: transparent;">
                Tạo mã dự phòng mới
              </button>
              <button @click="startDisable" class="px-4 py-2 rounded-xl text-sm font-semibold transition-all" style="border: 1px solid rgba(220,38,38,0.3); color: var(--ag-danger); background: transparent;">
                Tắt 2FA
              </button>
            </div>

            <div v-if="showRegenInput" class="p-3 rounded-xl" style="background: var(--ag-bg);">
              <label class="block text-xs font-semibold mb-2" style="color: var(--ag-text-secondary);">Nhập mã xác thực từ ứng dụng Authenticator</label>
              <div class="flex gap-2">
                <input v-model="regenCode" class="flex-1 h-10 px-3 rounded-lg text-sm outline-none tracking-widest text-center" style="border: 1px solid var(--ag-border); background: var(--ag-bg-card); color: var(--ag-text-primary); font-family: var(--ag-font-body);" placeholder="000000" maxlength="6" @keyup.enter="regenerateCodes" />
                <button @click="regenerateCodes" :disabled="regenCode.length !== 6 || regenLoading" class="h-10 px-4 rounded-lg text-sm font-semibold text-white disabled:opacity-50" style="background: var(--ag-primary-500);">
                  <span v-if="regenLoading" class="inline-block w-3 h-3 border-2 border-white/30 border-t-white rounded-full animate-spin mr-1"></span>
                  Xác nhận
                </button>
                <button @click="showRegenInput = false; regenCode = ''" class="h-10 px-3 text-sm" style="color: var(--ag-text-muted);">Hủy</button>
              </div>
            </div>

            <div v-if="disableConfirm" class="flex items-center gap-3 p-3 rounded-xl" style="background: var(--ag-bg);">
              <input v-model="disableCode" class="flex-1 h-10 px-3 rounded-lg text-sm outline-none" style="border: 1px solid var(--ag-border); background: var(--ag-bg-card); color: var(--ag-text-primary); font-family: var(--ag-font-body); letter-spacing: 4px; text-align: center;" placeholder="000000" maxlength="6" />
              <button @click="confirmDisable" :disabled="disableCode.length !== 6 || loading" class="h-10 px-4 rounded-lg text-sm font-semibold text-white disabled:opacity-50" style="background: var(--ag-danger);">
                Xác nhận
              </button>
              <button @click="disableConfirm = false; disableCode = ''" class="h-10 px-3 text-sm" style="color: var(--ag-text-muted);">Hủy</button>
            </div>
          </div>

          <button v-else @click="startSetup" class="w-full h-12 rounded-2xl text-sm font-semibold text-white transition-all active:scale-[0.98]" style="background: var(--ag-primary-500);">
            Bật xác thực hai yếu tố
          </button>
        </div>
      </div>

      <div v-if="setupMode && !setupVerified" class="max-w-xl">
        <div class="rounded-2xl border p-6" style="border-color: var(--ag-border); background: var(--ag-bg-card);">
          <div class="flex items-center gap-3 mb-6">
            <button @click="cancelSetup" class="w-8 h-8 rounded-lg flex items-center justify-center" style="border: 1px solid var(--ag-border); color: var(--ag-text-secondary);">
              <span class="material-symbols-outlined text-sm">arrow_back</span>
            </button>
            <h2 class="text-base font-semibold" style="color: var(--ag-text-primary);">Quét mã QR</h2>
          </div>

          <div v-if="qrCodeUrl" class="flex flex-col items-center py-4">
            <div class="w-48 h-48 rounded-2xl border p-3 mb-4" style="background: white; border-color: var(--ag-border);">
              <img :src="qrCodeUrl" alt="QR Code" class="w-full h-full" />
            </div>
            <p class="text-sm text-center max-w-sm mb-2" style="color: var(--ag-text-secondary);">
              Quét mã QR bằng <strong style="color: var(--ag-text-primary);">Google Authenticator</strong> hoặc <strong style="color: var(--ag-text-primary);">Authy</strong>
            </p>
            <p class="text-xs" style="color: var(--ag-text-muted);">Hoặc nhập mã thủ công: <code class="font-mono font-bold" style="color: var(--ag-text-primary);">{{ secret }}</code></p>
          </div>
          <div v-else class="flex justify-center py-8">
            <div class="w-6 h-6 border-2 rounded-full animate-spin" style="border-color: var(--ag-border); border-top-color: var(--ag-primary-500);"></div>
          </div>

          <div class="mt-6 pt-4" style="border-top: 1px solid var(--ag-border);">
            <label class="block text-xs font-semibold mb-2" style="color: var(--ag-text-secondary);">Nhập mã xác thực từ ứng dụng</label>
            <div class="flex gap-2">
              <input v-model="verifyCode" class="flex-1 h-12 px-4 rounded-xl text-lg outline-none tracking-[8px] text-center" style="border: 1px solid var(--ag-border); background: var(--ag-bg); color: var(--ag-text-primary); font-family: var(--ag-font-body); letter-spacing: 8px;" placeholder="000000" maxlength="6" @keyup.enter="confirmEnable" />
              <button @click="confirmEnable" :disabled="verifyCode.length !== 6 || loading" class="h-12 px-6 rounded-xl text-sm font-semibold text-white disabled:opacity-50" style="background: var(--ag-primary-500);">
                Xác nhận
              </button>
            </div>
            <p v-if="verifyError" class="text-xs mt-2" style="color: var(--ag-danger);">{{ verifyError }}</p>
          </div>
        </div>
      </div>

      <div v-if="setupVerified" class="max-w-xl">
        <div class="rounded-2xl border p-6" style="border-color: var(--ag-border); background: var(--ag-bg-card);">
          <div class="flex items-center gap-3 mb-4">
            <div class="w-10 h-10 rounded-xl flex items-center justify-center" style="background: rgba(72,103,48,0.1);">
              <span class="material-symbols-outlined text-xl" style="color: var(--ag-primary-500);">check</span>
            </div>
            <div>
              <h2 class="text-base font-semibold" style="color: var(--ag-text-primary);">Đã bật 2FA</h2>
              <p class="text-xs" style="color: var(--ag-text-secondary);">Lưu các mã dự phòng này ở nơi an toàn</p>
            </div>
          </div>

          <div class="p-4 rounded-xl mb-4" style="background: rgba(217,119,6,0.06); border: 1px solid rgba(217,119,6,0.12);">
            <div class="grid grid-cols-2 gap-2">
              <div v-for="(code, i) in newRecoveryCodes" :key="i" class="font-mono text-xs px-3 py-2 rounded text-center" style="background: var(--ag-bg-card); color: var(--ag-text-primary); border: 1px solid var(--ag-border);">
                {{ code }}
              </div>
            </div>
          </div>

          <button @click="doneSetup" class="w-full h-12 rounded-2xl text-sm font-semibold text-white transition-all" style="background: var(--ag-primary-500);">
            Hoàn tất
          </button>
        </div>
      </div>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import { ref } from 'vue';
import { route } from 'ziggy-js';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';
import { useToast } from 'primevue/usetoast';

const toast = useToast();

const props = defineProps({
  isEnabled: { type: Boolean, default: false },
  recoveryCodesCount: { type: Number, default: 0 },
  enabledAt: { type: String, default: null },
});

const loading = ref(false);
const regenLoading = ref(false);
const setupMode = ref(false);
const setupVerified = ref(false);
const qrCodeUrl = ref('');
const secret = ref('');
const verifyCode = ref('');
const verifyError = ref('');
const disableConfirm = ref(false);
const disableCode = ref('');
const showRecoveryCodes = ref(false);
const recoveryCodesList = ref([]);
const showRegenInput = ref(false);
const regenCode = ref('');
const newRecoveryCodes = ref([]);

async function startSetup() {
  setupMode.value = true;
  loading.value = true;
  try {
    const { data } = await window.axios.post(route('agriverse.shop.2fa.setup'));
    qrCodeUrl.value = data.qr_code_url;
    secret.value = data.secret;
  } catch (e) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Không thể tạo mã QR', life: 3000 });
    setupMode.value = false;
  } finally {
    loading.value = false;
  }
}

async function confirmEnable() {
  if (verifyCode.value.length !== 6) return;
  loading.value = true;
  verifyError.value = '';
  try {
    const { data } = await window.axios.post(route('agriverse.shop.2fa.enable'), { code: verifyCode.value });
    if (data.recovery_codes) {
      newRecoveryCodes.value = data.recovery_codes;
    }
    setupVerified.value = true;
    toast.add({ severity: 'success', summary: 'Đã bật xác thực hai yếu tố', life: 3000 });
  } catch (e) {
    verifyError.value = e.response?.data?.message || 'Mã không hợp lệ';
  } finally {
    loading.value = false;
  }
}

function cancelSetup() {
  setupMode.value = false;
  verifyCode.value = '';
  verifyError.value = '';
  qrCodeUrl.value = '';
  secret.value = '';
}

function doneSetup() {
  window.location.reload();
}

function startDisable() {
  disableConfirm.value = !disableConfirm.value;
  disableCode.value = '';
}

async function confirmDisable() {
  if (disableCode.value.length !== 6) return;
  loading.value = true;
  try {
    await window.axios.post(route('agriverse.shop.2fa.disable'), { code: disableCode.value });
    toast.add({ severity: 'success', summary: 'Đã tắt xác thực hai yếu tố', life: 3000 });
    window.location.reload();
  } catch (e) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Không thể tắt 2FA', life: 3000 });
  } finally {
    loading.value = false;
  }
}

async function regenerateCodes() {
  if (regenCode.value.length !== 6) return;
  regenLoading.value = true;
  try {
    const { data } = await window.axios.post(route('agriverse.shop.2fa.regenerate-recovery'), { code: regenCode.value });
    recoveryCodesList.value = data.recovery_codes || [];
    showRecoveryCodes.value = true;
    showRegenInput.value = false;
    regenCode.value = '';
    toast.add({ severity: 'success', summary: 'Đã tạo mã dự phòng mới', life: 3000 });
  } catch (e) {
    toast.add({ severity: 'error', summary: e.response?.data?.message || 'Không thể tạo mã', life: 3000 });
  } finally {
    regenLoading.value = false;
  }
}

function formatDate(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString('vi-VN', { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}
</script>

<template>
  <MarketplaceLayout>
    <main class="settings">
      <div class="settings-layout">
        <aside class="settings-sidebar">
          <div class="settings-sidebar-inner">
            <h1 class="settings-sidebar-title">Tài khoản</h1>
            <nav class="settings-nav">
              <button v-for="tab in tabs" :key="tab.id" @click="activeTab = tab.id"
                class="settings-nav-item" :class="{ 'settings-nav-active': activeTab === tab.id }">
                <span class="material-symbols-outlined settings-nav-icon"
                  :style="activeTab === tab.id ? 'font-variation-settings:\'FILL\' 1' : ''">{{ tab.icon }}</span>
                <span class="settings-nav-label">{{ tab.label }}</span>
              </button>
            </nav>
          </div>
        </aside>

        <div class="settings-content">
          <!-- Profile Info Tab -->
          <section v-if="activeTab === 'profile'" class="settings-section">
            <div class="settings-section-header">
              <h2 class="settings-section-title">Thông tin Cá nhân</h2>
              <p class="settings-section-desc">Cập nhật thông tin hồ sơ và cách mọi người nhìn thấy bạn.</p>
            </div>
            <div class="settings-section-body">
              <form @submit.prevent="saveProfile" class="settings-card">
                <div class="settings-form-grid">
                  <div class="settings-field">
                    <label class="settings-label">Họ và tên</label>
                    <input v-model="profileForm.name" type="text" class="settings-input" placeholder="Nhập họ tên" />
                    <span v-if="profileErrors.name" class="settings-error">{{ profileErrors.name }}</span>
                  </div>
                  <div class="settings-field">
                    <label class="settings-label">Email</label>
                    <input :value="user.email" type="email" class="settings-input settings-input-disabled" disabled />
                    <span class="settings-hint">Email không thể thay đổi</span>
                  </div>
                  <div class="settings-field">
                    <label class="settings-label">Số điện thoại</label>
                    <input v-model="profileForm.phone" type="tel" class="settings-input" placeholder="0912 345 678" />
                  </div>
                  <div class="settings-field">
                    <label class="settings-label">Giới tính</label>
                    <select v-model="profileForm.gender" class="settings-input">
                      <option value="">Chọn giới tính</option>
                      <option value="male">Nam</option>
                      <option value="female">Nữ</option>
                      <option value="other">Khác</option>
                    </select>
                  </div>
                  <div class="settings-field settings-field-full">
                    <label class="settings-label">Mô tả bản thân</label>
                    <textarea v-model="profileForm.bio" class="settings-input settings-textarea" rows="3"
                      placeholder="Giới thiệu ngắn về bạn..."></textarea>
                  </div>
                </div>
                <div class="settings-actions">
                  <button type="submit" class="settings-btn settings-btn-primary" :disabled="profileSaving">
                    <span v-if="profileSaving" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    {{ profileSaving ? 'Đang lưu...' : 'Lưu thay đổi' }}
                  </button>
                </div>
              </form>
            </div>
          </section>

          <!-- Password Tab -->
          <section v-if="activeTab === 'security'" class="settings-section">
            <div class="settings-section-header">
              <h2 class="settings-section-title">Bảo mật</h2>
              <p class="settings-section-desc">Bảo vệ tài khoản bằng mật khẩu và xác thực hai yếu tố.</p>
            </div>
            <div class="settings-section-body space-y-4">
              <form @submit.prevent="changePassword" class="settings-card">
                <div class="settings-form-grid">
                  <div class="settings-field settings-field-full">
                    <label class="settings-label">Mật khẩu hiện tại</label>
                    <input v-model="passwordForm.old_password" type="password" class="settings-input"
                      placeholder="Nhập mật khẩu hiện tại" />
                    <span v-if="passwordErrors.old_password" class="settings-error">{{ passwordErrors.old_password }}</span>
                  </div>
                  <div class="settings-field">
                    <label class="settings-label">Mật khẩu mới</label>
                    <input v-model="passwordForm.new_password" type="password" class="settings-input"
                      placeholder="Tối thiểu 8 ký tự" minlength="8" />
                    <span v-if="passwordErrors.new_password" class="settings-error">{{ passwordErrors.new_password }}</span>
                  </div>
                  <div class="settings-field">
                    <label class="settings-label">Xác nhận mật khẩu mới</label>
                    <input v-model="passwordForm.repass" type="password" class="settings-input"
                      placeholder="Nhập lại mật khẩu mới" />
                    <span v-if="passwordErrors.repass" class="settings-error">{{ passwordErrors.repass }}</span>
                  </div>
                </div>
                <div v-if="passwordSuccess" class="settings-success">{{ passwordSuccess }}</div>
                <div class="settings-actions">
                  <button type="submit" class="settings-btn settings-btn-primary" :disabled="passwordSaving">
                    <span v-if="passwordSaving" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    {{ passwordSaving ? 'Đang cập nhật...' : 'Cập nhật mật khẩu' }}
                  </button>
                </div>
              </form>

              <div class="settings-card">
                <div class="settings-form-grid">
                  <div class="settings-field settings-field-full">
                    <label class="settings-label">Xác thực hai yếu tố (2FA)</label>
                    <p class="text-sm text-[var(--ag-text-secondary)] mb-3">Tăng cường bảo mật tài khoản bằng mã xác thực từ ứng dụng Authenticator.</p>
                    <Link :href="route('agriverse.shop.2fa.index')"
                      class="settings-btn settings-btn-primary inline-flex items-center gap-2 no-underline">
                      <span class="material-symbols-outlined text-base">security</span>
                      Quản lý 2FA
                    </Link>
                  </div>
                </div>
              </div>
            </div>
          </section>

          <!-- Notifications Tab -->
          <section v-if="activeTab === 'notifications'" class="settings-section">
            <div class="settings-section-header">
              <h2 class="settings-section-title">Thông báo</h2>
              <p class="settings-section-desc">Cấu hình cách bạn nhận thông báo từ hệ thống.</p>
            </div>
            <div class="settings-section-body">
              <div class="settings-card">
                <div class="settings-toggles">
                  <div class="settings-toggle-row" :class="{ 'settings-toggle-row-active': notifSettings.push }">
                    <div>
                      <span class="settings-toggle-label">Thông báo Đẩy</span>
                      <p class="settings-toggle-desc">Nhận thông báo trên trình duyệt</p>
                    </div>
                    <div class="toggle-switch">
                      <input type="checkbox" id="push" class="toggle-checkbox" v-model="notifSettings.push" />
                      <label for="push" class="toggle-label"></label>
                    </div>
                  </div>
                  <div class="settings-toggle-row" :class="{ 'settings-toggle-row-active': notifSettings.email }">
                    <div>
                      <span class="settings-toggle-label">Báo cáo Email</span>
                      <p class="settings-toggle-desc">Nhận thông báo qua email</p>
                    </div>
                    <div class="toggle-switch">
                      <input type="checkbox" id="email" class="toggle-checkbox" v-model="notifSettings.email" />
                      <label for="email" class="toggle-label"></label>
                    </div>
                  </div>
                  <div class="settings-toggle-row" :class="{ 'settings-toggle-row-active': notifSettings.order_updates }">
                    <div>
                      <span class="settings-toggle-label">Cập nhật đơn hàng</span>
                      <p class="settings-toggle-desc">Thông báo khi trạng thái đơn hàng thay đổi</p>
                    </div>
                    <div class="toggle-switch">
                      <input type="checkbox" id="order_updates" class="toggle-checkbox" v-model="notifSettings.order_updates" />
                      <label for="order_updates" class="toggle-label"></label>
                    </div>
                  </div>
                  <div class="settings-toggle-row" :class="{ 'settings-toggle-row-active': notifSettings.promotions }">
                    <div>
                      <span class="settings-toggle-label">Khuyến mãi</span>
                      <p class="settings-toggle-desc">Thông báo về ưu đãi và mã giảm giá</p>
                    </div>
                    <div class="toggle-switch">
                      <input type="checkbox" id="promotions" class="toggle-checkbox" v-model="notifSettings.promotions" />
                      <label for="promotions" class="toggle-label"></label>
                    </div>
                  </div>
                </div>
              </div>
              <div class="settings-actions">
                <button class="settings-btn settings-btn-primary" @click="saveNotifications" :disabled="notifSaving">
                  <span v-if="notifSaving" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  {{ notifSaving ? 'Đang lưu...' : 'Lưu tùy chọn' }}
                </button>
              </div>
            </div>
          </section>
        </div>
      </div>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import { ref, reactive, computed } from 'vue';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';
import { useToast } from 'primevue/usetoast';
import { Link, router } from '@inertiajs/vue3';
import webApi from '@agriverse/services/webApi';

const toast = useToast();

const props = defineProps({
  user: { type: Object, required: true },
  notificationPreferences: { type: Object, default: () => ({}) },
});

const tabs = [
  { id: 'profile', label: 'Thông tin Cá nhân', icon: 'person' },
  { id: 'security', label: 'Bảo mật', icon: 'shield' },
  { id: 'notifications', label: 'Thông báo', icon: 'notifications' },
];

const activeTab = ref('profile');

// Profile
const profileForm = reactive({
  name: props.user.name || '',
  phone: props.user.phone || '',
  gender: props.user.gender || '',
  bio: props.user.bio || '',
});
const profileErrors = reactive({ name: '' });
const profileSaving = ref(false);

async function saveProfile() {
  profileErrors.name = '';
  if (!profileForm.name.trim()) {
    profileErrors.name = 'Họ tên không được để trống';
    return;
  }
  profileSaving.value = true;
  try {
    await webApi.post('/agriverse/api/settings/profile', profileForm);
    toast.add({ severity: 'success', summary: 'Đã cập nhật hồ sơ', life: 2000 });
  } catch (e) {
    const errs = e.response?.data?.error;
    if (errs?.name) profileErrors.name = Array.isArray(errs.name) ? errs.name[0] : errs.name;
    toast.add({ severity: 'error', summary: 'Cập nhật thất bại', life: 2000 });
  } finally {
    profileSaving.value = false;
  }
}

// Password
const passwordForm = reactive({ old_password: '', new_password: '', repass: '' });
const passwordErrors = reactive({ old_password: '', new_password: '', repass: '' });
const passwordSaving = ref(false);
const passwordSuccess = ref('');

async function changePassword() {
  passwordErrors.old_password = '';
  passwordErrors.new_password = '';
  passwordErrors.repass = '';
  passwordSuccess.value = '';

  if (!passwordForm.old_password) { passwordErrors.old_password = 'Vui lòng nhập mật khẩu hiện tại'; return; }
  if (passwordForm.new_password.length < 8) { passwordErrors.new_password = 'Mật khẩu mới phải từ 8 ký tự'; return; }
  if (passwordForm.new_password !== passwordForm.repass) { passwordErrors.repass = 'Mật khẩu xác nhận không khớp'; return; }

  passwordSaving.value = true;
  try {
    await webApi.post('/agriverse/api/settings/password', {
      oldpass: passwordForm.old_password,
      newpass: passwordForm.new_password,
      repass: passwordForm.repass,
    });
    passwordSuccess.value = 'Đã cập nhật mật khẩu thành công!';
    passwordForm.old_password = '';
    passwordForm.new_password = '';
    passwordForm.repass = '';
    toast.add({ severity: 'success', summary: 'Đã đổi mật khẩu', life: 2000 });
  } catch (e) {
    const err = e.response?.data?.mes || e.response?.data?.error;
    if (typeof err === 'string') {
      passwordErrors.old_password = err;
    } else if (typeof err === 'object') {
      if (err.oldpass) passwordErrors.old_password = Array.isArray(err.oldpass) ? err.oldpass[0] : err.oldpass;
      if (err.newpass) passwordErrors.new_password = Array.isArray(err.newpass) ? err.newpass[0] : err.newpass;
      if (err.repass) passwordErrors.repass = Array.isArray(err.repass) ? err.repass[0] : err.repass;
    }
    toast.add({ severity: 'error', summary: 'Đổi mật khẩu thất bại', life: 2000 });
  } finally {
    passwordSaving.value = false;
  }
}

// Notifications
const notifSettings = reactive({
  push: props.notificationPreferences.push ?? true,
  email: props.notificationPreferences.email ?? false,
  order_updates: props.notificationPreferences.order_updates ?? true,
  promotions: props.notificationPreferences.promotions ?? false,
});
const notifSaving = ref(false);

async function saveNotifications() {
  notifSaving.value = true;
  try {
    await webApi.post('/agriverse/api/settings/notifications', { ...notifSettings });
    toast.add({ severity: 'success', summary: 'Đã lưu tùy chọn thông báo', life: 2000 });
  } catch {
    toast.add({ severity: 'error', summary: 'Lưu thất bại', life: 2000 });
  } finally {
    notifSaving.value = false;
  }
}
</script>

<style scoped>
.settings {
  max-width: var(--ag-container-max, 1280px);
  margin: 0 auto;
  padding: 80px 64px 80px;
}
@media (max-width: 768px) {
  .settings { padding: 40px 16px 60px; }
}
.settings-layout {
  display: flex;
  flex-direction: column;
  gap: 40px;
}
@media (min-width: 768px) {
  .settings-layout { flex-direction: row; gap: 48px; }
}
.settings-sidebar { width: 100%; flex-shrink: 0; }
@media (min-width: 768px) { .settings-sidebar { width: 220px; } }
.settings-sidebar-inner { position: sticky; top: 100px; }
.settings-sidebar-title {
  font-family: var(--ag-font-display);
  font-size: 28px;
  font-weight: 500;
  color: var(--ag-text-primary);
  margin-bottom: 24px;
}
.settings-nav { display: flex; flex-direction: column; gap: 4px; }
.settings-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 10px;
  font-family: var(--ag-font-body);
  font-size: 14px;
  font-weight: 500;
  color: var(--ag-text-secondary);
  border: none;
  background: transparent;
  cursor: pointer;
  text-align: left;
  transition: all 0.2s;
  width: 100%;
}
.settings-nav-item:hover {
  background: color-mix(in srgb, var(--ag-primary-500) 6%, transparent);
  color: var(--ag-text-primary);
}
.settings-nav-active {
  background: color-mix(in srgb, var(--ag-primary-500) 10%, transparent);
  color: var(--ag-primary-500);
  font-weight: 600;
}
.settings-nav-icon { font-size: 20px; }

.settings-content { flex: 1; min-width: 0; }
.settings-section { margin-bottom: 40px; }
.settings-section-header { margin-bottom: 24px; }
.settings-section-title {
  font-family: var(--ag-font-display);
  font-size: 24px;
  font-weight: 500;
  color: var(--ag-text-primary);
  margin-bottom: 4px;
}
.settings-section-desc {
  font-family: var(--ag-font-body);
  font-size: 14px;
  color: var(--ag-text-secondary);
}
.settings-section-body { display: flex; flex-direction: column; gap: 24px; }
.settings-card {
  background: var(--ag-bg-card, white);
  border: 1px solid color-mix(in srgb, var(--ag-border) 30%, transparent);
  border-radius: 16px;
  padding: 28px;
}
@media (max-width: 768px) { .settings-card { padding: 20px; } }

.settings-form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}
@media (max-width: 640px) { .settings-form-grid { grid-template-columns: 1fr; } }
.settings-field { display: flex; flex-direction: column; gap: 6px; }
.settings-field-full { grid-column: 1 / -1; }
.settings-label {
  font-family: var(--ag-font-body);
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--ag-text-secondary);
}
.settings-input {
  width: 100%;
  height: 44px;
  padding: 0 14px;
  border: 2px solid var(--ag-border);
  border-radius: 10px;
  font-family: var(--ag-font-body);
  font-size: 14px;
  color: var(--ag-text-primary);
  background: var(--ag-bg-card, white);
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}
.settings-input:focus {
  border-color: var(--ag-primary-500);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ag-primary-500) 10%, transparent);
}
.settings-input-disabled {
  background: var(--ag-surface-container-low);
  color: var(--ag-text-muted);
  cursor: not-allowed;
}
.settings-textarea {
  height: auto;
  padding: 12px 14px;
  resize: vertical;
  min-height: 80px;
}
.settings-error {
  font-family: var(--ag-font-body);
  font-size: 12px;
  color: var(--ag-error, #dc2626);
}
.settings-hint {
  font-family: var(--ag-font-body);
  font-size: 12px;
  color: var(--ag-text-muted);
}
.settings-success {
  padding: 12px 16px;
  border-radius: 10px;
  background: color-mix(in srgb, var(--ag-primary-500) 8%, transparent);
  color: var(--ag-primary-500);
  font-family: var(--ag-font-body);
  font-size: 13px;
  font-weight: 500;
}
.settings-actions { display: flex; justify-content: flex-end; margin-top: 24px; }
.settings-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  border-radius: 10px;
  font-family: var(--ag-font-body);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}
.settings-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.settings-btn-primary {
  background: var(--ag-primary-500);
  color: white;
}
.settings-btn-primary:hover:not(:disabled) { background: var(--ag-primary-600); }

.settings-toggles { display: flex; flex-direction: column; gap: 0; }
.settings-toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0;
  border-bottom: 1px solid color-mix(in srgb, var(--ag-border) 50%, transparent);
}
.settings-toggle-row:last-child { border-bottom: none; }
.settings-toggle-label {
  font-family: var(--ag-font-body);
  font-size: 14px;
  font-weight: 500;
  color: var(--ag-text-primary);
}
.settings-toggle-desc {
  font-family: var(--ag-font-body);
  font-size: 12px;
  color: var(--ag-text-muted);
  margin-top: 2px;
}
.toggle-switch { position: relative; width: 44px; height: 24px; flex-shrink: 0; }
.toggle-checkbox { opacity: 0; width: 0; height: 0; position: absolute; }
.toggle-label {
  position: absolute;
  inset: 0;
  background: var(--ag-border);
  border-radius: 9999px;
  cursor: pointer;
  transition: all 0.3s;
}
.toggle-label::before {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 20px;
  height: 20px;
  background: white;
  border-radius: 50%;
  transition: all 0.3s;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}
.toggle-checkbox:checked + .toggle-label {
  background: var(--ag-primary-500);
}
.toggle-checkbox:checked + .toggle-label::before {
  transform: translateX(20px);
}
</style>

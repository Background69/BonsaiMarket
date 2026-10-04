<template>
  <MarketplaceLayout>
    <main style="padding-top: 80px;">
      <div class="checkout-layout">
        <div class="checkout-summary">
          <div class="checkout-summary-header">
            <h1 class="checkout-summary-title">Đơn hàng của bạn</h1>
            <span class="checkout-summary-count">{{ cartItems.length }} Sản phẩm</span>
          </div>
          <div class="checkout-items">
            <div v-for="item in cartItems" :key="item.id" class="checkout-item">
              <div class="checkout-item-image">
                <img v-if="item.product?.image" :src="item.product.image" :alt="item.product.name" class="w-full h-full object-cover" />
                <span v-else class="checkout-item-char">{{ item.product?.name?.charAt(0)?.toUpperCase() }}</span>
              </div>
              <div class="checkout-item-info">
                <div class="checkout-item-top">
                  <div>
                    <h3 class="checkout-item-name">{{ item.product?.name }}</h3>
                  </div>
                  <span class="checkout-item-price">{{ formatPrice(item.product?.price) }}₫</span>
                </div>
                <div class="checkout-item-actions">
                  <div class="checkout-qty">
                    <span class="checkout-qty-value">x{{ item.quantity }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="checkout-flow">
          <div class="checkout-card">
            <div class="checkout-progress">
              <div class="checkout-progress-bar">
                <div class="checkout-progress-line"></div>
                <div class="checkout-progress-fill" :style="{ transform: 'translateX(-60px) scaleX(' + (currentStep / totalSteps) + ')' }"></div>
                <div class="checkout-step" :class="currentStep >= 1 ? 'checkout-step-active' : ''">
                  <div class="checkout-step-icon">
                    <span class="material-symbols-outlined" style="font-size: 14px;">local_shipping</span>
                  </div>
                  <span class="checkout-step-label">Giao hàng</span>
                </div>
                <div class="checkout-step" :class="currentStep >= 2 ? 'checkout-step-active' : ''">
                  <div class="checkout-step-icon">
                    <span class="material-symbols-outlined" style="font-size: 14px;">verified</span>
                  </div>
                  <span class="checkout-step-label">Xác nhận</span>
                </div>
              </div>
            </div>

            <div class="checkout-content">
              <div v-show="currentStep === 1" class="step-transition">
                <h2 class="checkout-step-title">Địa chỉ giao hàng</h2>
                <div v-if="!addresses.length && !showNewAddress" class="checkout-address-empty">
                  <span class="material-symbols-outlined">location_off</span>
                  <p>Bạn chưa có địa chỉ nào được lưu. Vui lòng thêm địa chỉ giao hàng mới bên dưới.</p>
                </div>
                <div v-if="addresses.length" class="checkout-address-list">
                  <label v-for="addr in addresses" :key="addr.id"
                    class="checkout-address-item"
                    :class="{ 'checkout-address-selected': selectedAddrId === addr.id }">
                    <input type="radio" :value="addr.id" v-model="selectedAddrId" class="checkout-radio" />
                    <div class="checkout-address-info">
                      <div class="checkout-address-name">
                        {{ addr.recipient_name }}
                        <span class="checkout-address-phone">— {{ addr.phone }}</span>
                        <span v-if="addr.is_default" class="checkout-address-badge">Mặc định</span>
                      </div>
                      <p class="checkout-address-detail">{{ addr.address_detail }}, {{ addr.ward }}, {{ addr.district }}, {{ addr.province }}</p>
                    </div>
                  </label>
                </div>
                <div class="checkout-address-actions">
                  <button type="button" @click="showNewAddress = !showNewAddress" class="checkout-address-toggle">
                    <span class="material-symbols-outlined" style="font-size: 18px;">add</span>
                    {{ showNewAddress ? 'Đóng' : (addresses.length ? 'Địa chỉ mới' : 'Thêm địa chỉ') }}
                  </button>
                </div>
                <div v-if="showNewAddress" class="checkout-new-address">
                  <div class="checkout-form-grid">
                    <div>
                      <label class="checkout-label">Người nhận *</label>
                      <input v-model="newAddr.recipient_name" class="checkout-input" type="text" placeholder="Nguyễn Văn A" />
                    </div>
                    <div>
                      <label class="checkout-label">Số điện thoại *</label>
                      <input v-model="newAddr.phone" class="checkout-input" type="tel" placeholder="0987654321" />
                    </div>
                  </div>
                  <div class="checkout-form-grid checkout-form-grid-3">
                    <div>
                      <label class="checkout-label">Tỉnh/Thành *</label>
                      <select v-model="newProvince" @change="onProvinceChange" class="checkout-input" :disabled="addressLoading">
                        <option value="">Chọn</option>
                        <option v-for="p in provinces" :key="p.province_id" :value="p.province_id">{{ p.province_name }}</option>
                      </select>
                    </div>
                    <div>
                      <label class="checkout-label">Quận/Huyện *</label>
                      <select v-model="newDistrict" @change="onDistrictChange" class="checkout-input" :disabled="!newProvince || addressLoading">
                        <option value="">Chọn</option>
                        <option v-for="d in newDistricts" :key="d.district_id" :value="d.district_id">{{ d.district_name }}</option>
                      </select>
                    </div>
                    <div>
                      <label class="checkout-label">Phường/Xã *</label>
                      <select v-model="newWard" class="checkout-input" :disabled="!newDistrict || addressLoading">
                        <option value="">Chọn</option>
                        <option v-for="w in newWards" :key="w.ward_code" :value="w.ward_code">{{ w.ward_name }}</option>
                      </select>
                    </div>
                  </div>
                  <div>
                    <label class="checkout-label">Địa chỉ cụ thể *</label>
                    <input v-model="newAddr.address_detail" class="checkout-input" type="text" placeholder="Số nhà, tên đường..." />
                  </div>
                </div>

                <div v-if="shippingFee > 0" class="checkout-shipping-section">
                  <h3 class="checkout-step-subtitle">Đơn vị vận chuyển</h3>
                  <div class="checkout-shipping-card">
                    <img src="/images/ghtk-logo.svg" alt="Giao hàng tiết kiệm" class="checkout-shipping-logo" />
                    <div class="checkout-shipping-card-info">
                      <span class="checkout-shipping-card-name">Giao hàng tiết kiệm</span>
                      <span class="checkout-shipping-card-fee">{{ formatPrice(shippingFee) }}₫</span>
                    </div>
                  </div>
                </div>

                <div class="checkout-notes-section" style="margin-top: 20px;">
                  <label class="checkout-label">Ghi chú cho người bán</label>
                  <textarea v-model="form.notes" class="checkout-textarea" placeholder="Ghi chú thêm (không bắt buộc)..."></textarea>
                </div>
              </div>

              <div v-show="currentStep === 2" class="step-transition">
                <h2 class="checkout-step-title">Xác nhận đơn hàng</h2>
                <div class="checkout-review-rows">
                  <div class="checkout-review-row">
                    <span class="checkout-review-label">Giao đến</span>
                    <span class="checkout-review-value">{{ addressDisplay }}</span>
                  </div>
                  <div class="checkout-review-row">
                    <span class="checkout-review-label">Vận chuyển</span>
                    <span class="checkout-review-value">Giao hàng tiết kiệm</span>
                  </div>
                  <div v-if="form.notes" class="checkout-review-row">
                    <span class="checkout-review-label">Ghi chú</span>
                    <span class="checkout-review-value">{{ form.notes }}</span>
                  </div>
                </div>

                <div class="checkout-review-info-box">
                  <span class="material-symbols-outlined" style="color: var(--ag-warning); font-size: 20px;">info</span>
                  <span class="checkout-review-info-text">
                    Sau khi đặt hàng, vui lòng liên hệ trực tiếp với người bán để thỏa thuận phương thức thanh toán và xác nhận đơn hàng.
                  </span>
                </div>
              </div>
            </div>

            <div class="checkout-price-summary">
              <div class="checkout-price-row">
                <span>Tạm tính</span>
                <span>{{ formatPrice(subtotal) }}₫</span>
              </div>
              <div class="checkout-price-row">
                <span>Phí vận chuyển</span>
                <span class="checkout-price-value">{{ shippingFee ? formatPrice(shippingFee) + '₫' : '—' }}</span>
              </div>
              <div class="checkout-price-divider"></div>
              <div class="checkout-price-total-row">
                <span class="checkout-total-label">Tổng cộng</span>
                <span class="checkout-total-value">{{ formatPrice(total) }}₫</span>
              </div>
            </div>

            <button @click="handleNext" class="checkout-action-btn" :disabled="processing">
              <span v-if="processing" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
              <span v-else>{{ buttonText }}</span>
            </button>
          </div>
        </div>
      </div>
    </main>
  </MarketplaceLayout>
</template>

<script setup>
import { ref, computed, reactive, onMounted, watch } from 'vue';
import { router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';
import { formatPrice } from '@agriverse/utils';
import { useToast } from 'primevue/usetoast';

const toast = useToast();

const props = defineProps({
  cartItems: { type: Array, default: () => [] },
  addresses: { type: Array, default: () => [] },
  provinces: { type: Array, default: () => [] },
});

const currentStep = ref(1);
const totalSteps = 2;
const processing = ref(false);
const selectedAddrId = ref(null);
const showNewAddress = ref(false);
const addressLoading = ref(false);

const form = reactive({ shipping_address: '', notes: '', shipping_method: '', shipping_fee: 0 });

const newAddr = reactive({ recipient_name: '', phone: '', address_detail: '', province: '', district: '', ward: '' });

const newDistricts = ref([]);
const newWards = ref([]);
const newProvince = ref('');
const newDistrict = ref('');
const newWard = ref('');

const shippingServices = ref([]);
const selectedService = ref(null);
const shippingFee = ref(0);

const progressWidth = computed(() => `${(currentStep.value / totalSteps) * 100}%`);

const buttonText = computed(() => {
  if (currentStep.value === 1) return 'Xem lại đơn hàng';
  return 'Đặt hàng';
});

const subtotal = computed(() =>
  props.cartItems.reduce((sum, item) => sum + (Number(item.product?.price) || 0) * (item.quantity || 0), 0)
);

const total = computed(() => {
  const s = Number(subtotal.value) || 0;
  const f = Number(shippingFee.value) || 0;
  return Math.max(0, s + f);
});

const addressDisplay = computed(() => {
  const addr = props.addresses?.find(a => a.id === selectedAddrId.value);
  if (addr) return `${addr.recipient_name}, ${addr.address_detail}, ${addr.ward}, ${addr.district}, ${addr.province}`;
  if (showNewAddress.value && newAddr.recipient_name) {
    const parts = [newAddr.address_detail, newAddr.ward, newAddr.district, newAddr.province].filter(Boolean);
    return `${newAddr.recipient_name}, ${parts.join(', ') || '...'}`;
  }
  return 'Chưa chọn địa chỉ';
});

const totalQty = computed(() =>
  props.cartItems.reduce((sum, item) => sum + item.quantity, 0)
);

onMounted(() => {
  const def = props.addresses?.find(a => a.is_default);
  if (def) selectedAddrId.value = def.id;
  else if (props.addresses?.length) selectedAddrId.value = props.addresses[0].id;
  else showNewAddress.value = true;
});

watch(selectedAddrId, () => {
  showNewAddress.value = false;
  fetchShippingServices();
});

watch(newWard, (val) => {
  const w = newWards.value?.find(x => String(x.ward_code) === String(val));
  newAddr.ward = w?.ward_name || '';
  if (val) fetchShippingServices();
});

watch(newProvince, (val) => {
  const p = props.provinces?.find(x => String(x.province_id) === String(val));
  newAddr.province = p?.province_name || '';
});

watch(newDistrict, (val) => {
  const d = newDistricts.value?.find(x => String(x.district_id) === String(val));
  newAddr.district = d?.district_name || '';
});

function getProvinceName() {
  // Lấy tên tỉnh từ địa chỉ đã chọn
  const addr = props.addresses?.find(a => a.id === selectedAddrId.value);
  if (addr?.province) return addr.province;
  // Fallback: tìm từ provinces list theo province_id
  if (newProvince.value) {
    const found = props.provinces?.find(p => String(p.province_id) === String(newProvince.value));
    if (found) return found.province_name;
  }
  return 'Hồ Chí Minh';
}

function getDistrictName(districtId) {
  if (!districtId) return 'Quận 1';
  const addr = props.addresses?.find(a => a.id === selectedAddrId.value);
  if (addr?.district) return addr.district;
  const found = newDistricts.value?.find(d => d.district_id === districtId);
  return found?.district_name || 'Quận 1';
}

function fetchShippingServices() {
  let districtId = null;

  if (selectedAddrId.value) {
    const addr = props.addresses.find(a => a.id === selectedAddrId.value);
    if (addr?.ghn_district_id) districtId = parseInt(addr.ghn_district_id);
  }

  if (showNewAddress.value) {
    districtId = newDistrict.value ? parseInt(newDistrict.value) : null;
  }

  if (!districtId || isNaN(districtId)) {
    shippingFee.value = 35000;
    form.shipping_method = 'ghtk';
    form.shipping_fee = 35000;
    return;
  }

  const weight = Math.max(1, Math.round(Number(totalQty.value) || 1) * 500);
  const amount = Math.max(0, Math.round(Number(subtotal.value) || 0));

  window.axios.post('/agriverse/api/ghtk/shipping-fee', {
    pick_province: 'Hồ Chí Minh',
    deliver_province_name: getProvinceName(),
    deliver_district_name: getDistrictName(districtId),
    weight,
    amount,
  }).then(({ data }) => {
    const list = Array.isArray(data?.data) ? data.data : [];
    const first = list[0];
    const fee = Number(first?.fee) || 35000;
    shippingFee.value = fee;
    form.shipping_method = 'ghtk';
    form.shipping_fee = fee;
  }).catch(() => {
    shippingFee.value = 35000;
    form.shipping_method = 'ghtk';
    form.shipping_fee = 35000;
  });
}

function selectService(svc) {
  selectedService.value = svc;
  shippingFee.value = Number(svc.fee) || 0;
  form.shipping_method = String(svc.service_id);
  form.shipping_fee = Number(svc.fee) || 0;
}

async function onProvinceChange() {
  newDistrict.value = '';
  newWard.value = '';
  newDistricts.value = [];
  newWards.value = [];
  const p = props.provinces?.find(x => String(x.province_id) === String(newProvince.value));
  newAddr.province = p?.province_name || '';
  if (!newProvince.value) return;
  addressLoading.value = true;
  try {
    const { data } = await window.axios.post('/agriverse/api/ghtk/districts', { province_id: newProvince.value });
    newDistricts.value = data.data || [];
  } catch { newDistricts.value = []; }
  finally { addressLoading.value = false; }
}
async function onDistrictChange() {
  newWard.value = '';
  newWards.value = [];
  const d = newDistricts.value?.find(x => String(x.district_id) === String(newDistrict.value));
  newAddr.district = d?.district_name || '';
  if (!newDistrict.value) return;
  addressLoading.value = true;
  try {
    const { data } = await window.axios.post('/agriverse/api/ghtk/wards', { district_id: newDistrict.value });
    newWards.value = data.data || [];
  } catch { newWards.value = []; }
  finally { addressLoading.value = false; }
}

function handleNext() {
  if (processing.value) return;

  if (currentStep.value === 1) {
    if (!selectedAddrId.value && !showNewAddress.value) {
      toast.add({ severity: 'error', summary: 'Vui lòng chọn hoặc nhập địa chỉ giao hàng', life: 3000 });
      return;
    }
    if (showNewAddress.value) {
      if (!newAddr.recipient_name || !newAddr.phone || !newAddr.address_detail || !newProvince || !newDistrict || !newWard) {
        toast.add({ severity: 'error', summary: 'Vui lòng điền đầy đủ thông tin địa chỉ mới', life: 3000 });
        return;
      }
    }
    if (!shippingFee.value) {
      toast.add({ severity: 'error', summary: 'Đang tính phí vận chuyển, vui lòng đợi...', life: 3000 });
      return;
    }
    currentStep.value++;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  } else {
    submitOrder();
  }
}

function submitOrder() {
  if (processing.value) return;
  processing.value = true;
  form.shipping_address = addressDisplay.value;

  const pVal = String(newProvince.value || '');
  const dVal = String(newDistrict.value || '');
  const wVal = String(newWard.value || '');

  console.log('DEBUG submitOrder:', { pVal, dVal, wVal, showNew: showNewAddress.value, province: newAddr.province, district: newAddr.district, ward: newAddr.ward });

  const payload = {
    shipping_address: form.shipping_address,
    shipping_method: form.shipping_method || 'ghtk',
    shipping_fee: form.shipping_fee || 0,
    notes: form.notes || '',
  };

  if (showNewAddress.value && (pVal || newAddr.recipient_name)) {
    payload.new_address = {
      recipient_name: newAddr.recipient_name,
      phone: newAddr.phone,
      province_id: parseInt(pVal) || 0,
      province_name: newAddr.province || '',
      district_id: parseInt(dVal) || 0,
      district_name: newAddr.district || '',
      ward_code: wVal,
      ward_name: newAddr.ward || '',
      address_detail: newAddr.address_detail,
    };
  }

  console.log('DEBUG payload:', payload);

  router.post(route('agriverse.api.checkout.process'), payload, {
    onFinish: () => { processing.value = false; },
  });
}
</script>

<style scoped>
.checkout-layout {
  max-width: var(--ag-container-max, 1280px);
  margin: 0 auto;
  padding: 0 var(--ag-margin-desktop, 64px) 80px;
  display: grid;
  grid-template-columns: 1fr;
  gap: 48px;
}
@media (min-width: 1024px) {
  .checkout-layout { grid-template-columns: 7fr 5fr; gap: 64px; }
}
@media (max-width: 768px) {
  .checkout-layout { padding: 0 var(--ag-margin-mobile, 20px) 48px; }
  .checkout-summary-title { font-size: 28px; }
  .checkout-item { gap: 16px; }
  .checkout-item-name { font-size: 16px; }
  .checkout-item-price { font-size: 14px; }
}

.checkout-summary-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  border-bottom: 1px solid color-mix(in srgb, var(--ag-border) 10%, transparent);
  padding-bottom: 16px;
  margin-bottom: 24px;
}
.checkout-summary-title {
  font-family: var(--ag-font-display);
  font-size: 36px;
  font-weight: 500;
  color: var(--ag-text-primary);
  letter-spacing: -0.01em;
  line-height: 42px;
}
@media (min-width: 768px) {
  .checkout-summary-title { font-size: 40px; }
}
.checkout-summary-count {
  font-family: var(--ag-font-body);
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--ag-text-secondary);
}

.checkout-items { display: flex; flex-direction: column; }
.checkout-item {
  display: flex;
  gap: 24px;
  padding: 16px 0;
  border-bottom: 1px solid color-mix(in srgb, var(--ag-border) 5%, transparent);
  align-items: center;
}
.checkout-item:last-child { border-bottom: none; }
.checkout-item-image {
  width: 72px;
  height: 72px;
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--ag-surface-container);
  display: flex;
  align-items: center;
  justify-content: center;
}
.checkout-item-char {
  font-family: var(--ag-font-display);
  font-size: 28px;
  color: color-mix(in srgb, var(--ag-border) 20%, transparent);
}
.checkout-item-info { flex: 1; display: flex; flex-direction: column; gap: 8px; }
.checkout-item-top { display: flex; justify-content: space-between; align-items: flex-start; }
.checkout-item-name {
  font-family: var(--ag-font-display);
  font-size: 20px;
  font-weight: 500;
  color: var(--ag-text-primary);
  line-height: 28px;
}
.checkout-item-price {
  font-family: var(--ag-font-body);
  font-size: 16px;
  font-weight: 600;
  color: var(--ag-primary-500);
  white-space: nowrap;
}
.checkout-item-actions { display: flex; align-items: center; }
.checkout-qty { display: flex; align-items: center; }
.checkout-qty-value {
  padding: 2px 10px;
  font-family: var(--ag-font-body);
  font-size: 13px;
  font-weight: 600;
  color: var(--ag-text-secondary);
  background: var(--ag-bg);
  border-radius: 6px;
}

.checkout-flow {}
.checkout-card {
  background: white;
  border-radius: 16px;
  border: 1px solid color-mix(in srgb, var(--ag-border) 8%, transparent);
  overflow: hidden;
  position: sticky;
  top: 100px;
}

.checkout-progress {
  padding: 28px 32px;
  border-bottom: 1px solid color-mix(in srgb, var(--ag-border) 5%, transparent);
  background: rgba(252, 249, 248, 0.5);
}
@media (max-width: 640px) {
  .checkout-progress { padding: 20px 16px; }
}
.checkout-progress-bar {
  display: flex;
  justify-content: center;
  position: relative;
  padding: 0 32px;
  gap: 80px;
}
@media (max-width: 640px) {
  .checkout-progress-bar { gap: 40px; padding: 0 16px; }
  .checkout-progress-line { width: 60px; transform: translateX(-30px); }
  .checkout-progress-fill { transform: translateX(-30px) scaleX(0); }
}
.checkout-progress-line {
  position: absolute;
  top: 20px;
  left: 50%;
  width: 120px;
  height: 1px;
  background: color-mix(in srgb, var(--ag-border) 10%, transparent);
  z-index: 0;
  transform: translateX(-60px);
}
.checkout-progress-fill {
  position: absolute;
  top: 20px;
  left: 50%;
  width: 120px;
  height: 2px;
  background: var(--ag-primary-500);
  z-index: 0;
  transform-origin: left;
  transition: transform 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  transform: translateX(-60px) scaleX(0);
}
.checkout-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  z-index: 10;
  position: relative;
}
.checkout-step-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: white;
  border: 1px solid color-mix(in srgb, var(--ag-border) 20%, transparent);
  color: var(--ag-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.4s;
}
.checkout-step-active .checkout-step-icon {
  background: var(--ag-primary-500);
  border-color: var(--ag-primary-500);
  color: white;
}
.checkout-step-label {
  font-family: var(--ag-font-body);
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.15em;
  color: var(--ag-text-secondary);
  transition: color 0.3s;
}
.checkout-step-active .checkout-step-label { color: var(--ag-primary-500); }

.checkout-content { padding: 32px; }
@media (max-width: 640px) { .checkout-content { padding: 20px 16px; } }
.checkout-step-title {
  font-family: var(--ag-font-display);
  font-size: 24px;
  font-weight: 500;
  color: var(--ag-text-primary);
  margin-bottom: 24px;
  line-height: 32px;
}
.checkout-step-subtitle {
  font-family: var(--ag-font-body);
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--ag-text-secondary);
  margin-bottom: 12px;
  margin-top: 24px;
}
.step-transition { animation: fadeIn 0.4s ease-out; }
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.checkout-address-list { display: flex; flex-direction: column; gap: 12px; margin-bottom: 16px; }
.checkout-address-empty {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-radius: 12px;
  border: 1px dashed color-mix(in srgb, var(--ag-text-muted) 40%, transparent);
  background: var(--ag-surface-container-low);
  color: var(--ag-text-secondary);
  font-family: var(--ag-font-body);
  font-size: 13px;
  line-height: 20px;
  margin-bottom: 16px;
}
.checkout-address-empty .material-symbols-outlined {
  color: var(--ag-text-muted);
  font-size: 22px;
  flex-shrink: 0;
}
.checkout-address-empty p { margin: 0; }
.checkout-address-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  border-radius: 12px;
  border: 2px solid color-mix(in srgb, var(--ag-border) 12%, transparent);
  cursor: pointer;
  transition: all 0.2s;
}
.checkout-address-item:hover { border-color: color-mix(in srgb, var(--ag-border) 25%, transparent); }
.checkout-address-selected { border-color: var(--ag-primary-500); background: rgba(72, 103, 48, 0.04); }
.checkout-radio { margin-top: 2px; accent-color: var(--ag-primary-500); }
.checkout-address-info { flex: 1; }
.checkout-address-name {
  font-family: var(--ag-font-body);
  font-size: 14px;
  font-weight: 600;
  color: var(--ag-text-primary);
  margin-bottom: 4px;
}
.checkout-address-phone { font-weight: 400; color: var(--ag-text-secondary); font-size: 13px; }
.checkout-address-badge {
  display: inline-block;
  margin-left: 8px;
  padding: 1px 8px;
  border-radius: 9999px;
  background: rgba(72, 103, 48, 0.08);
  color: var(--ag-primary-500);
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
}
.checkout-address-detail {
  font-family: var(--ag-font-body);
  font-size: 13px;
  color: var(--ag-text-secondary);
  line-height: 18px;
}
.checkout-address-actions { margin-bottom: 16px; }
.checkout-address-toggle {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  border: 2px dashed color-mix(in srgb, var(--ag-border) 20%, transparent);
  border-radius: 10px;
  background: transparent;
  color: var(--ag-text-secondary);
  font-family: var(--ag-font-body);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.checkout-address-toggle:hover { border-color: rgba(72, 103, 48, 0.3); color: var(--ag-primary-500); }

.checkout-new-address {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-top: 16px;
  border-top: 1px solid color-mix(in srgb, var(--ag-border) 8%, transparent);
}
.checkout-form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
@media (max-width: 640px) { .checkout-form-grid { grid-template-columns: 1fr; } }
.checkout-form-grid-3 { grid-template-columns: 1fr 1fr 1fr; }
@media (max-width: 768px) { .checkout-form-grid-3 { grid-template-columns: 1fr; } }

.checkout-label {
  display: block;
  font-family: var(--ag-font-body);
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--ag-text-secondary);
  margin-bottom: 4px;
}
.checkout-input {
  width: 100%;
  background: var(--ag-bg);
  border: 1px solid color-mix(in srgb, var(--ag-border) 15%, transparent);
  border-radius: 8px;
  padding: 10px 14px;
  font-family: var(--ag-font-body);
  font-size: 14px;
  color: var(--ag-text-primary);
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}
.checkout-input:focus { border-color: var(--ag-primary-500); box-shadow: 0 0 0 3px rgba(72, 103, 48, 0.08); }
.checkout-input::placeholder { color: color-mix(in srgb, var(--ag-border) 30%, transparent); }
select.checkout-input {
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2374796c' d='M6 8L1 3h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  padding-right: 32px;
}

.checkout-shipping-section { margin-top: 8px; }
.checkout-shipping-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-radius: 12px;
  border: 2px solid var(--ag-primary-500);
  background: rgba(72, 103, 48, 0.04);
}
.checkout-shipping-logo { height: 28px; width: auto; }
.checkout-shipping-card-info {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.checkout-shipping-card-name { font-size: 14px; font-weight: 600; color: var(--ag-text-primary); font-family: var(--ag-font-body); }
.checkout-shipping-card-fee { font-size: 14px; font-weight: 700; color: var(--ag-primary-500); font-family: var(--ag-font-body); }

.checkout-notes-section { margin-bottom: 8px; }
.checkout-textarea {
  width: 100%;
  height: 72px;
  background: var(--ag-bg);
  border: 1px solid color-mix(in srgb, var(--ag-border) 15%, transparent);
  border-radius: 8px;
  padding: 12px 14px;
  font-family: var(--ag-font-body);
  font-size: 14px;
  color: var(--ag-text-primary);
  outline: none;
  resize: none;
  transition: all 0.2s;
  box-sizing: border-box;
}
.checkout-textarea:focus { border-color: var(--ag-primary-500); box-shadow: 0 0 0 3px rgba(72, 103, 48, 0.08); }

.checkout-review-rows { display: flex; flex-direction: column; margin-bottom: 20px; }
.checkout-review-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid color-mix(in srgb, var(--ag-border) 8%, transparent);
}
.checkout-review-label { font-family: var(--ag-font-body); font-size: 14px; color: var(--ag-text-secondary); }
.checkout-review-value {
  font-family: var(--ag-font-body);
  font-size: 14px;
  font-weight: 500;
  color: var(--ag-text-primary);
  text-align: right;
  max-width: 60%;
}
.checkout-review-info-box {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px;
  background: rgba(217, 119, 6, 0.06);
  border-radius: 12px;
  border: 1px solid rgba(217, 119, 6, 0.12);
}
.checkout-review-info-text {
  font-family: var(--ag-font-body);
  font-size: 13px;
  line-height: 18px;
  color: var(--ag-warning);
}

.checkout-price-summary {
  padding: 0 32px 8px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
@media (max-width: 640px) { .checkout-price-summary { padding: 0 16px 8px; } }
.checkout-price-row {
  display: flex;
  justify-content: space-between;
  font-family: var(--ag-font-body);
  font-size: 14px;
  color: var(--ag-text-secondary);
}
.checkout-price-value { font-weight: 500; }
.checkout-price-divider { height: 1px; background: color-mix(in srgb, var(--ag-border) 10%, transparent); }
.checkout-price-total-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding-top: 12px;
  border-top: 1px solid color-mix(in srgb, var(--ag-border) 10%, transparent);
}
.checkout-total-label {
  font-family: var(--ag-font-body);
  font-size: 20px;
  font-weight: 500;
  color: var(--ag-text-primary);
}
.checkout-total-value {
  font-family: var(--ag-font-display);
  font-size: 30px;
  font-weight: 500;
  color: var(--ag-primary-500);
  line-height: 40px;
}

.checkout-action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: calc(100% - 64px);
  margin: 24px 32px 24px;
}
@media (max-width: 640px) { .checkout-action-btn { width: calc(100% - 32px); margin: 24px 16px 24px; } }
.checkout-action-btn {
  padding: 16px 24px;
  background: var(--ag-primary-500);
  color: white;
  border: none;
  border-radius: 9999px;
  font-family: var(--ag-font-body);
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.2em;
  cursor: pointer;
  transition: all 0.3s;
}
.checkout-action-btn:hover { background: var(--ag-primary-600); }
.checkout-action-btn:active { transform: scale(0.98); }
.checkout-action-btn:disabled { opacity: 0.6; cursor: not-allowed; }
</style>

<template>
  <MarketplaceLayout>
    <section class="max-w-[1320px] mx-auto px-5 py-8" style="font-family: var(--ag-font-body);">
      <Link :href="route('agriverse.shop.seller.orders.show', order.id)"
        class="inline-flex items-center gap-1.5 text-sm font-semibold mb-6"
        style="color: var(--ag-text-secondary); text-decoration: none;">
        <span class="material-symbols-outlined text-lg">arrow_back</span>
        Quay lại đơn hàng
      </Link>

      <div class="grid grid-cols-12 gap-6">
        <div class="col-span-12 md:col-span-8">
          <h1 class="text-2xl font-bold mb-6" style="font-family: var(--ag-font-display); color: var(--ag-text-primary);">Tạo vận đơn GHN</h1>

          <!-- Order Info -->
          <div class="bg-white rounded-2xl border mb-6" style="border-color: var(--ag-border); padding: 20px;">
            <h2 class="text-sm font-bold mb-3" style="color: var(--ag-text-primary);">Thông tin đơn hàng</h2>
            <div class="grid grid-cols-2 gap-3 text-sm" style="color: var(--ag-text-secondary);">
              <div><span style="color: var(--ag-text-muted);">Mã đơn:</span> <strong style="color: var(--ag-text-primary);">#{{ order.id }}</strong></div>
              <div><span style="color: var(--ag-text-muted);">Sản phẩm:</span> <strong style="color: var(--ag-text-primary);">{{ order.product?.name }}</strong></div>
              <div><span style="color: var(--ag-text-muted);">Số lượng:</span> <strong style="color: var(--ag-text-primary);">x{{ order.quantity }}</strong></div>
              <div><span style="color: var(--ag-text-muted);">Tổng tiền:</span> <strong style="color: var(--ag-text-primary);">{{ formatPrice(order.total_amount) }}₫</strong></div>
              <div class="col-span-2"><span style="color: var(--ag-text-muted);">Địa chỉ:</span> <strong style="color: var(--ag-text-primary);">{{ order.shipping_address }}</strong></div>
            </div>
          </div>

          <!-- Shipping Form -->
          <div class="bg-white rounded-2xl border" style="border-color: var(--ag-border); padding: 24px;">
            <h2 class="text-sm font-bold mb-4" style="color: var(--ag-text-primary);">Thông tin vận chuyển</h2>

            <!-- Service Selection -->
            <div v-if="services.length" class="mb-5">
              <label class="text-sm font-semibold mb-3 block" style="color: var(--ag-text-primary);">Chọn dịch vụ vận chuyển</label>
              <div class="space-y-3">
                <label v-for="service in services" :key="service.service_id"
                  class="flex items-center gap-3 p-4 rounded-xl border cursor-pointer transition-all"
                  :style="selectedService === service.service_id
                    ? 'border-color: var(--ag-primary-500); background: color-mix(in srgb, var(--ag-primary-500) 4%, transparent);'
                    : 'border-color: var(--ag-border);'">
                  <input type="radio" :value="service.service_id" v-model="selectedService"
                    class="w-4 h-4" style="accent-color: var(--ag-primary-500);" />
                  <div>
                    <div class="text-sm font-semibold" style="color: var(--ag-text-primary);">{{ service.short_name || service.service_name || `Dịch vụ #${service.service_id}` }}</div>
                    <div class="text-xs" style="color: var(--ag-text-muted);">Mã dịch vụ: {{ service.service_id }}</div>
                  </div>
                </label>
              </div>
            </div>
            <div v-else class="mb-5 p-4 rounded-xl text-sm text-center" style="background: color-mix(in srgb, var(--ag-warning) 6%, transparent); color: var(--ag-warning);">
              Không tìm thấy dịch vụ vận chuyển phù hợp cho địa chỉ này.
            </div>

            <!-- Package Dimensions -->
            <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-5">
              <div>
                <label class="text-xs font-semibold mb-1.5 block" style="color: var(--ag-text-secondary);">Cân nặng (gram) *</label>
                <input v-model.number="weight" type="number" min="100" placeholder="500"
                  class="w-full h-11 px-4 rounded-xl border text-sm outline-none transition-all"
                  style="border-color: var(--ag-border); color: var(--ag-text-primary);" />
              </div>
              <div>
                <label class="text-xs font-semibold mb-1.5 block" style="color: var(--ag-text-secondary);">Dài (cm)</label>
                <input v-model.number="length" type="number" min="1" placeholder="10"
                  class="w-full h-11 px-4 rounded-xl border text-sm outline-none transition-all"
                  style="border-color: var(--ag-border); color: var(--ag-text-primary);" />
              </div>
              <div>
                <label class="text-xs font-semibold mb-1.5 block" style="color: var(--ag-text-secondary);">Rộng (cm)</label>
                <input v-model.number="width" type="number" min="1" placeholder="10"
                  class="w-full h-11 px-4 rounded-xl border text-sm outline-none transition-all"
                  style="border-color: var(--ag-border); color: var(--ag-text-primary);" />
              </div>
              <div>
                <label class="text-xs font-semibold mb-1.5 block" style="color: var(--ag-text-secondary);">Cao (cm)</label>
                <input v-model.number="height" type="number" min="1" placeholder="10"
                  class="w-full h-11 px-4 rounded-xl border text-sm outline-none transition-all"
                  style="border-color: var(--ag-border); color: var(--ag-text-primary);" />
              </div>
            </div>

            <!-- Submit -->
            <button @click="createShipment"
              :disabled="!canSubmit"
              class="w-full h-12 rounded-2xl text-sm font-semibold text-white transition-all"
              :style="canSubmit
                ? 'background: var(--ag-primary-500); border: none; cursor: pointer;'
                : 'background: var(--ag-border); color: var(--ag-text-muted); cursor: not-allowed;'">
              <span v-if="loading" class="material-symbols-outlined animate-spin inline-block text-lg">progress_activity</span>
              <span v-else>Tạo vận đơn</span>
            </button>
          </div>
        </div>

        <!-- Fee Preview -->
        <div class="col-span-12 md:col-span-4">
          <div class="bg-white rounded-2xl border" style="border-color: var(--ag-border); padding: 24px; position: sticky; top: 96px;">
            <h2 class="text-sm font-bold mb-4" style="color: var(--ag-text-primary);">Thông tin thêm</h2>
            <div class="text-sm space-y-3" style="color: var(--ag-text-secondary);">
              <p>Chọn dịch vụ vận chuyển và nhập kích thước gói hàng, sau đó nhấn "Tạo vận đơn" để tiến hành tạo đơn giao hàng qua GHN.</p>
              <div class="h-px" style="background: var(--ag-border);"></div>
              <p class="font-semibold" style="color: var(--ag-text-primary);">Lưu ý:</p>
              <ul class="list-disc pl-4 space-y-1 text-xs">
                <li>Cân nặng tối thiểu 100 gram</li>
                <li>Kích thước tối thiểu 1 cm</li>
                <li>Đơn hàng cần ở trạng thái "Đã xác nhận"</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </section>
  </MarketplaceLayout>
</template>

<script setup>
import { ref, computed } from 'vue';
import { Link, router } from '@inertiajs/vue3';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';
import { formatPrice } from '@agriverse/utils';

const props = defineProps({
  order: Object,
  services: Array,
  toDistrictId: Number,
});

const selectedService = ref(null);
const weight = ref(500);
const length = ref(10);
const width = ref(10);
const height = ref(10);
const loading = ref(false);

const canSubmit = computed(() => selectedService.value && weight.value >= 100);

function createShipment() {
  if (!canSubmit.value) return;
  loading.value = true;

  router.post(route('agriverse.shop.seller.orders.create-shipment', props.order.id), {
    service_id: selectedService.value,
    weight: weight.value,
    length: length.value || 10,
    width: width.value || 10,
    height: height.value || 10,
  }, {
    preserveScroll: true,
    onFinish: () => { loading.value = false; },
    onError: (e) => {
      alert(Object.values(e).join('\n'));
    },
  });
}
</script>

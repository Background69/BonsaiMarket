<template>
  <SellerLayout>
    <div class="max-w-5xl">
      <Link :href="route('agriverse.shop.seller.bonsai-passports.index')"
            class="inline-flex items-center gap-1 text-sm mb-6" style="color: var(--ag-primary-500);">
        <span class="material-symbols-outlined text-base">arrow_back</span> Quay lại
      </Link>

      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="text-2xl font-semibold" style="color: var(--ag-on-surface); font-family: var(--ag-font-display);">
            {{ bonsai.name }}</h1>
          <p class="text-sm mt-1 font-mono" style="color: var(--ag-on-surface-variant);">{{ bonsai.code }}</p>
        </div>
        <div v-if="!bonsai.is_minted" class="flex gap-2">
          <Link :href="route('agriverse.shop.seller.bonsai-passports.edit', bonsai.id)"
                class="px-4 py-2 rounded-xl text-sm font-semibold border transition-colors"
                style="border-color: var(--ag-border); color: var(--ag-text-secondary);">Sửa yêu cầu
          </Link>
          <button class="px-4 py-2 rounded-xl text-sm font-semibold border transition-colors" style="border-color: var(--ag-error, #fecaca); color: var(--ag-danger, #dc2626);"
                  @click="destroy">Xóa
          </button>
        </div>
      </div>

      <div v-if="flash.success" class="mb-4 px-4 py-3 rounded-xl text-sm font-semibold"
           style="background: #ecfdf5; color: #047857; border: 1px solid #a7f3d0;">{{ flash.success }}
      </div>
      <div v-if="flash.error" class="mb-4 px-4 py-3 rounded-xl text-sm font-semibold"
           style="background: #fef2f2; color: #b91c1c; border: 1px solid #fecaca;">{{ flash.error }}
      </div>

      <!-- Status banner -->
      <div :style="bannerStyle" class="mb-6 px-5 py-4 rounded-2xl border flex items-start gap-3">
        <span class="material-symbols-outlined mt-0.5">{{ bannerIcon }}</span>
        <div>
          <p class="text-sm font-semibold">{{ bannerTitle }}</p>
          <p class="text-sm mt-0.5 opacity-80">{{ bannerDescription }}</p>
        </div>
      </div>

      <div class="grid lg:grid-cols-3 gap-6 items-start">
        <div class="lg:col-span-2 space-y-6">
          <!-- Thông tin cây -->
          <div class="rounded-2xl border p-5" style="background: white; border-color: var(--ag-border);">
            <h3 class="text-sm font-semibold uppercase tracking-wider mb-4" style="color: var(--ag-text-secondary);">
              Thông tin cây</h3>
            <div class="grid md:grid-cols-2 gap-x-8 gap-y-3 text-sm">
              <div class="flex justify-between border-b py-2" style="border-color: var(--ag-border);"><span
                  style="color: var(--ag-text-secondary);">Loài</span><span class="font-medium"
                                                                            style="color: var(--ag-on-surface);">{{
                  bonsai.species || '—'
                }}</span></div>
              <div class="flex justify-between border-b py-2" style="border-color: var(--ag-border);"><span
                  style="color: var(--ag-text-secondary);">Biến thể</span><span class="font-medium"
                                                                                style="color: var(--ag-on-surface);">{{
                  bonsai.variety || '—'
                }}</span></div>
              <div class="flex justify-between border-b py-2" style="border-color: var(--ag-border);"><span
                  style="color: var(--ag-text-secondary);">Tuổi</span><span class="font-medium"
                                                                            style="color: var(--ag-on-surface);">{{
                  bonsai.age_years != null ? bonsai.age_years + ' năm' : '—'
                }}</span></div>
              <div class="flex justify-between border-b py-2" style="border-color: var(--ag-border);"><span
                  style="color: var(--ag-text-secondary);">Chiều cao</span><span class="font-medium"
                                                                                 style="color: var(--ag-on-surface);">{{
                  bonsai.height_cm != null ? bonsai.height_cm + ' cm' : '—'
                }}</span></div>
              <div class="flex justify-between border-b py-2" style="border-color: var(--ag-border);"><span
                  style="color: var(--ag-text-secondary);">Đường kính thân</span><span class="font-medium"
                                                                                       style="color: var(--ag-on-surface);">{{
                  bonsai.trunk_cm != null ? bonsai.trunk_cm + ' cm' : '—'
                }}</span></div>
              <div class="flex justify-between border-b py-2" style="border-color: var(--ag-border);"><span
                  style="color: var(--ag-text-secondary);">Nguồn gốc</span><span class="font-medium"
                                                                                 style="color: var(--ag-on-surface);">{{
                  bonsai.origin || '—'
                }}</span></div>
              <div class="md:col-span-2">
                <img v-if="bonsai.image_url" :src="bonsai.image_url"
                     class="w-full max-h-64 rounded-xl object-cover border" style="border-color: var(--ag-border);">
                <p v-if="bonsai.description" class="text-sm mt-3" style="color: var(--ag-text-primary);">
                  {{ bonsai.description }}</p>
              </div>
            </div>
          </div>

          <!-- Blockchain (khi đã mint) -->
          <div v-if="bonsai.is_minted" class="rounded-2xl border p-5"
               style="background: white; border-color: var(--ag-border);">
            <h3 class="text-sm font-semibold uppercase tracking-wider mb-4" style="color: var(--ag-text-secondary);">Hộ
              chiếu trên blockchain</h3>
            <div class="grid md:grid-cols-2 gap-x-8 gap-y-3 text-sm">
              <div class="flex justify-between border-b py-2" style="border-color: var(--ag-border);"><span
                  style="color: var(--ag-text-secondary);">Token ID</span><span class="font-mono font-bold"
                                                                                style="color: var(--ag-primary-600);">#{{
                  bonsai.token_id
                }}</span></div>
              <div class="flex justify-between border-b py-2" style="border-color: var(--ag-border);"><span
                  style="color: var(--ag-text-secondary);">Phiên bản metadata</span><span class="font-medium"
                                                                                          style="color: var(--ag-on-surface);">v{{
                  bonsai.metadata_version
                }}</span></div>
              <div class="flex justify-between border-b py-2" style="border-color: var(--ag-border);"><span
                  style="color: var(--ag-text-secondary);">CID (IPFS)</span><a v-if="bonsai.ipfs_gateway_uri"
                                                                               :href="bonsai.ipfs_gateway_uri"
                                                                               class="font-mono truncate max-w-[220px]"
                                                                               style="color: var(--ag-primary-500);"
                                                                               target="_blank">{{
                  bonsai.ipfs_cid || bonsai.ipfs_gateway_uri
                }}</a><span v-else class="font-mono">{{ bonsai.ipfs_cid || '—' }}</span></div>
              <div class="flex justify-between border-b py-2" style="border-color: var(--ag-border);"><span
                  style="color: var(--ag-text-secondary);">Giao dịch gần nhất</span><a v-if="bonsai.tx_explorer_url"
                                                                                       :href="bonsai.tx_explorer_url"
                                                                                       class="font-mono" style="color: var(--ag-primary-500);"
                                                                                       target="_blank">{{
                  (bonsai.last_sync_tx_hash || '').slice(0, 18)
                }}…</a><span v-else>—</span></div>
            </div>
            <div class="mt-4 p-4 rounded-xl flex items-center gap-3"
                 style="background: var(--ag-bg); border: 1px solid var(--ag-border);">
              <span class="material-symbols-outlined" style="color: var(--ag-primary-500);">qr_code</span>
              <div class="text-sm flex-1">
                <p class="font-semibold" style="color: var(--ag-on-surface);">Mã QR tra cứu công khai</p>
                <p class="text-xs mt-0.5 break-all" style="color: var(--ag-on-surface-variant);">{{
                    bonsai.lookup_url
                  }}</p>
              </div>
              <a :href="bonsai.lookup_url" class="px-4 py-2 rounded-xl text-sm font-semibold text-white" style="background: var(--ag-primary-500);"
                 target="_blank">Xem hộ chiếu</a>
            </div>
          </div>

          <!-- Nhật ký chăm sóc -->
          <div class="rounded-2xl border p-5" style="background: white; border-color: var(--ag-border);">
            <h3 class="text-sm font-semibold uppercase tracking-wider mb-4" style="color: var(--ag-text-secondary);">
              Nhật ký chăm sóc ({{ bonsai.logs?.length || 0 }})</h3>
            <div v-if="bonsai.logs?.length" class="space-y-3">
              <div v-for="log in bonsai.logs" :key="log.id" class="flex items-start gap-3 p-4 rounded-xl border"
                   style="border-color: var(--ag-border);">
                <span class="material-symbols-outlined mt-0.5"
                      style="color: var(--ag-primary-500);">{{ logIcon(log.event_type) }}</span>
                <div class="min-w-0 flex-1">
                  <div class="flex items-center gap-2 flex-wrap">
                    <span class="text-sm font-semibold" style="color: var(--ag-on-surface);">{{ log.title }}</span>
                    <span class="text-[11px] px-2 py-0.5 rounded-full font-semibold"
                          style="background: color-mix(in srgb, var(--ag-primary-500) 10%, transparent); color: var(--ag-primary-600);">{{
                        log.event_type
                      }}</span>
                  </div>
                  <div class="text-xs mt-0.5" style="color: var(--ag-on-surface-variant);">{{
                      formatDate(log.event_date)
                    }}
                  </div>
                  <p v-if="log.description" class="text-sm mt-1" style="color: var(--ag-text-primary);">
                    {{ log.description }}</p>
                  <img v-if="log.image_url" :src="log.image_url" class="mt-2 h-24 rounded-lg object-cover border"
                       style="border-color: var(--ag-border);">
                </div>
              </div>
            </div>
            <p v-else class="text-sm" style="color: var(--ag-text-muted);">Chưa có nhật ký chăm sóc. Admin có thể bổ
              sung sau khi duyệt.</p>
          </div>
        </div>

        <!-- Sidebar -->
        <div class="space-y-6">
          <div class="rounded-2xl border p-5" style="background: white; border-color: var(--ag-border);">
            <h3 class="text-sm font-semibold uppercase tracking-wider mb-4" style="color: var(--ag-text-secondary);">Chủ
              sở hữu</h3>
            <p class="text-sm font-medium" style="color: var(--ag-on-surface);">{{ bonsai.owner_name || '—' }}</p>
            <p class="font-mono text-xs mt-1 break-all" style="color: var(--ag-on-surface-variant);">
              {{ bonsai.owner_wallet || '—' }}</p>
          </div>

          <div class="rounded-2xl border p-5" style="background: white; border-color: var(--ag-border);">
            <h3 class="text-sm font-semibold uppercase tracking-wider mb-4" style="color: var(--ag-text-secondary);">
              Liên kết</h3>
            <Link v-if="bonsai.product" :href="route('agriverse.shop.seller.products.edit', bonsai.product.id)"
                  class="flex items-center gap-2 text-sm py-1.5" style="color: var(--ag-primary-500);">
              <span class="material-symbols-outlined text-base">inventory_2</span> Sản phẩm: {{ bonsai.product.name }}
            </Link>
            <a v-if="bonsai.is_minted && bonsai.lookup_url" :href="bonsai.lookup_url" class="flex items-center gap-2 text-sm py-1.5"
               style="color: var(--ag-primary-500);" target="_blank">
              <span class="material-symbols-outlined text-base">qr_code_2</span> Tra cứu công khai
            </a>
          </div>
        </div>
      </div>
    </div>
  </SellerLayout>
</template>

<script setup>
import {computed} from 'vue'
import {Link, router, usePage} from '@inertiajs/vue3'
import {route} from 'ziggy-js'
import SellerLayout from '../SellerLayout.vue'

const props = defineProps({bonsai: {type: Object, default: null}})

const page = usePage()
const flash = computed(() => page.props.flash || {})

const bannerStyle = computed(() => {
  if (props.bonsai?.is_minted) return {background: '#ecfdf5', color: '#047857', borderColor: '#a7f3d0'}
  if (props.bonsai?.status === 'rejected') return {background: '#fef2f2', color: '#b91c1c', borderColor: '#fecaca'}
  return {background: '#fffbeb', color: '#b45309', borderColor: '#fde68a'}
})
const bannerIcon = computed(() => {
  if (props.bonsai?.is_minted) return 'verified'
  if (props.bonsai?.status === 'rejected') return 'cancel'
  return 'hourglass_top'
})
const bannerTitle = computed(() => {
  if (props.bonsai?.is_minted) return `Hộ chiếu đã được cấp — Token #${props.bonsai.token_id}`
  if (props.bonsai?.status === 'rejected') return 'Yêu cầu bị từ chối'
  return 'Đang chờ admin duyệt'
})
const bannerDescription = computed(() => {
  if (props.bonsai?.is_minted) return 'Hộ chiếu NFT đã được mint lên blockchain và tra cứu công khai bằng mã QR.'
  if (props.bonsai?.status === 'rejected') return `Lý do: ${props.bonsai.reject_reason || 'Không có lý do.'} Bạn có thể sửa và gửi lại.`
  return 'Yêu cầu hộ chiếu của bạn đã được gửi lên hệ thống. Admin sẽ duyệt và cấp NFT. Thường hoàn tất trong 1–2 ngày làm việc.'
})

function logIcon(type) {
  return {
    tuoi: 'water_drop',
    'bon-phan': 'science',
    'cat-tia': 'content_cut',
    'thay-dat': 'grass',
    'phun-thuoc': 'spray',
    other: 'event_note'
  }[type] || 'event_note'
}

function formatDate(v) {
  return v ? new Date(v).toLocaleDateString('vi-VN') : ''
}

function destroy() {
  if (confirm('Xóa yêu cầu hộ chiếu này?')) {
    router.delete(route('agriverse.shop.seller.bonsai-passports.destroy', props.bonsai.id))
  }
}
</script>

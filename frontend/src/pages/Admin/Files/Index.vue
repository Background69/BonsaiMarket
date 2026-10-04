<template>
  <AdminLayout>
    <div class="ckfinder-admin-page">
      <div class="flex items-center justify-between mb-4">
        <div>
          <h1 class="text-base font-bold" style="color: var(--ag-primary-600);">Quản lý tệp</h1>
          <p class="text-xs mt-0.5" style="color: var(--ag-text-muted);">
            Bạn đang ở thư mục cá nhân. Admin có thể xem toàn bộ tệp.
          </p>
        </div>
        <div class="flex items-center gap-2">
          <span class="text-[11px] px-2.5 py-1 rounded-full font-semibold" 
                style="background: color-mix(in srgb, var(--ag-primary-500) 8%, transparent); color: var(--ag-primary-500);">
            <span class="material-symbols-outlined text-[14px] align-middle mr-1">folder</span>
            /userfiles/users/{{ userId }}
          </span>
        </div>
      </div>

      <div class="ckfinder-browser-wrapper" ref="browserRef">
        <iframe 
          :src="ckfinderUrl" 
          class="ckfinder-iframe" 
          frameborder="0"
          allowfullscreen>
        </iframe>
      </div>

      <div class="mt-3 flex items-center gap-3 text-[11px]" style="color: var(--ag-text-muted);">
        <span class="flex items-center gap-1">
          <span class="material-symbols-outlined text-[14px]">info</span>
          Dung lượng tối đa mỗi tệp: 64MB
        </span>
        <span class="flex items-center gap-1">
          <span class="material-symbols-outlined text-[14px]">image</span>
          Hỗ trợ: jpg, png, webp, pdf, doc, zip...
        </span>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed } from 'vue'
import { usePage } from '@inertiajs/vue3'
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue'

const page = usePage()
const userId = computed(() => page.props.auth?.user?.id || 0)
const ckfinderUrl = computed(() => `/ckfinder/browser?type=My%20Images`)
</script>

<style scoped>
.ckfinder-admin-page {
  height: calc(100vh - 140px);
  display: flex;
  flex-direction: column;
}
.ckfinder-browser-wrapper {
  flex: 1;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(116, 121, 108, 0.08);
  background: white;
  min-height: 0;
}
.ckfinder-iframe {
  width: 100%;
  height: 100%;
  min-height: 500px;
  display: block;
}
</style>

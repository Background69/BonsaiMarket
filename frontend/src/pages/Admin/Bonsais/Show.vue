<template>
  <AdminLayout>
    <div class="flex items-center justify-between mb-4">
      <div class="flex items-center gap-3">
        <Link :href="route('admin.agriverse.bonsais.index')" class="text-stone-500 hover:text-stone-700 text-xs">← Danh
          sách
        </Link>
        <h1 class="text-base font-bold text-stone-800">{{ bonsai.name }} <span
            class="font-mono text-xs text-stone-400 font-normal">{{ bonsai.code }}</span></h1>
      </div>
      <Link :href="route('admin.agriverse.bonsais.edit', bonsai.id)"
            class="h-8 px-3 rounded-lg border border-stone-300 text-stone-700 text-xs font-semibold leading-8 hover:bg-stone-50">
        Chỉnh sửa
      </Link>
    </div>

    <div v-if="flash.success"
         class="mb-3 px-4 py-3 bg-emerald-50 border border-emerald-200 text-emerald-700 rounded-lg text-xs font-semibold">
      {{ flash.success }}
    </div>
    <div v-if="flash.error"
         class="mb-3 px-4 py-3 bg-red-50 border border-red-200 text-red-700 rounded-lg text-xs font-semibold">
      {{ flash.error }}
    </div>
    <div v-if="Object.keys(errors).length"
         class="mb-3 px-4 py-3 bg-red-50 border border-red-200 text-red-700 rounded-lg text-xs font-semibold">
      <div v-for="(list, k) in errors" :key="k">{{ list.join(', ') }}</div>
    </div>

    <div class="grid lg:grid-cols-3 gap-5 items-start">
      <div class="lg:col-span-2 space-y-5">
        <div class="bg-white rounded-xl border border-stone-200 p-4">
          <div class="grid md:grid-cols-2 gap-x-8 gap-y-2 text-xs">
            <div class="flex justify-between border-b border-stone-100 py-2"><span class="text-stone-500 font-medium">Loài</span><span
                class="font-semibold text-stone-800">{{ bonsai.species || '—' }}</span></div>
            <div class="flex justify-between border-b border-stone-100 py-2"><span class="text-stone-500 font-medium">Biến thể</span><span
                class="font-semibold text-stone-800">{{ bonsai.variety || '—' }}</span></div>
            <div class="flex justify-between border-b border-stone-100 py-2"><span class="text-stone-500 font-medium">Tuổi</span><span
                class="font-semibold text-stone-800">{{
                bonsai.age_years != null ? bonsai.age_years + ' năm' : '—'
              }}</span></div>
            <div class="flex justify-between border-b border-stone-100 py-2"><span class="text-stone-500 font-medium">Chiều cao</span><span
                class="font-semibold text-stone-800">{{
                bonsai.height_cm != null ? bonsai.height_cm + ' cm' : '—'
              }}</span></div>
            <div class="flex justify-between border-b border-stone-100 py-2"><span class="text-stone-500 font-medium">Đường kính thân</span><span
                class="font-semibold text-stone-800">{{
                bonsai.trunk_cm != null ? bonsai.trunk_cm + ' cm' : '—'
              }}</span></div>
            <div class="flex justify-between border-b border-stone-100 py-2"><span class="text-stone-500 font-medium">Nguồn gốc</span><span
                class="font-semibold text-stone-800">{{ bonsai.origin || '—' }}</span></div>
            <div class="flex justify-between border-b border-stone-100 py-2"><span class="text-stone-500 font-medium">Chủ sở hữu</span><span
                class="font-semibold text-stone-800">{{ bonsai.owner_name || '—' }}</span></div>
            <div class="flex justify-between border-b border-stone-100 py-2"><span class="text-stone-500 font-medium">Ví chủ sở hữu</span><span
                class="font-mono text-[11px] font-semibold text-stone-800">{{ bonsai.owner_wallet || '—' }}</span></div>
            <div class="md:col-span-2 flex justify-between border-b border-stone-100 py-2"><span
                class="text-stone-500 font-medium">Mô tả</span><span
                class="font-semibold text-stone-800 text-right max-w-md">{{ bonsai.description || '—' }}</span></div>
          </div>
        </div>

        <div v-if="bonsai.is_minted" class="bg-white rounded-xl border border-stone-200 p-4">
          <h3 class="text-sm font-bold text-stone-800 mb-3">Blockchain (Polygon Amoy)</h3>
          <div class="text-xs space-y-2">
            <div class="flex justify-between border-b border-stone-100 py-2"><span class="text-stone-500 font-medium">Token ID</span><span
                class="font-mono font-bold text-emerald-700">#{{ bonsai.token_id }}</span></div>
            <div class="flex justify-between border-b border-stone-100 py-2"><span class="text-stone-500 font-medium">CID (IPFS)</span><a
                v-if="bonsai.ipfs_gateway_uri" :href="bonsai.ipfs_gateway_uri" class="font-mono text-emerald-600 truncate max-w-[200px]"
                target="_blank">{{ bonsai.ipfs_cid }}</a><span v-else
                                                                                                         class="font-mono">{{
                bonsai.ipfs_cid || '—'
              }}</span></div>
            <div class="flex justify-between border-b border-stone-100 py-2"><span class="text-stone-500 font-medium">Metadata version</span><span>v{{
                bonsai.metadata_version
              }}</span></div>
            <div v-if="bonsai.tx_explorer_url" class="flex justify-between border-b border-stone-100 py-2"><span
                class="text-stone-500 font-medium">Tx gần nhất</span><a :href="bonsai.tx_explorer_url" class="font-mono text-emerald-600"
                                                                        target="_blank">{{
                (bonsai.last_sync_tx_hash || '').slice(0, 18)
              }}…</a></div>
          </div>
        </div>

        <div class="bg-white rounded-xl border border-stone-200 p-4">
          <div class="flex items-center justify-between mb-3">
            <h3 class="text-sm font-bold text-stone-800">Nhật ký chăm sóc ({{ bonsai.logs.length }})</h3>
            <span class="text-[11px] text-stone-400">+ nhật ký = + phiên bản IPFS & auto setTokenURI</span>
          </div>

          <form class="grid md:grid-cols-2 gap-2 p-3 rounded-lg bg-stone-50 border border-stone-200 mb-4"
                @submit.prevent="addLog">
            <input v-model="logForm.title" class="h-8 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500" placeholder="Tiêu đề sự kiện *"
                   required>
            <select v-model="logForm.event_type"
                    class="h-8 px-2 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500">
              <option v-for="(label, key) in eventTypes" :key="key" :value="key">{{ label }}</option>
            </select>
            <input v-model="logForm.event_date" class="h-8 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"
                   type="date">
            <input v-model="logForm.image_url" class="h-8 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"
                   placeholder="URL ảnh (tùy chọn)">
            <textarea v-model="logForm.description" class="md:col-span-2 px-3 py-2 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500" placeholder="Chi tiết sự kiện..."
                      rows="2"></textarea>
            <button
                :disabled="logForm.processing"
                class="md:col-span-2 h-8 rounded-lg bg-emerald-600 text-white text-xs font-bold hover:bg-emerald-700">Thêm nhật ký + đồng bộ lên chain
            </button>
          </form>

          <div class="space-y-2">
            <div v-for="log in bonsai.logs" :key="log.id"
                 class="flex items-start gap-3 p-3 rounded-lg border border-stone-200">
              <div class="min-w-0 flex-1">
                <div class="flex items-center gap-2">
                  <span class="text-xs font-bold text-stone-800">{{ log.title }}</span>
                  <span class="px-1.5 py-0.5 rounded bg-emerald-50 text-emerald-700 text-[10px] font-bold uppercase">{{
                      eventTypeLabel(log.event_type)
                    }}</span>
                </div>
                <div class="text-[11px] text-stone-400 mt-0.5">{{ formatDate(log.event_date) }}</div>
                <p v-if="log.description" class="text-xs text-stone-700 mt-1">{{ log.description }}</p>
              </div>
              <button class="text-red-400 hover:text-red-600 text-[10px] font-bold uppercase"
                      @click="deleteLog(log.id)">Xóa
              </button>
            </div>
            <p v-if="!bonsai.logs.length" class="text-xs text-stone-400">Chưa có nhật ký nào.</p>
          </div>
        </div>
      </div>

      <div class="space-y-5">
        <div class="bg-white rounded-xl border border-stone-200 p-4 text-center">
          <h3 class="text-xs font-bold text-stone-800 mb-3 uppercase tracking-wider">Mã QR tra cứu công khai</h3>
          <template v-if="bonsai.is_minted">
            <img v-if="bonsai.lookup_url" :src="qrUrl" alt="QR"
                 class="mx-auto w-40 h-40 rounded-lg border border-stone-200">
            <p class="text-[10px] text-stone-400 mt-2 break-all">{{ bonsai.lookup_url }}</p>
          </template>
          <p v-else class="text-xs text-stone-400 py-6">Mã QR sẽ được tạo sau khi <b>duyệt (mint)</b> hộ chiếu.</p>
        </div>

        <div class="bg-white rounded-xl border border-stone-200 p-4 space-y-3">
          <h3 class="text-xs font-bold text-stone-800 uppercase tracking-wider">Trạng thái & Hành động</h3>
          <div class="flex items-center gap-2">
            <span :class="statusClass(bonsai.status)"
                  class="px-2 py-0.5 rounded-lg text-[10px] font-bold">{{ statusLabel(bonsai.status) }}</span>
            <span class="text-[11px] text-stone-400 font-medium">{{ bonsai.is_minted ? 'Đã mint' : 'Chưa mint' }}</span>
          </div>

          <form v-if="!bonsai.is_minted" @submit.prevent="approve">
            <button
                :disabled="approveForm.processing"
                class="w-full h-9 rounded-lg bg-emerald-600 text-white text-xs font-bold uppercase tracking-widest hover:bg-emerald-700">Duyệt & Mint hộ chiếu
            </button>
          </form>

          <form v-if="bonsai.status === 'pending'" class="space-y-2" @submit.prevent="reject">
            <input v-model="rejectForm.reject_reason" class="w-full h-8 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-red-500" placeholder="Lý do từ chối..."
                   required>
            <button
                :disabled="rejectForm.processing"
                class="w-full h-8 rounded-lg bg-red-50 border border-red-200 text-red-600 text-xs font-bold uppercase tracking-widest">Từ chối duyệt
            </button>
          </form>

          <div v-if="bonsai.is_minted" class="border-t border-stone-100 pt-3">
            <form class="space-y-2" @submit.prevent="transfer">
              <div class="text-[11px] font-bold text-stone-800 uppercase tracking-wider">Sang tay cây (adminTransfer)
              </div>
              <input v-model="transferForm.to_wallet" class="w-full h-8 px-3 rounded-lg border border-stone-300 text-xs font-mono outline-none focus:border-emerald-500" placeholder="Ví mới 0x..."
                     required>
              <input v-model="transferForm.new_owner_name" class="w-full h-8 px-3 rounded-lg border border-stone-300 text-xs outline-none focus:border-emerald-500"
                     placeholder="Tên chủ mới (tùy chọn)">
              <button
                  :disabled="transferForm.processing"
                  class="w-full h-8 rounded-lg bg-amber-500 text-white text-xs font-bold uppercase tracking-widest hover:bg-amber-600">Chuyển quyền sở hữu
              </button>
            </form>
          </div>

          <button class="w-full h-8 rounded-lg bg-red-50 border border-red-200 text-red-600 text-xs font-bold uppercase tracking-widest"
                  @click="destroy">
            Xóa bản ghi
          </button>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import {computed} from 'vue';
import {Link, router, useForm, usePage} from '@inertiajs/vue3';
import {route} from 'ziggy-js';
import AdminLayout from '@agriverse/Layouts/AdminLayout.vue';

const props = defineProps({bonsai: Object, eventTypes: Object});

const page = usePage();
const flash = computed(() => page.props.flash || {});
const errors = computed(() => page.props.errors || {});

const approveForm = useForm({});
const rejectForm = useForm({reject_reason: ''});
const transferForm = useForm({to_wallet: '', new_owner_name: ''});
const logForm = useForm({title: '', event_type: 'tuoi', event_date: '', image_url: '', description: ''});

const qrUrl = computed(() => props.bonsai.lookup_url ? `https://api.qrserver.com/v1/create-qr-code/?size=160x160&data=${encodeURIComponent(props.bonsai.lookup_url)}` : '');

function statusLabel(s) {
  return {draft: 'Draft', pending: 'Chờ duyệt', approved: 'Đã duyệt', rejected: 'Từ chối'}[s] || s;
}

function statusClass(s) {
  return {
    draft: 'bg-gray-100 text-gray-600',
    pending: 'bg-amber-50 text-amber-600',
    approved: 'bg-emerald-50 text-emerald-600',
    rejected: 'bg-red-50 text-red-600'
  }[s] || '';
}

function eventTypeLabel(key) {
  return props.eventTypes?.[key] || key;
}

function formatDate(v) {
  return v ? new Date(v).toLocaleDateString('vi-VN') : '';
}

function approve() {
  if (confirm('Mint hộ chiếu NFT SBT cho cây này ngay bây giờ?')) approveForm.post(route('admin.agriverse.bonsais.approve', props.bonsai.id));
}

function reject() {
  rejectForm.post(route('admin.agriverse.bonsais.reject', props.bonsai.id));
}

function transfer() {
  transferForm.post(route('admin.agriverse.bonsais.transfer', props.bonsai.id));
}

function addLog() {
  logForm.post(route('admin.agriverse.bonsais.logs.store', props.bonsai.id), {onSuccess: () => logForm.reset()});
}

function deleteLog(id) {
  if (confirm('Xóa nhật ký và đồng bộ metadata mới?')) router.delete(route('admin.agriverse.bonsais.logs.destroy', [props.bonsai.id, id]));
}

function destroy() {
  if (confirm('Xóa bản ghi này? (NFT trên chain vẫn còn)')) router.delete(route('admin.agriverse.bonsais.destroy', props.bonsai.id));
}
</script>

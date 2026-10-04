<template>
  <div v-if="visible" class="browser-overlay" @click.self="$emit('close')">
    <div class="browser-modal">
      <div class="browser-header">
        <h3 class="browser-title">Quản lý hình ảnh</h3>
        <button class="browser-close" @click="$emit('close')">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>

      <div class="browser-tabs">
        <button :class="['browser-tab', { active: tab === 'upload' }]" @click="tab = 'upload'">Tải lên</button>
        <button :class="['browser-tab', { active: tab === 'browse' }]" @click="tab = 'browse'; fetchImages()">Thư viện</button>
      </div>

      <div class="browser-body">
        <!-- Upload tab -->
        <div v-if="tab === 'upload'" class="browser-upload">
          <div class="browser-dropzone" @click="$refs.uploadInput.click()" @dragover.prevent @drop.prevent="handleDrop">
            <span class="material-symbols-outlined" style="font-size:40px;color:var(--ag-primary-300);">cloud_upload</span>
            <p class="browser-dropzone-text">Kéo thả ảnh vào đây hoặc nhấn để chọn</p>
            <p class="browser-dropzone-hint">JPEG, PNG, GIF, WebP — tối đa 10MB</p>
          </div>
          <input type="file" ref="uploadInput" class="hidden" accept="image/*" multiple @change="handleUpload" />
          <div v-if="uploading" class="browser-uploading">
            <span>Đang tải lên... {{ uploadedCount }}/{{ uploadTotal }}</span>
          </div>
          <div v-if="uploadedImages.length" class="browser-uploaded">
            <p class="browser-uploaded-title">Vừa tải lên:</p>
            <div class="browser-grid">
              <div v-for="(img, i) in uploadedImages" :key="i" class="browser-item" @click="insert(img)">
                <img :src="img.url" :alt="img.name" class="browser-thumb" />
                <span class="browser-item-hint">Nhấn để chèn</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Browse tab -->
        <div v-if="tab === 'browse'" class="browser-browse">
          <p v-if="loading" class="browser-loading">Đang tải...</p>
          <p v-else-if="!images.length" class="browser-empty">Chưa có ảnh nào trong thư viện.</p>
          <div v-else class="browser-grid">
            <div v-for="(img, i) in images" :key="i" class="browser-item" @click="insert(img)">
              <img :src="img.url" :alt="img.name" class="browser-thumb" />
              <span class="browser-item-name" :title="img.name">{{ img.name }}</span>
              <span class="browser-item-hint">Nhấn để chèn</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { route } from 'ziggy-js';

const emit = defineEmits(['close', 'insert', 'uploaded']);

const props = defineProps({
  visible: { type: Boolean, default: false },
});

const tab = ref('upload');
const uploadInput = ref(null);
const uploading = ref(false);
const uploadedCount = ref(0);
const uploadTotal = ref(0);
const uploadedImages = ref([]);
const images = ref([]);
const loading = ref(false);

async function handleUpload(e) {
  const files = Array.from(e.target.files);
  if (!files.length) return;
  uploadTotal.value = files.length;
  uploadedCount.value = 0;
  uploading.value = true;
  uploadedImages.value = [];
  const csrfToken = document.querySelector('meta[name="csrf-token"]')?.content;

  for (const file of files) {
    if (!file.type.startsWith('image/')) continue;
    if (file.size > 10 * 1024 * 1024) continue;
    const fd = new FormData();
    fd.append('image', file);
    try {
      const res = await fetch(route('agriverse.shop.forum.upload'), {
        method: 'POST', body: fd,
        headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest', 'X-CSRF-TOKEN': csrfToken },
      });
      if (res.ok) {
        const data = await res.json();
        uploadedImages.value.push({ url: data.url, name: file.name });
      }
    } catch {}
    uploadedCount.value++;
  }
  uploading.value = false;
  if (uploadedImages.value.length) {
    emit('uploaded', [...uploadedImages.value]);
  }
  e.target.value = null;
}

function handleDrop(e) {
  if (e.dataTransfer.files.length) {
    handleUpload({ target: { files: e.dataTransfer.files } });
  }
}

async function fetchImages() {
  loading.value = true;
  try {
    const res = await fetch(route('agriverse.shop.forum.images'), {
      headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
    });
    if (res.ok) {
      const data = await res.json();
      images.value = data.images || [];
    }
  } catch {} finally {
    loading.value = false;
  }
}

function insert(img) {
  emit('insert', img);
}
</script>

<style scoped>
.browser-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: rgba(0,0,0,0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.browser-modal {
  background: white;
  border-radius: 20px;
  width: 100%;
  max-width: 640px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 24px 48px -12px rgba(0,0,0,0.2);
}
.browser-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 0;
}
.browser-title {
  font-family: var(--ag-font-display);
  font-size: 20px;
  font-weight: 500;
  color: var(--ag-text-primary);
}
.browser-close {
  width: 32px; height: 32px;
  border-radius: 50%;
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ag-text-muted);
  transition: background 0.2s;
}
.browser-close:hover { background: var(--ag-surface-container); }
.browser-tabs {
  display: flex;
  gap: 0;
  padding: 16px 24px 0;
  border-bottom: 1px solid var(--ag-border);
}
.browser-tab {
  padding: 10px 20px;
  border: none;
  background: transparent;
  font-family: var(--ag-font-body);
  font-size: 14px;
  font-weight: 600;
  color: var(--ag-text-secondary);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  transition: all 0.2s;
}
.browser-tab.active { color: var(--ag-primary-500); border-bottom-color: var(--ag-primary-500); }
.browser-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}
.browser-dropzone {
  border: 2px dashed var(--ag-border);
  border-radius: 12px;
  padding: 40px 24px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s;
}
.browser-dropzone:hover { border-color: var(--ag-primary-500); }
.browser-dropzone-text {
  font-family: var(--ag-font-body);
  font-size: 14px;
  color: var(--ag-text-primary);
  margin-top: 12px;
}
.browser-dropzone-hint {
  font-size: 12px;
  color: var(--ag-text-muted);
  margin-top: 4px;
}
.hidden { display: none; }
.browser-uploading {
  text-align: center;
  padding: 16px;
  font-size: 14px;
  color: var(--ag-text-secondary);
}
.browser-uploaded { margin-top: 16px; }
.browser-uploaded-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--ag-text-secondary);
  margin-bottom: 8px;
}
.browser-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
}
.browser-item {
  position: relative;
  border: 1px solid var(--ag-border);
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
}
.browser-thumb {
  width: 100%;
  height: 100px;
  object-fit: cover;
  display: block;
}
.browser-item-name {
  display: block;
  padding: 4px 6px;
  font-size: 10px;
  color: var(--ag-text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.browser-item-hint {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0,0,0,0.5);
  color: white;
  font-size: 10px;
  text-align: center;
  padding: 4px 0;
  opacity: 0;
  transition: opacity 0.2s;
}
.browser-item:hover .browser-item-hint { opacity: 1; }
.browser-loading, .browser-empty {
  text-align: center;
  padding: 40px;
  color: var(--ag-text-muted);
  font-size: 14px;
}
</style>

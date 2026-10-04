<template>
  <MarketplaceLayout>
    <div class="create-page">
      <Link :href="route('agriverse.shop.forum.index')" class="create-back-link">
        <span class="material-symbols-outlined">arrow_back</span>
        Quay lại diễn đàn
      </Link>

      <div class="create-card">
        <h1 class="create-title">{{ isEdit ? 'Chỉnh sửa bài viết' : 'Tạo bài viết mới' }}</h1>
        <p class="create-desc">{{ isEdit ? 'Cập nhật nội dung bài viết của bạn' : 'Chia sẻ kinh nghiệm, câu hỏi hoặc ý tưởng với cộng đồng' }}</p>

        <form @submit.prevent="submit" class="create-form">
          <div class="create-field">
            <label class="create-label">Danh mục</label>
            <select v-model="form.category_id" class="create-select" required>
              <option value="">-- Chọn danh mục --</option>
              <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
            </select>
          </div>

          <div class="create-field">
            <label class="create-label">Tiêu đề</label>
            <input v-model="form.title" class="create-input" placeholder="Nhập tiêu đề bài viết..." maxlength="200" required />
            <span class="create-count">{{ form.title.length }}/200</span>
          </div>

            <div class="create-field">
              <div class="flex justify-between items-end mb-2">
                <label class="create-label mb-0">Nội dung</label>
                <div class="flex gap-2">
                  <button type="button" class="upload-img-btn" :disabled="uploading" @click="$refs.fileInput.click()">
                    <span class="material-symbols-outlined text-[16px]">image</span>
                    {{ uploading ? 'Đang tải...' : 'Tải ảnh' }}
                  </button>
                  <button type="button" class="upload-img-btn" @click="showBrowser = true">
                    <span class="material-symbols-outlined text-[16px]">photo_library</span>
                    Quản lý ảnh
                  </button>
                </div>
              </div>
              <textarea ref="contentInput" v-model="form.content" class="create-textarea" placeholder="Viết nội dung bài viết..." rows="8" required></textarea>
              <input type="file" ref="fileInput" class="hidden" accept="image/*" @change="handleImageUpload" />
              <!-- Uploaded images gallery -->
              <div v-if="uploadedImages.length" class="uploaded-gallery">
                <div v-for="(img, i) in uploadedImages" :key="i" class="uploaded-item">
                  <img :src="img.url" :alt="img.name" class="uploaded-thumb" @click="insertImage(img)">
                  <button type="button" class="uploaded-remove" @click.stop="removeImage(i)" title="Xoá ảnh">
                    <span class="material-symbols-outlined">close</span>
                  </button>
                  <span class="uploaded-hint">Nhấn để chèn</span>
                </div>
              </div>
            </div>

            <ForumImageBrowser :visible="showBrowser" @close="showBrowser = false" @insert="handleBrowserInsert" @uploaded="handleBrowserUploaded" />

          <div class="create-actions">
            <Link :href="isEdit ? route('agriverse.shop.forum.show', post.id) : route('agriverse.shop.forum.index')" class="create-cancel-btn">{{ isEdit ? 'Hủy' : 'Hủy' }}</Link>
            <button type="submit" class="create-submit-btn" :disabled="submitting">
              {{ submitting ? 'Đang gửi...' : (isEdit ? 'Cập nhật' : 'Đăng bài') }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </MarketplaceLayout>
</template>

<script setup>
import { ref, computed } from 'vue';
import { Link, router, usePage } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import MarketplaceLayout from '@agriverse/Layouts/MarketplaceLayout.vue';
import ForumImageBrowser from '@agriverse/Components/ForumImageBrowser.vue';

const props = defineProps({
  categories: Array,
  post: Object,
  prefill: Object,
});

const isEdit = computed(() => !!props.post);

const form = ref({
  category_id: props.post?.category_id || '',
  title: props.post?.title || props.prefill?.title || '',
  content: props.post?.content || props.prefill?.content || '',
});
const submitting = ref(false);
const fileInput = ref(null);
const contentInput = ref(null);
const uploading = ref(false);
const uploadedImages = ref([]);
const showBrowser = ref(false);

async function handleImageUpload(e) {
  const file = e.target.files[0];
  if (!file) return;

  if (!file.type.startsWith('image/')) {
    return alert('Chỉ chấp nhận file ảnh.');
  }
  if (file.size > 10 * 1024 * 1024) {
    return alert('Ảnh không được quá 10MB.');
  }

  const formData = new FormData();
  formData.append('image', file);
  const csrfToken = document.querySelector('meta[name="csrf-token"]')?.content;
  uploading.value = true;

  try {
    const res = await fetch(route('agriverse.shop.forum.upload'), {
      method: 'POST',
      body: formData,
      headers: {
        'Accept': 'application/json',
        'X-Requested-With': 'XMLHttpRequest',
        'X-CSRF-TOKEN': csrfToken,
      },
    });

    if (!res.ok) {
      let msg = 'Tải ảnh thất bại';
      try {
        const err = await res.json();
        if (err.message) msg = err.message;
        if (err.errors?.image) msg = err.errors.image.join(', ');
      } catch {}
      return alert(msg);
    }

    const data = await res.json();
    uploadedImages.value.push({ url: data.url, name: file.name });
  } catch (error) {
    console.error('Upload error:', error);
    alert('Có lỗi xảy ra khi tải ảnh.');
  } finally {
    uploading.value = false;
    e.target.value = null;
  }
}

function insertImage(img) {
  const markdownImage = `\n![${img.name}](${img.url})\n`;
  const textarea = contentInput.value;
  if (textarea) {
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    form.value.content = form.value.content.substring(0, start) + markdownImage + form.value.content.substring(end);
    setTimeout(() => {
      textarea.focus();
      textarea.setSelectionRange(start + markdownImage.length, start + markdownImage.length);
    }, 10);
  }
}

function removeImage(index) {
  uploadedImages.value.splice(index, 1);
}

function handleBrowserInsert(img) {
  const markdownImage = `\n![${img.name}](${img.url})\n`;
  const textarea = contentInput.value;
  if (textarea) {
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    form.value.content = form.value.content.substring(0, start) + markdownImage + form.value.content.substring(end);
    setTimeout(() => {
      textarea.focus();
      textarea.setSelectionRange(start + markdownImage.length, start + markdownImage.length);
    }, 10);
  }
  showBrowser.value = false;
}

function handleBrowserUploaded(images) {
  images.forEach(img => {
    if (!uploadedImages.value.find(i => i.url === img.url)) {
      uploadedImages.value.push(img);
    }
  });
}

function submit() {
  if (!form.value.category_id || !form.value.title.trim() || !form.value.content.trim()) return;

  // Auto-append images not yet inserted into content
  const imageUrls = uploadedImages.value.map(i => i.url);
  let content = form.value.content;
  imageUrls.forEach(url => {
    const markdownPattern = `](${url})`;
    if (!content.includes(markdownPattern)) {
      content += `\n![image](${url})\n`;
    }
  });
  form.value.content = content;

  submitting.value = true;
  const csrfToken = document.querySelector('meta[name="csrf-token"]')?.content;
  const data = { ...form.value, _token: csrfToken, images: imageUrls };
  if (isEdit.value) {
    router.put(route('agriverse.shop.forum.update', props.post.id), data, {
      onSuccess: () => { submitting.value = false; },
      onError: () => { submitting.value = false; },
      onFinish: () => { submitting.value = false; },
    });
  } else {
    router.post(route('agriverse.shop.forum.store'), data, {
      onSuccess: () => { submitting.value = false; },
      onError: () => { submitting.value = false; },
      onFinish: () => { submitting.value = false; },
    });
  }
}
</script>

<style scoped>
.create-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 104px 64px 80px;
}
@media (max-width: 768px) {
  .create-page { padding: 88px 20px 60px; }
}
.create-back-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-family: var(--ag-font-body);
  font-size: 14px;
  color: var(--ag-text-secondary);
  text-decoration: none;
  margin-bottom: 32px;
  transition: color 0.2s;
}
.create-back-link:hover { color: var(--ag-primary-500); }

.create-card {
  background: white;
  border: 1px solid color-mix(in srgb, var(--ag-border) 60%, transparent);
  border-radius: 20px;
  padding: 40px;
}
.create-title {
  font-family: var(--ag-font-display);
  font-size: 28px;
  font-weight: 500;
  color: var(--ag-text-primary);
  margin-bottom: 8px;
}
.create-desc {
  font-family: var(--ag-font-body);
  font-size: 15px;
  color: var(--ag-text-secondary);
  margin-bottom: 32px;
}
.create-form { display: flex; flex-direction: column; gap: 24px; }
.create-field { position: relative; }
.create-label {
  display: block;
  font-family: var(--ag-font-body);
  font-size: 13px;
  font-weight: 600;
  color: var(--ag-text-primary);
  margin-bottom: 6px;
}
.create-select, .create-input {
  width: 100%;
  height: 44px;
  padding: 0 14px;
  border: 1px solid var(--ag-border);
  border-radius: 10px;
  font-family: var(--ag-font-body);
  font-size: 14px;
  color: var(--ag-text-primary);
  outline: none;
  background: white;
  box-sizing: border-box;
  transition: border-color 0.2s;
}
.create-select:focus, .create-input:focus { border-color: var(--ag-primary-500); }
.create-textarea {
  width: 100%;
  min-height: 200px;
  padding: 14px;
  border: 1px solid var(--ag-border);
  border-radius: 10px;
  font-family: var(--ag-font-body);
  font-size: 14px;
  line-height: 1.6;
  color: var(--ag-text-primary);
  outline: none;
  resize: vertical;
  box-sizing: border-box;
  transition: border-color 0.2s;
}
.create-textarea:focus { border-color: var(--ag-primary-500); }
.create-count {
  position: absolute;
  bottom: 12px;
  right: 16px;
  font-family: var(--ag-font-mono);
  font-size: 12px;
  color: var(--ag-text-muted);
}
.upload-img-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-family: var(--ag-font-body);
  font-size: 13px;
  font-weight: 500;
  color: var(--ag-primary-500);
  background: transparent;
  border: 1px solid var(--ag-primary-500);
  border-radius: 6px;
  padding: 4px 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.upload-img-btn:hover {
  background: rgba(var(--ag-primary-rgb), 0.05);
}
.upload-img-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.uploading-spinner {
  display: inline-block;
  width: 12px;
  height: 12px;
  border: 2px solid var(--ag-primary-300);
  border-top-color: var(--ag-primary-500);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.hidden {
  display: none;
}
.create-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding-top: 8px;
}
.create-cancel-btn {
  padding: 10px 24px;
  border-radius: 8px;
  border: 1px solid var(--ag-border);
  font-family: var(--ag-font-body);
  font-size: 13px;
  font-weight: 600;
  color: var(--ag-text-secondary);
  text-decoration: none;
  transition: all 0.2s;
}
.create-cancel-btn:hover { border-color: var(--ag-primary-500); color: var(--ag-primary-500); }
.create-submit-btn {
  padding: 10px 32px;
  border-radius: 8px;
  border: none;
  background: var(--ag-primary-500);
  color: white;
  font-family: var(--ag-font-body);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.create-submit-btn:hover { background: var(--ag-primary-600); }
.create-submit-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* Uploaded images gallery */
.uploaded-gallery {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 12px;
}
.uploaded-item {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--ag-border);
  cursor: pointer;
}
.uploaded-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: opacity 0.2s;
}
.uploaded-item:hover .uploaded-thumb { opacity: 0.7; }
.uploaded-remove {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: none;
  background: rgba(0,0,0,0.5);
  color: white;
  display: none;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  padding: 0;
}
.uploaded-item:hover .uploaded-remove { display: flex; }
.uploaded-remove .material-symbols-outlined { font-size: 14px; }
.uploaded-hint {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0,0,0,0.5);
  color: white;
  font-size: 9px;
  text-align: center;
  padding: 2px 0;
  opacity: 0;
  transition: opacity 0.2s;
}
.uploaded-item:hover .uploaded-hint { opacity: 1; }
</style>

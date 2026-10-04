<template>
  <div class="ar-page">
    <!-- LOADING -->
    <div v-if="loading" class="loading-overlay">
      <div class="loading-spinner"></div>
      <div class="loading-text">{{ loadingText }}</div>
      <div v-if="loadError" class="loading-error">{{ loadError }}</div>
    </div>

    <!-- TOPBAR -->
    <div class="topbar">
      <button class="topbar-back" @click="goBack">
        <span class="material-symbols-outlined" style="font-size: 20px;">arrow_back</span>
      </button>
      <div class="topbar-title">{{ viewMode === '3d' ? 'Xem mô hình 3D' : 'Xem trong không gian' }}</div>
      <div v-if="viewMode === 'ar'" class="topbar-badge">AR</div>
      <div v-if="viewMode === '3d'" class="topbar-actions">
        <button class="topbar-btn" @click="resetCamera" title="Đặt lại">
          <span class="material-symbols-outlined" style="font-size: 18px;">restart_alt</span>
        </button>
        <button class="topbar-btn" @click="toggleFullscreen" title="Toàn màn hình">
          <span class="material-symbols-outlined" style="font-size: 18px;">fullscreen</span>
        </button>
      </div>
    </div>

    <!-- 3D VIEW MODE -->
    <template v-if="viewMode === '3d'">
      <div class="viewer-area">
        <Model3DViewer v-if="modelIsObj" :src="product?.model_3d_url || modelUrl" @load="onModelLoaded" @error="onModelError" />
        <model-viewer
          v-else
          ref="modelViewerRef"
          :src="product?.model_3d_url || modelUrl"
          alt="3D Model"
          ar
          ar-modes="scene-viewer webxr quick-look"
          camera-controls
          touch-action="pan-y"
          auto-rotate
          auto-rotate-delay="1000"
          rotation-per-second="20deg"
          camera-orbit="45deg 70deg 120%"
          min-camera-orbit="auto auto 30%"
          max-camera-orbit="Infinity Infinity 300%"
          field-of-view="30deg"
          shadow-intensity="0.6"
          shadow-softness="0.8"
          environment-image="neutral"
          exposure="1.0"
          style="background: transparent;"
          @load="onModelLoaded"
          @error="onModelError"
        />

        <div class="viewer-controls">
          <button class="ctrl-btn" :class="{ active: autoRotating }" @click="toggleAutoRotate" title="Tự xoay">
            <span class="material-symbols-outlined" style="font-size: 18px;">360</span>
          </button>
          <button class="ctrl-btn" @click="zoomIn" title="Phóng to">
            <span class="material-symbols-outlined" style="font-size: 18px;">zoom_in</span>
          </button>
          <button class="ctrl-btn" @click="zoomOut" title="Thu nhỏ">
            <span class="material-symbols-outlined" style="font-size: 18px;">zoom_out</span>
          </button>
          <button class="ctrl-btn" @click="resetCamera" title="Đặt lại">
            <span class="material-symbols-outlined" style="font-size: 18px;">restart_alt</span>
          </button>
        </div>

        <div class="viewer-badge">Kéo để xoay · Cuộn để zoom · Chuột phải để di chuyển</div>
      </div>

      <!-- SIDEBAR -->
      <div class="sidebar">
        <div class="sidebar-header">
          <div class="sidebar-tag">3D MODEL</div>
          <h1 class="sidebar-name">{{ product.name || 'Cây Bonsai' }}</h1>
          <div class="sidebar-store" v-if="product.store">
            <div class="store-avatar">{{ product.store.name?.charAt(0) || 'V' }}</div>
            {{ product.store.name }}
          </div>
        </div>

        <div class="sidebar-stats">
          <div class="stat">
            <div class="stat-val">GLB</div>
            <div class="stat-label">Định dạng</div>
          </div>
          <div class="stat">
            <div class="stat-val">{{ modelSize }}</div>
            <div class="stat-label">Kích thước</div>
          </div>
          <div class="stat">
            <div class="stat-val">360°</div>
            <div class="stat-label">Xoay tự do</div>
          </div>
        </div>

        <div class="sidebar-info">
          <div class="info-title">Thông tin</div>
          <div class="info-grid">
            <div class="info-item">
              <div class="info-key">Kích thước</div>
              <div class="info-val">{{ product.height || '—' }}</div>
            </div>
            <div class="info-item">
              <div class="info-key">Giá</div>
              <div class="info-val">{{ formatPrice(product.price) }}₫</div>
            </div>
            <div class="info-item">
              <div class="info-key">Tình trạng</div>
              <div class="info-val">{{ product.stock > 0 ? 'Còn hàng' : 'Hết hàng' }}</div>
            </div>
            <div class="info-item">
              <div class="info-key">Danh mục</div>
              <div class="info-val">{{ product.categories?.[0]?.name || '—' }}</div>
            </div>
          </div>
        </div>

        <div class="sidebar-specs" v-if="identitySpecs.length || careSpecs.length">
          <div v-if="identitySpecs.length" class="spec-block">
            <div class="info-title">Thông tin thực vật</div>
            <div v-for="spec in identitySpecs" :key="spec.label" class="spec-row">
              <span class="material-symbols-outlined spec-icon">{{ spec.icon }}</span>
              <div class="spec-body">
                <span class="spec-label">{{ spec.label }}</span>
                <span class="spec-value">{{ spec.value }}</span>
              </div>
            </div>
          </div>
          <div v-if="careSpecs.length" class="spec-block">
            <div class="info-title">Hướng dẫn chăm sóc</div>
            <div v-for="spec in careSpecs" :key="spec.label" class="spec-row">
              <span class="material-symbols-outlined spec-icon">{{ spec.icon }}</span>
              <div class="spec-body">
                <span class="spec-label">{{ spec.label }}</span>
                <span class="spec-value">{{ spec.value }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="sidebar-desc" v-if="cleanDesc">
          {{ cleanDesc }}
        </div>

        <div class="sidebar-actions">
          <button class="action-btn primary" @click="switchToAR">
            <span class="material-symbols-outlined" style="font-size: 18px;">view_in_ar</span>
            Xem trong không gian
          </button>
          <Link v-if="product.id" :href="route('agriverse.shop.products.show', product.id)" class="action-btn outline">
            <span class="material-symbols-outlined" style="font-size: 18px;">arrow_back</span>
            Quay lại
          </Link>
        </div>
      </div>
    </template>

    <!-- AR CAMERA MODE (sử dụng model-viewer AR native) -->
    <template v-if="viewMode === 'ar'">
      <div v-if="modelIsObj" class="ar-fullscreen" style="display:flex;align-items:center;justify-content:center;background:#f5f5f4;">
        <div style="text-align:center;padding:24px;color:#5f6358;">
          <span class="material-symbols-outlined" style="font-size:48px;display:block;margin-bottom:12px;">view_in_ar</span>
          <p style="font-weight:600;margin-bottom:6px;">File OBJ chưa hỗ trợ AR trên camera</p>
          <p style="font-size:13px;color:#8b9086;">Vui lòng đổi sang chế độ xem 3D hoặc tải lên mô hình GLB/GLTF để dùng AR.</p>
          <button class="topbar-back" style="margin-top:16px;position:static;" @click="switchTo3D">
            <span class="material-symbols-outlined" style="font-size:18px;">arrow_back</span> Quay lại 3D
          </button>
        </div>
      </div>
      <div v-else class="ar-fullscreen">
        <model-viewer
          ref="arModelViewerRef"
          :src="product?.model_3d_url || modelUrl"
          alt="AR Model"
          ar
          ar-modes="scene-viewer webxr quick-look"
          camera-controls
          auto-rotate
          shadow-intensity="0.6"
          environment-image="neutral"
          style="width: 100%; height: 100%; background: transparent;"
          @load="onModelLoaded"
          @error="onModelError"
        />
        <div class="ar-overlay-topbar">
          <button class="topbar-back" @click="switchTo3D">
            <span class="material-symbols-outlined" style="font-size: 20px;">arrow_back</span>
          </button>
          <div class="topbar-title">Xem trong không gian (AR)</div>
          <div class="topbar-badge">AR</div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import { Link } from '@inertiajs/vue3';
import { route } from 'ziggy-js';
import { parsePlantDescription } from '@agriverse/utils';
import '@google/model-viewer';
import Model3DViewer from '@agriverse/Components/Model3DViewer.vue';

const props = defineProps({
  product: { type: Object, default: () => ({}) },
  modelUrl: { type: String, default: '/storage/models/bonsai.glb' },
});

const modelIsObj = computed(() => {
  const url = String(props.product?.model_3d_url || props.modelUrl || '').toLowerCase();
  return url.endsWith('.obj') || /\.obj(\?|$)/.test(url);
});

const parsedDesc = computed(() => parsePlantDescription(props.product?.description));
const identitySpecs = computed(() => parsedDesc.value.identity);
const careSpecs = computed(() => parsedDesc.value.care);
const cleanDesc = computed(() => parsedDesc.value.cleanDesc);

const viewMode = ref('3d');
const loading = ref(true);
const loadingText = ref('Đang tải mô hình 3D...');
const loadError = ref('');
const autoRotating = ref(true);
const modelViewerRef = ref(null);
const arModelViewerRef = ref(null);
const modelSize = ref('27MB');

function goBack() {
  if (viewMode.value === 'ar') {
    viewMode.value = '3d';
    return;
  }
  window.history.back();
}

function formatPrice(price) {
  return new Intl.NumberFormat('vi-VN').format(price || 0);
}

function onModelLoaded() {
  loading.value = false;
  loadingText.value = '';
}

function onModelError(event) {
  loading.value = false;
  loadError.value = 'Không thể tải mô hình 3D. Vui lòng thử lại sau.';
  console.error('Model viewer error:', event);
}

function resetCamera() {
  const mv = modelViewerRef.value;
  if (mv) {
    mv.cameraOrbit = '45deg 70deg 120%';
    mv.fieldOfView = '30deg';
  }
}

function toggleAutoRotate() {
  autoRotating.value = !autoRotating.value;
  const mv = modelViewerRef.value;
  if (mv) mv.autoRotate = autoRotating.value;
}

function zoomIn() {
  const mv = modelViewerRef.value;
  if (mv) mv.fieldOfView = Math.max(10, parseFloat(mv.getFieldOfView()) - 5) + 'deg';
}

function zoomOut() {
  const mv = modelViewerRef.value;
  if (mv) mv.fieldOfView = Math.min(90, parseFloat(mv.getFieldOfView()) + 5) + 'deg';
}

function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen();
  } else {
    document.exitFullscreen();
  }
}

function switchToAR() {
  viewMode.value = 'ar';
}

function switchTo3D() {
  viewMode.value = '3d';
}
</script>

<style scoped>
.ar-page {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: var(--ag-bg);
  display: flex;
  flex-direction: column;
  position: relative;
}

/* LOADING */
.loading-overlay {
  position: fixed;
  inset: 0;
  background: var(--ag-bg);
  z-index: 200;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  transition: opacity 0.5s;
}
.loading-overlay:empty,
.loading-spinner + .loading-text:empty { opacity: 0; }
.loading-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid color-mix(in srgb, var(--ag-primary-300) 20%, transparent);
  border-top-color: var(--ag-primary-500);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.loading-text {
  margin-top: 14px;
  font: 500 13px/1 var(--ag-font-body);
  color: var(--ag-text-secondary);
}
.loading-error {
  margin-top: 16px;
  font: 500 14px/1.4 var(--ag-font-body);
  color: var(--ag-danger, #dc2626);
  padding: 0 20px;
  text-align: center;
}

/* TOPBAR */
.topbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  height: 56px;
  background: color-mix(in srgb, var(--ag-bg) 85%, transparent);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid color-mix(in srgb, var(--ag-border) 50%, transparent);
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 12px;
}
.topbar-back {
  width: 36px;
  height: 36px;
  background: color-mix(in srgb, var(--ag-primary-500) 8%, transparent);
  border: 1px solid color-mix(in srgb, var(--ag-border) 30%, transparent);
  border-radius: 50%;
  color: var(--ag-text-primary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.topbar-back:hover {
  background: color-mix(in srgb, var(--ag-primary-500) 14%, transparent);
  border-color: var(--ag-primary-500);
}
.topbar-title {
  flex: 1;
  font: 600 15px/1 var(--ag-font-body);
  color: var(--ag-text-primary);
}
.topbar-badge {
  font: 600 10px/1 var(--ag-font-body);
  color: white;
  background: var(--ag-primary-500);
  border-radius: 8px;
  padding: 5px 10px;
}
.topbar-actions {
  display: flex;
  gap: 6px;
}
.topbar-btn {
  width: 36px;
  height: 36px;
  background: color-mix(in srgb, var(--ag-primary-500) 8%, transparent);
  border: 1px solid color-mix(in srgb, var(--ag-border) 30%, transparent);
  border-radius: 8px;
  color: var(--ag-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.topbar-btn:hover {
  background: color-mix(in srgb, var(--ag-primary-500) 14%, transparent);
  color: var(--ag-primary-500);
  border-color: var(--ag-primary-500);
}

/* 3D VIEWER */
.viewer-area {
  flex: 1;
  position: relative;
  background: radial-gradient(ellipse at center, var(--ag-surface-container-low) 0%, var(--ag-bg) 70%);
  margin-top: 56px;
}
.viewer-area model-viewer {
  width: 100%;
  height: 100%;
  --poster-color: transparent;
}
.viewer-controls {
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 6px;
  background: color-mix(in srgb, var(--ag-bg) 80%, transparent);
  backdrop-filter: blur(12px);
  border: 1px solid color-mix(in srgb, var(--ag-border) 40%, transparent);
  border-radius: 12px;
  padding: 6px;
}
.ctrl-btn {
  width: 40px;
  height: 40px;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 8px;
  color: var(--ag-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.ctrl-btn:hover {
  background: color-mix(in srgb, var(--ag-primary-500) 8%, transparent);
  color: var(--ag-primary-500);
}
.ctrl-btn.active {
  background: var(--ag-primary-500);
  color: white;
  border-color: var(--ag-primary-500);
}
.viewer-badge {
  position: absolute;
  bottom: 16px;
  right: 16px;
  font: 400 11px/1 var(--ag-font-body);
  color: var(--ag-text-muted);
  background: color-mix(in srgb, var(--ag-bg) 60%, transparent);
  padding: 5px 10px;
  border-radius: 6px;
  border: 1px solid color-mix(in srgb, var(--ag-border) 30%, transparent);
}

/* SIDEBAR */
.sidebar {
  width: 100%;
  max-height: 40vh;
  overflow-y: auto;
  background: var(--ag-bg);
  border-top: 1px solid var(--ag-border);
  padding-bottom: 20px;
}
@media (min-width: 1024px) {
  .ar-page { flex-direction: row; }
  .viewer-area {
    flex: 1;
    margin-top: 0;
    height: 100vh;
  }
  .sidebar {
    width: 360px;
    max-height: 100vh;
    border-top: none;
    border-left: 1px solid var(--ag-border);
    flex-shrink: 0;
  }
}
.sidebar-header {
  padding: 24px 24px 20px;
  border-bottom: 1px solid color-mix(in srgb, var(--ag-border) 50%, transparent);
}
.sidebar-tag {
  display: inline-block;
  background: color-mix(in srgb, var(--ag-primary-500) 10%, transparent);
  color: var(--ag-primary-500);
  font: 600 10px/1 var(--ag-font-body);
  padding: 4px 10px;
  border-radius: 6px;
  margin-bottom: 10px;
  letter-spacing: 0.5px;
}
.sidebar-name {
  font-family: var(--ag-font-display);
  font-size: 22px;
  font-weight: 500;
  line-height: 1.2;
  color: var(--ag-text-primary);
  margin-bottom: 8px;
}
.sidebar-store {
  display: flex;
  align-items: center;
  gap: 8px;
  font: 400 13px/1 var(--ag-font-body);
  color: var(--ag-text-muted);
}
.store-avatar {
  width: 22px;
  height: 22px;
  background: var(--ag-primary-500);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font: 600 10px/1 var(--ag-font-body);
  color: white;
}
.sidebar-stats {
  display: flex;
  padding: 16px 24px;
  gap: 16px;
  border-bottom: 1px solid color-mix(in srgb, var(--ag-border) 50%, transparent);
}
.sidebar-stats .stat {
  text-align: center;
  flex: 1;
}
.sidebar-stats .stat-val {
  font: 700 16px/1 var(--ag-font-body);
  color: var(--ag-text-primary);
}
.sidebar-stats .stat-label {
  font: 400 11px/1 var(--ag-font-body);
  color: var(--ag-text-muted);
  margin-top: 4px;
}
.sidebar-info {
  padding: 16px 24px;
  border-bottom: 1px solid color-mix(in srgb, var(--ag-border) 50%, transparent);
}
.info-title {
  font: 600 11px/1 var(--ag-font-body);
  color: var(--ag-text-muted);
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 12px;
}
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.info-item {
  background: var(--ag-surface-container-low);
  border-radius: 10px;
  padding: 10px 12px;
}
.info-key {
  font: 400 11px/1 var(--ag-font-body);
  color: var(--ag-text-muted);
  margin-bottom: 4px;
}
.info-val {
  font: 600 13px/1 var(--ag-font-body);
  color: var(--ag-text-primary);
}
.sidebar-desc {
  padding: 16px 24px;
  font: 400 13px/1.6 var(--ag-font-body);
  color: var(--ag-text-secondary);
  border-bottom: 1px solid color-mix(in srgb, var(--ag-border) 50%, transparent);
}
.sidebar-specs {
  padding: 16px 24px;
  border-bottom: 1px solid color-mix(in srgb, var(--ag-border) 50%, transparent);
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.spec-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.spec-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  background: var(--ag-surface-container-low);
  border-radius: 10px;
  padding: 10px 12px;
}
.spec-icon {
  font-size: 18px;
  color: var(--ag-primary-500);
  flex-shrink: 0;
  margin-top: 1px;
}
.spec-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.spec-label {
  font: 600 11px/1 var(--ag-font-body);
  color: var(--ag-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.spec-value {
  font: 500 13px/1.45 var(--ag-font-body);
  color: var(--ag-text-primary);
}
.sidebar-actions {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.action-btn {
  width: 100%;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 10px;
  font: 600 14px/1 var(--ag-font-body);
  cursor: pointer;
  transition: all 0.2s;
  text-decoration: none;
}
.action-btn.primary {
  background: var(--ag-primary-500);
  color: white;
  border: none;
}
.action-btn.primary:hover {
  background: var(--ag-primary-600);
  box-shadow: 0 4px 14px -2px color-mix(in srgb, var(--ag-primary-500) 40%, transparent);
}
.action-btn.outline {
  background: transparent;
  color: var(--ag-text-secondary);
  border: 1px solid var(--ag-border);
}
.action-btn.outline:hover {
  background: color-mix(in srgb, var(--ag-primary-500) 6%, transparent);
  border-color: var(--ag-primary-500);
  color: var(--ag-primary-500);
}

/* AR MODE - Native model-viewer */
.ar-fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 200;
  background: var(--ag-bg);
}
.ar-overlay-topbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 210;
  height: 56px;
  background: color-mix(in srgb, var(--ag-bg) 85%, transparent);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid color-mix(in srgb, var(--ag-border) 50%, transparent);
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 12px;
}
</style>

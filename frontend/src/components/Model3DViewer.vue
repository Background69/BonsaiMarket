<template>
  <div class="w-full h-full relative">
    <div v-if="isObj" ref="canvasWrap" class="absolute inset-0">
      <canvas ref="canvas" class="w-full h-full" style="display:block;" />
      <div v-if="loading" class="absolute inset-0 flex items-center justify-center bg-stone-50">
        <span class="inline-flex items-center gap-2 text-xs text-stone-400">
          <span class="w-3 h-3 border-2 border-stone-300 border-t-transparent rounded-full animate-spin" />
          Đang tải mô hình 3D…
        </span>
      </div>
      <div v-if="error" class="absolute inset-0 flex items-center justify-center bg-stone-50">
        <span class="text-xs text-red-500">Không thể hiển thị mô hình 3D ({{ error }})</span>
      </div>
    </div>

    <model-viewer
      v-else-if="src"
      :src="src"
      alt="3D Model"
      camera-controls
      touch-action="pan-y"
      auto-rotate
      auto-rotate-delay="1000"
      rotation-per-second="15deg"
      camera-orbit="45deg 70deg 120%"
      min-camera-orbit="auto auto 30%"
      max-camera-orbit="Infinity Infinity 300%"
      field-of-view="30deg"
      shadow-intensity="0.4"
      shadow-softness="0.6"
      environment-image="neutral"
      exposure="1.0"
      style="width:100%;height:100%;background:transparent;"
    />
  </div>
</template>

<script setup>
import { ref, computed, onBeforeUnmount, onMounted } from 'vue';
import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { OBJLoader } from 'three/addons/loaders/OBJLoader.js';
import '@google/model-viewer';

const props = defineProps({
  src: { type: String, default: '' },
});
const emit = defineEmits(['load', 'error']);

const canvas = ref(null);
const canvasWrap = ref(null);
const loading = ref(false);
const error = ref(null);

const isObj = computed(() => {
  const url = String(props.src || '').toLowerCase();
  return url.endsWith('.obj') || /\.obj(\?|$)/.test(url);
});

let renderer, scene, camera, controls, animationId;
let disposed = false;

onMounted(() => {
  if (isObj.value) initThree();
});

onBeforeUnmount(() => dispose());

function initThree() {
  if (!canvas.value || !canvasWrap.value) return;
  const wrap = canvasWrap.value;
  const width = wrap.clientWidth || 400;
  const height = wrap.clientHeight || 400;

  scene = new THREE.Scene();
  scene.background = null;

  camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 100);
  camera.position.set(3, 2, 5);

  renderer = new THREE.WebGLRenderer({ canvas: canvas.value, antialias: true, alpha: true });
  renderer.setSize(width, height);
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
  renderer.shadowMap.enabled = true;
  renderer.shadowMap.type = THREE.PCFSoftShadowMap;
  renderer.toneMapping = THREE.ACESFilmicToneMapping;
  renderer.toneMappingExposure = 1.2;

  controls = new OrbitControls(camera, renderer.domElement);
  controls.enableDamping = true;
  controls.dampingFactor = 0.08;
  controls.autoRotate = true;
  controls.autoRotateSpeed = 2;
  controls.minDistance = 1.5;
  controls.maxDistance = 10;
  controls.target.set(0, 0.5, 0);

  const ambient = new THREE.AmbientLight(0xffffff, 0.6);
  scene.add(ambient);

  const dir = new THREE.DirectionalLight(0xffffff, 1.6);
  dir.position.set(5, 8, 5);
  dir.castShadow = true;
  scene.add(dir);

  const fill = new THREE.DirectionalLight(0xffffff, 0.4);
  fill.position.set(-3, 2, 4);
  scene.add(fill);

  const rim = new THREE.DirectionalLight(0xffffff, 0.3);
  rim.position.set(0, 3, -5);
  scene.add(rim);

  const groundGeo = new THREE.PlaneGeometry(6, 6);
  const groundMat = new THREE.ShadowMaterial({ opacity: 0.15 });
  const ground = new THREE.Mesh(groundGeo, groundMat);
  ground.rotation.x = -Math.PI / 2;
  ground.position.y = -0.01;
  ground.receiveShadow = true;
  scene.add(ground);

  animate();
  loading.value = true;
  loadObj();
  window.addEventListener('resize', resize);
}

function loadObj() {
  const url = String(props.src || '');
  if (!url) return;
  loading.value = true;
  error.value = null;

  const loader = new OBJLoader();
  loader.load(
    url,
    (obj) => {
      if (disposed || !scene) return;
      const box = new THREE.Box3().setFromObject(obj);
      const size = box.getSize(new THREE.Vector3());
      const maxDim = Math.max(size.x, size.y, size.z);
      if (maxDim > 0) {
        const scale = 2 / maxDim;
        obj.scale.set(scale, scale, scale);
      }
      obj.position.y = 0;
      obj.traverse((child) => {
        if (child.isMesh) {
          child.castShadow = true;
          child.receiveShadow = true;
          if (!child.material) {
            child.material = new THREE.MeshStandardMaterial({ color: 0x8a7f6f, roughness: 0.7 });
          }
        }
      });
      scene.add(obj);
      loading.value = false;
      emit('load');
    },
    undefined,
    (err) => {
      if (disposed) return;
      console.error('OBJ load error:', err);
      error.value = 'file không đúng định dạng hoặc bị hỏng';
      loading.value = false;
      emit('error', err);
    }
  );
}

function animate() {
  if (disposed) return;
  animationId = requestAnimationFrame(animate);
  if (controls) controls.update();
  if (renderer && scene && camera) renderer.render(scene, camera);
}

function resize() {
  if (!renderer || !camera || !canvasWrap.value) return;
  const w = canvasWrap.value.clientWidth || 400;
  const h = canvasWrap.value.clientHeight || 400;
  camera.aspect = w / h;
  camera.updateProjectionMatrix();
  renderer.setSize(w, h);
}

function dispose() {
  disposed = true;
  if (animationId) cancelAnimationFrame(animationId);
  window.removeEventListener('resize', resize);
  if (renderer) { renderer.dispose(); renderer = null; }
  if (controls) { controls.dispose(); controls = null; }
  scene = null;
  camera = null;
}
</script>
import { ref, onMounted, onUnmounted } from 'vue';
import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';

export function useThreeScene(canvasRef, options = {}) {
  const {
    backgroundColor = 0xf5f5f4,
    autoRotate = true,
    autoRotateSpeed = 2,
  } = options;

  const loading = ref(true);
  const error = ref(null);
  let scene, camera, renderer, controls;
  let animationId = null;

  function init() {
    if (!canvasRef.value) return;

    const canvas = canvasRef.value;
    const width = canvas.clientWidth;
    const height = canvas.clientHeight;

    scene = new THREE.Scene();
    scene.background = new THREE.Color(backgroundColor);

    camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 100);
    camera.position.set(3, 2, 5);

    renderer = new THREE.WebGLRenderer({
      canvas,
      antialias: true,
      alpha: true,
    });
    renderer.setSize(width, height);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.shadowMap.enabled = true;
    renderer.shadowMap.type = THREE.PCFSoftShadowMap;
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.2;

    controls = new OrbitControls(camera, renderer.domElement);
    controls.enableDamping = true;
    controls.dampingFactor = 0.08;
    controls.autoRotate = autoRotate;
    controls.autoRotateSpeed = autoRotateSpeed;
    controls.minDistance = 1.5;
    controls.maxDistance = 10;
    controls.target.set(0, 0.5, 0);

    // Lighting
    const ambientLight = new THREE.AmbientLight(0xffffff, 0.5);
    scene.add(ambientLight);

    const dirLight = new THREE.DirectionalLight(0xffffff, 1.5);
    dirLight.position.set(5, 8, 5);
    dirLight.castShadow = true;
    scene.add(dirLight);

    const fillLight = new THREE.DirectionalLight(0xffffff, 0.4);
    fillLight.position.set(-3, 2, 4);
    scene.add(fillLight);

    const rimLight = new THREE.DirectionalLight(0xffffff, 0.3);
    rimLight.position.set(0, 3, -5);
    scene.add(rimLight);

    // Ground plane
    const groundGeo = new THREE.PlaneGeometry(6, 6);
    const groundMat = new THREE.ShadowMaterial({ opacity: 0.15 });
    const ground = new THREE.Mesh(groundGeo, groundMat);
    ground.rotation.x = -Math.PI / 2;
    ground.position.y = -0.01;
    ground.receiveShadow = true;
    scene.add(ground);

    // Grid helper (subtle)
    const gridHelper = new THREE.GridHelper(4, 8, 0x000000, 0x000000);
    gridHelper.position.y = 0;
    gridHelper.material.opacity = 0.06;
    gridHelper.material.transparent = true;
    scene.add(gridHelper);

    animate();
    loading.value = false;
  }

  function animate() {
    animationId = requestAnimationFrame(animate);
    controls.update();
    renderer.render(scene, camera);
  }

  function resize() {
    if (!canvasRef.value || !camera || !renderer) return;
    const width = canvasRef.value.clientWidth;
    const height = canvasRef.value.clientHeight;
    camera.aspect = width / height;
    camera.updateProjectionMatrix();
    renderer.setSize(width, height);
  }

  function addModel(model) {
    if (!scene || !model) return;
    const box = new THREE.Box3().setFromObject(model);
    const size = box.getSize(new THREE.Vector3());
    const maxDim = Math.max(size.x, size.y, size.z);
    if (maxDim > 0) {
      const scale = 2 / maxDim;
      model.scale.set(scale, scale, scale);
    }
    model.position.y = 0;
    model.traverse((child) => {
      if (child.isMesh) {
        child.castShadow = true;
        child.receiveShadow = true;
      }
    });
    scene.add(model);
    loading.value = false;
  }

  function clearModel() {
    if (!scene) return;
    while (scene.children.length > 0) {
      const child = scene.children[0];
      if (child.isMesh || child.isGroup) {
        scene.remove(child);
        child.traverse((c) => {
          if (c.geometry) c.geometry.dispose();
          if (c.material) {
            if (Array.isArray(c.material)) {
              c.material.forEach(m => m.dispose());
            } else {
              c.material.dispose();
            }
          }
        });
      } else {
        scene.remove(child);
      }
    }
  }

  function resetCamera() {
    if (!controls || !camera) return;
    camera.position.set(3, 2, 5);
    controls.target.set(0, 0.5, 0);
    controls.update();
  }

  function toggleAutoRotate() {
    if (controls) controls.autoRotate = !controls.autoRotate;
  }

  function dispose() {
    if (animationId) cancelAnimationFrame(animationId);
    if (renderer) {
      renderer.dispose();
      renderer = null;
    }
    if (controls) {
      controls.dispose();
      controls = null;
    }
    scene = null;
    camera = null;
  }

  return {
    loading,
    error,
    init,
    resize,
    addModel,
    clearModel,
    resetCamera,
    toggleAutoRotate,
    dispose,
  };
}

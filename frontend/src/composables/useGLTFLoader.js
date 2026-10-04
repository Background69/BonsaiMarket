import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { DRACOLoader } from 'three/addons/loaders/DRACOLoader.js';

let loader = null;

function getLoader() {
  if (!loader) {
    loader = new GLTFLoader();
    const dracoLoader = new DRACOLoader();
    dracoLoader.setDecoderPath('/draco/');
    loader.setDRACOLoader(dracoLoader);
  }
  return loader;
}

export function useGLTFLoader() {
  function loadModel(url, onProgress) {
    return new Promise((resolve, reject) => {
      getLoader().load(
        url,
        (gltf) => resolve(gltf.scene),
        (xhr) => {
          if (onProgress && xhr.total > 0) {
            onProgress(Math.round((xhr.loaded / xhr.total) * 100));
          }
        },
        (err) => reject(err)
      );
    });
  }

  return { loadModel };
}

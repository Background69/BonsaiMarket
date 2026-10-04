import { ref, computed, readonly } from 'vue';

const STORAGE_KEY = 'ag_compare_ids';
const MAX_ITEMS = 4;

const compareIds = ref(loadIds());

function loadIds() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function saveIds() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(compareIds.value));
}

export function useCompare() {
  const count = computed(() => compareIds.value.length);
  const canCompare = computed(() => compareIds.value.length >= 2);

  function isComparing(productId) {
    return compareIds.value.includes(productId);
  }

  function toggle(productId) {
    const idx = compareIds.value.indexOf(productId);
    if (idx > -1) {
      compareIds.value.splice(idx, 1);
    } else {
      if (compareIds.value.length >= MAX_ITEMS) {
        compareIds.value.shift();
      }
      compareIds.value.push(productId);
    }
    saveIds();
  }

  function remove(productId) {
    const idx = compareIds.value.indexOf(productId);
    if (idx > -1) {
      compareIds.value.splice(idx, 1);
      saveIds();
    }
  }

  function clear() {
    compareIds.value = [];
    saveIds();
  }

  return {
    compareIds: readonly(compareIds),
    count,
    canCompare,
    isComparing,
    toggle,
    remove,
    clear,
  };
}

<template>
  <div class="chat-messages" ref="scrollRef" @scroll="onScroll">
    <div v-if="loadingOlder" class="chat-msg-loading">Đang tải tin nhắn cũ...</div>
    <div v-if="!hasMore && hasMessages" class="chat-msg-end">Đã xem tất cả tin nhắn</div>
    <div v-if="loading && !hasMessages" class="chat-loading chat-loading--msg">Đang tải tin nhắn...</div>
    <slot />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  loading: Boolean,
  hasMore: Boolean,
  loadingOlder: Boolean,
})

const emit = defineEmits(['load-older'])

const scrollRef = ref(null)

const hasMessages = computed(() => {
  const slot = scrollRef.value?.children
  if (!slot) return false
  for (const child of slot) {
    if (child.classList.contains('chat-msg-row')) return true
  }
  return false
})

function onScroll() {
  const el = scrollRef.value
  if (!el || props.loadingOlder || !props.hasMore) return
  if (el.scrollTop < 80) {
    emit('load-older')
  }
}

defineExpose({ scrollRef })
</script>

<style scoped>
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  background: var(--ag-bg-sand);
}

.chat-loading--msg {
  padding: 16px;
  text-align: center;
  color: var(--ag-text-muted);
  font-size: 13px;
}

.chat-msg-loading {
  text-align: center;
  color: var(--ag-text-muted);
  font-size: 12px;
  padding: 12px;
}

.chat-msg-end {
  text-align: center;
  color: var(--ag-text-muted);
  font-size: 11px;
  padding: 8px;
}
</style>

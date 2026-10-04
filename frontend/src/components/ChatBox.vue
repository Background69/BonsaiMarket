<template>
  <div class="chatbox" :class="{ 'chatbox--open': isOpen }">
    <div class="chatbox__header" @click="toggle">
      <div class="chatbox__header-left">
        <div class="chatbox__avatar">
          <span class="chatbox__avatar-text">?</span>
        </div>
        <span class="chatbox__header-title">Hỗ trợ</span>
      </div>
      <span class="material-symbols-outlined chatbox__header-icon">
        {{ isOpen ? 'close' : 'chat' }}
      </span>
    </div>
    <div v-if="isOpen" class="chatbox__body">
      <div class="chatbox__messages" ref="messagesRef" @scroll="onScroll">
        <div v-if="localLoadingOlder" class="chatbox__loading">Đang tải tin nhắn cũ...</div>
        <div v-if="!localHasMore && localMessages.length > 0" class="chatbox__loading" style="color:#bbb;font-size:11px">Đã xem tất cả tin nhắn</div>
        <div v-if="localLoading && localMessages.length === 0" class="chatbox__loading">Đang tải...</div>
        <template v-else>
          <div
            v-for="(msg, i) in computedMessages"
            :key="msg.id ?? i"
            class="chatbox__msg-row"
            :class="msg.is_mine ? 'chatbox__msg-row--mine' : 'chatbox__msg-row--theirs'"
          >
            <div class="chatbox__bubble" :class="{ 'chatbox__bubble--mine': msg.is_mine }">
              <span v-if="!msg.is_mine" class="chatbox__sender">{{ msg.sender_name }}</span>
              <span class="chatbox__text">{{ msg.message }}</span>
              <div class="chatbox__meta">
                <span class="chatbox__time">{{ formatTime(msg.created_at) }}</span>
                <span v-if="msg.is_mine && !msg.is_sending" class="material-symbols-outlined chatbox__seen">done_all</span>
                <span v-if="msg.is_sending" class="material-symbols-outlined chatbox__sending">schedule</span>
              </div>
            </div>
          </div>
        </template>
      </div>
      <div class="chatbox__input-wrap">
        <input
          ref="inputRef"
          v-model="newMessage"
          type="text"
          class="chatbox__input"
          placeholder="Nhập tin nhắn..."
          @keyup.enter="send"
          :disabled="sending"
        />
        <button class="chatbox__send" @click="send" :disabled="sending || !newMessage.trim()">
          <span class="material-symbols-outlined">send</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, watch, onMounted, onBeforeUnmount } from 'vue'
import axios from 'axios'
import { useChatSocket } from '@agriverse/Composables/useChatSocket'

const token = document.querySelector('meta[name="csrf-token"]')?.getAttribute('content')
if (token) {
  axios.defaults.headers.common['X-CSRF-TOKEN'] = token
}

const { connect, on, off } = useChatSocket()

const props = defineProps({
  title: { type: String, default: 'Chat hỗ trợ' },
  orderId: { type: [Number, String], default: null },
  messages: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  sending: { type: Boolean, default: false },
})

const emit = defineEmits(['send', 'toggle'])

const isOpen = ref(false)
const newMessage = ref('')
const messagesRef = ref(null)
const inputRef = ref(null)
const localMessages = ref([])
const localLoading = ref(false)
const localSending = ref(false)
const localPage = ref(1)
const localHasMore = ref(true)
const localLoadingOlder = ref(false)

const isSelfContained = computed(() => !!props.orderId)

const computedMessages = computed(() =>
  isSelfContained.value ? localMessages.value : props.messages
)

const loading = computed(() =>
  isSelfContained.value ? localLoading.value : props.loading
)

const sending = computed(() =>
  isSelfContained.value ? localSending.value : props.sending
)

async function fetchMessages() {
  if (!isSelfContained.value) return
  localPage.value = 1
  localHasMore.value = true
  localLoading.value = true
  try {
    const { data } = await axios.get(`/agriverse/api/orders/${props.orderId}/chat`, {
      params: { page: 1 }
    })
    const userId = document.querySelector('meta[name="user-id"]')?.getAttribute('content')
    localMessages.value = (data.messages || []).map(msg => ({
      ...msg,
      is_mine: String(msg.sender_id) === userId,
      sender_name: msg.sender?.name ?? 'Người dùng',
    }))
    localHasMore.value = data.has_more ?? false
    await nextTick()
    scrollToBottom()
  } catch {
    localMessages.value = []
    localHasMore.value = false
  } finally {
    localLoading.value = false
  }
}

async function loadOlderMessages() {
  if (!localHasMore.value || localLoadingOlder.value || localLoading.value) return
  localLoadingOlder.value = true
  localPage.value++
  const prevScrollHeight = messagesRef.value?.scrollHeight || 0
  try {
    const { data } = await axios.get(`/agriverse/api/orders/${props.orderId}/chat`, {
      params: { page: localPage.value }
    })
    const userId = document.querySelector('meta[name="user-id"]')?.getAttribute('content')
    const msgs = (data.messages || []).map(msg => ({
      ...msg,
      is_mine: String(msg.sender_id) === userId,
      sender_name: msg.sender?.name ?? 'Người dùng',
    }))
    localHasMore.value = data.has_more ?? false
    if (msgs.length > 0) {
      localMessages.value = [...msgs, ...localMessages.value]
      await nextTick()
      if (messagesRef.value) {
        messagesRef.value.scrollTop = messagesRef.value.scrollHeight - prevScrollHeight
      }
    }
  } catch {
    localPage.value--
  } finally {
    localLoadingOlder.value = false
  }
}

function onScroll() {
  const el = messagesRef.value
  if (!el || localLoadingOlder.value || !localHasMore.value) return
  if (el.scrollTop < 80) {
    loadOlderMessages()
  }
}

async function toggle() {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    if (isSelfContained.value && localMessages.value.length === 0) {
      await fetchMessages()
    }
    await nextTick()
    scrollToBottom()
    inputRef.value?.focus()
  }
  emit('toggle', isOpen.value)
}

async function send() {
  const text = newMessage.value.trim()
  if (!text || localSending.value) return

  if (isSelfContained.value) {
    const tempId = 'temp-' + Date.now()
    const optimisticMsg = {
      id: tempId,
      message: text,
      is_mine: true,
      sender_name: 'Tôi',
      created_at: new Date().toISOString(),
      is_sending: true
    }
    localMessages.value.push(optimisticMsg)
    newMessage.value = ''
    await nextTick()
    scrollToBottom()
    inputRef.value?.focus()

    localSending.value = true
    try {
      const { data } = await axios.post(`/agriverse/api/orders/${props.orderId}/chat`, { message: text })
      const idx = localMessages.value.findIndex(m => m.id === tempId)
      if (idx !== -1) {
        localMessages.value[idx] = {
          ...data.message,
          is_mine: true,
          sender_name: data.message.sender?.name ?? 'Tôi',
        }
      }
      } catch {
        const idx = localMessages.value.findIndex(m => m.id === tempId)
        if (idx !== -1) {
          localMessages.value.splice(idx, 1)
        }
      } finally {
        localSending.value = false
        inputRef.value?.focus()
      }
    return
  }

  emit('send', text)
  newMessage.value = ''
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  if (isToday) {
    return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
  }
  return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

watch(() => computedMessages.value.length, async () => {
  await nextTick()
  scrollToBottom()
})

const handleIncomingMessage = async (data) => {
  if (isOpen.value && isSelfContained.value) {
    if (data && data.type === 'private') {
      localMessages.value.push({
        id: 'ws-' + Date.now(),
        message: data.content,
        is_mine: false,
        sender_name: 'Người bán',
        created_at: new Date(data.timestamp || Date.now()).toISOString()
      })
      await nextTick()
      scrollToBottom()
    }
    fetchMessages()
  }
}

onMounted(() => {
  if (isSelfContained.value) {
    connect()
    on('message', handleIncomingMessage)
  }
})

onBeforeUnmount(() => {
  off('message', handleIncomingMessage)
})
</script>

<style scoped>
.chatbox {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 999;
  width: 360px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.15);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  transition: transform 0.2s, opacity 0.2s;
}

.chatbox__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: #0084ff;
  color: #fff;
  cursor: pointer;
  user-select: none;
}

.chatbox__header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chatbox__avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
}

.chatbox__header-title {
  font-weight: 600;
  font-size: 15px;
}

.chatbox__header-icon {
  font-size: 22px;
}

.chatbox__body {
  background: #fff;
  display: flex;
  flex-direction: column;
}

.chatbox__messages {
  max-height: 360px;
  overflow-y: auto;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  background: #f0f2f5;
}

.chatbox__loading {
  text-align: center;
  color: #999;
  font-size: 13px;
  padding: 16px;
}

.chatbox__msg-row {
  display: flex;
  margin-bottom: 4px;
}

.chatbox__msg-row--mine {
  justify-content: flex-end;
}

.chatbox__msg-row--theirs {
  justify-content: flex-start;
}

.chatbox__bubble {
  max-width: 85%;
  padding: 8px 14px;
  border-radius: 18px;
  background: #fff;
  color: #1a1a1a;
  font-size: 14px;
  line-height: 1.4;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
  word-wrap: break-word;
}

.chatbox__bubble--mine {
  background: #0084ff;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.chatbox__msg-row--theirs .chatbox__bubble {
  border-bottom-left-radius: 4px;
}

.chatbox__sender {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: #666;
  margin-bottom: 2px;
}

.chatbox__text {
  display: block;
  white-space: pre-wrap;
}

.chatbox__meta {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 3px;
  margin-top: 2px;
}

.chatbox__time {
  font-size: 11px;
}

.chatbox__bubble--mine .chatbox__time {
  color: rgba(255, 255, 255, 0.7);
}

.chatbox__msg-row--theirs .chatbox__time {
  color: #999;
}

.chatbox__seen,
.chatbox__sending {
  font-size: 14px;
  opacity: 0.7;
}

.chatbox__bubble--mine .chatbox__seen {
  color: rgba(255, 255, 255, 0.7);
}

.chatbox__sending {
  animation: pulse 1s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 0.4; }
  50% { opacity: 1; }
}

.chatbox__input-wrap {
  display: flex;
  padding: 10px 12px;
  border-top: 1px solid #e5e5e5;
  background: #fff;
  gap: 8px;
}

.chatbox__input {
  flex: 1;
  border: 1px solid #e5e5e5;
  border-radius: 20px;
  padding: 8px 14px;
  font-size: 14px;
  outline: none;
  color: #1a1a1a;
  background: #f0f2f5;
  transition: border-color 0.15s;
}

.chatbox__input:focus {
  border-color: #0084ff;
  background: #fff;
}

.chatbox__send {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: #0084ff;
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background 0.15s;
}

.chatbox__send:hover {
  background: #006edb;
}

.chatbox__send:disabled {
  background: #ccc;
  cursor: not-allowed;
}
</style>

<template>
  <div class="ai-expert">
    <!-- Floating trigger -->
    <button ref="triggerRef" :aria-expanded="opened" aria-controls="bonsai-ai-panel"
            aria-label="Mở trợ lý AI BonsaiMarket"
            class="ai-expert-fab" title="Mở trợ lý AI BonsaiMarket" type="button" @click="opened ? close() : open()">
      <MarketIcon height="24" name="robot" width="24"/>
    </button>

    <!-- Chat panel -->
    <Transition name="ai-fade">
      <section v-if="opened" id="bonsai-ai-panel" aria-labelledby="bonsai-ai-title" class="ai-expert-panel"
               role="dialog"
               @keydown.esc.stop.prevent="close">
        <header class="ai-expert-header">
          <div class="ai-expert-header-avatar">
            <MarketIcon name="leaf"/>
          </div>
          <div class="ai-expert-header-info">
            <p id="bonsai-ai-title" class="ai-expert-header-title">Trợ lý AI BonsaiMarket</p>
            <p class="ai-expert-header-status">{{ availability || 'Tham khảo chăm cây · tìm catalog' }}</p>
          </div>
          <button aria-label="Đóng trợ lý AI" class="ai-expert-close" type="button" @click="close">
            <MarketIcon name="close"/>
          </button>
        </header>

        <div ref="scrollRef" :aria-busy="pending" aria-live="polite" aria-relevant="additions" class="ai-expert-body"
             role="log">
          <div v-if="!messages.length" class="ai-expert-welcome">
            <div class="ai-expert-welcome-icon">
              <MarketIcon height="36" name="robot" width="36"/>
            </div>
            <h3 class="ai-expert-welcome-title">Bạn cần tư vấn gì về cây cảnh?</h3>
            <p class="ai-expert-welcome-desc">
              Xin chào! Tôi là trợ lý AI của BonsaiMarket. Tôi có thể giúp bạn tìm cây cảnh hoặc tham khảo cách chăm sóc
              bonsai.
            </p>
            <div class="ai-expert-suggestions">
              <button v-for="s in suggestions" :key="s" :disabled="pending" class="ai-expert-chip" type="button"
                      @click="send(s)">
                {{ s }}
              </button>
            </div>
          </div>

          <div v-for="m in messages" :key="m.id" :class="m.role === 'user' ? 'ai-expert-msg-user' : 'ai-expert-msg-ai'"
               class="ai-expert-msg">
            <div class="ai-expert-msg-bubble">
              <p>{{ m.content }}</p>
              <ul v-if="m.sources?.length" aria-label="Nguồn tham khảo" class="ai-expert-sources">
                <li v-for="s in m.sources" :key="`${s.type}-${s.id}-${s.name}`">
                  <RouterLink v-if="sourcePath(s)" :to="sourcePath(s)">{{ s.name }}</RouterLink>
                  <span v-else>{{ s.name }}</span>
                </li>
              </ul>
            </div>
          </div>

          <div v-if="pending" class="ai-expert-msg ai-expert-msg-ai" role="status">
            <p class="ai-expert-msg-bubble">
              <span aria-hidden="true" class="ai-expert-typing">
                <span></span><span></span><span></span>
              </span>
              AI đang trả lời...
            </p>
          </div>

        </div>

        <footer class="ai-expert-footer">
          <p v-if="error" id="bonsai-ai-error" class="ai-expert-error" role="alert">{{ error }}</p>
          <label class="ai-expert-label" for="bonsai-ai-input">Câu hỏi cho trợ lý AI</label>
          <form class="ai-expert-form" @submit.prevent="submit">
            <textarea id="bonsai-ai-input" ref="inputRef" v-model="input" :aria-describedby="error ? 'bonsai-ai-error bonsai-ai-hint' : 'bonsai-ai-hint'" :disabled="pending"
                      class="ai-expert-input" maxlength="2000"
                      placeholder="Hỏi về cây hoặc sản phẩm #1..."
                      rows="2" @keydown.enter="onEnter"/>
            <button :disabled="pending || !input.trim()" aria-label="Gửi câu hỏi" class="ai-expert-send" type="submit">
              <MarketIcon height="20" name="send" width="20"/>
            </button>
          </form>
          <p id="bonsai-ai-hint" class="ai-expert-hint">{{ input.length }}/2.000 · Enter gửi, Shift+Enter xuống dòng.
            FAQ tham khảo cần kiểm duyệt. Không gửi thông tin cá nhân.</p>
        </footer>
      </section>
    </Transition>
  </div>
</template>

<script setup>
import {ref, nextTick, onMounted, onUnmounted} from 'vue';
import {RouterLink} from 'vue-router';
import MarketIcon from './marketplace/MarketIcon.vue';

const opened = ref(false);
const input = ref('');
const messages = ref([]);
const pending = ref(false);
const error = ref('');
const availability = ref('');
const scrollRef = ref(null);
const inputRef = ref(null);
const triggerRef = ref(null);
let sequence = 0;
let requestController;
let disposed = false;

const suggestions = [
  'Cây bonsai dưới 500.000đ',
  'Cây phù hợp để bàn',
  'Cách tưới bonsai',
  'Tìm gian hàng',
];

function open() {
  opened.value = true;
  nextTick(() => {
    scrollToBottom();
    inputRef.value?.focus();
  });
}

function close() {
  opened.value = false;
  nextTick(() => triggerRef.value?.focus());
}

onMounted(() => {
  window.__agriverse_open_chat = open;
  window.addEventListener('agriverse-open-ai-expert', open);
});

onUnmounted(() => {
  disposed = true;
  requestController?.abort();
  if (window.__agriverse_open_chat === open) window.__agriverse_open_chat = null;
  window.removeEventListener('agriverse-open-ai-expert', open);
});

function submit() {
  const text = input.value.trim();
  if (!text || pending.value) return;
  send(text);
}

async function send(text) {
  text = text.trim();
  if (pending.value || !text || text.length > 2000) return;
  messages.value.push({id: ++sequence, role: 'user', content: text});
  if (messages.value.length > 40) messages.value.splice(0, messages.value.length - 40);
  error.value = '';
  input.value = '';
  pending.value = true;
  nextTick(scrollToBottom);
  requestController = new AbortController();
  const timer = setTimeout(() => requestController?.abort(), 35000);
  try {
    const response = await fetch('/api/ai/chat', {
      method: 'POST', headers: {'Content-Type': 'application/json', Accept: 'application/json'},
      credentials: 'omit', signal: requestController.signal, body: JSON.stringify({message: text}),
    });
    const data = await response.json().catch(cause => {
      if (cause.name === 'AbortError') throw cause;
      return null;
    });
    if (!response.ok) {
      const code = data?.code;
      if (response.status === 429) throw new Error('Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau ít phút.');
      if (response.status === 504 || code === 'AI_TIMEOUT') throw new Error('Trợ lý phản hồi quá chậm. Vui lòng thử lại sau.');
      if (response.status === 503) {
        availability.value = 'Trợ lý AI hiện chưa sẵn sàng.';
        throw new Error(availability.value);
      }
      throw new Error('Trợ lý chưa trả lời được. Vui lòng thử lại.');
    }
    if (!data || typeof data.answer !== 'string' || !data.answer.trim() || !Array.isArray(data.sources)) {
      throw new Error('Trợ lý chưa trả lời được. Vui lòng thử lại.');
    }
    if (disposed) return;
    availability.value = '';
    const sources = data.sources.filter(s => s && ['PRODUCT', 'STORE', 'FAQ'].includes(s.type)
        && typeof s.name === 'string' && s.name.length <= 300).slice(0, 34);
    messages.value.push({id: ++sequence, role: 'ai', content: data.answer, sources});
    // Keep this component session bounded; no persistent history or replay to the provider.
    if (messages.value.length > 40) messages.value.splice(0, messages.value.length - 40);
  } catch (cause) {
    if (!disposed) {
      error.value = cause.name === 'AbortError' ? 'Trợ lý phản hồi quá chậm. Vui lòng thử lại sau.'
          : cause instanceof TypeError ? 'Không thể kết nối với trợ lý BonsaiMarket. Vui lòng thử lại.' : cause.message;
    }
  } finally {
    clearTimeout(timer);
    pending.value = false;
    if (!disposed) nextTick(() => {
      scrollToBottom();
      if (opened.value) inputRef.value?.focus();
    });
  }
}

function onEnter(event) {
  if (event.isComposing || event.shiftKey) return;
  event.preventDefault();
  submit();
}

function sourcePath(source) {
  if (!Number.isSafeInteger(source.id) || source.id <= 0) return null;
  if (source.type === 'PRODUCT') return `/products/${source.id}`;
  if (source.type === 'STORE') return `/stores/${source.id}`;
  return null;
}

function scrollToBottom() {
  if (scrollRef.value) scrollRef.value.scrollTop = scrollRef.value.scrollHeight;
}

</script>

<style scoped>
.ai-expert-fab {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 90;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: none;
  background: var(--ag-primary-500);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px -4px color-mix(in srgb, var(--ag-primary-500) 45%, transparent);
  cursor: pointer;
  transition: transform 0.2s ease;
}

.ai-expert-fab:hover {
  transform: scale(1.06);
}

.ai-expert-fab:active {
  transform: scale(0.95);
}

.ai-expert-fab svg {
  width: 24px;
  height: 24px;
}

.ai-expert-fab-pulse {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #34c759;
  border: 2px solid white;
}

@keyframes ai-pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.2);
    opacity: 0.7;
  }
}

.ai-expert-panel {
  position: fixed;
  bottom: 92px;
  right: 24px;
  z-index: 91;
  width: 380px;
  max-width: calc(100vw - 32px);
  height: 540px;
  max-height: calc(100vh - 120px);
  background: white;
  border-radius: 20px;
  box-shadow: 0 24px 60px -12px rgba(44, 44, 44, 0.25);
  border: 1px solid var(--ag-border);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

@media (max-width: 640px) {
  .ai-expert-panel {
    bottom: 80px;
    right: 16px;
    height: calc(100vh - 100px);
  }
}

.ai-expert-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: linear-gradient(135deg, var(--ag-primary-600), var(--ag-primary-700));
  color: white;
}

.ai-expert-header-avatar {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-expert-header-info {
  flex: 1;
  min-width: 0;
}

.ai-expert-header-title {
  font-family: var(--ag-font-display);
  font-size: 15px;
  font-weight: 600;
  line-height: 1.2;
}

.ai-expert-header-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  opacity: 0.85;
  margin-top: 2px;
}

.ai-expert-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #34c759;
}

.ai-expert-close {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.12);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.2s;
}

.ai-expert-close:hover {
  background: rgba(255, 255, 255, 0.25);
}

.ai-expert-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: var(--ag-surface-container-lowest);
}

.ai-expert-welcome {
  text-align: center;
  padding: 24px 8px;
}

.ai-expert-welcome-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: color-mix(in srgb, var(--ag-primary-500) 10%, transparent);
  color: var(--ag-primary-500);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}

.ai-expert-welcome-title {
  font-family: var(--ag-font-display);
  font-size: 16px;
  font-weight: 600;
  color: var(--ag-text-primary);
  margin-bottom: 8px;
}

.ai-expert-welcome-desc {
  font-size: 13px;
  line-height: 20px;
  color: var(--ag-text-secondary);
}

.ai-expert-suggestions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 16px;
}

.ai-expert-chip {
  text-align: left;
  padding: 10px 14px;
  border-radius: 12px;
  border: 1px solid var(--ag-border);
  background: white;
  font-size: 13px;
  color: var(--ag-text-primary);
  cursor: pointer;
  transition: all 0.2s;
}

.ai-expert-chip:hover {
  border-color: var(--ag-primary-500);
  color: var(--ag-primary-500);
  background: color-mix(in srgb, var(--ag-primary-500) 6%, transparent);
}

.ai-expert-msg {
  display: flex;
}

.ai-expert-msg-user {
  justify-content: flex-end;
}

.ai-expert-msg-ai {
  justify-content: flex-start;
}

.ai-expert-msg-bubble {
  max-width: 85%;
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 13px;
  line-height: 20px;
  white-space: pre-wrap;
  word-break: break-word;
}

.ai-expert-msg-user .ai-expert-msg-bubble {
  background: var(--ag-primary-500);
  color: white;
  border-bottom-right-radius: 4px;
}

.ai-expert-msg-ai .ai-expert-msg-bubble {
  background: white;
  color: var(--ag-text-primary);
  border: 1px solid var(--ag-border);
  border-bottom-left-radius: 4px;
}

.ai-expert-typing {
  display: inline-flex;
  gap: 4px;
  align-items: center;
}

.ai-expert-typing span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--ag-text-muted);
  animation: ai-typing 1.2s infinite ease-in-out;
}

.ai-expert-typing span:nth-child(2) {
  animation-delay: 0.15s;
}

.ai-expert-typing span:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes ai-typing {
  0%, 80%, 100% {
    opacity: .4;
  }
  40% {
    opacity: 1;
  }
}

.ai-expert-fallback {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  text-align: center;
  padding: 16px;
  border-radius: 14px;
  border: 1px dashed color-mix(in srgb, var(--ag-secondary-500) 40%, transparent);
  background: color-mix(in srgb, var(--ag-secondary-500) 6%, transparent);
}

.ai-expert-fallback-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: color-mix(in srgb, var(--ag-secondary-500) 12%, transparent);
  color: var(--ag-secondary-500);
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-expert-fallback-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--ag-text-primary);
}

.ai-expert-fallback-desc {
  font-size: 12px;
  line-height: 18px;
  color: var(--ag-text-secondary);
}

.ai-expert-fallback-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 18px;
  border-radius: 9999px;
  border: none;
  background: var(--ag-secondary-500);
  color: white;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.ai-expert-fallback-btn:hover {
  opacity: 0.9;
}

.ai-expert-footer {
  padding: 12px;
  border-top: 1px solid var(--ag-border);
  background: white;
}

.ai-expert-form {
  display: flex;
  gap: 8px;
}

.ai-expert-input {
  flex: 1;
  padding: 10px 14px;
  border-radius: 9999px;
  border: 1px solid var(--ag-border);
  background: var(--ag-surface-container-lowest);
  font-size: 13px;
  outline: none;
  color: var(--ag-text-primary);
}

.ai-expert-input:focus {
  border-color: var(--ag-primary-500);
}

.ai-expert-send {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: var(--ag-primary-500);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: opacity 0.2s;
  flex-shrink: 0;
}

.ai-expert-send:disabled {
  opacity: 0.5;
  cursor: default;
}

.ai-fade-enter-active, .ai-fade-leave-active {
  transition: all 0.25s ease;
}

.ai-fade-enter-from, .ai-fade-leave-to {
  opacity: 0;
  transform: translateY(12px) scale(0.98);
}

.ai-expert-body {
  min-height: 0;
}

.ai-expert-label {
  display: block;
  margin-bottom: 6px;
  color: var(--ag-text-primary);
  font-size: 13px;
}

.ai-expert-input {
  min-width: 0;
  resize: vertical;
  border-radius: 14px;
  font-size: 16px;
  max-height: 120px;
}

.ai-expert-close, .ai-expert-send {
  min-width: 44px;
  min-height: 44px;
}

.ai-expert-error {
  color: var(--ag-text-primary);
  background: var(--ag-bg-sand);
  padding: 10px;
  margin-bottom: 8px;
  border-radius: 12px;
  font-size: 13px;
}

.ai-expert-hint {
  font-size: 11px;
  color: var(--ag-text-secondary);
  margin-top: 8px;
}

.ai-expert-sources {
  white-space: normal;
  margin-top: 10px;
  padding-left: 16px;
}

.ai-expert-sources a {
  color: var(--ag-primary-500);
  text-decoration: underline;
  overflow-wrap: anywhere;
}

.ai-expert button:focus-visible, .ai-expert a:focus-visible, .ai-expert textarea:focus-visible {
  outline: 2px solid var(--ag-primary-500);
  outline-offset: 3px;
}

.ai-expert button:disabled {
  opacity: 0.5;
  cursor: default;
}

.ai-expert-fab {
  bottom: calc(112px + env(safe-area-inset-bottom, 0px));
}

.ai-expert-panel {
  bottom: calc(180px + env(safe-area-inset-bottom, 0px));
  max-height: calc(100dvh - 208px);
}

@media (max-width: 640px) {
  .ai-expert-fab {
    right: 16px;
  }

  .ai-expert-panel {
    right: 16px;
    height: 540px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .ai-expert *, .ai-fade-enter-active, .ai-fade-leave-active {
    animation: none !important;
    transition: none !important;
  }
}
</style>

<template>
  <div class="ai-expert">
    <!-- Floating trigger -->
    <button type="button" class="ai-expert-fab" title="Nói chuyện với chuyên gia" @click="open">
      <span class="material-symbols-outlined">support_agent</span>
      <span v-if="!opened" class="ai-expert-fab-pulse"></span>
    </button>

    <!-- Chat panel -->
    <Transition name="ai-fade">
      <div v-if="opened" class="ai-expert-panel">
        <header class="ai-expert-header">
          <div class="ai-expert-header-avatar">
            <span class="material-symbols-outlined">eco</span>
          </div>
          <div class="ai-expert-header-info">
            <p class="ai-expert-header-title">Chuyên gia AgriVerse</p>
            <p class="ai-expert-header-status">
              <span class="ai-expert-dot"></span>
              Trợ lý AI — trả lời tức thì
            </p>
          </div>
          <button type="button" class="ai-expert-close" @click="opened = false">
            <span class="material-symbols-outlined">close</span>
          </button>
        </header>

        <div ref="scrollRef" class="ai-expert-body">
          <div v-if="!messages.length" class="ai-expert-welcome">
            <div class="ai-expert-welcome-icon">
              <span class="material-symbols-outlined">psychology</span>
            </div>
            <h3 class="ai-expert-welcome-title">Bạn cần tư vấn gì về cây cảnh?</h3>
            <p class="ai-expert-welcome-desc">
              Hỏi về cách chăm sóc bonsai, chọn cây hợp phong thủy, xử lý sâu bệnh...
              Nếu AI chưa trả lời được, chúng tôi sẽ giúp bạn đăng lên diễn đàn để nghệ nhân hỗ trợ.
            </p>
            <div class="ai-expert-suggestions">
              <button v-for="s in suggestions" :key="s" type="button" class="ai-expert-chip" @click="send(s)">
                {{ s }}
              </button>
            </div>
          </div>

          <div v-for="(m, i) in messages" :key="i" class="ai-expert-msg" :class="m.role === 'user' ? 'ai-expert-msg-user' : 'ai-expert-msg-ai'">
            <p class="ai-expert-msg-bubble">{{ m.content }}</p>
          </div>

          <div v-if="pending" class="ai-expert-msg ai-expert-msg-ai">
            <p class="ai-expert-msg-bubble">
              <span class="ai-expert-typing">
                <span></span><span></span><span></span>
              </span>
            </p>
          </div>

          <!-- AI cannot answer → forum fallback -->
          <div v-if="fallbackFor" class="ai-expert-fallback">
            <div class="ai-expert-fallback-icon">
              <span class="material-symbols-outlined">forum</span>
            </div>
            <div class="ai-expert-fallback-text">
              <p class="ai-expert-fallback-title">AI chưa trả lời được câu hỏi này</p>
              <p class="ai-expert-fallback-desc">
                Đừng lo! Hãy đăng câu hỏi lên diễn đàn để các nghệ nhân &amp; cộng đồng AgriVerse hỗ trợ bạn.
              </p>
            </div>
            <button type="button" class="ai-expert-fallback-btn" @click="goForum">
              Đăng lên diễn đàn
              <span class="material-symbols-outlined">arrow_forward</span>
            </button>
          </div>
        </div>

        <footer class="ai-expert-footer">
          <form class="ai-expert-form" @submit.prevent="submit">
            <input v-model="input" type="text" placeholder="Nhập câu hỏi về cây cảnh..." class="ai-expert-input" :disabled="pending" maxlength="1000" />
            <button type="submit" class="ai-expert-send" :disabled="pending || !input.trim()">
              <span class="material-symbols-outlined">send</span>
            </button>
          </form>
        </footer>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onUnmounted } from 'vue';
import { router } from '@inertiajs/vue3';
import { route } from 'ziggy-js';

const opened = ref(false);
const input = ref('');
const messages = ref([]);
const pending = ref(false);
const fallbackFor = ref(null);
const scrollRef = ref(null);

const suggestions = [
  'Cây bonsai bị vàng lá thì phải làm sao?',
  'Nên chọn cây gì hợp người mệnh Kim?',
  'Cách tưới nước cho sen đá như thế nào?',
];

function open() {
  opened.value = true;
  nextTick(scrollToBottom);
}

onMounted(() => {
  window.__agriverse_open_chat = open;
});

onUnmounted(() => {
  window.__agriverse_open_chat = null;
});

function submit() {
  const text = input.value.trim();
  if (!text || pending.value) return;
  send(text);
}

async function send(text) {
  messages.value.push({ role: 'user', content: text });
  fallbackFor.value = null;
  input.value = '';
  pending.value = true;
  nextTick(scrollToBottom);

  try {
    const { data } = await window.axios.post(route('agriverse.shop.ai.chat'), { message: text });
    if (data && data.answered) {
      messages.value.push({ role: 'ai', content: data.reply });
    } else {
      messages.value.push({
        role: 'ai',
        content: 'Xin lỗi, hiện tại tôi chưa thể trả lời câu hỏi này một cách chắc chắn. Bạn có thể đăng lên diễn đàn để được các nghệ nhân hỗ trợ nhé.',
      });
      fallbackFor.value = text;
    }
  } catch {
    messages.value.push({
      role: 'ai',
      content: 'Đã có lỗi khi kết nối. Vui lòng thử lại hoặc đăng câu hỏi lên diễn đàn để được hỗ trợ.',
    });
    fallbackFor.value = text;
  } finally {
    pending.value = false;
    nextTick(scrollToBottom);
  }
}

function goForum() {
  const q = fallbackFor.value || input.value;
  router.get(route('agriverse.shop.forum.create', q ? { title: q.slice(0, 200), content: q } : {}));
}

function scrollToBottom() {
  if (scrollRef.value) scrollRef.value.scrollTop = scrollRef.value.scrollHeight;
}

window.addEventListener('agriverse-open-ai-expert', open);
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
.ai-expert-fab:hover { transform: scale(1.06); }
.ai-expert-fab:active { transform: scale(0.95); }
.ai-expert-fab .material-symbols-outlined { font-size: 26px; }
.ai-expert-fab-pulse {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #34c759;
  border: 2px solid white;
  animation: ai-pulse 2s ease-in-out infinite;
}
@keyframes ai-pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.2); opacity: 0.7; }
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
  .ai-expert-panel { bottom: 80px; right: 16px; height: calc(100vh - 100px); }
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
.ai-expert-header-info { flex: 1; min-width: 0; }
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
.ai-expert-close:hover { background: rgba(255, 255, 255, 0.25); }

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
.ai-expert-msg-user { justify-content: flex-end; }
.ai-expert-msg-ai { justify-content: flex-start; }
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
  animation: ai-bounce 1.2s infinite ease-in-out;
}
.ai-expert-typing span:nth-child(2) { animation-delay: 0.15s; }
.ai-expert-typing span:nth-child(3) { animation-delay: 0.3s; }
@keyframes ai-bounce {
  0%, 80%, 100% { transform: translateY(0); }
  40% { transform: translateY(-4px); }
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
.ai-expert-fallback-btn:hover { opacity: 0.9; }

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
.ai-expert-input:focus { border-color: var(--ag-primary-500); }
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
.ai-expert-send:disabled { opacity: 0.5; cursor: default; }

.ai-fade-enter-active, .ai-fade-leave-active { transition: all 0.25s ease; }
.ai-fade-enter-from, .ai-fade-leave-to { opacity: 0; transform: translateY(12px) scale(0.98); }
</style>

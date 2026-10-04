import { reactive, readonly } from 'vue';

const state = reactive({
  panelOpen: false,
  activeConversationId: null,
  activeProduct: null,
  pendingMessage: null,
  unreadTotal: 0,
});

export function useChat() {
  function openPanel(conversationId = null, product = null, message = null) {
    if (conversationId) state.activeConversationId = conversationId;
    if (product) state.activeProduct = product;
    if (message && typeof message === 'string') state.pendingMessage = message;
    state.panelOpen = true;
  }

  function closePanel() {
    state.panelOpen = false;
    state.activeConversationId = null;
    state.activeProduct = null;
    state.pendingMessage = null;
  }

  function togglePanel() {
    state.panelOpen = !state.panelOpen;
    if (!state.panelOpen) {
      state.activeConversationId = null;
      state.activeProduct = null;
      state.pendingMessage = null;
    }
  }

  function selectConversation(id) {
    state.activeConversationId = id;
  }

  function clearPendingMessage() {
    state.pendingMessage = null;
  }

  function setUnreadTotal(n) {
    state.unreadTotal = Math.max(0, parseInt(n, 10) || 0);
  }

  return {
    state: readonly(state),
    openPanel,
    closePanel,
    togglePanel,
    selectConversation,
    clearPendingMessage,
    setUnreadTotal,
  };
}
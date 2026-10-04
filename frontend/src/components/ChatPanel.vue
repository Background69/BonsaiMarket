<template>
  <Transition name="chat-panel">
    <div v-if="state.panelOpen" class="chat-overlay" @click.self="closePanel">
      <div class="chat-panel" @click.stop>
        <!-- ===== LEFT SIDEBAR ===== -->
        <div class="chat-sidebar" :class="{ 'chat-sidebar--hidden': activeChatId }">
          <div class="chat-sidebar-header">
            <span class="chat-sidebar-title">Chat</span>
            <button class="chat-header-btn" @click="closePanel">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <!-- Tabs -->
          <div class="chat-tabs">
            <button
              class="chat-tab"
              :class="{ 'chat-tab--active': activeTab === 'messages' }"
              @click="activeTab = 'messages'"
            >Tin nhắn</button>
            <button
              class="chat-tab"
              :class="{ 'chat-tab--active': activeTab === 'groups' }"
              @click="activeTab = 'groups'"
            >Cộng đồng</button>
          </div>

          <!-- New message (messages tab) -->
          <div v-if="activeTab === 'messages'" class="chat-new-msg-wrap">
            <button class="chat-new-msg-btn" @click="openNewMessage">
              <span class="material-symbols-outlined" style="font-size:18px">edit</span>
              Tin nhắn mới
            </button>
          </div>

          <!-- Search (messages tab) -->
          <div v-if="activeTab === 'messages'" class="chat-search-wrap">
            <span class="material-symbols-outlined chat-search-icon">search</span>
            <input v-model="searchQuery" class="chat-search" placeholder="Tìm kiếm trên Chat" />
          </div>

          <!-- Groups tab header -->
          <div v-if="activeTab === 'groups'" class="chat-create-group-wrap">
            <button class="chat-create-group-btn" @click="fetchGroups">
              <span class="material-symbols-outlined">refresh</span>
              Làm mới
            </button>
          </div>

          <div class="chat-sidebar-body">
            <!-- Conversation List -->
            <template v-if="activeTab === 'messages'">
              <div v-if="loading" class="chat-loading">Đang tải...</div>
<div v-else-if="filteredConversations.length === 0" class="chat-empty">
    <span class="material-symbols-outlined" style="font-size:48px;opacity:0.3">chat</span>
    <p>Chưa có tin nhắn nào</p>
    <button class="chat-empty-btn" @click="openNewMessage">
      <span class="material-symbols-outlined" style="font-size:16px">edit</span>
      Bắt đầu cuộc trò chuyện
    </button>
  </div>
              <div v-else class="chat-conv-list">
                <div
                  v-for="conv in filteredConversations"
                  :key="'c' + conv.id"
                  class="chat-conv-item"
                  :class="{ 'chat-conv-item--active': activeChatId === 'c' + conv.id }"
                  @click="openConversation(conv)"
                >
                  <div class="chat-avatar">
                    <span class="chat-avatar-text">{{ conv.other_user?.name?.charAt(0)?.toUpperCase() || '?' }}</span>
                    <span v-if="isOnline(conv.other_user?.id)" class="chat-online-dot" title="Đang hoạt động"></span>
                  </div>
                  <div class="chat-conv-info">
                    <div class="chat-conv-top">
                      <span class="chat-conv-name">{{ conv.other_user?.name || 'Người dùng' }}</span>
                      <span class="chat-conv-time">{{ conv.last_message_at }}</span>
                    </div>
                    <div class="chat-conv-bottom">
                      <span class="chat-conv-preview">{{ conv.last_message || 'Chưa có tin nhắn' }}</span>
                      <span v-if="conv.unread > 0" class="chat-badge">{{ conv.unread }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </template>

            <!-- Group List -->
            <template v-if="activeTab === 'groups'">
              <div v-if="loadingGroups" class="chat-loading">Đang tải...</div>
              <div v-else-if="groups.length === 0" class="chat-empty">
                <span class="material-symbols-outlined" style="font-size:48px;opacity:0.3">groups</span>
                <p>Chưa tham gia nhóm nào</p>
              </div>
                  <div v-else class="chat-conv-list">
                <div
                  v-for="g in groups"
                  :key="'g' + g.id"
                  class="chat-conv-item"
                  :class="{
                    'chat-conv-item--active': activeChatId === 'g' + g.id,
                    'chat-conv-item--pending': g.is_pending,
                  }"
                  @click="openGroup(g)"
                >
                  <div class="chat-avatar chat-avatar--group">
                    <span class="material-symbols-outlined" style="font-size:22px">groups</span>
                  </div>
                  <div class="chat-conv-info">
                    <div class="chat-conv-top">
                      <span class="chat-conv-name">{{ g.name }}</span>
                      <span class="chat-conv-time">{{ g.member_count }} thành viên</span>
                    </div>
                    <div class="chat-conv-bottom">
                      <span class="chat-conv-preview">{{ g.created_by }}</span>
                      <span v-if="g.is_pending" class="chat-badge-pending">Chờ duyệt</span>
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </div>

        <!-- ===== RIGHT: Private Chat ===== -->
        <div v-if="activeChatId?.startsWith('c')" class="chat-main">
          <div class="chat-main-header">
            <button class="chat-back-btn" @click="activeChatId = null">
              <span class="material-symbols-outlined">arrow_back</span>
            </button>
            <div class="chat-main-user">
              <div class="chat-avatar chat-avatar--sm">
                <span class="chat-avatar-text">{{ activeConv?.other_user?.name?.charAt(0)?.toUpperCase() || '?' }}</span>
                <span v-if="isOnline(activeConv?.other_user?.id)" class="chat-online-dot" title="Đang hoạt động"></span>
              </div>
              <div class="chat-main-user-info">
                <span class="chat-main-user-name">{{ activeConv?.other_user?.name || 'Người dùng' }}</span>
                <span v-if="activeConv?.product" class="chat-context-chip">
                  <span class="material-symbols-outlined" style="font-size:13px">spa</span>
                  {{ activeConv.product.name }}
                </span>
              </div>
            </div>
            <div class="chat-main-actions">
              <button class="chat-header-btn" @click="closePanel">
                <span class="material-symbols-outlined">close</span>
              </button>
            </div>
          </div>
          <MessageArea
            ref="messageAreaRef"
            :loading="messagesLoading"
            :has-more="hasMore"
            :loading-older="isLoadingMore"
            @load-older="loadOlderMessages"
          >
            <div
              v-for="msg in chatMessages"
              :key="msg.id"
              class="chat-msg-row"
              :class="msg.is_mine ? 'chat-msg-row--mine' : 'chat-msg-row--theirs'"
            >
              <div class="chat-bubble" :class="{ 'chat-bubble--mine': msg.is_mine }">
                <span class="chat-bubble-text">{{ msg.message }}</span>
                <div class="chat-bubble-meta">
                  <span class="chat-bubble-time">{{ formatTime(msg.created_at) }}</span>
                  <span v-if="msg.is_mine && !msg.is_sending" class="material-symbols-outlined chat-seen-icon">done_all</span>
                  <span v-if="msg.is_sending" class="material-symbols-outlined chat-seen-icon chat-seen-icon--sending">schedule</span>
                </div>
              </div>
            </div>
          </MessageArea>
          <div class="chat-input-wrap">
            <div class="chat-input-target">
              <span class="material-symbols-outlined" style="font-size:14px;">chat</span>
              <span class="chat-input-target-text">{{ activeConv?.other_user?.name || 'Người dùng' }}</span>
            </div>
            <div class="chat-input-box">
              <input
                ref="inputRef"
                v-model="newMessage"
                class="chat-input"
                placeholder="Nhập tin nhắn..."
                @keyup.enter="sendMessage"
                :disabled="sending"
              />
              <button class="chat-send-btn" @click="sendMessage" :disabled="sending || !newMessage.trim()">
                <span class="material-symbols-outlined">send</span>
              </button>
            </div>
          </div>
        </div>

        <!-- ===== RIGHT: Group Chat ===== -->
        <div v-if="activeChatId?.startsWith('g')" class="chat-main">
          <div class="chat-main-header">
            <button class="chat-back-btn" @click="activeChatId = null">
              <span class="material-symbols-outlined">arrow_back</span>
            </button>
            <div class="chat-main-user">
              <div class="chat-avatar chat-avatar--sm chat-avatar--group">
                <span class="material-symbols-outlined" style="font-size:18px">groups</span>
              </div>
              <div class="chat-main-user-info">
                <span class="chat-main-user-name">{{ activeGroup?.name }}</span>
                <span class="chat-main-user-status">{{ activeGroup?.member_count }} thành viên</span>
              </div>
            </div>
            <div class="chat-main-actions">
              <button v-if="activeGroup?.is_creator" class="chat-header-btn" title="Thêm thành viên" @click="showAddMember = true">
                <span class="material-symbols-outlined">person_add</span>
              </button>
              <button class="chat-header-btn" @click="closePanel">
                <span class="material-symbols-outlined">close</span>
              </button>
            </div>
          </div>

          <!-- Pending state -->
          <div v-if="activeGroup?.is_pending" class="chat-pending-state">
            <span class="material-symbols-outlined" style="font-size:48px;opacity:0.3">hourglass_empty</span>
            <h3>Yêu cầu đang chờ duyệt</h3>
            <p>Vui lòng chờ admin duyệt yêu cầu tham gia nhóm của bạn</p>
          </div>

          <template v-else>
            <MessageArea
              ref="messageAreaRef"
              :loading="groupMessagesLoading"
              :has-more="groupHasMore"
              :loading-older="groupLoadingOlder"
              @load-older="loadOlderGroupMessages"
            >
              <div
                v-for="msg in groupMessages"
                :key="msg.id"
                class="chat-msg-row"
                :class="msg.is_mine ? 'chat-msg-row--mine' : 'chat-msg-row--theirs'"
              >
                <div class="chat-bubble" :class="{ 'chat-bubble--mine': msg.is_mine }">
                  <span v-if="!msg.is_mine" class="chat-bubble-sender">{{ msg.sender_name }}</span>
                  <span class="chat-bubble-text">{{ msg.message }}</span>
                  <div class="chat-bubble-meta">
                    <span class="chat-bubble-time">{{ formatTime(msg.created_at) }}</span>
                  </div>
                </div>
              </div>
            </MessageArea>
            <div class="chat-input-wrap">
              <div class="chat-input-target">
                <span class="material-symbols-outlined" style="font-size:14px;">groups</span>
                <span class="chat-input-target-text">{{ activeGroup?.name || 'Nhóm' }}</span>
              </div>
              <div class="chat-input-box">
                <input
                  ref="inputRef"
                  v-model="newMessage"
                  class="chat-input"
                  placeholder="Nhập tin nhắn..."
                  @keyup.enter="sendGroupMessage"
                  :disabled="groupSending"
                />
                <button class="chat-send-btn" @click="sendGroupMessage" :disabled="groupSending || !newMessage.trim()">
                  <span class="material-symbols-outlined">send</span>
                </button>
              </div>
            </div>
          </template>
        </div>

        <!-- ===== EMPTY STATE ===== -->
        <div v-if="!activeChatId" class="chat-main chat-main--empty">
          <div class="chat-empty-state">
            <span class="material-symbols-outlined" style="font-size:72px;opacity:0.15">chat</span>
            <h3>Tin nhắn của bạn</h3>
            <p>Chọn một cuộc trò chuyện hoặc bắt đầu cuộc trò chuyện mới với người bán.</p>
            <p class="chat-empty-state-hint">Bắt đầu bằng cách chọn sản phẩm yêu thích để nhắn tin với người bán.</p>
            <button class="chat-empty-state-btn" @click="openNewMessage">
              <span class="material-symbols-outlined" style="font-size:18px">edit</span>
              Tin nhắn mới
            </button>
          </div>
        </div>

        <!-- Create Group Modal -->
        <Teleport to="body">
          <div v-if="showCreateGroup" class="chat-overlay" @click.self="showCreateGroup = false">
            <div class="chat-modal">
              <div class="chat-modal-header">
                <span class="chat-modal-title">Tạo nhóm mới</span>
                <button class="chat-header-btn" @click="showCreateGroup = false">
                  <span class="material-symbols-outlined">close</span>
                </button>
              </div>
              <div class="chat-modal-body">
                <input
                  v-model="newGroupName"
                  class="chat-modal-input"
                  placeholder="Nhập tên nhóm..."
                  @keyup.enter="createGroup"
                  maxlength="100"
                />
              </div>
              <div class="chat-modal-footer">
                <button class="chat-modal-cancel" @click="showCreateGroup = false">Hủy</button>
                <button class="chat-modal-confirm" @click="createGroup" :disabled="!newGroupName.trim()">Tạo</button>
              </div>
            </div>
          </div>
        </Teleport>

        <!-- Add Member Modal -->
        <Teleport to="body">
          <div v-if="showAddMember" class="chat-overlay" @click.self="showAddMember = false">
            <div class="chat-modal">
              <div class="chat-modal-header">
                <span class="chat-modal-title">Thêm thành viên</span>
                <button class="chat-header-btn" @click="showAddMember = false">
                  <span class="material-symbols-outlined">close</span>
                </button>
              </div>
              <div class="chat-modal-body">
                <input
                  v-model="searchUserQuery"
                  class="chat-modal-input"
                  placeholder="Tìm kiếm người dùng..."
                  @input="searchUsers"
                />
                <div v-if="searchingUsers" class="chat-loading" style="padding:12px">Đang tìm...</div>
                <div v-else-if="searchUserResults.length > 0" class="chat-user-list">
                  <div
                    v-for="u in searchUserResults"
                    :key="u.id"
                    class="chat-user-item"
                    @click="addMember(u)"
                  >
                    <div class="chat-avatar chat-avatar--xs">
                      <span class="chat-avatar-text">{{ u.name.charAt(0).toUpperCase() }}</span>
                    </div>
                    <span class="chat-user-name">{{ u.name }}</span>
                  </div>
                </div>
                <div v-else-if="searchUserQuery.length >= 2" class="chat-loading" style="padding:12px">Không tìm thấy người dùng</div>
              </div>
            </div>
          </div>
        </Teleport>

        <!-- New Message Modal -->
        <Teleport to="body">
          <div v-if="showNewMessage" class="chat-overlay" @click.self="showNewMessage = false">
            <div class="chat-modal">
              <div class="chat-modal-header">
                <span class="chat-modal-title">Bắt đầu cuộc trò chuyện</span>
                <button class="chat-header-btn" @click="showNewMessage = false">
                  <span class="material-symbols-outlined">close</span>
                </button>
              </div>
              <div class="chat-modal-body">
                <input
                  v-model="productSearchQuery"
                  class="chat-modal-input"
                  placeholder="Tìm sản phẩm / người bán..."
                  @input="fetchPickableProducts"
                />
                <div v-if="loadingPickable" class="chat-loading" style="padding:16px">Đang tải sản phẩm...</div>
                <div v-else-if="pickableProducts.length > 0" class="chat-user-list">
                  <div
                    v-for="p in filteredPickableProducts"
                    :key="p.id"
                    class="chat-product-item"
                    @click="startConversation(p)"
                  >
                    <img v-if="p.image" :src="p.image" class="chat-product-thumb" alt="" />
                    <div v-else class="chat-product-thumb" style="display:flex;align-items:center;justify-content:center;">
                      <span class="material-symbols-outlined" style="opacity:0.4">spa</span>
                    </div>
                    <div class="chat-product-info">
                      <span class="chat-product-name">{{ p.name }}</span>
                      <span class="chat-product-seller">{{ p.seller_name }}</span>
                    </div>
                  </div>
                </div>
                <div v-else class="chat-loading" style="padding:16px">Không có sản phẩm nào để liên hệ. Hãy vào trang sản phẩm để nhắn tin với người bán.</div>
              </div>
            </div>
          </div>
        </Teleport>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import axios from 'axios'
import { useChat } from '@agriverse/Composables/useChat'
import { useChatSocket } from '@agriverse/Composables/useChatSocket'
import MessageArea from './MessageArea.vue'

// CSRF token cho các POST request dạng web
const token = document.querySelector('meta[name="csrf-token"]')?.getAttribute('content')
if (token) {
  axios.defaults.headers.common['X-CSRF-TOKEN'] = token
}

const { state, closePanel, selectConversation, clearPendingMessage, setUnreadTotal } = useChat()
const { connect, on, off, connected, onlineUserIds } = useChatSocket()
const userId = document.querySelector('meta[name="user-id"]')?.getAttribute('content')

const activeTab = ref('messages')

// Private chat
const conversations = ref([])
const chatMessages = ref([])
const loading = ref(false)
const messagesLoading = ref(false)
const isLoadingMore = ref(false)
const hasMore = ref(true)
const page = ref(1)

// Group chat
const groups = ref([])
const groupMessages = ref([])
const loadingGroups = ref(false)
const groupMessagesLoading = ref(false)
const groupLoadingOlder = ref(false)
const groupHasMore = ref(true)
const groupPage = ref(1)

const sending = ref(false)
const groupSending = ref(false)
const newMessage = ref('')
const messagesRef = ref(null)
const inputRef = ref(null)
const searchQuery = ref('')
const messageAreaRef = ref(null)

const showCreateGroup = ref(false)
const newGroupName = ref('')

const showAddMember = ref(false)
const searchUserQuery = ref('')
const searchUserResults = ref([])
const searchingUsers = ref(false)

// New message flow
const showNewMessage = ref(false)
const productSearchQuery = ref('')
const pickableProducts = ref([])
const loadingPickable = ref(false)

// activeChatId = 'c{id}' for conversation, 'g{id}' for group
const activeChatId = ref(null)

const activeConv = computed(() =>
  conversations.value.find(c => 'c' + c.id === activeChatId.value) || null
)

const activeGroup = computed(() =>
  groups.value.find(g => 'g' + g.id === activeChatId.value) || null
)

const filteredConversations = computed(() => {
  const q = searchQuery.value.toLowerCase().trim()
  if (!q) return conversations.value
  return conversations.value.filter(c =>
    c.other_user?.name?.toLowerCase().includes(q) ||
    c.last_message?.toLowerCase().includes(q)
  )
})

watch(() => state.activeConversationId, (id) => {
  if (id) {
    activeTab.value = 'messages'
    resetChat()
    activeChatId.value = 'c' + id
    const conv = conversations.value.find(c => 'c' + c.id === activeChatId.value)
    if (conv && conv.unread > 0) {
      conv.unread = 0
      recomputeUnread()
    }
    fetchMessages()
  }
})

watch(() => state.panelOpen, (open) => {
  if (open) {
    fetchConversations()
    fetchGroups()
    if (state.activeConversationId) {
      activeTab.value = 'messages'
      resetChat()
      activeChatId.value = 'c' + state.activeConversationId
      fetchMessages()
    }
  } else {
    activeChatId.value = null
    stopGroupPolling()
  }
})

watch(activeChatId, (id) => {
  if (!id?.startsWith('g')) {
    stopGroupPolling()
  }
})

const handleIncomingMessage = async (data) => {
  if (!data) return

  if (data.type === 'private') {
    const otherId = String(data.from)
    const isOwn = otherId === String(userId)
    const conv = conversations.value.find(c =>
      String(c.buyer_id) === otherId ||
      String(c.seller_id) === otherId
    )

    if (conv) {
      conv.last_message = data.content
      conv.last_message_at = new Date(data.timestamp || Date.now()).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
      if (!isOwn && activeChatId.value !== 'c' + conv.id) {
        conv.unread = (conv.unread || 0) + 1
      }
      sortConversationToTop(conv)
    } else {
      fetchConversations()
    }

    if (state.panelOpen && activeChatId.value === 'c' + conv?.id) {
      if (!isOwn) {
        appendSortedMessage(chatMessages, {
          id: 'ws-' + (data.timestamp || Date.now()),
          message: data.content,
          from: data.from,
          is_mine: false,
          created_at: new Date(data.timestamp || Date.now()).toISOString()
        })
      }
      await scrollToBottom()
    }
    recomputeUnread()
  }

  if (data.type === 'group' && state.panelOpen) {
    const isOwn = String(data.from) === userId
    if (activeChatId.value === 'g' + data.to) {
      if (!isOwn) {
        const senderName = groupMembersMap.value[data.from] || 'Thành viên'
        appendSortedMessage(groupMessages, {
          id: 'ws-' + (data.timestamp || Date.now()),
          message: data.content,
          from: data.from,
          sender_name: senderName,
          is_mine: false,
          created_at: new Date(data.timestamp || Date.now()).toISOString()
        })
      }
      await scrollToBottom()
    } else {
      fetchGroups()
    }
  }
}

function recomputeUnread() {
  const total = conversations.value.reduce((sum, c) => sum + (c.unread || 0), 0)
  setUnreadTotal(total)
}

let onlineDirty = false
function handlePresence(data) {
  // onlineUserIds đã được cập nhật trong useChatSocket; chỉ cần trigger re-render
  onlineDirty = true
}

function isOnline(otherUserId) {
  if (!otherUserId) return false
  return onlineUserIds.value.has(String(otherUserId))
}

onMounted(() => {
  connect()
  on('message', handleIncomingMessage)
  on('presence', handlePresence)
  fetchConversations()
  fetchGroups()
})

const groupMembersMap = ref({})
let pollTimer = null

function startGroupPolling() {
  stopGroupPolling()
  if (!connected.value) {
    pollTimer = setInterval(fetchGroupMessages, 5000)
  }
}

function stopGroupPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

onBeforeUnmount(() => {
  off('message', handleIncomingMessage)
  off('presence', handlePresence)
  stopGroupPolling()
})

function resetChat() {
  chatMessages.value = []
  groupMessages.value = []
  page.value = 1
  hasMore.value = true
  isLoadingMore.value = false
  groupPage.value = 1
  groupHasMore.value = true
  groupLoadingOlder.value = false
}

async function fetchConversations() {
  loading.value = true
  try {
    const { data } = await axios.get('/agriverse/api/chat/conversations')
    conversations.value = data.conversations || []
    recomputeUnread()
  } catch {
    conversations.value = []
    recomputeUnread()
  } finally {
    loading.value = false
  }
}

async function fetchGroups() {
  loadingGroups.value = true
  try {
    const { data } = await axios.get('/agriverse/api/chat/groups')
    groups.value = data.groups || []
  } catch {
    groups.value = []
  } finally {
    loadingGroups.value = false
  }
}

// ===== Private Conversation =====

async function openConversation(conv) {
  resetChat()
  activeChatId.value = 'c' + conv.id
  selectConversation(conv.id)
  activeTab.value = 'messages'
  if (conv.unread > 0) {
    conv.unread = 0
    recomputeUnread()
  }
  await fetchMessages()
  inputRef.value?.focus()
}

async function fetchMessages() {
  if (!activeChatId.value?.startsWith('c')) return
  messagesLoading.value = true
  try {
    const convId = activeChatId.value.slice(1)
    const { data } = await axios.get(`/agriverse/api/chat/${convId}/messages`, {
      params: { page: page.value }
    })
    // Merge history vào mảng hiện có (nếu có tin realtime đến trong lúc load) — không replace/đè
    mergeMessages(chatMessages, data.messages || [])
    hasMore.value = data.has_more ?? false
    await nextTick()
    scrollToBottom()
    if (state.pendingMessage) {
      const text = state.pendingMessage
      clearPendingMessage()
      await sendRawMessage(text)
    }
  } catch {
    chatMessages.value = []
    hasMore.value = false
  } finally {
    messagesLoading.value = false
    await nextTick()
    scrollToBottom()
  }
}

async function loadOlderMessages() {
  if (!hasMore.value || isLoadingMore.value || messagesLoading.value) return
  isLoadingMore.value = true
  page.value++
  const prevScrollHeight = messageAreaRef.value?.scrollRef?.scrollHeight || 0
  try {
    const convId = activeChatId.value.slice(1)
    const { data } = await axios.get(`/agriverse/api/chat/${convId}/messages`, {
      params: { page: page.value }
    })
    const msgs = data.messages || []
    hasMore.value = data.has_more ?? false
    if (msgs.length > 0) {
      // Gộp trang cũ lên đầu + dedup + sort, giữ scroll không nhảy qua trang mới
      const prev = chatMessages.value
      chatMessages.value = []
      mergeMessages(chatMessages, [...msgs, ...prev])
      await nextTick()
      if (messageAreaRef.value?.scrollRef) {
        messageAreaRef.value.scrollRef.scrollTop = messageAreaRef.value.scrollRef.scrollHeight - prevScrollHeight
      }
    }
  } catch {
    page.value--
  } finally {
    isLoadingMore.value = false
  }
}

async function sendRawMessage(text) {
  if (!text || sending.value || !activeChatId.value?.startsWith('c')) return

  const tempId = 'temp-' + Date.now()
  chatMessages.value.push({
    id: tempId,
    message: text,
    is_mine: true,
    created_at: new Date().toISOString(),
    is_sending: true
  })
  await nextTick()
  scrollToBottom()
  inputRef.value?.focus()

  const convId = activeChatId.value.slice(1)
  // Optimistic: cap nhat sidebar ngay lap tuc, khong cho den khi WS echo hay refetch
  const conv = conversations.value.find(c => String(c.id) === String(convId))
  if (conv) {
    const hadUnread = (conv.unread || 0) > 0
    conv.last_message = text
    conv.last_message_at = new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
    if (hadUnread) {
      conv.unread = 0
    }
    sortConversationToTop(conv)
    recomputeUnread()
  }
  sending.value = true
  try {
    const { data } = await axios.post(`/agriverse/api/chat/${convId}/send`, { message: text })
    const idx = chatMessages.value.findIndex(m => m.id === tempId)
    if (idx !== -1) chatMessages.value[idx] = data.message
    await fetchConversations()
  } catch {
    const idx = chatMessages.value.findIndex(m => m.id === tempId)
    if (idx !== -1) chatMessages.value.splice(idx, 1)
  } finally {
    sending.value = false
    await nextTick()
    inputRef.value?.focus()
  }
}

async function sendMessage() {
  const text = newMessage.value.trim()
  if (!text) return
  newMessage.value = ''
  await sendRawMessage(text)
}

// ===== Group Chat =====

async function openGroup(g) {
  resetChat()
  activeChatId.value = 'g' + g.id
  activeTab.value = 'groups'
  if (g.is_pending) return
  await Promise.all([
    fetchGroupMessages(),
    loadGroupMembers(g.id),
  ])
  inputRef.value?.focus()
  startGroupPolling()
}

async function loadGroupMembers(groupId) {
  try {
    const { data } = await axios.get(`/agriverse/api/chat/groups/${groupId}/members`)
    if (data.members) {
      const map = {}
      for (const m of data.members) {
        map[m.id] = m.name
      }
      groupMembersMap.value = map
    }
  } catch {
    // ignore
  }
}

async function fetchGroupMessages() {
  if (!activeChatId.value?.startsWith('g')) return
  groupMessagesLoading.value = true
  try {
    const groupId = activeChatId.value.slice(1)
    const { data } = await axios.get(`/agriverse/api/chat/groups/${groupId}/messages`, {
      params: { page: groupPage.value }
    })
    mergeMessages(groupMessages, data.messages || [])
    groupHasMore.value = data.has_more ?? false
    await nextTick()
    scrollToBottom()
  } catch {
    groupMessages.value = []
    groupHasMore.value = false
  } finally {
    groupMessagesLoading.value = false
  }
}

async function loadOlderGroupMessages() {
  if (!groupHasMore.value || groupLoadingOlder.value || groupMessagesLoading.value) return
  groupLoadingOlder.value = true
  groupPage.value++
  const prevScrollHeight = messageAreaRef.value?.scrollRef?.scrollHeight || 0
  try {
    const groupId = activeChatId.value.slice(1)
    const { data } = await axios.get(`/agriverse/api/chat/groups/${groupId}/messages`, {
      params: { page: groupPage.value }
    })
    const msgs = data.messages || []
    groupHasMore.value = data.has_more ?? false
    if (msgs.length > 0) {
      const prev = groupMessages.value
      groupMessages.value = []
      mergeMessages(groupMessages, [...msgs, ...prev])
      await nextTick()
      if (messageAreaRef.value?.scrollRef) {
        messageAreaRef.value.scrollRef.scrollTop = messageAreaRef.value.scrollRef.scrollHeight - prevScrollHeight
      }
    }
  } catch {
    groupPage.value--
  } finally {
    groupLoadingOlder.value = false
  }
}

async function sendGroupMessage() {
  const text = newMessage.value.trim()
  if (!text || groupSending.value || !activeChatId.value?.startsWith('g')) return

  const tempId = 'temp-' + Date.now()
  groupMessages.value.push({
    id: tempId,
    message: text,
    sender_name: 'Tôi',
    is_mine: true,
    created_at: new Date().toISOString(),
    is_sending: true
  })
  newMessage.value = ''
  await nextTick()
  scrollToBottom()
  inputRef.value?.focus()

  const groupId = activeChatId.value.slice(1)
  groupSending.value = true
  try {
    const { data } = await axios.post(`/agriverse/api/chat/groups/${groupId}/send`, { message: text })
    const idx = groupMessages.value.findIndex(m => m.id === tempId)
    if (idx !== -1) groupMessages.value[idx] = data.message
    await fetchGroups()
  } catch (e) {
    console.error('[GroupChat] Send failed:', e.response?.status, e.response?.data)
    const idx = groupMessages.value.findIndex(m => m.id === tempId)
    if (idx !== -1) groupMessages.value.splice(idx, 1)
  } finally {
    groupSending.value = false
    await nextTick()
    inputRef.value?.focus()
  }
}

async function createGroup() {
  const name = newGroupName.value.trim()
  if (!name) return
  try {
    await axios.post('/agriverse/api/chat/groups', { name })
    showCreateGroup.value = false
    newGroupName.value = ''
    activeTab.value = 'groups'
    await fetchGroups()
  } catch {
    // ignore
  }
}

let searchTimeout = null
function searchUsers() {
  clearTimeout(searchTimeout)
  const q = searchUserQuery.value.trim()
  if (q.length < 2) {
    searchUserResults.value = []
    return
  }
  searchingUsers.value = true
  searchTimeout = setTimeout(async () => {
    try {
      const { data } = await axios.get('/agriverse/api/chat/search-users', { params: { q } })
      searchUserResults.value = data.users || []
    } catch {
      searchUserResults.value = []
    } finally {
      searchingUsers.value = false
    }
  }, 300)
}

async function addMember(user) {
  if (!activeChatId.value?.startsWith('g')) return
  const groupId = activeChatId.value.slice(1)
  try {
    await axios.post(`/agriverse/api/chat/groups/${groupId}/add-member`, { user_id: user.id })
    showAddMember.value = false
    searchUserQuery.value = ''
    searchUserResults.value = []
    await fetchGroups()
  } catch (e) {
    const msg = e.response?.data?.error || 'Không thể thêm thành viên'
    alert(msg)
  }
}

// ===== New message flow =====

const filteredPickableProducts = computed(() => {
  const q = productSearchQuery.value.toLowerCase().trim()
  if (!q) return pickableProducts.value
  return pickableProducts.value.filter(p =>
    p.name?.toLowerCase().includes(q) || p.seller_name?.toLowerCase().includes(q)
  )
})

async function openNewMessage() {
  showNewMessage.value = true
  productSearchQuery.value = ''
  await fetchPickableProducts()
}

async function fetchPickableProducts() {
  loadingPickable.value = true
  try {
    const { data } = await axios.get('/agriverse/api/chat/pickable-products')
    pickableProducts.value = data.products || []
  } catch {
    pickableProducts.value = []
  } finally {
    loadingPickable.value = false
  }
}

async function startConversation(product) {
  showNewMessage.value = false
  try {
    const { data } = await axios.post('/agriverse/api/chat/start', { product_id: product.id })
    activeTab.value = 'messages'
    resetChat()
    activeChatId.value = 'c' + data.conversation.id
    await fetchMessages()
    inputRef.value?.focus()
  } catch (e) {
    const msg = e.response?.data?.error || 'Không thể bắt đầu trò chuyện'
    alert(msg)
  }
}

const scrollToBottom = () => {
  return new Promise((resolve) => {
    // Đợi DOM render xong (nhiều tick + rAF) rồi cuộn xuống tin mới nhất
    nextTick(() => {
      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          const el = messageAreaRef.value?.scrollRef
          if (el) {
            el.scrollTop = el.scrollHeight
          }
          resolve()
        })
      })
    })
  })
}

// Đưa 1 hội thoại lên đầu danh sách sau khi có activity mới (gửi/nhận)
function sortConversationToTop(conv) {
  const idx = conversations.value.indexOf(conv)
  if (idx > 0) {
    conversations.value.splice(idx, 1)
    conversations.value.unshift(conv)
  }
}

/**
 * Hợp nhất mảng tin nhắn (history + realtime + load-older) thành 1 chuỗi liên tục:
 * - concat (spread [...old, ...new]) chứ KHÔNG replace/overwrite từng mảng riêng
 *   → tránh bị tách thành 2 cụm (cụm cũ / cụm realtime dạt đi chỗ khác).
 * - Dedup theo (sender + nội dung + thời gian gần nhau <5s) để echo/retry không nhân đôi.
 * - sort() lại toàn bộ theo timestamp ASC sau khi gộp → tin cũ trên, mới dưới.
 */
function mergeMessages(list, incoming) {
  const merged = [...list.value, ...incoming]
  const seen = new Map()
  for (const m of merged) {
    const key = `${m.sender_id || m.from || ''}:${m.message || m.content}`
    const t = new Date(m.created_at || m.timestamp || 0).getTime()
    const prev = seen.get(key)
    if (!prev) {
      seen.set(key, m)
    } else {
      const pt = new Date(prev.created_at || prev.timestamp || 0).getTime()
      if (Math.abs(t - pt) < 5000) {
        // trùng (echo/retry) → giữ bản ghi có đầy đủ id (bản từ DB) nếu có
        if (!prev.id && m.id) seen.set(key, m)
      } else {
        seen.set(key, m)
      }
    }
  }
  list.value = Array.from(seen.values()).sort((a, b) => {
    const ta = new Date(a.created_at || a.timestamp || 0).getTime()
    const tb = new Date(b.created_at || b.timestamp || 0).getTime()
    return ta - tb
  })
  return true
}

/**
 * Nhét 1 tin nhắn (realtime) vào mảng theo đúng thứ tự + chống trùng.
 */
function appendSortedMessage(list, msg) {
  return mergeMessages(list, [msg])
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  if (isToday) {
    return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
  }
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  if (d.toDateString() === yesterday.toDateString()) {
    return 'Hôm qua ' + d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
  }
  return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
}
</script>

<style scoped>
/* ===== Overlay & Panel ===== */
.chat-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  justify-content: center;
  align-items: center;
  background: rgba(27, 28, 28, 0.45);
  backdrop-filter: blur(2px);
}

.chat-panel {
  width: 880px;
  max-width: 95vw;
  height: 660px;
  max-height: 90vh;
  background: var(--ag-bg-card);
  border-radius: var(--ag-radius-2xl);
  display: flex;
  overflow: hidden;
  box-shadow: var(--ag-shadow-xl);
}

/* ===== Left Sidebar ===== */
.chat-sidebar {
  width: 300px;
  min-width: 300px;
  border-right: 1px solid var(--ag-border);
  display: flex;
  flex-direction: column;
  background: var(--ag-bg-card);
}

.chat-sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 16px 0;
}

.chat-sidebar-title {
  font-family: var(--ag-font-display);
  font-size: 20px;
  font-weight: 700;
  color: var(--ag-text-primary);
}

.chat-header-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ag-text-secondary);
  transition: background 0.15s, color 0.15s;
}

.chat-header-btn:hover {
  background: var(--ag-surface-container);
  color: var(--ag-text-primary);
}

/* Tabs */
.chat-tabs {
  display: flex;
  padding: 12px 16px 0;
  gap: 4px;
}

.chat-tab {
  flex: 1;
  padding: 8px 0;
  border: none;
  background: transparent;
  border-radius: var(--ag-radius-md);
  font-size: 13px;
  font-weight: 500;
  color: var(--ag-text-secondary);
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.chat-tab:hover {
  background: var(--ag-surface-container);
  color: var(--ag-text-primary);
}

.chat-tab--active {
  background: color-mix(in srgb, var(--ag-primary-500) 12%, transparent);
  color: var(--ag-primary-600);
  font-weight: 600;
}

/* New message button (messages tab) */
.chat-new-msg-wrap {
  padding: 12px 16px 0;
}

.chat-new-msg-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 9px 12px;
  border: none;
  border-radius: var(--ag-radius-lg);
  background: var(--ag-primary-500);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, transform 0.1s;
}

.chat-new-msg-btn:hover {
  background: var(--ag-primary-600);
}

.chat-new-msg-btn:active {
  transform: scale(0.98);
}

/* Search */
.chat-search-wrap {
  position: relative;
  padding: 12px 16px;
}

.chat-search-icon {
  position: absolute;
  left: 24px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 18px;
  color: var(--ag-text-muted);
  pointer-events: none;
}

.chat-search {
  width: 100%;
  padding: 8px 12px 8px 36px;
  border: 1px solid var(--ag-border);
  border-radius: var(--ag-radius-full);
  font-size: 13px;
  outline: none;
  background: var(--ag-surface-container-low);
  box-sizing: border-box;
  transition: background 0.15s, border-color 0.15s;
}

.chat-search:focus {
  background: var(--ag-bg-card);
  border-color: var(--ag-primary-500);
}

/* Create group button */
.chat-create-group-wrap {
  padding: 12px 16px;
}

.chat-create-group-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 12px;
  border: 1px dashed var(--ag-border);
  border-radius: var(--ag-radius-md);
  background: transparent;
  font-size: 13px;
  color: var(--ag-text-secondary);
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}

.chat-create-group-btn:hover {
  background: var(--ag-surface-container);
  border-color: var(--ag-primary-500);
  color: var(--ag-primary-600);
}

/* Sidebar body */
.chat-sidebar-body {
  flex: 1;
  overflow-y: auto;
}

.chat-loading {
  padding: 24px;
  text-align: center;
  color: var(--ag-text-muted);
  font-size: 13px;
}

.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 8px;
  color: var(--ag-text-muted);
  font-size: 13px;
  padding: 24px;
}

.chat-empty-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  padding: 8px 16px;
  border: none;
  border-radius: var(--ag-radius-full);
  background: var(--ag-primary-500);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}

.chat-empty-btn:hover {
  background: var(--ag-primary-600);
}

.chat-empty-state-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 20px;
  padding: 10px 22px;
  border: none;
  border-radius: var(--ag-radius-full);
  background: var(--ag-primary-500);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, transform 0.1s;
}

.chat-empty-state-btn:hover {
  background: var(--ag-primary-600);
}

.chat-empty-state-btn:active {
  transform: scale(0.97);
}

/* Conversation/Group items */
.chat-conv-list {
  padding: 4px 0;
}

.chat-conv-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.1s;
}

.chat-conv-item:hover {
  background: var(--ag-surface-container);
}

.chat-conv-item--active {
  background: color-mix(in srgb, var(--ag-primary-500) 12%, transparent);
}

.chat-avatar {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: var(--ag-primary-500);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 17px;
  flex-shrink: 0;
  position: relative;
}

.chat-online-dot {
  position: absolute;
  bottom: 1px;
  right: 1px;
  width: 13px;
  height: 13px;
  border-radius: 50%;
  background: #22c55e;
  border: 2px solid #fff;
  box-shadow: 0 0 0 1px rgba(34, 197, 94, 0.4);
}

.chat-avatar--sm .chat-online-dot {
  width: 12px;
  height: 12px;
  bottom: 0px;
  right: 0px;
}

.chat-avatar--sm {
  width: 38px;
  height: 38px;
  font-size: 14px;
}

.chat-avatar--group {
  background: var(--ag-secondary-500);
}

.chat-avatar-text {
  line-height: 1;
}

.chat-conv-info {
  flex: 1;
  min-width: 0;
}

.chat-conv-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 4px;
}

.chat-conv-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--ag-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-conv-time {
  font-size: 11px;
  color: var(--ag-text-muted);
  white-space: nowrap;
}

.chat-conv-bottom {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 2px;
}

.chat-conv-preview {
  font-size: 13px;
  color: var(--ag-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.chat-badge {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: var(--ag-danger);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.chat-badge-pending {
  font-size: 10px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 9999px;
  background: color-mix(in srgb, var(--ag-warning) 15%, transparent);
  color: var(--ag-warning);
  white-space: nowrap;
}
.chat-conv-item--pending {
  opacity: 0.6;
  pointer-events: none;
}

/* ===== Right Main Area ===== */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--ag-bg-card);
  min-width: 0;
}

.chat-main--empty {
  align-items: center;
  justify-content: center;
  background: var(--ag-bg-sand);
}

.chat-empty-state {
  text-align: center;
  color: var(--ag-text-muted);
  max-width: 320px;
}

.chat-empty-state h3 {
  font-family: var(--ag-font-display);
  font-size: 18px;
  font-weight: 600;
  color: var(--ag-text-primary);
  margin: 16px 0 4px;
}

.chat-empty-state p {
  font-size: 14px;
  color: var(--ag-text-secondary);
  line-height: 1.5;
}

.chat-empty-state-hint {
  margin-top: 16px;
  font-size: 13px;
  color: var(--ag-text-muted);
}

.chat-pending-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: var(--ag-text-muted);
  padding: 40px 24px;
}
.chat-pending-state h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--ag-warning);
  margin: 12px 0 4px;
}
.chat-pending-state p {
  font-size: 13px;
  color: var(--ag-text-muted);
  max-width: 240px;
}

/* Main Header */
.chat-main-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--ag-border);
  gap: 12px;
  flex-shrink: 0;
}

.chat-back-btn {
  display: none;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 50%;
  cursor: pointer;
  align-items: center;
  justify-content: center;
  color: var(--ag-text-secondary);
}

.chat-back-btn:hover {
  background: var(--ag-surface-container);
}

.chat-main-user {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.chat-main-user-info {
  min-width: 0;
}

.chat-main-user-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--ag-text-primary);
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-main-user-status {
  font-size: 12px;
  color: var(--ag-text-muted);
}

.chat-main-actions {
  display: flex;
  gap: 4px;
}

/* Bubbles */
.chat-msg-row {
  display: flex;
  margin-bottom: 4px;
}

.chat-msg-row--mine {
  justify-content: flex-end;
}

.chat-msg-row--theirs {
  justify-content: flex-start;
}

.chat-bubble {
  max-width: 70%;
  padding: 8px 14px;
  border-radius: 18px;
  background: var(--ag-bg-card);
  color: var(--ag-text-primary);
  font-size: 14px;
  line-height: 1.45;
  box-shadow: var(--ag-shadow-sm);
  word-wrap: break-word;
}

.chat-bubble--mine {
  background: var(--ag-primary-500);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.chat-msg-row--theirs .chat-bubble {
  border-bottom-left-radius: 4px;
}

.chat-bubble-sender {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--ag-primary-600);
  margin-bottom: 2px;
}

.chat-bubble-text {
  display: block;
  white-space: pre-wrap;
}

.chat-bubble-meta {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 3px;
  margin-top: 2px;
}

.chat-bubble-time {
  font-size: 11px;
  opacity: 0.75;
}

.chat-bubble--mine .chat-bubble-time {
  color: rgba(255, 255, 255, 0.85);
}

.chat-msg-row--theirs .chat-bubble-time {
  color: var(--ag-text-muted);
}

.chat-seen-icon {
  font-size: 14px;
  opacity: 0.8;
}

.chat-bubble--mine .chat-seen-icon {
  color: rgba(255, 255, 255, 0.85);
}

.chat-seen-icon--sending {
  animation: pulse 1s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 0.4; }
  50% { opacity: 1; }
}

/* Input */
.chat-input-wrap {
  padding: 8px 16px 12px;
  border-top: 1px solid var(--ag-border);
  flex-shrink: 0;
  background: var(--ag-bg-card);
}
.chat-input-target {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 4px 6px;
  color: var(--ag-text-muted);
  font-size: 12px;
}
.chat-input-target-text {
  font-weight: 600;
  color: var(--ag-primary-600);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-input-box {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--ag-surface-container-low);
  border: 1px solid var(--ag-border);
  border-radius: var(--ag-radius-full);
  padding: 4px 4px 4px 16px;
  transition: border-color 0.15s, background 0.15s;
}

.chat-input-box:focus-within {
  background: var(--ag-bg-card);
  border-color: var(--ag-primary-500);
}

.chat-input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 14px;
  outline: none;
  color: var(--ag-text-primary);
  min-height: 24px;
}

.chat-send-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: var(--ag-primary-500);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background 0.15s, transform 0.1s;
}

.chat-send-btn:hover {
  background: var(--ag-primary-600);
}

.chat-send-btn:active {
  transform: scale(0.92);
}

.chat-send-btn:disabled {
  background: var(--ag-surface-container);
  color: var(--ag-text-muted);
  cursor: not-allowed;
}

/* Product context chip in header */
.chat-context-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 180px;
  padding: 3px 10px;
  border-radius: var(--ag-radius-full);
  background: var(--ag-surface-container-low);
  border: 1px solid var(--ag-border);
  font-size: 11px;
  color: var(--ag-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Modal (create group / new message) */
.chat-modal {
  background: var(--ag-bg-card);
  border-radius: var(--ag-radius-2xl);
  width: 420px;
  max-width: 90vw;
  box-shadow: var(--ag-shadow-xl);
  overflow: hidden;
}

.chat-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--ag-border);
}

.chat-modal-title {
  font-family: var(--ag-font-display);
  font-size: 16px;
  font-weight: 600;
  color: var(--ag-text-primary);
}

.chat-modal-body {
  padding: 20px;
  max-height: 60vh;
  overflow-y: auto;
}

.chat-modal-input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid var(--ag-border);
  border-radius: var(--ag-radius-md);
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.15s;
}

.chat-modal-input:focus {
  border-color: var(--ag-primary-500);
}

.chat-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 20px;
  border-top: 1px solid var(--ag-border);
}

.chat-modal-cancel,
.chat-modal-confirm {
  padding: 8px 20px;
  border-radius: var(--ag-radius-md);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: background 0.15s;
}

.chat-modal-cancel {
  background: var(--ag-surface-container);
  color: var(--ag-text-secondary);
}

.chat-modal-cancel:hover {
  background: var(--ag-surface-container-high);
}

.chat-modal-confirm {
  background: var(--ag-primary-500);
  color: #fff;
}

.chat-modal-confirm:hover {
  background: var(--ag-primary-600);
}

.chat-modal-confirm:disabled {
  background: var(--ag-surface-container);
  color: var(--ag-text-muted);
  cursor: not-allowed;
}

/* User list in add-member modal */
.chat-user-list {
  margin-top: 12px;
  max-height: 240px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.chat-user-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: var(--ag-radius-md);
  cursor: pointer;
  transition: background 0.1s;
}

.chat-user-item:hover {
  background: var(--ag-surface-container);
}

.chat-avatar--xs {
  width: 32px;
  height: 32px;
  font-size: 13px;
}

.chat-user-name {
  font-size: 14px;
  color: var(--ag-text-primary);
}

/* Product picker for new message */
.chat-product-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: var(--ag-radius-md);
  cursor: pointer;
  transition: background 0.1s;
}

.chat-product-item:hover {
  background: var(--ag-surface-container);
}

.chat-product-thumb {
  width: 40px;
  height: 40px;
  border-radius: var(--ag-radius-md);
  object-fit: cover;
  background: var(--ag-surface-container);
  flex-shrink: 0;
}

.chat-product-info {
  flex: 1;
  min-width: 0;
}

.chat-product-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--ag-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-product-seller {
  font-size: 12px;
  color: var(--ag-text-muted);
}

/* Transitions */
.chat-panel-enter-active,
.chat-panel-leave-active {
  transition: opacity 0.2s ease;
}

.chat-panel-enter-active .chat-panel,
.chat-panel-leave-active .chat-panel {
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.chat-panel-enter-from,
.chat-panel-leave-to {
  opacity: 0;
}

.chat-panel-enter-from .chat-panel {
  transform: scale(0.95);
  opacity: 0;
}

.chat-panel-leave-to .chat-panel {
  transform: scale(0.95);
  opacity: 0;
}

/* Mobile */
@media (max-width: 768px) {
  .chat-overlay {
    align-items: stretch;
  }

  .chat-panel {
    width: 100vw;
    max-width: 100vw;
    height: 100vh;
    max-height: 100vh;
    border-radius: 0;
  }

  .chat-sidebar {
    width: 100%;
    min-width: 100%;
  }

  .chat-sidebar--hidden {
    display: none;
  }

  .chat-main {
    width: 100%;
  }

  .chat-back-btn {
    display: flex;
  }

  .chat-input-box {
    margin: 0 8px;
  }

  .chat-modal {
    width: 90vw;
    max-width: 90vw;
  }
}
</style>

import { ref } from 'vue';
import { getApiToken, setApiToken } from '../services/api.js';

const ws = ref(null);
const connected = ref(false);
const messages = ref([]);
const onlineUserIds = ref(new Set());
let handlers = {};
let pollingFreshToken = false;

export function useChatSocket() {
  let reconnectTimer = null;
  let heartbeatTimer = null;
  let connecting = false;
  let attempts = 0;
  let lastPong = 0;

  /**
   * Lấy token mới qua web session (route /agriverse/chat/ws-token) — KHÔNG cần token cũ.
   * Gọi khi WS bị chối vì token hết hạn / chưa có token → refresh rồi nối lại.
   */
  async function fetchFreshToken() {
    if (pollingFreshToken) return null;
    pollingFreshToken = true;
    try {
      const resp = await fetch('/agriverse/chat/ws-token', {
        headers: {
          'X-Requested-With': 'XMLHttpRequest',
          'X-CSRF-TOKEN': document.querySelector('meta[name="csrf-token"]')?.getAttribute('content') || '',
        },
      });
      if (!resp.ok) return null;
      const json = await resp.json();
      if (json.token) {
        setApiToken(json.token);
        return json.token;
      }
      return null;
    } catch (e) {
      console.warn('[ChatSocket] fetchFreshToken failed:', e);
      return null;
    } finally {
      pollingFreshToken = false;
    }
  }

  // Bắt đầu ping định kỳ để giữ kết nối sống (server cũng tự detect client còn sống)
  function startHeartbeat(socket) {
    stopHeartbeat();
    lastPong = Date.now();
    heartbeatTimer = setInterval(() => {
      if (socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({ type: 'ping', ts: Date.now() }));
        // Nếu quá lâu không có pong/hoạt động thì coi như chết → đóng để kích reconnect
        if (Date.now() - lastPong > 30000) {
          console.warn('[ChatSocket] No activity for 30s - forcing reconnect');
          socket.close(1001, 'reconnect');
        }
      }
    }, 15000);
  }

  function stopHeartbeat() {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer);
      heartbeatTimer = null;
    }
  }

  function scheduleReconnect() {
    if (reconnectTimer) return;
    // Backoff nhẹ nhưng bám sát: 1.2s → lên tối đa 5s để không spam khi server down
    const delay = Math.min(1200 * Math.pow(1.5, attempts), 5000);
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null;
      connect();
    }, delay);
  }

  function connect() {
    if (ws.value && (ws.value.readyState === WebSocket.OPEN || ws.value.readyState === WebSocket.CONNECTING)) return;
    if (connecting) return;
    connecting = true;

    let token = getApiToken();

    // Chưa có token (đang login/sync) → thử xin token mới; nếu vẫn không có, hẹn lại
    if (!token) {
      fetchFreshToken().then((fresh) => {
        connecting = false;
        if (fresh) {
          connect();
        } else {
          console.warn('[ChatSocket] No auth yet - will retry');
          scheduleReconnect();
        }
      });
      return;
    }

    try {
      const configured = document.querySelector('meta[name="ws-url"]')?.getAttribute('content') || '';
      let base = configured;
      if (!base) {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        base = `${protocol}//${window.location.host}`;
      }
      const socket = new WebSocket(`${base}/ws/chat/${token}`);
      const openToken = token;

      socket.onopen = () => {
        attempts = 0;
        connecting = false;
        connected.value = true;
        console.log('[ChatSocket] Connected to:', `${base}/ws/chat/ping`);
        if (reconnectTimer) {
          clearTimeout(reconnectTimer);
          reconnectTimer = null;
        }
        startHeartbeat(socket);
      };

      socket.onmessage = async (event) => {
        lastPong = Date.now();

        // Ping/pong từ JakartaEE → chỉ làm nhiệm vụ giữ sống, không render
        let raw = event.data;
        if (typeof raw === 'string' && raw.trim().startsWith('{')) {
          try {
            const obj = JSON.parse(raw);
            if (obj.type === 'pong' || obj.type === 'ping') return;
          } catch (e) { /* fallthrough */ }
        }

        let data = null;
        try {
          if (event.data instanceof Blob) {
            const buffer = await event.data.arrayBuffer();
            const bytes = new Uint8Array(buffer);
            let offset = 0;
            const decoder = new TextDecoder();

            function readVarint() {
              let low = 0, high = 0, shift = 0;
              while (offset < bytes.length) {
                const b = bytes[offset++];
                if (shift < 32) low |= (b & 0x7F) << shift;
                else high |= (b & 0x7F) << (shift - 32);
                if (!(b & 0x80)) break;
                shift += 7;
              }
              return high * 0x100000000 + low;
            }

            const msg = {};
            while (offset < bytes.length) {
              const tag = readVarint();
              const fieldNumber = tag >> 3;
              const wireType = tag & 0x07;
              if (wireType === 2) {
                const length = readVarint();
                const strBytes = bytes.subarray(offset, offset + length);
                offset += length;
                const str = decoder.decode(strBytes);
                if (fieldNumber === 1) msg.type = str;
                else if (fieldNumber === 2) msg.from = str;
                else if (fieldNumber === 3) msg.to = str;
                else if (fieldNumber === 4) msg.content = str;
              } else if (wireType === 0) {
                const val = readVarint();
                if (fieldNumber === 5) msg.timestamp = val;
              } else break;
            }

            if (msg.content) {
              try { data = JSON.parse(msg.content); }
              catch (e) { data = msg; }
            } else {
              data = msg;
            }
          } else if (typeof event.data === 'string') {
            try { data = JSON.parse(event.data); }
            catch (e) { data = event.data; }
          } else {
            data = event.data;
          }
        } catch (e) {
          console.error('[ChatSocket] Message parse error:', e);
        }

        if (!data) return;

        // Presence (danh sách user online) — không render lên tin nhắn, emit event riêng
        if (data.type === 'presence') {
          try {
            const parsed = typeof data.content === 'string' ? JSON.parse(data.content) : data.content;
            if (Array.isArray(parsed.online)) {
              onlineUserIds.value = new Set(parsed.online.map(String));
            } else if (data.online) {
              onlineUserIds.value = new Set(data.online.map(String));
            }
          } catch (e) {
            /* ignore malformed presence */
          }
          if (handlers.presence) {
            for (const handler of handlers.presence) handler(data);
          }
          return;
        }

        messages.value.push(data);
        if (handlers.message) {
          for (const handler of handlers.message) handler(data);
        }
      };

      socket.onclose = async (e) => {
        stopHeartbeat();
        connecting = false;
        connected.value = false;
        ws.value = null;
        attempts++;
        console.warn('[ChatSocket] Closed:', e.code, e.reason, e.code === 1008 ? '(token expired - refreshing)' : '');
        if (e.code === 1008 || e.code === 1006) {
          // 1008 = JWT hết hạn/bị chối; 1006 = mất mạng/server reset → xin token mới
          await fetchFreshToken();
        }
        scheduleReconnect();
      };

      socket.onerror = (e) => {
        connecting = false;
        connected.value = false;
      };

      ws.value = socket;
    } catch (e) {
      connecting = false;
      console.error('[ChatSocket] Connect error:', e);
      scheduleReconnect();
    }
  }

  function disconnect() {
    stopHeartbeat();
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }
    if (ws.value) {
      ws.value.close(1000, 'manual disconnect');
      ws.value = null;
      connected.value = false;
    }
    connecting = false;
    attempts = 0;
  }

  function send(data) {
    if (ws.value && ws.value.readyState === WebSocket.OPEN) {
      ws.value.send(typeof data === 'string' ? data : JSON.stringify(data));
    }
  }

  function on(event, handler) {
    if (!handlers[event]) handlers[event] = new Set();
    handlers[event].add(handler);
  }

  function off(event, handler) {
    if (handlers[event] && handler) handlers[event].delete(handler);
  }

  return {
    ws,
    connected,
    messages,
    onlineUserIds,
    connect,
    disconnect,
    send,
    on,
    off,
  };
}

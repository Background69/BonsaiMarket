import {ref, onBeforeUnmount, reactive} from 'vue';
import {getApiToken} from '../services/api.js';
import {encodeMarketMessage, decodeMarketMessage, parsePayload} from './marketProtobuf.js';

/**
 * useMarketSocket — kết nối WebSocket realtime tới /ws/market/{token}.
 * Hỗ trợ giữ nguyên đường dẫn /ws/market như mô hình cũ (giống /ws/chat),
 * dùng Protobuf binary + Redis Pub/Sub để phân tán đa instance.
 */
export function useMarketSocket() {
    const ws = ref(null);
    const connected = ref(false);

    // Dữ liệu realtime có cấu trúc theo eventType
    const deals = ref([]);          // flash deals đang hoạt động
    const offers = reactive({in: [], out: []}); // đề xuất giá (in = đến mình, out = mình gửi)
    const pendingApprovals = ref([]); // đề xuất cần admin duyệt (ACCEPTED -> NEED_APPROVAL)
    const events = ref([]);         // log sự kiện (debug/toast)

    let handlers = {};
    let reconnectTimer = null;

    function connect() {
        if (ws.value && (ws.value.readyState === WebSocket.OPEN || ws.value.readyState === WebSocket.CONNECTING)) return;

        let token = getApiToken();
        if (!token) {
            fetchFreshToken().then((fresh) => {
                if (fresh) connect(); else scheduleReconnect();
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
            const socket = new WebSocket(`${base}/ws/market/${token}`);

            socket.onopen = () => {
                connected.value = true;
                if (reconnectTimer) {
                    clearTimeout(reconnectTimer);
                    reconnectTimer = null;
                }
            };

            socket.onmessage = async (event) => {
                let buffer;
                if (event.data instanceof Blob) {
                    buffer = await event.data.arrayBuffer();
                } else if (event.data instanceof ArrayBuffer) {
                    buffer = event.data;
                } else {
                    // Text (JSON) fallback
                    try {
                        const msg = JSON.parse(event.data);
                        applyMessage(msg);
                    } catch { /* ignore */
                    }
                    return;
                }
                const msg = decodeMarketMessage(new Uint8Array(buffer));
                applyMessage(msg);
            };

            socket.onclose = async (e) => {
                connected.value = false;
                ws.value = null;
                if (e.code === 1008 || e.code === 1006) {
                    await fetchFreshToken();
                }
                scheduleReconnect();
            };
            socket.onerror = () => {
                connected.value = false;
            };
            ws.value = socket;
        } catch {
            scheduleReconnect();
        }
    }

    async function fetchFreshToken() {
        try {
            const resp = await fetch('/chat/ws-token', {headers: {'X-Requested-With': 'XMLHttpRequest'}});
            if (resp.ok) {
                const data = await resp.json();
                if (data.token) localStorage.setItem('token', data.token);
                return data.token || null;
            }
        } catch { /* ignore */
        }
        return null;
    }

    function scheduleReconnect() {
        if (!reconnectTimer) {
            reconnectTimer = setTimeout(() => {
                reconnectTimer = null;
                connect();
            }, 5000);
        }
    }

    function disconnect() {
        if (reconnectTimer) {
            clearTimeout(reconnectTimer);
            reconnectTimer = null;
        }
        if (ws.value) {
            ws.value.close();
            ws.value = null;
            connected.value = false;
        }
    }

    function send(msg) {
        if (ws.value && ws.value.readyState === WebSocket.OPEN) {
            ws.value.send(encodeMarketMessage(msg).buffer);
        }
    }

    // ---- apply sự kiện từ server vào state ----
    function applyMessage(msg) {
        const payload = parsePayload(msg);

        if (msg.eventType === 'FLASH_DEAL') {
            if (msg.action === 'CREATE' || msg.action === 'CREATED') {
                // pener chỉ giữ deal duy nhất theo dealId
                upsertDeal(payload);
            } else if (msg.action === 'CLAIMED' || msg.action === 'CLAIM_OK') {
                const idx = deals.value.findIndex(d => d.dealId === payload.dealId);
                if (idx >= 0) {
                    deals.value[idx] = {...deals.value[idx], ...payload};
                }
            } else if (msg.action === 'CLAIM_FAILED') {
                pushEvent('flash_deal', 'Đã bị khóa', payload.message);
            }
        } else if (msg.eventType === 'OFFER') {
            handleOffer(msg, payload);
        } else if (msg.eventType === 'CHAT') {
            pushEvent('chat', `${msg.fromName || msg.from}: ${payload.content || ''}`);
        }
    }

    function upsertDeal(deal) {
        if (!deal || !deal.dealId) return;
        const idx = deals.value.findIndex(d => d.dealId === deal.dealId);
        if (idx >= 0) deals.value[idx] = {...deals.value[idx], ...deal};
        else deals.value.unshift(deal);
    }

    function handleOffer(msg, payload) {
        const myId = String(currentUserId());
        const isMine = payload.buyerId && String(payload.buyerId) === myId;
        const isIncoming = payload.sellerId && String(payload.sellerId) === myId;
        const affectsMe = isMine || isIncoming;

        if (msg.action === 'PROPOSE' || msg.action === 'REJECTED' || msg.action === 'COUNTERED'
            || msg.action === 'ACCEPTED' || msg.action === 'APPROVED' || msg.action === 'DECLINED'
            || msg.action === 'PROPOSED') {
            upsertOffer(payload, isMine, isIncoming);
        }

        const productName = payload.productName || 'đề xuất';
        const offerToast = {
            REJECTED: `Người bán đã từ chối đề xuất “${productName}”`,
            COUNTERED: `Người bán trả giá lại cho “${productName}”`,
            ACCEPTED: `Đề xuất “${productName}” đã được đồng ý`,
            APPROVED: `Đề xuất “${productName}” đã được admin duyệt`,
            DECLINED: `Đề xuất “${productName}” bị admin từ chối`,
        };
        if (affectsMe && offerToast[msg.action]) {
            pushEvent('offer', offerToast[msg.action]);
        }

        // Admin: đơn đã được seller/buyer ACCEPT cần duyệt
        if (msg.action === 'NEED_APPROVAL') {
            if (!pendingApprovals.value.find(a => a.offerId === payload.offerId)) {
                pendingApprovals.value.push(payload);
            }
            pushEvent('offer', `Có đề xuất “${productName}” chờ bạn duyệt`);
        }
        // Sau khi admin duyệt -> bỏ khỏi danh sách pending
        if (msg.action === 'APPROVED' || msg.action === 'DECLINED') {
            pendingApprovals.value = pendingApprovals.value.filter(a => a.offerId !== payload.offerId);
        }
    }

    function upsertOffer(offer, isMine, isIncoming) {
        const offerId = offer.offerId;
        if (!offerId) return;
        const merge = (list) => {
            const idx = list.findIndex(o => o.offerId === offerId);
            if (idx >= 0) list[idx] = {...list[idx], ...offer};
            else list.unshift(offer);
        };
        if (isIncoming) merge(offers.in);
        if (isMine) merge(offers.out);
    }

    function pushEvent(kind, text) {
        events.value.unshift({kind, text, at: Date.now()});
        if (events.value.length > 100) events.value.splice(100);
        emit('event', {kind, text});
    }

    function currentUserId() {
        // Lấy từ Inertia props nếu có
        if (window.__agri_user_id__) return window.__agri_user_id__;
        return getApiToken() ? '' : '';
    }

    const handlersSet = {};

    function emit(event, ...args) {
        (handlersSet[event] || []).forEach(h => h(...args));
    }

    function on(event, handler) {
        (handlersSet[event] = handlersSet[event] || []).push(handler);
    }

    function off(event, handler) {
        if (handlersSet[event]) handlersSet[event] = handlersSet[event].filter(h => h !== handler);
    }

    return {
        ws, connected, deals, offers, pendingApprovals, events,
        connect, disconnect, send, on, off,
        // helper trả state realtime đã được set userId từ trang
        setUserId: (id) => {
            window.__agri_user_id__ = id;
        },
    };
}

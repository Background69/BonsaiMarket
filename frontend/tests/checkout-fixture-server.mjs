// UI-only fixture on loopback. No real accounts, orders, payments or backend connections.
// Run `npm run build`, then `node tests/checkout-fixture-server.mjs` from frontend/.
import http from 'node:http';
import {readFile} from 'node:fs/promises';
import {resolve, extname, sep} from 'node:path';
import {fileURLToPath} from 'node:url';
const root = fileURLToPath(new URL('../dist/', import.meta.url));
const address = {id: 1, recipientName: 'Khách thử nghiệm', phone: '0901234567', provinceName: 'TP Hồ Chí Minh',
    districtName: '', wardName: 'Bến Thành', addressDetail: '12 Đường Số 1', isDefault: true};
const product = {id: 1, name: 'Tùng La Hán', price: 100000, stock: 10, status: 'ACTIVE', imageUrl: null, store: {id: 1, name: 'Vườn Xanh'}, category: {id: 1, name: 'Bonsai'}};
let quote, receipt, attempts = 0;
const server = http.createServer(async (req, res) => {
    const url = new URL(req.url, 'http://127.0.0.1');
    const reply = (body, status = 200) => {res.writeHead(status, {'Content-Type': 'application/json', 'Cache-Control': 'no-store'}); res.end(JSON.stringify(body));};
    try {
        if (url.pathname.startsWith('/api/')) {
            if (url.pathname === '/api/auth/me') return reply({id: 999, name: 'Khách thử nghiệm', email: 'fixture@example.com', role: 'customer'});
            if (url.pathname === '/api/auth/providers') return reply({google: false, facebook: false});
            if (url.pathname === '/api/auth/csrf') return reply({headerName: 'X-CSRF-TOKEN', token: 'fixture'});
            if (url.pathname === '/api/account/checkout') return reply({items: [], addresses: [address], shippingFeePerStore: 30000});
            if (url.pathname === '/api/products') return reply({content: [product], totalElements: 1, totalPages: 1, page: 0, size: 12});
            if (url.pathname === '/api/products/1') return reply(product);
            if (url.pathname === '/api/account/addresses') return reply([address]);
            if (url.pathname === '/api/account/checkout/quote' && req.method === 'POST') {
                let body = ''; for await (const chunk of req) body += chunk;
                const request = JSON.parse(body);
                if (request.quantity > 10) return reply({code: 'OUT_OF_STOCK', message: 'Sản phẩm không đủ tồn kho.'}, 409);
                const subtotal = 100000 * request.quantity;
                const total = subtotal + 30000;
                quote = {quoteId: '00000000-0000-4000-8000-000000000001', expiresAt: new Date(Date.now() + 900000).toISOString(),
                    review: {address, items: [{productId: 1, name: product.name, storeId: 1, storeName: 'Vườn Xanh', unitPrice: 100000,
                        quantity: request.quantity, lineTotal: subtotal}], stores: [{storeId: 1, storeName: 'Vườn Xanh', subtotal, shippingFee: 30000, total}],
                        subtotal, shippingFee: 30000, total, notes: request.notes}};
                return reply(quote);
            }
            if (url.pathname === '/api/account/checkout/orders' && req.method === 'POST') {
                if (!quote) return reply({message: 'Missing fixture quote'}, 400);
                receipt ??= {quoteId: quote.quoteId, orders: [{id: 1, code: 'BSM-UI-TEST', storeName: 'Vườn Xanh', status: 'PENDING', total: quote.review.total}], total: quote.review.total};
                // Optional manual failure scenario: CHECKOUT_FIXTURE_LOST_RESPONSE=1 returns one unknown result after commit.
                if (process.env.CHECKOUT_FIXTURE_LOST_RESPONSE === '1' && attempts++ === 0)
                    return reply({message: 'Mất phản hồi thử nghiệm; thử lại cùng yêu cầu.'}, 503);
                return reply(receipt);
            }
            if (url.pathname.startsWith('/api/account/checkout/quotes/') && quote) return reply(quote);
            if (url.pathname.startsWith('/api/account/checkout/receipts/') && receipt) return reply(receipt);
            return reply({code: 'NOT_FOUND', message: 'Không tìm thấy dữ liệu thử nghiệm.'}, 404);
        }
        const relative = decodeURIComponent(url.pathname).replace(/^\/+/, '');
        const path = resolve(root, relative || 'index.html');
        if (!path.startsWith(resolve(root) + sep)) return reply({message: 'Forbidden'}, 403);
        const ext = extname(path);
        const content = await readFile(ext ? path : resolve(root, 'index.html'));
        res.writeHead(200, {'Content-Type': {'.js': 'text/javascript', '.css': 'text/css', '.svg': 'image/svg+xml', '.png': 'image/png'}[ext] || 'text/html'});
        res.end(content);
    } catch {reply({message: 'Fixture error'}, 500);}
});
server.listen(5177, '127.0.0.1', () => console.log('Checkout UI fixture: http://127.0.0.1:5177/checkout'));

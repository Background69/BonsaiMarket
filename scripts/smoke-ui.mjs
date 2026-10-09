import {spawn} from 'node:child_process';
import {mkdtemp, readFile, writeFile} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import {join} from 'node:path';

const chrome = process.env.CHROME_PATH || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const profile = await mkdtemp(join(tmpdir(), 'bonsai-ui-'));
const browser = spawn(chrome, [
    '--headless=new', '--disable-gpu', '--no-first-run', '--no-default-browser-check',
    '--disable-background-networking', '--remote-debugging-port=0',
    `--user-data-dir=${profile}`, 'about:blank',
], {windowsHide: true, stdio: 'ignore'});

const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));
let ws;
try {
    let port;
    for (let i = 0; i < 100; i++) {
        try {
            port = (await readFile(join(profile, 'DevToolsActivePort'), 'utf8')).split('\n')[0];
            break;
        } catch {
            await sleep(100);
        }
    }
    if (!port) throw new Error('Chrome DevTools did not start');
    const tabs = await (await fetch(`http://127.0.0.1:${port}/json/list`)).json();
    ws = new WebSocket(tabs.find(tab => tab.type === 'page').webSocketDebuggerUrl);
    await new Promise((resolve, reject) => {
        ws.addEventListener('open', resolve, {once: true});
        ws.addEventListener('error', reject, {once: true});
    });
    let nextId = 0;
    const pending = new Map();
    let exceptions = [];
    const fixtureMode = process.argv.includes('--fixture');
    const category = {id: 1, name: 'Bonsai', slug: 'bonsai', description: 'Cây bonsai'};
    const store = {
        id: 1,
        name: 'Vườn Bonsai',
        description: 'Vườn cây mẫu',
        logoUrl: null,
        phone: null,
        address: null,
        status: 'ACTIVE'
    };
    const products = [
        {
            id: 1,
            name: 'Tùng bonsai',
            slug: 'tung-bonsai',
            description: 'Cây tùng',
            price: 250000,
            comparePrice: 300000,
            stock: 5,
            imageUrl: null,
            status: 'ACTIVE',
            category,
            store
        },
        {
            id: 2,
            name: 'Mai chiếu thủy',
            slug: 'mai-chieu-thuy',
            description: 'Cây mai',
            price: 180000,
            comparePrice: null,
            stock: 0,
            imageUrl: '/storage/missing.jpg',
            status: 'ACTIVE',
            category: null,
            store: null
        },
    ];
    ws.addEventListener('message', event => {
        const message = JSON.parse(event.data);
        if (message.method === 'Runtime.exceptionThrown') exceptions.push(message.params.exceptionDetails.exception?.description || message.params.exceptionDetails.text);
        if (fixtureMode && message.method === 'Fetch.requestPaused') {
            const path = new URL(message.params.request.url).pathname;
            const data = path === '/api/categories' ? [category]
                : path === '/api/products' ? products
                    : path === '/api/products/1' ? products[0]
                        : path === '/api/stores' ? [store]
                            : path === '/api/stores/1' ? store : {error: 'Not found'};
            send('Fetch.fulfillRequest', {
                requestId: message.params.requestId,
                responseCode: path.includes('999999') ? 404 : 200,
                responseHeaders: [{name: 'Content-Type', value: 'application/json'}],
                body: Buffer.from(JSON.stringify(data)).toString('base64'),
            }).catch(error => console.error(error));
        }
        if (!message.id) return;
        const item = pending.get(message.id);
        if (!item) return;
        pending.delete(message.id);
        message.error ? item.reject(new Error(message.error.message)) : item.resolve(message.result);
    });

    function send(method, params = {}) {
        return new Promise((resolve, reject) => {
            const id = ++nextId;
            pending.set(id, {resolve, reject});
            ws.send(JSON.stringify({id, method, params}));
        });
    }

    await send('Page.enable');
    await send('Runtime.enable');
    if (fixtureMode) await send('Fetch.enable', {patterns: [{urlPattern: '*://127.0.0.1:5173/api/*'}]});
    const cases = [
        ['/', 390], ['/products', 390], ['/products/1', 390], ['/products/999999', 390],
        ['/categories', 390], ['/stores', 390], ['/stores/1', 390], ['/cart', 390],
        ['/checkout', 390], ['/seller', 390], ['/seller/products', 390],
        ['/admin', 390], ['/admin/products', 390],
        ['/', 768], ['/products', 768], ['/products/1', 768], ['/categories', 768],
        ['/stores', 768], ['/cart', 768], ['/checkout', 768],
        ['/', 1024], ['/products', 1024], ['/products/1', 1024], ['/categories', 1024], ['/stores', 1024], ['/cart', 1024], ['/checkout', 1024],
        ['/', 1440], ['/products', 1440], ['/products/1', 1440], ['/categories', 1440], ['/stores', 1440], ['/cart', 1440], ['/checkout', 1440],
    ];
    const selected = fixtureMode ? cases.filter(([path, width]) => (width === 390 && ['/', '/products', '/products/1', '/products/999999', '/categories', '/stores', '/stores/1', '/admin/products', '/seller/products'].includes(path)) || (width === 1440 && ['/', '/products', '/products/1', '/categories', '/stores'].includes(path)))
        : process.argv.includes('--once') ? cases.slice(0, 1)
            : process.argv.includes('--focus') ? cases.filter(([path, width]) => (width === 390 || width === 768) && ['/', '/seller/products', '/products'].includes(path))
                : cases;
    const pathArg = process.argv.find(arg => arg.startsWith('--path='))?.slice(7);
    const widthArg = Number(process.argv.find(arg => arg.startsWith('--width='))?.slice(8));
    for (const [path, width] of selected.filter(([casePath, caseWidth]) => (!pathArg || casePath === pathArg) && (!widthArg || caseWidth === widthArg))) {
        exceptions = [];
        await send('Emulation.setDeviceMetricsOverride', {width, height: 900, deviceScaleFactor: 1, mobile: false});
        await send('Page.navigate', {url: `http://127.0.0.1:5173${path}`});
        await sleep(1500);
        const result = await send('Runtime.evaluate', {
            expression: `(() => ({ href: location.href, ready: document.readyState, bodyLength: document.body?.innerHTML.length, title: document.title, mounted: !!document.querySelector('#app')?.firstElementChild, width: document.documentElement.clientWidth, scrollWidth: document.documentElement.scrollWidth, error: !!document.querySelector('.integration-error'), loading: !!document.querySelector('.integration-loading'), feature: !!document.querySelector('.feature-page'), catalogText: document.body.innerText.includes('Tùng bonsai'), brokenImages: [...document.images].filter(img => img.complete && img.naturalWidth === 0).map(img => img.getAttribute('src')), links: [...document.querySelectorAll('a[href]')].some(a => /(?:localhost:8000|\\.php(?:[?#]|$)|\\/resources\\/|\\/storage\\/)/i.test(a.getAttribute('href'))), offenders: [...document.querySelectorAll('body *')].filter(e => { const r=e.getBoundingClientRect(); return r.right > document.documentElement.clientWidth + .5 && r.width && getComputedStyle(e).position !== 'fixed'; }).slice(0, 5).map(e => e.tagName.toLowerCase() + '.' + String(e.className).replace(/\\s+/g,'.').slice(0,70)) }))()`,
            returnByValue: true,
        });
        console.log(JSON.stringify({path, requestedWidth: width, ...result.result.value, exceptions}));
        if ((process.argv.includes('--focus') && width === 390) || (fixtureMode && path === '/products/1' && (width === 390 || width === 1440))) {
            const shot = await send('Page.captureScreenshot', {format: 'png', captureBeyondViewport: false});
            await writeFile(join(tmpdir(), `bonsai-cdp-${path === '/' ? 'home' : path.slice(1).replaceAll('/', '-')}-${width}.png`), Buffer.from(shot.data, 'base64'));
        }
    }
} finally {
    ws?.close();
    browser.kill();
}

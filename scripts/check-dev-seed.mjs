import {readFileSync} from 'node:fs'

const sql = readFileSync(new URL('../src/main/resources/db/dev/V9__seed_development_data.sql', import.meta.url), 'utf8')
const block = (table) => {
    const match = sql.match(new RegExp(`INSERT INTO ${table} \\([^;]+? VALUES\\s*([\\s\\S]*?);`))
    if (!match) throw new Error(`Missing ${table} seed`)
    return match[1].split('\n').filter((line) => line.trim().startsWith('('))
}

const orders = new Map()
for (const row of block('orders')) {
    const id = Number(row.match(/^\((\d+),/)[1])
    const amounts = row.match(/, (\d+)\.00, (\d+)\.00, (\d+)\.00, '([A-Z]+)', DATE_SUB/)
    if (!amounts) throw new Error(`Cannot parse order ${id}`)
    const [, fee, subtotal, total, status] = amounts
    if (Number(total) !== Number(subtotal) + Number(fee)) throw new Error(`Bad total: order ${id}`)
    orders.set(id, {subtotal: Number(subtotal), status, sum: 0, count: 0})
}

for (const row of block('order_items')) {
    const match = row.match(/^\((\d+), (\d+), '([^']+)', (\d+)\.00, (\d+), (\d+)\.00\)/)
    if (!match) throw new Error(`Cannot parse item: ${row}`)
    const [, orderId, productId, name, unitPrice, quantity, lineTotal] = match
    const order = orders.get(Number(orderId))
    if (!order || Number(productId) < 1 || Number(productId) > 20 || !name) throw new Error(`Bad item FK/snapshot: ${row}`)
    if (Number(lineTotal) !== Number(unitPrice) * Number(quantity)) throw new Error(`Bad item total: ${row}`)
    order.sum += Number(lineTotal)
    order.count++
}

for (const [id, order] of orders) {
    if (order.sum !== order.subtotal || order.count < 1 || order.count > 4) throw new Error(`Bad subtotal/items: order ${id}`)
}

if (orders.size < 50 || orders.size > 100 || block('products').length !== 20) throw new Error('Seed volume mismatch')
const statusCounts = Object.groupBy([...orders.values()], (order) => order.status)
console.log(JSON.stringify({
    products: block('products').length, orders: orders.size,
    orderItems: [...orders.values()].reduce((sum, order) => sum + order.count, 0),
    statuses: Object.fromEntries(Object.entries(statusCounts).map(([status, values]) => [status, values.length]))
}))

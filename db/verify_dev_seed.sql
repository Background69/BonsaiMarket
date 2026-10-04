-- Run against a database migrated with the dev profile.
SELECT 'users' AS table_name, COUNT(*) AS row_count FROM users
UNION ALL SELECT 'addresses', COUNT(*) FROM addresses
UNION ALL SELECT 'categories', COUNT(*) FROM categories
UNION ALL SELECT 'stores', COUNT(*) FROM stores
UNION ALL SELECT 'products', COUNT(*) FROM products
UNION ALL SELECT 'carts', COUNT(*) FROM carts
UNION ALL SELECT 'cart_items', COUNT(*) FROM cart_items
UNION ALL SELECT 'orders', COUNT(*) FROM orders
UNION ALL SELECT 'order_items', COUNT(*) FROM order_items
UNION ALL SELECT 'reviews', COUNT(*) FROM reviews
UNION ALL SELECT 'wishlist_items', COUNT(*) FROM wishlist_items;

SELECT o.code, o.subtotal, SUM(oi.line_total) AS item_sum, o.shipping_fee, o.total,
       (o.subtotal = SUM(oi.line_total) AND o.total = o.subtotal + o.shipping_fee) AS amounts_valid
FROM orders o JOIN order_items oi ON oi.order_id = o.id
GROUP BY o.id, o.code, o.subtotal, o.shipping_fee, o.total
ORDER BY o.id LIMIT 10;

SELECT COUNT(*) AS invalid_order_count FROM (
    SELECT o.id FROM orders o JOIN order_items oi ON oi.order_id = o.id
    GROUP BY o.id, o.subtotal, o.shipping_fee, o.total
    HAVING o.subtotal <> SUM(oi.line_total) OR o.total <> o.subtotal + o.shipping_fee
) invalid_orders;

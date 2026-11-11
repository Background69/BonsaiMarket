package com.bonsaimarket.backend.catalog;

import com.bonsaimarket.backend.product.Product;
import jakarta.persistence.criteria.*;

/** Delivered/completed item units, conservatively excluding any return or non-denied refund workflow. */
final class ProductSales {
    static Expression<Long> quantity(Root<Product> product,CriteriaQuery<?> query,CriteriaBuilder cb){
        return ((org.hibernate.query.criteria.HibernateCriteriaBuilder)cb).sql("coalesce((select sum(oi.quantity) from order_items oi join orders so on so.id=oi.order_id where oi.product_id=? and so.status in ('DELIVERED','COMPLETED') and not exists(select 1 from order_returns sr where sr.order_id=so.id) and not exists(select 1 from order_refunds sf where sf.order_id=so.id and sf.status<>'DENIED')),0)",Long.class,product.get("id"));
    }
}

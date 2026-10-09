package com.bonsaimarket.backend.ai;

import com.bonsaimarket.backend.product.*;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

/** Validate JPQL against actual entity mappings with MySQL dialect, without a JDBC connection. */
class CatalogQueryTest {
    @Test void boundedCatalogQueryIsValidHibernateHql() throws Exception {
        var configuration = new Configuration();
        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        configuration.setProperty("hibernate.boot.allow_jdbc_metadata_access", "false");
        configuration.setProperty("hibernate.hbm2ddl.auto", "none");
        configuration.setProperty("hibernate.connection.provider_class", "org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl");
        for (String name : new String[]{"category.Category", "store.Store", "product.Product", "user.User", "address.Address",
                "cart.Cart", "cart.CartItem", "order.Order", "order.OrderItem", "review.Review", "wishlist.WishlistItem"}) {
            configuration.addAnnotatedClass(Class.forName("com.bonsaimarket.backend." + name));
        }
        String hql = ProductRepository.class.getMethod("findAiPublicCatalog", String.class, BigDecimal.class,
                BigDecimal.class, boolean.class, Long.class, Pageable.class).getAnnotation(Query.class).value();
        try (var factory = configuration.buildSessionFactory()) {
            var sessionFactory = factory.unwrap(SessionFactoryImplementor.class);
            assertNotNull(sessionFactory.getQueryEngine().getHqlTranslator().translate(hql, Product.class));
        }
    }
}

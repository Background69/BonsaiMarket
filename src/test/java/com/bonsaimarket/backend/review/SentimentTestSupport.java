package com.bonsaimarket.backend.review;

import com.bonsaimarket.backend.auth.*;
import com.bonsaimarket.backend.category.*;
import com.bonsaimarket.backend.order.*;
import com.bonsaimarket.backend.order.Order;
import com.bonsaimarket.backend.product.*;
import com.bonsaimarket.backend.store.*;
import com.bonsaimarket.backend.user.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.profiles.active=sentiment-test",
    "spring.datasource.url=jdbc:h2:mem:sentiment;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
    "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
    "spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=create-drop",
    "sentiment.enabled=true","sentiment.allow-demo=true","sentiment.internal-token=isolated-sentiment-test-token-32-chars",
    "sentiment.poll-ms=3600000"})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class SentimentTestSupport {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired OrderRepository orders;
    @Autowired OrderItemRepository items;
    @Autowired ReviewSentiments worker;
    @MockitoBean AuthMailSender mail;
    User seller,admin,buyer,foreign;
    Product product;
    Order order;
    OrderItem item;

    @BeforeAll void schema() throws Exception {
        try(var c=db.getDataSource().getConnection()) {
            c.createStatement().execute("CREATE ALIAS IF NOT EXISTS SHA2 FOR 'com.bonsaimarket.backend.review.SentimentTestSupport.sha2'");
            c.createStatement().execute("DROP TABLE IF EXISTS review_sentiment");
            ScriptUtils.executeSqlScript(c,new FileSystemResource("src/main/resources/db/migration/V10__review_sentiment.sql"));
        }
    }
    public static String sha2(String text,int bits) { return ReviewSentiments.version(text); }
    @BeforeEach void fixture() {
        db.update("delete from review_sentiment");
        seller=user(UserRole.SELLER);admin=user(UserRole.ADMIN);buyer=user(UserRole.CUSTOMER);foreign=user(UserRole.SELLER);
        var now=LocalDateTime.now();
        var store=new Store();store.setOwner(seller);store.setName("Vườn QA Sentiment");store.setStatus(StoreStatus.ACTIVE);store.setCreatedAt(now);store.setUpdatedAt(now);store=stores.save(store);
        var category=new Category();category.setName("Bonsai");category.setSlug(UUID.randomUUID().toString());category.setIsActive(true);category.setSortOrder(0);category.setCreatedAt(now);category.setUpdatedAt(now);category=categories.save(category);
        product=new Product();product.setName("Mai chiếu thủy QA");product.setSlug(UUID.randomUUID().toString());product.setStore(store);product.setCategory(category);product.setPrice(BigDecimal.TEN);product.setStock(10);product.setStatus(ProductStatus.ACTIVE);product.setCreatedAt(now);product.setUpdatedAt(now);product=products.save(product);
        order=new Order();order.setCode(UUID.randomUUID().toString());order.setStore(store);order.setBuyer(buyer);order.setStatus(OrderStatus.COMPLETED);order.setCreatedAt(LocalDateTime.now());order.setUpdatedAt(order.getCreatedAt());order.setSubtotal(BigDecimal.TEN);order.setShippingFee(BigDecimal.ZERO);order.setTotal(BigDecimal.TEN);order.setShippingAddressSnapshot("QA only");order.setShippingMethod("STANDARD");order=orders.save(order);
        item=line();
    }
    User user(UserRole role) {var u=new User();u.setName("QA "+role);u.setEmail(UUID.randomUUID()+"@example.invalid");u.setPasswordHash("!");u.setIsActive(true);u.setRole(role);u.setEmailVerifiedAt(LocalDateTime.now());u.setCreatedAt(LocalDateTime.now());u.setUpdatedAt(u.getCreatedAt());return users.save(u);}
    OrderItem line() {var i=new OrderItem();i.setOrder(order);i.setProduct(product);i.setQuantity(1);i.setProductNameSnapshot(product.getName());i.setUnitPrice(BigDecimal.TEN);i.setLineTotal(BigDecimal.TEN);return items.save(i);}
    RequestPostProcessor as(User user) {return authentication(UsernamePasswordAuthenticationToken.authenticated(AuthPrincipal.of(user),null,List.of(new SimpleGrantedAuthority("ROLE_"+user.getRole()))));}
    long submit(String text) throws Exception {
        String body=new tools.jackson.databind.ObjectMapper().writeValueAsString(Map.of("orderItemId",item.getId(),"rating",1,"comment",text));
        mvc.perform(post("/api/account/products/"+product.getId()+"/reviews").with(as(buyer)).with(csrf()).contentType("application/json").content(body)).andExpect(status().isCreated());
        return db.queryForObject("select id from reviews where order_item_id=?",Long.class,item.getId());
    }
}

package com.bonsaimarket.backend.community;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import com.bonsaimarket.backend.category.*;
import com.bonsaimarket.backend.product.*;
import com.bonsaimarket.backend.store.*;
import com.bonsaimarket.backend.user.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Bằng chứng contract H2/MockMvc; chưa chứng minh tương đương MySQL/Flyway. */
@SpringBootTest(properties={"spring.profiles.active=discussion-test","spring.datasource.url=jdbc:h2:mem:discussion;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
    "spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureMockMvc @TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DiscoveryCommunityIntegrationTest {
    @Autowired MockMvc mvc;@Autowired JdbcTemplate db;@Autowired UserRepository users;@Autowired StoreRepository stores;@Autowired ProductRepository products;@Autowired CategoryRepository categories;
    @Autowired jakarta.persistence.EntityManagerFactory factory;
    User seller,other,buyer;Store shop,otherShop;Category category,alternate;Product product;
    @BeforeAll void schema()throws Exception {
        try(var c=db.getDataSource().getConnection()){
            com.bonsaimarket.backend.support.BaselineSql.execute(c,com.bonsaimarket.backend.support.BaselineSql.tables("product_questions","community_posts","community_comments"));
        }
    }
    @BeforeEach void setup(){
        for(String table:List.of("community_comments","community_posts","product_questions","products","stores","categories","users"))db.update("delete from "+table);
        seller=user(UserRole.SELLER,"Người bán");other=user(UserRole.SELLER,"Vườn khác");buyer=user(UserRole.CUSTOMER,"Người hỏi");
        shop=shop(seller,"Vườn Hiên");shop.setAddress("Đà Nẵng");shop.setDescription("Góc cây bên hiên");stores.save(shop);otherShop=shop(other,"Mộc Viên");
        category=category("Sen đá");alternate=category("Bonsai");product=product(shop,category,100);
    }
    User user(UserRole role,String name){var u=new User();u.setName(name);u.setEmail(UUID.randomUUID()+"@example.invalid");u.setPasswordHash("!");u.setRole(role);u.setIsActive(true);u.setEmailVerifiedAt(LocalDateTime.now());return users.save(u);}
    Store shop(User u,String name){var s=new Store();s.setName(name);s.setStatus(StoreStatus.ACTIVE);s.setOwner(u);return stores.save(s);}
    Category category(String name){var c=new Category();c.setName(name);c.setSlug(UUID.randomUUID().toString());c.setIsActive(true);return categories.save(c);}
    Product product(Store s,Category c,int price){var p=new Product();p.setName("Cây xanh");p.setSlug(UUID.randomUUID().toString());p.setStore(s);p.setCategory(c);p.setStatus(ProductStatus.ACTIVE);p.setStock(2);p.setPrice(BigDecimal.valueOf(price));p.setCreatedAt(LocalDateTime.of(2026,1,1,0,0));return products.save(p);}
    RequestPostProcessor as(User u){return authentication(UsernamePasswordAuthenticationToken.authenticated(AuthPrincipal.of(u),null,List.of(new SimpleGrantedAuthority("ROLE_"+u.getRole()))));}
    String questionUrl(){return "/api/account/products/"+product.getId()+"/questions";}
    long ask()throws Exception {mvc.perform(post(questionUrl()).with(as(buyer)).with(csrf()).contentType("application/json").content("{\"body\":\"Có bao gồm chậu không?\"}")).andExpect(status().isOk());return db.queryForObject("select max(id) from product_questions",Long.class);}
    long createPost()throws Exception {mvc.perform(post("/api/account/community/posts").with(as(buyer)).with(csrf()).contentType("application/json").content("{\"title\":\"Góc sen đá\",\"body\":\"Quan sát giá thể trước khi tưới.\",\"topic\":\"CARE\"}")).andExpect(status().isOk());return db.queryForObject("select max(id) from community_posts",Long.class);}
    @Test void storeSearchUsesRealPublicProductCategoriesAndPublicLocation()throws Exception {
        product(otherShop,alternate,120);
        mvc.perform(get("/api/stores").param("search"," Đà Nẵng ").param("size","1")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].id").value(shop.getId())).andExpect(jsonPath("$.content[0].owner").doesNotExist());
        mvc.perform(get("/api/stores").param("search","Sen đá").param("size","1")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/stores?categoryId="+category.getId()+"&size=1")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        product.setStatus(ProductStatus.DRAFT);products.save(product);
        mvc.perform(get("/api/stores?categoryId="+category.getId())).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }
    @Test void storePaginationIsStableBoundedAndDoesNotReadPrivateAddresses()throws Exception {
        var stats=factory.unwrap(org.hibernate.SessionFactory.class).getStatistics();stats.clear();
        mvc.perform(get("/api/stores?size=1&page=1")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2)).andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.content[0].id").value(otherShop.getId()));
        assertTrue(stats.getPrepareStatementCount()<=2);
        mvc.perform(get("/api/stores?size=51")).andExpect(status().isBadRequest());mvc.perform(get("/api/stores?page=-1")).andExpect(status().isBadRequest());
        shop.setStatus(StoreStatus.INACTIVE);stores.save(shop);
        mvc.perform(get("/api/stores?size=10")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }
    @Test void latestUsesCreationTimestampAndDescendingIdTieBreak()throws Exception {
        var newer=product(shop,category,150);var tied=product(shop,category,150);
        newer.setCreatedAt(product.getCreatedAt().plusDays(1));tied.setCreatedAt(newer.getCreatedAt());products.save(newer);products.save(tied);
        mvc.perform(get("/api/products?sort=latest&size=2")).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(tied.getId())).andExpect(jsonPath("$.content[1].id").value(newer.getId()));
    }
    @Test void recommendationsChoosePriceProximityAndOnePerOtherStore()throws Exception {
        var far=product(otherShop,category,400);var near=product(otherShop,category,110);product(otherShop,alternate,100);
        var sold=product(otherShop,category,100);sold.setStock(0);products.save(sold);
        mvc.perform(get("/api/products/"+product.getId()+"/recommendations")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(near.getId()));
        otherShop.setStatus(StoreStatus.INACTIVE);stores.save(otherShop);
        mvc.perform(get("/api/products/"+product.getId()+"/recommendations")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }
    @Test void homepageRecommendationsExcludeNewestAndRepeatDeterministically()throws Exception {
        product(otherShop,category,90);
        String uri="/api/products/recommendations?exclude="+product.getId();
        var a=mvc.perform(get(uri)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andReturn().getResponse().getContentAsString();
        assertEquals(a,mvc.perform(get(uri)).andReturn().getResponse().getContentAsString());
    }
    @Test void questionReadsArePublicButWritesRequireSessionAndCsrf()throws Exception {
        mvc.perform(get("/api/products/"+product.getId()+"/questions")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(post(questionUrl()).with(csrf()).contentType("application/json").content("{\"body\":\"Câu hỏi\"}")).andExpect(status().isUnauthorized());
        mvc.perform(post(questionUrl()).with(as(buyer)).contentType("application/json").content("{\"body\":\"Câu hỏi\"}")).andExpect(status().isForbidden());
        ask();mvc.perform(get("/api/products/"+product.getId()+"/questions")).andExpect(jsonPath("$.content[0].authorName").value(buyer.getName())).andExpect(jsonPath("$.content[0].email").doesNotExist()).andExpect(jsonPath("$.content[0].canAnswer").value(false));
    }
    @Test void sellerAnswerRequiresExactOwnerAndRevision()throws Exception {
        long id=ask();String uri="/api/seller/product-questions/"+id+"/answer",body="{\"body\":\"Có chậu đi kèm.\",\"revision\":0}";
        mvc.perform(put(uri).with(as(other)).with(csrf()).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(put(uri).with(as(buyer)).with(csrf()).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(put(uri).with(as(seller)).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.answerAuthorId").value(seller.getId())).andExpect(jsonPath("$.revision").value(1));
        mvc.perform(put(uri).with(as(seller)).with(csrf()).contentType("application/json").content(body)).andExpect(status().isConflict());
    }
    @Test void whitespaceOversizeDuplicateAndHiddenQuestionsAreHandled()throws Exception {
        for(String text:List.of(" ","x".repeat(2001)))mvc.perform(post(questionUrl()).with(as(buyer)).with(csrf()).contentType("application/json").content("{\"body\":\""+text+"\"}")).andExpect(status().isBadRequest());
        long id=ask();mvc.perform(post(questionUrl()).with(as(buyer)).with(csrf()).contentType("application/json").content("{\"body\":\"Có bao gồm chậu không?\"}")).andExpect(status().isConflict());
        db.update("update product_questions set status='HIDDEN' where id=?",id);
        mvc.perform(get("/api/products/"+product.getId()+"/questions")).andExpect(jsonPath("$.totalElements").value(0));
        product.setStatus(ProductStatus.DRAFT);products.save(product);
        mvc.perform(get("/api/products/"+product.getId()+"/questions")).andExpect(status().isNotFound());
        mvc.perform(post(questionUrl()).with(as(buyer)).with(csrf()).contentType("application/json").content("{\"body\":\"Còn cây không?\"}")).andExpect(status().isNotFound());
    }
    @Test void questionRateLimitCountsRemovedContent()throws Exception {
        for(int i=0;i<20;i++)db.update("insert into product_questions(product_id,author_id,body,status) values(?,?,?,'REMOVED')",product.getId(),buyer.getId(),"Nội dung "+i);
        mvc.perform(post(questionUrl()).with(as(buyer)).with(csrf()).contentType("application/json").content("{\"body\":\"Câu mới\"}")).andExpect(status().isTooManyRequests());
    }
    @Test void communityCreationPaginationFilteringAndPlainTextContract()throws Exception {
        long id=createPost();
        mvc.perform(get("/api/community/posts").param("search","giá thể").param("topic","CARE")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].id").value(id));
        mvc.perform(get("/api/community/posts?topic=BONSAI")).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/community/posts?page=-1")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/account/community/posts").with(as(buyer)).with(csrf()).contentType("application/json").content("{\"title\":\"<script>alert(1)</script>\",\"body\":\"<img onerror=alert(1)>\",\"topic\":\"OTHER\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("<script>alert(1)</script>"));
        // API trả văn bản; bộ test UI kiểm tra trình duyệt escape nội dung.
    }
    @Test void communityOwnershipRevisionAndSoftDeletionProtectComments()throws Exception {
        long id=createPost();String uri="/api/account/community/posts/"+id;
        String edit="{\"title\":\"Đổi tên\",\"body\":\"Nội dung mới\",\"topic\":\"CARE\",\"revision\":0}";
        mvc.perform(put(uri).with(as(other)).with(csrf()).contentType("application/json").content(edit)).andExpect(status().isForbidden());
        mvc.perform(put(uri).with(as(buyer)).with(csrf()).contentType("application/json").content(edit)).andExpect(status().isOk());
        mvc.perform(put(uri).with(as(buyer)).with(csrf()).contentType("application/json").content(edit)).andExpect(status().isConflict());
        mvc.perform(delete(uri+"?revision=1").with(as(buyer)).with(csrf())).andExpect(status().isOk());
        mvc.perform(get("/api/community/posts/"+id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/community/posts/"+id+"/comments")).andExpect(status().isNotFound());
        mvc.perform(post(uri+"/comments").with(as(other)).with(csrf()).contentType("application/json").content("{\"body\":\"Bình luận\"}")).andExpect(status().isNotFound());
        assertEquals(1,db.queryForObject("select count(*) from community_posts where status='REMOVED'",Long.class));
    }
    @Test void commentCreationEditingDuplicatesAndAdminRemovalAreScoped()throws Exception {
        long id=createPost();String uri="/api/account/community/posts/"+id+"/comments",body="{\"body\":\"Mình cũng quan sát đất trước khi tưới.\"}";
        mvc.perform(post(uri).with(csrf()).contentType("application/json").content(body)).andExpect(status().isUnauthorized());
        mvc.perform(post(uri).with(as(other)).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk());
        mvc.perform(post(uri).with(as(other)).with(csrf()).contentType("application/json").content(body)).andExpect(status().isConflict());
        long comment=db.queryForObject("select max(id) from community_comments",Long.class);
        mvc.perform(put("/api/account/community/comments/"+comment).with(as(buyer)).with(csrf()).contentType("application/json").content(body)).andExpect(status().isForbidden());
        var admin=user(UserRole.ADMIN,"Người điều hành");
        mvc.perform(delete("/api/account/community/comments/"+comment+"?revision=0").with(as(admin)).with(csrf())).andExpect(status().isOk());
        mvc.perform(get("/api/community/posts/"+id+"/comments")).andExpect(jsonPath("$.totalElements").value(0));
    }
    @Test void communityRateLimitsAndValidationRejectSpamWithoutWriting()throws Exception {
        long id=createPost();
        for(String body:List.of("{\"title\":\" \",\"body\":\"Nội dung\",\"topic\":\"CARE\"}","{\"title\":\"Chủ đề\",\"body\":\"Nội dung\",\"topic\":\"UNKNOWN\"}"))
            mvc.perform(post("/api/account/community/posts").with(as(buyer)).with(csrf()).contentType("application/json").content(body)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/account/community/posts").with(as(buyer)).contentType("application/json").content("{}")) .andExpect(status().isForbidden());
        for(int i=0;i<4;i++)db.update("insert into community_posts(author_id,title,body,topic,status) values(?,?,?,'CARE','REMOVED')",buyer.getId(),"Ghi chép","Nội dung "+i);
        mvc.perform(post("/api/account/community/posts").with(as(buyer)).with(csrf()).contentType("application/json").content("{\"title\":\"Bài mới\",\"body\":\"Quan sát cây\",\"topic\":\"CARE\"}")).andExpect(status().isTooManyRequests());
        for(int i=0;i<30;i++)db.update("insert into community_comments(post_id,author_id,body,status) values(?,?,?,'REMOVED')",id,other.getId(),"Bình luận "+i);
        mvc.perform(post("/api/account/community/posts/"+id+"/comments").with(as(other)).with(csrf()).contentType("application/json").content("{\"body\":\"Câu trao đổi mới\"}")).andExpect(status().isTooManyRequests());
    }
    @Test void hiddenPostsCannotLeakIntoSearchOrCommentsAndPagingIsBounded()throws Exception {
        long id=createPost();
        for(int i=0;i<12;i++)db.update("insert into community_posts(author_id,title,body,topic) values(?,?,?,'CARE')",buyer.getId(),"Chăm cây "+i,"Ghi chép "+i);
        mvc.perform(get("/api/community/posts?page=1")).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(3)).andExpect(jsonPath("$.totalPages").value(2));
        db.update("update community_posts set status='HIDDEN' where id=?",id);
        mvc.perform(get("/api/community/posts/"+id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/community/posts/"+id+"/comments")).andExpect(status().isNotFound());
        mvc.perform(get("/api/community/posts").param("search","Góc sen đá")).andExpect(jsonPath("$.totalElements").value(0));
    }
    @Test void commentsEditOnlyByAuthorAndStaleChangesCannotOverwrite()throws Exception {
        long id=createPost();db.update("insert into community_comments(post_id,author_id,body) values(?,?,?)",id,other.getId(),"Chia sẻ ban đầu");
        long comment=db.queryForObject("select max(id) from community_comments",Long.class);String uri="/api/account/community/comments/"+comment,body="{\"body\":\"Chia sẻ đã chỉnh\",\"revision\":0}";
        mvc.perform(put(uri).with(as(other)).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(1));
        mvc.perform(put(uri).with(as(other)).with(csrf()).contentType("application/json").content(body)).andExpect(status().isConflict());
        mvc.perform(delete(uri+"?revision=1").with(as(buyer)).with(csrf())).andExpect(status().isForbidden());
    }
    @Test void concurrentIdenticalQuestionSubmissionsPersistExactlyOnce()throws Exception {
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)){
            var start=new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.Callable<Integer> task=()->{start.await();return mvc.perform(post(questionUrl()).with(as(buyer)).with(csrf()).contentType("application/json").content("{\"body\":\"Hai lần nhấn gửi cùng một câu hỏi\"}")).andReturn().getResponse().getStatus();};
            var first=pool.submit(task);var second=pool.submit(task);start.countDown();
            assertEquals(Set.of(200,409),Set.of(first.get(),second.get()));
            assertEquals(1,db.queryForObject("select count(*) from product_questions",Long.class));
        }
    }
    @Test void questionsPaginationAndDeletionCannotImpersonateAnAuthor()throws Exception {
        for(int i=0;i<12;i++)db.update("insert into product_questions(product_id,author_id,body) values(?,?,?)",product.getId(),buyer.getId(),"Câu hỏi "+i);
        mvc.perform(get("/api/products/"+product.getId()+"/questions?page=1")).andExpect(jsonPath("$.content.length()").value(2)).andExpect(jsonPath("$.totalElements").value(12));
        long id=db.queryForObject("select max(id) from product_questions",Long.class);
        mvc.perform(delete("/api/account/product-questions/"+id+"?revision=0").with(as(other)).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/account/product-questions/"+id+"?revision=0").with(as(buyer)).with(csrf())).andExpect(status().isOk());
        mvc.perform(get("/api/products/"+product.getId()+"/questions")).andExpect(jsonPath("$.totalElements").value(11));
    }
}

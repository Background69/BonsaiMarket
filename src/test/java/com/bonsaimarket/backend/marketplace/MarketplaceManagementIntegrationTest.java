package com.bonsaimarket.backend.marketplace;

import com.bonsaimarket.backend.seller.application.*;
import com.bonsaimarket.backend.media.*;
import com.bonsaimarket.backend.report.*;
import com.bonsaimarket.backend.profile.*;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import com.bonsaimarket.backend.category.*;
import com.bonsaimarket.backend.product.*;
import com.bonsaimarket.backend.store.*;
import com.bonsaimarket.backend.user.*;
import com.bonsaimarket.backend.marketplace.MarketplaceException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.bonsaimarket.backend.category.CategoryDtos.*;
import static com.bonsaimarket.backend.profile.ProfileDtos.*;
import static com.bonsaimarket.backend.store.StoreProfileDtos.*;
import static com.bonsaimarket.backend.seller.application.SellerApplicationDtos.*;
import static com.bonsaimarket.backend.report.ReportDtos.*;

/** MockMvc + actual JPA transactions/row locks on isolated H2. No live database/provider calls. */
@SpringBootTest(properties={"spring.profiles.active=management-test","spring.datasource.url=jdbc:h2:mem:management;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
    "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
    "spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=create-drop","marketplace.media.root=.cache/management/test-images"})
@AutoConfigureMockMvc
class MarketplaceManagementIntegrationTest {
    @Autowired MockMvc mvc;@Autowired JdbcTemplate jdbc;@Autowired UserRepository users;@Autowired StoreRepository stores;
    @Autowired ProductRepository products;@Autowired CategoryRepository categories;@Autowired ApplicationRepository applications;
    @Autowired SellerApplicationService onboarding;@Autowired CategoryManagementService categoryService;@Autowired ReportService reports;
    @Autowired StoreProfileService profiles;@Autowired ProductImageService images;@Autowired PlatformTransactionManager manager;
    User buyer,other,seller,otherSeller,admin;Store store;Category category;Product product;
    static final String APPLICATION="""
        {"name":"Vườn mới","description":"Giới thiệu thật","phone":"0901234567","address":"TP Hồ Chí Minh","revision":0}
        """;
    static final String CATEGORY="""
        {"name":"Chậu gốm","description":"Danh mục mới","active":true,"revision":0}
        """;
    @BeforeEach void setup() throws Exception {
        for(String table:List.of("marketplace_audit","marketplace_reports","seller_applications","product_images","inventory_debits","order_items","orders","cart_items","carts","addresses","reviews","wishlist_items","products","stores","categories","auth_challenges","oauth_identities","users")) jdbc.update("delete from "+table);
        Path media=Path.of(".cache/management/test-images");if(Files.isDirectory(media))try(var files=Files.list(media)){for(var f:files.toList())Files.delete(f);}
        buyer=user(UserRole.CUSTOMER);other=user(UserRole.CUSTOMER);seller=user(UserRole.SELLER);otherSeller=user(UserRole.SELLER);admin=user(UserRole.ADMIN);
        store=new Store();store.setName("Store");store.setOwner(seller);store.setStatus(StoreStatus.ACTIVE);stores.save(store);
        category=new Category();category.setName("Bonsai");category.setSlug("bonsai");category.setIsActive(true);category.setSortOrder(0);categories.save(category);
        product=new Product();product.setName("Mai");product.setSlug(UUID.randomUUID().toString());product.setStock(3);product.setPrice(BigDecimal.TEN);product.setStatus(ProductStatus.ACTIVE);product.setCategory(category);product.setStore(store);products.save(product);
    }
    User user(UserRole role){var u=new User();u.setName("Nguyễn An");u.setEmail(UUID.randomUUID()+"@example.com");u.setPasswordHash("secret-hash");u.setIsActive(true);u.setEmailVerifiedAt(LocalDateTime.now());u.setRole(role);return users.save(u);}
    RequestPostProcessor as(User u){return authentication(UsernamePasswordAuthenticationToken.authenticated(AuthPrincipal.of(u),null,List.of(new SimpleGrantedAuthority("ROLE_"+u.getRole()))));}
    ApplicationWrite application(long revision){return new ApplicationWrite("Vườn mới","Mô tả","0901234567","Đà Nẵng",revision);}
    CategoryWrite category(String name,long revision){return new CategoryWrite(name,"Mô tả",true,revision);}
    ReportWrite report(Long id){return new ReportWrite("PRODUCT",id,"MISLEADING","Nội dung cần kiểm tra");}
    MarketplaceException failure(Runnable action,String code){var e=assertThrows(MarketplaceException.class,action::run);assertEquals(code,e.code);return e;}
    List<String> parallel(Runnable a,Runnable b)throws Exception{var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);try{var tasks=new ArrayList<Future<String>>();for(var action:List.of(a,b))tasks.add(pool.submit(()->{assertTrue(start.await(5,TimeUnit.SECONDS));try{action.run();return "OK";}catch(MarketplaceException e){return e.code;}catch(org.springframework.dao.DataIntegrityViolationException e){return "DATA_CONFLICT";}}));start.countDown();return List.of(tasks.get(0).get(15,TimeUnit.SECONDS),tasks.get(1).get(15,TimeUnit.SECONDS));}finally{pool.shutdownNow();}}
    byte[] png()throws Exception{var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(12,12,BufferedImage.TYPE_INT_RGB),"png",out);return out.toByteArray();}
    MockMultipartFile image(byte[] bytes){return new MockMultipartFile("file","../../evil.html","text/html",bytes);}
    long fileCount()throws Exception{var dir=Path.of(".cache/management/test-images");if(!Files.isDirectory(dir))return 0;try(var files=Files.list(dir)){return files.count();}}

    @Test void adminCategoryCrudStableSlugAndPublicVisibility()throws Exception{
        mvc.perform(post("/api/admin/categories").with(as(admin)).with(csrf()).contentType("application/json").content(CATEGORY)).andExpect(status().isCreated()).andExpect(jsonPath("$.slug").value("chau-gom"));
        var c=categoryService.list(admin.getId(),0,"Chậu").content().getFirst();
        mvc.perform(put("/api/admin/categories/"+c.id()).with(as(admin)).with(csrf()).contentType("application/json").content(CATEGORY.replace("Chậu gốm","Chậu mới").replace("true","false"))).andExpect(status().isOk()).andExpect(jsonPath("$.slug").value("chau-gom")).andExpect(jsonPath("$.active").value(false));
        assertEquals(1,categories.findByIsActiveTrueOrderBySortOrderAscIdAsc().size());
        var latest=categoryService.list(admin.getId(),0,"Chậu").content().getFirst();categoryService.delete(admin.getId(),latest.id(),latest.revision());assertEquals(1,categories.count());
    }
    @Test void categoryCannotDeleteReferencedProductOrParent(){failure(()->categoryService.delete(admin.getId(),category.getId(),0),"CATEGORY_IN_USE");var child=new Category();child.setName("Child");child.setSlug("child");child.setParent(category);child.setIsActive(true);categories.save(child);products.deleteAll();failure(()->categoryService.delete(admin.getId(),category.getId(),0),"CATEGORY_IN_USE");assertEquals(2,categories.count());}
    @Test void categoryDuplicateAndStaleWritesReturnConflict()throws Exception{
        categoryService.create(admin.getId(),category("New",0));mvc.perform(post("/api/admin/categories").with(as(admin)).with(csrf()).contentType("application/json").content(CATEGORY.replace("Chậu gốm"," new "))).andExpect(status().isConflict());
        var c=categoryService.update(admin.getId(),category.getId(),category("Bonsai mới",0));assertEquals(1,c.revision());failure(()->categoryService.update(admin.getId(),category.getId(),category("Stale",0)),"DATA_CONFLICT");
    }
    @Test void concurrentCategoryCreateHasOneWinner()throws Exception{var results=parallel(()->categoryService.create(admin.getId(),category("Concurrent",0)),()->categoryService.create(admin.getId(),category("Concurrent",0)));assertEquals(1,results.stream().filter("OK"::equals).count());assertEquals(1,categoryService.list(admin.getId(),0,"Concurrent").totalElements());}
    @Test void categoryAdminRoleAndValidation()throws Exception{
        mvc.perform(get("/api/admin/categories")).andExpect(status().isUnauthorized());
        for(var u:List.of(buyer,seller)){mvc.perform(get("/api/admin/categories").with(as(u))).andExpect(status().isForbidden());mvc.perform(post("/api/admin/categories").with(as(u)).with(csrf()).contentType("application/json").content(CATEGORY)).andExpect(status().isForbidden());}
        mvc.perform(post("/api/admin/categories").with(as(admin)).contentType("application/json").content(CATEGORY)).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/categories").with(as(admin)).with(csrf()).contentType("application/json").content(CATEGORY.replace("Chậu gốm"," "))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.name").exists());
        mvc.perform(get("/api/admin/categories?page=-1").with(as(admin))).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/admin/categories/999999?revision=0").with(as(admin)).with(csrf())).andExpect(status().isNotFound());
    }
    @Test void profileUsesPrincipalAndCannotAssignRolesOrEmail()throws Exception{
        String body="{\"name\":\"Tên mới\",\"phone\":\"0901234567\",\"bio\":\"<script>alert(1)</script>\",\"userId\":"+other.getId()+",\"role\":\"ADMIN\",\"email\":\"evil@example.com\",\"isActive\":false}";
        mvc.perform(patch("/api/account/profile").with(as(buyer)).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Tên mới")).andExpect(jsonPath("$.email").value(buyer.getEmail())).andExpect(jsonPath("$.passwordHash").doesNotExist()).andExpect(jsonPath("$.role").doesNotExist());
        var saved=users.findById(buyer.getId()).orElseThrow();assertEquals(UserRole.CUSTOMER,saved.getRole());assertTrue(saved.getIsActive());assertEquals("Nguyễn An",users.findById(other.getId()).orElseThrow().getName());
        mvc.perform(get("/api/auth/me").with(as(buyer))).andExpect(jsonPath("$.name").value("Tên mới"));
    }
    @Test void profileAuthValidationAndNoSecretFields()throws Exception{
        mvc.perform(get("/api/account/profile")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/account/profile").with(as(buyer))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(patch("/api/account/profile").with(as(buyer)).with(csrf()).contentType("application/json").content("{\"name\":\" \",\"phone\":\"abc\",\"bio\":\"\"}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.phone").exists());
    }
    @Test void applicationPersistsPendingAndCannotGrantOwnRole()throws Exception{
        mvc.perform(post("/api/account/seller-application").with(as(buyer)).with(csrf()).contentType("application/json").content(APPLICATION.replace("\"revision\":0","\"revision\":0,\"status\":\"APPROVED\",\"role\":\"ADMIN\""))).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"));
        assertEquals(UserRole.CUSTOMER,users.findById(buyer.getId()).orElseThrow().getRole());assertEquals(1,stores.count());
        mvc.perform(get("/api/account/seller-application").with(as(other))).andExpect(status().isNoContent());
        mvc.perform(post("/api/account/seller-application").with(as(buyer)).with(csrf()).contentType("application/json").content(APPLICATION)).andExpect(status().isConflict());
    }
    @Test void concurrentSubmitHasOnePendingApplication()throws Exception{var results=parallel(()->onboarding.submit(buyer.getId(),application(0)),()->onboarding.submit(buyer.getId(),application(0)));assertEquals(1,results.stream().filter("OK"::equals).count());assertEquals(1,applications.count());}
    @Test void approveCreatesSingleStoreAndRefreshesExistingSessionAuthority()throws Exception{
        var a=onboarding.submit(buyer.getId(),application(0));onboarding.decide(admin.getId(),a.id(),new Decision("APPROVED","Thông tin phù hợp",a.revision()));
        // Original principal and CUSTOMER authorities are deliberately reused: SessionGuard must refresh them.
        mvc.perform(get("/api/seller/store").with(as(buyer))).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Vườn mới"));
        mvc.perform(get("/api/auth/me").with(as(buyer))).andExpect(jsonPath("$.role").value("seller"));
        mvc.perform(get("/api/stores")).andExpect(jsonPath("$.length()").value(2));assertEquals(2,stores.count());
        assertThrows(MarketplaceException.class,()->onboarding.decide(admin.getId(),a.id(),new Decision("APPROVED","",a.revision())));
    }
    @Test void concurrentAdministratorsHaveOneDecisionAndOneStore()throws Exception{var a=onboarding.submit(buyer.getId(),application(0));var admin2=user(UserRole.ADMIN);var results=parallel(()->onboarding.decide(admin.getId(),a.id(),new Decision("APPROVED","",a.revision())),()->onboarding.decide(admin2.getId(),a.id(),new Decision("REJECTED","Thiếu thông tin",a.revision())));assertEquals(1,results.stream().filter("OK"::equals).count());assertTrue(stores.count()<=2);assertEquals(2,jdbc.queryForObject("select count(*) from marketplace_audit where target_type='APPLICATION'",Integer.class));}
    @Test void rejectedApplicantCanResubmitWithFreshRevisionAndAuditSurvives(){var a=onboarding.submit(buyer.getId(),application(0));failure(()->onboarding.decide(admin.getId(),a.id(),new Decision("REJECTED"," ",a.revision())),"VALIDATION_FAILED");var rejected=onboarding.decide(admin.getId(),a.id(),new Decision("REJECTED","Bổ sung mô tả",a.revision()));assertEquals("REJECTED",onboarding.current(buyer.getId()).status());var pending=onboarding.submit(buyer.getId(),application(rejected.revision()));assertEquals("PENDING",pending.status());assertEquals(3,jdbc.queryForObject("select count(*) from marketplace_audit",Integer.class));}
    @Test void existingSellerStorePreservedAndNonAdminCannotApprove()throws Exception{
        assertEquals("APPROVED",onboarding.current(seller.getId()).status());assertEquals(store.getId(),onboarding.current(seller.getId()).storeId());
        var a=onboarding.submit(buyer.getId(),application(0));
        for(var u:List.of(buyer,seller)){mvc.perform(patch("/api/admin/store-applications/"+a.id()).with(as(u)).with(csrf()).contentType("application/json").content("{\"status\":\"APPROVED\",\"reason\":\"\",\"revision\":0}")).andExpect(status().isForbidden());}
        failure(()->onboarding.submit(seller.getId(),application(0)),"STORE_ALREADY_EXISTS");
    }
    @Test void inactiveApplicantCannotBeApproved(){var a=onboarding.submit(buyer.getId(),application(0));buyer.setIsActive(false);users.save(buyer);failure(()->onboarding.decide(admin.getId(),a.id(),new Decision("APPROVED","",a.revision())),"APPLICANT_CHANGED");assertEquals(1,stores.count());}
    @Test void storeProfileIgnoresForeignIdAndDoesNotExposeOwner()throws Exception{
        mvc.perform(put("/api/seller/store").with(as(otherSeller)).with(csrf()).contentType("application/json").content(APPLICATION.replace("\"revision\":0","\"revision\":0,\"storeId\":"+store.getId()))).andExpect(status().isNotFound());
        mvc.perform(put("/api/seller/store").with(as(seller)).with(csrf()).contentType("application/json").content(APPLICATION)).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Vườn mới")).andExpect(jsonPath("$.owner").doesNotExist());
        mvc.perform(get("/api/stores/"+store.getId())).andExpect(jsonPath("$.name").value("Vườn mới"));
        mvc.perform(put("/api/seller/store").with(as(seller)).with(csrf()).contentType("application/json").content(APPLICATION)).andExpect(status().isConflict());
    }
    @Test void storeProfileCustomerForbiddenAndInactiveCannotMutate()throws Exception{mvc.perform(get("/api/seller/store").with(as(buyer))).andExpect(status().isForbidden());store.setStatus(StoreStatus.INACTIVE);stores.save(store);failure(()->profiles.update(seller.getId(),new StoreWrite("New","","","",store.getRevision())),"STORE_INACTIVE");}
    @Test void reportPersistsReceiptWithoutReporterPiiAndRejectsDuplicates()throws Exception{
        String body="{\"targetType\":\"PRODUCT\",\"targetId\":"+product.getId()+",\"reason\":\"FRAUD\",\"description\":\"<script>alert(1)</script>\",\"reporterId\":"+other.getId()+"}";
        mvc.perform(post("/api/account/reports").with(as(buyer)).with(csrf()).contentType("application/json").content(body)).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("OPEN")).andExpect(jsonPath("$.reporter").doesNotExist());
        assertEquals(buyer.getId(),jdbc.queryForObject("select reporter_id from marketplace_reports",Long.class));
        mvc.perform(post("/api/account/reports").with(as(buyer)).with(csrf()).contentType("application/json").content(body)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("REPORT_EXISTS"));assertEquals(1,reports.list(admin.getId(),"OPEN",0).totalElements());
    }
    @Test void reportServerValidationTargetVisibilityAndRoleIsolation()throws Exception{
        mvc.perform(get("/api/admin/reports").with(as(seller))).andExpect(status().isForbidden());
        failure(()->reports.create(buyer.getId(),report(999999L)),"NOT_FOUND");
        failure(()->reports.create(buyer.getId(),new ReportWrite("USER",buyer.getId(),"OTHER","abc")),"VALIDATION_FAILED");
        failure(()->reports.create(buyer.getId(),new ReportWrite("STORE",store.getId(),"OTHER"," ")),"VALIDATION_FAILED");
        product.setStatus(ProductStatus.DRAFT);products.save(product);failure(()->reports.create(buyer.getId(),report(product.getId())),"NOT_FOUND");
    }
    @Test void reportRateLimitUsesPersistedUserLock(){for(int i=0;i<5;i++){var p=new Product();p.setName("P"+i);p.setSlug(UUID.randomUUID().toString());p.setStatus(ProductStatus.ACTIVE);p.setStore(store);p.setCategory(category);p.setStock(1);p.setPrice(BigDecimal.ONE);products.save(p);reports.create(buyer.getId(),report(p.getId()));}failure(()->reports.create(buyer.getId(),report(product.getId())),"REPORT_RATE_LIMIT");assertEquals(5,reports.list(admin.getId(),"OPEN",0).totalElements());}
    @Test void concurrentReportsHaveOneWinner()throws Exception{var results=parallel(()->reports.create(buyer.getId(),report(product.getId())),()->reports.create(buyer.getId(),report(product.getId())));assertEquals(1,results.stream().filter("OK"::equals).count());assertTrue(results.contains("REPORT_EXISTS"));}
    @Test void moderationTransitionsAreAuditedAndNeverMutateTarget(){var r=reports.create(buyer.getId(),report(product.getId()));failure(()->reports.decide(admin.getId(),r.id(),new ReportDecision("RESOLVED","Checked",0L)),"REPORT_STATE_INVALID");var review=reports.decide(admin.getId(),r.id(),new ReportDecision("IN_REVIEW","Tiếp nhận",0L));var done=reports.decide(admin.getId(),r.id(),new ReportDecision("RESOLVED","Đã kiểm tra",review.revision()));failure(()->reports.decide(admin.getId(),r.id(),new ReportDecision("IN_REVIEW","Again",done.revision())),"REPORT_STATE_INVALID");assertEquals(ProductStatus.ACTIVE,products.findById(product.getId()).orElseThrow().getStatus());assertEquals(2,jdbc.queryForObject("select count(*) from marketplace_audit",Integer.class));}
    @Test void reportHistoryIsOrderedPrivateAndRestrictedToAdministrators()throws Exception{
        var report=reports.create(buyer.getId(),report(product.getId()));
        var review=reports.decide(admin.getId(),report.id(),new ReportDecision("IN_REVIEW","Tiếp nhận",0L));
        reports.decide(admin.getId(),report.id(),new ReportDecision("RESOLVED","Đã kiểm tra",review.revision()));
        String path="/api/admin/reports/"+report.id()+"/history";
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).with(as(buyer))).andExpect(status().isForbidden());
        mvc.perform(get(path).with(as(seller))).andExpect(status().isForbidden());
        mvc.perform(get(path).with(as(admin))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].action").value("REPORT_IN_REVIEW"))
            .andExpect(jsonPath("$[1].reason").value("Đã kiểm tra")).andExpect(jsonPath("$[0].actorId").doesNotExist())
            .andExpect(jsonPath("$[0].reporter").doesNotExist());
        mvc.perform(get("/api/admin/reports/999999/history").with(as(admin))).andExpect(status().isNotFound());
    }
    @Test void inventorySuppliesPersistedImageUrlWithoutAdditionalProductLookups()throws Exception{
        product.setImageUrl("https://example.com/plant.jpg");products.save(product);
        mvc.perform(get("/api/seller/inventory").with(as(seller))).andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].imageUrl").value("https://example.com/plant.jpg"));
    }
    @Test void imageSignatureOverridesClientMimeAndSafeGeneratedPath()throws Exception{
        mvc.perform(multipart("/api/seller/products/"+product.getId()+"/image").file(image(png())).with(as(seller)).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.imageUrl").value(org.hamcrest.Matchers.startsWith("/api/products/"+product.getId()+"/image?v=")));
        assertEquals(1,fileCount());mvc.perform(get("/api/products/"+product.getId()+"/image")).andExpect(status().isOk()).andExpect(content().contentType("image/png")).andExpect(header().string("X-Content-Type-Options","nosniff"));
    }
    @Test void imageRejectsHtmlSvgForgedMimeTruncatedAndOversize()throws Exception{
        for(byte[] bytes:List.of("<html><script>1</script>".getBytes(),"<svg xmlns='http://www.w3.org/2000/svg'/>".getBytes(),new byte[]{(byte)137,80,78,71}))
            mvc.perform(multipart("/api/seller/products/"+product.getId()+"/image").file(new MockMultipartFile("file","valid.png","image/png",bytes)).with(as(seller)).with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/seller/products/"+product.getId()+"/image").file(image(new byte[5242881])).with(as(seller)).with(csrf())).andExpect(status().isPayloadTooLarge());assertEquals(0,fileCount());
    }
    @Test void imageOwnerAdminRoleCsrfAndNotFound()throws Exception{
        String path="/api/seller/products/"+product.getId()+"/image";
        mvc.perform(multipart(path).file(image(png())).with(as(otherSeller)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(multipart(path).file(image(png())).with(as(buyer)).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(multipart(path).file(image(png())).with(as(seller))).andExpect(status().isForbidden());
        mvc.perform(multipart(path).file(image(png())).with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/seller/products/999999/image").file(image(png())).with(as(admin)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(multipart(path).file(image(png())).with(as(admin)).with(csrf())).andExpect(status().isOk());
    }
    @Test void imageReplaceDeleteIsIdempotentAndNoOrphans()throws Exception{var a=images.upload(seller.getId(),product.getId(),image(png()));var b=images.upload(seller.getId(),product.getId(),image(png()));assertNotEquals(a.imageUrl(),b.imageUrl());assertEquals(1,fileCount());images.delete(seller.getId(),product.getId());images.delete(seller.getId(),product.getId());assertEquals(0,fileCount());assertNull(products.findById(product.getId()).orElseThrow().getImageUrl());}
    @Test void imageRollbackRemovesNewFileAndKeepsPrevious()throws Exception{var before=images.upload(seller.getId(),product.getId(),image(png()));var tx=new TransactionTemplate(manager);tx.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);tx.executeWithoutResult(status->{try{images.upload(seller.getId(),product.getId(),image(png()));status.setRollbackOnly();}catch(Exception e){throw new RuntimeException(e);}});assertEquals(1,fileCount());assertEquals(before.imageUrl(),products.findById(product.getId()).orElseThrow().getImageUrl());}
    @Test void imageInactiveProductNotPublicAndLegacyDetachDoesNotDeleteOtherFiles()throws Exception{images.upload(seller.getId(),product.getId(),image(png()));product=products.findById(product.getId()).orElseThrow();product.setStatus(ProductStatus.DRAFT);products.save(product);mvc.perform(get("/api/products/"+product.getId()+"/image")).andExpect(status().isNotFound());}
    @Test void managementBodyBoundAndMalformedInputDoNotLeakInternals()throws Exception{mvc.perform(patch("/api/account/profile").with(as(buyer)).with(csrf()).contentType("application/json").content("x".repeat(32769))).andExpect(status().isPayloadTooLarge());mvc.perform(patch("/api/account/profile").with(as(buyer)).with(csrf()).contentType("application/json").content("{")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.exception").doesNotExist());}
    @Test void twoApprovalsCreateExactlyOneStore()throws Exception{
        var app=onboarding.submit(buyer.getId(),application(0));var second=user(UserRole.ADMIN);
        var results=parallel(()->onboarding.decide(admin.getId(),app.id(),new Decision("APPROVED","",0L)),()->onboarding.decide(second.getId(),app.id(),new Decision("APPROVED","",0L)));
        assertEquals(1,results.stream().filter("OK"::equals).count());assertEquals(2,stores.count());assertEquals(UserRole.SELLER,users.findById(buyer.getId()).orElseThrow().getRole());
    }
    @Test void adminCannotApproveOwnHistoricalApplication(){
        var app=onboarding.submit(buyer.getId(),application(0));buyer.setRole(UserRole.ADMIN);users.save(buyer);
        failure(()->onboarding.decide(buyer.getId(),app.id(),new Decision("APPROVED","",0L)),"SELF_APPROVAL_FORBIDDEN");assertEquals(1,stores.count());
    }
    @Test void reportModerationMutationsRequireAdminCsrfAndKnownResource()throws Exception{
        var report=reports.create(buyer.getId(),report(product.getId()));String path="/api/admin/reports/"+report.id();String decision="{\"status\":\"IN_REVIEW\",\"reason\":\"Checked\",\"revision\":0}";
        for(var actor:List.of(buyer,seller))mvc.perform(patch(path).with(as(actor)).with(csrf()).contentType("application/json").content(decision)).andExpect(status().isForbidden());
        mvc.perform(patch(path).with(csrf()).contentType("application/json").content(decision)).andExpect(status().isUnauthorized());
        mvc.perform(patch(path).with(as(admin)).contentType("application/json").content(decision)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/reports/999999").with(as(admin)).with(csrf()).contentType("application/json").content(decision)).andExpect(status().isNotFound());
        mvc.perform(patch(path).with(as(admin)).with(csrf()).contentType("application/json").content(decision.replace("IN_REVIEW","INVALID"))).andExpect(status().isBadRequest());
    }
    @Test void imageDimensionFormatAndEmptyValidation()throws Exception{
        var large=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(6001,1,BufferedImage.TYPE_INT_RGB),"png",large);
        for(var bytes:List.of(large.toByteArray(),new byte[0]))mvc.perform(multipart("/api/seller/products/"+product.getId()+"/image").file(image(bytes)).with(as(seller)).with(csrf())).andExpect(status().is4xxClientError());
        var gif=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"gif",gif);
        mvc.perform(multipart("/api/seller/products/"+product.getId()+"/image").file(image(gif.toByteArray())).with(as(seller)).with(csrf())).andExpect(status().isBadRequest());assertEquals(0,fileCount());
    }
    @Test void jpegIsDecodedAndReencodedAndPrivateDraftOnlyVisibleToOwnerOrAdmin()throws Exception{
        var jpeg=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(5,5,BufferedImage.TYPE_INT_RGB),"jpeg",jpeg);images.upload(seller.getId(),product.getId(),image(jpeg.toByteArray()));
        product=products.findById(product.getId()).orElseThrow();product.setStatus(ProductStatus.DRAFT);products.save(product);String path="/api/products/"+product.getId()+"/image";
        mvc.perform(get(path).with(as(seller))).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        mvc.perform(get(path).with(as(admin))).andExpect(status().isOk());mvc.perform(get(path).with(as(otherSeller))).andExpect(status().isNotFound());mvc.perform(get(path).with(as(buyer))).andExpect(status().isNotFound());mvc.perform(get(path)).andExpect(status().isNotFound());
    }
    @Test void concurrentImageReplacementsKeepOneReferencedFile()throws Exception{
        var bytes=png();assertEquals(List.of("OK","OK"),parallel(()->images.upload(seller.getId(),product.getId(),image(bytes)),()->images.upload(seller.getId(),product.getId(),image(bytes))));
        assertEquals(1,fileCount());assertEquals(1,jdbc.queryForObject("select count(*) from product_images",Integer.class));
        mvc.perform(delete("/api/seller/products/"+product.getId()+"/image").with(as(otherSeller)).with(csrf())).andExpect(status().isNotFound());assertEquals(1,fileCount());
    }
}

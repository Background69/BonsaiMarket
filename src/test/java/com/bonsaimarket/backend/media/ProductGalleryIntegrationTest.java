package com.bonsaimarket.backend.media;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import com.bonsaimarket.backend.category.Category;
import com.bonsaimarket.backend.marketplace.MarketplaceException;
import com.bonsaimarket.backend.product.*;
import com.bonsaimarket.backend.store.*;
import com.bonsaimarket.backend.user.*;
import jakarta.persistence.EntityManager;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static com.bonsaimarket.backend.media.ProductGalleryDtos.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Prepared only. H2/service/MockMvc evidence when separately run, never MySQL or browser proof. */
@SpringBootTest(properties={"spring.profiles.active=gallery-test","spring.datasource.url=jdbc:h2:mem:product-gallery;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
    "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
class ProductGalleryIntegrationTest {
    static final Path ROOT=tempRoot();
    static Path tempRoot(){try{return Files.createTempDirectory("bm-gallery-test-");}catch(IOException e){throw new UncheckedIOException(e);}}
    @DynamicPropertySource static void media(DynamicPropertyRegistry registry){registry.add("marketplace.media.root",()->ROOT.toString());}
    @MockitoBean ProductImageCleanup cleanup;
    @Autowired ProductGalleryService gallery;
    @Autowired ProductImageService legacy;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager manager;
    @Autowired MockMvc mvc;
    User seller,other,buyer;Product product;
    void tx(Runnable action){new TransactionTemplate(manager).executeWithoutResult(status->action.run());}
    @BeforeEach void setup(){tx(()->{
        seller=user(UserRole.SELLER);other=user(UserRole.SELLER);buyer=user(UserRole.CUSTOMER);
        var category=new Category();category.setName("Gallery fixture");category.setSlug(UUID.randomUUID().toString());category.setIsActive(true);em.persist(category);
        var store=new Store();store.setName("Gallery fixture");store.setOwner(seller);store.setStatus(StoreStatus.ACTIVE);em.persist(store);
        product=new Product();product.setName("Gallery fixture");product.setSlug(UUID.randomUUID().toString());product.setPrice(new BigDecimal("100000"));product.setStock(3);product.setStatus(ProductStatus.ACTIVE);product.setStore(store);product.setCategory(category);em.persist(product);em.flush();
    });}
    User user(UserRole role){var user=new User();user.setName("Gallery fixture");user.setEmail(UUID.randomUUID()+"@example.invalid");user.setPasswordHash("unused");user.setRole(role);user.setIsActive(true);user.setEmailVerifiedAt(LocalDateTime.now());em.persist(user);return user;}
    MockMultipartFile image(int color)throws IOException{var image=new BufferedImage(40,30,BufferedImage.TYPE_INT_RGB);for(int y=0;y<30;y++)for(int x=0;x<40;x++)image.setRGB(x,y,color);var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);return new MockMultipartFile("file","../../ignored.png","image/png",out.toByteArray());}
    Upload upload(int color)throws IOException{return gallery.upload(seller.getId(),product.getId(),image(color),UUID.randomUUID().toString());}
    void error(String code,Runnable action){assertEquals(code,assertThrows(MarketplaceException.class,action::run).code);}
    RequestPostProcessor as(User user){return authentication(UsernamePasswordAuthenticationToken.authenticated(AuthPrincipal.of(user),null,List.of(new SimpleGrantedAuthority("ROLE_"+user.getRole()))));}

    @Test void retryAndDuplicateContentNeverAppendAnotherImage()throws Exception{
        String key=UUID.randomUUID().toString();var file=image(0x225533);
        var first=gallery.upload(seller.getId(),product.getId(),file,key);var retry=gallery.upload(seller.getId(),product.getId(),file,key);
        assertEquals(first.imageId(),retry.imageId());assertTrue(retry.replayed());assertEquals(1,retry.gallery().images().size());
        var duplicate=gallery.upload(seller.getId(),product.getId(),file,UUID.randomUUID().toString());assertTrue(duplicate.duplicate());assertEquals(first.imageId(),duplicate.imageId());
        var different=image(0xaa3300);error("IMAGE_REQUEST_CONFLICT",()->gallery.upload(seller.getId(),product.getId(),different,key));
    }
    @Test void coverReorderAndDeletionKeepCanonicalCoverAndRemovedReceipts()throws Exception{
        var a=upload(0x113322);String key=UUID.randomUUID().toString();var file=image(0x884422);var b=gallery.upload(seller.getId(),product.getId(),file,key);
        var reordered=gallery.reorder(seller.getId(),product.getId(),new Reorder(b.gallery().revision(),List.of(b.imageId(),a.imageId())));
        assertEquals(a.imageId(),reordered.images().stream().filter(Image::primary).findFirst().orElseThrow().id());
        var cover=gallery.setPrimary(seller.getId(),product.getId(),b.imageId(),reordered.revision());
        assertEquals(List.of(b.imageId(),a.imageId()),cover.images().stream().map(Image::id).toList());assertEquals(cover.imageUrl(),legacy.current(seller.getId(),product.getId()).imageUrl());
        error("DATA_CONFLICT",()->gallery.setPrimary(seller.getId(),product.getId(),a.imageId(),reordered.revision()));
        var removed=gallery.remove(seller.getId(),product.getId(),b.imageId(),cover.revision());assertEquals(a.imageId(),removed.images().getFirst().id());assertTrue(removed.images().getFirst().primary());
        error("IMAGE_UPLOAD_REMOVED",()->gallery.upload(seller.getId(),product.getId(),file,key));
        var empty=gallery.remove(seller.getId(),product.getId(),a.imageId(),removed.revision());assertTrue(empty.initialized());assertTrue(empty.images().isEmpty());assertNull(empty.imageUrl());assertTrue(gallery.publicGallery(product.getId(),null).images().isEmpty());
    }
    @Test void foreignSellerBuyerAndCsrfCannotMutate()throws Exception{
        var file=image(0x333333);
        error("NOT_FOUND",()->gallery.upload(other.getId(),product.getId(),file,UUID.randomUUID().toString()));
        error("FORBIDDEN",()->gallery.upload(buyer.getId(),product.getId(),file,UUID.randomUUID().toString()));
        mvc.perform(multipart("/api/seller/products/"+product.getId()+"/gallery").file(file).param("requestKey",UUID.randomUUID().toString()).with(as(seller))).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/seller/products/"+product.getId()+"/gallery").file(file).param("requestKey",UUID.randomUUID().toString()).with(as(buyer)).with(csrf())).andExpect(status().isForbidden());
    }
    @Test void rejectsWrongMimeSvgCorruptBytesAndForeignImageIds()throws Exception{
        var spoofed=new MockMultipartFile("file","photo.png","image/png","<svg onload='bad'/ >".getBytes());
        error("IMAGE_FORMAT_UNSUPPORTED",()->gallery.upload(seller.getId(),product.getId(),spoofed,UUID.randomUUID().toString()));
        var accepted=upload(0x445566);
        error("NOT_FOUND",()->gallery.setPrimary(seller.getId(),product.getId(),Long.MAX_VALUE,accepted.gallery().revision()));
        error("GALLERY_CHANGED",()->gallery.reorder(seller.getId(),product.getId(),new Reorder(accepted.gallery().revision(),List.of(accepted.imageId(),accepted.imageId()))));
        mvc.perform(get("/api/products/"+product.getId()+"/gallery")).andExpect(status().isOk()).andExpect(jsonPath("$.images[0].storageKey").doesNotExist()).andExpect(jsonPath("$.images[0].thumbnailUrl").exists());
    }
    @Test void rejectsFractionalRevisionAndMultiplePartsWithoutWriting()throws Exception{
        var added=upload(0x336644);
        mvc.perform(put("/api/seller/products/"+product.getId()+"/gallery/"+added.imageId()+"/primary").with(as(seller)).with(csrf()).contentType("application/json").content("{\"revision\":1.5}")).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/seller/products/"+product.getId()+"/gallery").file(image(0x557733)).file(image(0x883355)).param("requestKey",UUID.randomUUID().toString()).with(as(seller)).with(csrf())).andExpect(status().isBadRequest());
        assertEquals(1,gallery.seller(seller.getId(),product.getId()).images().size());
    }
    @Test void sharedImageReferencePreventsRemoval()throws Exception{
        var added=upload(0x551122);
        tx(()->{var p=em.find(Product.class,product.getId());var second=new Product();second.setName("Shared reference fixture");second.setSlug(UUID.randomUUID().toString());second.setPrice(p.getPrice());second.setStore(p.getStore());second.setCategory(p.getCategory());second.setStatus(ProductStatus.DRAFT);second.setImageUrl("https://example.invalid"+added.gallery().imageUrl());em.persist(second);});
        error("IMAGE_REFERENCED",()->gallery.remove(seller.getId(),product.getId(),added.imageId(),added.gallery().revision()));
        assertEquals(1,gallery.seller(seller.getId(),product.getId()).images().size());
    }
    @Test void concurrentUploadsCannotExceedEight()throws Exception{
        for(int i=0;i<7;i++)upload(0x100000+i*0x1122);
        var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try{
            var tasks=new ArrayList<Future<Boolean>>();
            for(int i=0;i<2;i++){var file=image(0x990000+i*0x2222);tasks.add(pool.submit(()->{start.await();try{gallery.upload(seller.getId(),product.getId(),file,UUID.randomUUID().toString());return true;}catch(MarketplaceException e){assertEquals("GALLERY_FULL",e.code);return false;}}));}
            start.countDown();int successes=0;for(var task:tasks)if(task.get(30,TimeUnit.SECONDS))successes++;
            assertEquals(1,successes);assertEquals(8,gallery.seller(seller.getId(),product.getId()).images().size());
        }finally{pool.shutdownNow();}
    }
    @Test void externalLegacyCoverAndStaleInternalRecordProduceOnlyOneCover()throws Exception{
        String oldKey=UUID.randomUUID()+".png";tx(()->{var p=em.find(Product.class,product.getId());p.setImageUrl("https://example.invalid/legacy-photo.jpg");var old=new ProductImage();old.setProduct(p);old.setProductId(p.getId());old.setFileKey(oldKey);old.setContentType("image/png");old.setBytes(100);em.persist(old);});
        var view=gallery.initialize(seller.getId(),product.getId());assertEquals(1,view.images().size());assertEquals("LEGACY_URL",view.images().getFirst().source());assertNull(view.images().getFirst().fileSizeBytes());
        tx(()->assertNotNull(em.find(ProductImage.class,product.getId())));
    }
    @AfterAll static void removeTaskTempFiles()throws IOException{try(var paths=Files.walk(ROOT)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
}

package com.bonsaimarket.backend.marketplace;

import com.bonsaimarket.backend.banner.BannerCampaigns;
import com.bonsaimarket.backend.wallet.WalletCredits;
import com.bonsaimarket.backend.product.SellerProducts;
import com.bonsaimarket.backend.catalog.CatalogSearch;
import com.bonsaimarket.backend.support.BaselineSql;
import java.math.BigDecimal;
import java.sql.DriverManager;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.junit.jupiter.api.Assertions.*;

/** Real transactions against the complete H2 schema; MySQL/HTTP proof is recorded separately. */
@SpringBootTest(properties={"spring.profiles.active=marketplace-test","spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa","spring.datasource.password=","spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=validate"})
class InternalCreditsIntegrationTest {
    static final String URL="jdbc:h2:mem:internalcredits;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000";
    // H2 2.4 CHECK IN expressions retain their defining session comparator.
    // Keep that isolated schema session open while pooled connections exercise its constraints.
    static java.sql.Connection schemaConnection;
    @DynamicPropertySource static void database(DynamicPropertyRegistry r)throws Exception {
        schemaConnection=DriverManager.getConnection(URL,"sa","");BaselineSql.currentSchema(schemaConnection);
        r.add("spring.datasource.url",()->URL);
    }
    @AfterAll static void closeSchema()throws Exception {schemaConnection.close();}
    static class MutableClock extends Clock {
        volatile Instant now=Instant.parse("2030-01-01T00:00:00Z");
        public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}public Instant instant(){return now;}
    }
    @TestConfiguration static class TimeConfig {@Bean @Primary MutableClock testClock(){return new MutableClock();}}
    @Autowired MutableClock clock;@Autowired JdbcTemplate db;@Autowired WalletCredits wallet;
    @Autowired BannerCampaigns banners;@Autowired SellerProducts products;@Autowired CatalogSearch catalog;
    @Autowired com.bonsaimarket.backend.management.AdminCatalogService moderation;
    long admin,a,b,buyer,category,pa,pb;
    @BeforeEach void setup(){
        clock.now=Instant.parse("2030-01-01T00:00:00Z");String key=UUID.randomUUID().toString();
        admin=user(key+"admin","ADMIN");a=user(key+"a","SELLER");b=user(key+"b","SELLER");buyer=user(key+"buyer","CUSTOMER");
        db.update("insert into categories(name,slug,name_key,sort_order,is_active) values(?,?,?,0,true)",key,key,key);
        category=db.queryForObject("select id from categories where slug=?",Long.class,key);
        for(long seller:List.of(a,b))db.update("insert into stores(owner_user_id,name,status) values(?,?,'ACTIVE')",seller,"store "+seller);
        pa=product(a);pb=product(b);
    }
    long user(String name,String role){db.update("insert into users(name,email,password_hash,role,is_active,email_verified_at) values(?,?,?, ?,true,CURRENT_TIMESTAMP)",name,name+"@example.invalid","test-disabled",role);return db.queryForObject("select id from users where name=?",Long.class,name);}
    long product(long seller){var p=products.create(seller,new SellerProducts.Create("QA product "+seller,BigDecimal.TEN,category,5,UUID.randomUUID()));db.update("update products set status='ACTIVE',description='Fixture description',image_url='/fixture.png' where id=?",p.id());return p.id();}
    void grant(long who,int n){wallet.grant(admin,who,BigDecimal.valueOf(n),UUID.randomUUID(),"Isolated test credits");}
    long campaign(){String slot="test "+UUID.randomUUID();banners.createSlot(admin,slot);long id=db.queryForObject("select id from banner_slots where name=?",Long.class,slot);banners.create(admin,new BannerCampaigns.Create(id,"Test campaign",clock.instant().plusSeconds(10),clock.instant().plusSeconds(20),clock.instant().plusSeconds(40),BigDecimal.ONE,UUID.randomUUID()));return db.queryForObject("select id from banner_campaigns where slot_id=?",Long.class,id);}
    long revision(long id){return db.queryForObject("select revision from banner_campaigns where id=?",Long.class,id);}
    BannerCampaigns.Decision decision(long id){return new BannerCampaigns.Decision(revision(id),"QA decision with audit");}
    long bid(long seller,long campaign,long product,int amount){banners.bid(seller,campaign,new BannerCampaigns.BidWrite(product,BigDecimal.valueOf(amount),UUID.randomUUID()));return db.queryForObject("select id from banner_bids where campaign_id=? and seller_id=?",Long.class,campaign,seller);}
    void balanced(){assertEquals(0,db.queryForObject("select count(*) from (select journal_id from credit_entries group by journal_id having sum(available_delta+reserved_delta)<>0) x",Integer.class));assertTrue(((List<?>)wallet.reconcile(admin)).isEmpty());}

    @Test void grantsAreReplaySafePrivateAndRejectFractionalCredits(){
        assertEquals(BigDecimal.ZERO.setScale(0),wallet.balance(a).available().setScale(0));UUID key=UUID.randomUUID();
        var j=wallet.grant(admin,a,BigDecimal.TEN,key,"Isolated test grant");assertEquals(j.id(),wallet.grant(admin,a,BigDecimal.TEN,key,"Isolated test grant").id());
        assertThrows(MarketplaceException.class,()->wallet.grant(admin,a,BigDecimal.ONE,key,"Isolated test grant"));
        assertThrows(MarketplaceException.class,()->wallet.grant(buyer,a,BigDecimal.ONE,UUID.randomUUID(),"Forbidden grant"));
        assertThrows(MarketplaceException.class,()->wallet.detail(b,j.id()));assertThrows(MarketplaceException.class,()->WalletCredits.amount(new BigDecimal("1.5")));balanced();
    }
    @Test void concurrentReservationsNeverOverspendAndInsufficientWriteRollsBack()throws Exception {
        grant(a,100);var pool=Executors.newFixedThreadPool(2);
        try{var start=new CountDownLatch(1);var calls=new ArrayList<Future<Boolean>>();for(int i=0;i<2;i++){final int n=i;calls.add(pool.submit(()->{start.await();try{wallet.reserve(a,BigDecimal.valueOf(80),UUID.randomUUID().toString(),"race:"+n);return true;}catch(MarketplaceException e){assertEquals("INSUFFICIENT_CREDITS",e.code);return false;}}));}start.countDown();int successes=0;for(var f:calls)if(f.get(15,TimeUnit.SECONDS))successes++;assertEquals(1,successes);assertEquals(20,wallet.balance(a).available().intValueExact());assertEquals(80,wallet.balance(a).reserved().intValueExact());balanced();}finally{pool.shutdownNow();}
    }
    @Test void highestApprovedBidWinsLoserReleasedAndExpirationChargesOnce(){
        grant(a,100);grant(b,100);long c=campaign(),ba=bid(a,c,pa,60),bb=bid(b,c,pb,80);
        banners.moderate(admin,c,ba,true,decision(c));banners.moderate(admin,c,bb,true,decision(c));
        assertTrue(((List<?>)banners.winners()).isEmpty());clock.now=clock.now.plusSeconds(21);banners.settle(admin,c,decision(c));
        assertEquals(bb,db.queryForObject("select winner_bid_id from banner_campaigns where id=?",Long.class,c));assertEquals(100,wallet.balance(a).available().intValueExact());
        assertFalse(((List<?>)banners.winners()).isEmpty());banners.settle(admin,c,decision(c));
        clock.now=clock.now.plusSeconds(20);banners.expire(admin);banners.expire(admin);assertEquals(20,wallet.balance(b).available().intValueExact());assertEquals(0,wallet.balance(b).reserved().intValueExact());assertTrue(((List<?>)banners.winners()).isEmpty());balanced();
    }
    @Test void tiesAreStableAndCancelledCampaignReleasesOnce(){
        grant(a,100);grant(b,100);long c=campaign(),ba=bid(a,c,pa,50),bb=bid(b,c,pb,50);banners.moderate(admin,c,ba,true,decision(c));banners.moderate(admin,c,bb,true,decision(c));clock.now=clock.now.plusSeconds(21);banners.settle(admin,c,decision(c));assertEquals(ba,db.queryForObject("select winner_bid_id from banner_campaigns where id=?",Long.class,c));banners.cancel(admin,c,decision(c));banners.cancel(admin,c,decision(c));assertEquals(100,wallet.balance(a).available().intValueExact());assertEquals(100,wallet.balance(b).available().intValueExact());balanced();
    }
    @Test void ownershipPendingBidsAndHiddenWinnerFailClosed(){
        grant(a,100);long c=campaign();assertThrows(MarketplaceException.class,()->bid(b,c,pa,30));long bid=bid(a,c,pa,40);clock.now=clock.now.plusSeconds(21);banners.settle(admin,c,decision(c));assertEquals("CANCELLED",db.queryForObject("select status from banner_campaigns where id=?",String.class,c));assertEquals(100,wallet.balance(a).available().intValueExact());balanced();
    }
    @Test void sellerCreateEditRetryRevisionAndModerationBoundary(){
        var body=new SellerProducts.Create("New QA",BigDecimal.TEN,category,2,UUID.randomUUID());var p=products.create(a,body);assertEquals(p.id(),products.create(a,body).id());assertEquals("DRAFT",p.status());assertThrows(MarketplaceException.class,()->products.get(b,p.id()));assertThrows(MarketplaceException.class,()->products.create(buyer,body));
        var edit=products.edit(a,p.id(),new SellerProducts.Edit("Updated",BigDecimal.valueOf(20),category,p.revision()));assertThrows(MarketplaceException.class,()->products.edit(a,p.id(),new SellerProducts.Edit("Stale",BigDecimal.TEN,category,0L)));assertThrows(MarketplaceException.class,()->products.submit(a,p.id(),edit.revision()));
        var live=products.get(a,pa);assertEquals("DRAFT",products.edit(a,pa,new SellerProducts.Edit("Material change",BigDecimal.valueOf(30),category,live.revision())).status());
    }
    long order(long product,int quantity,String status){
        String code="QA-"+UUID.randomUUID();long store=db.queryForObject("select store_id from products where id=?",Long.class,product);
        db.update("insert into orders(code,buyer_user_id,store_id,shipping_address_snapshot,shipping_method,shipping_fee,subtotal,total,status) values(?,?,?,'{}','TEST',0,?,?,?)",code,buyer,store,quantity*10,quantity*10,status);
        long id=db.queryForObject("select id from orders where code=?",Long.class,code);
        db.update("insert into order_items(order_id,product_id,product_name_snapshot,unit_price,quantity,line_total) values(?,?,'Original snapshot',10,?,?)",id,product,quantity,quantity*10);return id;
    }
    @Test void soldRankingUsesCompletedUnitsBeforePaginationAndPreservesOrderPrice(){
        order(pa,2,"COMPLETED");order(pb,3,"DELIVERED");order(pa,100,"CANCELLED");order(pa,100,"PENDING");
        var query=new HashMap<String,String>();query.put("category",String.valueOf(category));query.put("sort","sold_desc");query.put("size","1");
        // category accepts a slug; use its real value to exercise the filter contract.
        query.put("category",db.queryForObject("select slug from categories where id=?",String.class,category));
        assertEquals(pb,catalog.search(query).getContent().getFirst().getId());query.put("page","1");assertEquals(pa,catalog.search(query).getContent().getFirst().getId());
        var p=products.get(a,pa);products.edit(a,pa,new SellerProducts.Edit("Changed price",BigDecimal.valueOf(999),category,p.revision()));
        assertEquals(10,db.queryForObject("select min(unit_price) from order_items where product_id=?",BigDecimal.class,pa).intValueExact());
        query.put("page","0");assertEquals(1,catalog.search(query).getTotalElements());
    }
    @Test void hiddenWinnerImmediatelyLeavesPublicPlacementAndReleasesAtExpiry(){
        grant(a,100);long c=campaign(),ba=bid(a,c,pa,50);banners.moderate(admin,c,ba,true,decision(c));clock.now=clock.now.plusSeconds(21);banners.settle(admin,c,decision(c));
        db.update("update products set status='ARCHIVED' where id=?",pa);assertTrue(((List<?>)banners.winners()).isEmpty());clock.now=clock.now.plusSeconds(20);banners.expire(admin);assertEquals(100,wallet.balance(a).available().intValueExact());assertEquals(0,wallet.balance(a).reserved().intValueExact());balanced();
    }
    @Test void adminApprovalRechecksEligibilityAfterPendingGalleryChanges(){
        var p=products.get(a,pa);p=products.edit(a,pa,new SellerProducts.Edit("Ready for moderation",BigDecimal.TEN,category,p.revision()));
        p=products.submit(a,pa,p.revision());long revision=p.revision();
        db.update("update products set image_url=null where id=?",pa);
        assertThrows(MarketplaceException.class,()->moderation.moderate(admin,pa,"APPROVE",revision,"QA review of product"));
        assertEquals("PENDING",products.get(a,pa).status());
        db.update("update products set image_url='/fixture.png' where id=?",pa);
        moderation.moderate(admin,pa,"APPROVE",revision,"QA reviewed complete product");assertEquals("ACTIVE",products.get(a,pa).status());
    }
}

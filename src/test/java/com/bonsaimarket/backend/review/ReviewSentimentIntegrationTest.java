package com.bonsaimarket.backend.review;

import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReviewSentimentIntegrationTest extends SentimentTestSupport {
    @MockitoBean SentimentClient client;
    @Autowired PlatformTransactionManager transactions;
    @Autowired ProductReviews reviews;
    SentimentClient.Prediction negative() {return new SentimentClient.Prediction("negative","tfidf-logreg-"+"a".repeat(64),"train_only",true);}

    @Test void queuedAfterReviewThenPrivateFiltersAndPublicNegativeRemain() throws Exception {
        long id=submit("Cây héo và chậu bị nứt.");
        verifyNoInteractions(client);
        assertEquals("PENDING",db.queryForObject("select status from review_sentiment where review_id=?",String.class,id));
        when(client.predict(anyString())).thenReturn(negative());
        assertTrue(worker.processOnce());assertFalse(worker.processOnce());
        mvc.perform(get("/api/seller/reviews?sentiment=negative").with(as(seller))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].sentimentLabel").value("negative"));
        mvc.perform(get("/api/admin/reviews?sentiment=negative").with(as(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/products/"+product.getId()+"/reviews")).andExpect(status().isOk())
            .andExpect(jsonPath("$.reviews.content[0].comment").value("Cây héo và chậu bị nứt."))
            .andExpect(jsonPath("$.reviews.content[0].rating").value(1))
            .andExpect(jsonPath("$.reviews.content[0].sentimentLabel").doesNotExist())
            .andExpect(jsonPath("$.reviews.content[0].modelVersion").doesNotExist());
        assertEquals(1,db.queryForObject("select count(*) from review_sentiment where review_id=?",Integer.class,id));
    }

    @Test void outageStillSavesReviewAndRetriesAreBoundedWithoutNeutral() throws Exception {
        when(client.predict(anyString())).thenThrow(new java.net.http.HttpTimeoutException("timeout"));
        long id=submit("Giao hàng chậm, tôi chưa hài lòng.");
        for(int n=0;n<3;n++) {db.update("update review_sentiment set next_attempt_at=? where review_id=?",java.time.LocalDateTime.now().minusDays(1),id);assertTrue(worker.processOnce());}
        assertFalse(worker.processOnce());
        assertEquals("FAILED",db.queryForObject("select status from review_sentiment where review_id=?",String.class,id));
        assertNull(db.queryForObject("select label from review_sentiment where review_id=?",String.class,id));
        assertEquals(3,db.queryForObject("select attempts from review_sentiment where review_id=?",Integer.class,id));
        verify(client,times(3)).predict("Giao hàng chậm, tôi chưa hài lòng.");
    }

    @Test void sellerCannotReadForeignShopAndBuyerCannotReadInternalEndpoints() throws Exception {
        submit("Chất lượng cần cải thiện.");
        mvc.perform(get("/api/seller/reviews").with(as(foreign))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/seller/reviews").with(as(buyer))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/reviews").with(as(seller))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/reviews")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/seller/reviews?sentiment=bad").with(as(seller))).andExpect(status().isBadRequest());
    }

    @Test void changedDuringInferenceCannotPublishOldResult() throws Exception {
        long id=submit("Cây bị hư.");
        when(client.predict(anyString())).thenAnswer(call->{db.update("update reviews set comment='Nội dung mới' where id=?",id);return negative();});
        assertTrue(worker.processOnce());
        assertEquals("STALE",db.queryForObject("select status from review_sentiment where review_id=?",String.class,id));
        mvc.perform(get("/api/seller/reviews?sentiment=negative").with(as(seller))).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/seller/reviews?sentiment=UNANALYZED").with(as(seller))).andExpect(jsonPath("$.content[0].sentimentStatus").value("UNANALYZED"));
    }

    @Test void deleteDuringInferenceCannotRecreateResult() throws Exception {
        long id=submit("Chậu bị vỡ.");
        when(client.predict(anyString())).thenAnswer(call->{db.update("delete from reviews where id=?",id);return negative();});
        assertTrue(worker.processOnce());
        assertEquals(0,db.queryForObject("select count(*) from review_sentiment where review_id=?",Integer.class,id));
    }

    @Test void concurrentWorkersAndExpiredLeaseDoNotDuplicateResults() throws Exception {
        long id=submit("Đóng gói chưa tốt.");
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        when(client.predict(anyString())).thenAnswer(call->{entered.countDown();assertTrue(release.await(5,TimeUnit.SECONDS));return negative();});
        try(var pool=Executors.newSingleThreadExecutor()) {
            var first=pool.submit(worker::processOnce);assertTrue(entered.await(5,TimeUnit.SECONDS));
            assertFalse(worker.processOnce());release.countDown();assertTrue(first.get(5,TimeUnit.SECONDS));
        }
        assertEquals(1,db.queryForObject("select attempts from review_sentiment where review_id=?",Integer.class,id));
        assertFalse(worker.processOnce());verify(client,times(1)).predict(anyString());
    }

    @Test void rolledBackReviewHasNoJobAndInferenceRejectsCallerTransaction() {
        var tx=new TransactionTemplate(transactions);
        long before=db.queryForObject("select count(*) from reviews",Long.class);
        tx.executeWithoutResult(s->{reviews.create(product.getId(),buyer.getId(),new ProductReviews.Write(item.getId(),java.math.BigDecimal.ONE,"Rollback QA"));s.setRollbackOnly();});
        assertEquals(before,db.queryForObject("select count(*) from reviews",Long.class));
        assertEquals(0,db.queryForObject("select count(*) from review_sentiment",Integer.class));
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(s->worker.processOnce()));
        verifyNoInteractions(client);
    }

    @Test void oldLeaseCannotOverwriteNewWorkerAndExhaustedLeaseFails() throws Exception {
        long id=submit("Nội dung cho lease.");
        when(client.predict(anyString())).thenAnswer(call->{
            db.update("update review_sentiment set lease_token='new-owner' where review_id=?",id);
            return negative();
        });
        assertTrue(worker.processOnce());
        assertEquals("PROCESSING",db.queryForObject("select status from review_sentiment where review_id=?",String.class,id));
        db.update("update review_sentiment set attempts=3,next_attempt_at=? where review_id=?",java.time.LocalDateTime.now().minusDays(1),id);
        assertFalse(worker.processOnce());
        assertEquals("FAILED",db.queryForObject("select status from review_sentiment where review_id=?",String.class,id));
    }

    @Test void analyzedContentChangedLaterIsExcludedFromLabelFilter() throws Exception {
        long id=submit("Cây hư hỏng.");when(client.predict(anyString())).thenReturn(negative());assertTrue(worker.processOnce());
        db.update("update reviews set comment='Cây đẹp sau khi chăm sóc' where id=?",id);
        mvc.perform(get("/api/seller/reviews?sentiment=negative").with(as(seller))).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/seller/reviews?sentiment=UNANALYZED").with(as(seller))).andExpect(jsonPath("$.content[0].sentimentLabel").isEmpty());
    }

    @Test void paginationFiltersBeforeLimitAndIncludesUnanalyzedInAll() throws Exception {
        when(client.predict(anyString())).thenReturn(negative());
        for(int n=0;n<12;n++) {item=line();submit("Chất lượng kém "+n);assertTrue(worker.processOnce());}
        item=line();long pending=submit("Đang chờ phân tích");db.update("delete from review_sentiment where review_id=?",pending);
        mvc.perform(get("/api/seller/reviews?sentiment=negative&page=1").with(as(seller))).andExpect(jsonPath("$.totalElements").value(12)).andExpect(jsonPath("$.content.length()").value(2));
        mvc.perform(get("/api/seller/reviews?sentiment=ALL").with(as(seller))).andExpect(jsonPath("$.totalElements").value(13));
    }
}

package com.bonsaimarket.backend.review;

import com.bonsaimarket.backend.marketplace.MarketplaceAccess;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Durable, bounded CPU inference jobs; all network work is outside database transactions. */
@Service
public class ReviewSentiments {
    static final String PURCHASE = " join order_items i on i.id=r.order_item_id join orders o on o.id=i.order_id"
        + " join products p on p.id=r.product_id where i.product_id=r.product_id and o.buyer_user_id=r.user_id"
        + " and o.status='COMPLETED' and p.store_id=o.store_id";
    private final JdbcTemplate db;
    private final MarketplaceAccess access;
    private final SentimentSettings settings;
    private final SentimentClient client;
    private final TransactionTemplate tx;
    record Job(long id, String contentVersion, String text, int attempt, String token) {}

    public ReviewSentiments(JdbcTemplate db, MarketplaceAccess access, SentimentSettings settings,
            SentimentClient client, PlatformTransactionManager manager) {
        this.db = db; this.access = access; this.settings = settings; this.client = client;
        tx = new TransactionTemplate(manager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    public static String version(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((text == null ? "" : text).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    // Called only after purchase authorization and the review INSERT, in the same transaction.
    public void enqueue(long reviewId, String text) {
        if (!settings.enabled) return;
        if (!TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException("Review outbox requires a transaction");
        db.update("insert into review_sentiment(review_id,content_version,status,attempts,next_attempt_at,updated_at) values(?,?,'PENDING',0,?,?)",
            reviewId, version(text), access.now(), access.now());
    }

    @Scheduled(fixedDelayString="${sentiment.poll-ms:5000}", initialDelayString="${sentiment.poll-ms:5000}")
    public void tick() {
        if (!settings.enabled) return;
        for (int i = 0; i < 5; i++) if (!processOnce()) break;
    }

    public boolean processOnce() {
        if (!settings.enabled) return false;
        // Never suspend a caller's review transaction and accidentally infer inside its request.
        if (TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException("Inference cannot run in a caller transaction");
        Job job = tx.execute(ignored -> claim());
        if (job == null) return false;
        SentimentClient.Prediction prediction = null;
        String error = null;
        try { prediction = client.predict(job.text()); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); error = "INFERENCE_INTERRUPTED"; }
        catch (Exception failure) { error = "INFERENCE_UNAVAILABLE_OR_INVALID"; }
        final var result = prediction;
        final var code = error;
        tx.executeWithoutResult(ignored -> complete(job, result, code));
        return true;
    }

    private Job claim() {
        LocalDateTime now = access.now();
        var ids = db.query("select review_id from review_sentiment where (status='PENDING' or status='PROCESSING') and next_attempt_at<=? order by next_attempt_at,review_id limit 1",
            (r,n) -> r.getLong(1), now);
        if (ids.isEmpty()) return null;
        long id = ids.getFirst();
        String token = UUID.randomUUID().toString();
        int claimed = db.update("update review_sentiment set status='PROCESSING',lease_token=?,attempts=attempts+1,next_attempt_at=?,updated_at=?"
                + " where review_id=? and (status='PENDING' or status='PROCESSING') and next_attempt_at<=? and attempts<3",
            token, now.plusSeconds(30), now, id, now);
        if (claimed == 0) {
            db.update("update review_sentiment set status='FAILED',label=null,lease_token=null,error_code='LEASE_EXHAUSTED',updated_at=? where review_id=? and attempts>=3 and status in ('PENDING','PROCESSING') and next_attempt_at<=?", now,id,now);
            return null;
        }
        var rows = db.query("select r.comment,s.content_version,s.attempts from reviews r join review_sentiment s on s.review_id=r.id"
                + PURCHASE + " and r.id=?", (r,n) -> new Job(id,r.getString(2),r.getString(1),r.getInt(3),token), id);
        if (rows.isEmpty() || !version(rows.getFirst().text()).equals(rows.getFirst().contentVersion())) {
            db.update("update review_sentiment set status='STALE',label=null,lease_token=null,error_code='CONTENT_CHANGED',updated_at=? where review_id=? and lease_token=?",now,id,token);
            return null;
        }
        return rows.getFirst();
    }

    private void complete(Job job, SentimentClient.Prediction result, String error) {
        // Lock content before checking its fingerprint; edit/delete cannot race result persistence.
        var content = db.query("select comment from reviews where id=? for update", (r,n) -> r.getString(1), job.id());
        if (content.isEmpty()) return; // ON DELETE CASCADE removed the job.
        boolean current = version(content.getFirst()).equals(job.contentVersion());
        boolean purchased = db.queryForObject("select count(*) from reviews r" + PURCHASE + " and r.id=?",Long.class,job.id()) == 1;
        if (!current || !purchased) {
            db.update("update review_sentiment set status='STALE',label=null,lease_token=null,error_code='CONTENT_CHANGED',updated_at=? where review_id=? and lease_token=?",access.now(),job.id(),job.token());
            return;
        }
        String status = result != null ? "ANALYZED" : job.attempt() >= 3 ? "FAILED" : "PENDING";
        db.update("update review_sentiment set status=?,label=?,model_version=?,evaluation_mode=?,demo_only=?,lease_token=null,error_code=?,next_attempt_at=?,updated_at=?"
                + " where review_id=? and status='PROCESSING' and lease_token=? and content_version=?",
            status, result == null ? null : result.label(), result == null ? null : result.modelVersion(),
            result == null ? null : result.evaluationMode(), result == null ? null : result.demoOnly(), error,
            access.now().plusSeconds(30L * job.attempt()), access.now(),job.id(),job.token(),job.contentVersion());
    }
}

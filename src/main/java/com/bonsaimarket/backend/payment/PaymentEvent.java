package com.bonsaimarket.backend.payment;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Minimal immutable evidence; never stores raw callback/query payloads. */
@Entity @Table(name="payment_events",uniqueConstraints=@UniqueConstraint(columnNames="dedup_key"))
public class PaymentEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    private Long transactionId;
    @Column(name="dedup_key",length=64) private String dedupKey;
    @Column(length=30) private String source;
    @Column(length=30) private String outcome;
    private LocalDateTime createdAt;
    protected PaymentEvent(){}
    PaymentEvent(long transaction,String key,String source,String outcome,LocalDateTime now){this.transactionId=transaction;this.dedupKey=key;this.source=source;this.outcome=outcome;this.createdAt=now;}
}

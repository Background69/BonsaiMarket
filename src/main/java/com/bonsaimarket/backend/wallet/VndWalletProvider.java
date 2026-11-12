package com.bonsaimarket.backend.wallet;

/** Contract for a future licensed provider. There is deliberately no production implementation. */
public interface VndWalletProvider {
    record Bank(String reference,String name,String maskedNumber,String status) {}
    record Confirmation(String eventId,String transactionId,long amount,String currency,String outcome,String signature) {}
    Bank linkTestBank();
    Confirmation simulate(String transactionId,long amount,String outcome);
    boolean verify(Confirmation event);
}

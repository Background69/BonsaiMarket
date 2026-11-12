package com.bonsaimarket.backend.wallet;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/** In-process simulator, never an external payment or a production callback endpoint. */
@Component
public class SandboxVndWalletProvider implements VndWalletProvider {
    private final byte[] key=new byte[32];
    public SandboxVndWalletProvider(){new SecureRandom().nextBytes(key);}
    public Bank linkTestBank(){String ref=UUID.randomUUID().toString();return new Bank("SIM:"+ref,"Ngân hàng thử nghiệm","•••• "+ref.substring(0,4),"SIMULATED");}
    public Confirmation simulate(String id,long amount,String outcome){var e=new Confirmation(UUID.randomUUID().toString(),id,amount,"VND",outcome,"");return new Confirmation(e.eventId(),id,amount,"VND",outcome,sign(e));}
    private String sign(Confirmation e){try{var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal((e.eventId()+"|"+e.transactionId()+"|"+e.amount()+"|"+e.currency()+"|"+e.outcome()).getBytes(StandardCharsets.UTF_8)));}catch(GeneralSecurityException ex){throw new IllegalStateException(ex);}}
    public boolean verify(Confirmation e){return e!=null&&e.signature()!=null&&MessageDigest.isEqual(sign(e).getBytes(StandardCharsets.US_ASCII),e.signature().getBytes(StandardCharsets.US_ASCII));}
}

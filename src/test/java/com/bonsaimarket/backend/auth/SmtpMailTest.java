package com.bonsaimarket.backend.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SmtpMailTest {
    @Test void demoRecipientsAreBlockedBeforeAnySmtpInteraction() {
        var mail=mock(JavaMailSender.class);
        for(String name:List.of("admin","seller","user","storefront-v1-vuon-hien","storefront-v1-nang-som",
            "storefront-v1-goc-la","storefront-v1-moc-vien","storefront-v1-nha-chau",
            "discussion-v1-lan","discussion-v1-minh","discussion-v1-an")) {
            for(String purpose:List.of(AuthService.VERIFY,AuthService.RESET))
                assertEquals("DEMO_MAIL_BLOCKED",assertThrows(AuthException.class,
                    ()->sender(mail).send(" "+name.toUpperCase(Locale.ROOT)+"@BONSAI.MARKET ","123456",purpose)).code);
        }
        verifyNoInteractions(mail);
        assertFalse(DemoMailRecipients.blocked("real-user@bonsai.market"));
        assertFalse(DemoMailRecipients.blocked("admin@other.market"));
    }
    @SuppressWarnings("unchecked")
    private SmtpAuthMailSender sender(JavaMailSender mail) {
        ObjectProvider<JavaMailSender> provider=mock(ObjectProvider.class);when(provider.getIfAvailable()).thenReturn(mail);
        var settings=new AuthSettings(Base64.getEncoder().encodeToString(new byte[32]),"sender@example.com","smtp.invalid","http://localhost:5173","http://localhost:5173");
        return new SmtpAuthMailSender(provider,settings);
    }
    private List<String> parts(Part part) throws Exception {
        Object content=part.getContent();List<String> text=new ArrayList<>();
        if(content instanceof Multipart multi) {for(int i=0;i<multi.getCount();i++) text.addAll(parts(multi.getBodyPart(i)));}
        else if(content instanceof String value) text.add(value);return text;
    }
    @Test void mailHasVietnameseSubjectRecipientHtmlAndPlainText() throws Exception {
        var mail=mock(JavaMailSender.class);var message=new MimeMessage(Session.getInstance(new Properties()));when(mail.createMimeMessage()).thenReturn(message);
        sender(mail).send("buyer@example.com","123456",AuthService.VERIFY);message.saveChanges();
        verify(mail).send(message);assertTrue(message.getSubject().contains("xác thực email"));assertEquals("buyer@example.com",message.getAllRecipients()[0].toString());
        var text=parts(message);assertEquals(2,text.size());for(String body:text) {assertTrue(body.contains("123456"));assertTrue(body.contains("10 phút"));assertTrue(body.contains("bỏ qua"));assertTrue(body.contains("BonsaiMarket"));}
    }
    @Test void providerExceptionsNeverEscapeAsCredentialOrOtpMessages() {
        var mail=mock(JavaMailSender.class);var message=new MimeMessage(Session.getInstance(new Properties()));when(mail.createMimeMessage()).thenReturn(message);
        doThrow(new org.springframework.mail.MailSendException("credential-and-123456-must-not-escape")).when(mail).send(any(MimeMessage.class));
        var error=assertThrows(AuthException.class,()->sender(mail).send("buyer@example.com","123456",AuthService.RESET));
        assertEquals("MAIL_DELIVERY_FAILED",error.code);assertFalse(error.getMessage().contains("123456"));assertFalse(error.getMessage().contains("credential"));assertNull(error.getCause());
    }
    @Test void forgotRequestsQueueWithoutLookupAndQueueOverloadFailsUniformly() {
        var executor=mock(java.util.concurrent.Executor.class);var auth=mock(AuthService.class);
        var settings=new AuthSettings(Base64.getEncoder().encodeToString(new byte[32]),"sender@example.com","smtp.invalid","http://localhost:5173","http://localhost:5173");
        var requests=new PasswordResetRequests(executor,auth,settings);requests.request("buyer@example.com");verifyNoInteractions(auth);verify(executor).execute(any(Runnable.class));
        doThrow(new java.util.concurrent.RejectedExecutionException()).when(executor).execute(any());
        assertEquals("AUTH_BUSY",assertThrows(AuthException.class,()->requests.request("unknown@example.com")).code);
    }
}

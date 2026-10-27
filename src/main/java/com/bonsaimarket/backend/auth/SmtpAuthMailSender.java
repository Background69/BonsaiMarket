package com.bonsaimarket.backend.auth;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SmtpAuthMailSender implements AuthMailSender {
    private static final Logger log=LoggerFactory.getLogger(SmtpAuthMailSender.class);
    private final ObjectProvider<JavaMailSender> mail;
    private final AuthSettings settings;
    public SmtpAuthMailSender(ObjectProvider<JavaMailSender> mail,AuthSettings settings) {this.mail=mail;this.settings=settings;}
    @Override public void send(String email,String code,String purpose) {
        settings.requireDelivery();
        String action=purpose.equals("VERIFY_EMAIL")?"xác thực email":"đặt lại mật khẩu";
        try {
            JavaMailSender sender=mail.getIfAvailable();
            if(sender==null) throw new IllegalStateException("Mail sender unavailable");
            var message=sender.createMimeMessage();
            var helper=new MimeMessageHelper(message,true,"UTF-8");
            helper.setFrom(settings.mailFrom); helper.setTo(email); helper.setSubject("BonsaiMarket — Mã "+action);
            String text="Mã "+action+" của bạn: "+code+"\nMã có hiệu lực trong 10 phút và chỉ dùng một lần.\nNếu bạn không thực hiện yêu cầu này, hãy bỏ qua email.\nBonsaiMarket";
            String html="<div style='font-family:Arial,sans-serif;color:#104e35;max-width:520px'><h1 style='font-size:24px'>BonsaiMarket</h1><p>Mã "+action+" của bạn:</p><p style='font-size:32px;letter-spacing:8px;font-weight:700'>"+code+"</p><p>Mã có hiệu lực trong <strong>10 phút</strong> và chỉ dùng một lần.</p><p>Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email.</p></div>";
            helper.setText(text,html); sender.send(message);
        } catch (Exception e) {
            // Never log SMTP exception messages (they may contain credentials or content); class names are safe and enough to diagnose.
            log.warn("SMTP delivery failed: {}",diagnose(e));
            throw new AuthException(503,"MAIL_DELIVERY_FAILED","Không thể gửi email lúc này. Vui lòng thử gửi lại mã.");
        }
    }
    private static String diagnose(Throwable error) {
        var chain=new java.util.ArrayList<String>();
        for(Throwable t=error;t!=null && chain.size()<6;t=t.getCause()) chain.add(t.getClass().getSimpleName());
        String path=String.join(" > ",chain);
        String hint;
        if(path.contains("AuthenticationFailed")) hint="SMTP rejected the login: check MAIL_USERNAME / MAIL_PASSWORD (Gmail needs a 16-character App Password, not the account password).";
        else if(path.contains("MailConnect") || path.contains("ConnectException") || path.contains("UnknownHost") || path.contains("SocketTimeout"))
            hint="Cannot reach the SMTP server: check MAIL_HOST / MAIL_PORT, network or firewall.";
        else if(path.contains("SSL") || path.contains("Certificate")) hint="TLS handshake failed: port 587 needs MAIL_SMTP_STARTTLS=true; port 465 needs MAIL_SMTP_SSL=true and MAIL_SMTP_STARTTLS=false.";
        else if(path.contains("SendFailed") || path.contains("Address")) hint="The server refused the sender or recipient: MAIL_FROM must be an address the SMTP account is allowed to send as.";
        else if(path.contains("IllegalState")) hint="No JavaMailSender bean: spring.mail.host (MAIL_HOST) is empty.";
        else hint="See the exception chain.";
        return path+" — "+hint;
    }
}

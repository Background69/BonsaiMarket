package com.bonsaimarket.backend.auth;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.*;

@Service
public class SmtpAuthMailSender implements AuthMailSender {
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
            // Never log SMTP exceptions: a provider may include credentials or message contents.
            throw new AuthException(503,"MAIL_DELIVERY_FAILED","Không thể gửi email lúc này. Vui lòng thử gửi lại mã.");
        }
    }
}

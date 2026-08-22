package com.memories.platform.auth.service;

import com.memories.platform.auth.dto.VerificationEmail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpVerificationEmailSender implements VerificationEmailSender {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpVerificationEmailSender(
            JavaMailSender mailSender,
            @Value("${platform.mail.from}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void send(VerificationEmail email) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email.recipient());
        message.setSubject("Xác thực tài khoản Memories");
        message.setText(
                "Mở liên kết sau để xác thực tài khoản của bạn:\n"
                        + email.verificationUri()
                        + "\n\nLiên kết hết hạn lúc "
                        + email.expiresAt()
                        + "."
        );
        mailSender.send(message);
    }
}

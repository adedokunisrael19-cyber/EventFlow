package com.eventflow.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "eventflow.email", name = "enabled", havingValue = "true")
public class SmtpVerificationEmailSender implements VerificationEmailSender {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String applicationBaseUrl;

    public SmtpVerificationEmailSender(
            JavaMailSender mailSender,
            @Value("${eventflow.email.from}") String fromAddress,
            @Value("${eventflow.email.application-base-url}") String applicationBaseUrl
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.applicationBaseUrl = applicationBaseUrl;
    }

    @Override
    public void send(String email, String token) {
        String verificationUrl = applicationBaseUrl + "/verify-email?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("Verify your EventFlow account");
        message.setText("Use this link to verify your email address. It expires in 24 hours:\n\n"
                + verificationUrl + "\n\nIf you did not create this account, you can ignore this email.");
        mailSender.send(message);
    }
}

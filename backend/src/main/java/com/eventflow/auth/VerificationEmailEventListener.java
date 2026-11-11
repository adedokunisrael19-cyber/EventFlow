package com.eventflow.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class VerificationEmailEventListener {

    private static final Logger logger = LoggerFactory.getLogger(VerificationEmailEventListener.class);

    private final ObjectProvider<VerificationEmailSender> senderProvider;

    public VerificationEmailEventListener(ObjectProvider<VerificationEmailSender> senderProvider) {
        this.senderProvider = senderProvider;
    }

    @TransactionalEventListener
    public void sendAfterRegistrationCommits(VerificationEmailRequested event) {
        VerificationEmailSender sender = senderProvider.getIfAvailable();
        if (sender == null) {
            logger.warn("Verification email was not sent because SMTP is not configured.");
            return;
        }

        try {
            sender.send(event.email(), event.token());
        } catch (RuntimeException exception) {
            // Registration has already committed. Do not log the token or provider details that may include PII.
            logger.warn("Verification email delivery failed. Configure SMTP and add a resend flow.");
        }
    }
}

package com.app.trekha.common.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String fromNumber;

    @PostConstruct
    public void initTwilio() {
        // Check if credentials are provided before initializing
        if (accountSid != null && !accountSid.startsWith("${")) {
            Twilio.init(accountSid, authToken);
            logger.info("Twilio SMS service initialized.");
        } else {
            logger.warn("Twilio credentials not found. SMS service will be disabled. Please set TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, and TWILIO_PHONE_NUMBER environment variables.");
        }
    }

    public void sendSms(String to, String body) {
        try {
            Message.creator(new PhoneNumber(to), new PhoneNumber(fromNumber), body).create();
            logger.info("SMS sent successfully to {}", to);
        } catch (Exception e) {
            logger.error("Failed to send SMS to {}: {}", to, e.getMessage());
            // In a real application, you might want to throw a custom exception here
            // to be handled by a global exception handler.
        }
    }

}

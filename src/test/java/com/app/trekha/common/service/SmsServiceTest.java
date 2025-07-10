package com.app.trekha.common.service;

import com.app.trekha.common.service.SmsService;
import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmsServiceTest {

    @InjectMocks
    private SmsService smsService;

    private MockedStatic<Twilio> mockedTwilio;
    private MockedStatic<Message> mockedMessage;

    @BeforeEach
    void setUp() {
        // Mock the static methods before each test
        mockedTwilio = Mockito.mockStatic(Twilio.class);
        mockedMessage = Mockito.mockStatic(Message.class);
    }

    @AfterEach
    void tearDown() {
        // Close the static mocks after each test to avoid leakage
        mockedTwilio.close();
        mockedMessage.close();
    }

    @Test
    void initTwilioWithValidCredentialsShouldInitializeTwilio() {
        // Arrange
        ReflectionTestUtils.setField(smsService, "accountSid", "ACxxxxxxxxxxxx");
        ReflectionTestUtils.setField(smsService, "authToken", "test_auth_token");

        // Act
        smsService.initTwilio();

        // Assert
        mockedTwilio.verify(() -> Twilio.init("ACxxxxxxxxxxxx", "test_auth_token"));
    }

    @Test
    void initTwilioWithMissingCredentialsShouldNotInitializeTwilio() {
        // Arrange
        ReflectionTestUtils.setField(smsService, "accountSid", null);

        // Act
        smsService.initTwilio();

        // Assert
        mockedTwilio.verifyNoInteractions();
    }

    @Test
    void sendSmsWithValidInputShouldCallTwilioMessageCreator() {
        // Arrange
        String to = "+15005550006"; // Use a valid test number format
        String from = "+15005550001";
        String body = "Test SMS message";
        ReflectionTestUtils.setField(smsService, "fromNumber", from);

        // No need to mock Message.creator, we'll just verify its call

        // Act
        smsService.sendSms(to, body);
        
        // Assert
        ArgumentCaptor<PhoneNumber> toCaptor = ArgumentCaptor.forClass(PhoneNumber.class);
        ArgumentCaptor<PhoneNumber> fromCaptor = ArgumentCaptor.forClass(PhoneNumber.class);
        mockedMessage.verify(() -> Message.creator(toCaptor.capture(), fromCaptor.capture(), eq(body)));
        assertEquals(to, toCaptor.getValue().toString());
        assertEquals(from, fromCaptor.getValue().toString());
    }

    @Test
    void sendSmsWhenTwilioThrowsExceptionShouldLogAndNotThrow() {
        // Arrange
        mockedMessage.when(() -> Message.creator(
                (PhoneNumber) any(PhoneNumber.class),
                (PhoneNumber) any(PhoneNumber.class),
                any(String.class)))
            .thenThrow(new ApiException("Twilio API error"));

        // Act & Assert
        assertDoesNotThrow(() -> smsService.sendSms("+123", "test"), "The service should handle the exception gracefully.");
    }
}
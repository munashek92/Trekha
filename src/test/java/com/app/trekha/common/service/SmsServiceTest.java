package com.app.trekha.common.service;

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
    void initTwilio_withValidCredentials_shouldInitializeTwilio() {
        // Arrange
        ReflectionTestUtils.setField(smsService, "accountSid", "ACxxxxxxxxxxxx");
        ReflectionTestUtils.setField(smsService, "authToken", "test_auth_token");

        // Act
        smsService.initTwilio();

        // Assert
        mockedTwilio.verify(() -> Twilio.init("ACxxxxxxxxxxxx", "test_auth_token"));
    }

    @Test
    void initTwilio_withMissingCredentials_shouldNotInitializeTwilio() {
        // Arrange
        ReflectionTestUtils.setField(smsService, "accountSid", null);

        // Act
        smsService.initTwilio();

        // Assert
        mockedTwilio.verifyNoInteractions();
    }

    @Test
    void sendSms_withValidInput_shouldCallTwilioMessageCreator() {
        // Arrange
        String to = "+15005550006"; // Use a valid test number format
        String from = "+15005550001";
        String body = "Test SMS message";
        ReflectionTestUtils.setField(smsService, "fromNumber", from);

        Message.Creator creator = mock(Message.Creator.class);
        mockedMessage.when(() -> Message.creator(any(PhoneNumber.class), any(PhoneNumber.class), any(String.class)))
                .thenReturn(creator);

        // Act
        smsService.sendSms(to, body);

        // Assert
        ArgumentCaptor<PhoneNumber> toCaptor = ArgumentCaptor.forClass(PhoneNumber.class);
        ArgumentCaptor<PhoneNumber> fromCaptor = ArgumentCaptor.forClass(PhoneNumber.class);
        mockedMessage.verify(() -> Message.creator(toCaptor.capture(), fromCaptor.capture(), eq(body)));
        assertEquals(to, toCaptor.getValue().toString());
        assertEquals(from, fromCaptor.getValue().toString());
        verify(creator).create();
    }

    @Test
    void sendSms_whenTwilioThrowsException_shouldLogAndNotThrow() {
        // Arrange
        Message.Creator creator = mock(Message.Creator.class);
        when(creator.create()).thenThrow(new ApiException("Twilio API error"));
        mockedMessage.when(() -> Message.creator(any(), any(), any())).thenReturn(creator);

        // Act & Assert
        assertDoesNotThrow(() -> smsService.sendSms("+123", "test"), "Service should handle the exception gracefully.");
    }
}
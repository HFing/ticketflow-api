package com.hfing.ticketflowapi.notification.consumer;

import com.hfing.ticketflowapi.notification.dto.PaymentCompletedEvent;
import com.hfing.ticketflowapi.notification.service.INotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentCompletedConsumerTest {
    @Mock private INotificationService notificationService;

    @Test
    void delegatesPaymentEmailToNotificationService() {
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                "payment-1",
                "booking-1",
                "customer@example.com",
                "Test",
                "Customer",
                "Live Concert",
                "Ho Chi Minh City",
                "TicketFlow Arena",
                LocalDateTime.parse("2026-07-23T19:00:00"),
                LocalDateTime.parse("2026-07-23T21:00:00"),
                new BigDecimal("300000"),
                "VND",
                Instant.parse("2026-07-22T03:00:00Z"),
                List.of());

        new PaymentCompletedConsumer(notificationService).consume(event);

        verify(notificationService).sendPaymentConfirmationEmail(event);
    }
}

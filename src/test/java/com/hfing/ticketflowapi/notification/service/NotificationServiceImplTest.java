package com.hfing.ticketflowapi.notification.service;

import com.hfing.ticketflowapi.notification.dto.PaidTicketInfo;
import com.hfing.ticketflowapi.notification.dto.PaymentCompletedEvent;
import com.hfing.ticketflowapi.notification.service.impl.NotificationServiceImpl;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {
    @Mock private JavaMailSender mailSender;

    @Test
    void paymentEmailContainsEventAndIndividualTicketInformation() throws Exception {
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);
        NotificationServiceImpl service = new NotificationServiceImpl(mailSender);
        ReflectionTestUtils.setField(service, "fromAddress", "no-reply@ticketflow.com");
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
                List.of(
                        new PaidTicketInfo("TKT-ONE", "VIP"),
                        new PaidTicketInfo("TKT-TWO", "VIP")));

        service.sendPaymentConfirmationEmail(event);

        verify(mailSender).send(message);
        String html = message.getContent().toString();
        assertThat(html)
                .contains("Live Concert", "booking-1", "TKT-ONE", "TKT-TWO", "VIP", "300.000");
    }
}

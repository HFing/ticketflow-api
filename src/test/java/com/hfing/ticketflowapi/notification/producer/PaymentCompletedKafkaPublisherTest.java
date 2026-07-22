package com.hfing.ticketflowapi.notification.producer;

import com.hfing.ticketflowapi.notification.dto.PaymentCompletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.hfing.ticketflowapi.common.config.KafkaTopicConfiguration.PAYMENT_COMPLETED_TOPIC;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCompletedKafkaPublisherTest {
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void publishesPaymentEventUsingBookingIdAsKafkaKey() {
        PaymentCompletedEvent event = event();
        CompletableFuture<SendResult<String, Object>> result =
                CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(PAYMENT_COMPLETED_TOPIC, "booking-1", event))
                .thenReturn(result);

        new PaymentCompletedKafkaPublisher(kafkaTemplate).publishAfterCommit(event);

        verify(kafkaTemplate).send(PAYMENT_COMPLETED_TOPIC, "booking-1", event);
    }

    private PaymentCompletedEvent event() {
        return new PaymentCompletedEvent(
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
    }
}

package com.hfing.ticketflowapi.notification.consumer;

import com.hfing.ticketflowapi.notification.dto.PaymentCompletedEvent;
import com.hfing.ticketflowapi.notification.service.INotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.hfing.ticketflowapi.common.config.KafkaTopicConfiguration.PAYMENT_COMPLETED_TOPIC;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "PAYMENT-COMPLETED-CONSUMER")
public class PaymentCompletedConsumer {
    private final INotificationService notificationService;

    @KafkaListener(topics = PAYMENT_COMPLETED_TOPIC, groupId = "ticketflow-payment-mail-group")
    public void consume(PaymentCompletedEvent event) {
        log.info("Consumed payment completed event for bookingId={}", event.bookingId());
        notificationService.sendPaymentConfirmationEmail(event);
    }
}

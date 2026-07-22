package com.hfing.ticketflowapi.notification.producer;

import com.hfing.ticketflowapi.notification.dto.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.hfing.ticketflowapi.common.config.KafkaTopicConfiguration.PAYMENT_COMPLETED_TOPIC;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "PAYMENT-COMPLETED-PUBLISHER")
public class PaymentCompletedKafkaPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishAfterCommit(PaymentCompletedEvent event) {
        try {
            kafkaTemplate.send(PAYMENT_COMPLETED_TOPIC, event.bookingId(), event)
                    .whenComplete((result, exception) -> {
                        if (exception == null) {
                            log.info("Published payment completed event for bookingId={}", event.bookingId());
                        } else {
                            log.error("Failed to publish payment completed event for bookingId={}",
                                    event.bookingId(), exception);
                        }
                    });
        } catch (RuntimeException exception) {
            log.error("Failed to enqueue payment completed event for bookingId={}",
                    event.bookingId(), exception);
        }
    }
}

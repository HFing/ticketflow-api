package com.hfing.ticketflowapi.notification.consumer;

import com.hfing.ticketflowapi.notification.dto.UserRegisteredEvent;
import com.hfing.ticketflowapi.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j(topic = "USER-REGISTRATION-CONSUMER")
public class UserRegistrationConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "user-registration", groupId = "ticketflow-mail-group")
    public void consume(UserRegisteredEvent event) {
        log.info("Consumed user registration event: {}", event);
        try {
            notificationService.sendRegistrationEmail(event);
        } catch (Exception e) {
            log.error("Failed to process consumed event for email: {}", event.email(), e);
        }
    }
}
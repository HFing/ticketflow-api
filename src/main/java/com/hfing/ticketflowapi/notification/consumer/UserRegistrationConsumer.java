package com.hfing.ticketflowapi.notification.consumer;

import com.hfing.ticketflowapi.notification.dto.UserRegisteredEvent;
import com.hfing.ticketflowapi.notification.service.INotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.hfing.ticketflowapi.common.config.KafkaTopicConfiguration.USER_REGISTRATION_TOPIC;


@Component
@RequiredArgsConstructor
@Slf4j(topic = "USER-REGISTRATION-CONSUMER")
public class UserRegistrationConsumer {

    private final INotificationService notificationService;

    @KafkaListener(topics = USER_REGISTRATION_TOPIC, groupId = "ticketflow-mail-group")
    public void consume(UserRegisteredEvent event) {
        log.info("Consumed user registration event: {}", event);
        try {
            notificationService.sendRegistrationEmail(event);
        } catch (Exception e) {
            log.error("Failed to process consumed event for email: {}", event.email(), e);
        }
    }
}

package com.hfing.ticketflowapi.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;


@Configuration
public class KafkaTopicConfiguration {
    public static final String USER_REGISTRATION_TOPIC = "user-registration";
    public static final String PAYMENT_COMPLETED_TOPIC = "payment-completed";

    @Bean
    public NewTopic userRegistrationTopic() {
        return TopicBuilder.name(USER_REGISTRATION_TOPIC)
                .partitions(3)
                .replicas(2)
                .build();
    }

    @Bean
    public NewTopic paymentCompletedTopic() {
        return TopicBuilder.name(PAYMENT_COMPLETED_TOPIC)
                .partitions(3)
                .replicas(2)
                .build();
    }
}

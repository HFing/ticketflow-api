package com.hfing.ticketflowapi.payment.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(PaymentProperties.class)
public class PaymentConfiguration {
    @Bean
    Clock paymentClock() {
        return Clock.systemDefaultZone();
    }
}

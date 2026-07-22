package com.hfing.ticketflowapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableJpaAuditing
@EnableCaching
@EnableScheduling
@SpringBootApplication
public class TicketflowApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketflowApiApplication.class, args);
    }

}

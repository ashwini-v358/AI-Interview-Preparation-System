package com.smartinterview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main entry point for the Smart Interview Platform backend.
 *
 * Concepts introduced:
 * - @SpringBootApplication: Combines @Configuration, @EnableAutoConfiguration, and @ComponentScan.
 * - @EnableJpaAuditing: Automatically populates @CreatedDate and @LastModifiedDate fields on JPA entities.
 */
@SpringBootApplication
@EnableJpaAuditing
public class SmartInterviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartInterviewApplication.class, args);
    }
}

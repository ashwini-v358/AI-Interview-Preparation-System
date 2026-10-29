package com.smartinterview;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring Boot integration test to verify ApplicationContext initialization.
 *
 * Concepts introduced:
 * - @SpringBootTest: Boots the entire Spring application context in test mode.
 * - @ActiveProfiles("local"): Uses the in-memory H2 profile during tests to avoid
 *   requiring external services.
 * - JUnit 5: Standard Java testing framework.
 */
@SpringBootTest
@ActiveProfiles("local")
class SmartInterviewApplicationTests {

    @Test
    @DisplayName("Application context loads successfully")
    void contextLoads() {
        assertTrue(true, "Spring context initialized without exceptions");
    }
}

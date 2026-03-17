package com.uzima.bootstrap.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Configuration Spring pour les tests Cucumber.
 * Démarre un serveur Spring Boot complet sur un port aléatoire
 * avec une base PostgreSQL Testcontainers isolée.
 *
 * Utilisé par tous les step definitions via @CucumberContextConfiguration.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers
public class CucumberSpringConfiguration {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("uzima_bdd_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Flyway applique les migrations V1..V5 sur ce container de test
        registry.add("spring.flyway.enabled", () -> "true");
        // Désactive Socket.IO en test (pas de port WebSocket à allouer)
        registry.add("uzima.websocket.enabled", () -> "false");
    }

    @LocalServerPort
    protected int serverPort;

    protected String baseUrl() {
        return "http://localhost:" + serverPort;
    }
}

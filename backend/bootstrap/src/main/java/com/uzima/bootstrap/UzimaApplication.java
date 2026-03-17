package com.uzima.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée Spring Boot.
 *
 * Ce module est le seul à connaître Spring Boot.
 * Le domaine et l'application compilent et fonctionnent SANS Spring.
 */
@SpringBootApplication
public class UzimaApplication {

    public static void main(String[] args) {
        SpringApplication.run(UzimaApplication.class, args);
    }
}

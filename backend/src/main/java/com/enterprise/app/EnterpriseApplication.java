package com.enterprise.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Point d'entrée de l'API enterprise.
 *
 * <p>Architecture par domaines : {@code controller -> service -> repository -> entity},
 * DTOs (records) exposés par les contrôleurs, jamais les entités JPA.</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class EnterpriseApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnterpriseApplication.class, args);
    }
}

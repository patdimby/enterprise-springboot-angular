package com.enterprise.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * POINT D'ENTRÉE de toute l'application — c'est ici que la JVM démarre.
 *
 * <p>{@code @SpringBootApplication} regroupe 3 annotations :</p>
 * <ul>
 *   <li>{@code @Configuration} : cette classe peut déclarer des beans ;</li>
 *   <li>{@code @EnableAutoConfiguration} : Spring Boot configure tout seul
 *       la base de données, la sécurité, Swagger... selon ce qu'il trouve
 *       dans le pom.xml et les fichiers application*.yml ;</li>
 *   <li>{@code @ComponentScan} : il cherche dans ce package et tous ses
 *       sous-packages (com.enterprise.app.*) les classes marquées
 *       @Service, @Component, @RestController... et les enregistre.</li>
 * </ul>
 *
 * <p>{@code @ConfigurationPropertiesScan} : charge en plus les classes de
 * configuration typée comme AppProperties (préfixe "app" dans le yml).</p>
 *
 * <p>Architecture du projet, organisée par domaines :
 * {@code Controller (HTTP) → Service (logique) → Repository (SQL) → Entité (table)},
 * avec des DTO (records) en entrée/sortie — jamais les entités JPA exposées.
 * Voir ARCHITECTURE.md pour la vue d'ensemble.</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class EnterpriseApplication {

    /**
     * Le main classique : démarre le serveur web embarqué (Tomcat),
     * initialise Spring, la connexion MySQL du profil actif... puis écoute
     * sur le port 8080.
     */
    public static void main(String[] args) {
        SpringApplication.run(EnterpriseApplication.class, args);
    }
}

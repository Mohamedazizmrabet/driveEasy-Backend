package tn.sesame.driveeasy.vehicule.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriétés métier externalisées dans le Config Server (config-repo/vehicule-service.yml).
 *
 * @param anneeMinimale année de fabrication la plus ancienne acceptée
 * @param donneesDemo   charge un jeu de données de démonstration au démarrage
 */
@ConfigurationProperties(prefix = "driveeasy.vehicule")
public record VehiculeProperties(Integer anneeMinimale, Boolean donneesDemo) {
    public VehiculeProperties {
        if (anneeMinimale == null) anneeMinimale = 1990;
        if (donneesDemo == null) donneesDemo = true;
    }
}

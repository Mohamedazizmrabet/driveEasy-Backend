package tn.sesame.driveeasy.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriétés métier externalisées (config-repo/client-service.yml).
 *
 * @param ageMinimum  âge minimum pour louer
 * @param donneesDemo charge un jeu de données de démonstration au démarrage
 */
@ConfigurationProperties(prefix = "driveeasy.client")
public record ClientProperties(Integer ageMinimum, Boolean donneesDemo) {
    public ClientProperties {
        if (ageMinimum == null) ageMinimum = 21;
        if (donneesDemo == null) donneesDemo = true;
    }
}

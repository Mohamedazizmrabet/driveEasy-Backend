package tn.sesame.driveeasy.location.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Règles tarifaires et de fidélité externalisées (config-repo/location-service.yml).
 *
 * @param remiseFidele          taux de remise d'un client FIDELE (0.05 = 5 %)
 * @param remiseVip             taux de remise d'un client VIP
 * @param coefficientPenalite   pénalité par jour de retard = coefficient × prix journalier
 * @param coefficientsCategorie coefficient de prix par catégorie de véhicule (1.0 par défaut)
 * @param seuilFidele           nombre de locations terminées pour passer FIDELE
 * @param seuilVip              nombre de locations terminées pour passer VIP
 * @param taillePageDefaut      taille de page par défaut de GET /locations
 * @param donneesDemo           charge un historique de démonstration au démarrage
 */
@ConfigurationProperties(prefix = "driveeasy.location")
public record LocationProperties(BigDecimal remiseFidele, BigDecimal remiseVip, BigDecimal coefficientPenalite,
                                 Map<String, BigDecimal> coefficientsCategorie, Integer seuilFidele,
                                 Integer seuilVip, Integer taillePageDefaut, Boolean donneesDemo) {
    public LocationProperties {
        if (remiseFidele == null) remiseFidele = new BigDecimal("0.05");
        if (remiseVip == null) remiseVip = new BigDecimal("0.10");
        if (coefficientPenalite == null) coefficientPenalite = new BigDecimal("1.5");
        if (coefficientsCategorie == null) coefficientsCategorie = Map.of();
        if (seuilFidele == null) seuilFidele = 5;
        if (seuilVip == null) seuilVip = 15;
        if (taillePageDefaut == null) taillePageDefaut = 10;
        if (donneesDemo == null) donneesDemo = true;
    }
}

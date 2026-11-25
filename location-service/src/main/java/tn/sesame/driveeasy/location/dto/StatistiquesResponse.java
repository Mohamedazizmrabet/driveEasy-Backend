package tn.sesame.driveeasy.location.dto;

import tn.sesame.driveeasy.location.entity.StatutLocation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Indicateurs : chiffre d'affaires, taux d'occupation du parc, véhicules les plus loués. */
public record StatistiquesResponse(
        BigDecimal chiffreAffaires, BigDecimal chiffreAffairesMois, long nombreLocations,
        Map<StatutLocation, Long> locationsParStatut, Integer totalVehicules,
        Map<String, Long> vehiculesParStatut, Double tauxOccupation, List<TopVehicule> topVehicules,
        List<String> avertissements) {

    public record TopVehicule(Long vehiculeId, String libelle, String immatriculation, long nombreLocations,
                              BigDecimal chiffreAffaires) {
    }
}

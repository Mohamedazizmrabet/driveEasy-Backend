package tn.sesame.driveeasy.vehicule.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import tn.sesame.driveeasy.vehicule.entity.StatutVehicule;

/**
 * Changement de statut (appelé par location-service).
 * Le kilométrage est optionnel : il est transmis au retour du véhicule.
 */
public record StatutRequest(
        @NotNull(message = "Le statut est obligatoire") StatutVehicule statut,
        @PositiveOrZero(message = "Le kilométrage ne peut pas être négatif") Integer kilometrage) {
}

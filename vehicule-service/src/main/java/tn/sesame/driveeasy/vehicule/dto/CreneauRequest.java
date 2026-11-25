package tn.sesame.driveeasy.vehicule.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Blocage d'une période sur un véhicule pour une location donnée. */
public record CreneauRequest(
        @NotNull(message = "locationId est obligatoire") Long locationId,
        @NotNull(message = "dateDebut est obligatoire") LocalDate dateDebut,
        @NotNull(message = "dateFin est obligatoire") LocalDate dateFin) {
}

package tn.sesame.driveeasy.location.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

/** Retour du véhicule ; dateRetour vaut aujourd'hui si elle est omise. */
public record RetourRequest(
        @NotNull(message = "Le kilométrage de retour est obligatoire")
        @PositiveOrZero(message = "Le kilométrage ne peut pas être négatif")
        Integer kmRetour,
        LocalDate dateRetour) {
}

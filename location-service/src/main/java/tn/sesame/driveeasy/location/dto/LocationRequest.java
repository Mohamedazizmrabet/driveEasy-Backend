package tn.sesame.driveeasy.location.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Demande de réservation (ou de simulation de prix). */
public record LocationRequest(
        @NotNull(message = "clientId est obligatoire") Long clientId,
        @NotNull(message = "vehiculeId est obligatoire") Long vehiculeId,
        @NotNull(message = "La date de début est obligatoire")
        @FutureOrPresent(message = "La date de début ne peut pas être dans le passé")
        LocalDate dateDebut,
        @NotNull(message = "La date de fin est obligatoire") LocalDate dateFin) {
}

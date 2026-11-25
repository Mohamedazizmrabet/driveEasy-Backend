package tn.sesame.driveeasy.client.dto;

import jakarta.validation.constraints.*;
import tn.sesame.driveeasy.client.entity.CategorieClient;

import java.time.LocalDate;

/** Données reçues pour créer ou modifier un client. */
public record ClientRequest(
        @NotBlank(message = "Le CIN est obligatoire")
        @Pattern(regexp = "\\d{8}", message = "Le CIN doit contenir exactement 8 chiffres")
        String cin,
        @NotBlank(message = "Le nom est obligatoire") String nom,
        @NotBlank(message = "Le prénom est obligatoire") String prenom,
        @NotNull(message = "La date de naissance est obligatoire")
        @Past(message = "La date de naissance doit être dans le passé")
        LocalDate dateNaissance,
        @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") String email,
        @Pattern(regexp = "^\\+?[0-9 ]{8,15}$", message = "Numéro de téléphone invalide") String telephone,
        String adresse,
        @NotBlank(message = "Le numéro de permis est obligatoire") String numeroPermis,
        @NotNull(message = "La date d'expiration du permis est obligatoire") LocalDate dateExpirationPermis,
        CategorieClient categorieClient) {
}

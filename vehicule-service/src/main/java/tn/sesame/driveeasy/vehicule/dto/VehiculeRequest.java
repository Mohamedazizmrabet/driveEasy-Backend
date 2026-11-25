package tn.sesame.driveeasy.vehicule.dto;

import jakarta.validation.constraints.*;
import tn.sesame.driveeasy.vehicule.entity.Boite;
import tn.sesame.driveeasy.vehicule.entity.Carburant;
import tn.sesame.driveeasy.vehicule.entity.Categorie;
import tn.sesame.driveeasy.vehicule.entity.StatutVehicule;

import java.math.BigDecimal;

/** Données reçues pour créer ou modifier un véhicule. */
public record VehiculeRequest(
        @NotBlank(message = "L'immatriculation est obligatoire")
        @Size(max = 20, message = "L'immatriculation ne doit pas dépasser 20 caractères")
        String immatriculation,
        @NotBlank(message = "La marque est obligatoire") String marque,
        @NotBlank(message = "Le modèle est obligatoire") String modele,
        @NotNull(message = "L'année est obligatoire") Integer annee,
        String couleur,
        @NotNull(message = "La catégorie est obligatoire") Categorie categorie,
        @NotNull(message = "Le carburant est obligatoire") Carburant carburant,
        @NotNull(message = "Le type de boîte est obligatoire") Boite boite,
        @NotNull(message = "Le nombre de places est obligatoire")
        @Min(value = 1, message = "Au moins 1 place") @Max(value = 9, message = "Au plus 9 places")
        Integer nbPlaces,
        @NotNull(message = "Le kilométrage est obligatoire")
        @PositiveOrZero(message = "Le kilométrage ne peut pas être négatif")
        Integer kilometrage,
        @NotNull(message = "Le prix journalier est obligatoire")
        @Positive(message = "Le prix journalier doit être strictement positif")
        BigDecimal prixJournalier,
        StatutVehicule statut,
        String imageUrl) {
}

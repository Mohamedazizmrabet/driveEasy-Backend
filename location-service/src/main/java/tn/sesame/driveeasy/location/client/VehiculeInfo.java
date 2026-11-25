package tn.sesame.driveeasy.location.client;

import java.math.BigDecimal;

/** Vue partielle d'un véhicule renvoyé par vehicule-service. */
public record VehiculeInfo(Long id, String immatriculation, String marque, String modele, String categorie,
                           String carburant, int kilometrage, BigDecimal prixJournalier, String statut,
                           String imageUrl) {
}

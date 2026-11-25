package tn.sesame.driveeasy.vehicule.dto;

import tn.sesame.driveeasy.vehicule.entity.*;

import java.math.BigDecimal;

/** Représentation d'un véhicule renvoyée par l'API (séparée de l'entité). */
public record VehiculeResponse(
        Long id, String immatriculation, String marque, String modele, int annee, String couleur,
        Categorie categorie, Carburant carburant, Boite boite, int nbPlaces, int kilometrage,
        BigDecimal prixJournalier, StatutVehicule statut, String imageUrl) {

    public static VehiculeResponse from(Vehicule v) {
        return new VehiculeResponse(v.getId(), v.getImmatriculation(), v.getMarque(), v.getModele(),
                v.getAnnee(), v.getCouleur(), v.getCategorie(), v.getCarburant(), v.getBoite(),
                v.getNbPlaces(), v.getKilometrage(), v.getPrixJournalier(), v.getStatut(), v.getImageUrl());
    }
}

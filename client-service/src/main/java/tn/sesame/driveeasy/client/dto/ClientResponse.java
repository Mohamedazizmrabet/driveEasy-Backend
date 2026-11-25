package tn.sesame.driveeasy.client.dto;

import tn.sesame.driveeasy.client.entity.CategorieClient;
import tn.sesame.driveeasy.client.entity.Client;

import java.time.LocalDate;
import java.time.Period;

/** Représentation d'un client renvoyée par l'API, avec des champs calculés (âge, permis expiré). */
public record ClientResponse(
        Long id, String cin, String nom, String prenom, LocalDate dateNaissance, int age, String email,
        String telephone, String adresse, String numeroPermis, LocalDate dateExpirationPermis,
        boolean permisExpire, CategorieClient categorieClient, LocalDate dateInscription) {

    public static ClientResponse from(Client c) {
        LocalDate today = LocalDate.now();
        return new ClientResponse(c.getId(), c.getCin(), c.getNom(), c.getPrenom(), c.getDateNaissance(),
                Period.between(c.getDateNaissance(), today).getYears(), c.getEmail(), c.getTelephone(),
                c.getAdresse(), c.getNumeroPermis(), c.getDateExpirationPermis(),
                c.getDateExpirationPermis().isBefore(today), c.getCategorieClient(), c.getDateInscription());
    }
}

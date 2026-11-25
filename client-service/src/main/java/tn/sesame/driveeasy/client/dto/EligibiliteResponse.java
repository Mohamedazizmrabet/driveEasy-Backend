package tn.sesame.driveeasy.client.dto;

import tn.sesame.driveeasy.client.entity.CategorieClient;

import java.util.List;

/** Résultat du contrôle d'éligibilité (âge minimum et permis valide). */
public record EligibiliteResponse(Long clientId, boolean eligible, int age, boolean permisValide,
                                  CategorieClient categorieClient, List<String> motifs) {
}

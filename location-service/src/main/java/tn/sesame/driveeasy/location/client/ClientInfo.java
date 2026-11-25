package tn.sesame.driveeasy.location.client;

/** Vue partielle d'un client renvoyé par client-service (les champs inutiles sont ignorés). */
public record ClientInfo(Long id, String cin, String nom, String prenom, String email, String telephone,
                         String categorieClient, boolean permisExpire) {
}

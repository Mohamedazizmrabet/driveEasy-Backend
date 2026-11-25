package tn.sesame.driveeasy.client.dto;

/** Réponse de location-service : nombre de locations RESERVEE ou EN_COURS d'un client. */
public record LocationsActivesResponse(Long clientId, long locationsActives) {
}

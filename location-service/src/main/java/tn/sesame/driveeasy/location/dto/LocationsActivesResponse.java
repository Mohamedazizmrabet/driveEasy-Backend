package tn.sesame.driveeasy.location.dto;

/** Nombre de locations RESERVEE ou EN_COURS d'un client (utilisé par client-service). */
public record LocationsActivesResponse(Long clientId, long locationsActives) {
}

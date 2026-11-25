package tn.sesame.driveeasy.location.dto;

import tn.sesame.driveeasy.location.client.ClientInfo;
import tn.sesame.driveeasy.location.client.VehiculeInfo;

import java.util.List;

/**
 * Détail enrichi : la location + le client et le véhicule récupérés auprès des autres services.
 * Si un service est indisponible, la partie concernée est null et un avertissement est ajouté.
 */
public record LocationDetailResponse(LocationResponse location, ClientInfo client, VehiculeInfo vehicule,
                                     List<String> avertissements) {
}

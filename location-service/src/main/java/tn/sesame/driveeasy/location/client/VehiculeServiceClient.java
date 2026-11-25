package tn.sesame.driveeasy.location.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.*;

import java.util.List;

/**
 * Client déclaratif vers vehicule-service (même principe que EmployeeClient dans le TP) :
 * on déclare seulement les méthodes, Spring génère le code HTTP (voir WebClientConfig).
 * Les dates sont passées au format ISO (yyyy-MM-dd).
 */
@HttpExchange("/vehicules")
public interface VehiculeServiceClient {

    @GetExchange("/{id}")
    VehiculeInfo getVehicule(@PathVariable Long id);

    @GetExchange
    List<VehiculeInfo> getVehicules();

    @GetExchange("/{id}/disponibilite")
    DisponibiliteInfo getDisponibilite(@PathVariable Long id,
                                       @RequestParam String dateDebut,
                                       @RequestParam String dateFin);

    /** Départ (LOUE) ou retour (DISPONIBLE + kilométrage). */
    @PatchExchange("/{id}/statut")
    VehiculeInfo changerStatut(@PathVariable Long id, @RequestBody StatutRequest request);

    /** Bloque la période sur le véhicule ; 409 s'il est loué, en maintenance ou déjà réservé. */
    @PostExchange("/{id}/reservations")
    void reserver(@PathVariable Long id, @RequestBody CreneauRequest request);

    /** Libère la période (retour, annulation ou compensation). */
    @DeleteExchange("/{id}/reservations/{locationId}")
    void liberer(@PathVariable Long id, @PathVariable Long locationId);

    /** Corps de PATCH /vehicules/{id}/statut (kilometrage peut être null). */
    record StatutRequest(String statut, Integer kilometrage) {
    }

    /** Corps de POST /vehicules/{id}/reservations. */
    record CreneauRequest(Long locationId, String dateDebut, String dateFin) {
    }
}

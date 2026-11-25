package tn.sesame.driveeasy.vehicule.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tn.sesame.driveeasy.vehicule.dto.*;
import tn.sesame.driveeasy.vehicule.entity.Carburant;
import tn.sesame.driveeasy.vehicule.entity.Categorie;
import tn.sesame.driveeasy.vehicule.entity.StatutVehicule;
import tn.sesame.driveeasy.vehicule.service.VehiculeService;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;

/** API REST du parc automobile (exposée via la gateway sous /api/vehicules). */
@RestController
@RequestMapping("/vehicules")
public class VehiculeController {

    private final VehiculeService service;

    public VehiculeController(VehiculeService service) {
        this.service = service;
    }

    @GetMapping
    public List<VehiculeResponse> lister(@RequestParam(required = false) Categorie categorie,
                                         @RequestParam(required = false) Carburant carburant,
                                         @RequestParam(required = false) StatutVehicule statut,
                                         @RequestParam(required = false) BigDecimal prixMax) {
        return service.rechercher(categorie, carburant, statut, prixMax);
    }

    @GetMapping("/disponibles")
    public List<VehiculeResponse> disponibles(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return service.disponibles(dateDebut, dateFin);
    }

    @GetMapping("/{id}")
    public VehiculeResponse detail(@PathVariable Long id) {
        return service.trouver(id);
    }

    @PostMapping
    public ResponseEntity<VehiculeResponse> creer(@Valid @RequestBody VehiculeRequest request) {
        VehiculeResponse cree = service.creer(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(cree.id()).toUri();
        return ResponseEntity.created(location).body(cree);
    }

    @PutMapping("/{id}")
    public VehiculeResponse modifier(@PathVariable Long id, @Valid @RequestBody VehiculeRequest request) {
        return service.modifier(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    /** Utilisé par location-service au départ (LOUE) et au retour (DISPONIBLE + kilométrage). */
    @PatchMapping("/{id}/statut")
    public VehiculeResponse changerStatut(@PathVariable Long id, @Valid @RequestBody StatutRequest request) {
        return service.changerStatut(id, request);
    }

    @GetMapping("/{id}/disponibilite")
    public DisponibiliteResponse disponibilite(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return service.disponibilite(id, dateDebut, dateFin);
    }

    /** Réserve une période pour une location (409 si indisponible). */
    @PostMapping("/{id}/reservations")
    public ResponseEntity<Void> reserver(@PathVariable Long id, @Valid @RequestBody CreneauRequest request) {
        service.reserver(id, request);
        return ResponseEntity.status(201).build();
    }

    /** Libère la période (retour, annulation ou compensation après échec). */
    @DeleteMapping("/{id}/reservations/{locationId}")
    public ResponseEntity<Void> liberer(@PathVariable Long id, @PathVariable Long locationId) {
        service.liberer(id, locationId);
        return ResponseEntity.noContent().build();
    }
}

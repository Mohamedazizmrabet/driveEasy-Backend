package tn.sesame.driveeasy.location.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tn.sesame.driveeasy.location.dto.*;
import tn.sesame.driveeasy.location.entity.StatutLocation;
import tn.sesame.driveeasy.location.service.LocationService;

import java.net.URI;
import java.time.LocalDate;

/** API REST des locations (exposée via la gateway sous /api/locations). */
@RestController
@RequestMapping("/locations")
public class LocationController {

    private final LocationService service;

    public LocationController(LocationService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<LocationResponse> lister(
            @RequestParam(required = false) StatutLocation statut,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long vehiculeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size) {
        return service.lister(statut, clientId, vehiculeId, dateDebut, dateFin, page, size);
    }

    @GetMapping("/statistiques")
    public StatistiquesResponse statistiques() {
        return service.statistiques();
    }

    @GetMapping("/{id}")
    public LocationDetailResponse detail(@PathVariable Long id) {
        return service.detail(id);
    }

    @PostMapping
    public ResponseEntity<LocationResponse> creer(@Valid @RequestBody LocationRequest request) {
        LocationResponse creee = service.creer(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creee.id()).toUri();
        return ResponseEntity.created(location).body(creee);
    }

    @PostMapping("/simulation")
    public SimulationResponse simuler(@Valid @RequestBody LocationRequest request) {
        return service.simuler(request);
    }

    @PatchMapping("/{id}/depart")
    public LocationResponse depart(@PathVariable Long id) {
        return service.depart(id);
    }

    @PatchMapping("/{id}/retour")
    public LocationResponse retour(@PathVariable Long id, @Valid @RequestBody RetourRequest request) {
        return service.retour(id, request);
    }

    @PatchMapping("/{id}/annulation")
    public LocationResponse annuler(@PathVariable Long id) {
        return service.annuler(id);
    }

    /** Utilisé par client-service avant la suppression d'un client. */
    @GetMapping("/clients/{clientId}/actives")
    public LocationsActivesResponse locationsActives(@PathVariable Long clientId) {
        return service.locationsActives(clientId);
    }
}

package tn.sesame.driveeasy.client.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tn.sesame.driveeasy.client.dto.*;
import tn.sesame.driveeasy.client.service.ClientService;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

/** API REST des clients (exposée via la gateway sous /api/clients). */
@RestController
@RequestMapping("/clients")
public class ClientController {

    private final ClientService service;

    public ClientController(ClientService service) {
        this.service = service;
    }

    @GetMapping
    public List<ClientResponse> rechercher(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) String nom,
                                           @RequestParam(required = false) String cin,
                                           @RequestParam(required = false) String email) {
        return service.rechercher(q, nom, cin, email);
    }

    @GetMapping("/{id}")
    public ClientResponse detail(@PathVariable Long id) {
        return service.trouver(id);
    }

    @PostMapping
    public ResponseEntity<ClientResponse> creer(@Valid @RequestBody ClientRequest request) {
        ClientResponse cree = service.creer(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(cree.id()).toUri();
        return ResponseEntity.created(location).body(cree);
    }

    @PutMapping("/{id}")
    public ClientResponse modifier(@PathVariable Long id, @Valid @RequestBody ClientRequest request) {
        return service.modifier(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    /** dateFin (optionnelle) : le permis doit rester valide jusqu'à la fin de la location. */
    @GetMapping("/{id}/eligibilite")
    public EligibiliteResponse eligibilite(@PathVariable Long id,
                                           @RequestParam(required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return service.eligibilite(id, dateFin);
    }

    @PatchMapping("/{id}/categorie")
    public ClientResponse changerCategorie(@PathVariable Long id, @Valid @RequestBody CategorieRequest request) {
        return service.changerCategorie(id, request);
    }
}

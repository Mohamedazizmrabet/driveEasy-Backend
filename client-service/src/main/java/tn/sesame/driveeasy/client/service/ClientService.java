package tn.sesame.driveeasy.client.service;

import org.springframework.stereotype.Service;
import tn.sesame.driveeasy.client.remote.LocationServiceClient;
import tn.sesame.driveeasy.client.config.ClientProperties;
import tn.sesame.driveeasy.client.dto.*;
import tn.sesame.driveeasy.client.entity.CategorieClient;
import tn.sesame.driveeasy.client.entity.Client;
import tn.sesame.driveeasy.client.exception.ConflictException;
import tn.sesame.driveeasy.client.exception.ResourceNotFoundException;
import tn.sesame.driveeasy.client.repository.ClientRepository;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** Règles de gestion des clients. */
@Service
public class ClientService {

    private final ClientRepository repository;
    private final ClientProperties properties;
    private final LocationServiceClient locationClient;

    public ClientService(ClientRepository repository, ClientProperties properties,
                         LocationServiceClient locationClient) {
        this.repository = repository;
        this.properties = properties;
        this.locationClient = locationClient;
    }

    /** Recherche par nom (ou prénom), CIN ou email ; {@code q} cherche dans les trois. */
    public List<ClientResponse> rechercher(String q, String nom, String cin, String email) {
        return repository.findAll().stream()
                .filter(c -> q == null || contient(c.getNom() + " " + c.getPrenom(), q)
                        || contient(c.getCin(), q) || contient(c.getEmail(), q))
                .filter(c -> nom == null || contient(c.getNom() + " " + c.getPrenom(), nom))
                .filter(c -> cin == null || contient(c.getCin(), cin))
                .filter(c -> email == null || contient(c.getEmail(), email))
                .map(ClientResponse::from)
                .toList();
    }

    public ClientResponse trouver(Long id) {
        return ClientResponse.from(charger(id));
    }

    public synchronized ClientResponse creer(ClientRequest req) {
        verifierUnicite(req, null);
        Client c = new Client();
        appliquer(c, req);
        c.setCategorieClient(req.categorieClient() == null ? CategorieClient.STANDARD : req.categorieClient());
        c.setDateInscription(LocalDate.now());
        return ClientResponse.from(repository.save(c));
    }

    public synchronized ClientResponse modifier(Long id, ClientRequest req) {
        Client c = charger(id);
        verifierUnicite(req, id);
        appliquer(c, req);
        if (req.categorieClient() != null) c.setCategorieClient(req.categorieClient());
        return ClientResponse.from(repository.save(c));
    }

    /** Suppression refusée (409) si le client a une location RESERVEE ou EN_COURS. */
    public synchronized void supprimer(Long id) {
        charger(id);
        long actives = locationClient.getLocationsActives(id).locationsActives();
        if (actives > 0) {
            throw new ConflictException("Suppression impossible : le client a " + actives + " location(s) active(s)");
        }
        repository.deleteById(id);
    }

    /**
     * Le client peut louer s'il a l'âge minimum et un permis valide jusqu'à {@code dateFin}
     * (aujourd'hui par défaut).
     */
    public EligibiliteResponse eligibilite(Long id, LocalDate dateFin) {
        Client c = charger(id);
        LocalDate today = LocalDate.now();
        LocalDate validiteRequise = dateFin == null || dateFin.isBefore(today) ? today : dateFin;
        int age = Period.between(c.getDateNaissance(), today).getYears();
        boolean ageOk = age >= properties.ageMinimum();
        boolean permisValide = !c.getDateExpirationPermis().isBefore(validiteRequise);

        List<String> motifs = new ArrayList<>();
        if (!ageOk) motifs.add("Âge insuffisant : " + age + " ans (minimum " + properties.ageMinimum() + ")");
        if (!permisValide) motifs.add("Permis expiré ou expirant le " + c.getDateExpirationPermis());
        return new EligibiliteResponse(id, ageOk && permisValide, age, permisValide, c.getCategorieClient(), motifs);
    }

    /** Mise à jour de la catégorie de fidélité (pilotée par location-service). */
    public synchronized ClientResponse changerCategorie(Long id, CategorieRequest req) {
        Client c = charger(id);
        c.setCategorieClient(req.categorieClient());
        return ClientResponse.from(repository.save(c));
    }

    // ----------------------------------------------------------------------------------------

    private Client charger(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client " + id + " introuvable"));
    }

    private void verifierUnicite(ClientRequest req, Long idCourant) {
        unique(Client::getCin, req.cin(), "Le CIN", idCourant);
        unique(Client::getEmail, req.email(), "L'email", idCourant);
        unique(Client::getNumeroPermis, req.numeroPermis(), "Le numéro de permis", idCourant);
    }

    private void unique(Function<Client, String> champ, String valeur, String libelle, Long idCourant) {
        repository.findBy(champ, valeur.trim())
                .filter(existant -> !existant.getId().equals(idCourant))
                .ifPresent(existant -> {
                    throw new ConflictException(libelle + " " + valeur + " est déjà utilisé");
                });
    }

    private static boolean contient(String valeur, String recherche) {
        return valeur != null && valeur.toLowerCase(Locale.ROOT).contains(recherche.trim().toLowerCase(Locale.ROOT));
    }

    private static void appliquer(Client c, ClientRequest req) {
        c.setCin(req.cin().trim());
        c.setNom(req.nom().trim());
        c.setPrenom(req.prenom().trim());
        c.setDateNaissance(req.dateNaissance());
        c.setEmail(req.email().trim().toLowerCase(Locale.ROOT));
        c.setTelephone(req.telephone());
        c.setAdresse(req.adresse());
        c.setNumeroPermis(req.numeroPermis().trim().toUpperCase(Locale.ROOT));
        c.setDateExpirationPermis(req.dateExpirationPermis());
    }
}

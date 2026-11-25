package tn.sesame.driveeasy.vehicule.service;

import org.springframework.stereotype.Service;
import tn.sesame.driveeasy.vehicule.config.VehiculeProperties;
import tn.sesame.driveeasy.vehicule.dto.*;
import tn.sesame.driveeasy.vehicule.entity.*;
import tn.sesame.driveeasy.vehicule.exception.BusinessRuleException;
import tn.sesame.driveeasy.vehicule.exception.ConflictException;
import tn.sesame.driveeasy.vehicule.exception.ResourceNotFoundException;
import tn.sesame.driveeasy.vehicule.repository.VehiculeRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

/** Règles de gestion du parc automobile. */
@Service
public class VehiculeService {

    private final VehiculeRepository repository;
    private final VehiculeProperties properties;

    public VehiculeService(VehiculeRepository repository, VehiculeProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public List<VehiculeResponse> rechercher(Categorie categorie, Carburant carburant, StatutVehicule statut,
                                             BigDecimal prixMax) {
        return repository.findAll().stream()
                .filter(v -> categorie == null || v.getCategorie() == categorie)
                .filter(v -> carburant == null || v.getCarburant() == carburant)
                .filter(v -> statut == null || v.getStatut() == statut)
                .filter(v -> prixMax == null || v.getPrixJournalier().compareTo(prixMax) <= 0)
                .map(VehiculeResponse::from)
                .toList();
    }

    public VehiculeResponse trouver(Long id) {
        return VehiculeResponse.from(charger(id));
    }

    public synchronized VehiculeResponse creer(VehiculeRequest req) {
        String immat = normaliser(req.immatriculation());
        verifierImmatriculationUnique(immat, null);
        verifierAnnee(req.annee());
        if (req.statut() == StatutVehicule.LOUE) {
            throw new BusinessRuleException("Un véhicule ne peut pas être créé au statut LOUE");
        }
        Vehicule v = new Vehicule();
        appliquer(v, req, immat);
        v.setStatut(req.statut() == null ? StatutVehicule.DISPONIBLE : req.statut());
        return VehiculeResponse.from(repository.save(v));
    }

    public synchronized VehiculeResponse modifier(Long id, VehiculeRequest req) {
        Vehicule v = charger(id);
        String immat = normaliser(req.immatriculation());
        verifierImmatriculationUnique(immat, id);
        verifierAnnee(req.annee());
        if (req.statut() != null && req.statut() != v.getStatut()
                && (req.statut() == StatutVehicule.LOUE || v.getStatut() == StatutVehicule.LOUE)) {
            throw new ConflictException("Le statut LOUE est géré uniquement par location-service (départ / retour)");
        }
        appliquer(v, req, immat);
        if (req.statut() != null) v.setStatut(req.statut());
        return VehiculeResponse.from(repository.save(v));
    }

    public synchronized void supprimer(Long id) {
        Vehicule v = charger(id);
        if (v.getStatut() == StatutVehicule.LOUE) {
            throw new ConflictException("Suppression impossible : le véhicule est actuellement loué");
        }
        if (!v.getCreneaux().isEmpty()) {
            throw new ConflictException("Suppression impossible : le véhicule a des réservations en cours");
        }
        repository.deleteById(id);
    }

    public synchronized VehiculeResponse changerStatut(Long id, StatutRequest req) {
        Vehicule v = charger(id);
        if (req.kilometrage() != null) {
            if (req.kilometrage() < v.getKilometrage()) {
                throw new BusinessRuleException("Le kilométrage ne peut pas diminuer (actuel : " + v.getKilometrage() + ")");
            }
            v.setKilometrage(req.kilometrage());
        }
        v.setStatut(req.statut());
        return VehiculeResponse.from(repository.save(v));
    }

    /** Véhicules au statut DISPONIBLE sans réservation qui chevauche la période. */
    public List<VehiculeResponse> disponibles(LocalDate debut, LocalDate fin) {
        verifierPeriode(debut, fin);
        return repository.findAll().stream()
                .filter(v -> motifIndisponibilite(v, debut, fin) == null)
                .map(VehiculeResponse::from)
                .toList();
    }

    public DisponibiliteResponse disponibilite(Long id, LocalDate debut, LocalDate fin) {
        verifierPeriode(debut, fin);
        String motif = motifIndisponibilite(charger(id), debut, fin);
        return new DisponibiliteResponse(id, debut, fin, motif == null, motif);
    }

    /** Bloque une période pour une location (appelé par location-service à la réservation). */
    public synchronized void reserver(Long id, CreneauRequest req) {
        verifierPeriode(req.dateDebut(), req.dateFin());
        Vehicule v = charger(id);
        boolean dejaReserve = v.getCreneaux().stream().anyMatch(c -> c.locationId().equals(req.locationId()));
        if (dejaReserve) return; // appel idempotent
        String motif = motifIndisponibilite(v, req.dateDebut(), req.dateFin());
        if (motif != null) {
            throw new ConflictException(motif);
        }
        v.getCreneaux().add(new Creneau(req.locationId(), req.dateDebut(), req.dateFin()));
    }

    /** Libère la période d'une location (retour, annulation ou compensation). */
    public synchronized void liberer(Long id, Long locationId) {
        charger(id).getCreneaux().removeIf(c -> c.locationId().equals(locationId));
    }

    // ----------------------------------------------------------------------------------------

    private String motifIndisponibilite(Vehicule v, LocalDate debut, LocalDate fin) {
        if (v.getStatut() == StatutVehicule.MAINTENANCE) return "Le véhicule est en maintenance";
        if (v.getStatut() == StatutVehicule.LOUE) return "Le véhicule est actuellement loué";
        boolean chevauche = v.getCreneaux().stream().anyMatch(c -> c.chevauche(debut, fin));
        return chevauche ? "Le véhicule est déjà réservé sur cette période" : null;
    }

    private Vehicule charger(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule " + id + " introuvable"));
    }

    private void verifierImmatriculationUnique(String immat, Long idCourant) {
        repository.findByImmatriculation(immat)
                .filter(existant -> !existant.getId().equals(idCourant))
                .ifPresent(existant -> {
                    throw new ConflictException("L'immatriculation " + immat + " existe déjà");
                });
    }

    private void verifierAnnee(int annee) {
        int courante = Year.now().getValue();
        if (annee > courante || annee < properties.anneeMinimale()) {
            throw new BusinessRuleException("L'année doit être comprise entre "
                    + properties.anneeMinimale() + " et " + courante);
        }
    }

    private static void verifierPeriode(LocalDate debut, LocalDate fin) {
        if (!fin.isAfter(debut)) {
            throw new BusinessRuleException("La date de fin doit être postérieure à la date de début");
        }
    }

    private static String normaliser(String immatriculation) {
        return immatriculation.trim().toUpperCase().replaceAll("\\s+", " ");
    }

    private static void appliquer(Vehicule v, VehiculeRequest req, String immat) {
        v.setImmatriculation(immat);
        v.setMarque(req.marque().trim());
        v.setModele(req.modele().trim());
        v.setAnnee(req.annee());
        v.setCouleur(req.couleur());
        v.setCategorie(req.categorie());
        v.setCarburant(req.carburant());
        v.setBoite(req.boite());
        v.setNbPlaces(req.nbPlaces());
        v.setKilometrage(req.kilometrage());
        v.setPrixJournalier(req.prixJournalier());
        v.setImageUrl(req.imageUrl());
    }
}

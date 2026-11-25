package tn.sesame.driveeasy.location.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.sesame.driveeasy.location.config.LocationProperties;
import tn.sesame.driveeasy.location.dto.*;
import tn.sesame.driveeasy.location.entity.Location;
import tn.sesame.driveeasy.location.entity.StatutLocation;
import tn.sesame.driveeasy.location.exception.BusinessRuleException;
import tn.sesame.driveeasy.location.exception.ConflictException;
import tn.sesame.driveeasy.location.exception.ResourceNotFoundException;
import tn.sesame.driveeasy.location.exception.ServiceIndisponibleException;
import tn.sesame.driveeasy.location.client.*;
import tn.sesame.driveeasy.location.repository.LocationRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Cycle de vie des locations ; orchestre les appels vers client-service et vehicule-service. */
@Service
public class LocationService {

    private static final Logger log = LoggerFactory.getLogger(LocationService.class);
    private static final List<String> ORDRE_CATEGORIES = List.of("STANDARD", "FIDELE", "VIP");

    private final LocationRepository repository;
    private final TarificationService tarification;
    private final ClientServiceClient clientService;
    private final VehiculeServiceClient vehiculeService;
    private final LocationProperties properties;

    public LocationService(LocationRepository repository, TarificationService tarification,
                           ClientServiceClient clientService, VehiculeServiceClient vehiculeService,
                           LocationProperties properties) {
        this.repository = repository;
        this.tarification = tarification;
        this.clientService = clientService;
        this.vehiculeService = vehiculeService;
        this.properties = properties;
    }

    // ------------------------------------------------------------------ consultation

    /** Liste paginée ; la période garde les locations qui chevauchent [dateDebut, dateFin]. */
    public PageResponse<LocationResponse> lister(StatutLocation statut, Long clientId, Long vehiculeId,
                                                 LocalDate dateDebut, LocalDate dateFin, int page, Integer size) {
        int taille = size == null ? properties.taillePageDefaut() : size;
        if (page < 0 || taille < 1 || taille > 100) {
            throw new BusinessRuleException("Pagination invalide : page >= 0 et 1 <= size <= 100");
        }
        List<LocationResponse> resultat = repository.findAll().stream()
                .filter(l -> statut == null || l.getStatut() == statut)
                .filter(l -> clientId == null || l.getClientId().equals(clientId))
                .filter(l -> vehiculeId == null || l.getVehiculeId().equals(vehiculeId))
                .filter(l -> dateDebut == null || !finEffective(l).isBefore(dateDebut))
                .filter(l -> dateFin == null || !l.getDateDebut().isAfter(dateFin))
                .map(LocationResponse::from)
                .toList();
        return PageResponse.of(resultat, page, taille);
    }

    /** Détail enrichi ; une panne d'un service distant ne bloque pas la consultation. */
    public LocationDetailResponse detail(Long id) {
        Location l = charger(id);
        List<String> avertissements = new ArrayList<>();
        ClientInfo client = null;
        VehiculeInfo vehicule = null;
        try {
            client = clientService.getClient(l.getClientId());
        } catch (RuntimeException e) {
            avertissements.add("Client non disponible : " + e.getMessage());
        }
        try {
            vehicule = vehiculeService.getVehicule(l.getVehiculeId());
        } catch (RuntimeException e) {
            avertissements.add("Véhicule non disponible : " + e.getMessage());
        }
        return new LocationDetailResponse(LocationResponse.from(l), client, vehicule, avertissements);
    }

    public LocationsActivesResponse locationsActives(Long clientId) {
        long n = repository.findAll().stream()
                .filter(l -> l.getClientId().equals(clientId))
                .filter(l -> l.getStatut() == StatutLocation.RESERVEE || l.getStatut() == StatutLocation.EN_COURS)
                .count();
        return new LocationsActivesResponse(clientId, n);
    }

    // ------------------------------------------------------------------ simulation et réservation

    public SimulationResponse simuler(LocationRequest req) {
        verifierPeriode(req.dateDebut(), req.dateFin());
        EligibiliteInfo eligibilite = clientService.getEligibilite(req.clientId(), req.dateFin().toString());
        VehiculeInfo vehicule = vehiculeService.getVehicule(req.vehiculeId());
        DisponibiliteInfo dispo = vehiculeService.getDisponibilite(req.vehiculeId(), req.dateDebut().toString(), req.dateFin().toString());
        Tarif t = tarification.calculer(req.dateDebut(), req.dateFin(), vehicule.prixJournalier(),
                vehicule.categorie(), eligibilite.categorieClient());
        return new SimulationResponse(req.clientId(), req.vehiculeId(), req.dateDebut(), req.dateFin(),
                t.nombreJours(), t.prixJournalier(), t.coefficientCategorie(), t.prixJournalierApplique(),
                eligibilite.categorieClient(), t.tauxRemise(), t.montantBrut(), t.remise(), t.montantTotal(),
                eligibilite.eligible(), eligibilite.motifs(), dispo.disponible(), dispo.motif());
    }

    /**
     * Création d'une réservation :
     * 1. client éligible (client-service) ; 2. tarif du véhicule (vehicule-service) ;
     * 3. enregistrement local ; 4. blocage de la période sur le véhicule.
     * Si l'étape 4 échoue, la réservation locale est supprimée (compensation).
     */
    public LocationResponse creer(LocationRequest req) {
        verifierPeriode(req.dateDebut(), req.dateFin());

        EligibiliteInfo eligibilite = clientService.getEligibilite(req.clientId(), req.dateFin().toString());
        if (!eligibilite.eligible()) {
            throw new BusinessRuleException("Client non éligible : " + String.join(" ; ", eligibilite.motifs()));
        }
        VehiculeInfo vehicule = vehiculeService.getVehicule(req.vehiculeId());
        Tarif t = tarification.calculer(req.dateDebut(), req.dateFin(), vehicule.prixJournalier(),
                vehicule.categorie(), eligibilite.categorieClient());

        Location l = new Location();
        l.setClientId(req.clientId());
        l.setVehiculeId(req.vehiculeId());
        l.setDateDebut(req.dateDebut());
        l.setDateFinPrevue(req.dateFin());
        l.setNombreJours(t.nombreJours());
        l.setPrixJournalierApplique(t.prixJournalierApplique());
        l.setCategorieClient(eligibilite.categorieClient());
        l.setRemise(t.remise());
        l.setMontantTotal(t.montantTotal());
        l.setStatut(StatutLocation.RESERVEE);
        l.setDateCreation(LocalDateTime.now());
        repository.save(l);

        try {
            vehiculeService.reserver(req.vehiculeId(),
                    new VehiculeServiceClient.CreneauRequest(l.getId(), req.dateDebut().toString(), req.dateFin().toString()));
        } catch (RuntimeException e) {
            // Compensation : on supprime la réservation locale pour rester cohérent.
            repository.deleteById(l.getId());
            if (e instanceof ServiceIndisponibleException) {
                // La requête a pu aboutir côté véhicule avant la panne : on tente de libérer la période.
                tenter(() -> vehiculeService.liberer(req.vehiculeId(), l.getId()), "libération après échec");
            }
            log.warn("Réservation {} annulée par compensation : {}", l.getId(), e.getMessage());
            throw e;
        }
        log.info("Location {} réservée (client {}, véhicule {})", l.getId(), l.getClientId(), l.getVehiculeId());
        return LocationResponse.from(l);
    }

    // ------------------------------------------------------------------ cycle de vie

    /** Départ : le véhicule passe à LOUE puis la location à EN_COURS. */
    public synchronized LocationResponse depart(Long id) {
        Location l = charger(id);
        exigerStatut(l, StatutLocation.RESERVEE, "démarrer");
        VehiculeInfo vehicule = vehiculeService.getVehicule(l.getVehiculeId());
        if (!"DISPONIBLE".equals(vehicule.statut())) {
            throw new ConflictException("Départ impossible : le véhicule est " + vehicule.statut());
        }
        // Appel distant d'abord : s'il échoue, la location reste RESERVEE (état cohérent).
        vehiculeService.changerStatut(l.getVehiculeId(), new VehiculeServiceClient.StatutRequest("LOUE", null));
        l.setKmDepart(vehicule.kilometrage());
        l.setStatut(StatutLocation.EN_COURS);
        return LocationResponse.from(repository.save(l));
    }

    /** Retour : calcul de la pénalité, véhicule DISPONIBLE, mise à jour de la fidélité du client. */
    public synchronized LocationResponse retour(Long id, RetourRequest req) {
        Location l = charger(id);
        exigerStatut(l, StatutLocation.EN_COURS, "clôturer");
        LocalDate dateRetour = req.dateRetour() == null ? LocalDate.now() : req.dateRetour();
        if (dateRetour.isBefore(l.getDateDebut())) {
            throw new BusinessRuleException("La date de retour ne peut pas précéder la date de début");
        }
        if (l.getKmDepart() != null && req.kmRetour() < l.getKmDepart()) {
            throw new BusinessRuleException("Le kilométrage de retour (" + req.kmRetour()
                    + ") est inférieur au kilométrage de départ (" + l.getKmDepart() + ")");
        }

        // Appel distant d'abord : en cas d'échec, la location reste EN_COURS et peut être relancée.
        vehiculeService.changerStatut(l.getVehiculeId(),
                new VehiculeServiceClient.StatutRequest("DISPONIBLE", req.kmRetour()));
        tenter(() -> vehiculeService.liberer(l.getVehiculeId(), l.getId()), "libération de la période");

        BigDecimal penalite = tarification.penalite(l.getDateFinPrevue(), dateRetour, l.getPrixJournalierApplique());
        l.setDateRetourEffectif(dateRetour);
        l.setKmRetour(req.kmRetour());
        l.setPenalite(penalite);
        l.setMontantTotal(l.getMontantTotal().add(penalite));
        l.setStatut(StatutLocation.TERMINEE);
        repository.save(l);

        mettreAJourFidelite(l.getClientId());
        return LocationResponse.from(l);
    }

    /** Annulation d'une réservation : la période est libérée sur le véhicule. */
    public synchronized LocationResponse annuler(Long id) {
        Location l = charger(id);
        exigerStatut(l, StatutLocation.RESERVEE, "annuler");
        vehiculeService.liberer(l.getVehiculeId(), l.getId());
        l.setStatut(StatutLocation.ANNULEE);
        return LocationResponse.from(repository.save(l));
    }

    // ------------------------------------------------------------------ statistiques

    public StatistiquesResponse statistiques() {
        List<Location> toutes = repository.findAll();
        List<Location> terminees = toutes.stream().filter(l -> l.getStatut() == StatutLocation.TERMINEE).toList();
        YearMonth moisCourant = YearMonth.now();

        BigDecimal ca = somme(terminees);
        BigDecimal caMois = somme(terminees.stream()
                .filter(l -> YearMonth.from(l.getDateRetourEffectif()).equals(moisCourant)).toList());

        Map<StatutLocation, Long> parStatut = new EnumMap<>(StatutLocation.class);
        for (StatutLocation s : StatutLocation.values()) parStatut.put(s, 0L);
        toutes.forEach(l -> parStatut.merge(l.getStatut(), 1L, Long::sum));

        // Indicateurs du parc : récupérés auprès de vehicule-service (non bloquant).
        List<String> avertissements = new ArrayList<>();
        Map<Long, VehiculeInfo> vehicules = Map.of();
        Integer total = null;
        Map<String, Long> vehiculesParStatut = null;
        Double tauxOccupation = null;
        try {
            List<VehiculeInfo> liste = vehiculeService.getVehicules();
            vehicules = liste.stream().collect(Collectors.toMap(VehiculeInfo::id, Function.identity()));
            total = liste.size();
            vehiculesParStatut = new TreeMap<>(liste.stream()
                    .collect(Collectors.groupingBy(VehiculeInfo::statut, Collectors.counting())));
            long loues = vehiculesParStatut.getOrDefault("LOUE", 0L);
            tauxOccupation = total == 0 ? 0.0
                    : BigDecimal.valueOf(loues * 100.0 / total).setScale(1, RoundingMode.HALF_UP).doubleValue();
        } catch (RuntimeException e) {
            avertissements.add("Indicateurs du parc indisponibles : " + e.getMessage());
        }

        Map<Long, VehiculeInfo> infos = vehicules;
        List<StatistiquesResponse.TopVehicule> top = toutes.stream()
                .filter(l -> l.getStatut() != StatutLocation.ANNULEE)
                .collect(Collectors.groupingBy(Location::getVehiculeId))
                .entrySet().stream()
                .map(e -> {
                    VehiculeInfo v = infos.get(e.getKey());
                    List<Location> terminesVehicule = e.getValue().stream()
                            .filter(l -> l.getStatut() == StatutLocation.TERMINEE).toList();
                    return new StatistiquesResponse.TopVehicule(e.getKey(),
                            v == null ? "Véhicule " + e.getKey() : v.marque() + " " + v.modele(),
                            v == null ? null : v.immatriculation(), e.getValue().size(), somme(terminesVehicule));
                })
                .sorted(Comparator.comparingLong(StatistiquesResponse.TopVehicule::nombreLocations).reversed())
                .limit(5)
                .toList();

        return new StatistiquesResponse(ca, caMois, toutes.size(), parStatut, total, vehiculesParStatut,
                tauxOccupation, top, avertissements);
    }

    // ------------------------------------------------------------------ utilitaires

    /**
     * Passe le client en FIDELE / VIP selon ses locations terminées (jamais de rétrogradation).
     * Non bloquant : un échec est journalisé, le retour du véhicule reste valide.
     */
    private void mettreAJourFidelite(Long clientId) {
        long terminees = repository.findAll().stream()
                .filter(l -> l.getClientId().equals(clientId) && l.getStatut() == StatutLocation.TERMINEE)
                .count();
        String cible = tarification.categorieMeritee(terminees);
        if (cible == null) return;
        tenter(() -> {
            String actuelle = clientService.getClient(clientId).categorieClient();
            if (ORDRE_CATEGORIES.indexOf(cible) > ORDRE_CATEGORIES.indexOf(actuelle)) {
                clientService.changerCategorie(clientId, new ClientServiceClient.CategorieRequest(cible));
                log.info("Client {} promu {} ({} locations terminées)", clientId, cible, terminees);
            }
        }, "mise à jour de la catégorie du client " + clientId);
    }

    private void tenter(Runnable action, String description) {
        try {
            action.run();
        } catch (RuntimeException e) {
            log.warn("Échec non bloquant ({}) : {}", description, e.getMessage());
        }
    }

    private Location charger(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location " + id + " introuvable"));
    }

    private static void exigerStatut(Location l, StatutLocation attendu, String action) {
        if (l.getStatut() != attendu) {
            throw new ConflictException("Impossible de " + action + " la location " + l.getId()
                    + " : statut " + l.getStatut() + " (attendu " + attendu + ")");
        }
    }

    private static void verifierPeriode(LocalDate debut, LocalDate fin) {
        if (!fin.isAfter(debut)) {
            throw new BusinessRuleException("La date de fin doit être postérieure à la date de début");
        }
    }

    private static LocalDate finEffective(Location l) {
        return l.getDateRetourEffectif() != null ? l.getDateRetourEffectif() : l.getDateFinPrevue();
    }

    private static BigDecimal somme(List<Location> locations) {
        return locations.stream().map(Location::getMontantTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

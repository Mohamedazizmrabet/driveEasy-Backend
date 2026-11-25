package tn.sesame.driveeasy.location.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tn.sesame.driveeasy.location.entity.Location;
import tn.sesame.driveeasy.location.entity.StatutLocation;
import tn.sesame.driveeasy.location.repository.LocationRepository;
import tn.sesame.driveeasy.location.service.Tarif;
import tn.sesame.driveeasy.location.service.TarificationService;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Historique de démonstration (locations TERMINEE / ANNULEE uniquement, pour ne pas
 * contredire le statut des véhicules). Les identifiants et tarifs correspondent aux
 * données de démo de vehicule-service et client-service.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    /** Index = id véhicule − 1 → {prix journalier, catégorie} (cf. vehicule-service). */
    private static final String[][] VEHICULES = {
            {"95", "ECONOMIQUE"}, {"110", "COMPACTE"}, {"140", "COMPACTE"},
            {"160", "BERLINE"}, {"210", "SUV"}, {"320", "LUXE"}};

    private final LocationRepository repository;
    private final TarificationService tarification;
    private final LocationProperties properties;

    public DataInitializer(LocationRepository repository, TarificationService tarification,
                           LocationProperties properties) {
        this.repository = repository;
        this.tarification = tarification;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        if (!properties.donneesDemo()) return;
        historique(1L, 5, "STANDARD", 0);   // Amine : 5 terminées (FIDELE)
        historique(2L, 2, "STANDARD", 2);   // Sarra : 2 terminées
        historique(3L, 15, "FIDELE", 4);    // Mehdi : 15 terminées (VIP)

        Location annulee = terminee(2L, 4L, LocalDate.now().minusDays(20), 3, "STANDARD", 0);
        annulee.setStatut(StatutLocation.ANNULEE);
        annulee.setDateRetourEffectif(null);
        annulee.setKmDepart(null);
        annulee.setKmRetour(null);
        annulee.setPenalite(BigDecimal.ZERO);
    }

    private void historique(Long clientId, int nombre, String categorie, int decalage) {
        for (int i = 0; i < nombre; i++) {
            long vehiculeId = (i + decalage) % VEHICULES.length + 1L;
            LocalDate debut = LocalDate.now().minusDays(12L + i * 9L + decalage);
            // Une location sur quatre est rendue avec un jour de retard (pénalité).
            terminee(clientId, vehiculeId, debut, 3 + (i % 3), categorie, i % 4 == 3 ? 1 : 0);
        }
    }

    private Location terminee(Long clientId, long vehiculeId, LocalDate debut, int jours, String categorie,
                              int retard) {
        String[] v = VEHICULES[(int) vehiculeId - 1];
        LocalDate fin = debut.plusDays(jours);
        Tarif t = tarification.calculer(debut, fin, new BigDecimal(v[0]), v[1], categorie);
        LocalDate retour = fin.plusDays(retard);
        BigDecimal penalite = tarification.penalite(fin, retour, t.prixJournalierApplique());

        Location l = new Location();
        l.setClientId(clientId);
        l.setVehiculeId(vehiculeId);
        l.setDateDebut(debut);
        l.setDateFinPrevue(fin);
        l.setDateRetourEffectif(retour);
        l.setKmDepart(1000);
        l.setKmRetour(1000 + jours * 180);
        l.setNombreJours(t.nombreJours());
        l.setPrixJournalierApplique(t.prixJournalierApplique());
        l.setCategorieClient(categorie);
        l.setRemise(t.remise());
        l.setPenalite(penalite);
        l.setMontantTotal(t.montantTotal().add(penalite));
        l.setStatut(StatutLocation.TERMINEE);
        l.setDateCreation(debut.minusDays(2).atTime(10, 0));
        return repository.save(l);
    }
}

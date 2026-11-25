package tn.sesame.driveeasy.location.service;

import org.springframework.stereotype.Service;
import tn.sesame.driveeasy.location.config.LocationProperties;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Calculs purs (sans appel réseau), testés unitairement :
 * <ul>
 *   <li>Prix = jours × prix journalier × coefficient catégorie − remise fidélité (FIDELE 5 %, VIP 10 %)</li>
 *   <li>Pénalité = 1,5 × prix journalier × jours de retard</li>
 *   <li>Catégorie : FIDELE après 5 locations terminées, VIP après 15</li>
 * </ul>
 */
@Service
public class TarificationService {

    private final LocationProperties properties;

    public TarificationService(LocationProperties properties) {
        this.properties = properties;
    }

    public Tarif calculer(LocalDate debut, LocalDate fin, BigDecimal prixJournalier, String categorieVehicule,
                          String categorieClient) {
        long jours = Math.max(1, ChronoUnit.DAYS.between(debut, fin));
        BigDecimal coefficient = properties.coefficientsCategorie().getOrDefault(categorieVehicule, BigDecimal.ONE);
        BigDecimal prixApplique = arrondir(prixJournalier.multiply(coefficient));
        BigDecimal brut = arrondir(prixApplique.multiply(BigDecimal.valueOf(jours)));
        BigDecimal taux = tauxRemise(categorieClient);
        BigDecimal remise = arrondir(brut.multiply(taux));
        return new Tarif(jours, prixJournalier, coefficient, prixApplique, taux, brut, remise, brut.subtract(remise));
    }

    public BigDecimal tauxRemise(String categorieClient) {
        if ("VIP".equals(categorieClient)) return properties.remiseVip();
        if ("FIDELE".equals(categorieClient)) return properties.remiseFidele();
        return BigDecimal.ZERO;
    }

    public long joursDeRetard(LocalDate dateFinPrevue, LocalDate dateRetour) {
        return Math.max(0, ChronoUnit.DAYS.between(dateFinPrevue, dateRetour));
    }

    public BigDecimal penalite(LocalDate dateFinPrevue, LocalDate dateRetour, BigDecimal prixJournalier) {
        long retard = joursDeRetard(dateFinPrevue, dateRetour);
        return arrondir(prixJournalier.multiply(properties.coefficientPenalite()).multiply(BigDecimal.valueOf(retard)));
    }

    /** Catégorie méritée selon le nombre de locations terminées, ou null si STANDARD. */
    public String categorieMeritee(long locationsTerminees) {
        if (locationsTerminees >= properties.seuilVip()) return "VIP";
        if (locationsTerminees >= properties.seuilFidele()) return "FIDELE";
        return null;
    }

    private static BigDecimal arrondir(BigDecimal valeur) {
        return valeur.setScale(2, RoundingMode.HALF_UP);
    }
}

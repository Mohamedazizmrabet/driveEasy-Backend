package tn.sesame.driveeasy.location.service;

import java.math.BigDecimal;

/** Détail du calcul de prix d'une location. */
public record Tarif(long nombreJours, BigDecimal prixJournalier, BigDecimal coefficientCategorie,
                    BigDecimal prixJournalierApplique, BigDecimal tauxRemise, BigDecimal montantBrut,
                    BigDecimal remise, BigDecimal montantTotal) {
}

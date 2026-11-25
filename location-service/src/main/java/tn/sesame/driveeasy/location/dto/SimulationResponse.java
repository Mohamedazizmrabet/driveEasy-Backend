package tn.sesame.driveeasy.location.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Résultat d'une simulation de prix (rien n'est enregistré). */
public record SimulationResponse(
        Long clientId, Long vehiculeId, LocalDate dateDebut, LocalDate dateFin, long nombreJours,
        BigDecimal prixJournalier, BigDecimal coefficientCategorie, BigDecimal prixJournalierApplique,
        String categorieClient, BigDecimal tauxRemise, BigDecimal montantBrut, BigDecimal remise,
        BigDecimal montantTotal, boolean clientEligible, List<String> motifsIneligibilite,
        boolean vehiculeDisponible, String motifIndisponibilite) {
}

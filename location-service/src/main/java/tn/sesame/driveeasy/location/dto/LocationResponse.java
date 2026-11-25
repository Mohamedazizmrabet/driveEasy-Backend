package tn.sesame.driveeasy.location.dto;

import tn.sesame.driveeasy.location.entity.Location;
import tn.sesame.driveeasy.location.entity.StatutLocation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LocationResponse(
        Long id, Long clientId, Long vehiculeId, LocalDate dateDebut, LocalDate dateFinPrevue,
        LocalDate dateRetourEffectif, Integer kmDepart, Integer kmRetour, long nombreJours,
        BigDecimal prixJournalierApplique, String categorieClient, BigDecimal remise, BigDecimal penalite,
        BigDecimal montantTotal, StatutLocation statut, LocalDateTime dateCreation) {

    public static LocationResponse from(Location l) {
        return new LocationResponse(l.getId(), l.getClientId(), l.getVehiculeId(), l.getDateDebut(),
                l.getDateFinPrevue(), l.getDateRetourEffectif(), l.getKmDepart(), l.getKmRetour(),
                l.getNombreJours(), l.getPrixJournalierApplique(), l.getCategorieClient(), l.getRemise(),
                l.getPenalite(), l.getMontantTotal(), l.getStatut(), l.getDateCreation());
    }
}

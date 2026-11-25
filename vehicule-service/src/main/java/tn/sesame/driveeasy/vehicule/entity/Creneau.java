package tn.sesame.driveeasy.vehicule.entity;

import java.time.LocalDate;

/**
 * Période réservée sur un véhicule par une location (dateFin exclusive :
 * le véhicule rendu le jour J peut repartir le jour J).
 */
public record Creneau(Long locationId, LocalDate dateDebut, LocalDate dateFin) {

    public boolean chevauche(LocalDate debut, LocalDate fin) {
        return dateDebut.isBefore(fin) && debut.isBefore(dateFin);
    }
}

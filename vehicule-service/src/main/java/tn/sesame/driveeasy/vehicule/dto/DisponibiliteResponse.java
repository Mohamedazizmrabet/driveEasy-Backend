package tn.sesame.driveeasy.vehicule.dto;

import java.time.LocalDate;

public record DisponibiliteResponse(Long vehiculeId, LocalDate dateDebut, LocalDate dateFin,
                                    boolean disponible, String motif) {
}

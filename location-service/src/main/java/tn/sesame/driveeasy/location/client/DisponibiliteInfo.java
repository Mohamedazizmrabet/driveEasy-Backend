package tn.sesame.driveeasy.location.client;

import java.time.LocalDate;

public record DisponibiliteInfo(Long vehiculeId, LocalDate dateDebut, LocalDate dateFin, boolean disponible,
                                String motif) {
}

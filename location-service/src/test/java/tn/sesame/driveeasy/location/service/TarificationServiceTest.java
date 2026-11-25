package tn.sesame.driveeasy.location.service;

import org.junit.jupiter.api.Test;
import tn.sesame.driveeasy.location.config.LocationProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Tests unitaires des règles de prix, de pénalité et de fidélité du cahier des charges. */
class TarificationServiceTest {

    private static final LocalDate DEBUT = LocalDate.of(2026, 12, 1);

    private final TarificationService service = new TarificationService(new LocationProperties(
            new BigDecimal("0.05"), new BigDecimal("0.10"), new BigDecimal("1.5"),
            Map.of("SUV", new BigDecimal("1.2")), 5, 15, 10, false));

    @Test
    void prixStandardSansRemise() {
        Tarif t = service.calculer(DEBUT, DEBUT.plusDays(3), new BigDecimal("100"), "ECONOMIQUE", "STANDARD");
        assertThat(t.nombreJours()).isEqualTo(3);
        assertThat(t.montantTotal()).isEqualByComparingTo("300.00");
    }

    @Test
    void coefficientCategorieEtRemiseVip() {
        // 4 jours × 100 × 1.2 = 480 ; remise VIP 10 % = 48 ; total 432
        Tarif t = service.calculer(DEBUT, DEBUT.plusDays(4), new BigDecimal("100"), "SUV", "VIP");
        assertThat(t.montantBrut()).isEqualByComparingTo("480.00");
        assertThat(t.remise()).isEqualByComparingTo("48.00");
        assertThat(t.montantTotal()).isEqualByComparingTo("432.00");
    }

    @Test
    void remiseFidele() {
        Tarif t = service.calculer(DEBUT, DEBUT.plusDays(2), new BigDecimal("100"), "ECONOMIQUE", "FIDELE");
        assertThat(t.montantTotal()).isEqualByComparingTo("190.00");
    }

    @Test
    void penaliteDeRetard() {
        // 2 jours de retard × 1.5 × 100 = 300
        assertThat(service.penalite(DEBUT, DEBUT.plusDays(2), new BigDecimal("100"))).isEqualByComparingTo("300.00");
        // retour anticipé : pas de pénalité
        assertThat(service.penalite(DEBUT, DEBUT.minusDays(1), new BigDecimal("100"))).isEqualByComparingTo("0");
    }

    @Test
    void categorieMeritee() {
        assertThat(service.categorieMeritee(4)).isNull();
        assertThat(service.categorieMeritee(5)).isEqualTo("FIDELE");
        assertThat(service.categorieMeritee(15)).isEqualTo("VIP");
    }
}

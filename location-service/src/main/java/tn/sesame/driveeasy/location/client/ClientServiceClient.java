package tn.sesame.driveeasy.location.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;

/**
 * Client déclaratif vers client-service (même principe que EmployeeClient dans le TP).
 * L'implémentation est générée par Spring (voir WebClientConfig).
 */
@HttpExchange("/clients")
public interface ClientServiceClient {

    @GetExchange("/{id}")
    ClientInfo getClient(@PathVariable Long id);

    /** Éligibilité : âge minimum et permis valide jusqu'à dateFin (yyyy-MM-dd). */
    @GetExchange("/{id}/eligibilite")
    EligibiliteInfo getEligibilite(@PathVariable Long id, @RequestParam String dateFin);

    /** Mise à jour de la catégorie de fidélité (FIDELE / VIP). */
    @PatchExchange("/{id}/categorie")
    ClientInfo changerCategorie(@PathVariable Long id, @RequestBody CategorieRequest request);

    record CategorieRequest(String categorieClient) {
    }
}

package tn.sesame.driveeasy.client.remote;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import tn.sesame.driveeasy.client.dto.LocationsActivesResponse;

/**
 * Client déclaratif vers location-service (même principe que EmployeeClient dans le TP).
 * L'implémentation est générée par Spring (voir config/WebClientConfig).
 */
@HttpExchange("/locations")
public interface LocationServiceClient {

    /** Nombre de locations RESERVEE ou EN_COURS du client. */
    @GetExchange("/clients/{clientId}/actives")
    LocationsActivesResponse getLocationsActives(@PathVariable Long clientId);
}

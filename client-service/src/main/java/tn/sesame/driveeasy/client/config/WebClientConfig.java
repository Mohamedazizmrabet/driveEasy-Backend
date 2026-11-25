package tn.sesame.driveeasy.client.config;

import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.core.publisher.Mono;
import tn.sesame.driveeasy.client.exception.ServiceIndisponibleException;
import tn.sesame.driveeasy.client.remote.LocationServiceClient;

/**
 * Crée le client @HttpExchange vers location-service, comme WebClientConfig dans le TP.
 * {@code http://location-service} est un nom Eureka résolu par le filtre de load balancing ;
 * toute erreur (service éteint, aucune instance, réponse en erreur) devient un 503.
 */
@Configuration
public class WebClientConfig {

    @Bean
    LocationServiceClient locationServiceClient(WebClient.Builder builder, LoadBalancedExchangeFilterFunction lb) {
        String message = "location-service indisponible : impossible de vérifier les locations actives du client";
        WebClient webClient = builder.clone()
                .baseUrl("http://location-service")
                .filter(lb)
                .filter((request, next) -> next.exchange(request)
                        .onErrorMap(WebClientRequestException.class, e -> new ServiceIndisponibleException(message, e)))
                .defaultStatusHandler(HttpStatusCode::isError,
                        response -> Mono.just(new ServiceIndisponibleException(message, null)))
                .build();
        return HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient)).build()
                .createClient(LocationServiceClient.class);
    }
}

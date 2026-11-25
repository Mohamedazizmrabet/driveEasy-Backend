package tn.sesame.driveeasy.location.config;

import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.core.publisher.Mono;
import tn.sesame.driveeasy.location.client.ClientServiceClient;
import tn.sesame.driveeasy.location.client.VehiculeServiceClient;
import tn.sesame.driveeasy.location.exception.*;

/**
 * Crée les clients @HttpExchange, comme WebClientConfig dans le TP.
 * <ul>
 *   <li>{@code http://vehicule-service} : nom Eureka, résolu par le filtre de load balancing ;</li>
 *   <li>le WebClient.Builder fourni par Spring Boot propage l'identifiant de trace (Zipkin) ;</li>
 *   <li>les erreurs distantes sont traduites : 404 → 404, 409 → 409, autre 4xx → 400,
 *       5xx / service injoignable / aucune instance Eureka → 503.</li>
 * </ul>
 */
@Configuration
public class WebClientConfig {

    @Bean
    VehiculeServiceClient vehiculeServiceClient(WebClient.Builder builder, LoadBalancedExchangeFilterFunction lb) {
        return creerClient(builder, lb, "vehicule-service", VehiculeServiceClient.class);
    }

    @Bean
    ClientServiceClient clientServiceClient(WebClient.Builder builder, LoadBalancedExchangeFilterFunction lb) {
        return creerClient(builder, lb, "client-service", ClientServiceClient.class);
    }

    private static <T> T creerClient(WebClient.Builder builder, LoadBalancedExchangeFilterFunction lb,
                                     String service, Class<T> type) {
        WebClient webClient = builder.clone()
                .baseUrl("http://" + service)
                .filter(lb)
                // Service éteint / injoignable → 503
                .filter((request, next) -> next.exchange(request)
                        .onErrorMap(WebClientRequestException.class,
                                e -> new ServiceIndisponibleException(service + " est indisponible", e)))
                // Réponse 4xx / 5xx → exception métier avec le message renvoyé par le service
                .defaultStatusHandler(HttpStatusCode::isError, response -> traduireErreur(service, response))
                .build();
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient)).build();
        return factory.createClient(type);
    }

    private static Mono<RuntimeException> traduireErreur(String service, ClientResponse response) {
        HttpStatusCode status = response.statusCode();
        return response.bodyToMono(ApiError.class)
                .map(ApiError::message)
                .onErrorResume(e -> Mono.empty())
                .defaultIfEmpty(service + " a répondu " + status.value())
                .map(message -> {
                    if (status.value() == HttpStatus.NOT_FOUND.value()) return new ResourceNotFoundException(message);
                    if (status.value() == HttpStatus.CONFLICT.value()) return new ConflictException(message);
                    if (status.is4xxClientError()) return new BusinessRuleException(message);
                    return new ServiceIndisponibleException(service + " est indisponible (" + message + ")", null);
                });
    }
}

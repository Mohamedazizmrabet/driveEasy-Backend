package tn.sesame.driveeasy.location.exception;

import java.time.Instant;
import java.util.Map;

/** Format JSON commun des erreurs renvoyées par les microservices. */
public record ApiError(Instant timestamp, int status, String error, String message, String path,
                       Map<String, String> details) {
}

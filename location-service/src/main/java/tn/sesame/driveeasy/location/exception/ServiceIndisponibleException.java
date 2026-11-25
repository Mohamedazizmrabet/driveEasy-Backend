package tn.sesame.driveeasy.location.exception;

/** Un microservice distant ne répond pas (traduit en 503). */
public class ServiceIndisponibleException extends RuntimeException {
    public ServiceIndisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}

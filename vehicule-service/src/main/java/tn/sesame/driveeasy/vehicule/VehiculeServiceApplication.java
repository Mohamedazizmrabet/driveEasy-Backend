package tn.sesame.driveeasy.vehicule;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@EnableDiscoveryClient 
@ConfigurationPropertiesScan
public class VehiculeServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(VehiculeServiceApplication.class, args);
    }
}

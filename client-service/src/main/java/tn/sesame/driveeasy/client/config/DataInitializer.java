package tn.sesame.driveeasy.client.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tn.sesame.driveeasy.client.entity.CategorieClient;
import tn.sesame.driveeasy.client.entity.Client;
import tn.sesame.driveeasy.client.repository.ClientRepository;

import java.time.LocalDate;

/** Jeu de données de démonstration chargé en RAM au démarrage. */
@Component
public class DataInitializer implements CommandLineRunner {

    private final ClientRepository repository;
    private final ClientProperties properties;

    public DataInitializer(ClientRepository repository, ClientProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        if (!properties.donneesDemo()) return;
        LocalDate today = LocalDate.now();
        add("09876543", "Ben Salah", "Amine", today.minusYears(34), "amine.bensalah@example.tn", "+216 22 111 222", "Tunis", "P-100231", today.plusYears(4), CategorieClient.FIDELE);
        add("07654321", "Trabelsi", "Sarra", today.minusYears(28), "sarra.trabelsi@example.tn", "+216 55 333 444", "Sousse", "P-200455", today.plusYears(2), CategorieClient.STANDARD);
        add("06543210", "Gharbi", "Mehdi", today.minusYears(45), "mehdi.gharbi@example.tn", "+216 98 555 666", "Sfax", "P-300789", today.plusYears(6), CategorieClient.VIP);
        // Client trop jeune (19 ans) : non éligible
        add("11223344", "Jaziri", "Yassine", today.minusYears(19), "yassine.jaziri@example.tn", "+216 20 777 888", "Nabeul", "P-400112", today.plusYears(8), CategorieClient.STANDARD);
        // Permis expiré : non éligible
        add("55667788", "Mansour", "Leila", today.minusYears(39), "leila.mansour@example.tn", "+216 50 999 000", "Bizerte", "P-500334", today.minusMonths(2), CategorieClient.STANDARD);
    }

    private void add(String cin, String nom, String prenom, LocalDate naissance, String email, String tel,
                     String adresse, String permis, LocalDate expiration, CategorieClient categorie) {
        Client c = new Client();
        c.setCin(cin);
        c.setNom(nom);
        c.setPrenom(prenom);
        c.setDateNaissance(naissance);
        c.setEmail(email);
        c.setTelephone(tel);
        c.setAdresse(adresse);
        c.setNumeroPermis(permis);
        c.setDateExpirationPermis(expiration);
        c.setCategorieClient(categorie);
        c.setDateInscription(LocalDate.now().minusMonths(6));
        repository.save(c);
    }
}

package tn.sesame.driveeasy.vehicule.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tn.sesame.driveeasy.vehicule.entity.*;
import tn.sesame.driveeasy.vehicule.repository.VehiculeRepository;

import java.math.BigDecimal;

/** Jeu de données de démonstration chargé en RAM au démarrage. */
@Component
public class DataInitializer implements CommandLineRunner {

    private final VehiculeRepository repository;
    private final VehiculeProperties properties;

    public DataInitializer(VehiculeRepository repository, VehiculeProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        if (!properties.donneesDemo()) return;
        add("123 TU 4567", "Renault", "Clio 5", 2023, "Blanc", Categorie.ECONOMIQUE, Carburant.ESSENCE, Boite.MANUELLE, 5, 18500, "95", StatutVehicule.DISPONIBLE);
        add("124 TU 1020", "Peugeot", "208", 2022, "Gris", Categorie.COMPACTE, Carburant.DIESEL, Boite.MANUELLE, 5, 42000, "110", StatutVehicule.DISPONIBLE);
        add("130 TU 8899", "Volkswagen", "Golf 8", 2024, "Noir", Categorie.COMPACTE, Carburant.ESSENCE, Boite.AUTOMATIQUE, 5, 9000, "140", StatutVehicule.DISPONIBLE);
        add("128 TU 3344", "Toyota", "Corolla Hybrid", 2023, "Bleu", Categorie.BERLINE, Carburant.HYBRIDE, Boite.AUTOMATIQUE, 5, 25000, "160", StatutVehicule.DISPONIBLE);
        add("131 TU 7001", "Hyundai", "Tucson", 2024, "Blanc", Categorie.SUV, Carburant.DIESEL, Boite.AUTOMATIQUE, 5, 12000, "210", StatutVehicule.DISPONIBLE);
        add("132 TU 0505", "Tesla", "Model 3", 2024, "Rouge", Categorie.LUXE, Carburant.ELECTRIQUE, Boite.AUTOMATIQUE, 5, 7000, "320", StatutVehicule.DISPONIBLE);
        add("120 TU 2211", "Kia", "Picanto", 2021, "Jaune", Categorie.ECONOMIQUE, Carburant.ESSENCE, Boite.MANUELLE, 4, 67000, "75", StatutVehicule.MAINTENANCE);
        add("126 TU 6060", "Renault", "Trafic", 2022, "Blanc", Categorie.UTILITAIRE, Carburant.DIESEL, Boite.MANUELLE, 9, 88000, "180", StatutVehicule.DISPONIBLE);
    }

    private void add(String immat, String marque, String modele, int annee, String couleur, Categorie categorie,
                     Carburant carburant, Boite boite, int places, int km, String prix, StatutVehicule statut) {
        Vehicule v = new Vehicule();
        v.setImmatriculation(immat);
        v.setMarque(marque);
        v.setModele(modele);
        v.setAnnee(annee);
        v.setCouleur(couleur);
        v.setCategorie(categorie);
        v.setCarburant(carburant);
        v.setBoite(boite);
        v.setNbPlaces(places);
        v.setKilometrage(km);
        v.setPrixJournalier(new BigDecimal(prix));
        v.setStatut(statut);
        v.setImageUrl("https://placehold.co/600x400?text=" + (marque + "+" + modele).replace(' ', '+'));
        repository.save(v);
    }
}

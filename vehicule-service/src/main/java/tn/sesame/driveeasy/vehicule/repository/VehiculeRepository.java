package tn.sesame.driveeasy.vehicule.repository;

import org.springframework.stereotype.Repository;
import tn.sesame.driveeasy.vehicule.entity.Vehicule;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Dépôt en mémoire (RAM) : les données sont perdues à l'arrêt du service. */
@Repository
public class VehiculeRepository {

    private final Map<Long, Vehicule> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public List<Vehicule> findAll() {
        return store.values().stream().sorted(Comparator.comparing(Vehicule::getId)).toList();
    }

    public Optional<Vehicule> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public Optional<Vehicule> findByImmatriculation(String immatriculation) {
        return store.values().stream()
                .filter(v -> v.getImmatriculation().equalsIgnoreCase(immatriculation))
                .findFirst();
    }

    public Vehicule save(Vehicule vehicule) {
        if (vehicule.getId() == null) {
            vehicule.setId(sequence.incrementAndGet());
        }
        store.put(vehicule.getId(), vehicule);
        return vehicule;
    }

    public void deleteById(Long id) {
        store.remove(id);
    }
}

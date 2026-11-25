package tn.sesame.driveeasy.location.repository;

import org.springframework.stereotype.Repository;
import tn.sesame.driveeasy.location.entity.Location;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Dépôt en mémoire (RAM) : les données sont perdues à l'arrêt du service. */
@Repository
public class LocationRepository {

    private final Map<Long, Location> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    /** Les plus récentes d'abord. */
    public List<Location> findAll() {
        return store.values().stream().sorted(Comparator.comparing(Location::getId).reversed()).toList();
    }

    public Optional<Location> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public Location save(Location location) {
        if (location.getId() == null) {
            location.setId(sequence.incrementAndGet());
        }
        store.put(location.getId(), location);
        return location;
    }

    public void deleteById(Long id) {
        store.remove(id);
    }
}

package tn.sesame.driveeasy.client.repository;

import org.springframework.stereotype.Repository;
import tn.sesame.driveeasy.client.entity.Client;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/** Dépôt en mémoire (RAM) : les données sont perdues à l'arrêt du service. */
@Repository
public class ClientRepository {

    private final Map<Long, Client> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public List<Client> findAll() {
        return store.values().stream().sorted(Comparator.comparing(Client::getId)).toList();
    }

    public Optional<Client> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    /** Recherche insensible à la casse sur un champ unique (cin, email, numeroPermis). */
    public Optional<Client> findBy(Function<Client, String> champ, String valeur) {
        return store.values().stream().filter(c -> champ.apply(c).equalsIgnoreCase(valeur)).findFirst();
    }

    public Client save(Client client) {
        if (client.getId() == null) {
            client.setId(sequence.incrementAndGet());
        }
        store.put(client.getId(), client);
        return client;
    }

    public void deleteById(Long id) {
        store.remove(id);
    }
}

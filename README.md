
## Architecture

| Composant | Port | Rôle |
|---|---:|---|
| service-registry | 8761 | Eureka Server : annuaire des services |
| config-server | 8888 | Configuration centralisée (`config-repo/`) |
| vehicule-service | 8081 | Parc automobile, catégories, disponibilité |
| client-service | 8082 | Clients, éligibilité (âge ≥ 21 ans, permis valide), catégorie de fidélité |
| location-service | 8083 | Locations ; orchestre client-service et vehicule-service |
| api-gateway | 8080 | Point d'entrée unique : routage `lb://`, CORS (`http://localhost:4200`), filtre `X-DriveEasy-Gateway` |
| Zipkin (Docker) | 9411 | Traçage distribué (échantillonnage 1.0) |

Chaque microservice métier suit la structure `controller → service → repository` (dépôt en mémoire),
avec des **DTO** (`dto/`) séparés des **entités** (`entity/`) et un `GlobalExceptionHandler`
qui renvoie un JSON d'erreur uniforme (`400, 404, 409, 500`, et `503` si un service distant est indisponible).

Les appels inter-services utilisent des **clients déclaratifs `@HttpExchange`** (comme `EmployeeClient`
dans le TP) : de simples interfaces (`location-service/.../client/VehiculeServiceClient`,
`ClientServiceClient`, `client-service/.../remote/LocationServiceClient`) dont Spring génère
l'implémentation dans `config/WebClientConfig`. Le `WebClient` utilise le load balancing Eureka
(`http://vehicule-service`), propage l'identifiant de trace Zipkin et traduit les erreurs distantes
(404 → 404, 409 → 409, autre 4xx → 400, service injoignable → 503).

## Prérequis

- **JDK 21** (`java -version` doit afficher 21)
- Maven 3.9+ (ou le wrapper `mvnw.cmd` fourni)
- Docker Desktop (pour Zipkin)

## Lancement

Depuis le dossier `driveeasy-backend` :

```powershell
mvn clean package
```

Puis, chacun dans un terminal, **dans cet ordre** :

```powershell
./scripts/start-zipkin.ps1                                  # http://localhost:9411 (à tout moment)
./scripts/start-registry.ps1                                # http://localhost:8761
./scripts/start-config-server.ps1                           # http://localhost:8888/vehicule-service/default
./scripts/start-backend.ps1 -Service vehicule-service
./scripts/start-backend.ps1 -Service client-service
./scripts/start-backend.ps1 -Service location-service
./scripts/start-backend.ps1 -Service api-gateway             # http://localhost:8080
```

Alternative sans Maven : `java -jar <service>/target/<service>-1.0.0-SNAPSHOT.jar` (depuis `driveeasy-backend`).

Attendre que les 4 services apparaissent sur le tableau de bord Eureka avant de tester via la gateway.


## Règles de gestion implémentées

- Immatriculation, CIN, email et numéro de permis **uniques** → `409`.
- Prix journalier > 0 et année ≤ année courante → `400`.
- Un véhicule `MAINTENANCE` ou `LOUE`, ou déjà réservé sur la période, ne peut pas être réservé → `409`.
- Suppression refusée si véhicule loué / réservé, ou si le client a une location active → `409`.
- Éligibilité : âge ≥ 21 ans et permis valide jusqu'à la fin de la location.
- **Prix** = jours × prix journalier × coefficient catégorie véhicule − remise fidélité (FIDELE 5 %, VIP 10 %).
- **Pénalité** = 1,5 × prix journalier appliqué × jours de retard.
- Départ : véhicule → `LOUE` ; retour : véhicule → `DISPONIBLE` + kilométrage mis à jour.
- Passage automatique `FIDELE` après 5 locations terminées, `VIP` après 15 (piloté par location-service).
- **Cohérence distribuée** : à la création, la réservation locale est supprimée si le blocage de la
  période échoue côté vehicule-service (compensation) ; pour départ / retour / annulation, l'appel distant
  est fait d'abord, donc un échec laisse la location dans son état précédent.

## API (via la gateway `http://localhost:8080`)

### Véhicules — `/api/vehicules`

| Méthode | URI | Description |
|---|---|---|
| GET | `/api/vehicules?categorie=&carburant=&statut=&prixMax=` | Liste filtrée |
| GET | `/api/vehicules/{id}` | Détail |
| POST | `/api/vehicules` | Création (`201`) |
| PUT | `/api/vehicules/{id}` | Modification |
| DELETE | `/api/vehicules/{id}` | Suppression (`204`, `409` si loué) |
| PATCH | `/api/vehicules/{id}/statut` | `{"statut":"LOUE","kilometrage":12000}` |
| GET | `/api/vehicules/disponibles?dateDebut=&dateFin=` | Disponibles sur la période |
| GET | `/api/vehicules/{id}/disponibilite?dateDebut=&dateFin=` | Disponibilité d'un véhicule |
| POST / DELETE | `/api/vehicules/{id}/reservations[/{locationId}]` | Blocage / libération d'une période (interne) |

### Clients — `/api/clients`

| Méthode | URI | Description |
|---|---|---|
| GET | `/api/clients?q=&nom=&cin=&email=` | Recherche |
| GET | `/api/clients/{id}` | Détail (avec `age` et `permisExpire`) |
| POST | `/api/clients` | Création (`201`) |
| PUT | `/api/clients/{id}` | Modification |
| DELETE | `/api/clients/{id}` | Suppression (`409` si location active) |
| GET | `/api/clients/{id}/eligibilite?dateFin=` | Éligibilité + motifs |
| PATCH | `/api/clients/{id}/categorie` | `{"categorieClient":"VIP"}` |

### Locations — `/api/locations`

| Méthode | URI | Description |
|---|---|---|
| GET | `/api/locations?statut=&clientId=&vehiculeId=&dateDebut=&dateFin=&page=0&size=10` | Liste paginée |
| GET | `/api/locations/{id}` | Détail enrichi (client + véhicule) |
| POST | `/api/locations` | Réservation `{"clientId":1,"vehiculeId":2,"dateDebut":"2026-12-01","dateFin":"2026-12-04"}` |
| POST | `/api/locations/simulation` | Simulation du prix (même corps) |
| PATCH | `/api/locations/{id}/depart` | Départ → `EN_COURS` |
| PATCH | `/api/locations/{id}/retour` | `{"kmRetour":19000,"dateRetour":"2026-12-06"}` (date optionnelle) |
| PATCH | `/api/locations/{id}/annulation` | Annulation d'une réservation |
| GET | `/api/locations/statistiques` | CA total / du mois, taux d'occupation, top 5 véhicules |

## Jeu de données de démonstration

| Clients | Particularité |
|---|---|
| 1 — Amine Ben Salah | FIDELE (5 locations terminées) |
| 2 — Sarra Trabelsi | STANDARD (2 locations terminées) |
| 3 — Mehdi Gharbi | VIP (15 locations terminées) |
| 4 — Yassine Jaziri | 19 ans → **non éligible** |
| 5 — Leila Mansour | permis expiré → **non éligible** |

Véhicules 1 à 8 (Clio, 208, Golf, Corolla, Tucson, Model 3, Picanto en **MAINTENANCE**, Trafic).
Aucun compte de test : l'API n'a pas d'authentification.

## Tests

### Collection Postman (tests de bout en bout via la gateway)

[`postman/DriveEasy.postman_collection.json`](postman/DriveEasy.postman_collection.json) : 49 requêtes et
108 assertions réparties en 6 dossiers (infrastructure Eureka / Config Server / CORS, véhicules, clients,
cycle complet d'une location, consultation et statistiques, fidélité et nettoyage). La collection crée
son propre client et son propre véhicule avec des valeurs aléatoires et des dates relatives au jour
courant : elle peut être relancée sans redémarrer les services.

- **Postman** : *Import* → choisir le fichier → *Run collection* (dans l'ordre).
- **Postman CLI** (tous les services démarrés) :

```powershell
postman collection run postman/DriveEasy.postman_collection.json
```

La collection est générée par `node postman/generate-collection.mjs` (modifier ce script plutôt que le JSON).

### Tests unitaires

```powershell
mvn test
```

Tests unitaires de la tarification (`location-service/.../TarificationServiceTest`) : prix, coefficient
de catégorie, remises FIDELE/VIP, pénalités et seuils de fidélité.

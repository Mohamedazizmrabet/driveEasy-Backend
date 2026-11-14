# DriveEasy Backend

Backend workspace for the DriveEasy vehicle-rental management project at Université SESAME. It contains the six Spring Boot applications and a Git-backed local configuration repository. Business data is held in each service's RAM only; no database is used, and data resets when a service restarts.

The university document mentions a datasource property in one generic Config Server checklist. The project-specific rule explicitly bans databases, so DriveEasy intentionally has no datasource configuration, database dependency, or database container.

## Planned services

| Application | Port | Responsibility |
|---|---:|---|
| service-registry | 8761 | Eureka service discovery |
| config-server | 8888 | Central configuration served from `config-repo/` |
| vehicule-service | 8081 | Fleet data and availability |
| client-service | 8082 | Customer data and eligibility |
| location-service | 8083 | Reservations, rentals, returns, and statistics |
| api-gateway | 8080 | Front door for the Angular application |
| Zipkin | 9411 | Distributed request traces (Docker Compose) |

## Environment

Use Java 21. `mvnw.cmd` bootstraps the pinned Maven 3.9.16 distribution into the ignored `.mvn/dists/` folder, so a separate Maven installation is not required. Docker Desktop is needed for Zipkin. The selected compatible framework line is Spring Boot 3.5.16 with Spring Cloud 2025.0.3. Spring's published release notes state these were the final open-source releases of their respective 3.5.x and 2025.0.x lines in 2026; the university's Spring Boot 3.x requirement takes precedence, so this support-lifecycle limitation is documented.

## Current status

The workspace is being implemented in phases. See [`../PROJECT_PROGRESS.md`](../PROJECT_PROGRESS.md) for verified environment results, completed work, and remaining tasks. The architecture diagram is in [`docs/architecture.md`](docs/architecture.md).

## Startup order (once Java 21 and Maven Wrapper are ready)

Open a PowerShell terminal in `driveeasy-backend` for each foreground service and leave each running:

1. `./scripts/start-registry.ps1` — Eureka dashboard at http://localhost:8761.
2. `./scripts/start-config-server.ps1` — config endpoint at http://localhost:8888/vehicule-service/default.
3. `./scripts/start-backend.ps1 -Service vehicule-service`.
4. `./scripts/start-backend.ps1 -Service client-service`.
5. `./scripts/start-backend.ps1 -Service location-service`.
6. `./scripts/start-backend.ps1 -Service api-gateway`.
7. In another terminal, run `./scripts/start-zipkin.ps1`; Zipkin is at http://localhost:9411.

You can run the services manually with `./mvnw.cmd -pl <service-folder> spring-boot:run`. The API and tests will be documented after their implementations and actual verification.

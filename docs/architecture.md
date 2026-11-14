# DriveEasy architecture

```mermaid
flowchart LR
    UI[Angular 22<br/>localhost:4200] -->|/api/vehicules, /api/clients, /api/locations| GW[API Gateway<br/>:8080]
    GW -->|Eureka + load balancing| VS[vehicule-service<br/>:8081]
    GW -->|Eureka + load balancing| CS[client-service<br/>:8082]
    GW -->|Eureka + load balancing| LS[location-service<br/>:8083]
    LS -->|service discovery| VS
    LS -->|service discovery| CS
    VS -.registers.-> ER[Eureka Registry<br/>:8761]
    CS -.registers.-> ER
    LS -.registers.-> ER
    GW -.registers.-> ER
    VS -->|central properties| CF[Config Server<br/>:8888]
    CS -->|central properties| CF
    LS -->|central properties| CF
    GW -->|central properties| CF
    CF --> CR[(Git working tree<br/>config-repo/)]
    GW -.traces.-> ZK[Zipkin<br/>:9411]
    LS -.traces.-> ZK
    VS -.traces.-> ZK
    CS -.traces.-> ZK
```

Each business service owns a separate in-memory repository. The location service checks customer eligibility and vehicle availability through Eureka-discovered HTTP clients. Rental periods are authoritative in location-service; the vehicle service's date availability endpoint asks it for conflicting active reservations. No database is used; all in-memory business data resets when its owning service restarts.

The Config Server serves the `config-repo/` path from the backend Git repository. For local runs, start it with the backend directory as the working directory so its relative `file:.` URI resolves to this repository. Do not add passwords or tokens to the config repository.

# BOE Proxy Service

Internal Spring Boot proxy that encapsulates all BOE/Raylight communication for the Entity application. The Entity frontend calls this proxy's REST API — it never interacts with BOE or the EAG gateway directly.

## Architecture

```
Entity Frontend → BOE Proxy Service → EAG API Gateway (mTLS) → BOE Platform (Raylight)
```

### Internal Components

| Component | Class | Purpose |
|-----------|-------|---------|
| Token Manager | `BoeTokenManager` | BOE session auth, token caching, proactive refresh |
| Report Registry | `ReportRegistry` | Maps Entity report codes → BOE Document IDs |
| Report Executor | `ReportExecutor` | Orchestrates sync/async execution, manages job tracker |
| Polling Engine | `PollingEngine` | Background status checks for async report jobs |
| Response Transformer | `ResponseTransformer` | Converts Raylight JSON → Entity DTOs |
| Circuit Breaker | `CircuitBreakerService` | Protects against cascading BOE failures |

## API

```
POST   /api/reports/run                  Submit a report
GET    /api/reports/{jobId}/status       Poll job status
GET    /api/reports/{jobId}/results      Retrieve completed results
DELETE /api/reports/{jobId}              Cancel a running job
```

## Build & Run

### Local Development

```bash
mvn clean package -DskipTests
java -jar target/boe-proxy-service-1.0.0-SNAPSHOT.jar --spring.profiles.active=local
```

### Docker

```bash
mvn clean package
docker build -t boe-proxy-service .
docker run -p 8080:8080 \
  -e EAG_BASE_URL=https://eag-gateway.dstest.abc.com \
  -e BOE_USERNAME=service_user \
  -e BOE_PASSWORD=secret \
  boe-proxy-service
```

### With mTLS

```bash
docker run -p 8080:8080 \
  -e EAG_BASE_URL=https://eag-gateway.dstest.abc.com \
  -e BOE_USERNAME=service_user \
  -e BOE_PASSWORD=secret \
  -e MTLS_ENABLED=true \
  -e MTLS_CERT_PATH=/certs/client.pem \
  -e MTLS_KEY_PATH=/certs/client.key \
  -v /path/to/certs:/certs:ro \
  boe-proxy-service
```

## Configuration

All configuration is externalized via `application.yml` with environment variable overrides. See `src/main/resources/application.yml` for the full list of properties.

Key environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `EAG_BASE_URL` | EAG gateway base URL | — |
| `BOE_USERNAME` | BOE service account username | — |
| `BOE_PASSWORD` | BOE service account password | — |
| `MTLS_ENABLED` | Enable mTLS for EAG | `false` |
| `MTLS_CERT_PATH` | Path to mTLS client certificate | — |
| `MTLS_KEY_PATH` | Path to mTLS client key | — |

## Testing

```bash
# Unit tests
mvn test

# Quick smoke test (local profile)
curl -X POST http://localhost:8080/api/reports/run \
  -H "Content-Type: application/json" \
  -d '{"reportCode": "QUICK_LOOKUP", "parameters": {"TIN": "123456789"}}'
```

## TODOs

Items marked `TODO` in the codebase that require action once BOE/EAG details are confirmed:

- `ReportRegistry.init()` — Replace placeholder Document IDs with actual BOE IDs per environment.
- `ReportExecutor.extractScheduleId()` — Parse actual Raylight schedule response JSON.
- `PollingEngine.parseJobStatus()` — Parse actual Raylight status response structure.
- `ResponseTransformer` — Validate against real Raylight response payloads and adjust JSON paths.
- mTLS certificate configuration — Finalize cert format (PEM vs PKCS12) based on EAG requirements.
- Credential source — Wire up AWS Secrets Manager for ECS deployment.

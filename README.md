# WiseWay

Plan your route. Stay ready.

WiseWay is a local-first commute companion for comparing illustrative transport options, saving recurring routes, tracking actual commute expenses, and finding simulated Plan B alternatives.

## What is implemented

- React/Vite responsive web app with Overview, Compare, Plan B, Saved routes, and Expenses views.
- Java 21/Spring Boot REST API under `/api/v1`.
- SQLite persistence for saved routes and actual journey logs.
- Location search, configurable routing providers, route-specific distance/duration, and mode-specific fare estimates.
- Compare displays returned road geometry on an OpenStreetMap/Leaflet map when the routing provider supplies it.
- Deterministic recommendation engine with Budget Saver, Time Saver, Comfort First, and Balanced profiles.
- Fare/deadline hard constraints, stable tie-breaking, explanations, and explicit no-fit results.
- Narrow local-development CORS and consistent validation/not-found/server errors.

OSRM can be configured for road distance and duration. Public transport availability and fares remain unknown unless the configured dataset verifies them. Formula-based fares are explicitly estimates, not official prices. Plan B is a local simulation, not a live disruption alert.

## Prerequisites

- Java 21
- Maven 3.9+ (or an IDE with Maven support)
- Node.js LTS and npm

## Run the backend

```powershell
cd backend
mvn spring-boot:run
```

On Windows, if `mvn` resolves to a Python package and fails with `ModuleNotFoundError: No module named 'pwd'`, that is a PATH collision unrelated to WiseWay. Use a real Apache Maven installation instead:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Alternatively set `MAVEN_HOME` to your Apache Maven directory and run `mvnw.cmd`. Do not use the Python `mvn.exe` shim.

The API starts on `http://localhost:8080`; SQLite is created at `backend/data/wiseway.db`.

## Run the frontend

```powershell
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. Set `VITE_API_URL` if the backend uses a different base URL.

### Routing provider

The default provider is the local curated demo route catalog. To use a local OSRM instance, configure the backend before starting it:

```powershell
$env:WISEWAY_ROUTING_PROVIDER="osrm"
$env:WISEWAY_OSRM_BASE_URL="http://localhost:5000"
$env:WISEWAY_ROUTING_TIMEOUT_MS="5000"
.\mvnw.cmd spring-boot:run
```

OSRM must be supplied with the map data and profile appropriate for the region. A public demonstration endpoint is suitable only for limited manual testing; it is not a production dependency. WiseWay returns `NO_ROUTE` when OSRM fails or returns no route and does not substitute straight-line or fabricated distance.

### Geocoding provider

The default location search uses the bundled demo catalog. For provider-backed search, configure Photon:

```powershell
$env:WISEWAY_GEOCODING_PROVIDER="photon"
$env:WISEWAY_PHOTON_BASE_URL="https://photon.komoot.io"
```

Photon results include a display name, context, latitude and longitude. Use reasonable request rates, cache repeated searches, and provide OpenStreetMap/Photon attribution in deployments. The public Photon demo service is not guaranteed production infrastructure. WiseWay does not use public Nominatim autocomplete.

When OSRM is enabled, Compare sends the selected coordinates to OSRM and renders its returned GeoJSON route geometry. The map does not represent live traffic or public-transport availability. Curated demo routes without geometry show a clear unavailable-map state rather than a fabricated straight line.

## Test and build

```powershell
cd backend
mvn test
mvn package

cd ../frontend
npm test
npm run build
```

## API overview

- `GET /api/v1/health`
- `GET /api/v1/locations/search?q=...`
- `POST /api/v1/recommendations/compare`
- `POST /api/v1/recommendations/plan-b`
- `GET|POST|PUT|DELETE /api/v1/saved-routes`
- `GET|POST|DELETE /api/v1/journey-logs`
- `GET /api/v1/expenses/summary?from=YYYY-MM-DD&to=YYYY-MM-DD&groupBy=MODE`

Recommendation requests use `preference`, optional `constraints.maxFare`, and optional `constraints.arriveBy`. Expense amounts are actual user-entered amounts and use exact decimal values in the backend.

## Architecture

The backend is a small layered monolith: controllers bind/validate HTTP requests, services orchestrate use cases, repositories persist JPA entities, and the pure `RecommendationEngine` applies scenario exclusions and hard constraints before scoring. REST responses use DTO records rather than exposing entities.

The frontend keeps HTTP calls in `src/api.js`, manages state locally per view, and renders loading, empty, validation, error, success, and no-fit states.

## Demo recording

For an internship demo, record: Overview → Compare with a profile/budget change → Plan B scenario → save a route → log an actual expense. Keep the persistent “Illustrative data · not live transit” disclosure visible.

## Limitations

This is intentionally a single-user local MVP. Location search currently uses the bundled demo catalog; OSRM routing is opt-in and requires a separately managed OSRM service. It does not provide GPS, official transit feeds/fares, bookings, payments, or cloud synchronization. OSM/OSRM users must follow the applicable attribution and service-use policies.

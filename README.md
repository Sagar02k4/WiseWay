# WiseWay

Plan your route. Stay ready.

WiseWay is a local-first commute companion for comparing illustrative transport options, saving recurring routes, tracking actual commute expenses, and finding simulated Plan B alternatives.

## What is implemented

- React/Vite responsive web app with Overview, Compare, Plan B, Saved routes, and Expenses views.
- Java 21/Spring Boot REST API under `/api/v1`.
- SQLite persistence for saved routes and actual journey logs.
- Deterministic recommendation engine with Budget Saver, Time Saver, Comfort First, and Balanced profiles.
- Fare/deadline hard constraints, stable tie-breaking, explanations, and explicit no-fit results.
- Narrow local-development CORS and consistent validation/not-found/server errors.

All transport durations and fares are curated sample estimates. They are not live schedules, traffic, service status, route guarantees, or ticket prices. Plan B is a local simulation, not a live disruption alert.

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

This is intentionally a single-user local MVP. It does not provide authentication, live routing, GPS, official transit data, bookings, payments, or cloud synchronization.

# WiseWay development progress

## Current increment: offline Delhi/NCR transit data import

### Completed

- Confirmed the working branch is `feature/delhi-gurugram-transit-routing`.
- Added `backend/scripts/transit/Download-NorthernZone.ps1` with HTTP range probing, 16 MB resumable chunks, per-chunk retries, Content-Range checks, final Content-Length validation, and MD5 verification.
- Resumed the existing partial download rather than restarting it.
- Verified the final PBF at `backend/data/transit/raw/northern-zone-latest.osm.pbf`:
  - Content-Length: `224864535` bytes
  - MD5: `74a58d850f10edfedd7b1e50303bbb02`
- Added `backend/scripts/transit/extract_osm_transit.py` for offline relation/way/stop extraction and NCR coverage reporting.
- Added `backend/scripts/transit/extract_osm_transit_stream.py`, a three-pass memory-bounded extractor.
- Generated and validated `backend/data/transit/normalized/ncr-transit.json` and `coverage-report.md`.
- Added the Python virtual environment path to `.gitignore`.

### Current status

The three-pass PyOsmium extraction completed in approximately 95 seconds without native `osmium-tool`. The normalized dataset is 17,505,209 bytes and contains 762 mapped route relations, 1,166 stops, and 675 complete route geometries. These are OSM-mapped relations, not proof of live service, schedules, fares, or connected journeys. Transit API integration remains pending.

The dataset audit found 540 route relations with associated mapped stops (2,215 route-stop associations), 675 complete geometries, and 87 incomplete geometries. A conservative `OsmTransitRoutingProvider` now loads complete relations and accepts only direct journeys whose origin and destination are within 1.5 km of ordered stops. It rejects crossing-only routes and does not invent transfers, schedules, fares, or live availability. It is currently a provider seam and still requires API DTO/frontend wiring before transit journeys appear in Compare or Plan B.

Imported OSM stops are now included in `/api/v1/locations/search` with deterministic, limited, case-insensitive matching and `OSM transit stop` context. Real browser verification succeeded on ports 8081/5174 for `Samaypur Badli → Haiderpur Badli Mor`: Compare and Plan B both displayed the mapped Metro journey and Leaflet rendered its GeoJSON geometry. The frontend required an explicit `VITE_API_URL` and backend CORS origin for the alternate verification ports.

### Remaining work

- Implement direct/one-transfer routing from verified relations and stop relationships.
- Integrate imported geometry into Compare and Plan B without using OSRM geometry for transit modes.
- Add fixture-based backend/frontend tests and browser verification.
- Update API documentation, commit logical changes on a feature branch, and push if credentials permit.

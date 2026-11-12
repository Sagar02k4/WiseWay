# WiseWay development progress

## Current increment: offline Delhi/NCR transit data import

### Completed

- Confirmed the working branch is `feature/location-picker-geocoding`.
- Added `backend/scripts/transit/Download-NorthernZone.ps1` with HTTP range probing, 16 MB resumable chunks, per-chunk retries, Content-Range checks, final Content-Length validation, and MD5 verification.
- Resumed the existing partial download rather than restarting it.
- Verified the final PBF at `backend/data/transit/raw/northern-zone-latest.osm.pbf`:
  - Content-Length: `224864535` bytes
  - MD5: `74a58d850f10edfedd7b1e50303bbb02`
- Added `backend/scripts/transit/extract_osm_transit.py` for offline relation/way/stop extraction and NCR coverage reporting.
- Added the Python virtual environment path to `.gitignore`.

### Current status

The PBF is valid, but the first complete Python binding pass did not finish within the available local processing window and produced no normalized output. No transit route or geometry is being claimed from the dataset yet. The next action is to run the extractor with a native `osmium-tool` installation or an optimized staged parser, inspect its coverage report, and only then connect verified relations to the backend transit provider.

### Remaining work

- Finish offline extraction and validate coverage for Delhi, Gurugram, Faridabad, Noida and Ghaziabad.
- Implement direct/one-transfer routing from verified relations and stop relationships.
- Integrate imported geometry into Compare and Plan B without using OSRM geometry for transit modes.
- Add fixture-based backend/frontend tests and browser verification.
- Update API documentation, commit logical changes on a feature branch, and push if credentials permit.

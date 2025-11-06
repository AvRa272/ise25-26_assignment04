# Changelog

All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added
- Implemented complete OSM POS import feature with real OpenStreetMap API integration
- Extended `OsmNode` domain model with all required fields (latitude, longitude, tags)
- Implemented XML parsing for OSM API responses in `OsmDataServiceImpl`
- Added comprehensive OSM-to-POS conversion logic with helper methods:
  - Field validation for required OSM tags
  - Amenity type to POS type mapping
  - Campus determination based on geographical coordinates
  - Postal code parsing and validation
  - Description generation from OSM tags
- Enhanced `OsmNodeMissingFieldsException` to include list of specific missing fields
- Added comprehensive Javadoc documentation for all new methods and classes
- Created `AGENTS.md` - comprehensive AI agent guide for the project
- Created implementation plan documentation in `doc/prp/`

### Changed
- Replaced stub implementation in `OsmDataServiceImpl` with real HTTP client using RestTemplate
- Updated `PosServiceImpl.convertOsmNodeToPos()` from hardcoded data to dynamic field mapping
- `OsmNodeMissingFieldsException` now accepts and reports specific missing field names

## Previous Changes

- Fix broken test case in `PosSystemTests` (assignment 3).
- Extend GitHub Actions triggers to include pushes to feature branches (assignment 3).
- Add new `POST` endpoint `/api/pos/import/osm/{nodeId}` that allows API users to import a `POS` based on an OpenStreetMap node.
- Extend `PosService` interface by adding a `importFromOsmNode` method.
- Add example of new OSM import endpoint to `README` file.

## Removed

- n/a

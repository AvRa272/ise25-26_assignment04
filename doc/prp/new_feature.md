# Project Requirement Proposal (PRP)
<!-- Adapted from https://github.com/Wirasm/PRPs-agentic-eng/tree/development/PRPs -->

You are a senior software engineer.
Use the information below to implement a new feature or improvement in this software project.

## Goal

**Feature Goal**: Implement a feature to import a new Point of Sale (POS) based on an existing OpenStreetMap (OSM) entry.

**Deliverable**: Add an API endpoint that allows creating a new POS entry in the local database from an OSM node.

**Success Definition**: A new POS is successfully created and stored in the database using data retrieved from a valid OSM entry.

## User Persona

**Target User**: Admin

**Use Case**: Add a new POS to the database based on data from OpenStreetMap.

**User Journey**: 
1. Admin identifies a POS in OSM that does not yet exist in the Campus-Coffee system.
2. Admin provides the OSM node ID or link via the REST API.
3. The system fetches the XML from the OSM API, extracts relevant data, and creates a new POS entry.

**Pain Points Addressed**: Manual creation of POS entries is time-consuming and prone to data entry errors.

## Why

- This feature automates POS import, saving time, reducing manual errors, and helping maintain accurate and up-to-date location data.
- Streamlines the process of adding new coffee shops and cafés to the Campus-Coffee system.
- Ensures data consistency by importing directly from OpenStreetMap's authoritative source.

## What

### Technical Requirements

Implement a REST API endpoint such as:

```shell
curl --request POST http://localhost:8080/api/pos/import/osm/5589879349
```

(where `5589879349` is a valid OSM node ID).

The system should:
- Extract the node ID from the request path.
- Retrieve XML data from `https://www.openstreetmap.org/api/0.6/node/<nodeID>`.
- Parse relevant fields (e.g., name, coordinates, tags).
- Create and persist a new POS entity in the local database.

### Success Criteria

- [ ] REST API endpoint `/api/pos/import/osm/{nodeId}` is implemented and accepts POST requests.
- [ ] System successfully extracts the OSM node ID from the request path.
- [ ] System retrieves XML data from the OpenStreetMap API using the provided node ID.
- [ ] System parses relevant fields including name, coordinates (latitude/longitude), and tags from the OSM XML response.
- [ ] System maps OSM data to POS entity fields appropriately.
- [ ] New POS entity is created and persisted in the local database.
- [ ] Given a valid OSM node ID, the system correctly imports, parses, and stores the POS in the database.
- [ ] Appropriate error handling is implemented for invalid node IDs or API failures.

## Documentation & References

MUST READ - Include the following information in your context window.

The `README.md` file at the root of the project contains setup instructions and example API calls.

This Java Spring Boot application is structured as a multi-module Maven project following the ports-and-adapters architectural pattern.
There are the following submodules:

`api` - Maven submodule for controller adapter.

`application` - Maven submodule for Spring Boot application, test data import, and system tests.

`data` - Maven submodule for data adapter.

`domain` - Maven submodule for domain model, main business logic, and ports.

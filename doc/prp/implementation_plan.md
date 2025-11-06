# Implementation Plan: OSM POS Import Feature

## Executive Summary

The OSM POS import feature is **partially implemented** with scaffolding in place. The REST API endpoint exists, but the core logic uses hardcoded data. This plan outlines the steps to complete the implementation by adding real OSM API integration and data parsing.

---

## Current State Analysis

### ✅ Already Implemented

1. **API Layer** (`api` module)
   - `PosController.java` - Endpoint exists: `POST /api/pos/import/osm/{nodeId}` (lines 51-60)
   - Proper HTTP response with 201 Created status
   - Exception handling via `GlobalExceptionHandler`

2. **Domain Layer** (`domain` module)
   - `PosService.importFromOsmNode()` interface method defined (line 86 in PosService.java)
   - `PosServiceImpl.importFromOsmNode()` method exists (lines 68-80) - calls stub services
   - Exception classes: `OsmNodeNotFoundException`, `OsmNodeMissingFieldsException`
   - `OsmNode` domain model (currently minimal)

3. **Data Layer** (`data` module)
   - `OsmDataService` port interface defined
   - `OsmDataServiceImpl` stub implementation exists (returns hardcoded data)
   - Database entities and mappers for POS are complete

### ❌ Needs Implementation (TODOs)

1. **OsmNode Model Enhancement** - Add fields for name, coordinates, tags, etc.
2. **OSM API Integration** - Real HTTP call to OpenStreetMap API
3. **XML Parsing** - Parse OSM API XML response
4. **Data Mapping** - Convert OSM tags to POS fields
5. **Error Handling** - Handle API failures, network issues, invalid data
6. **Testing** - Unit and integration tests

---

## Required POS Fields

Based on `Pos.java` domain model, we need:
- `name` (String) - **Required**
- `description` (String) - **Required** 
- `type` (PosType enum: CAFE, BAKERY, etc.) - **Required**
- `campus` (CampusType enum: ALTSTADT, NEUENHEIMER_FELD, etc.) - **Required**
- `street` (String) - **Required**
- `houseNumber` (String) - **Required**
- `postalCode` (Integer) - **Required**
- `city` (String) - **Required**

---

## Implementation Tasks

### Phase 1: Extend OsmNode Domain Model

**File:** `domain/src/main/java/de/seuhd/campuscoffee/domain/model/OsmNode.java`

**Current State:** Only contains `nodeId`

**Required Changes:**
```java
@Builder
public record OsmNode(
    @NonNull Long nodeId,
    @NonNull Double latitude,      // OSM: lat attribute
    @NonNull Double longitude,      // OSM: lon attribute
    @Nullable String name,          // OSM: tag k="name"
    @Nullable String amenity,       // OSM: tag k="amenity"
    @Nullable String cuisine,       // OSM: tag k="cuisine"
    @Nullable String street,        // OSM: tag k="addr:street"
    @Nullable String houseNumber,   // OSM: tag k="addr:housenumber"
    @Nullable String postcode,      // OSM: tag k="addr:postcode"
    @Nullable String city,          // OSM: tag k="addr:city"
    @Nullable String website,       // OSM: tag k="website"
    @Nullable String phone,         // OSM: tag k="phone"
    @Nullable String openingHours   // OSM: tag k="opening_hours"
) {
    // Additional tag fields as needed
}
```

**Acceptance Criteria:**
- [ ] OsmNode record includes all necessary fields
- [ ] Fields are properly annotated with @NonNull/@Nullable
- [ ] Javadoc describes the OSM tag mapping for each field

---

### Phase 2: Implement OSM API Client

**File:** `data/src/main/java/de/seuhd/campuscoffee/data/impl/OsmDataServiceImpl.java`

**Current State:** Stub returning hardcoded data

**Required Changes:**

1. **Add Dependencies** (if needed)
   - Spring Boot already includes `RestTemplate` and `WebClient`
   - Consider using `RestTemplate` for simplicity or `WebClient` for reactive approach
   - XML parsing: Use Spring's built-in XML support or add lightweight parser

2. **Implementation Steps:**

   a. **Configure HTTP Client**
   ```java
   @Service
   @Slf4j
   class OsmDataServiceImpl implements OsmDataService {
       private static final String OSM_API_BASE_URL = "https://www.openstreetmap.org/api/0.6/node/";
       private final RestTemplate restTemplate;
       
       public OsmDataServiceImpl(RestTemplateBuilder restTemplateBuilder) {
           this.restTemplate = restTemplateBuilder
               .setConnectTimeout(Duration.ofSeconds(5))
               .setReadTimeout(Duration.ofSeconds(10))
               .build();
       }
   }
   ```

   b. **Fetch OSM Node**
   ```java
   @Override
   public @NonNull OsmNode fetchNode(@NonNull Long nodeId) throws OsmNodeNotFoundException {
       try {
           String url = OSM_API_BASE_URL + nodeId;
           log.debug("Fetching OSM node {} from {}", nodeId, url);
           
           String xmlResponse = restTemplate.getForObject(url, String.class);
           
           if (xmlResponse == null || xmlResponse.isEmpty()) {
               throw new OsmNodeNotFoundException(nodeId);
           }
           
           return parseOsmXml(xmlResponse, nodeId);
           
       } catch (RestClientException e) {
           log.error("Failed to fetch OSM node {}: {}", nodeId, e.getMessage());
           throw new OsmNodeNotFoundException(nodeId);
       }
   }
   ```

   c. **Parse XML Response**
   ```java
   private OsmNode parseOsmXml(String xmlResponse, Long nodeId) {
       // OSM XML structure:
       // <osm>
       //   <node id="5589879349" lat="49.4134" lon="8.6889">
       //     <tag k="name" v="Rada Coffee & Rösterei"/>
       //     <tag k="amenity" v="cafe"/>
       //     <tag k="addr:street" v="Untere Straße"/>
       //     <tag k="addr:housenumber" v="21"/>
       //     <tag k="addr:postcode" v="69117"/>
       //     <tag k="addr:city" v="Heidelberg"/>
       //   </node>
       // </osm>
       
       // Use DOM, SAX, or StAX parser
       // Extract lat/lon attributes and tag key-value pairs
       // Build and return OsmNode
   }
   ```

**XML Parsing Options:**
1. **DOM Parser** (Simple, suitable for small responses)
2. **SAX Parser** (Event-driven, memory efficient)
3. **StAX Parser** (Pull-based, good balance)
4. **Spring OXM** (Object-XML Mapping)

**Recommended:** Use Java's built-in `DocumentBuilder` (DOM) for simplicity:
```java
DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
DocumentBuilder builder = factory.newDocumentBuilder();
Document doc = builder.parse(new InputSource(new StringReader(xmlResponse)));
```

**Acceptance Criteria:**
- [ ] Makes real HTTP GET request to OSM API
- [ ] Parses XML response correctly
- [ ] Extracts node attributes (id, lat, lon)
- [ ] Extracts all relevant tags (name, address, amenity, etc.)
- [ ] Throws `OsmNodeNotFoundException` for invalid/non-existent nodes
- [ ] Handles network errors gracefully
- [ ] Includes proper logging

---

### Phase 3: Implement OSM-to-POS Conversion Logic

**File:** `domain/src/main/java/de/seuhd/campuscoffee/domain/impl/PosServiceImpl.java`

**Current State:** Method `convertOsmNodeToPos()` returns hardcoded data (lines 86-101)

**Required Changes:**

```java
private @NonNull Pos convertOsmNodeToPos(@NonNull OsmNode osmNode) {
    log.debug("Converting OSM node {} to POS", osmNode.nodeId());
    
    // Validate required fields
    validateRequiredFields(osmNode);
    
    // Extract and map fields
    String name = extractName(osmNode);
    String description = extractDescription(osmNode);
    PosType type = mapAmenityToType(osmNode.amenity());
    CampusType campus = determineCampus(osmNode.latitude(), osmNode.longitude());
    String street = osmNode.street();
    String houseNumber = osmNode.houseNumber();
    Integer postalCode = parsePostalCode(osmNode.postcode());
    String city = osmNode.city();
    
    return Pos.builder()
            .name(name)
            .description(description)
            .type(type)
            .campus(campus)
            .street(street)
            .houseNumber(houseNumber)
            .postalCode(postalCode)
            .city(city)
            .build();
}
```

**Helper Methods Needed:**

1. **Field Validation**
   ```java
   private void validateRequiredFields(OsmNode osmNode) {
       List<String> missingFields = new ArrayList<>();
       
       if (osmNode.name() == null) missingFields.add("name");
       if (osmNode.street() == null) missingFields.add("addr:street");
       if (osmNode.houseNumber() == null) missingFields.add("addr:housenumber");
       if (osmNode.postcode() == null) missingFields.add("addr:postcode");
       if (osmNode.city() == null) missingFields.add("addr:city");
       
       if (!missingFields.isEmpty()) {
           throw new OsmNodeMissingFieldsException(osmNode.nodeId(), missingFields);
       }
   }
   ```

2. **Type Mapping**
   ```java
   private PosType mapAmenityToType(String amenity) {
       return switch (amenity != null ? amenity.toLowerCase() : "") {
           case "cafe" -> PosType.CAFE;
           case "bakery" -> PosType.BAKERY;
           case "restaurant" -> PosType.RESTAURANT;
           // Add more mappings as needed
           default -> PosType.CAFE; // Default fallback
       };
   }
   ```

3. **Campus Determination** (based on coordinates)
   ```java
   private CampusType determineCampus(Double latitude, Double longitude) {
       // Heidelberg coordinates:
       // Altstadt: ~49.410, 8.710
       // Neuenheimer Feld: ~49.420, 8.670
       
       // Simple distance-based logic or bounding box approach
       // This is simplified; consider using proper geospatial library for production
       
       if (latitude >= 49.408 && latitude <= 49.414 && 
           longitude >= 8.705 && longitude <= 8.715) {
           return CampusType.ALTSTADT;
       } else if (latitude >= 49.415 && latitude <= 49.425 && 
                  longitude >= 8.665 && longitude <= 8.675) {
           return CampusType.NEUENHEIMER_FELD;
       }
       
       return CampusType.ALTSTADT; // Default
   }
   ```

4. **Postal Code Parsing**
   ```java
   private Integer parsePostalCode(String postcode) {
       if (postcode == null) return null;
       try {
           return Integer.parseInt(postcode.trim());
       } catch (NumberFormatException e) {
           log.warn("Invalid postal code format: {}", postcode);
           return null;
       }
   }
   ```

5. **Description Extraction**
   ```java
   private String extractDescription(OsmNode osmNode) {
       // Generate description from available data
       StringBuilder desc = new StringBuilder();
       
       if (osmNode.amenity() != null) {
           desc.append(capitalize(osmNode.amenity()));
       }
       
       if (osmNode.cuisine() != null) {
           desc.append(" - ").append(osmNode.cuisine());
       }
       
       return desc.length() > 0 ? desc.toString() : "Imported from OpenStreetMap";
   }
   ```

**Acceptance Criteria:**
- [ ] Validates all required OSM fields are present
- [ ] Throws `OsmNodeMissingFieldsException` with details of missing fields
- [ ] Maps OSM amenity to PosType correctly
- [ ] Determines campus based on coordinates
- [ ] Handles missing/invalid postal codes
- [ ] Generates meaningful description
- [ ] Includes proper logging

---

### Phase 4: Update Exception Classes

**Files:** 
- `domain/src/main/java/de/seuhd/campuscoffee/domain/exceptions/OsmNodeMissingFieldsException.java`
- `api/src/main/java/de/seuhd/campuscoffee/api/exceptions/GlobalExceptionHandler.java`

**Required Changes:**

1. **Enhance OsmNodeMissingFieldsException**
   ```java
   public OsmNodeMissingFieldsException(Long nodeId, List<String> missingFields) {
       super(String.format("OSM node %d is missing required fields: %s", 
             nodeId, String.join(", ", missingFields)));
       this.nodeId = nodeId;
       this.missingFields = missingFields;
   }
   ```

2. **Add Exception Handlers**
   - Check `GlobalExceptionHandler` has handlers for:
     - `OsmNodeNotFoundException` → 404 Not Found
     - `OsmNodeMissingFieldsException` → 422 Unprocessable Entity

**Acceptance Criteria:**
- [ ] Exception messages are clear and actionable
- [ ] HTTP status codes are appropriate
- [ ] Error responses follow consistent format

---

### Phase 5: Testing

**Required Tests:**

1. **Unit Tests - OsmDataServiceImpl**
   - Test successful API call and parsing
   - Test node not found (404)
   - Test network failure
   - Test invalid XML response
   - Test missing tags
   - Mock RestTemplate responses

2. **Unit Tests - PosServiceImpl**
   - Test OSM to POS conversion with complete data
   - Test conversion with missing required fields
   - Test amenity type mapping
   - Test campus determination
   - Test postal code parsing
   - Mock OsmDataService

3. **Integration Tests**
   - Test full flow from API endpoint to database
   - Test duplicate name handling
   - Test actual OSM node import (using test node ID)
   - Verify database persistence

**Test Data:**
- Use real OSM node: `5589879349` (Rada Coffee & Rösterei)
- Create mock XML responses for various scenarios
- Test edge cases: missing tags, special characters, etc.

**Acceptance Criteria:**
- [ ] Unit test coverage > 80%
- [ ] Integration tests pass with real database
- [ ] Edge cases handled
- [ ] Tests are maintainable and well-documented

---

## Dependencies & Configuration

### Required Dependencies
- ✅ `spring-boot-starter-web` (already included - provides RestTemplate)
- ✅ Java XML parsers (already in JDK)
- ⚠️ Consider adding: `spring-boot-starter-validation` (for input validation)

### Configuration Properties
Add to `application.yaml`:
```yaml
osm:
  api:
    base-url: https://www.openstreetmap.org/api/0.6
    connect-timeout: 5000
    read-timeout: 10000
```

---

## Implementation Order & Effort Estimate

| Phase | Task | Effort | Priority | Dependencies |
|-------|------|--------|----------|--------------|
| 1 | Extend OsmNode model | 1h | High | None |
| 2a | Setup HTTP client in OsmDataServiceImpl | 1h | High | Phase 1 |
| 2b | Implement XML parsing | 2-3h | High | Phase 2a |
| 3 | Implement conversion logic | 2-3h | High | Phase 1, 2 |
| 4 | Update exception handling | 1h | Medium | Phase 3 |
| 5a | Write unit tests | 3-4h | High | Phase 2, 3 |
| 5b | Write integration tests | 2h | Medium | Phase 5a |
| 6 | Documentation & cleanup | 1h | Low | All phases |
| **Total** | | **13-16h** | | |

---

## Testing Strategy

### Manual Testing Checklist

1. **Start Application**
   ```bash
   docker run -d -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:17-alpine
   cd application
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

2. **Test Successful Import**
   ```bash
   curl --request POST http://localhost:8080/api/pos/import/osm/5589879349
   ```
   Expected: 201 Created with POS data

3. **Test Invalid Node**
   ```bash
   curl --request POST http://localhost:8080/api/pos/import/osm/999999999999
   ```
   Expected: 404 Not Found

4. **Test Node with Missing Fields**
   - Find OSM node without address
   Expected: 422 Unprocessable Entity

5. **Verify Database**
   ```bash
   curl http://localhost:8080/api/pos
   ```
   Check imported POS appears in list

---

## Risk Analysis & Mitigation

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| OSM API rate limiting | Medium | High | Add retry logic, caching, exponential backoff |
| Invalid/incomplete OSM data | High | Medium | Robust validation, clear error messages |
| Network failures | Medium | Medium | Timeout configuration, graceful error handling |
| XML parsing errors | Low | High | Comprehensive error handling, validation |
| Campus determination inaccuracy | High | Low | Document limitations, allow manual override |

---

## Future Enhancements (Out of Scope)

1. **Caching** - Cache OSM responses to reduce API calls
2. **Batch Import** - Import multiple nodes at once
3. **Update Detection** - Check if OSM data changed since last import
4. **Coordinate Storage** - Store lat/lon in POS entity
5. **Advanced Campus Detection** - Use proper geospatial queries
6. **Async Processing** - Make imports non-blocking
7. **Import History** - Track which nodes were imported when
8. **Data Quality Checks** - Verify imported data against business rules

---

## Success Criteria (From PRP)

- [x] REST API endpoint `/api/pos/import/osm/{nodeId}` exists
- [ ] System successfully extracts the OSM node ID from request path
- [ ] System retrieves XML data from OpenStreetMap API
- [ ] System parses relevant fields (name, coordinates, tags)
- [ ] System maps OSM data to POS entity fields
- [ ] New POS entity is created and persisted in database
- [ ] Given valid OSM node ID, system correctly imports, parses, and stores POS
- [ ] Appropriate error handling for invalid node IDs or API failures

---

## References

- OpenStreetMap API Documentation: https://wiki.openstreetmap.org/wiki/API_v0.6
- Example API Call: https://www.openstreetmap.org/api/0.6/node/5589879349
- OSM Tag Reference: https://wiki.openstreetmap.org/wiki/Tags
- Spring RestTemplate: https://docs.spring.io/spring-framework/reference/integration/rest-clients.html

---

## Notes

- The existing codebase already has excellent architecture with clear separation of concerns
- The ports-and-adapters pattern is well-implemented
- Focus on maintaining code quality and consistency with existing patterns
- All TODOs in the code should be addressed during implementation
- Consider adding configuration for OSM API URL to make it testable


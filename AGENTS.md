# AI Agent Guide for CampusCoffee Project

This document provides comprehensive guidance for AI agents (LLMs, coding assistants) working on the CampusCoffee project. Following these guidelines ensures consistency with the existing codebase and architectural patterns.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture & Module Structure](#architecture--module-structure)
3. [Technology Stack](#technology-stack)
4. [Coding Conventions](#coding-conventions)
5. [Design Patterns](#design-patterns)
6. [Testing Guidelines](#testing-guidelines)
7. [Documentation Standards](#documentation-standards)
8. [Common Tasks & Workflows](#common-tasks--workflows)
9. [Error Handling](#error-handling)
10. [Database & Persistence](#database--persistence)
11. [API Design](#api-design)
12. [Development Workflow](#development-workflow)
13. [Don'ts - Critical Mistakes to Avoid](#donts---critical-mistakes-to-avoid)

---

## Project Overview

**Project Name**: CampusCoffee (WS 25/26)  
**Purpose**: REST API for managing Point of Sale (POS) locations on university campuses  
**Repository**: `https://github.com/se-ubt/ise25-26_campus-coffee`  
**Version**: 0.0.1  
**Java Version**: 21  
**Spring Boot Version**: 3.5.7

### Key Features
- CRUD operations for POS entities (cafes, bakeries, vending machines)
- Import POS data from OpenStreetMap nodes
- PostgreSQL database with Flyway migrations
- RESTful API with standardized error handling

---

## Architecture & Module Structure

### Hexagonal Architecture (Ports and Adapters Pattern)

This project strictly follows hexagonal architecture with clear separation of concerns:

```
├── domain/          # Core business logic (hexagon)
│   ├── model/       # Domain models (POJOs, records, enums)
│   ├── ports/       # Interfaces (contracts)
│   ├── impl/        # Service implementations
│   └── exceptions/  # Domain-specific exceptions
│
├── api/             # REST API adapter (inbound)
│   ├── controller/  # REST controllers
│   ├── dtos/        # Data Transfer Objects
│   ├── mapper/      # DTO ↔ Domain mappers
│   └── exceptions/  # API exception handling
│
├── data/            # Database adapter (outbound)
│   ├── persistence/ # JPA entities & repositories
│   ├── mapper/      # Entity ↔ Domain mappers
│   └── impl/        # Data service implementations
│
└── application/     # Spring Boot app (main)
    ├── src/main/    # Application configuration
    └── src/test/    # System/integration tests
```

### Module Dependencies

```
api → domain → data
        ↓
   application (orchestrates all)
```

**RULE**: 
- `domain` must NOT depend on `api` or `data`
- `api` and `data` depend on `domain` for interfaces
- `application` ties everything together

---

## Technology Stack

### Core Frameworks & Libraries
- **Spring Boot 3.5.7** - Application framework
- **Spring Web** - REST API
- **Spring Data JPA** - Database access
- **PostgreSQL 17** - Database
- **Flyway** - Database migrations
- **Lombok** - Boilerplate reduction
- **MapStruct 1.6.3** - Object mapping
- **JSpecify** - Null annotations
- **SLF4J/Logback** - Logging
- **JUnit 5.14.0** - Testing

### Build & Tooling
- **Maven 3.9** - Build tool
- **Docker** - PostgreSQL container
- **Mise** - Development environment management

---

## Coding Conventions

### General Guidelines

1. **Always use the existing patterns** - Consistency is critical
2. **Follow Java naming conventions** - CamelCase for classes, camelCase for methods/variables
3. **Use meaningful names** - Avoid abbreviations unless standard (e.g., POS, OSM, ID)
4. **Keep methods focused** - Single Responsibility Principle
5. **Favor immutability** - Use records and final fields where possible

### Java Language Features

#### Use Records for Immutable Data
```java
// ✅ CORRECT - Use records for domain models
@Builder(toBuilder = true)
public record Pos(
    @Nullable Long id,
    @NonNull String name,
    @NonNull PosType type
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}

// ❌ WRONG - Don't use classes for simple data holders
public class Pos {
    private Long id;
    private String name;
    // ... getters/setters
}
```

#### Null Safety with JSpecify
```java
// ✅ CORRECT - Always annotate parameters and return types
public @NonNull Pos getById(@NonNull Long id) throws PosNotFoundException {
    // implementation
}

// ❌ WRONG - Missing null annotations
public Pos getById(Long id) {
    // implementation
}
```

#### Use Lombok Appropriately
```java
// ✅ CORRECT - Use Lombok for entities and services
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "pos")
public class PosEntity {
    // fields
}

@Service
@RequiredArgsConstructor
@Slf4j
public class PosServiceImpl implements PosService {
    private final PosDataService posDataService;
}

// ❌ WRONG - Don't use Lombok @Data on entities (breaks JPA)
@Data
@Entity
public class PosEntity { }
```

### Package Naming
- All packages start with: `de.seuhd.campuscoffee`
- Module-specific: `de.seuhd.campuscoffee.{module}.{subpackage}`
  - Example: `de.seuhd.campuscoffee.domain.model`
  - Example: `de.seuhd.campuscoffee.api.controller`

### File Organization
- One public class per file
- File name matches class name
- Group related classes in appropriate subpackages
- Keep test files in mirrored structure under `src/test/java`

---

## Design Patterns

### 1. Ports and Adapters (Hexagonal Architecture)

**Ports** = Interfaces (define contracts)
**Adapters** = Implementations (fulfill contracts)

```java
// ✅ CORRECT - Define port (interface) in domain
package de.seuhd.campuscoffee.domain.ports;

public interface PosDataService {
    @NonNull Pos upsert(@NonNull Pos pos);
}

// ✅ CORRECT - Implement adapter in data layer
package de.seuhd.campuscoffee.data.impl;

@Service
class PosDataServiceImpl implements PosDataService {
    // Implementation using JPA
}
```

### 2. Service Layer Pattern

Services contain business logic and orchestrate operations:

```java
// ✅ CORRECT - Service orchestrates domain operations
@Service
@RequiredArgsConstructor
@Slf4j
public class PosServiceImpl implements PosService {
    private final PosDataService posDataService;
    private final OsmDataService osmDataService;
    
    @Override
    public @NonNull Pos importFromOsmNode(@NonNull Long nodeId) {
        log.info("Importing POS from OSM node {}...", nodeId);
        
        // Fetch data through port
        OsmNode osmNode = osmDataService.fetchNode(nodeId);
        
        // Business logic
        Pos pos = convertOsmNodeToPos(osmNode);
        
        // Persist through port
        return posDataService.upsert(pos);
    }
}
```

### 3. Builder Pattern (via Lombok)

Use for objects with many fields:

```java
Pos pos = Pos.builder()
    .name("Café Example")
    .type(PosType.CAFE)
    .campus(CampusType.ALTSTADT)
    .street("Hauptstraße")
    .houseNumber("100")
    .postalCode(69117)
    .city("Heidelberg")
    .build();

// For updates (records are immutable):
Pos updated = existingPos.toBuilder()
    .name("New Name")
    .build();
```

### 4. Mapper Pattern (via MapStruct)

Separate mapping logic from business logic:

```java
// ✅ CORRECT - Use MapStruct for layer boundaries
@Mapper(componentModel = "spring")
public interface PosDtoMapper {
    PosDto fromDomain(Pos pos);
    Pos toDomain(PosDto posDto);
}

// In controller:
@PostMapping("")
public ResponseEntity<PosDto> create(@RequestBody PosDto posDto) {
    Pos domain = posDtoMapper.toDomain(posDto);
    Pos saved = posService.upsert(domain);
    return ResponseEntity.ok(posDtoMapper.fromDomain(saved));
}
```

---

## Testing Guidelines

### Test Structure

```
application/src/test/java/
└── de/seuhd/campuscoffee/
    └── systest/
        ├── AbstractSysTest.java        # Base test class
        └── PosSystemTests.java         # Feature tests
```

### System Tests

```java
// ✅ CORRECT - Extend AbstractSysTest for integration tests
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PosSystemTests extends AbstractSysTest {
    
    @Test
    void testCreatePos() {
        // Arrange
        PosDto posDto = PosDto.builder()
            .name("Test Café")
            // ... other fields
            .build();
        
        // Act
        ResponseEntity<PosDto> response = testRestTemplate.postForEntity(
            "/api/pos", posDto, PosDto.class
        );
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Test Café");
    }
}
```

### Unit Test Naming

```java
// Pattern: methodName_scenario_expectedBehavior

@Test
void upsert_withNewPos_createsNewEntity() { }

@Test
void upsert_withExistingId_updatesEntity() { }

@Test
void importFromOsmNode_withInvalidId_throwsNotFoundException() { }
```

### Test Data

Use builder pattern and meaningful test data:

```java
// ✅ CORRECT - Clear, realistic test data
Pos testPos = Pos.builder()
    .name("Test Café Heidelberg")
    .description("Coffee shop for testing")
    .type(PosType.CAFE)
    .campus(CampusType.ALTSTADT)
    .street("Hauptstraße")
    .houseNumber("100")
    .postalCode(69117)
    .city("Heidelberg")
    .build();

// ❌ WRONG - Unclear test data
Pos testPos = Pos.builder()
    .name("Test")
    .description("Desc")
    .type(PosType.CAFE)
    .street("Street")
    .city("City")
    .build();
```

---

## Documentation Standards

### Javadoc Requirements

**MUST document**:
- All public classes and interfaces
- All public methods
- All package-visible methods in ports/services
- Complex private methods

**Template**:
```java
/**
 * Brief one-line summary ending with period.
 * <p>
 * More detailed explanation if needed. Can span multiple paragraphs.
 * Explain the "why" and "what", not just "how".
 * 
 * <p>
 * Example usage:
 * <pre>{@code
 * Pos pos = posService.getById(123L);
 * }</pre>
 *
 * @param id the unique identifier; must not be null
 * @return the POS entity; never null
 * @throws PosNotFoundException if no POS exists with the given ID
 */
@NonNull Pos getById(@NonNull Long id) throws PosNotFoundException;
```

### Comment Style

```java
// ✅ CORRECT - Explain "why" and business context
// POS names must be unique across all campuses to prevent confusion
// when users search for locations. Database constraint enforces this.
@Column(unique = true)
private String name;

// ✅ CORRECT - Explain non-obvious logic
// Extract numeric part and suffix separately (e.g., "21a" → 21 + "a")
// because the database schema stores them in separate columns
String numeric = houseNumber.replaceAll("[^0-9]", "");

// ❌ WRONG - Obvious comments
// Set the name
pos.setName(name);

// Get all POS
List<Pos> all = posService.getAll();
```

### TODO Comments

```java
// ✅ CORRECT - Specific, actionable TODO
// TODO: Implement actual campus determination based on coordinates.
//  Current implementation uses hardcoded values for Heidelberg campuses only.
//  Consider using a geospatial library or external service for production.

// ❌ WRONG - Vague TODO
// TODO: fix this
```

### Method Documentation Standards

1. **Parameters**: Describe what they represent and constraints
2. **Return**: Describe what is returned and any guarantees (never null, etc.)
3. **Exceptions**: Document all checked and significant runtime exceptions
4. **Side effects**: Document if method modifies state or external systems

### Class Documentation Standards

```java
/**
 * Service interface for POS (Point of Sale) operations.
 * <p>
 * This interface defines the core business logic operations for managing Points of Sale.
 * It serves as a port in the hexagonal architecture pattern, implemented by the domain layer
 * and consumed by the API layer. It encapsulates business rules and orchestrates
 * data operations through the {@link PosDataService} port.
 * <p>
 * All methods in this interface enforce business rules such as:
 * <ul>
 *   <li>Name uniqueness across all POS</li>
 *   <li>Required field validation</li>
 *   <li>Timestamp management</li>
 * </ul>
 *
 * @see PosServiceImpl for the implementation
 * @see PosDataService for persistence operations
 */
public interface PosService {
    // methods
}
```

---

## Error Handling

### Exception Hierarchy

```
RuntimeException (unchecked)
├── PosNotFoundException (404)
├── DuplicatePosNameException (409)
├── OsmNodeNotFoundException (404)
└── OsmNodeMissingFieldsException (400)
```

### Creating Domain Exceptions

```java
// ✅ CORRECT - Domain exception with clear message
package de.seuhd.campuscoffee.domain.exceptions;

public class PosNotFoundException extends RuntimeException {
    public PosNotFoundException(Long id) {
        super(String.format("POS with ID %d not found", id));
    }
}

// ✅ CORRECT - Exception with additional context
public class OsmNodeMissingFieldsException extends RuntimeException {
    private final Long nodeId;
    private final List<String> missingFields;
    
    public OsmNodeMissingFieldsException(Long nodeId, List<String> missingFields) {
        super(String.format("OSM node %d is missing required fields: %s", 
              nodeId, String.join(", ", missingFields)));
        this.nodeId = nodeId;
        this.missingFields = missingFields;
    }
    
    // Getters for programmatic access
}
```

### Exception Handling in GlobalExceptionHandler

```java
// ✅ CORRECT - Register new exceptions in GlobalExceptionHandler
@ExceptionHandler({
    PosNotFoundException.class,
    OsmNodeNotFoundException.class
})
public ResponseEntity<ErrorResponse> handleNotFoundException(
    RuntimeException exception,
    WebRequest request
) {
    log.warn("Resource not found: {}", exception.getMessage());
    return buildErrorResponse(exception, HttpStatus.NOT_FOUND, request);
}
```

### HTTP Status Code Mapping

| Exception Type | HTTP Status | Use Case |
|----------------|-------------|----------|
| `*NotFoundException` | 404 Not Found | Resource doesn't exist |
| `Duplicate*Exception` | 409 Conflict | Uniqueness constraint violation |
| `*MissingFieldsException` | 400 Bad Request | Invalid/incomplete data |
| `IllegalArgumentException` | 400 Bad Request | Invalid input parameters |
| Generic `Exception` | 500 Internal Server Error | Unexpected errors |

---

## Database & Persistence

### Entity Design

```java
// ✅ CORRECT - JPA entity with lifecycle callbacks
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "pos")
public class PosEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pos_sequence_generator")
    @SequenceGenerator(name = "pos_sequence_generator", sequenceName = "pos_seq", allocationSize = 1)
    private Long id;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(unique = true)
    private String name;
    
    @Embedded
    private AddressEntity address;
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        createdAt = now;
        updatedAt = now;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(ZoneId.of("UTC"));
    }
}
```

### Repository Pattern

```java
// ✅ CORRECT - Simple JPA repository
public interface PosRepository extends JpaRepository<PosEntity, Long> {
    // Spring Data JPA provides standard CRUD operations
    // Add custom queries only when needed
}
```

### Flyway Migrations

- Location: `data/src/main/resources/db/migration/`
- Naming: `V{version}__{description}.sql`
- Example: `V1__init_pos_table.sql`

```sql
-- ✅ CORRECT - Migration with clear comments
-- Create POS table with address and timestamps
CREATE TABLE pos (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    name VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    type VARCHAR(50) NOT NULL,
    campus VARCHAR(50) NOT NULL,
    -- Address fields (embedded)
    street VARCHAR(255) NOT NULL,
    house_number INTEGER NOT NULL,
    house_number_suffix CHAR(1),
    postal_code INTEGER NOT NULL,
    city VARCHAR(100) NOT NULL
);

CREATE INDEX idx_pos_name ON pos(name);
```

### Mapping Between Layers

```java
// Domain ↔ Entity mapping (in data module)
@Mapper(componentModel = "spring")
public interface PosEntityMapper {
    Pos fromEntity(PosEntity source);
    PosEntity toEntity(Pos source);
    void updateEntity(Pos source, @MappingTarget PosEntity target);
}

// Domain ↔ DTO mapping (in api module)
@Mapper(componentModel = "spring")
public interface PosDtoMapper {
    PosDto fromDomain(Pos source);
    Pos toDomain(PosDto source);
}
```

---

## API Design

### REST Endpoint Conventions

```java
// ✅ CORRECT - RESTful endpoint design
@Controller
@RequestMapping("/api/pos")
@RequiredArgsConstructor
public class PosController {
    
    // GET /api/pos - Get all
    @GetMapping("")
    public ResponseEntity<List<PosDto>> getAll() { }
    
    // GET /api/pos/{id} - Get one
    @GetMapping("/{id}")
    public ResponseEntity<PosDto> getById(@PathVariable Long id) { }
    
    // POST /api/pos - Create
    @PostMapping("")
    public ResponseEntity<PosDto> create(@RequestBody PosDto posDto) {
        PosDto created = /* ... */;
        return ResponseEntity
            .created(getLocation(created.id()))
            .body(created);
    }
    
    // PUT /api/pos/{id} - Update
    @PutMapping("/{id}")
    public ResponseEntity<PosDto> update(
        @PathVariable Long id,
        @RequestBody PosDto posDto
    ) {
        // Validate ID matches
        if (!id.equals(posDto.id())) {
            throw new IllegalArgumentException("ID mismatch");
        }
        return ResponseEntity.ok(upsert(posDto));
    }
    
    // POST /api/pos/import/osm/{nodeId} - Import action
    @PostMapping("/import/osm/{nodeId}")
    public ResponseEntity<PosDto> importFromOsm(@PathVariable Long nodeId) { }
}
```

### Response Status Codes

| Operation | Success Status | Body |
|-----------|---------------|------|
| GET (collection) | 200 OK | List of resources |
| GET (single) | 200 OK | Single resource |
| POST (create) | 201 Created | Created resource + Location header |
| PUT (update) | 200 OK | Updated resource |
| DELETE | 204 No Content | Empty |

### Error Response Format

All errors follow this structure (see `ErrorResponse.java`):

```json
{
  "errorCode": "PosNotFoundException",
  "message": "POS with ID 123 not found",
  "statusCode": 404,
  "statusMessage": "Not Found",
  "timestamp": "2025-11-06T10:30:00",
  "path": "/api/pos/123"
}
```

---

## Development Workflow

### Building the Application

```bash
# Full build with tests
mvn clean install

# Quiet mode (suppress logs)
mvn clean install -q

# Skip tests (not recommended)
mvn clean install -DskipTests
```

### Running the Application

```bash
# 1. Start PostgreSQL
docker run -d \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:17-alpine

# 2. Run application (from project root)
cd application
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Application starts on http://localhost:8080
```

### Testing the API

```bash
# Get all POS
curl http://localhost:8080/api/pos

# Get single POS
curl http://localhost:8080/api/pos/1

# Create POS
curl --header "Content-Type: application/json" \
     --request POST \
     --data '{"name":"New Café","description":"Test","type":"CAFE","campus":"ALTSTADT","street":"Hauptstraße","houseNumber":"100","postalCode":69117,"city":"Heidelberg"}' \
     http://localhost:8080/api/pos

# Import from OSM
curl --request POST http://localhost:8080/api/pos/import/osm/5589879349
```

### Git Workflow

```bash
# Create feature branch
git checkout -b feature/osm-import

# Commit with clear messages
git commit -m "Implement OSM XML parsing in OsmDataServiceImpl"

# Push and create PR
git push origin feature/osm-import
```

### Commit Message Convention

```
<type>: <short summary>

<detailed description if needed>

<reference to issue/PRP if applicable>

Types:
- feat: New feature
- fix: Bug fix
- docs: Documentation only
- refactor: Code restructuring
- test: Adding/updating tests
- chore: Maintenance tasks
```

Example:
```
feat: Implement OSM node XML parsing

- Add XML parsing using DocumentBuilder
- Extract node attributes (lat, lon)
- Parse OSM tags into OsmNode record
- Handle malformed XML responses

Implements requirements from doc/prp/new_feature.md
```

---

## Common Tasks & Workflows

### Adding a New Domain Model

1. **Create record in `domain/src/main/java/.../model/`**
   ```java
   @Builder(toBuilder = true)
   public record MyModel(
       @Nullable Long id,
       @NonNull String name
   ) implements Serializable {
       @Serial
       private static final long serialVersionUID = 1L;
   }
   ```

2. **Create JPA entity in `data/src/main/java/.../persistence/`**
   ```java
   @Entity
   @Getter
   @Setter
   @NoArgsConstructor
   @AllArgsConstructor
   @Table(name = "my_model")
   public class MyModelEntity {
       @Id
       @GeneratedValue(strategy = GenerationType.SEQUENCE)
       private Long id;
       // fields...
   }
   ```

3. **Create MapStruct mapper in `data/src/main/java/.../mapper/`**
   ```java
   @Mapper(componentModel = "spring")
   public interface MyModelEntityMapper {
       MyModel fromEntity(MyModelEntity entity);
       MyModelEntity toEntity(MyModel model);
   }
   ```

4. **Create Flyway migration**
   ```sql
   -- data/src/main/resources/db/migration/V2__add_my_model.sql
   CREATE TABLE my_model (
       id BIGSERIAL PRIMARY KEY,
       name VARCHAR(255) NOT NULL
   );
   ```

### Adding a New REST Endpoint

1. **Define service method in domain port interface**
2. **Implement in service implementation**
3. **Create DTO if needed**
4. **Add controller method**
5. **Register exceptions in GlobalExceptionHandler**
6. **Write tests**
7. **Update README.md with example**

### Adding External API Integration

1. **Create port interface in `domain/src/main/java/.../ports/`**
   ```java
   public interface ExternalApiService {
       @NonNull Data fetchData(@NonNull String id);
   }
   ```

2. **Implement in `data/src/main/java/.../impl/`**
   ```java
   @Service
   @RequiredArgsConstructor
   class ExternalApiServiceImpl implements ExternalApiService {
       private final RestTemplate restTemplate;
       
       @Override
       public @NonNull Data fetchData(@NonNull String id) {
           // HTTP call implementation
       }
   }
   ```

3. **Configure RestTemplate in Application configuration**
4. **Add timeout and error handling**
5. **Write tests with mocked responses**

---

## Don'ts - Critical Mistakes to Avoid

### ❌ Architecture Violations

```java
// ❌ WRONG - Domain depending on persistence
package de.seuhd.campuscoffee.domain.impl;
import de.seuhd.campuscoffee.data.persistence.PosEntity; // NO!

// ❌ WRONG - Direct database access from controller
@Controller
public class PosController {
    private final PosRepository repository; // NO! Use service instead
}

// ❌ WRONG - Business logic in controller
@PostMapping("")
public ResponseEntity<PosDto> create(@RequestBody PosDto dto) {
    // NO! This belongs in service layer
    if (dto.name().length() < 3) {
        throw new ValidationException("Name too short");
    }
}
```

### ❌ Incorrect Exception Handling

```java
// ❌ WRONG - Catching and swallowing exceptions
try {
    posService.getById(id);
} catch (PosNotFoundException e) {
    // Silently ignoring!
}

// ❌ WRONG - Throwing generic exceptions
throw new Exception("Something went wrong");

// ❌ WRONG - Not logging errors
catch (Exception e) {
    throw new RuntimeException(e); // Should log first!
}
```

### ❌ Poor Logging Practices

```java
// ❌ WRONG - String concatenation in logs
log.info("Processing POS with ID: " + id); // Evaluated even if log disabled

// ✅ CORRECT - Use parameterized logging
log.info("Processing POS with ID: {}", id);

// ❌ WRONG - Logging sensitive data
log.info("User password: {}", password);

// ❌ WRONG - Excessive logging in loops
for (Pos pos : allPos) {
    log.debug("Processing: {}", pos); // Can overwhelm logs
}
```

### ❌ Database Anti-patterns

```java
// ❌ WRONG - N+1 query problem
List<PosEntity> allPos = posRepository.findAll();
for (PosEntity pos : allPos) {
    AddressEntity address = addressRepository.findByPosId(pos.getId()); // N queries!
}

// ✅ CORRECT - Use proper fetching or embedded entities
@Embedded
private AddressEntity address; // Single query

// ❌ WRONG - Not using transactions for multi-step operations
public void transferData() {
    posRepository.delete(oldPos);
    posRepository.save(newPos); // If this fails, old is already deleted!
}

// ✅ CORRECT - Use @Transactional
@Transactional
public void transferData() {
    posRepository.delete(oldPos);
    posRepository.save(newPos); // Atomic operation
}
```

### ❌ Testing Mistakes

```java
// ❌ WRONG - Testing implementation details
@Test
void test() {
    verify(posRepository).saveAndFlush(any()); // Testing internals
}

// ✅ CORRECT - Test behavior
@Test
void createPos_validInput_returnsCreatedPos() {
    Pos result = posService.upsert(testPos);
    assertThat(result.id()).isNotNull();
}

// ❌ WRONG - No assertions
@Test
void testGetAll() {
    posService.getAll(); // What are we testing?
}

// ❌ WRONG - Brittle tests dependent on data
@Test
void testGetAll() {
    List<Pos> all = posService.getAll();
    assertEquals(5, all.size()); // Fails if test data changes!
}
```

### ❌ API Design Mistakes

```java
// ❌ WRONG - Returning null
@GetMapping("/{id}")
public PosDto getById(@PathVariable Long id) {
    return posService.getById(id); // Returns null if not found
}

// ✅ CORRECT - Throw exception or use Optional
@GetMapping("/{id}")
public ResponseEntity<PosDto> getById(@PathVariable Long id) {
    Pos pos = posService.getById(id); // Throws PosNotFoundException
    return ResponseEntity.ok(mapper.fromDomain(pos));
}

// ❌ WRONG - Exposing internal IDs/entities
@GetMapping("")
public List<PosEntity> getAll() { // Exposing JPA entity!
    return posRepository.findAll();
}

// ✅ CORRECT - Use DTOs
@GetMapping("")
public ResponseEntity<List<PosDto>> getAll() {
    return ResponseEntity.ok(
        posService.getAll().stream()
            .map(mapper::fromDomain)
            .toList()
    );
}
```

### ❌ Null Handling

```java
// ❌ WRONG - Returning null
public Pos findByName(String name) {
    return null; // Caller must null-check everywhere!
}

// ✅ CORRECT - Throw exception or use Optional
public @NonNull Pos getByName(@NonNull String name) throws PosNotFoundException {
    return posRepository.findByName(name)
        .orElseThrow(() -> new PosNotFoundException(name));
}

// ❌ WRONG - Not checking parameters
public void updatePos(Pos pos) {
    posRepository.save(pos); // NPE if pos is null!
}

// ✅ CORRECT - Use @NonNull annotation (checked by IDE/runtime)
public void updatePos(@NonNull Pos pos) {
    Objects.requireNonNull(pos, "pos must not be null");
    posRepository.save(pos);
}
```

---

## Quick Reference Checklist

When implementing a new feature, ensure:

- [ ] Follows hexagonal architecture (domain → ports → adapters)
- [ ] All public methods have Javadoc
- [ ] Parameters annotated with `@NonNull` or `@Nullable`
- [ ] Exceptions are domain-specific and handled in `GlobalExceptionHandler`
- [ ] Use records for immutable domain models
- [ ] Use Lombok appropriately (`@RequiredArgsConstructor`, `@Slf4j`, etc.)
- [ ] MapStruct mappers for layer boundaries
- [ ] Comprehensive logging with parameterized messages
- [ ] Tests follow naming convention and test behavior, not implementation
- [ ] RESTful endpoint design with correct HTTP status codes
- [ ] Database entities use `@PrePersist`/`@PreUpdate` for timestamps
- [ ] Flyway migrations for schema changes
- [ ] Updated `README.md` with API examples
- [ ] Updated `CHANGELOG.md` with changes
- [ ] Created or updated PRP document if significant feature

---

## Additional Resources

### Documentation Files
- `README.md` - Setup and API examples
- `CHANGELOG.md` - Project change history
- `doc/prp/` - Project Requirement Proposals
- `doc/prp/0_template.md` - PRP template for new features

### Key Classes to Reference
- `domain/impl/PosServiceImpl.java` - Example service implementation
- `api/controller/PosController.java` - Example REST controller
- `data/impl/PosDataServiceImpl.java` - Example data adapter
- `api/exceptions/GlobalExceptionHandler.java` - Exception handling patterns
- `data/mapper/PosEntityMapper.java` - MapStruct mapping examples

### External Documentation
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [MapStruct Documentation](https://mapstruct.org/documentation/)
- [Lombok Features](https://projectlombok.org/features/)
- [JSpecify Annotations](https://jspecify.dev/)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)

---

## Contact & Support

For questions about this codebase:
1. Review this AGENTS.md file
2. Check existing code for patterns
3. Review PRP documents in `doc/prp/`
4. Consult Spring Boot/Java documentation

---

**Last Updated**: 2025-11-06  
**Version**: 1.0  
**Maintainer**: CampusCoffee Development Team


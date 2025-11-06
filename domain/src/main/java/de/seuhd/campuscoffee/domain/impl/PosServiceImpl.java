package de.seuhd.campuscoffee.domain.impl;

import de.seuhd.campuscoffee.domain.exceptions.DuplicatePosNameException;
import de.seuhd.campuscoffee.domain.exceptions.OsmNodeMissingFieldsException;
import de.seuhd.campuscoffee.domain.exceptions.OsmNodeNotFoundException;
import de.seuhd.campuscoffee.domain.model.CampusType;
import de.seuhd.campuscoffee.domain.model.OsmNode;
import de.seuhd.campuscoffee.domain.model.Pos;
import de.seuhd.campuscoffee.domain.exceptions.PosNotFoundException;
import de.seuhd.campuscoffee.domain.model.PosType;
import de.seuhd.campuscoffee.domain.ports.OsmDataService;
import de.seuhd.campuscoffee.domain.ports.PosDataService;
import de.seuhd.campuscoffee.domain.ports.PosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Implementation of the POS service that handles business logic related to POS entities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PosServiceImpl implements PosService {
    private final PosDataService posDataService;
    private final OsmDataService osmDataService;

    @Override
    public void clear() {
        log.warn("Clearing all POS data");
        posDataService.clear();
    }

    @Override
    public @NonNull List<Pos> getAll() {
        log.debug("Retrieving all POS");
        return posDataService.getAll();
    }

    @Override
    public @NonNull Pos getById(@NonNull Long id) throws PosNotFoundException {
        log.debug("Retrieving POS with ID: {}", id);
        return posDataService.getById(id);
    }

    @Override
    public @NonNull Pos upsert(@NonNull Pos pos) throws PosNotFoundException {
        if (pos.id() == null) {
            // Create new POS
            log.info("Creating new POS: {}", pos.name());
            return performUpsert(pos);
        } else {
            // Update existing POS
            log.info("Updating POS with ID: {}", pos.id());
            // POS ID must be set
            Objects.requireNonNull(pos.id());
            // POS must exist in the database before the update
            posDataService.getById(pos.id());
            return performUpsert(pos);
        }
    }

    @Override
    public @NonNull Pos importFromOsmNode(@NonNull Long nodeId) throws OsmNodeNotFoundException {
        log.info("Importing POS from OpenStreetMap node {}...", nodeId);

        // Fetch the OSM node data using the port
        OsmNode osmNode = osmDataService.fetchNode(nodeId);

        // Convert OSM node to POS domain object and upsert it
        // TODO: Implement the actual conversion (the response is currently hard-coded).
        Pos savedPos = upsert(convertOsmNodeToPos(osmNode));
        log.info("Successfully imported POS '{}' from OSM node {}", savedPos.name(), nodeId);

        return savedPos;
    }

    /**
     * Converts an OSM node to a POS domain object.
     * <p>
     * This method validates that all required fields are present in the OSM node,
     * maps OSM amenity types to POS types, determines the campus based on coordinates,
     * and constructs a complete POS object ready for persistence.
     *
     * @param osmNode the OSM node containing location and tag data
     * @return a fully populated POS domain object
     * @throws OsmNodeMissingFieldsException if required fields are missing
     */
    private @NonNull Pos convertOsmNodeToPos(@NonNull OsmNode osmNode) {
        log.debug("Converting OSM node {} to POS", osmNode.nodeId());

        // Validate required fields
        validateRequiredFields(osmNode);

        // Extract and map fields
        String name = osmNode.name();
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

    /**
     * Validates that all required fields for creating a POS are present in the OSM node.
     * <p>
     * Required fields:
     * <ul>
     *   <li>name - The establishment's name</li>
     *   <li>addr:street - Street address</li>
     *   <li>addr:housenumber - House number</li>
     *   <li>addr:postcode - Postal code</li>
     *   <li>addr:city - City name</li>
     * </ul>
     *
     * @param osmNode the OSM node to validate
     * @throws OsmNodeMissingFieldsException if any required field is missing
     */
    private void validateRequiredFields(@NonNull OsmNode osmNode) {
        List<String> missingFields = new java.util.ArrayList<>();

        if (osmNode.name() == null || osmNode.name().isEmpty()) {
            missingFields.add("name");
        }
        if (osmNode.street() == null || osmNode.street().isEmpty()) {
            missingFields.add("addr:street");
        }
        if (osmNode.houseNumber() == null || osmNode.houseNumber().isEmpty()) {
            missingFields.add("addr:housenumber");
        }
        if (osmNode.postcode() == null || osmNode.postcode().isEmpty()) {
            missingFields.add("addr:postcode");
        }
        if (osmNode.city() == null || osmNode.city().isEmpty()) {
            missingFields.add("addr:city");
        }

        if (!missingFields.isEmpty()) {
            log.warn("OSM node {} is missing required fields: {}", osmNode.nodeId(), missingFields);
            throw new OsmNodeMissingFieldsException(osmNode.nodeId(), missingFields);
        }
    }

    /**
     * Maps OSM amenity tag to POS type.
     * <p>
     * Mapping:
     * <ul>
     *   <li>"cafe" → CAFE</li>
     *   <li>"bakery" → BAKERY</li>
     *   <li>"restaurant", "fast_food" → CAFETERIA</li>
     *   <li>"vending_machine" → VENDING_MACHINE</li>
     *   <li>null or unknown → CAFE (default)</li>
     * </ul>
     *
     * @param amenity the OSM amenity tag value (may be null)
     * @return the corresponding POS type
     */
    private PosType mapAmenityToType(String amenity) {
        if (amenity == null) {
            log.debug("No amenity tag found, defaulting to CAFE");
            return PosType.CAFE;
        }

        return switch (amenity.toLowerCase()) {
            case "cafe" -> PosType.CAFE;
            case "bakery" -> PosType.BAKERY;
            case "restaurant", "fast_food" -> PosType.CAFETERIA;
            case "vending_machine" -> PosType.VENDING_MACHINE;
            default -> {
                log.debug("Unknown amenity type '{}', defaulting to CAFE", amenity);
                yield PosType.CAFE;
            }
        };
    }

    /**
     * Determines campus based on geographical coordinates.
     * <p>
     * Uses simple bounding box approach for Heidelberg campuses:
     * <ul>
     *   <li>Altstadt: latitude 49.408-49.414, longitude 8.705-8.715</li>
     *   <li>Bergheim: latitude 49.412-49.418, longitude 8.665-8.680</li>
     *   <li>INF (Neuenheimer Feld): latitude 49.415-49.425, longitude 8.665-8.675</li>
     * </ul>
     * <p>
     * Note: This is a simplified implementation. For production use, consider
     * using a proper geospatial library or service.
     *
     * @param latitude the latitude coordinate
     * @param longitude the longitude coordinate
     * @return the determined campus type
     */
    private CampusType determineCampus(Double latitude, Double longitude) {
        // Altstadt campus (city center)
        if (latitude >= 49.408 && latitude <= 49.414 &&
            longitude >= 8.705 && longitude <= 8.715) {
            log.debug("Coordinates match Altstadt campus");
            return CampusType.ALTSTADT;
        }

        // Bergheim campus
        if (latitude >= 49.412 && latitude <= 49.418 &&
            longitude >= 8.665 && longitude <= 8.680) {
            log.debug("Coordinates match Bergheim campus");
            return CampusType.BERGHEIM;
        }

        // INF campus (Neuenheimer Feld)
        if (latitude >= 49.415 && latitude <= 49.425 &&
            longitude >= 8.665 && longitude <= 8.675) {
            log.debug("Coordinates match INF campus");
            return CampusType.INF;
        }

        // Default to Altstadt if no match
        log.debug("Coordinates don't match known campus, defaulting to Altstadt");
        return CampusType.ALTSTADT;
    }

    /**
     * Parses postal code string to integer.
     * <p>
     * Handles various formats and returns null if parsing fails.
     *
     * @param postcode the postal code string from OSM (may be null)
     * @return the parsed integer postal code, or null if invalid
     */
    private Integer parsePostalCode(String postcode) {
        if (postcode == null || postcode.isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(postcode.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid postal code format: '{}', returning null", postcode);
            return null;
        }
    }

    /**
     * Generates a description from available OSM data.
     * <p>
     * Combines amenity type and cuisine information if available.
     * Falls back to a generic message if no descriptive data is present.
     * <p>
     * Examples:
     * <ul>
     *   <li>amenity="cafe", cuisine="coffee_shop" → "Cafe - coffee_shop"</li>
     *   <li>amenity="bakery", cuisine=null → "Bakery"</li>
     *   <li>amenity=null, cuisine=null → "Imported from OpenStreetMap"</li>
     * </ul>
     *
     * @param osmNode the OSM node containing tag data
     * @return a descriptive string for the POS
     */
    private String extractDescription(@NonNull OsmNode osmNode) {
        StringBuilder desc = new StringBuilder();

        if (osmNode.amenity() != null && !osmNode.amenity().isEmpty()) {
            // Capitalize first letter of amenity type
            String amenity = osmNode.amenity();
            desc.append(Character.toUpperCase(amenity.charAt(0)))
                .append(amenity.substring(1).replace('_', ' '));
        }

        if (osmNode.cuisine() != null && !osmNode.cuisine().isEmpty()) {
            if (desc.length() > 0) {
                desc.append(" - ");
            }
            desc.append(osmNode.cuisine().replace('_', ' '));
        }

        // Fallback to generic description if nothing was found
        if (desc.length() == 0) {
            return "Imported from OpenStreetMap";
        }

        return desc.toString();
    }

    /**
     * Performs the actual upsert operation with consistent error handling and logging.
     * Database constraint enforces name uniqueness - data layer will throw DuplicatePosNameException if violated.
     * JPA lifecycle callbacks (@PrePersist/@PreUpdate) set timestamps automatically.
     *
     * @param pos the POS to upsert
     * @return the persisted POS with updated ID and timestamps
     * @throws DuplicatePosNameException if a POS with the same name already exists
     */
    private @NonNull Pos performUpsert(@NonNull Pos pos) throws DuplicatePosNameException {
        try {
            Pos upsertedPos = posDataService.upsert(pos);
            log.info("Successfully upserted POS with ID: {}", upsertedPos.id());
            return upsertedPos;
        } catch (DuplicatePosNameException e) {
            log.error("Error upserting POS '{}': {}", pos.name(), e.getMessage());
            throw e;
        }
    }
}

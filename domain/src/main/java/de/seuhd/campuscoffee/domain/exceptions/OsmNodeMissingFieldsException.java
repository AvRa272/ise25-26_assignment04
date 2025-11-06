package de.seuhd.campuscoffee.domain.exceptions;

import lombok.Getter;

import java.util.List;

/**
 * Exception thrown when an OpenStreetMap node does not contain the fields required to create a POS.
 * Provides detailed information about which specific fields are missing.
 */
@Getter
public class OsmNodeMissingFieldsException extends RuntimeException {
    private final Long nodeId;
    private final List<String> missingFields;

    /**
     * Creates an exception for a node missing required fields.
     *
     * @param nodeId the OpenStreetMap node ID
     * @param missingFields list of missing field names (e.g., "name", "addr:street")
     */
    public OsmNodeMissingFieldsException(Long nodeId, List<String> missingFields) {
        super(String.format("OSM node %d is missing required fields: %s",
                nodeId, String.join(", ", missingFields)));
        this.nodeId = nodeId;
        this.missingFields = missingFields;
    }

    /**
     * Creates an exception for a node missing required fields (backward compatibility).
     *
     * @param nodeId the OpenStreetMap node ID
     */
    public OsmNodeMissingFieldsException(Long nodeId) {
        super("The OpenStreetMap node with ID " + nodeId + " does not have the required fields.");
        this.nodeId = nodeId;
        this.missingFields = List.of();
    }
}

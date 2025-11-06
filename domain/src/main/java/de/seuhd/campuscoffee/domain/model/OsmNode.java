package de.seuhd.campuscoffee.domain.model;

import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents an OpenStreetMap node with relevant Point of Sale information.
 * This is the domain model for OSM data before it is converted to a POS object.
 * <p>
 * OSM XML structure maps to this record as follows:
 * <ul>
 *   <li>{@code nodeId} - OSM node ID attribute</li>
 *   <li>{@code latitude} - lat attribute on node element</li>
 *   <li>{@code longitude} - lon attribute on node element</li>
 *   <li>{@code name} - tag with k="name"</li>
 *   <li>{@code amenity} - tag with k="amenity" (cafe, bakery, etc.)</li>
 *   <li>{@code cuisine} - tag with k="cuisine"</li>
 *   <li>{@code street} - tag with k="addr:street"</li>
 *   <li>{@code houseNumber} - tag with k="addr:housenumber"</li>
 *   <li>{@code postcode} - tag with k="addr:postcode"</li>
 *   <li>{@code city} - tag with k="addr:city"</li>
 *   <li>{@code website} - tag with k="website"</li>
 *   <li>{@code phone} - tag with k="phone"</li>
 *   <li>{@code openingHours} - tag with k="opening_hours"</li>
 * </ul>
 *
 * @param nodeId The OpenStreetMap node ID
 * @param latitude The latitude coordinate of the node
 * @param longitude The longitude coordinate of the node
 * @param name The name of the establishment
 * @param amenity The type of amenity (cafe, bakery, restaurant, etc.)
 * @param cuisine The type of cuisine offered
 * @param street The street name
 * @param houseNumber The house number (may include suffix like "21a")
 * @param postcode The postal code
 * @param city The city name
 * @param website The website URL
 * @param phone The phone number
 * @param openingHours The opening hours string
 */
@Builder
public record OsmNode(
        @NonNull Long nodeId,
        @NonNull Double latitude,
        @NonNull Double longitude,
        @Nullable String name,
        @Nullable String amenity,
        @Nullable String cuisine,
        @Nullable String street,
        @Nullable String houseNumber,
        @Nullable String postcode,
        @Nullable String city,
        @Nullable String website,
        @Nullable String phone,
        @Nullable String openingHours
) {
}

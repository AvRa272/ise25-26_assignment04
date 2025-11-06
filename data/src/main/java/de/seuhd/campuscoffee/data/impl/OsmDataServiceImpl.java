package de.seuhd.campuscoffee.data.impl;

import de.seuhd.campuscoffee.domain.exceptions.OsmNodeNotFoundException;
import de.seuhd.campuscoffee.domain.model.OsmNode;
import de.seuhd.campuscoffee.domain.ports.OsmDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of OSM data service that fetches node data from OpenStreetMap API.
 * <p>
 * This service:
 * <ul>
 *   <li>Makes HTTP GET requests to the OSM API</li>
 *   <li>Parses XML responses to extract node data and tags</li>
 *   <li>Maps OSM data to the OsmNode domain model</li>
 * </ul>
 * <p>
 * OSM API endpoint: https://www.openstreetmap.org/api/0.6/node/{nodeId}
 */
@Service
@Slf4j
@RequiredArgsConstructor
class OsmDataServiceImpl implements OsmDataService {
    private static final String OSM_API_BASE_URL = "https://www.openstreetmap.org/api/0.6/node/";
    private final RestTemplate restTemplate;

    @Override
    public @NonNull OsmNode fetchNode(@NonNull Long nodeId) throws OsmNodeNotFoundException {
        String url = OSM_API_BASE_URL + nodeId;
        log.info("Fetching OSM node {} from {}", nodeId, url);

        try {
            // Set headers to explicitly request XML response
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_XML, MediaType.TEXT_XML));
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);
            
            log.info("OSM API response - Status: {}, Content-Type: {}", 
                    response.getStatusCode(), response.getHeaders().getContentType());

            if (response.getStatusCode() != HttpStatus.OK) {
                log.error("OSM API returned non-OK status {} for node {}", 
                         response.getStatusCode(), nodeId);
                throw new OsmNodeNotFoundException(nodeId);
            }

            String xmlResponse = response.getBody();
            if (xmlResponse == null || xmlResponse.isEmpty()) {
                log.error("Empty response body from OSM API for node {}", nodeId);
                throw new OsmNodeNotFoundException(nodeId);
            }

            log.debug("Received XML response for node {}: {} bytes", nodeId, xmlResponse.length());
            log.trace("XML content: {}", xmlResponse);
            
            OsmNode osmNode = parseOsmXml(xmlResponse, nodeId);
            log.info("Successfully fetched OSM node {} with name: {}", nodeId, osmNode.name());
            return osmNode;

        } catch (OsmNodeNotFoundException e) {
            // Re-throw domain exceptions as-is
            throw e;
        } catch (HttpClientErrorException e) {
            log.error("HTTP client error fetching OSM node {}: {} {} - Response: {}", 
                     nodeId, e.getStatusCode(), e.getStatusText(), e.getResponseBodyAsString(), e);
            throw new OsmNodeNotFoundException(nodeId);
        } catch (HttpServerErrorException e) {
            log.error("HTTP server error fetching OSM node {}: {} {} - Response: {}", 
                     nodeId, e.getStatusCode(), e.getStatusText(), e.getResponseBodyAsString(), e);
            throw new OsmNodeNotFoundException(nodeId);
        } catch (RestClientException e) {
            log.error("REST client error fetching OSM node {}: {} - {}", 
                     nodeId, e.getClass().getSimpleName(), e.getMessage(), e);
            throw new OsmNodeNotFoundException(nodeId);
        } catch (Exception e) {
            log.error("Unexpected error fetching OSM node {}: {} - {}", 
                     nodeId, e.getClass().getSimpleName(), e.getMessage(), e);
            throw new OsmNodeNotFoundException(nodeId);
        }
    }

    /**
     * Parses OSM XML response and extracts node data and tags.
     * <p>
     * Expected XML structure:
     * <pre>{@code
     * <osm>
     *   <node id="5589879349" lat="49.4134" lon="8.6889">
     *     <tag k="name" v="Rada Coffee & Rösterei"/>
     *     <tag k="amenity" v="cafe"/>
     *     <tag k="addr:street" v="Untere Straße"/>
     *     <tag k="addr:housenumber" v="21"/>
     *     <tag k="addr:postcode" v="69117"/>
     *     <tag k="addr:city" v="Heidelberg"/>
     *   </node>
     * </osm>
     * }</pre>
     *
     * @param xmlResponse the XML response from OSM API
     * @param nodeId the expected node ID (for validation)
     * @return OsmNode with extracted data
     * @throws OsmNodeNotFoundException if XML is invalid or node not found
     */
    private OsmNode parseOsmXml(String xmlResponse, Long nodeId) throws OsmNodeNotFoundException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlResponse)));
            doc.getDocumentElement().normalize();

            // Get the node element
            NodeList nodeList = doc.getElementsByTagName("node");
            if (nodeList.getLength() == 0) {
                log.warn("No <node> element found in OSM XML for node {}", nodeId);
                throw new OsmNodeNotFoundException(nodeId);
            }

            Element nodeElement = (Element) nodeList.item(0);

            // Extract node attributes
            Double latitude = parseDoubleAttribute(nodeElement, "lat", nodeId);
            Double longitude = parseDoubleAttribute(nodeElement, "lon", nodeId);

            // Extract all tags
            Map<String, String> tags = extractTags(nodeElement);

            // Build and return OsmNode
            return OsmNode.builder()
                    .nodeId(nodeId)
                    .latitude(latitude)
                    .longitude(longitude)
                    .name(tags.get("name"))
                    .amenity(tags.get("amenity"))
                    .cuisine(tags.get("cuisine"))
                    .street(tags.get("addr:street"))
                    .houseNumber(tags.get("addr:housenumber"))
                    .postcode(tags.get("addr:postcode"))
                    .city(tags.get("addr:city"))
                    .website(tags.get("website"))
                    .phone(tags.get("phone"))
                    .openingHours(tags.get("opening_hours"))
                    .build();

        } catch (OsmNodeNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse OSM XML for node {}: {}", nodeId, e.getMessage(), e);
            throw new OsmNodeNotFoundException(nodeId);
        }
    }

    /**
     * Parses a double attribute from an XML element.
     *
     * @param element the XML element
     * @param attributeName the attribute name
     * @param nodeId the node ID (for error messages)
     * @return the parsed double value
     * @throws OsmNodeNotFoundException if attribute is missing or invalid
     */
    private Double parseDoubleAttribute(Element element, String attributeName, Long nodeId)
            throws OsmNodeNotFoundException {
        String value = element.getAttribute(attributeName);
        if (value == null || value.isEmpty()) {
            log.warn("Missing {} attribute in OSM node {}", attributeName, nodeId);
            throw new OsmNodeNotFoundException(nodeId);
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid {} value '{}' in OSM node {}", attributeName, value, nodeId);
            throw new OsmNodeNotFoundException(nodeId);
        }
    }

    /**
     * Extracts all tags from a node element as a key-value map.
     * <p>
     * Each {@code <tag k="key" v="value"/>} element is extracted into the map.
     *
     * @param nodeElement the node XML element
     * @return map of tag keys to values
     */
    private Map<String, String> extractTags(Element nodeElement) {
        Map<String, String> tags = new HashMap<>();
        NodeList tagList = nodeElement.getElementsByTagName("tag");

        for (int i = 0; i < tagList.getLength(); i++) {
            Element tagElement = (Element) tagList.item(i);
            String key = tagElement.getAttribute("k");
            String value = tagElement.getAttribute("v");

            if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
                tags.put(key, value);
            }
        }

        log.debug("Extracted {} tags from OSM node", tags.size());
        return tags;
    }
}

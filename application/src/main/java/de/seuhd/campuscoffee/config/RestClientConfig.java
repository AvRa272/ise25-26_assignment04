package de.seuhd.campuscoffee.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for REST client beans used for external API calls.
 */
@Configuration
public class RestClientConfig {

    /**
     * Creates a RestTemplate bean for making HTTP requests to external APIs.
     * Configured with reasonable timeouts and User-Agent header.
     *
     * @param builder Spring's RestTemplate builder
     * @return configured RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .additionalInterceptors(userAgentInterceptor())
                .build();
    }

    /**
     * Adds User-Agent header to all requests.
     * Some APIs (including OpenStreetMap) require or prefer a proper User-Agent.
     *
     * @return interceptor that adds User-Agent header
     */
    private ClientHttpRequestInterceptor userAgentInterceptor() {
        return (request, body, execution) -> {
            request.getHeaders().add("User-Agent", "CampusCoffee/1.0 (Campus Coffee Management System)");
            return execution.execute(request, body);
        };
    }
}


# External System Adapter

Example of integrating with external REST API using Spring's RestTemplate:

```java
@Component
@RequiredArgsConstructor
class ExternalDeviceApiClient implements ExternalDeviceGateway {

    private final RestTemplate restTemplate;
    private final ExternalApiProperties properties;

    @Override
    public Optional<ExternalDeviceData> fetchDeviceData(String deviceId) {
        try {
            String url = properties.getBaseUrl() + "/devices/" + deviceId;
            ExternalDeviceResponse response = restTemplate.getForObject(
                    url,
                    ExternalDeviceResponse.class
            );
            return Optional.ofNullable(response)
                    .map(this::toDomainModel);
        } catch (RestClientException e) {
            log.error("Failed to fetch device data from external API: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private ExternalDeviceData toDomainModel(ExternalDeviceResponse response) {
        // Map external API response to domain model
        return new ExternalDeviceData(
                response.getId(),
                response.getName(),
                response.getStatus()
        );
    }
}

// Configuration properties
@ConfigurationProperties(prefix = "external.device.api")
@Validated
record ExternalApiProperties(
        @NotBlank String baseUrl,
        int timeout
) {}
```

**Best practices:**

- Each adapter should implement only one port
- Adapters should be isolated to be easily replaceable
- External system API / types / interfaces should be encapsulated in adapter
- Use Spring's dependency injection - no manual registration needed
- Use `@ConfigurationProperties` for external configuration
- Adapter implementation should handle mapping between domain model and external API model
- Handle external system failures gracefully (return Optional, use Circuit Breaker patterns)
- Use `@Component` or `@Service` to register adapters


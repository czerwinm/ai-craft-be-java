**# Instructions for implementing Business Modules with Domain-Driven Design and Ports and Adapters

## Overview

This document provides comprehensive instructions for implementing business modules in Java Spring Boot applications using Domain-Driven Design (DDD) and Ports and Adapters (Hexagonal Architecture) patterns. Follow these guidelines to ensure consistent, maintainable, and testable code.

### Key Principles

1. **Domain Independence**: Domain logic must be independent of infrastructure concerns
2. **Aggregate Encapsulation**: Aggregates control access to their internal state and enforce business rules
3. **Ports and Adapters**: Use interfaces (ports) to define contracts between domain and infrastructure
4. **Immutability**: Prefer immutable value objects (Java records) over mutable entities
5. **Event-Driven**: Emit domain events to communicate state changes
6. **Package-Private by Default**: Expose only what's necessary; keep implementation details hidden

### Document Structure

- **Domain-Driven Design Patterns**: Aggregates, Value Objects, Domain Events
- **Ports and Adapters Architecture**: Primary/Secondary Ports, Adapters for HTTP, Persistence, External Systems
- **Persistence**: JPA with JSONB, Liquibase for schema management, Optimistic Locking
- **Testing**: Unit, Integration, and End-to-End testing strategies
- **Spring Boot Integration**: Dependency Injection, Configuration, Pagination
- **Cross-Cutting Concerns**: Logging, Exception Handling

## Example Structure of a Module with Domain-Driven Design, Ports and Adapters, and Tests

```
src/main/java/
  {domain}/
    {module}/
        {Feature}Controller.java # HTTP adapter implementations
        {Feature}Service.java  # Primary port implementations, facade for module functionality
        {Aggregate}.java # Aggregate implementation (package-private)
        {ValueObject}.java # Immutable value objects (records)
        DomainEvent.java  # Domain events (sealed interface or records)
        {Feature}Repository.java # Repository port (interface)
        {Feature}DocumentRepository.java # JPA repository adapter implementation
        {ExternalSystem}Adapter.java # Adapter implementation for external system

src/test/java/
  {domain}/
    {module}/
        {Aggregate}Test.java # Unit test implementation for aggregate
        {ValueObject}Test.java # Unit test implementation for value objects
        {Feature}ServiceTest.java # Service layer unit tests
        {Feature}RepositoryTest.java # Integration test using testcontainers
        {Feature}ControllerTest.java # End-to-end test implementations
        {feature.name}.http # Http client file for manual testing
```

## Domain-Driven Design - Key Patterns

### Aggregate (Aggregate Root)

Aggregates encapsulate key business activities that require validation, consistency, and business sense for processes or edited data.

An aggregate is a collection of objects treated as a whole, with an ID and mutable state, with a clearly designated main object (aggregate root). Only the root can be referenced from outside, and any state changes must be made through the root with rules enforcement, ensuring data consistency.

**How to implement:**

- in aggregate name and file, do not use aggregate suffix, use clean name
- make aggregate class package-private (no public modifier)
- prefer value objects (records) as fields
- if a field should be a mutable object, define it in the aggregate file and make it package-private
- never place aggregate snapshot as an aggregate field, add a method returning snapshot
- do not expose public getter methods for individual fields; package-private access is acceptable for internal use within the module
- implement business rules as private methods (name them according to business rule)
- use final for the aggregate ID and events list, private for mutable state

```java
@AllArgsConstructor
class Device {
    final String deviceId;
    final List<DomainEvent> events;
    private Ownership ownership;
    private Location location;
    private OpeningHours openingHours;
    private Settings settings;

    // Static factory - readable way to create new aggregates
    static Device newDevice(String deviceId) {
        return new Device(
                deviceId,
                new ArrayList<>(),
                Ownership.unowned(),
                null,
                OpeningHours.alwaysOpened(),
                Settings.defaultSettings()
        );
    }

    // Public methods modifying internal state and emitting events
    void assignTo(Ownership ownership) {
        Objects.requireNonNull(ownership);

        // Ensuring idempotency of processed commands
        if (!Objects.equals(this.ownership, ownership)) {
            // Changing internal state
            this.ownership = ownership;
            // Formulating event and storing event for emission during persistence
            events.add(new OwnershipUpdated(deviceId, ownership));

            // Ensuring additional business rules
            if (ownership.isUnowned()) {
                resetToDefaults();
            }
        }
    }

    // Private method implementing business rule
    private void resetToDefaults() {
        updateLocation(null);
        updateOpeningHours(OpeningHours.alwaysOpened());
        updateSettings(Settings.defaultSettings());
    }

    // Method creating state snapshot
    DeviceConfiguration toDeviceConfiguration() {
        Violations violations = checkViolations();
        Visibility visibility = Visibility.basedOn(
                violations.isValid() && settings.isPublicAccess(),
                settings.isShowOnMap()
        );
        return new DeviceConfiguration(
                deviceId,
                ownership,
                location,
                openingHours,
                settings,
                violations,
                visibility
        );
    }

    private Violations checkViolations() {
        return Violations.builder()
                .operatorNotAssigned(ownership.operator() == null)
                .providerNotAssigned(ownership.provider() == null)
                .locationMissing(location == null)
                .build();
    }
}
```

**Best practices:**

- Aggregate controls access to its internal objects
- All state changes are visible through emitted events
- Validations and business rules are enforced inside aggregate
- Aggregate identifier (ID) is unique in the entire system
- Aggregate should have a constructor accepting all fields (use @AllArgsConstructor from Lombok)
- Aggregate functionality is always exposed through Service (Primary Port), which manages aggregate lifecycle:
    - obtains existing instances from repository,
    - creates new aggregate instance in appropriate business situations
    - ensures repository save call
    - may coordinate cooperation with other services / modules / external dependencies

### Value Objects

Value objects are immutable, have no identity, and represent domain concepts. Two value objects with the same attributes are considered equal.

**How to implement:**

Use Java records for value objects - they provide immutability, automatic equals/hashCode, and concise syntax:

```java
public record Ownership(String operator, String provider) {

    // Compact constructor for validation
    public Ownership {
        assert isUnowned() || isOwned();
    }

    // Factory methods for commonly used instances
    public static Ownership unowned() {
        return new Ownership(null, null);
    }

    public static Ownership of(String operator, String provider) {
        return new Ownership(operator, provider);
    }

    // Predicate methods expressing domain concepts
    @JsonIgnore
    public boolean isUnowned() {
        return operator == null && provider == null;
    }

    @JsonIgnore
    public boolean isOwned() {
        return operator != null && provider != null;
    }
}
```

**Best practices:**

- Use Java records for value objects (automatic equals/hashCode)
- Implement static factory methods for readability
- Validate data in compact constructor
- Records are immutable by design - no setters needed
- Method names should reflect domain language
- Use @JsonIgnore for derived properties that shouldn't be serialized

### Domain Events

Domain events represent facts that occurred in the business domain and may trigger further actions.

**How to implement:**

Use Java records for domain events - they provide immutability and clear structure:

```java
// Marker interface or sealed interface
public sealed interface DomainEvent {
    String deviceId();

    // Concrete events as records
    record OwnershipUpdated(String deviceId, Ownership ownership) implements DomainEvent {}

    record LocationUpdated(String deviceId, Location location) implements DomainEvent {}

    record SettingsUpdated(String deviceId, Settings settings) implements DomainEvent {}

}
```

**Best practices:**

- Name events in past tense (e.g., `OwnershipUpdated`)
- Use Java records for immutability
- Use sealed interfaces to restrict which events can implement the marker
- Each event should contain all data needed to understand what happened
- Events should be serializable (records are serializable by default with Jackson)

**What to avoid:**
- never place events directly in package, place them inside marker / sealed interface
- Do not add event timestamp to events. Timestamps are added in persistence layer

## Ports and Adapters (Hexagonal Architecture)

### Primary Port

Module functionality is always exposed through Service (Primary Port), which manages domain object lifecycle:

- obtains instance from repository,
- creates new aggregate instance
- ensures repository save call
  And coordinates cooperation with other services / modules / external dependencies.

```java
@Service
@Transactional
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository repository;

    @Transactional(readOnly = true)
    public Optional<DeviceConfiguration> getDevice(String deviceId) {
        return repository.get(deviceId)
                .map(Device::toDeviceConfiguration);
    }

    public DeviceConfiguration createNewDevice(String deviceId, UpdateDevice update) {
        Device device = Device.newDevice(deviceId);
        update.apply(device);
        repository.save(device);
        return device.toDeviceConfiguration();
    }

    public Optional<DeviceConfiguration> updateDevice(String deviceId, UpdateDevice update) {
        return repository.get(deviceId)
                .map(device -> {
                    update.apply(device);
                    repository.save(device);
                    return device.toDeviceConfiguration();
                });
    }

    @EventListener
    public void handleTaskCreated(TaskCreatedEvent event) {
        repository.get(event.deviceId())
                .ifPresent(device -> {
                    UpdateDevice update = taskToUpdateDevice(event);
                    update.apply(device);
                    repository.save(device);
                });
    }
}
```

### Primary Port Adapters

Primary port adapters are application entry points that initiate interaction with the business domain through primary ports. In hexagonal architecture, they include elements such as API controllers, event listeners, user interfaces, or cron scripts.

### HTTP Adapter (API Controllers)

API controllers are adapters for primary port that handle HTTP requests and translate them into domain service calls.

**How to implement:**

```java
@RestController
@RequiredArgsConstructor
class DeviceController {

    private final DeviceService service;

    @GetMapping(path = "/devices/{deviceId}", produces = APPLICATION_JSON_VALUE)
    DeviceConfiguration get(@PathVariable String deviceId) {
        return service.getDevice(deviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PutMapping(path = "/devices/{deviceId}",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE)
    DeviceConfiguration put(@PathVariable String deviceId,
                            @RequestBody @Valid UpdateDevice update) {
        return service.createNewDevice(deviceId, update);
    }

    @PatchMapping(path = "/devices/{deviceId}",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE)
    DeviceConfiguration patch(@PathVariable String deviceId,
                              @RequestBody @Valid UpdateDevice update) {
        return service.updateDevice(deviceId, update)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
```

**Best practices:**

- Controllers should be thin. Controllers only handle input validation, primary port call, and response formatting
- Make controllers package-private (no public modifier) when possible
- Use @RequestBody with @Valid for automatic validation
- Use Optional and orElseThrow() for proper error handling
- Return appropriate HTTP status codes using ResponseStatusException
- Business logic should always be in Domain Model, never in controller
- Use @RequiredArgsConstructor from Lombok for constructor injection
- Group endpoints in controllers by business resources

### Message Queue and Message Broker Adapters

Message queue and message broker adapters allow integration with external asynchronous communication systems like Kafka.

**How to implement:**

```java
@Component
@RequiredArgsConstructor
@Slf4j
class KafkaEventAdapter {

    private final ApplicationEventPublisher eventPublisher;

    @KafkaListener(topics = "device-events", groupId = "device-service")
    public void handleDeviceEvent(String message) {
        try {
            DeviceEventMessage event = objectMapper.readValue(message, DeviceEventMessage.class);
            // Passing to internal event bus (Spring ApplicationEventPublisher)
            eventPublisher.publishEvent(new DeviceCreatedEvent(
                    event.deviceId(),
                    event.name()
            ));
        } catch (Exception e) {
            log.error("Error processing device event: {}", e.getMessage(), e);
            throw e; // Reprocessing by Kafka
        }
    }
}
```

**Best practices:**

- Separate business logic from message broker integration details
- Implement appropriate message processing patterns (Circuit Breaker, Retry, Dead Letter Queue)
- Map messages from external format to domain objects
- Use Spring Kafka for Kafka integration
- Ensure proper error and exception handling
- Standardize topic naming according to project convention
- Use @Slf4j from Lombok for logging

### Scheduled Jobs Adapter

Scheduled job adapters trigger business processes at specified time intervals using Spring's @Scheduled annotation.

**How to implement:**

```java
@Component
@RequiredArgsConstructor
@Slf4j
class DeviceScheduledJobs {

    private final DeviceService deviceService;

    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void cleanupUnassignedDevices() {
        log.info("Starting cleanup of unassigned devices");
        try {
            deviceService.cleanupUnassignedDevices();
        } catch (Exception e) {
            log.error("Failed to cleanup devices: {}", e.getMessage(), e);
        }
    }
}
```

**Best practices:**

- Use @Scheduled annotation with cron expressions
- Implement robust error handling for scheduled jobs
- Monitor job execution through logs and metrics
- Use @EnableScheduling in configuration class to enable scheduling
- Consider using ShedLock for distributed environments to prevent concurrent execution

### Secondary Ports (Interfaces)

Ports define communication contracts between the domain layer and external systems. In hexagonal architecture, ports are contracts that specify how the domain communicates with the external world.

**How to implement:**

```java
// Secondary/Driven Port - used by domain
interface DeviceRepository {
    Optional<Device> get(String deviceId);
    void save(Device device);
}

// Secondary port for external service
interface FileStorage {
    FileMetadata uploadFile(String path, byte[] content, String contentType);
    byte[] downloadFile(String path);
}
```

**Best practices:**

- Define ports as Java interfaces
- Port methods should clearly define expected input and output types using Optional where appropriate
- Port names should reflect their role in the system (e.g., Repository, Gateway, Client)
- Ports should be independent of implementation details
- Make repository interfaces package-private when they're only used within the module

### Secondary Port Adapters

Adapters are concrete implementations of ports that connect domains with external infrastructure such as databases, file systems, or external services.

### Persistence Adapter

**How to implement JPA Repository with JSONB storage and optimistic locking:**

The repository adapter uses JPA with Hibernate's JSONB support to store aggregates as JSON documents:

```java
@Primary
@Repository
@AllArgsConstructor
class DeviceDocumentWithHistoryRepository implements DeviceRepository {

    private final DocumentRepository documents;
    private final EventRepository events;
    private final ApplicationEventPublisher publisher;

    @Override
    public Optional<Device> get(String deviceId) {
        return documents.findById(deviceId)
                .map(DeviceDocumentEntity::getDevice);
    }

    @Override
    public void save(Device device) {
        List<DomainEvent> emitted = eventsEmittedFrom(device);

        documents.save(documents.findById(device.deviceId)
                .orElseGet(() -> new DeviceDocumentEntity(device.deviceId))
                .setDevice(device)
        );

        emitted.forEach(event -> events.save(
                new DeviceEventEntity(device.deviceId, event)
        ));

        if (!emitted.isEmpty()) {
            publisher.publishEvent(device.toDeviceConfiguration());
        }
        emitted.forEach(publisher::publishEvent);
    }

    private static List<DomainEvent> eventsEmittedFrom(Device device) {
        List<DomainEvent> emitted = List.copyOf(device.events);
        device.events.clear();
        return emitted;
    }

    // JPA Repository for document storage
    @Repository
    interface DocumentRepository extends JpaRepository<DeviceDocumentEntity, String> {
    }

    // Entity with JSONB column storing the aggregate
    @Entity
    @Table(name = "device_document")
    @NoArgsConstructor
    static class DeviceDocumentEntity {
        @Id
        private String deviceId;

        @Version
        private long version;  // Optimistic locking

        @Getter
        @Type(JsonBinaryType.class)
        private Device device;

        public DeviceDocumentEntity setDevice(Device device) {
            this.device = device;
            return this;
        }

        DeviceDocumentEntity(String deviceId) {
            this.deviceId = deviceId;
        }
    }

    // JPA Repository for event history
    @Repository
    interface EventRepository extends CrudRepository<DeviceEventEntity, UUID> {
    }

    // Entity for event log
    @Entity
    @Table(name = "device_events")
    @NoArgsConstructor
    static class DeviceEventEntity {
        @Id
        private UUID id;
        private String deviceId;
        private String type;
        private Instant time;

        @Type(JsonBinaryType.class)
        private DomainEvent event;

        DeviceEventEntity(String deviceId, DomainEvent event) {
            this.id = UUID.randomUUID();
            this.deviceId = deviceId;
            this.type = EventTypes.of(event).type();
            this.time = Instant.now();
            this.event = event;
        }
    }
}
```

**Best practices for JPA with JSONB:**

- Use `@Type(JsonBinaryType.class)` from Hypersistence Utils for JSONB columns
- Use `@Version` for optimistic locking (automatic handling by JPA)
- Nest JPA repository interfaces and entities within the adapter class
- Make entity classes static and package-private
- Use `@Primary` annotation when multiple repository implementations exist
- Events are automatically persisted with metadata (type, timestamp)
- Use `ApplicationEventPublisher` to publish domain events to Spring event bus
- Clear the events list after publishing to prevent duplicate publications

#### Schema Management

**Schema Initialization with Liquibase**

This project uses Liquibase for database schema management. Liquibase provides version-controlled, declarative schema changes that work consistently across all environments.

**Configuration in `application.properties`:**

```properties
# Disable Hibernate auto-DDL - Liquibase manages schema
spring.jpa.hibernate.ddl-auto=validate

# Enable Liquibase
spring.liquibase.enabled=true
spring.liquibase.change-log=classpath:db/db.changelog.yaml
```

**Liquibase Changelog Structure** (`src/main/resources/db/db.changelog.yaml`):

Each change is defined as a changeSet with a unique UUID and author. Example from the project:

```yaml
databaseChangeLog:

  - changeSet:
      id: 6a47fffc-a97f-4e6a-b37d-22945a26d39a
      author: michal.michaluk
      changes:
        - createTable:
            tableName: device_document
            columns:
              - column:
                  name: device_id
                  type: varchar(255)
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: version
                  type: int
                  defaultValue: 1
                  constraints:
                    nullable: false
              - column:
                  name: device
                  type: jsonb
                  constraints:
                    nullable: false

  - changeSet:
      id: 29dfd3d3-47e1-4afa-b80d-74f6727544fc
      author: michal.michaluk
      changes:
        - createTable:
            tableName: device_events
            columns:
              - column:
                  name: id
                  type: uuid
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: device_id
                  type: varchar(255)
                  constraints:
                    nullable: false
              - column:
                  name: type
                  type: varchar(255)
                  constraints:
                    nullable: false
              - column:
                  name: time
                  type: timestamp without time zone
                  constraints:
                    nullable: false
              - column:
                  name: event
                  type: jsonb
                  constraints:
                    nullable: false
```

**Best Practices for Liquibase:**

- **Use UUIDs for changeSet IDs** - ensures global uniqueness across team members
- **Never modify existing changeSets** - always add new changeSets for schema changes
- **Use YAML format** - more readable than XML, less error-prone than SQL
- **Include author information** - helps track who made which changes
- **Use descriptive comments** - add comments for complex or non-obvious changes
- **JSONB for aggregate storage** - store entire aggregates as JSON in PostgreSQL
- **Version columns for optimistic locking** - use `int` with `defaultValue: 1`
- **Separate event tables** - maintain audit trail with dedicated event tables
- **Run Liquibase on startup** - `spring.liquibase.enabled=true` ensures schema is always up-to-date

**Testing with Testcontainers:**

For effective testing of JPA repositories, use Testcontainers with Spring Boot Test:

```java
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DeviceDocumentWithHistoryRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.5-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private DeviceRepository repository;

    @Test
    void shouldSaveAndRetrieveDevice() {
        // Given
        Device device = Device.newDevice("test-device-id");
        device.assignTo(Ownership.of("operator1", "provider1"));

        // When
        repository.save(device);
        Optional<Device> retrieved = repository.get("test-device-id");

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().toDeviceConfiguration().ownership())
                .isEqualTo(Ownership.of("operator1", "provider1"));
    }

    @Test
    void shouldReturnEmptyForNonExistentDevice() {
        // When
        Optional<Device> result = repository.get("non-existent-id");

        // Then
        assertThat(result).isEmpty();
    }
}
```

**Best Practices for JPA Repository Testing:**

- Use `@Testcontainers` and `@Container` annotations for automatic container lifecycle
- Use `@DynamicPropertySource` to configure Spring datasource from container
- Use `@AutoConfigureTestDatabase(replace = NONE)` to prevent H2 auto-configuration
- Containers are started once per test class and shared across tests
- Clean up test data between tests using `@Transactional` or manual cleanup
- Use AssertJ for fluent assertions
- Test both successful operations and edge cases

**Best practices:**

- Each adapter should implement only one port
- Adapters should be isolated to be easily replaceable
- Use Spring's dependency injection - no manual provider registration needed
- Adapter implementation should handle mapping between domain model and infrastructure model
- Make adapters package-private when they're only used within the module

### External System Adapter

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

## Unit Testing

Unit tests verify individual components (aggregates, value objects, services) in isolation from infrastructure.

**Testing Aggregates:**

```java
class DeviceTest {

    @Test
    void shouldAssignOwnershipAndEmitEvent() {
        // Given
        Device device = Device.newDevice("device-1");

        // When
        device.assignTo(Ownership.of("operator1", "provider1"));

        // Then
        assertThat(device.toDeviceConfiguration().ownership())
                .isEqualTo(Ownership.of("operator1", "provider1"));
        assertThat(device.events).hasSize(1);
        assertThat(device.events.get(0)).isInstanceOf(OwnershipUpdated.class);
    }

    @Test
    void shouldResetToDefaultsWhenUnassigned() {
        // Given
        Device device = Device.newDevice("device-1");
        device.assignTo(Ownership.of("operator1", "provider1"));
        device.updateSettings(Settings.of(true, true));
        device.events.clear();

        // When
        device.assignTo(Ownership.unowned());

        // Then
        DeviceConfiguration config = device.toDeviceConfiguration();
        assertThat(config.ownership()).isEqualTo(Ownership.unowned());
        assertThat(config.settings()).isEqualTo(Settings.defaultSettings());
        assertThat(device.events).hasSizeGreaterThan(1);
    }
}
```

**Testing Value Objects:**

```java
class OwnershipTest {

    @Test
    void shouldBeUnownedWhenBothNull() {
        Ownership ownership = Ownership.unowned();
        assertThat(ownership.isUnowned()).isTrue();
        assertThat(ownership.isOwned()).isFalse();
    }

    @Test
    void shouldBeOwnedWhenBothPresent() {
        Ownership ownership = Ownership.of("operator1", "provider1");
        assertThat(ownership.isOwned()).isTrue();
        assertThat(ownership.isUnowned()).isFalse();
    }

    @Test
    void shouldUseValueEquality() {
        Ownership ownership1 = Ownership.of("operator1", "provider1");
        Ownership ownership2 = Ownership.of("operator1", "provider1");
        assertThat(ownership1).isEqualTo(ownership2);
    }
}
```

## Testing Adapters

Adapters should be tested in isolation to ensure correct integration with external systems. Integration tests using Testcontainers allow effective testing of real adapter implementations without using mocks.

**How to implement repository tests:**

```java

@IntegrationTest
@RecordApplicationEvents
class DeviceDocumentWithHistoryRepositoryTest {

    @Autowired
    DeviceDocumentWithHistoryRepository repository;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private ApplicationEvents emitted;

    @Test
    void saveAndGetDevice() {
        Device saved = DeviceFixture.givenStepByStepConfiguredDevice();
        transactional(() -> repository.save(saved));
        Optional<Device> read = transactional(() -> repository.get(saved.deviceId));

        JsonAssert.assertThat(read).isExactlyLike("""
                {
                  "deviceId": "%s",
                  "events": [],
                  "ownership": {
                    "operator": "Devicex.nl",
                    "provider": "public-devices"
                  },
                  "location": {
                    "street": "Rakietowa",
                    "houseNumber": "1A",
                    "city": "Wrocław",
                    "postalCode": "54-621",
                    "state": null,
                    "country": "POL",
                    "coordinates": {
                      "longitude": 51.09836221719513,
                      "latitude": 16.931752852309156
                    }
                  },
                  "openingHours": {
                    "alwaysOpen": true
                  },
                  "settings": {
                    "autoStart": false,
                    "remoteControl": false,
                    "billing": false,
                    "reimbursement": false,
                    "showOnMap": false,
                    "publicAccess": false
                  }
                }
                """, saved.deviceId);
    }

    @Test
    void emitsDomainEvents() {
        Device saved = DeviceFixture.givenStepByStepConfiguredDevice();
        transactional(() -> repository.save(saved));

        assertThat(emitted.stream(DomainEvent.class))
                .containsExactly(
                        new OwnershipUpdated(saved.deviceId, DeviceFixture.ownership()),
                        new LocationUpdated(saved.deviceId, DeviceFixture.location())
                );
        assertThat(emitted.stream(DeviceConfiguration.class))
                .containsExactly(saved.toDeviceConfiguration());
    }
}
```

**Best practices:**

- Use Testcontainers to test real external dependencies (databases, services, etc.)
- Test all adapter methods in isolation from the rest of the system
- Prepare clean environment before each test to ensure their independence
- Test positive and negative scenarios (e.g., saving and retrieving correct data and error handling)
- Use adapters directly, bypassing API layer, to test specifically adapter functionality
- Remember about resources - always clean up after tests (stop containers, remove test data)
- Prefer assertions by comparing entire object instead of many single assertions
    - using toEqual method if we want to check all fields
    - using toMatchObject method if we want to match only some fields like IDs or dates to expected format

## End-to-End Testing of Business Modules

End-to-end (e2e) tests verify the operation of the entire business module, from API controllers to infrastructure adapters, using real external dependencies. Containerization (Testcontainers) enables isolated and repeatable test environment.

**How to implement repository tests:**

```java
@IntegrationTest(profiles = {"auth-test", "kafka-test", "integration-test"})
class E2EScenariosTest {

    @Autowired
    AuthFixture auth;
    @Autowired
    RequestsFixture requests;
    @Autowired
    InstallationService service;
    @Autowired
    KafkaFixture kafka;
    @Autowired
    RestTemplateFixture rest;
    private MockRestServiceServer installations2DeviceClient;

    final String orderId = DeviceFixture.randomId();
    final String deviceId = DeviceFixture.randomId();

    @BeforeEach
    void setUp() {
        installations2DeviceClient = rest.getRestTemplate("devicesClient", "rest")
            .overrideToLocalServer()
            .interceptToMockRestServiceServer();
        requests.withJwt(auth.tokenFor("john", "john"));
    }

    @Test
    void fullInstallationAndConfigurationOfDevice() {
        // when
        kafka.publish("sales.work-orders", orderId, """
        {
            "id": "%s",
            "tenant": "Devicex.nl",
            "account": "public-devices"
        }
        """, orderId);

        // given
        requests.installations.get(0, 10000).isExactlyLike("""
        {"content":[{"orderId":"%s","deviceId":null,"state":"PENDING"}],"totalPages":1,"totalElements":1,"page":0,"size":1}""", orderId);
        requests.installations.get(orderId).isExactlyLike("""
        {"orderId":"%s","deviceId":null,"state":"PENDING"} """, orderId);

        // when
        requests.installations.patch(orderId, """
        { "assignDevice": "%s" } """, deviceId)
            .isExactlyLike("""
        {"orderId":"%s","deviceId":"%s","state":"DEVICE_ASSIGNED"}""", orderId, deviceId);

        // when
        requests.installations.patch(orderId, """
        {
            "assignLocation": {
            "street": "Rakietowa",
                "houseNumber": "1A",
                "city": "Wrocław",
                "postalCode": "54-621",
                "state": null,
                "country": "POL",
                "coordinates": {
                "longitude": 51.09836221719513,
                    "latitude": 16.931752852309156
            }
        }
        }""")
            .isExactlyLike("""
        {"orderId":"%s","deviceId":"%s","state":"DEVICE_ASSIGNED"}
        """, orderId, deviceId);

        requests.communication.bootIot16(deviceId, """
        {
            "chargePointVendor": "Garo",
            "chargePointModel": "CPF25 Family",
            "chargePointSerialNumber": "820394A93203",
            "chargeBoxSerialNumber": "891234A56711",
            "firmwareVersion": "1.1",
            "iccid": "112233445566778899C1",
            "imsi": "082931213347973812",
            "meterType": "5051",
            "meterSerialNumber": "937462A48276"
        }
        """)
            .hasFieldsLike("""
        {"interval":1800,"status":"Pending"}
        """, orderId, deviceId);

        requests.installations.get(orderId)
            .isExactlyLike("""
        {"orderId":"%s","deviceId":"%s","state":"BOOTED"}
        """, orderId, deviceId);

        requests.installations.patch(orderId, """
        { "confirmBoot": true }""")
            .isExactlyLike("""
        {"orderId":"%s","deviceId":"%s","state":"BOOTED"}""", orderId, deviceId);

        requests.intervals.put("""
        {
            "byIds": [ { "seconds": 600, "devices": [ "%s" ] } ],
            "byModel": [ ],
            "defSeconds": 1800
        }
        """, deviceId);

        installations2DeviceClient.expect(requestTo(STR."http://devices-service.cpo-namespace/devices/\{deviceId}"))
    .andExpect(method(PUT))
            .andExpect(content().json("""
        {
            "ownership": {
            "operator": "Devicex.nl",
                "provider": "public-devices"
        },
            "location": {
            "street": "Rakietowa",
                "houseNumber": "1A",
                "city": "Wrocław",
                "postalCode": "54-621",
                "state": null,
                "country": "POL",
                "coordinates": {
                "longitude": 51.09836221719513,
                    "latitude": 16.931752852309156
            }
        }
        }
        """))
            .andRespond(withSuccess());

        requests.installations.patch(orderId, """
        { "complete": true }""")
            .isExactlyLike("""
        {"orderId":"%s","deviceId":"%s","state":"COMPLETED"}""", orderId, deviceId);

        requests.communication.bootIot16(deviceId, """
        {
            "chargePointVendor": "Garo",
            "chargePointModel": "CPF25 Family",
            "chargePointSerialNumber": "820394A93203",
            "chargeBoxSerialNumber": "891234A56711",
            "firmwareVersion": "1.13",
            "iccid": "112233445566778899C1",
            "imsi": "082931213347973812",
            "meterType": "5051",
            "meterSerialNumber": "937462A48276"
        }
        """)
            .hasFieldsLike("""
        {"interval":600,"status":"Accepted"}
        """, orderId, deviceId);

        requests.devices.get(deviceId).isExactlyLike("""
        {
            "deviceId": "%s",
            "ownership": {
            "operator": "Devicex.nl",
                "provider": "public-devices"
        },
            "location": {
            "street": "Rakietowa",
                "houseNumber": "1A",
                "city": "Wrocław",
                "postalCode": "54-621",
                "state": null,
                "country": "POL",
                "coordinates": {
                "longitude": 51.09836221719513,
                    "latitude": 16.931752852309156
            }
        },
            "openingHours": {
            "alwaysOpen": true
        },
            "settings": {
            "autoStart": false,
                "remoteControl": false,
                "billing": false,
                "reimbursement": false,
                "showOnMap": false,
                "publicAccess": false
        },
            "violations": {
            "operatorNotAssigned": false,
                "providerNotAssigned": false,
                "locationMissing": false,
                "showOnMapButMissingLocation": false,
                "showOnMapButNoPublicAccess": false
        },
            "visibility": {
            "roamingEnabled": false,
                "forCustomer": "INACCESSIBLE_AND_HIDDEN_ON_MAP"
        },
            "boot": {
            "protocol": "IoT16",
                "vendor": "Garo",
                "model": "CPF25 Family",
                "serial": "891234A56711",
                "firmware": "1.13"
        }
        }
        """, deviceId);

        requests.devices.patch(deviceId, """
        {
            "settings": {
            "publicAccess": true,
                "showOnMap": true
        }
        }
        """).hasFieldsLike("""
        {
            "deviceId": "%s",
            "settings": {
            "showOnMap": true,
                "publicAccess": true
        },
            "visibility": {
            "forCustomer": "USABLE_AND_VISIBLE_ON_MAP"
        }
        }
        """, deviceId);

        requests.devices.get(deviceId).hasFieldsLike("""
        {
            "deviceId": "%s",
            "settings": {
            "showOnMap": true,
                "publicAccess": true
        },
            "visibility": {
            "forCustomer": "USABLE_AND_VISIBLE_ON_MAP"
        }
        }
        """, deviceId);
    }
}
```

**Key e2e test configuration elements:**

1. **Test isolation**: Use `@Testcontainers` with PostgreSQL containers to ensure each test suite runs in an isolated environment

2. **Test profiles**: Use Spring profiles (e.g., `@SpringBootTest(properties = {"spring.profiles.active=test"})`) to enable test-specific configurations

3. **Test fixtures**: Create fixture classes to encapsulate common test setup and assertions (e.g., `RequestsFixture`, `KafkaFixture`)

4. **Database state management**: Use `@Transactional` on test methods to automatically rollback changes, or implement manual cleanup between tests

5. **Mock external dependencies**: Use `MockRestServiceServer` or similar tools to mock external HTTP services

**Best practices:**

- Test complete business flows from start to finish
- Use Testcontainers to isolate external dependencies (databases, queues, services)
- Simulate events that come from outside the tested module
- Verify both positive and negative scenarios
- Check system reactions to domain events
- Test full resource lifecycle (creation, update, read, delete)
- Test integration between business module components
- Organize tests by business flows, not technical components
- Use helper functions for repeatable operations (e.g., external event emission)
- Use random IDs to ensure test isolation
- Prefer assertions by comparing entire object instead of many single assertions
    - using toEqual method if we want to check all fields
    - using toMatchObject method if we want to match only some fields like IDs or dates to expected format

Remember that e2e tests are the highest level of tests and should verify key business functionalities from the end-user perspective, not implementation details. These tests complement, not replace, unit and integration tests.

### Pagination with Spring Data JPA

When implementing pagination in Spring Data JPA repositories, Spring provides built-in support through `org.springframework.data.domain.Pageable` interface and `org.springframework.data.domain.Page<T>` return type.

**How to implement:**

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// Repository with pagination support
@Repository
interface DeviceReadsRepository extends JpaRepository<DeviceReadsEntity, String> {
    // Returns all results without pagination
    Stream<DeviceReadsEntity> findAllByOperator(String operator);

    // Returns paginated results
    Page<DeviceReadsEntity> findAllByOperator(String operator, Pageable pageable);
}

// Service layer with pagination
@Component
@Transactional
@AllArgsConstructor
class DevicesReadModel {

    private final DeviceReadsRepository repository;

    @Transactional(readOnly = true)
    public List<DevicePin> queryPins(String operator) {
        return repository.findAllByOperator(operator)
                .map(DeviceReadsEntity::getPin)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<DeviceSummary> querySummary(String operator, Pageable pageable) {
        return repository.findAllByOperator(operator, pageable)
                .map(DeviceReadsEntity::getSummary);
    }
}

// Controller with pagination parameters
@RestController
@RequiredArgsConstructor
class DeviceReadsController {

    private final DevicesReadModel projection;

    @GetMapping(path = "/devices/summary", produces = APPLICATION_JSON_VALUE)
    Page<DeviceSummary> getSummary(
            @RequestParam String operator,
            Pageable pageable) {
        return projection.querySummary(operator, pageable);
    }
}
```

**Best practices for pagination:**

- Use Spring Data's `Pageable` interface for pagination parameters
- Use `Page<T>` return type to include pagination metadata
- Spring automatically handles query parameters like `page`, `size`, `sort`
- Use `Page.map()` method for transforming paginated results
- Apply proper indexing on frequently used filtering and sorting columns
- Consider using cursor-based pagination for large datasets
- Configure default and maximum page size in application.properties:
  ```properties
  spring.data.web.pageable.default-page-size=20
  spring.data.web.pageable.max-page-size=100
  ```
- Use `@Transactional(readOnly = true)` for read operations
- Return `Stream<T>` for non-paginated queries that may return many results

For JSONB columns, you might want to add appropriate indexes:

```sql
-- Index for filtering by operator (regular column)
CREATE INDEX idx_device_reads_operator ON search (operator);

-- Index for sorting by title field in JSONB
CREATE INDEX idx_device_details_title ON search ((details->'ownership'->>'operator') ASC);

-- Index for filtering by type in JSONB
CREATE INDEX idx_device_type ON search ((details->'metadata'->>'type'));

-- Index for text search on title using pg_trgm extension
CREATE INDEX idx_device_title_search ON search USING gin ((details->'metadata'->>'title') gin_trgm_ops);
```

## Logging Guidelines

Proper logging is essential for monitoring, debugging, and understanding system behavior. Follow these guidelines to ensure consistent and useful logging across the application.

### Logging in Spring Boot

Spring Boot uses SLF4J with Logback as the default logging framework. Use Lombok's `@Slf4j` annotation to add a logger to your classes.

**How to implement:**

```java
@RestController
@RequiredArgsConstructor
@Slf4j
class InstallationController {

    private final InstallationService service;

    @PatchMapping(path = "/installations/{orderId}",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE)
    InstallationProcessState patch(
            @PathVariable String orderId,
            @RequestBody @Valid ApplyStep step) {
        log.info("PATCH /installations/{} with step: {}", orderId, step.getClass().getSimpleName());

        return service.applyStep(orderId, step)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Work order not found"));
    }
}
```

**Best practices for controller logging:**

- Use `@Slf4j` annotation from Lombok to add logger
- Log HTTP method, path, and key request parameters
- Use parameterized logging (e.g., `log.info("message: {}", value)`) for better performance
- Log at INFO level for normal operations
- Don't log full request body unless necessary for debugging

### Event Handler Logging

Event handlers with `@EventListener` should log the event being processed. Handle exceptions gracefully within event handlers.

**How to implement:**

```java
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class InstallationService {

    private final InstallationRepository repository;

    @EventListener
    public void handle(DeviceBootedEvent event) {
        log.info("Handling DeviceBootedEvent for device: {}", event.deviceId());

        try {
            repository.findByDeviceId(event.deviceId())
                    .ifPresent(installation -> {
                        installation.markAsBooted();
                        repository.save(installation);
                        log.info("Successfully marked installation {} as booted",
                                installation.getOrderId());
                    });
        } catch (Exception e) {
            log.error("Failed to handle DeviceBootedEvent for device {}: {}",
                    event.deviceId(), e.getMessage(), e);
            // Event handlers should not re-throw exceptions
        }
    }
}
```

**Best practices for event handler logging:**

- Log event name and key identifiers at the beginning
- Use try-catch blocks to prevent exceptions from propagating
- Log errors with stack trace using `log.error("message", exception)`
- Never re-throw exceptions from event handlers
- Log successful completion for important operations

### Error Logging Guidelines

**When to log errors:**

- Log errors when you catch them but don't re-throw the same exception
- Log exceptional situations that complete successfully but in an unusual way
- Log errors in event handlers (never re-throw)

**When NOT to log errors:**

- Don't log errors if you're going to re-throw the same exception
- Don't log routine error conditions that are handled gracefully (like Optional.empty())
- Don't log errors just before throwing them

**Example of proper error handling:**

```java
// Good: Log error and handle gracefully
public Result processData(Data data) {
    try {
        return validateAndProcess(data);
    } catch (Exception e) {
        log.error("Failed to process data: {}", e.getMessage(), e);
        return Result.failure("Processing failed");
    }
}

// Bad: Logging normal behaviour expressed in return signature
public Optional<User> findUser(String id) {
    Optional<User> user = repository.findById(id);

    if (user.isEmpty()) {
        log.warn("User not found: {} - returning empty", id); // Don't log
        return Optional.empty();
    }

    return user;
}

// Bad: Logging before re-throwing
public void processData(Data data) {
    try {
        validateAndProcess(data);
    } catch (Exception e) {
        log.error("Failed to process data: {}", e.getMessage()); // Don't log if re-throwing
        throw e; // Re-throwing the same exception
    }
}
```

### General Logging Best Practices

- **Use `@Slf4j` from Lombok** to add logger field automatically
- **Don't log method entry/exit** unless there's a specific debugging need
- **Log exceptional situations** that complete successfully but in an unusual way
- **Use appropriate log levels:**
    - `log.debug()` for detailed debugging information
    - `log.info()` for informational messages
    - `log.warn()` for warning conditions
    - `log.error()` for error conditions with stack trace
- **Use parameterized logging** for better performance: `log.info("User {} logged in", userId)`
- **Include relevant context** in log messages (IDs, parameters, etc.)
- **Avoid logging sensitive information** (passwords, tokens, personal data)
- **Log at the appropriate level** - don't log everything at error level
- **Configure logging in application.properties:**
  ```properties
  logging.level.root=INFO
  logging.level.devices.configuration=DEBUG
  logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %logger{36} - %msg%n
  ```

## Global Exception Handling with @ControllerAdvice

Spring Boot provides centralized exception handling through `@ControllerAdvice` and `@ExceptionHandler` annotations. This approach intercepts exceptions thrown during HTTP request processing and transforms them into appropriate HTTP responses.

### Implementation

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(
            OptimisticLockingFailureException ex,
            HttpServletRequest request) {

        log.warn("Optimistic lock conflict on {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Resource was modified by another transaction. Please retry.",
                Instant.now(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .header("Retry-After", "1")
                .body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation failed on {}: {}", request.getRequestURI(), message);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed: " + message,
                Instant.now(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
            ResponseStatusException ex,
            HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                ex.getStatusCode().value(),
                ex.getReason() != null ? ex.getReason() : ex.getMessage(),
                Instant.now(),
                request.getRequestURI()
        );

        return ResponseEntity.status(ex.getStatusCode()).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception on {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal server error",
                Instant.now(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

// Error response DTO
record ErrorResponse(
        int statusCode,
        String message,
        Instant timestamp,
        String path
) {}
```

### Role and Responsibilities

1. **Centralized Error Handling**: Catches all unhandled exceptions thrown during HTTP request processing
2. **HTTP Status Code Mapping**: Maps different exception types to appropriate HTTP status codes
3. **Consistent Error Response Format**: Ensures all error responses follow the same structure
4. **Logging**: Logs errors with relevant context (URL, message)
5. **Special Headers**: Adds appropriate HTTP headers (e.g., Retry-After for optimistic lock conflicts)

### Supported Exception Types

#### JPA and Validation Exceptions

- **OptimisticLockingFailureException**: Thrown by JPA when optimistic locking fails (version mismatch)
    - Returns HTTP 409 (Conflict) with Retry-After header
- **MethodArgumentNotValidException**: Thrown by Spring when `@Valid` validation fails
    - Returns HTTP 400 (Bad Request) with validation error details
- **ResponseStatusException**: Spring's built-in exception for HTTP errors
    - Returns the specified status code and message
- **Exception**: Catch-all for unhandled exceptions
    - Returns HTTP 500 (Internal Server Error) with detailed logging

### Error Response Format

All error responses follow a consistent format:

```json
{
    "statusCode": 400,
    "message": "Validation failed: field: must not be null",
    "timestamp": "2025-01-15T10:30:00.000Z",
    "path": "/api/devices/123"
}
```

### Best Practices

- **Use `@RestControllerAdvice`** for automatic JSON serialization of error responses
- **Don't log exceptions in controllers** - the global handler handles logging
- **Use `ResponseStatusException`** for simple HTTP errors in controllers:
  `service.findDevice(deviceId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND))`
- **Let JPA handle optimistic locking** - no need to manually check versions
- **Use `@Valid` for request validation** - exceptions are automatically handled
- **Keep exception messages user-friendly** but informative
- **Test exception scenarios** to ensure proper HTTP status codes and responses


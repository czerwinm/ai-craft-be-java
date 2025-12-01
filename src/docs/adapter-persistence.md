# Persistence Adapter

## How to implement JPA Repository with JSONB storage and optimistic locking

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

## Schema Management

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

## Testing with Testcontainers

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


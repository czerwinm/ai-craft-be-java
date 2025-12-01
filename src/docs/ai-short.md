
# Quick Reference for AI Code Generation

## Implementation Guidelines

### Implementation Workflow

When implementing a new feature or module, follow this order:

1. **Identify Domain Concepts**:
    - Identify aggregates (entities with identity and lifecycle)
    - Identify value objects (immutable concepts without identity)
    - Define domain events that represent state changes

2. **Design Domain Model**:
    - Define value objects as records (public when needed, prefer package-private)
    - Create an aggregate class (package-private)
    - Implement business methods that modify the state and emit events
    - Add method to create immutable snapshot (e.g., `toDeviceConfiguration()`)

3. **Define Ports**:
    - Service exposing functionality (public)
    - Create a repository interface (package-private)
    - Define secondary ports for external systems (package-private interfaces)

4. **Implement Service (Primary Port)**:
    - Create a service class (public, annotated with @Service)
    - Implement business operations using aggregates
    - Manage the aggregate lifecycle (load from repository, create new, save)
    - Coordinate with other services if needed

5. **Implement Adapters**:
    - Implement repository adapter with JPA (package-private)
    - Implement HTTP controller (package-private)
    - Implement external system adapters (package-private)

6. **Add Database Schema**:
    - Create a Liquibase changelog with UUID-based changeSet IDs
    - Define tables for document storage (with @Version for optimistic locking)
    - Define tables for event storage

7. **Write Tests**:
    - Unit tests for aggregates and value objects
    - Integration tests for repository adapters (using Testcontainers)
    - End-to-end tests for complete business flows

### Core Rules

1. **Domain layer should be independent of infrastructure:**
    - Do not import framework libraries in domain classes (aggregates, value objects)
    - Use ports to communicate with external systems
    - Domain events and value objects can use framework annotations for serialization (e.g., @JsonIgnore)

2. **Naming should reflect domain language:**
    - Use terms from domain experts' language (ubiquitous language)
    - Avoid technical terms in domain classes (no "Entity", "DTO", "Manager" suffixes)
    - Method names should express business operations, not CRUD operations

3. **Testing:**
    - Test aggregates in isolation (unit tests)
    - Test value objects with logic in isolation (unit tests)
    - Implement integration tests for adapters (using Testcontainers)
    - Implement end-to-end tests for entire module functionality through API, along with persistence adapter, use testcontainers where possible

**General best practices:**

- do not use comments in code, if code block is larger prefer well-named private method
- do not add \_ prefix to private fields
- prefer composition over inheritance
- use Optional for nullable return types, never return null from public methods
- use records for immutable DTOs, snapshots, and value objects
- use sealed interfaces for type hierarchies that should be closed (e.g., domain events)

### When Creating a New Aggregate

```java
// 1. Define value objects first (public records)
public record Ownership(String operator, String provider) {
    public static Ownership unowned() { return new Ownership(null, null); }
    public boolean isUnowned() { return operator == null && provider == null; }
}

// 2. Define domain events (public sealed interface + records)
public sealed interface DomainEvent permits OwnershipUpdated {
    String aggregateId();
}
public record OwnershipUpdated(String deviceId, Ownership ownership) implements DomainEvent {
    @Override public String aggregateId() { return deviceId; }
}

// 3. Create aggregate (package-private class)
@AllArgsConstructor
class DeviceConfigurationEditor {
    final String deviceId;
    final List<DomainEvent> events;
    private Ownership ownership;

    static Device newDevice(String deviceId) {
        return new Device(deviceId, new ArrayList<>(), Ownership.unowned());
    }

    void assignTo(Ownership ownership) {
        if (!Objects.equals(this.ownership, ownership)) {
            this.ownership = ownership;
            events.add(new OwnershipUpdated(deviceId, ownership));
        }
    }

    DeviceConfiguration toDeviceConfiguration() {
        return new DeviceConfiguration(deviceId, ownership);
    }
}

// 4. Create immutable snapshot (public record)
public record DeviceConfiguration(String deviceId, Ownership ownership) {}
```

### When Creating a Service (Primary Port)

```java
@Service
@Transactional
@RequiredArgsConstructor
public class DeviceService {
    private final DeviceRepository repository;

    @Transactional(readOnly = true)
    public Optional<DeviceSnapshot> getDevice(String deviceId) {
        return repository.get(deviceId).map(Device::toSnapshot);
    }

    public DeviceSnapshot createDevice(String deviceId, Ownership ownership) {
        Device device = Device.newDevice(deviceId);
        device.assignTo(ownership);
        repository.save(device);
        return device.toSnapshot();
    }

    public Optional<DeviceSnapshot> updateDevice(String deviceId, Ownership ownership) {
        return repository.get(deviceId).map(device -> {
            device.assignTo(ownership);
            repository.save(device);
            return device.toSnapshot();
        });
    }
}
```

### When Creating a Controller (Primary Port Adapter)

```java
@RestController
@RequiredArgsConstructor
class DeviceController {
    private final DeviceService service;

    @GetMapping("/devices/{deviceId}")
    DeviceSnapshot get(@PathVariable String deviceId) {
        return service.getDevice(deviceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/devices/{deviceId}")
    DeviceSnapshot create(@PathVariable String deviceId,
                         @RequestBody @Valid OwnershipRequest request) {
        return service.createDevice(deviceId, request.toOwnership());
    }

    @PatchMapping("/devices/{deviceId}")
    DeviceSnapshot update(@PathVariable String deviceId,
                         @RequestBody @Valid OwnershipRequest request) {
        return service.updateDevice(deviceId, request.toOwnership())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}

record OwnershipRequest(@NotBlank String operator, @NotBlank String provider) {
    Ownership toOwnership() { return new Ownership(operator, provider); }
}
```

### When Creating a Repository

```java
// 1. Define port (package-private interface)
interface DeviceRepository {
    Optional<Device> get(String deviceId);
    void save(Device device);
}

// 2. Implement adapter (package-private class)
@Repository
@AllArgsConstructor
class DeviceDocumentRepository implements DeviceRepository {
    private final JpaRepo jpaRepo;
    private final ApplicationEventPublisher publisher;

    @Override
    public Optional<Device> get(String deviceId) {
        return jpaRepo.findById(deviceId).map(DeviceEntity::toDevice);
    }

    @Override
    public void save(Device device) {
        List<DomainEvent> events = List.copyOf(device.events);
        device.events.clear();

        jpaRepo.save(new DeviceEntity(device));
        events.forEach(publisher::publishEvent);
    }

    @Repository
    interface JpaRepo extends JpaRepository<DeviceEntity, String> {}

    @Entity
    @Table(name = "device_document")
    @NoArgsConstructor
    static class DeviceEntity {
        @Id private String deviceId;
        @Version private long version;
        @Type(JsonBinaryType.class) private Device device;

        DeviceEntity(Device device) {
            this.deviceId = device.deviceId;
            this.device = device;
        }

        Device toDevice() { return device; }
    }
}
```

### Common Annotations Cheat Sheet

| Component | Visibility | Annotations |
|-----------|-----------|-------------|
| Aggregate | package-private | `@AllArgsConstructor` |
| Value Object | public | `record` keyword, `@JsonIgnore` for derived fields |
| Domain Event | public | `record` keyword, `sealed interface` |
| Service | public | `@Service`, `@Transactional`, `@RequiredArgsConstructor` |
| Controller | package-private | `@RestController`, `@RequiredArgsConstructor` |
| Repository Port | package-private | `interface` (no annotations) |
| Repository Adapter | package-private | `@Repository`, `@AllArgsConstructor` |
| JPA Entity | static nested, package-private | `@Entity`, `@Table`, `@NoArgsConstructor` |
| External Adapter | package-private | `@Component`, `@RequiredArgsConstructor` |

### Liquibase Changelog Template

```yaml
databaseChangeLog:
  - changeSet:
      id: <generate-uuid>  # Use UUID generator
      author: <your-name>
      changes:
        - createTable:
            tableName: <table_name>
            columns:
              - column:
                  name: id
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
                  name: data
                  type: jsonb
                  constraints:
                    nullable: false
```

### Testing Template

```java
// Unit test for aggregate
class DeviceTest {
    @Test
    void shouldEmitEventWhenStateChanges() {
        Device device = Device.newDevice("device-1");
        device.assignTo(Ownership.of("op1", "prov1"));

        assertThat(device.events).hasSize(1);
        assertThat(device.events.get(0)).isInstanceOf(OwnershipUpdated.class);
    }
}

// Integration test for repository
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = NONE)
class DeviceRepositoryTest {
    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:17.5-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
    }

    @Autowired DeviceRepository repository;

    @Test
    void shouldSaveAndRetrieve() {
        Device device = Device.newDevice("test-1");
        repository.save(device);

        Optional<Device> retrieved = repository.get("test-1");
        assertThat(retrieved).isPresent();
    }
}
```

# Domain-Driven Design - Key Patterns

## Aggregate (Aggregate Root)

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

## Value Objects

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

## Domain Events

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


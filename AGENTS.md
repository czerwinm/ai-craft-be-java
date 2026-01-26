# Developer Guidelines

## Build & Test
- **Run all tests:** `./gradlew test`
- **Run single test:** `./gradlew test --tests "ClassName"` (e.g., `./gradlew test --tests "DeviceTest"`)
- **Build:** `./gradlew build`
- **Language:** Java 21 (Preview features enabled: `--enable-preview`)

## Key Principles

1. **Domain Independence**: Domain logic must be independent of infrastructure concerns
2. **Aggregate Encapsulation**: Aggregates control access to their internal state and enforce business rules
3. **Ports and Adapters**: Use interfaces (ports) to define contracts between domain and infrastructure
4. **Immutability**: Prefer immutable value objects (Java records) over mutable entities
5. **Event-Driven**: Emit domain events to communicate state changes
6. **Package-Private by Default**: Expose only what's necessary; keep implementation details hidden


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

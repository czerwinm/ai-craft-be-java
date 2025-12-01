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

## Document Structure

The documentation is organized into focused documents covering specific aspects of the architecture:

-   **@src/docs/domain-model.md**: Domain-Driven Design patterns (Aggregates, Value Objects, Domain Events) and Unit Testing - when developing business logic, always reference and follow the @src/docs/domain-model.md documentation
-   **@src/docs/ports.md**: Primary Port and Secondary Ports definitions - when implementing service layer or defining repository interfaces, reference @src/docs/ports.md
-   **@src/docs/adapter-http.md**: HTTP Adapter (API Controllers) - when implementing REST API endpoints, reference @src/docs/adapter-http.md
-   **@src/docs/adapter-kafka.md**: Message Queue and Message Broker Adapters - when integrating with Kafka or other message brokers, reference @src/docs/adapter-kafka.md
-   **@src/docs/adapter-scheduler.md**: Scheduled Jobs Adapter - when implementing scheduled tasks, reference @src/docs/adapter-scheduler.md
-   **@src/docs/adapter-persistence.md**: Persistence Adapter and Testing Adapters - when implementing JPA repositories or testing adapters, reference @src/docs/adapter-persistence.md
-   **@src/docs/adapter-rest-client.md**: External System Adapter - when integrating with external REST APIs, reference @src/docs/adapter-rest-client.md
-   **@src/docs/e2e-test.md**: End-to-End Testing - when writing e2e tests, reference @src/docs/e2e-test.md


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

# Message Queue and Message Broker Adapters

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


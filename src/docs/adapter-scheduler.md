# Scheduled Jobs Adapter

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


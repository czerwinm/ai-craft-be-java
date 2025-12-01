# HTTP Adapter (API Controllers)

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


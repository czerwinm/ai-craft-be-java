# Ports and Adapters (Hexagonal Architecture)

## Primary Port

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

## Secondary Ports (Interfaces)

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


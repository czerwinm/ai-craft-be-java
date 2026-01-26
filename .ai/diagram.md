# Application Flow Diagram

## Device Configuration Update Flow

This diagram illustrates how a device configuration update (PATCH) is processed through the layers of the application, following the Domain-Driven Design and Ports & Adapters architecture.

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant Controller as DeviceConfigurationController
    participant Service as DeviceConfigurationService
    participant Repository as DeviceConfigurationRepository (Impl)
    participant DB as Database
    participant Publisher as ApplicationEventPublisher

    Client->>Controller: PATCH /devices/{deviceId}
    activate Controller
    
    Controller->>Service: patchDevice(deviceId, patch, version)
    activate Service

    Service->>Repository: findById(deviceId)
    activate Repository
    Repository->>DB: SELECT FROM device_document
    activate DB
    DB-->>Repository: DeviceDocumentEntity
    deactivate DB
    Repository-->>Service: VersionedDevice
    deactivate Repository

    Note over Service: Domain Logic Execution
    Service->>Service: device = versioned.device()
    Service->>Service: patch.applyTo(device, snapshot)
    
    Note right of Service: Aggregate updates internal state<br/>and records DomainEvents

    Service->>Repository: save(device, version)
    activate Repository
    
    Note over Repository: Optimistic Locking Verification
    
    Repository->>DB: UPDATE device_document
    activate DB
    DB-->>Repository: Success
    deactivate DB
    
    Repository->>DB: INSERT INTO device_events
    activate DB
    DB-->>Repository: Success
    deactivate DB
    
    Note over Repository: Event Publishing (Side Effects)
    Repository->>Publisher: publishEvent(DeviceConfigurationSnapshot)
    Repository->>Publisher: publishEvent(DomainEvent...)
    
    Repository-->>Service: newVersion
    deactivate Repository

    Service-->>Controller: DeviceConfigurationSnapshot
    deactivate Service

    Controller-->>Client: 200 OK (DeviceConfigurationSnapshot)
    deactivate Controller
```

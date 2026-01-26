package devices.configuration.management;

import devices.configuration.tools.EventTypes;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
class DeviceConfigurationDocumentRepository implements DeviceConfigurationRepository {

  private final DocumentRepository documents;
  private final EventRepository events;
  private final ApplicationEventPublisher publisher;

  @Override
  public Optional<VersionedDevice> findById(String deviceId) {
    return documents
        .findById(deviceId)
        .map(entity -> new VersionedDevice(entity.getDevice(), entity.getVersion()));
  }

  @Override
  public long save(DeviceConfiguration device, Long expectedVersion) {
    List<DomainEvent> emitted = eventsEmittedFrom(device);
    DeviceDocumentEntity entity =
        documents
            .findById(device.deviceId)
            .orElseGet(() -> new DeviceDocumentEntity(device.deviceId));
    if (expectedVersion != null && entity.getVersion() != expectedVersion) {
      throw new OptimisticLockException(device.deviceId, expectedVersion, entity.getVersion());
    }
    entity.setDevice(device);
    DeviceDocumentEntity saved = documents.save(entity);
    emitted.forEach(event -> events.save(new DeviceEventEntity(device.deviceId, event)));
    if (!emitted.isEmpty()) {
      publisher.publishEvent(device.toSnapshot());
    }
    emitted.forEach(publisher::publishEvent);
    return saved.getVersion();
  }

  @Override
  public void delete(String deviceId, long expectedVersion) {
    DeviceDocumentEntity entity =
        documents.findById(deviceId).orElseThrow(() -> new DeviceNotFoundException(deviceId));
    if (entity.getVersion() != expectedVersion) {
      throw new OptimisticLockException(deviceId, expectedVersion, entity.getVersion());
    }
    documents.delete(entity);
  }

  @Override
  public boolean exists(String deviceId) {
    return documents.existsById(deviceId);
  }

  @Override
  public Page<VersionedDevice> findAll(Pageable pageable) {
    return documents
        .findAll(pageable)
        .map(entity -> new VersionedDevice(entity.getDevice(), entity.getVersion()));
  }

  private static List<DomainEvent> eventsEmittedFrom(DeviceConfiguration device) {
    List<DomainEvent> emitted = List.copyOf(device.events);
    device.events.clear();
    return emitted;
  }

  @Repository
  interface DocumentRepository extends JpaRepository<DeviceDocumentEntity, String> {}

  @Entity
  @Table(name = "device_document")
  @NoArgsConstructor
  static class DeviceDocumentEntity {
    @Id private String deviceId;

    @Version @Getter private long version;

    @Getter
    @Type(JsonBinaryType.class)
    private DeviceConfiguration device;

    DeviceDocumentEntity(String deviceId) {
      this.deviceId = deviceId;
    }

    DeviceDocumentEntity setDevice(DeviceConfiguration device) {
      this.device = device;
      return this;
    }
  }

  @Repository
  interface EventRepository extends JpaRepository<DeviceEventEntity, UUID> {}

  @Entity
  @Table(name = "device_events")
  @NoArgsConstructor
  static class DeviceEventEntity {
    @Id private UUID id;
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

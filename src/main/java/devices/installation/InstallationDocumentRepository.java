package devices.installation;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
class InstallationDocumentRepository implements InstallationRepository {

  private final DocumentRepository documents;
  private final EventRepository events;
  private final ApplicationEventPublisher publisher;

  @Override
  public Optional<Installation> get(WorkOrderId workOrderId) {
    return documents.findById(workOrderId.value()).map(InstallationDocumentEntity::getInstallation);
  }

  @Override
  public void save(Installation installation) {
    List<InstallationEvent> emitted = eventsEmittedFrom(installation);

    documents.save(
        documents
            .findById(installation.workOrderId.value())
            .orElseGet(() -> new InstallationDocumentEntity(installation.workOrderId.value()))
            .setInstallation(installation));

    emitted.forEach(
        event -> events.save(new InstallationEventEntity(installation.workOrderId.value(), event)));

    if (!emitted.isEmpty()) {
      publisher.publishEvent(installation.toSnapshot());
    }
    emitted.forEach(publisher::publishEvent);
  }

  private static List<InstallationEvent> eventsEmittedFrom(Installation installation) {
    List<InstallationEvent> emitted = List.copyOf(installation.events);
    installation.events.clear();
    return emitted;
  }

  @Repository
  interface DocumentRepository extends JpaRepository<InstallationDocumentEntity, String> {}

  @Entity
  @Table(name = "installation_document")
  @NoArgsConstructor
  static class InstallationDocumentEntity {
    @Id
    @Column(name = "order_id")
    private String workOrderId;

    @Version private long version;

    @Column(name = "device_id")
    private String deviceId;

    @Getter
    @Type(JsonBinaryType.class)
    @Column(name = "process")
    private Installation installation;

    public InstallationDocumentEntity setInstallation(Installation installation) {
      this.installation = installation;
      this.deviceId =
          installation.toSnapshot().deviceIdentifier() != null
              ? installation.toSnapshot().deviceIdentifier().value()
              : null;
      return this;
    }

    InstallationDocumentEntity(String workOrderId) {
      this.workOrderId = workOrderId;
    }
  }

  @Repository
  interface EventRepository extends CrudRepository<InstallationEventEntity, UUID> {}

  @Entity
  @Table(name = "installation_events")
  @NoArgsConstructor
  static class InstallationEventEntity {
    @Id private UUID id;

    @Column(name = "order_id")
    private String workOrderId;

    @Column(name = "device_id")
    private String deviceId;

    private String type;
    private Instant time;

    @Type(JsonBinaryType.class)
    private InstallationEvent event;

    InstallationEventEntity(String workOrderId, InstallationEvent event) {
      this.id = UUID.randomUUID();
      this.workOrderId = workOrderId;
      this.type = extractType(event);
      this.time = Instant.now();
      this.event = event;
      this.deviceId = extractDeviceId(event);
    }

    private String extractType(InstallationEvent event) {
      return event.getClass().getSimpleName();
    }

    private String extractDeviceId(InstallationEvent event) {
      if (event instanceof InstallationEvent.DeviceAssigned da) {
        return da.deviceIdentifier().value();
      } else if (event instanceof InstallationEvent.DeviceSwapped ds) {
        return ds.newDeviceIdentifier().value();
      } else if (event instanceof InstallationEvent.InstallationFinished finished) {
        return finished.deviceIdentifier().value();
      }
      return null;
    }
  }
}
